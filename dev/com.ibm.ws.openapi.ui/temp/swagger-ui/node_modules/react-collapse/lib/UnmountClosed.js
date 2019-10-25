'use strict';

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.UnmountClosed = undefined;

var _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; };

var _createClass = function () { function defineProperties(target, props) { for (var i = 0; i < props.length; i++) { var descriptor = props[i]; descriptor.enumerable = descriptor.enumerable || false; descriptor.configurable = true; if ("value" in descriptor) descriptor.writable = true; Object.defineProperty(target, descriptor.key, descriptor); } } return function (Constructor, protoProps, staticProps) { if (protoProps) defineProperties(Constructor.prototype, protoProps); if (staticProps) defineProperties(Constructor, staticProps); return Constructor; }; }();

var _react = require('react');

var _react2 = _interopRequireDefault(_react);

var _propTypes = require('prop-types');

var _propTypes2 = _interopRequireDefault(_propTypes);

var _Collapse = require('./Collapse');

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

function _objectWithoutProperties(obj, keys) { var target = {}; for (var i in obj) { if (keys.indexOf(i) >= 0) continue; if (!Object.prototype.hasOwnProperty.call(obj, i)) continue; target[i] = obj[i]; } return target; }

function _classCallCheck(instance, Constructor) { if (!(instance instanceof Constructor)) { throw new TypeError("Cannot call a class as a function"); } }

function _possibleConstructorReturn(self, call) { if (!self) { throw new ReferenceError("this hasn't been initialised - super() hasn't been called"); } return call && (typeof call === "object" || typeof call === "function") ? call : self; }

function _inherits(subClass, superClass) { if (typeof superClass !== "function" && superClass !== null) { throw new TypeError("Super expression must either be null or a function, not " + typeof superClass); } subClass.prototype = Object.create(superClass && superClass.prototype, { constructor: { value: subClass, enumerable: false, writable: true, configurable: true } }); if (superClass) Object.setPrototypeOf ? Object.setPrototypeOf(subClass, superClass) : subClass.__proto__ = superClass; }

var UnmountClosed = exports.UnmountClosed = function (_React$PureComponent) {
  _inherits(UnmountClosed, _React$PureComponent);

  function UnmountClosed(props) {
    _classCallCheck(this, UnmountClosed);

    var _this = _possibleConstructorReturn(this, (UnmountClosed.__proto__ || Object.getPrototypeOf(UnmountClosed)).call(this, props));

    _this.componentWillReceiveProps = function (_ref) {
      var isOpened = _ref.isOpened;

      if (!_this.props.isOpened && isOpened) {
        _this.setState({
          forceInitialAnimation: true,
          shouldUnmount: false
        });
      }
    };

    _this.onRest = function () {
      var _this$props = _this.props,
          isOpened = _this$props.isOpened,
          onRest = _this$props.onRest;


      if (!isOpened) {
        _this.setState({ shouldUnmount: true });
      }
      if (onRest) {
        onRest.apply(undefined, arguments);
      }
    };

    _this.state = {
      shouldUnmount: !_this.props.isOpened,
      forceInitialAnimation: !_this.props.isOpened
    };
    return _this;
  }

  _createClass(UnmountClosed, [{
    key: 'render',
    value: function render() {
      var _props = this.props,
          isOpened = _props.isOpened,
          _onRest = _props.onRest,
          props = _objectWithoutProperties(_props, ['isOpened', 'onRest']);

      var _state = this.state,
          forceInitialAnimation = _state.forceInitialAnimation,
          shouldUnmount = _state.shouldUnmount;


      return shouldUnmount ? null : _react2.default.createElement(_Collapse.Collapse, _extends({
        forceInitialAnimation: forceInitialAnimation,
        isOpened: isOpened,
        onRest: this.onRest
      }, props));
    }
  }]);

  return UnmountClosed;
}(_react2.default.PureComponent);

UnmountClosed.propTypes = {
  isOpened: _propTypes2.default.bool.isRequired,
  onRest: _propTypes2.default.func
};