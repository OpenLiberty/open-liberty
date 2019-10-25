'use strict'

const fs = require('fs')
const glob = require('glob')
const cssScss = require('css-scss')
const stream = require('stream')

glob('./node_modules/tachyons-custom/src/**/*.css', (err, files) => {
  if (err) {
    throw err
  }

  files.forEach(file => {
    var css = fs.readFileSync(file, 'utf8')
    var fileName = file.replace(/(\.\/node_modules\/tachyons-custom\/src\/|\.css)/g, '')

    if (fileName !== 'tachyons' && fileName !== '_debug') {
      fs.writeFileSync('scss/' + fileName + '.scss', cssScss(css))
    }
  })
})

const tachyonsCSS = fs.createReadStream('./node_modules/tachyons-custom/src/tachyons.css')
const tachyonsSCSS = fs.createWriteStream('./tachyons.scss')
tachyonsCSS.on('data', (data) => {
  const sassStream = new stream.Readable()
  sassStream.push(
    data
      .toString('utf8')
      .replace(/\.\/_/g, 'scss/') // Update paths
      .replace(/^((.|\n)+)(\n \/\*\sModules(.|\n)+)(\n\/\*\sVariables(.|\n)+)/g, '$1$5$3') // Move import orders
      .replace(/\/\*(.*)\*\//g, '// $1') // Change single-line comments to //
      .replace(/\/\*|\s\*\/?/g, '//') // Change multi-line comments to //
  )
  sassStream.push(null)
  sassStream.pipe(tachyonsSCSS)
})
