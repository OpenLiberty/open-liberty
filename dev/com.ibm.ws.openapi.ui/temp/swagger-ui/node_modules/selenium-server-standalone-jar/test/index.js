var expect = require('chai').expect;
var path = require('path');
var fs = require('fs');

var subject = require('../index');

describe('selenium-standalone-server-jar', function() {
  describe('path', function() {
    it('should point to a jar', function() {
      expect(subject.path).to.exist;
      expect(path.extname(subject.path)).to.equal('.jar');
    });

    it('should be an absolute path', function() {
      expect(path.resolve(subject.path)).to.equal(subject.path);
    });

    it('should exist', function(done) {
      fs.exists(subject.path, function(exists) {
        expect(exists).to.be.true;
        done();
      });
    });
  });

  describe('version', function() {
    it('should be a valid version', function() {
      expect(subject.version).to.match(/^\d+\.\d+\.\d+$/)
    });
  });
});
