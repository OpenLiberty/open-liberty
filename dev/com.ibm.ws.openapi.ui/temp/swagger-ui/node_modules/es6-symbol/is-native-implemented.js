// Exports true if environment provides native `Symbol` implementation

"use strict";

var Symbol = require("es5-ext/global").Symbol;

module.exports = typeof Symbol === "function" && typeof Symbol() === "symbol";
