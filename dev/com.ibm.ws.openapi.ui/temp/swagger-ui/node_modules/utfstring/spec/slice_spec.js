var UtfString = require('../utfstring.js');

describe('UtfString', function() {
  describe('#slice', function() {
    describe('with standard ASCII characters', function() {
      var str = 'abc';

      it('works when given start and end indices', function() {
        expect(UtfString.slice(str, 0, 1)).toEqual('a');
        expect(UtfString.slice(str, 1, 2)).toEqual('b');
        expect(UtfString.slice(str, 2, 3)).toEqual('c');

        expect(UtfString.slice(str, 1, 3)).toEqual('bc');
        expect(UtfString.slice(str, 0, 3)).toEqual('abc');
      });

      it('works when not given an end index', function() {
        expect(UtfString.slice(str, 0)).toEqual('abc');
        expect(UtfString.slice(str, 1)).toEqual('bc');
        expect(UtfString.slice(str, 2)).toEqual('c');
      });

      it('returns an empty string when given out-of-bounds indices', function() {
        expect(UtfString.slice(str, 3)).toEqual('');
        expect(UtfString.slice(str, 3, 4)).toEqual('');
      });
    });

    describe('with multi-byte characters', function() {
      var str = 'ありがとう';

      it('works when given start and end indices', function() {
        expect(UtfString.slice(str, 0, 1)).toEqual('あ');
        expect(UtfString.slice(str, 1, 2)).toEqual('り');
        expect(UtfString.slice(str, 2, 3)).toEqual('が');
        expect(UtfString.slice(str, 3, 4)).toEqual('と');
        expect(UtfString.slice(str, 4, 5)).toEqual('う');

        expect(UtfString.slice(str, 0, 3)).toEqual('ありが');
        expect(UtfString.slice(str, 1, 3)).toEqual('りが');
      });

      it('works when not given an end index', function() {
        expect(UtfString.slice(str, 0)).toEqual('ありがとう');
        expect(UtfString.slice(str, 1)).toEqual('りがとう');
        expect(UtfString.slice(str, 2)).toEqual('がとう');
        expect(UtfString.slice(str, 3)).toEqual('とう');
        expect(UtfString.slice(str, 4)).toEqual('う');
      });

      it('returns an empty string when given out-of-bounds indices', function() {
        expect(UtfString.slice(str, 5)).toEqual('');
        expect(UtfString.slice(str, 5, 6)).toEqual('');
      });
    });

    describe('with astral plane unicode characters', function() {
      var str = '𤔣𤔤𤔥𤔦';

      it('works when given start and end indices', function() {
        expect(UtfString.slice(str, 0, 1)).toEqual('𤔣');
        expect(UtfString.slice(str, 1, 2)).toEqual('𤔤');
        expect(UtfString.slice(str, 2, 3)).toEqual('𤔥');
        expect(UtfString.slice(str, 3, 4)).toEqual('𤔦');

        expect(UtfString.slice(str, 1, 3)).toEqual('𤔤𤔥');
        expect(UtfString.slice(str, 0, 4)).toEqual('𤔣𤔤𤔥𤔦');
      });

      it('works when not given an end index', function() {
        expect(UtfString.slice(str, 0)).toEqual('𤔣𤔤𤔥𤔦');
        expect(UtfString.slice(str, 1)).toEqual('𤔤𤔥𤔦');
        expect(UtfString.slice(str, 2)).toEqual('𤔥𤔦');
        expect(UtfString.slice(str, 3)).toEqual('𤔦');
      });

      it('returns an empty string when given out-of-bounds indices', function() {
        expect(UtfString.slice(str, 4)).toEqual('');
        expect(UtfString.slice(str, 4, 5)).toEqual('');
      });
    });
  });
});
