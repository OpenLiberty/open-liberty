(function webpackUniversalModuleDefinition(root, factory) {
	if(typeof exports === 'object' && typeof module === 'object')
		module.exports = factory(require("react"), require("react-motion"));
	else if(typeof define === 'function' && define.amd)
		define(["react", "react-motion"], factory);
	else if(typeof exports === 'object')
		exports["ReactCollapse"] = factory(require("react"), require("react-motion"));
	else
		root["ReactCollapse"] = factory(root["React"], root["ReactMotion"]);
})(this, function(__WEBPACK_EXTERNAL_MODULE_1__, __WEBPACK_EXTERNAL_MODULE_3__) {
return /******/ (function(modules) { // webpackBootstrap
/******/ 	// The module cache
/******/ 	var installedModules = {};
/******/
/******/ 	// The require function
/******/ 	function __webpack_require__(moduleId) {
/******/
/******/ 		// Check if module is in cache
/******/ 		if(installedModules[moduleId]) {
/******/ 			return installedModules[moduleId].exports;
/******/ 		}
/******/ 		// Create a new module (and put it into the cache)
/******/ 		var module = installedModules[moduleId] = {
/******/ 			i: moduleId,
/******/ 			l: false,
/******/ 			exports: {}
/******/ 		};
/******/
/******/ 		// Execute the module function
/******/ 		modules[moduleId].call(module.exports, module, module.exports, __webpack_require__);
/******/
/******/ 		// Flag the module as loaded
/******/ 		module.l = true;
/******/
/******/ 		// Return the exports of the module
/******/ 		return module.exports;
/******/ 	}
/******/
/******/
/******/ 	// expose the modules object (__webpack_modules__)
/******/ 	__webpack_require__.m = modules;
/******/
/******/ 	// expose the module cache
/******/ 	__webpack_require__.c = installedModules;
/******/
/******/ 	// define getter function for harmony exports
/******/ 	__webpack_require__.d = function(exports, name, getter) {
/******/ 		if(!__webpack_require__.o(exports, name)) {
/******/ 			Object.defineProperty(exports, name, {
/******/ 				configurable: false,
/******/ 				enumerable: true,
/******/ 				get: getter
/******/ 			});
/******/ 		}
/******/ 	};
/******/
/******/ 	// getDefaultExport function for compatibility with non-harmony modules
/******/ 	__webpack_require__.n = function(module) {
/******/ 		var getter = module && module.__esModule ?
/******/ 			function getDefault() { return module['default']; } :
/******/ 			function getModuleExports() { return module; };
/******/ 		__webpack_require__.d(getter, 'a', getter);
/******/ 		return getter;
/******/ 	};
/******/
/******/ 	// Object.prototype.hasOwnProperty.call
/******/ 	__webpack_require__.o = function(object, property) { return Object.prototype.hasOwnProperty.call(object, property); };
/******/
/******/ 	// __webpack_public_path__
/******/ 	__webpack_require__.p = "";
/******/
/******/ 	// Load entry module and return exports
/******/ 	return __webpack_require__(__webpack_require__.s = 2);
/******/ })
/************************************************************************/
/******/ ([
/* 0 */
/***/ (function(module, exports, __webpack_require__) {

"use strict";


Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.Collapse = undefined;

var _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; };

var _createClass = function () { function defineProperties(target, props) { for (var i = 0; i < props.length; i++) { var descriptor = props[i]; descriptor.enumerable = descriptor.enumerable || false; descriptor.configurable = true; if ("value" in descriptor) descriptor.writable = true; Object.defineProperty(target, descriptor.key, descriptor); } } return function (Constructor, protoProps, staticProps) { if (protoProps) defineProperties(Constructor.prototype, protoProps); if (staticProps) defineProperties(Constructor, staticProps); return Constructor; }; }();

var _react = __webpack_require__(1);

var _react2 = _interopRequireDefault(_react);

var _reactMotion = __webpack_require__(3);

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

function _objectWithoutProperties(obj, keys) { var target = {}; for (var i in obj) { if (keys.indexOf(i) >= 0) continue; if (!Object.prototype.hasOwnProperty.call(obj, i)) continue; target[i] = obj[i]; } return target; }

function _classCallCheck(instance, Constructor) { if (!(instance instanceof Constructor)) { throw new TypeError("Cannot call a class as a function"); } }

function _possibleConstructorReturn(self, call) { if (!self) { throw new ReferenceError("this hasn't been initialised - super() hasn't been called"); } return call && (typeof call === "object" || typeof call === "function") ? call : self; }

function _inherits(subClass, superClass) { if (typeof superClass !== "function" && superClass !== null) { throw new TypeError("Super expression must either be null or a function, not " + typeof superClass); } subClass.prototype = Object.create(superClass && superClass.prototype, { constructor: { value: subClass, enumerable: false, writable: true, configurable: true } }); if (superClass) Object.setPrototypeOf ? Object.setPrototypeOf(subClass, superClass) : subClass.__proto__ = superClass; }

var SPRING_PRECISION = 1;

var WAITING = 'WAITING';
var RESIZING = 'RESIZING';
var RESTING = 'RESTING';
var IDLING = 'IDLING';

var noop = function noop() {
  return null;
};
var css = {
  collapse: 'ReactCollapse--collapse',
  content: 'ReactCollapse--content'
};

var Collapse = exports.Collapse = function (_React$PureComponent) {
  _inherits(Collapse, _React$PureComponent);

  function Collapse(props) {
    _classCallCheck(this, Collapse);

    var _this = _possibleConstructorReturn(this, (Collapse.__proto__ || Object.getPrototypeOf(Collapse)).call(this, props));

    _initialiseProps.call(_this);

    _this.state = {
      currentState: IDLING,
      from: 0,
      to: 0
    };
    return _this;
  }

  _createClass(Collapse, [{
    key: 'componentDidMount',
    value: function componentDidMount() {
      var _props = this.props,
          isOpened = _props.isOpened,
          forceInitialAnimation = _props.forceInitialAnimation,
          onRest = _props.onRest;

      if (isOpened) {
        var to = this.getTo();
        if (forceInitialAnimation) {
          var from = this.wrapper.clientHeight;
          this.setState({ currentState: RESIZING, from: from, to: to });
        } else {
          this.setState({ currentState: IDLING, from: to, to: to });
        }
      }
      onRest();
    }
  }, {
    key: 'componentWillReceiveProps',
    value: function componentWillReceiveProps(nextProps) {
      if (nextProps.hasNestedCollapse) {
        // For nested collapses we do not need to change to waiting state
        // and should keep `height:auto`
        // Because children will be animated and height will not jump anyway
        // See https://github.com/nkbt/react-collapse/issues/76 for more details
        if (nextProps.isOpened !== this.props.isOpened) {
          // Still go to WAITING state if own isOpened was changed
          this.setState({ currentState: WAITING });
        }
      } else if (this.state.currentState === IDLING && (nextProps.isOpened || this.props.isOpened)) {
        this.setState({ currentState: WAITING });
      }
    }
  }, {
    key: 'componentDidUpdate',
    value: function componentDidUpdate(_, prevState) {
      var _props2 = this.props,
          isOpened = _props2.isOpened,
          onRest = _props2.onRest,
          onMeasure = _props2.onMeasure;


      if (this.state.currentState === IDLING) {
        onRest();
        return;
      }

      if (prevState.to !== this.state.to) {
        onMeasure({ height: this.state.to, width: this.content.clientWidth });
      }

      var from = this.wrapper.clientHeight;
      var to = isOpened ? this.getTo() : 0;

      if (from !== to) {
        this.setState({ currentState: RESIZING, from: from, to: to });
        return;
      }

      if (this.state.currentState === RESTING || this.state.currentState === WAITING) {
        this.setState({ currentState: IDLING, from: from, to: to });
      }
    }
  }, {
    key: 'componentWillUnmount',
    value: function componentWillUnmount() {
      cancelAnimationFrame(this.raf);
    }
  }, {
    key: 'render',
    value: function render() {
      return _react2.default.createElement(_reactMotion.Motion, _extends({}, this.getMotionProps(), {
        onRest: this.onRest,
        children: this.renderContent }));
    }
  }]);

  return Collapse;
}(_react2.default.PureComponent);

Collapse.defaultProps = {
  forceInitialAnimation: false,
  hasNestedCollapse: false,
  fixedHeight: -1,
  style: {},
  theme: css,
  onRender: noop,
  onRest: noop,
  onMeasure: noop
};

var _initialiseProps = function _initialiseProps() {
  var _this2 = this;

  this.onContentRef = function (content) {
    _this2.content = content;
  };

  this.onWrapperRef = function (wrapper) {
    _this2.wrapper = wrapper;
  };

  this.onRest = function () {
    _this2.raf = requestAnimationFrame(_this2.setResting);
  };

  this.setResting = function () {
    _this2.setState({ currentState: RESTING });
  };

  this.getTo = function () {
    var fixedHeight = _this2.props.fixedHeight;

    return fixedHeight > -1 ? fixedHeight : _this2.content.clientHeight;
  };

  this.getWrapperStyle = function (height) {
    if (_this2.state.currentState === IDLING && _this2.state.to) {
      var fixedHeight = _this2.props.fixedHeight;

      if (fixedHeight > -1) {
        return { overflow: 'hidden', height: fixedHeight };
      }
      return { height: 'auto' };
    }

    if (_this2.state.currentState === WAITING && !_this2.state.to) {
      return { overflow: 'hidden', height: 0 };
    }

    return { overflow: 'hidden', height: Math.max(0, height) };
  };

  this.getMotionProps = function () {
    var springConfig = _this2.props.springConfig;


    return _this2.state.currentState === IDLING ? {
      // When completely stable, instantly jump to the position
      defaultStyle: { height: _this2.state.to },
      style: { height: _this2.state.to }
    } : {
      // Otherwise, animate
      defaultStyle: { height: _this2.state.from },
      style: { height: (0, _reactMotion.spring)(_this2.state.to, _extends({ precision: SPRING_PRECISION }, springConfig)) }
    };
  };

  this.renderContent = function (_ref) {
    var height = _ref.height;

    // eslint-disable-line
    var _props3 = _this2.props,
        _isOpened = _props3.isOpened,
        _springConfig = _props3.springConfig,
        _forceInitialAnimation = _props3.forceInitialAnimation,
        _hasNestedCollapse = _props3.hasNestedCollapse,
        _fixedHeight = _props3.fixedHeight,
        theme = _props3.theme,
        style = _props3.style,
        onRender = _props3.onRender,
        _onRest = _props3.onRest,
        _onMeasure = _props3.onMeasure,
        children = _props3.children,
        props = _objectWithoutProperties(_props3, ['isOpened', 'springConfig', 'forceInitialAnimation', 'hasNestedCollapse', 'fixedHeight', 'theme', 'style', 'onRender', 'onRest', 'onMeasure', 'children']);

    var _state = _this2.state,
        from = _state.from,
        to = _state.to;

    // DANGEROUS, use with caution, never do setState with it

    onRender({ current: height, from: from, to: to });

    return _react2.default.createElement(
      'div',
      _extends({
        ref: _this2.onWrapperRef,
        className: theme.collapse,
        style: _extends({}, _this2.getWrapperStyle(Math.max(0, height)), style)
      }, props),
      _react2.default.createElement(
        'div',
        { ref: _this2.onContentRef, className: theme.content },
        children
      )
    );
  };
};

/***/ }),
/* 1 */
/***/ (function(module, exports) {

module.exports = __WEBPACK_EXTERNAL_MODULE_1__;

/***/ }),
/* 2 */
/***/ (function(module, exports, __webpack_require__) {

"use strict";


var _require = __webpack_require__(0),
    Collapse = _require.Collapse;

var _require2 = __webpack_require__(4),
    UnmountClosed = _require2.UnmountClosed;

UnmountClosed.Collapse = Collapse;
UnmountClosed.UnmountClosed = UnmountClosed;

module.exports = UnmountClosed;

/***/ }),
/* 3 */
/***/ (function(module, exports) {

module.exports = __WEBPACK_EXTERNAL_MODULE_3__;

/***/ }),
/* 4 */
/***/ (function(module, exports, __webpack_require__) {

"use strict";


Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.UnmountClosed = undefined;

var _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; };

var _createClass = function () { function defineProperties(target, props) { for (var i = 0; i < props.length; i++) { var descriptor = props[i]; descriptor.enumerable = descriptor.enumerable || false; descriptor.configurable = true; if ("value" in descriptor) descriptor.writable = true; Object.defineProperty(target, descriptor.key, descriptor); } } return function (Constructor, protoProps, staticProps) { if (protoProps) defineProperties(Constructor.prototype, protoProps); if (staticProps) defineProperties(Constructor, staticProps); return Constructor; }; }();

var _react = __webpack_require__(1);

var _react2 = _interopRequireDefault(_react);

var _Collapse = __webpack_require__(0);

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

/***/ })
/******/ ]);
});