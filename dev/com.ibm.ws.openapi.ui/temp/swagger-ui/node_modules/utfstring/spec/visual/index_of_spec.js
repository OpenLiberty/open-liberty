var UtfString = require('../../utfstring.js');

describe('UtfString visual', function() {
  describe('#indexOf', function() {
    it('works with regional indicators', function() {
      var str = '🇸🇴🇫🇷';
      expect(UtfString.visual.indexOf(str, '🇸🇴')).toEqual(0);
      expect(UtfString.visual.indexOf(str, '🇫🇷')).toEqual(1);
      expect(UtfString.visual.indexOf(str, '🇸')).toEqual(0);
      expect(UtfString.visual.indexOf(str, '🇴')).toEqual(0);
      expect(UtfString.visual.indexOf(str, '🇫')).toEqual(1);
      expect(UtfString.visual.indexOf(str, '🇷')).toEqual(1);
    });
  });
});
