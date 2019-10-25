var UtfString = require('../utfstring.js');

describe('UtfString', function() {
  describe('#codePointsToString', function() {
    it('works with standard ASCII characters', function() {
      var arr = [97, 98, 99];
      expect(UtfString.codePointsToString(arr)).toEqual('abc');
    });

    it('works with multi-byte characters', function() {
      var arr = [12354, 12426, 12364, 12392, 12358];
      expect(UtfString.codePointsToString(arr)).toEqual('ありがとう');
    });

    it('works with characters in the unicode astral plane', function() {
      var arr = [148771, 148772, 148773, 148774];
      expect(UtfString.codePointsToString(arr)).toEqual('𤔣𤔤𤔥𤔦');
    });

    it('works with regional indicators', function() {
      var arr = [127467, 127479];
      expect(UtfString.codePointsToString(arr)).toEqual('🇫🇷');
    });
  });
});
