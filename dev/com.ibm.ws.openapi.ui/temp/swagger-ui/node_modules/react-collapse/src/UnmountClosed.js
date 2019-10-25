import React from 'react';
import PropTypes from 'prop-types';
import {Collapse} from './Collapse';


export class UnmountClosed extends React.PureComponent {
  static propTypes = {
    isOpened: PropTypes.bool.isRequired,
    onRest: PropTypes.func
  };

  constructor(props) {
    super(props);
    this.state = {
      shouldUnmount: !this.props.isOpened,
      forceInitialAnimation: !this.props.isOpened
    };
  }

  componentWillReceiveProps = ({isOpened}) => {
    if (!this.props.isOpened && isOpened) {
      this.setState({
        forceInitialAnimation: true,
        shouldUnmount: false
      });
    }
  };


  onRest = (...args) => {
    const {isOpened, onRest} = this.props;

    if (!isOpened) {
      this.setState({shouldUnmount: true});
    }
    if (onRest) {
      onRest(...args);
    }
  };


  render() {
    const {
      isOpened,
      onRest: _onRest,
      ...props
    } = this.props;

    const {
      forceInitialAnimation,
      shouldUnmount
    } = this.state;

    return shouldUnmount ? null : (
      <Collapse
        forceInitialAnimation={forceInitialAnimation}
        isOpened={isOpened}
        onRest={this.onRest}
        {...props} />
    );
  }
}
