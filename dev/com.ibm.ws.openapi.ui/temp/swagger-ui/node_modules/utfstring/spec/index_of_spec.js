var UtfString = require('../utfstring.js');

describe('UtfString', function() {
  describe('#indexOf', function() {
    it('works with standard ASCII characters', function() {
      var str = 'abc';
      expect(UtfString.indexOf(str, 'a')).toEqual(0);
      expect(UtfString.indexOf(str, 'b')).toEqual(1);
      expect(UtfString.indexOf(str, 'c')).toEqual(2);
    });

    it('works with multi-byte characters', function() {
      var str = 'ありがとう';
      expect(UtfString.indexOf(str, 'あ')).toEqual(0);
      expect(UtfString.indexOf(str, 'り')).toEqual(1);
      expect(UtfString.indexOf(str, 'が')).toEqual(2);
      expect(UtfString.indexOf(str, 'と')).toEqual(3);
      expect(UtfString.indexOf(str, 'う')).toEqual(4);
    });

    it('works with astral plane unicode characters', function() {
      var str = '𤔣𤔤𤔥𤔦';

      expect(str.indexOf('𤔣')).toEqual(0);
      expect(UtfString.indexOf(str, '𤔣')).toEqual(0);

      expect(str.indexOf('𤔤')).toEqual(2);
      expect(UtfString.indexOf(str, '𤔤')).toEqual(1);

      expect(str.indexOf('𤔥')).toEqual(4);
      expect(UtfString.indexOf(str, '𤔥')).toEqual(2);

      expect(str.indexOf('𤔦')).toEqual(6);
      expect(UtfString.indexOf(str, '𤔦')).toEqual(3);
    });

    it('works with mixed characters', function() {
      var str = 'あaりbがc𤔣dとeうf';
      expect(UtfString.indexOf(str, 'a')).toEqual(1);
      expect(UtfString.indexOf(str, 'が')).toEqual(4);
      expect(UtfString.indexOf(str, '𤔣')).toEqual(6);
      expect(UtfString.indexOf(str, 'e')).toEqual(9);
    });

    it('returns -1 if search value is not found', function() {
      expect(UtfString.indexOf('abc', 'd')).toEqual(-1);
    });

    it('respects the start parameter', function() {
      expect(UtfString.indexOf('abcabc', 'b', 2)).toEqual(4);
      expect(UtfString.indexOf('ありがとうり', 'り', 2)).toEqual(5);
      expect(UtfString.indexOf('𤔣𤔤𤔥𤔤𤔦', '𤔤', 2)).toEqual(3);
    });
  });
});
