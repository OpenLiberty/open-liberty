var index = require('./');

index.save = function(db, destination) {
  destination = destination || 'db';
  localStorage.setItem(destination, JSON.stringify(db, null, 2));
};

index.load = function(source) {
  source = source || 'db';
  return JSON.parse(localStorage.getItem(source));
};

_.mixin(index);