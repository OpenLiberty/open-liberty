var fs = require('fs'),
  index = require('./');

index.save = function(db, destination) {
  destination = destination || 'db.json';
  fs.writeFileSync(destination, JSON.stringify(db, null, 2));
};

index.load = function(source) {
  source = source || 'db.json';
  return JSON.parse(fs.readFileSync(source, 'utf-8'));
};

module.exports = index;