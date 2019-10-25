var UtfString = require('../../utfstring.js');

describe('UtfString visual', function() {
  describe('#findCharIndex', function() {
    it('works with regional indicators', function() {
      var str = '🇸🇴🇫🇷';
      expect(UtfString.visual.findCharIndex(str, 0)).toEqual(0);
      expect(UtfString.visual.findCharIndex(str, 1)).toEqual(0);
      expect(UtfString.visual.findCharIndex(str, 2)).toEqual(0);
      expect(UtfString.visual.findCharIndex(str, 3)).toEqual(0);
      expect(UtfString.visual.findCharIndex(str, 4)).toEqual(1);
      expect(UtfString.visual.findCharIndex(str, 5)).toEqual(1);
      expect(UtfString.visual.findCharIndex(str, 6)).toEqual(1);
      expect(UtfString.visual.findCharIndex(str, 7)).toEqual(1);
      expect(UtfString.visual.findCharIndex(str, 8)).toEqual(-1);
    });
  });
});
