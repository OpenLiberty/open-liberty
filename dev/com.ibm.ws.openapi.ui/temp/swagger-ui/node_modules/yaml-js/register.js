/*
 * Register .yml and .yaml requires with yaml-js
 */
if (!require || !require.extensions) {
  throw new Error('yaml-js/register only works with require.extensions')
}

const fs = require('fs')
const yaml = require('./')

require.extensions['.yml'] = require.extensions['.yaml'] = requireYaml

function requireYaml (module, filename) {
  module.exports = yaml.load_all(fs.readFileSync(filename, 'utf8'))
}
