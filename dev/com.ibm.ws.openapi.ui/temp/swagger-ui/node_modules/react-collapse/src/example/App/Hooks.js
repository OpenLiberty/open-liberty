import React from 'react';
import {Collapse} from '../..';
import text from './text.json';


const getText = num => text.slice(0, num).map((p, i) => <p key={i}>{p}</p>);


export class Hooks extends React.PureComponent {
  constructor(props) {
    super(props);
    this.state = {
      isOpened: false,
      isResting: false,
      renderHeight: -1,
      keepContent: false,
      height: -1,
      width: -1,
      paragraphs: 0
    };
  }

  onRender = ({current, from, to}) => {
    if (this.ref) {
      this.ref.innerHTML = `
        from: ${from.toFixed(2)},
        to: ${to.toFixed(2)},
        current: ${current.toFixed(2)}
      `;
    }
  };

  onMeasure = ({height, width}) => {
    this.setState({height, width});
  };

  onRest = () => {
    this.setState({isResting: true});
  };

  render() {
    const {isOpened, height, width, paragraphs} = this.state;

    return (
      <div>
        <div className="config">
          <label className="label">
            Opened:
            <input className="input"
              type="checkbox"
              checked={isOpened}
              onChange={({target: {checked}}) => this.setState({
                isOpened: checked,
                isResting: false
              })} />
          </label>

          <label className="label">
            Paragraphs:
            <input className="input"
              type="range"
              value={paragraphs} step={1} min={0} max={4}
              onChange={({target: {value}}) => this.setState({
                paragraphs: parseInt(value, 10),
                isResting: false
              })} />
            {paragraphs}
          </label>
        </div>
        <div className="config">
          <label className="label">
            height: {height}px
          </label>
          <label className="label">
            width: {width}px
          </label>
          <label className="label">
            resting: {this.state.isResting ? 'true' : 'false'}
          </label>
          <label className="label" ref={ref => (this.ref = ref)} />
        </div>
        <Collapse
          isOpened={isOpened}
          onRender={this.onRender}
          onMeasure={this.onMeasure}
          onRest={this.onRest}>
          <div className="text">{paragraphs ? getText(paragraphs) : <p>No text</p>}</div>
        </Collapse>
      </div>
    );
  }
}
