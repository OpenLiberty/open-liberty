'use strict';

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.RESIZER_DEFAULT_CLASSNAME = undefined;

var _createClass = function () { function defineProperties(target, props) { for (var i = 0; i < props.length; i++) { var descriptor = props[i]; descriptor.enumerable = descriptor.enumerable || false; descriptor.configurable = true; if ("value" in descriptor) descriptor.writable = true; Object.defineProperty(target, descriptor.key, descriptor); } } return function (Constructor, protoProps, staticProps) { if (protoProps) defineProperties(Constructor.prototype, protoProps); if (staticProps) defineProperties(Constructor, staticProps); return Constructor; }; }();

var _react = require('react');

var _react2 = _interopRequireDefault(_react);

var _propTypes = require('prop-types');

var _propTypes2 = _interopRequireDefault(_propTypes);

var _inlineStylePrefixer = require('inline-style-prefixer');

var _inlineStylePrefixer2 = _interopRequireDefault(_inlineStylePrefixer);

var _reactStyleProptype = require('react-style-proptype');

var _reactStyleProptype2 = _interopRequireDefault(_reactStyleProptype);

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

function _classCallCheck(instance, Constructor) { if (!(instance instanceof Constructor)) { throw new TypeError("Cannot call a class as a function"); } }

function _possibleConstructorReturn(self, call) { if (!self) { throw new ReferenceError("this hasn't been initialised - super() hasn't been called"); } return call && (typeof call === "object" || typeof call === "function") ? call : self; }

function _inherits(subClass, superClass) { if (typeof superClass !== "function" && superClass !== null) { throw new TypeError("Super expression must either be null or a function, not " + typeof superClass); } subClass.prototype = Object.create(superClass && superClass.prototype, { constructor: { value: subClass, enumerable: false, writable: true, configurable: true } }); if (superClass) Object.setPrototypeOf ? Object.setPrototypeOf(subClass, superClass) : subClass.__proto__ = superClass; }

var DEFAULT_USER_AGENT = 'Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.2 (KHTML, like Gecko) Safari/537.2';
var USER_AGENT = typeof navigator !== 'undefined' ? navigator.userAgent : DEFAULT_USER_AGENT;
var RESIZER_DEFAULT_CLASSNAME = exports.RESIZER_DEFAULT_CLASSNAME = 'Resizer';

var Resizer = function (_React$Component) {
  _inherits(Resizer, _React$Component);

  function Resizer() {
    _classCallCheck(this, Resizer);

    return _possibleConstructorReturn(this, (Resizer.__proto__ || Object.getPrototypeOf(Resizer)).apply(this, arguments));
  }

  _createClass(Resizer, [{
    key: 'render',
    value: function render() {
      var _props = this.props,
          className = _props.className,
          _onClick = _props.onClick,
          _onDoubleClick = _props.onDoubleClick,
          _onMouseDown = _props.onMouseDown,
          _onTouchEnd = _props.onTouchEnd,
          _onTouchStart = _props.onTouchStart,
          prefixer = _props.prefixer,
          resizerClassName = _props.resizerClassName,
          split = _props.split,
          style = _props.style;

      var classes = [resizerClassName, split, className];

      return _react2.default.createElement('span', {
        className: classes.join(' '),
        style: prefixer.prefix(style) || {},
        onMouseDown: function onMouseDown(event) {
          return _onMouseDown(event);
        },
        onTouchStart: function onTouchStart(event) {
          event.preventDefault();
          _onTouchStart(event);
        },
        onTouchEnd: function onTouchEnd(event) {
          event.preventDefault();
          _onTouchEnd(event);
        },
        onClick: function onClick(event) {
          if (_onClick) {
            event.preventDefault();
            _onClick(event);
          }
        },
        onDoubleClick: function onDoubleClick(event) {
          if (_onDoubleClick) {
            event.preventDefault();
            _onDoubleClick(event);
          }
        }
      });
    }
  }]);

  return Resizer;
}(_react2.default.Component);

Resizer.propTypes = {
  className: _propTypes2.default.string.isRequired,
  onClick: _propTypes2.default.func,
  onDoubleClick: _propTypes2.default.func,
  onMouseDown: _propTypes2.default.func.isRequired,
  onTouchStart: _propTypes2.default.func.isRequired,
  onTouchEnd: _propTypes2.default.func.isRequired,
  prefixer: _propTypes2.default.instanceOf(_inlineStylePrefixer2.default).isRequired,
  split: _propTypes2.default.oneOf(['vertical', 'horizontal']),
  style: _reactStyleProptype2.default,
  resizerClassName: _propTypes2.default.string.isRequired
};

Resizer.defaultProps = {
  prefixer: new _inlineStylePrefixer2.default({ userAgent: USER_AGENT }),
  resizerClassName: RESIZER_DEFAULT_CLASSNAME
};

exports.default = Resizer;