var UtfString = require('../../utfstring.js');

describe('UtfString visual', function() {
  describe('#substr', function() {
    describe('with regional indicators', function() {
      var str = '🇸🇴🇫🇷';

      it('works when given a start and a length', function() {
        expect(UtfString.visual.substr(str, 0, 1)).toEqual('🇸🇴');
        expect(UtfString.visual.substr(str, 1, 1)).toEqual('🇫🇷');
      });

      it('works when not given a length', function() {
        expect(UtfString.visual.substr(str, 0)).toEqual('🇸🇴🇫🇷');
        expect(UtfString.visual.substr(str, 1)).toEqual('🇫🇷');
      });

      it('returns an empty string if given an out-of-bounds start', function() {
        expect(UtfString.visual.substr(str, 4, 1)).toEqual('');
      });

      it('returns up to the length of the string if given an out-of-bounds length', function() {
        expect(UtfString.visual.substr(str, 1, 10)).toEqual('🇫🇷');
      });

      it('accepts a negative start value', function() {
        expect(UtfString.visual.substr(str, -1, 1)).toEqual('🇫🇷');
        expect(UtfString.visual.substr(str, -2, 1)).toEqual('🇸🇴');
      });

      it('returns an empty string if the negative start value is out-of-bounds', function() {
        expect(UtfString.visual.substr(str, -3, 1)).toEqual('');
      });
    });
  });
});
