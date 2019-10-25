import React from 'react';
import {Collapse} from '../..';


export class InitiallyOpened extends React.PureComponent {
  constructor(props) {
    super(props);
    this.state = {isOpened: true};
  }

  render() {
    const {isOpened} = this.state;
    const height = 100;

    return (
      <div>
        <div className="config">
          <label className="label">
            Opened:
            <input className="input"
              type="checkbox"
              checked={isOpened}
              onChange={({target: {checked}}) => this.setState({isOpened: checked})} />
          </label>
        </div>
        <Collapse isOpened={isOpened}>
          <div style={{height}} className="blob" />
        </Collapse>
      </div>
    );
  }
}
