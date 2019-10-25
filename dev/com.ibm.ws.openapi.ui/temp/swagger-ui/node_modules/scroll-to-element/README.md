# scroll-to-element

Smooth scrolls to element of the specified selector or element reference with optional offset, scroll-positon, easing, and duration. Takes into account document height for elements low on the page.

[![NPM](https://nodei.co/npm/scroll-to-element.png)](https://nodei.co/npm/scroll-to-element/)

## `scrollToElement(selector, <options>)`
##### Valid options:

###### offset : *number*

> Add an additional offset to the final position. if
> \> 0 then page is moved to the bottom otherwise the page is moved to the top.

###### align : *string*

> Alignment of the element in the resulting viewport. Can be
> one of `'top'`, `'middle'` or `'bottom'`. Defaulting to `'top'`.

###### ease : *string*

> Easing function defaulting to "out-circ" (view [ease](https://github.com/component/ease) for more)

###### duration : *number*

> Animation duration defaulting to `1000`

## EXAMPLE

```js
var scrollToElement = require('scroll-to-element');

scrollToElement('#id');

// with options
scrollToElement('.className', {
	offset: 0,
	ease: 'out-bounce',
	duration: 1500
});

// or if you already have a reference to the element
var elem = document.querySelector('.className');
scrollToElement(elem, {
	offset: 0,
	ease: 'out-bounce',
	duration: 1500
});
```

## LICENSE

MIT
