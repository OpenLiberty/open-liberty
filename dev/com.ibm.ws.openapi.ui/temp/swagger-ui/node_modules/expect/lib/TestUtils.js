'use strict';

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.stringContains = exports.objectContains = exports.arrayContains = exports.functionThrows = exports.isA = exports.isObject = exports.isArray = exports.isFunction = exports.isEqual = exports.whyNotEqual = undefined;

var _typeof = typeof Symbol === "function" && typeof Symbol.iterator === "symbol" ? function (obj) { return typeof obj; } : function (obj) { return obj && typeof Symbol === "function" && obj.constructor === Symbol ? "symbol" : typeof obj; };

var _isRegex = require('is-regex');

var _isRegex2 = _interopRequireDefault(_isRegex);

var _why = require('is-equal/why');

var _why2 = _interopRequireDefault(_why);

var _objectKeys = require('object-keys');

var _objectKeys2 = _interopRequireDefault(_objectKeys);

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

/**
 * Returns the reason why the given arguments are not *conceptually*
 * equal, if any; the empty string otherwise.
 */
var whyNotEqual = exports.whyNotEqual = function whyNotEqual(a, b) {
  return a == b ? '' : (0, _why2.default)(a, b);
};

/**
 * Returns true if the given arguments are *conceptually* equal.
 */
var isEqual = exports.isEqual = function isEqual(a, b) {
  return whyNotEqual(a, b) === '';
};

/**
 * Returns true if the given object is a function.
 */
var isFunction = exports.isFunction = function isFunction(object) {
  return typeof object === 'function';
};

/**
 * Returns true if the given object is an array.
 */
var isArray = exports.isArray = function isArray(object) {
  return Array.isArray(object);
};

/**
 * Returns true if the given object is an object.
 */
var isObject = exports.isObject = function isObject(object) {
  return object && !isArray(object) && (typeof object === 'undefined' ? 'undefined' : _typeof(object)) === 'object';
};

/**
 * Returns true if the given object is an instanceof value
 * or its typeof is the given value.
 */
var isA = exports.isA = function isA(object, value) {
  if (isFunction(value)) return object instanceof value;

  if (value === 'array') return Array.isArray(object);

  return (typeof object === 'undefined' ? 'undefined' : _typeof(object)) === value;
};

/**
 * Returns true if the given function throws the given value
 * when invoked. The value may be:
 *
 * - undefined, to merely assert there was a throw
 * - a constructor function, for comparing using instanceof
 * - a regular expression, to compare with the error message
 * - a string, to find in the error message
 */
var functionThrows = exports.functionThrows = function functionThrows(fn, context, args, value) {
  try {
    fn.apply(context, args);
  } catch (error) {
    if (value == null) return true;

    if (isFunction(value) && error instanceof value) return true;

    var message = error.message || error;

    if (typeof message === 'string') {
      if ((0, _isRegex2.default)(value) && value.test(error.message)) return true;

      if (typeof value === 'string' && message.indexOf(value) !== -1) return true;
    }
  }

  return false;
};

/**
 * Returns true if the given array contains the value, false
 * otherwise. The compareValues function must return false to
 * indicate a non-match.
 */
var arrayContains = exports.arrayContains = function arrayContains(array, value, compareValues) {
  return array.some(function (item) {
    return compareValues(item, value) !== false;
  });
};

var ownEnumerableKeys = function ownEnumerableKeys(object) {
  if ((typeof Reflect === 'undefined' ? 'undefined' : _typeof(Reflect)) === 'object' && typeof Reflect.ownKeys === 'function') {
    return Reflect.ownKeys(object).filter(function (key) {
      return Object.getOwnPropertyDescriptor(object, key).enumerable;
    });
  }

  if (typeof Object.getOwnPropertySymbols === 'function') {
    return Object.getOwnPropertySymbols(object).filter(function (key) {
      return Object.getOwnPropertyDescriptor(object, key).enumerable;
    }).concat((0, _objectKeys2.default)(object));
  }

  return (0, _objectKeys2.default)(object);
};

/**
 * Returns true if the given object contains the value, false
 * otherwise. The compareValues function must return false to
 * indicate a non-match.
 */
var objectContains = exports.objectContains = function objectContains(object, value, compareValues) {
  return ownEnumerableKeys(value).every(function (k) {
    if (isObject(object[k]) && isObject(value[k])) return objectContains(object[k], value[k], compareValues);

    return compareValues(object[k], value[k]);
  });
};

/**
 * Returns true if the given string contains the value, false otherwise.
 */
var stringContains = exports.stringContains = function stringContains(string, value) {
  return string.indexOf(value) !== -1;
};