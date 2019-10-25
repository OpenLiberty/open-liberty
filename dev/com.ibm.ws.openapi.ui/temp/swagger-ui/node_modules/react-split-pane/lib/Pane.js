'use strict';

Object.defineProperty(exports, "__esModule", {
  value: true
});

var _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; };

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

var Pane = function (_React$Component) {
  _inherits(Pane, _React$Component);

  function Pane(props) {
    _classCallCheck(this, Pane);

    var _this = _possibleConstructorReturn(this, (Pane.__proto__ || Object.getPrototypeOf(Pane)).call(this, props));

    _this.state = { size: _this.props.size };
    return _this;
  }

  _createClass(Pane, [{
    key: 'render',
    value: function render() {
      var _props = this.props,
          children = _props.children,
          className = _props.className,
          prefixer = _props.prefixer,
          split = _props.split,
          styleProps = _props.style;
      var size = this.state.size;

      var classes = ['Pane', split, className];

      var style = _extends({}, styleProps || {}, {
        flex: 1,
        position: 'relative',
        outline: 'none'
      });

      if (size !== undefined) {
        if (split === 'vertical') {
          style.width = size;
        } else {
          style.height = size;
          style.display = 'flex';
        }
        style.flex = 'none';
      }

      return _react2.default.createElement(
        'div',
        { className: classes.join(' '), style: prefixer.prefix(style) },
        children
      );
    }
  }]);

  return Pane;
}(_react2.default.Component);

Pane.propTypes = {
  className: _propTypes2.default.string.isRequired,
  children: _propTypes2.default.node.isRequired,
  prefixer: _propTypes2.default.instanceOf(_inlineStylePrefixer2.default).isRequired,
  size: _propTypes2.default.oneOfType([_propTypes2.default.string, _propTypes2.default.number]),
  split: _propTypes2.default.oneOf(['vertical', 'horizontal']),
  style: _reactStyleProptype2.default
};

Pane.defaultProps = {
  prefixer: new _inlineStylePrefixer2.default({ userAgent: USER_AGENT })
};

exports.default = Pane;
module.exports = exports['default'];