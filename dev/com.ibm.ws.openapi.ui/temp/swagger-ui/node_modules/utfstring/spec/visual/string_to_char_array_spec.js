var UtfString = require('../../utfstring.js');

describe('UtfString visual', function() {
  describe('#stringToCharArray', function() {
    it('works with regional indicators', function() {
      var str = '🇸🇴🇫🇷';
      expect(UtfString.visual.stringToCharArray(str)).toEqual(['🇸🇴', '🇫🇷']);
    });
  });
});
