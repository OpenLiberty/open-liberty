'use strict';

var CommonMark = require('commonmark');
var ReactRenderer = require('../');

var parser = new CommonMark.Parser();
var renderer = new ReactRenderer();

var input = '# This is a header\n\nAnd this is a paragraph';
var ast = parser.parse(input);

// Result of this operation will be an array of React elements
renderer.render(ast);
