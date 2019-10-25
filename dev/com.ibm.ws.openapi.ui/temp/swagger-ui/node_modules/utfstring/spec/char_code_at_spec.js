var UtfString = require('../utfstring.js');

describe('UtfString', function() {
  describe('#charCodeAt', function() {
    it('works with standard ASCII characters', function() {
      var str = 'abc';
      expect(UtfString.charCodeAt(str, 0)).toEqual(97);
      expect(UtfString.charCodeAt(str, 1)).toEqual(98);
      expect(UtfString.charCodeAt(str, 2)).toEqual(99);
    });

    it('works with multi-byte characters', function() {
      var str = 'ありがとう';  // "arigatou"
      expect(UtfString.charCodeAt(str, 0)).toEqual(12354);  // あ "a"
      expect(UtfString.charCodeAt(str, 1)).toEqual(12426);  // り "ri"
      expect(UtfString.charCodeAt(str, 2)).toEqual(12364);  // が "ga"
      expect(UtfString.charCodeAt(str, 3)).toEqual(12392);  // と "to"
      expect(UtfString.charCodeAt(str, 4)).toEqual(12358);  // う "u"
    });

    it('works with astral plane unicode characters', function() {
      str = '𤔣';
      expect(UtfString.charCodeAt(str, 0)).toEqual(148771);
      expect(UtfString.charCodeAt(str, 1)).toBeNaN();
    });

    it('works with regional indicators', function() {
      var str = '🇫🇷';
      expect(UtfString.charCodeAt(str, 0)).toEqual(127467);
      expect(UtfString.charCodeAt(str, 1)).toEqual(127479);
    });
  });
});
