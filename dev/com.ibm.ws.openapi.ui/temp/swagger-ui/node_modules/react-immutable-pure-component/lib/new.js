(function (global, factory) {
  typeof exports === 'object' && typeof module !== 'undefined' ? factory(exports, require('react'), require('immutable')) :
  typeof define === 'function' && define.amd ? define(['exports', 'react', 'immutable'], factory) :
  (factory((global.window = {}),global.React,global.Immutable));
}(this, (function (exports,React,immutable) { 'use strict';

  React = React && React.hasOwnProperty('default') ? React['default'] : React;

  function _classCallCheck(instance, Constructor) {
    if (!(instance instanceof Constructor)) {
      throw new TypeError("Cannot call a class as a function");
    }
  }

  function _defineProperties(target, props) {
    for (var i = 0; i < props.length; i++) {
      var descriptor = props[i];
      descriptor.enumerable = descriptor.enumerable || false;
      descriptor.configurable = true;
      if ("value" in descriptor) descriptor.writable = true;
      Object.defineProperty(target, descriptor.key, descriptor);
    }
  }

  function _createClass(Constructor, protoProps, staticProps) {
    if (protoProps) _defineProperties(Constructor.prototype, protoProps);
    if (staticProps) _defineProperties(Constructor, staticProps);
    return Constructor;
  }

  function _defineProperty(obj, key, value) {
    if (key in obj) {
      Object.defineProperty(obj, key, {
        value: value,
        enumerable: true,
        configurable: true,
        writable: true
      });
    } else {
      obj[key] = value;
    }

    return obj;
  }

  function _objectSpread(target) {
    for (var i = 1; i < arguments.length; i++) {
      var source = arguments[i] != null ? arguments[i] : {};
      var ownKeys = Object.keys(source);

      if (typeof Object.getOwnPropertySymbols === 'function') {
        ownKeys = ownKeys.concat(Object.getOwnPropertySymbols(source).filter(function (sym) {
          return Object.getOwnPropertyDescriptor(source, sym).enumerable;
        }));
      }

      ownKeys.forEach(function (key) {
        _defineProperty(target, key, source[key]);
      });
    }

    return target;
  }

  function _inherits(subClass, superClass) {
    if (typeof superClass !== "function" && superClass !== null) {
      throw new TypeError("Super expression must either be null or a function");
    }

    subClass.prototype = Object.create(superClass && superClass.prototype, {
      constructor: {
        value: subClass,
        writable: true,
        configurable: true
      }
    });
    if (superClass) _setPrototypeOf(subClass, superClass);
  }

  function _getPrototypeOf(o) {
    _getPrototypeOf = Object.setPrototypeOf ? Object.getPrototypeOf : function _getPrototypeOf(o) {
      return o.__proto__ || Object.getPrototypeOf(o);
    };
    return _getPrototypeOf(o);
  }

  function _setPrototypeOf(o, p) {
    _setPrototypeOf = Object.setPrototypeOf || function _setPrototypeOf(o, p) {
      o.__proto__ = p;
      return o;
    };

    return _setPrototypeOf(o, p);
  }

  function _assertThisInitialized(self) {
    if (self === void 0) {
      throw new ReferenceError("this hasn't been initialised - super() hasn't been called");
    }

    return self;
  }

  function _possibleConstructorReturn(self, call) {
    if (call && (typeof call === "object" || typeof call === "function")) {
      return call;
    }

    return _assertThisInitialized(self);
  }

  var ImmutablePureComponent =
  /*#__PURE__*/
  function (_React$Component) {
    _inherits(ImmutablePureComponent, _React$Component);

    function ImmutablePureComponent() {
      _classCallCheck(this, ImmutablePureComponent);

      return _possibleConstructorReturn(this, _getPrototypeOf(ImmutablePureComponent).apply(this, arguments));
    }

    _createClass(ImmutablePureComponent, [{
      key: "shouldComponentUpdate",
      value: function shouldComponentUpdate(nextProps) {
        var _this = this;

        var nextState = arguments.length > 1 && arguments[1] !== undefined ? arguments[1] : {};
        var state = this.state || {};
        return !(this.updateOnProps || Object.keys(_objectSpread({}, nextProps, this.props))).every(function (p) {
          return immutable.is(nextProps[p], _this.props[p]);
        }) || !(this.updateOnStates || Object.keys(_objectSpread({}, nextState, state))).every(function (s) {
          return immutable.is(nextState[s], state[s]);
        });
      }
    }]);

    return ImmutablePureComponent;
  }(React.Component);

  exports.ImmutablePureComponent = ImmutablePureComponent;
  exports.default = ImmutablePureComponent;

  Object.defineProperty(exports, '__esModule', { value: true });

})));
