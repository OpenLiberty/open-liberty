(function() {
  var composer, constructor, parser, reader, resolver, scanner, util,
    slice = [].slice;

  util = require('./util');

  reader = require('./reader');

  scanner = require('./scanner');

  parser = require('./parser');

  composer = require('./composer');

  resolver = require('./resolver');

  constructor = require('./constructor');

  this.make_loader = function(Reader, Scanner, Parser, Composer, Resolver, Constructor) {
    var Loader, components;
    if (Reader == null) {
      Reader = reader.Reader;
    }
    if (Scanner == null) {
      Scanner = scanner.Scanner;
    }
    if (Parser == null) {
      Parser = parser.Parser;
    }
    if (Composer == null) {
      Composer = composer.Composer;
    }
    if (Resolver == null) {
      Resolver = resolver.Resolver;
    }
    if (Constructor == null) {
      Constructor = constructor.Constructor;
    }
    components = [Reader, Scanner, Parser, Composer, Resolver, Constructor];
    return Loader = (function() {
      var component;

      util.extend.apply(util, [Loader.prototype].concat(slice.call((function() {
        var i, len, results;
        results = [];
        for (i = 0, len = components.length; i < len; i++) {
          component = components[i];
          results.push(component.prototype);
        }
        return results;
      })())));

      function Loader(stream) {
        var i, len, ref;
        components[0].call(this, stream);
        ref = components.slice(1);
        for (i = 0, len = ref.length; i < len; i++) {
          component = ref[i];
          component.call(this);
        }
      }

      return Loader;

    })();
  };

  this.Loader = this.make_loader();

}).call(this);
