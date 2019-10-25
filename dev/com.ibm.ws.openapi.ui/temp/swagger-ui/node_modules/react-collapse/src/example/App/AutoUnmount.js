import React from 'react';
import PropTypes from 'prop-types';
import {UnmountClosed} from '../..';


class Test extends React.PureComponent {
  static propTypes = {
    onMount: PropTypes.func.isRequired,
    onUnmount: PropTypes.func.isRequired
  };


  componentDidMount() {
    this.props.onMount();
  }


  componentWillUnmount() {
    this.props.onUnmount();
  }


  render() {
    return <div>Test</div>;
  }
}


export class AutoUnmount extends React.PureComponent {
  static propTypes = {
    isOpened: PropTypes.bool.isRequired
  };


  constructor(props) {
    super(props);
    this.state = {isOpened: this.props.isOpened, shouldRender: false};
  }


  componentWillMount() {
    this.counter = 0;
    this.messages = [];
  }


  onRef = ref => {
    this.ref = ref;
  };


  onMount = () => {
    if (this.ref) {
      this.messages.unshift(`${this.counter}. Mounted`);
      this.messages = this.messages.slice(0, 5);
      this.ref.innerHTML = this.messages.join('<br />');
      this.counter = this.counter + 1;
    }
  };


  onUnmount = () => {
    if (this.ref) {
      this.messages.unshift(`${this.counter}. Unmounted`);
      this.messages = this.messages.slice(0, 5);
      this.ref.innerHTML = this.messages.join('<br />');
      this.counter = this.counter + 1;
    }
  };


  onChange = ({target: {checked}}) => {
    this.setState({isOpened: checked});
  };


  render() {
    const {isOpened} = this.state;

    return (
      <div>
        <div className="config">
          <label className="label">
            Opened:
            <input className="input" type="checkbox" checked={isOpened} onChange={this.onChange} />
          </label>
        </div>

        <UnmountClosed isOpened={isOpened}>
          <Test onMount={this.onMount} onUnmount={this.onUnmount} />
        </UnmountClosed>

        <div className="log" ref={this.onRef} />
      </div>
    );
  }
}
