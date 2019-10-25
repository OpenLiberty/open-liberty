import React from 'react';
import {Collapse} from '../..';


const styles = {
  ba: {
    borderWidth: '1px',
    borderStyle: 'solid'
  },
  mb3: {
    marginBottom: '1rem'
  },
  pa3: {
    padding: '1rem'
  },
  h3: {
    height: '4rem',
    width: '4rem',
    backgroundColor: '#000'
  },
  w3: {
    width: '4rem'
  },
  bgBlack: {
    backgroundColor: '#000'
  }
};


export class Issue59 extends React.PureComponent {
  constructor(props) {
    super(props);
    this.state = {opened: 1};
  }


  onClick1 = () => {
    this.setState({opened: 1});
  };


  onClick2 = () => {
    this.setState({opened: 2}, () => setTimeout(() => this.setState({whatever: 1}), 50));
  };


  render() {
    return (
      <div>
        <div style={styles.mb3}>
          <div style={styles.ba} onClick={this.onClick1}>Header 1</div>
          <Collapse isOpened={this.state.opened === 1}>
            <div style={{...styles.ba, ...styles.pa3}}>
              <div style={{...styles.h3, ...styles.w3, ...styles.bgBlack}}>a</div>
            </div>
          </Collapse>
        </div>
        <div style={styles.mb3}>
          <div style={styles.ba} onClick={this.onClick2}>Header 2</div>
          <Collapse isOpened={this.state.opened === 2}>
            <div style={{...styles.ba, ...styles.pa3}}>
              <div style={{...styles.h3, ...styles.w3, ...styles.bgBlack}}>b</div>
            </div>
          </Collapse>
        </div>
      </div>
    );
  }
}
