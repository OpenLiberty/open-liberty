'use strict';

Object.defineProperty(exports, "__esModule", {
  value: true
});

var _objectInspect = require('object-inspect');

var _objectInspect2 = _interopRequireDefault(_objectInspect);

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

var formatString = function formatString(string, args) {
  var index = 0;
  return string.replace(/%s/g, function () {
    return (0, _objectInspect2.default)(args[index++]);
  });
};

var assert = function assert(condition, createMessage) {
  for (var _len = arguments.length, extraArgs = Array(_len > 2 ? _len - 2 : 0), _key = 2; _key < _len; _key++) {
    extraArgs[_key - 2] = arguments[_key];
  }

  if (condition) return;

  var message = typeof createMessage === 'string' ? formatString(createMessage, extraArgs) : createMessage(extraArgs);

  throw new Error(message);
};

exports.default = assert;