import React from 'react';
import PropTypes from 'prop-types';
import {Collapse} from '../..';
import text from './text.json';


const getText = num => text.slice(0, num)
  .map((p, i) => <p key={i}>{p}</p>);


export class VariableText extends React.Component {
  static propTypes = {
    isOpened: PropTypes.bool
  };


  static defaultProps = {
    isOpened: false
  };


  constructor(props) {
    super(props);
    this.state = {isOpened: this.props.isOpened, paragraphs: 0};
  }


  render() {
    const {isOpened, paragraphs} = this.state;

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
            Paragraphs:
            <input className="input"
              type="range"
              value={paragraphs} step={1} min={0} max={4}
              onChange={({target: {value}}) => this.setState({paragraphs: parseInt(value, 10)})} />
            {paragraphs}
          </label>
        </div>

        <Collapse isOpened={isOpened}>
          <div className="text">
            {paragraphs ? getText(paragraphs) : <p>No text</p>}
          </div>
        </Collapse>
      </div>
    );
  }
}
