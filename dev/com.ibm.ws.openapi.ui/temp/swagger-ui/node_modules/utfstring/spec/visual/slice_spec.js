var UtfString = require('../../utfstring.js');

describe('UtfString visual', function() {
  describe('#slice', function() {
    describe('with regional indicators', function() {
      var str = '🇸🇴🇫🇷';

      it('works when given start and end indices', function() {
        expect(UtfString.visual.slice(str, 0, 1)).toEqual('🇸🇴');
        expect(UtfString.visual.slice(str, 1, 2)).toEqual('🇫🇷');
      });

      it('works when not given an end index', function() {
        expect(UtfString.visual.slice(str, 0)).toEqual('🇸🇴🇫🇷');
        expect(UtfString.visual.slice(str, 1)).toEqual('🇫🇷');
      });

      it('returns an empty string when given out-of-bounds indices', function() {
        expect(UtfString.visual.slice(str, 4)).toEqual('');
        expect(UtfString.visual.slice(str, 4, 5)).toEqual('');
      });
    });
  });
});
