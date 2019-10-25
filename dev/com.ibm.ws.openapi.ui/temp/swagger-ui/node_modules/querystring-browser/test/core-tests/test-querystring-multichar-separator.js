'use strict';

var qs = require('../../querystring');

module.exports = function (assert) {
  assert.deepEqual(
    qs.parse('foo=>bar&&bar=>baz', '&&', '=>'),
    {foo: 'bar', bar: 'baz'}
  );

  assert.strictEqual(
    qs.stringify({foo: 'bar', bar: 'baz'}, '&&', '=>'),
    'foo=>bar&&bar=>baz'
  );

  assert.deepEqual(
    qs.parse('foo==>bar, bar==>baz', ', ', '==>'),
    {foo: 'bar', bar: 'baz'}
  );

  assert.strictEqual(
    qs.stringify({foo: 'bar', bar: 'baz'}, ', ', '==>'),
    'foo==>bar, bar==>baz'
  );
}