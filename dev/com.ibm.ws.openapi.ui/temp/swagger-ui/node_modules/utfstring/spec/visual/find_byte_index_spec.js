var UtfString = require('../../utfstring.js');

describe('UtfString visual', function() {
  describe('#findByteIndex', function() {
    it('works with regional indicators', function() {
      var str = '🇸🇴🇫🇷';
      expect(UtfString.visual.findByteIndex(str, 0)).toEqual(0);
      expect(UtfString.visual.findByteIndex(str, 1)).toEqual(4);
      expect(UtfString.visual.findByteIndex(str, 2)).toEqual(-1);
    });
  });
});
