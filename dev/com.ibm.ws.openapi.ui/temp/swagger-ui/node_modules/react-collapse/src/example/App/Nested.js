import React from 'react';
import {Collapse} from '../..';
import {VariableHeight} from './VariableHeight';


export class Nested extends React.PureComponent {
  constructor(props) {
    super(props);
    this.state = {isOpened: false};
  }


  render() {
    const {isOpened} = this.state;

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

        <Collapse isOpened={isOpened} hasNestedCollapse={true}>
          <VariableHeight className="subCollapse" />
          <VariableHeight className="subCollapse" />
          <VariableHeight className="subCollapse" />
        </Collapse>
      </div>
    );
  }
}
