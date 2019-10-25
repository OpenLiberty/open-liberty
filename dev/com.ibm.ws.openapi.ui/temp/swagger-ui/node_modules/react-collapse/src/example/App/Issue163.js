import React from 'react';
import {Collapse} from '../..';


export class Issue163 extends React.PureComponent {
  constructor(props) {
    super(props);
    this.state = {
      isOpened: true,
      isOverflowOpened: true
    };
  }

  onClick = () => this.setState({opened: !this.state.opened});

  render() {
    const {isOpened, isOverflowOpened} = this.state;

    return (
      <div>
        <div className="config">
          <label className="label">
            Opened:
            <input
              className="input"
              type="checkbox"
              checked={isOpened}
              onChange={({target: {checked}}) => this.setState({isOpened: checked})} />
          </label>
        </div>

        <Collapse isOpened={isOpened}>
          <div style={{height: 100, paddingTop: 50, paddingLeft: 50}} className="blob">
            <div className="config" style={{position: 'relative'}}>
              <label className="label">
                Overflow opened:
                <input
                  className="input"
                  type="checkbox"
                  checked={isOverflowOpened}
                  onChange={({target: {checked}}) => this.setState({isOverflowOpened: checked})} />
              </label>
              {isOverflowOpened && (
                <div style={{width: 200, height: 200, background: 'black', position: 'absolute'}} />
              )}
            </div>
          </div>
        </Collapse>
      </div>
    );
  }
}
