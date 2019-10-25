var json = require('./package.json');
var path = require('path');

module.exports = {
  path: path.resolve(__dirname, 'jar/selenium-server-standalone-' + json.version + '.jar'),
  version: json.version
};
