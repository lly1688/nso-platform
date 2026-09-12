import { readFile } from 'node:fs/promises';
import { gzipSync } from 'node:zlib';
import { resolve } from 'node:path';

const outputDirectory = resolve(process.cwd(), 'dist');
const manifest = JSON.parse(await readFile(resolve(outputDirectory, '.vite', 'manifest.json'), 'utf8'));
const maxInitialBytes = Number(process.env.NSO_MAX_INITIAL_JS_GZIP || 450 * 1024);
const maxChunkBytes = Number(process.env.NSO_MAX_CHUNK_JS_GZIP || 350 * 1024);
const compressedSizes = new Map();

async function gzipSize(file) {
    if (!compressedSizes.has(file)) {
        const content = await readFile(resolve(outputDirectory, file));
        compressedSizes.set(file, gzipSync(content).length);
    }
    return compressedSizes.get(file);
}

function initialFiles(entry) {
    const files = new Set();
    const visited = new Set();
    const collect = (key) => {
        if (visited.has(key)) {
            return;
        }
        visited.add(key);
        const item = manifest[key];
        if (!item) {
            return;
        }
        if (item.file?.endsWith('.js')) {
            files.add(item.file);
        }
        item.imports?.forEach(collect);
    };
    collect(entry);
    return files;
}

const oversizedChunks = [];
for (const item of Object.values(manifest)) {
    if (item.file?.endsWith('.js')) {
        const size = await gzipSize(item.file);
        if (size > maxChunkBytes) {
            oversizedChunks.push(`${item.file} (${Math.ceil(size / 1024)} KiB gzip)`);
        }
    }
}

const entry = Object.entries(manifest).find(([, item]) => item.isEntry)?.[0];
if (!entry) {
    throw new Error('Vite manifest does not contain an application entry.');
}
const entryFiles = initialFiles(entry);
const initialBytes = (await Promise.all([...entryFiles].map(gzipSize))).reduce((total, size) => total + size, 0);
const initialDescription = [...entryFiles].join(', ');
console.log(`Initial JS: ${Math.ceil(initialBytes / 1024)} KiB gzip (${initialDescription})`);

if (initialBytes > maxInitialBytes || oversizedChunks.length) {
    const failures = [];
    if (initialBytes > maxInitialBytes) {
        failures.push(`Initial JavaScript exceeds ${Math.ceil(maxInitialBytes / 1024)} KiB gzip.`);
    }
    if (oversizedChunks.length) {
        failures.push(`Chunks exceed ${Math.ceil(maxChunkBytes / 1024)} KiB gzip: ${oversizedChunks.join(', ')}`);
    }
    throw new Error(failures.join(' '));
}
