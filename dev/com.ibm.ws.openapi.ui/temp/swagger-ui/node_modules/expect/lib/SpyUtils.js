'use strict';

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.spyOn = exports.createSpy = exports.restoreSpies = exports.isSpy = undefined;

var _defineProperties = require('define-properties');

var _assert = require('./assert');

var _assert2 = _interopRequireDefault(_assert);

var _TestUtils = require('./TestUtils');

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

function _toConsumableArray(arr) { if (Array.isArray(arr)) { for (var i = 0, arr2 = Array(arr.length); i < arr.length; i++) { arr2[i] = arr[i]; } return arr2; } else { return Array.from(arr); } } /*eslint-disable prefer-rest-params, no-underscore-dangle*/


var noop = function noop() {};

var supportsConfigurableFnLength = _defineProperties.supportsDescriptors && Object.getOwnPropertyDescriptor(function () {}, 'length').configurable;

var isSpy = exports.isSpy = function isSpy(object) {
  return object && object.__isSpy === true;
};

var spies = [];

var restoreSpies = exports.restoreSpies = function restoreSpies() {
  for (var i = spies.length - 1; i >= 0; i--) {
    spies[i].restore();
  }spies = [];
};

var createSpy = exports.createSpy = function createSpy(fn) {
  var restore = arguments.length <= 1 || arguments[1] === undefined ? noop : arguments[1];

  if (fn == null) fn = noop;

  (0, _assert2.default)((0, _TestUtils.isFunction)(fn), 'createSpy needs a function');

  var targetFn = void 0,
      thrownValue = void 0,
      returnValue = void 0,
      spy = void 0;

  function spyLogic() {
    spy.calls.push({
      context: this,
      arguments: Array.prototype.slice.call(arguments, 0)
    });

    if (targetFn) return targetFn.apply(this, arguments);

    if (thrownValue) throw thrownValue;

    return returnValue;
  }

  if (supportsConfigurableFnLength) {
    spy = Object.defineProperty(spyLogic, 'length', { value: fn.length, writable: false, enumerable: false, configurable: true });
  } else {
    spy = new Function('spy', 'return function(' + // eslint-disable-line no-new-func
    [].concat(_toConsumableArray(Array(fn.length))).map(function (_, i) {
      return '_' + i;
    }).join(',') + ') {\n      return spy.apply(this, arguments)\n    }')(spyLogic);
  }

  spy.calls = [];

  spy.andCall = function (otherFn) {
    targetFn = otherFn;
    return spy;
  };

  spy.andCallThrough = function () {
    return spy.andCall(fn);
  };

  spy.andThrow = function (value) {
    thrownValue = value;
    return spy;
  };

  spy.andReturn = function (value) {
    returnValue = value;
    return spy;
  };

  spy.getLastCall = function () {
    return spy.calls[spy.calls.length - 1];
  };

  spy.reset = function () {
    spy.calls = [];
  };

  spy.restore = spy.destroy = restore;

  spy.__isSpy = true;

  spies.push(spy);

  return spy;
};

var spyOn = exports.spyOn = function spyOn(object, methodName) {
  var original = object[methodName];

  if (!isSpy(original)) {
    (0, _assert2.default)((0, _TestUtils.isFunction)(original), 'Cannot spyOn the %s property; it is not a function', methodName);

    object[methodName] = createSpy(original, function () {
      object[methodName] = original;
    });
  }

  return object[methodName];
};