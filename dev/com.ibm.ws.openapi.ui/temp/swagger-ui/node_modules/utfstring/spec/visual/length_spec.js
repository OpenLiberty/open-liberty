var UtfString = require('../../utfstring.js');

describe('UtfString visual', function() {
  describe('#length', function() {
    it('correctly counts single regional indicator characters', function() {
      var str = '🇸'
      expect(str.length).toEqual(2);
      expect(UtfString.visual.length(str)).toEqual(1);
    });

    it('correctly counts pairs of regional indicator characters', function() {
      var str = '🇸🇴'
      expect(str.length).toEqual(4);
      expect(UtfString.visual.length(str)).toEqual(1);
    });

    it('correctly counts multiple pairs of regional indicator characters', function() {
      var str = '🇸🇴🇫🇷'
      expect(str.length).toEqual(8);
      expect(UtfString.visual.length(str)).toEqual(2);
    });
  });
});
