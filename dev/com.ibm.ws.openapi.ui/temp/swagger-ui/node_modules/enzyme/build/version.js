Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.REACT155 = exports.REACT15 = exports.REACT014 = exports.REACT013 = exports.VERSION = undefined;

var _slicedToArray = function () { function sliceIterator(arr, i) { var _arr = []; var _n = true; var _d = false; var _e = undefined; try { for (var _i = arr[Symbol.iterator](), _s; !(_n = (_s = _i.next()).done); _n = true) { _arr.push(_s.value); if (i && _arr.length === i) break; } } catch (err) { _d = true; _e = err; } finally { try { if (!_n && _i["return"]) _i["return"](); } finally { if (_d) throw _e; } } return _arr; } return function (arr, i) { if (Array.isArray(arr)) { return arr; } else if (Symbol.iterator in Object(arr)) { return sliceIterator(arr, i); } else { throw new TypeError("Invalid attempt to destructure non-iterable instance"); } }; }();

var _react = require('react');

var _react2 = _interopRequireDefault(_react);

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { 'default': obj }; }

var VERSION = exports.VERSION = _react2['default'].version;

var _VERSION$split = VERSION.split('.'),
    _VERSION$split2 = _slicedToArray(_VERSION$split, 2),
    major = _VERSION$split2[0],
    minor = _VERSION$split2[1];

var REACT013 = exports.REACT013 = VERSION.slice(0, 4) === '0.13';
var REACT014 = exports.REACT014 = VERSION.slice(0, 4) === '0.14';
var REACT15 = exports.REACT15 = major === '15';
var REACT155 = exports.REACT155 = REACT15 && minor >= 5;