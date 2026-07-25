const major = Number.parseInt(process.versions.node.split('.')[0], 10)

if (Number.isNaN(major) || major < 18) {
  console.error('Node.js 18 or newer is required.')
  process.exit(1)
}

console.log(`Node.js ${process.version} is ready.`)
