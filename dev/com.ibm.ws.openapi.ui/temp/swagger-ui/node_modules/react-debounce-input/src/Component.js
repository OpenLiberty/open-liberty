import React from 'react';
import PropTypes from 'prop-types';
import debounce from 'lodash.debounce';


export class DebounceInput extends React.PureComponent {
  static propTypes = {
    element: PropTypes.oneOfType([PropTypes.string, PropTypes.func]),
    type: PropTypes.string,
    onChange: PropTypes.func.isRequired,
    onKeyDown: PropTypes.func,
    onBlur: PropTypes.func,
    value: PropTypes.oneOfType([
      PropTypes.string,
      PropTypes.number
    ]),
    minLength: PropTypes.number,
    debounceTimeout: PropTypes.number,
    forceNotifyByEnter: PropTypes.bool,
    forceNotifyOnBlur: PropTypes.bool,
    inputRef: PropTypes.func
  };


  static defaultProps = {
    element: 'input',
    type: 'text',
    onKeyDown: undefined,
    onBlur: undefined,
    value: undefined,
    minLength: 0,
    debounceTimeout: 100,
    forceNotifyByEnter: true,
    forceNotifyOnBlur: true,
    inputRef: undefined
  };


  constructor(props) {
    super(props);

    this.isDebouncing = false;
    this.state = {value: props.value || ''};

    const {debounceTimeout} = this.props;
    this.createNotifier(debounceTimeout);
  }


  componentDidUpdate(prevProps) {
    if (this.isDebouncing) {
      return;
    }
    const {value, debounceTimeout} = this.props;

    const {debounceTimeout: oldTimeout, value: oldValue} = prevProps;
    const {value: stateValue} = this.state;

    if (typeof value !== 'undefined' && oldValue !== value && stateValue !== value) {
      // Update state.value if new value passed via props, yep re-render should happen
      // eslint-disable-next-line react/no-did-update-set-state
      this.setState({value});
    }
    if (debounceTimeout !== oldTimeout) {
      this.createNotifier(debounceTimeout);
    }
  }


  componentWillUnmount() {
    if (this.flush) {
      this.flush();
    }
  }


  onChange = event => {
    event.persist();

    const {value: oldValue} = this.state;
    const {minLength} = this.props;

    this.setState({value: event.target.value}, () => {
      const {value} = this.state;

      if (value.length >= minLength) {
        this.notify(event);
        return;
      }

      // If user hits backspace and goes below minLength consider it cleaning the value
      if (oldValue.length > value.length) {
        this.notify({...event, target: {...event.target, value: ''}});
      }
    });
  };


  onKeyDown = event => {
    if (event.key === 'Enter') {
      this.forceNotify(event);
    }
    // Invoke original onKeyDown if present
    const {onKeyDown} = this.props;
    if (onKeyDown) {
      event.persist();
      onKeyDown(event);
    }
  };


  onBlur = event => {
    this.forceNotify(event);
    // Invoke original onBlur if present
    const {onBlur} = this.props;
    if (onBlur) {
      event.persist();
      onBlur(event);
    }
  };


  createNotifier = debounceTimeout => {
    if (debounceTimeout < 0) {
      this.notify = () => null;
    } else if (debounceTimeout === 0) {
      this.notify = this.doNotify;
    } else {
      const debouncedChangeFunc = debounce(event => {
        this.isDebouncing = false;
        this.doNotify(event);
      }, debounceTimeout);

      this.notify = event => {
        this.isDebouncing = true;
        debouncedChangeFunc(event);
      };

      this.flush = () => debouncedChangeFunc.flush();

      this.cancel = () => {
        this.isDebouncing = false;
        debouncedChangeFunc.cancel();
      };
    }
  };


  doNotify = (...args) => {
    const {onChange} = this.props;

    onChange(...args);
  };


  forceNotify = event => {
    const {debounceTimeout} = this.props;
    if (!this.isDebouncing && debounceTimeout > 0) {
      return;
    }

    if (this.cancel) {
      this.cancel();
    }

    const {value} = this.state;
    const {minLength} = this.props;

    if (value.length >= minLength) {
      this.doNotify(event);
    } else {
      this.doNotify({...event, target: {...event.target, value}});
    }
  };


  render() {
    const {
      element,
      onChange: _onChange,
      value: _value,
      minLength: _minLength,
      debounceTimeout: _debounceTimeout,
      forceNotifyByEnter,
      forceNotifyOnBlur,
      onKeyDown,
      onBlur,
      inputRef,
      ...props
    } = this.props;
    const {value} = this.state;

    let maybeOnKeyDown;
    if (forceNotifyByEnter) {
      maybeOnKeyDown = {onKeyDown: this.onKeyDown};
    } else if (onKeyDown) {
      maybeOnKeyDown = {onKeyDown};
    } else {
      maybeOnKeyDown = {};
    }

    let maybeOnBlur;
    if (forceNotifyOnBlur) {
      maybeOnBlur = {onBlur: this.onBlur};
    } else if (onBlur) {
      maybeOnBlur = {onBlur};
    } else {
      maybeOnBlur = {};
    }

    const maybeRef = inputRef ? {ref: inputRef} : {};

    return React.createElement(element, {
      ...props,
      onChange: this.onChange,
      value,
      ...maybeOnKeyDown,
      ...maybeOnBlur,
      ...maybeRef
    });
  }
}
