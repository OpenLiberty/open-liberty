var UtfString = require('../utfstring.js');

describe('UtfString', function() {
  describe('#charAt', function() {
    it('works with standard ASCII characters', function() {
      var str = 'abc';
      expect(UtfString.charAt(str, 0)).toEqual('a');
      expect(UtfString.charAt(str, 1)).toEqual('b');
      expect(UtfString.charAt(str, 2)).toEqual('c');
    });

    it('works with multi-byte characters', function() {
      var str = 'ありがとう';  // "arigatou"
      expect(UtfString.charAt(str, 0)).toEqual('あ');  // "a"
      expect(UtfString.charAt(str, 1)).toEqual('り');  // "ri"
      expect(UtfString.charAt(str, 2)).toEqual('が');  // "ga"
      expect(UtfString.charAt(str, 3)).toEqual('と');  // "to"
      expect(UtfString.charAt(str, 4)).toEqual('う');  // "u"
    });

    it('works with astral plane unicode characters', function() {
      var str = '𤔣𤔤𤔥𤔦';
      expect(UtfString.charAt(str, 0)).toEqual('𤔣');
      expect(UtfString.charAt(str, 1)).toEqual('𤔤');
      expect(UtfString.charAt(str, 2)).toEqual('𤔥');
      expect(UtfString.charAt(str, 3)).toEqual('𤔦');
    });

    it('returns an empty string for indices that are out of range', function() {
      var str = 'abc';
      expect(UtfString.charAt(str, 3)).toEqual('');
    });
  });
});
