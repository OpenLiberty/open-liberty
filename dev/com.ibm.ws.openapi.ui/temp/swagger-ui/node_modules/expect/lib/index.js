'use strict';

var _Expectation = require('./Expectation');

var _Expectation2 = _interopRequireDefault(_Expectation);

var _SpyUtils = require('./SpyUtils');

var _assert = require('./assert');

var _assert2 = _interopRequireDefault(_assert);

var _extend = require('./extend');

var _extend2 = _interopRequireDefault(_extend);

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

function expect(actual) {
  return new _Expectation2.default(actual);
}

expect.createSpy = _SpyUtils.createSpy;
expect.spyOn = _SpyUtils.spyOn;
expect.isSpy = _SpyUtils.isSpy;
expect.restoreSpies = _SpyUtils.restoreSpies;
expect.assert = _assert2.default;
expect.extend = _extend2.default;

module.exports = expect;