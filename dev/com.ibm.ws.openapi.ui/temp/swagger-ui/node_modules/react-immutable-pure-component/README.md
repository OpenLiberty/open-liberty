[![npm version](https://badge.fury.io/js/react-immutable-pure-component.svg)](https://badge.fury.io/js/react-immutable-pure-component)

# ImmutablePureComponent

Unfortunately `React.PureComponent` is not embracing `Immutable.js` to it full potential. So here is my solution to this problem.
[npm package](https://www.npmjs.com/package/react-immutable-pure-component) is
parsed with babel so feel safe to use it from package repository or just copy
it to your project and go from here.

[Here](https://monar.github.io/react-immutable-pure-component/) you will find a simple example of a problem it's solving.

The `ImmutablePureComponent` extends component functionality by introducing:
* `updateOnProps`
* `updateOnStates`

With those properties you can specify
list of props or states that will be checked for changes. If value is
`undefined` (default) then all `props` and `state` will be checked, otherwise
array of strings is expected.

This way component can react to property  changes that matters. Useful when
passing lambda function like this: `<Component onChange={(e) =>
doWhatEver(e)}/> `, that otherwise would trigger update every time.

```js
/*
  Copyright (C) 2017 Piotr Tomasz Monarski.
  Licensed under the MIT License (MIT), see
  https://github.com/Monar/react-immutable-pure-component
*/

import React from 'react';
import { is } from 'immutable';


export class ImmutablePureComponent extends React.Component {

  shouldComponentUpdate(nextProps, nextState = {}) {
    const state = this.state || {};

    return !(this.updateOnProps || Object.keys({...nextProps, ...this.props})).every((p) => is(nextProps[p], this.props[p]))
      || !(this.updateOnStates || Object.keys({ ...nextState, ...state})).every((s) => is(nextState[s], state[s]));
  }
}

export default ImmutablePureComponent;
```
