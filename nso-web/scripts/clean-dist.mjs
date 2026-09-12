import { rm } from 'node:fs/promises';
import { resolve } from 'node:path';

const outputDirectory = resolve(process.cwd(), 'dist');
const generatedEntries = ['.vite', 'assets', 'index.html'];

await Promise.all(generatedEntries.map((entry) => rm(resolve(outputDirectory, entry), {
    force: true,
    recursive: true
})));
