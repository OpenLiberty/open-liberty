import React from 'react';
import {presets} from 'react-motion';
import {Collapse} from '../..';


export class SpringConfig extends React.PureComponent {
  constructor(props) {
    super(props);

    const preset = 'stiff';
    const {stiffness, damping} = presets[preset];

    this.state = {isOpened: false, height: 100, preset: 'stiff', stiffness, damping};
  }


  onChangePreset = ({target: {value: preset}}) => {
    const {stiffness, damping} = presets[preset];

    this.setState({preset, stiffness, damping});
  };


  render() {
    const {isOpened, height, preset, stiffness, damping} = this.state;

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

          <label className="label">
            Content height:
            <input className="input"
              type="range"
              value={height} step={50} min={0} max={500}
              onChange={({target: {value}}) => this.setState({height: parseInt(value, 10)})} />
            {height}
          </label>

          <label className="label">
            Preset:
            <select className="input"
              value={preset} step={10} min={0} max={300}
              onChange={this.onChangePreset}>
              {Object.keys(presets).map(p => <option key={p} value={p}>{p}</option>)}
            </select>
          </label>

          <label className="label">
            Stiffness:
            <input className="input"
              type="range"
              value={stiffness} step={10} min={0} max={300}
              onChange={({target: {value}}) => this.setState({stiffness: parseInt(value, 10)})} />
            {stiffness}
          </label>

          <label className="label">
            Damping:
            <input className="input"
              type="range"
              value={damping} step={5} min={0} max={40}
              onChange={({target: {value}}) => this.setState({damping: parseInt(value, 10)})} />
            {damping}
          </label>
        </div>
        <Collapse

          isOpened={isOpened}
          springConfig={{stiffness, damping}}>
          <div style={{height}} className="blob" />
        </Collapse>

      </div>
    );
  }
}
