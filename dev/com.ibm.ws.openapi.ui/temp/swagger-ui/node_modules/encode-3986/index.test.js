var test = require('tape')
var encode = require('./')

test('encode', function (t) {
  t.equal(decodeURIComponent('I%20am%20a%20T-Rex%21'), 'I am a T-Rex!', 'sanity check')
  t.equal(encode('I am a T-Rex!'), 'I%20am%20a%20T-Rex%21')
  t.equal(encode("!'()*"), '%21%27%28%29%2A')
  t.equal(encode("((-!-))"), '%28%28-%21-%29%29')
  ;(function () {
    var input = ',/?:@&=+$#'
    t.equal(encode(input), encodeURIComponent(input))
  })()
  t.end()
})
