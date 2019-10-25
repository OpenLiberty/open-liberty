var UtfString = require('../utfstring.js');

describe('UtfString', function() {
  describe('#findCharIndex', function() {
    it('works with standard ASCII characters', function() {
      var str = 'abc';
      expect(UtfString.findCharIndex(str, 0)).toEqual(0);
      expect(UtfString.findCharIndex(str, 1)).toEqual(1);
      expect(UtfString.findCharIndex(str, 2)).toEqual(2);
      expect(UtfString.findCharIndex(str, 3)).toEqual(-1);
    });

    it('works with multi-byte characters', function() {
      var str = 'ありがとう';
      expect(UtfString.findCharIndex(str, 0)).toEqual(0);
      expect(UtfString.findCharIndex(str, 1)).toEqual(1);
      expect(UtfString.findCharIndex(str, 2)).toEqual(2);
      expect(UtfString.findCharIndex(str, 3)).toEqual(3);
      expect(UtfString.findCharIndex(str, 4)).toEqual(4);
      expect(UtfString.findCharIndex(str, 5)).toEqual(-1);
    });

    it('works with astral plane unicode characters', function() {
      var str = '𤔣𤔤𤔥𤔦';
      expect(UtfString.findCharIndex(str, 0)).toEqual(0);
      expect(UtfString.findCharIndex(str, 1)).toEqual(0);
      expect(UtfString.findCharIndex(str, 2)).toEqual(1);
      expect(UtfString.findCharIndex(str, 3)).toEqual(1);
      expect(UtfString.findCharIndex(str, 4)).toEqual(2);
      expect(UtfString.findCharIndex(str, 5)).toEqual(2);
      expect(UtfString.findCharIndex(str, 6)).toEqual(3);
      expect(UtfString.findCharIndex(str, 7)).toEqual(3);
      expect(UtfString.findCharIndex(str, 8)).toEqual(-1);
    });

    it('works with a newline character', function() {
      var str = "\u{000D}\u{1F1E6}";
      expect(UtfString.findCharIndex(str, 0)).toEqual(0);
      expect(UtfString.findCharIndex(str, 1)).toEqual(1);
      expect(UtfString.findCharIndex(str, 2)).toEqual(1);
    });
  });
});
