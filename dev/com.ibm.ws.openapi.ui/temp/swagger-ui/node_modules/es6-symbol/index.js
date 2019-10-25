"use strict";

module.exports = require("./is-implemented")()
	? require("es5-ext/global").Symbol
	: require("./polyfill");
