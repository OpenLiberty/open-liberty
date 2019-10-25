var UtfString = require('../../utfstring.js');

describe('UtfString visual', function() {
  describe('#lastIndexOf', function() {
    it('works with regional indicators', function() {
      var str = '🇫🇷🇸🇴🇫🇷';
      expect(UtfString.visual.lastIndexOf(str, '🇫🇷')).toEqual(2);
      expect(UtfString.visual.lastIndexOf(str, '🇫')).toEqual(2);
      expect(UtfString.visual.lastIndexOf(str, '🇷')).toEqual(2);
      expect(UtfString.visual.lastIndexOf(str, '🇸🇴')).toEqual(1);
    });
  });
});
