var UtfString = require('../../utfstring.js');

describe('UtfString visual', function() {
  describe('#charAt', function() {
    it('works with regional indicators', function() {
      var str = '🇸🇴🇫🇷';
      expect(UtfString.visual.charAt(str, 0)).toEqual('🇸🇴');
      expect(UtfString.visual.charAt(str, 1)).toEqual('🇫🇷');
    });
  });
});
