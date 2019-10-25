import React from 'react';
import {Collapse} from '../..';


export class VariableHeight extends React.PureComponent {
  constructor(props) {
    super(props);
    this.state = {isOpened: false, height: 100};
  }


  render() {
    const {isOpened, height} = this.state;

    return (
      <div {...this.props}>
        <div className="config">
          <label className="label">
            Opened:
            <input className="input"
              type="checkbox"
              checked={isOpened}
              onChange={({target: {checked}}) => this.setState({isOpened: checked})} />
          </label>

          <label className="label">
            Content height:
            <input className="input"
              type="range"
              value={height} step={50} min={0} max={500}
              onChange={({target: {value}}) => this.setState({height: parseInt(value, 10)})} />
            {height}
          </label>
        </div>

        <Collapse isOpened={isOpened}>
          <div style={{height}} className="blob" />
        </Collapse>

      </div>
    );
  }
}
