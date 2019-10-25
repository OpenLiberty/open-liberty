import React, {PropTypes} from 'react';
import ReactDOM from 'react-dom';
import TestUtils from 'react-addons-test-utils';

const ObjectDescription = require('../src/ObjectDescription');

describe('ObjectDescription', () => {

  beforeEach(() => {

  });

  it('should render number', () => {
    const desc = TestUtils.renderIntoDocument(<ObjectDescription object={0} />);
    const span = TestUtils.findRenderedDOMComponentWithTag(desc, 'span');

    expect(ReactDOM.findDOMNode(span).textContent).toBe("0");
  });

  it('should render string with quotes', () => {
    const desc = TestUtils.renderIntoDocument(<ObjectDescription object={"octocat"} />);
    const span = TestUtils.findRenderedDOMComponentWithTag(desc, 'span');

    expect(ReactDOM.findDOMNode(span).textContent).toBe("\"octocat\"");
  });

  it('should render boolean', () => {
    for(let value of [true, false]){
      const desc = TestUtils.renderIntoDocument(<ObjectDescription object={value} />);
      const span = TestUtils.findRenderedDOMComponentWithTag(desc, 'span');

      expect(ReactDOM.findDOMNode(span).textContent).toBe(value.toString());
    }
  });

  it('should render undefined', () => {
    const desc = TestUtils.renderIntoDocument(<ObjectDescription />);
    const span = TestUtils.findRenderedDOMComponentWithTag(desc, 'span');

    expect(ReactDOM.findDOMNode(span).textContent).toBe("undefined");
  });

  it('shoudl render null', () => {
    const desc = TestUtils.renderIntoDocument(<ObjectDescription object={null}/>);
    const span = TestUtils.findRenderedDOMComponentWithTag(desc, 'span');

    expect(ReactDOM.findDOMNode(span).textContent).toBe("null");
  });

  it('should display date correctly', () => {
    const dateString = 'December 17, 1995 03:24:00';
    const date = new Date(dateString);
    const desc = TestUtils.renderIntoDocument(<ObjectDescription object={date}/>);
    const span = TestUtils.findRenderedDOMComponentWithTag(desc, 'span');

    expect(ReactDOM.findDOMNode(span).textContent).toBe((new Date(dateString)).toString());
  });

  it('should render array with length information', () => {
    const desc = TestUtils.renderIntoDocument(<ObjectDescription object={[1,2,3,4,5]}/>);
    const span = TestUtils.findRenderedDOMComponentWithTag(desc, 'span');

    expect(ReactDOM.findDOMNode(span).textContent).toBe("Array[5]");
  });

  it('should render an empty object', () => {
    const desc = TestUtils.renderIntoDocument(<ObjectDescription object={{}}/>);
    const span = TestUtils.findRenderedDOMComponentWithTag(desc, 'span');

    expect(ReactDOM.findDOMNode(span).textContent).toBe("Object");
  });

  it('should render a simple object', () => {
    const desc = TestUtils.renderIntoDocument(<ObjectDescription object={{'k': 'v'}}/>);
    const span = TestUtils.findRenderedDOMComponentWithTag(desc, 'span');

    expect(ReactDOM.findDOMNode(span).textContent).toBe("Object");
  });

  it('should render an anonymous function', () => {
    const desc = TestUtils.renderIntoDocument(<ObjectDescription object={function(){}}/>);
    const spans = TestUtils.scryRenderedDOMComponentsWithTag(desc, 'span');

    expect(spans.length).toBe(3);
  });

  it('should render a named function', () => {
    const desc = TestUtils.renderIntoDocument(<ObjectDescription object={function id(a){return a;}}/>);
    const spans = TestUtils.scryRenderedDOMComponentsWithTag(desc, 'span');

    expect(spans.length).toBe(3);
    expect(ReactDOM.findDOMNode(spans[0]).textContent).toBe("function\xa0id()");
  });

  // Need phantomjs 2
  // it('symbol', () => {
  //   const desc = TestUtils.renderIntoDocument(<ObjectDescription object={Symbol("foo")}/>);
  //   const span = TestUtils.findRenderedDOMComponentWithTag(desc, 'span');
  //
  //   expect(React.findDOMNode(span).textContent).toBe("Symbol()");
  // });

});
