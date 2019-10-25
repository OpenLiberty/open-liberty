var UtfString = require('../utfstring.js');

describe('UtfString', function() {
  describe('#substr', function() {
    describe('with standard ASCII characters', function() {
      var str = 'abc';

      it('works when given a start and a length', function() {
        expect(UtfString.substr(str, 0, 1)).toEqual('a');
        expect(UtfString.substr(str, 1, 1)).toEqual('b');
        expect(UtfString.substr(str, 2, 1)).toEqual('c');
      });

      it('works when not given a length', function() {
        expect(UtfString.substr(str, 0)).toEqual('abc');
        expect(UtfString.substr(str, 1)).toEqual('bc');
        expect(UtfString.substr(str, 2)).toEqual('c');
      });

      it('returns an empty string if given an out-of-bounds start', function() {
        expect(UtfString.substr(str, 3, 1)).toEqual('');
      });

      it('returns up to the length of the string if given an out-of-bounds length', function() {
        expect(UtfString.substr(str, 2, 10)).toEqual('c');
      });

      it('accepts a negative start value', function() {
        expect(UtfString.substr(str, -1, 1)).toEqual('c');
        expect(UtfString.substr(str, -2, 1)).toEqual('b');
        expect(UtfString.substr(str, -3, 1)).toEqual('a');
      });

      it('returns an empty string if the negative start value is out-of-bounds', function() {
        expect(UtfString.substr(str, -4, 1)).toEqual('');
      });
    });

    describe('with multi-byte characters', function() {
      var str = 'ありがとう';

      it('works when given a start and a length', function() {
        expect(UtfString.substr(str, 0, 1)).toEqual('あ');
        expect(UtfString.substr(str, 1, 1)).toEqual('り');
        expect(UtfString.substr(str, 2, 1)).toEqual('が');
        expect(UtfString.substr(str, 3, 1)).toEqual('と');
        expect(UtfString.substr(str, 4, 1)).toEqual('う');
      });

      it('works when not given a length', function() {
        expect(UtfString.substr(str, 0)).toEqual('ありがとう');
        expect(UtfString.substr(str, 1)).toEqual('りがとう');
        expect(UtfString.substr(str, 2)).toEqual('がとう');
        expect(UtfString.substr(str, 3)).toEqual('とう');
        expect(UtfString.substr(str, 4)).toEqual('う');
      });

      it('returns an empty string if given an out-of-bounds start', function() {
        expect(UtfString.substr(str, 5, 1)).toEqual('');
      });

      it('returns up to the length of the string if given an out-of-bounds length', function() {
        expect(UtfString.substr(str, 2, 10)).toEqual('がとう');
      });

      it('accepts a negative start value', function() {
        expect(UtfString.substr(str, -1, 1)).toEqual('う');
        expect(UtfString.substr(str, -2, 1)).toEqual('と');
        expect(UtfString.substr(str, -3, 1)).toEqual('が');
        expect(UtfString.substr(str, -4, 1)).toEqual('り');
        expect(UtfString.substr(str, -5, 1)).toEqual('あ');
      });

      it('returns an empty string if the negative start value is out-of-bounds', function() {
        expect(UtfString.substr(str, -6, 1)).toEqual('');
      });
    });

    describe('with astral plane unicode characters', function() {
      var str = '𤔣𤔤𤔥𤔦';

      it('works when given a start and a length', function() {
        expect(UtfString.substr(str, 0, 1)).toEqual('𤔣');
        expect(UtfString.substr(str, 1, 1)).toEqual('𤔤');
        expect(UtfString.substr(str, 2, 1)).toEqual('𤔥');
        expect(UtfString.substr(str, 3, 1)).toEqual('𤔦');
      });

      it('works when not given a length', function() {
        expect(UtfString.substr(str, 0)).toEqual('𤔣𤔤𤔥𤔦');
        expect(UtfString.substr(str, 1)).toEqual('𤔤𤔥𤔦');
        expect(UtfString.substr(str, 2)).toEqual('𤔥𤔦');
        expect(UtfString.substr(str, 3)).toEqual('𤔦');
      });

      it('returns an empty string if given an out-of-bounds start', function() {
        expect(UtfString.substr(str, 4, 1)).toEqual('');
      });

      it('returns up to the length of the string if given an out-of-bounds length', function() {
        expect(UtfString.substr(str, 2, 10)).toEqual('𤔥𤔦');
      });

      it('accepts a negative start value', function() {
        expect(UtfString.substr(str, -1, 1)).toEqual('𤔦');
        expect(UtfString.substr(str, -2, 1)).toEqual('𤔥');
        expect(UtfString.substr(str, -3, 1)).toEqual('𤔤');
        expect(UtfString.substr(str, -4, 1)).toEqual('𤔣');
      });

      it('returns an empty string if the negative start value is out-of-bounds', function() {
        expect(UtfString.substr(str, -5, 1)).toEqual('');
      });
    });
  });
});
