1.5.5 / 2017-02-05
=================
  * [Fix] remove early failure for stringified arrays, to handle Symbols in arrays (https://github.com/mjackson/expect/issues/194)
  * [Dev Deps] update `eslint`, `@ljharb/eslint-config`

1.5.4 / 2017-01-25
=================
  * Exclude `html` files, which were never intentionally added to the package (#4)
  * [Deps] update `object-entries`, `is-generator-function`
  * [Dev Deps] update `eslint`, `@ljharb/eslint-config`, `tape`, `jscs`, `semver`, `nsp`, `es6-shim`; remove extra `object.entries`
  * [Tests] up to `node` `v7.4`, `v6.9`, `v4.7`; improve test matrix

1.5.3 / 2016-05-24
=================
  * [Fix] avoid false circular reference positives with falsy values in nested objects (#24)

1.5.2 / 2016-05-18
=================
  * [Deps] update `is-callable`
  * [Dev Deps] update `tape`, `jscs`, `eslint`, `@ljharb/eslint-config`, `nsp`, `es6-shim`, `core-js`
  * [Tests] up to `node` `v6.1`, `v5.11`, `v4.4`
  * [Tests] use pretest/posttest for linting/security
  * [Fix] fix `Object.prototype.toString()` comparison

1.5.1 / 2016-02-22
=================
  * [Fix] fix "why" message for circular reference inequality (#12)
  * [Refactor] Delete unreachable code (#13)
  * [Tests] fix assertion messages (#11)
  * [Docs] fix example (#10)

1.5.0 / 2016-02-15
=================
  * [New] add “whyNotEqual” at `require(‘is-equal/why’)` to provide an inequality reason
  * [Refactor] use `whyNotEqual` internally in `isEqual`
  * [Deps] update `is-callable`
  * [Dev Deps] update `es6-shim`, `tape`, `nsp`, `core-js`, `jscs`, `eslint`, `@ljharb/eslint-config`
  * [Tests] add some more tests
  * [Tests] use `getSymbolIterator` internal module
  * [Tests] up to `node` `v5.6`, `v4.3`

1.4.2 / 2015-12-16
=================
  * [Fix] avoid false positives when the first items in two arrays are not equal (#5)

1.4.1 / 2015-12-15
=================
  * [Fix] ensure that https://github.com/zloirock/core-js/issues/144 doesn't cause false positives (#4)
  * [Refactor] move `Map`/`Set` `forEach` method detection to a separate module
  * [Refactor] Factor out "get Symbol.iterator" logic
  * [Robustness] cache `Object#isPrototypeOf`
  * [Tests] add more tests (#3, #4)

1.4.0 / 2015-12-10
=================
  * [New] Restore basic circular reference support
  * [Deps] use `has` instead of relying on `Function#call`

1.3.1 / 2015-12-10
=================
  * [Fix] Partially revert 2517c2151d57451f7f7009df065bf1601316ee46, since object key ordering shouldn't matter. Reopens #2.
  * [Fix] handle `Map`/`Set` in Safari 8, which lacks `Symbol.iterator` (#3)
  * [Fix] normalize function signature whitespace, for IE 9.
  * [Fix] ignore function name when comparing in engines that lack the "name" property (IE)
  * [Tests] Only skip generic iterable tests when there's no Symbol.iterator (#3)
  * [Refactor] don't attempt to run iterable comparisons when there's no Symbol.iterator
  * [Tests] Separate shimmed from native tests
  * [Tests] relocate native tests
  * [Tests] add tests for circular references (#2)

1.3.0 / 2015-12-09
=================
  * [New] add support for comparing iterables, including native or `es6-shim`med `Map` and `Set` (#1)
  * [Refactor] Use `object.entries` to compare objects instead of for loops
  * [Deps] update `is-callable`
  * [Dev Deps] update `tape`, `jscs`, `semver`, `eslint`, `@ljharb/eslint-config`, `nsp`
  * [Tests] fix npm upgrades for older nodes
  * [Tests] up to `node` `v5.1`
  * [Tests] ensure node 0.8 doesn't fail

1.2.4 / 2015-09-27
=================
  * [Fix] Boxed Symbols should be coerced to primitives before testing for equality
  * [Refactor] Use `is-boolean-object` to reliably detect Booleans
  * [Deps] update `is-arrow-function`, `is-date-object`
  * [Docs] Switch from vb.teelaun.ch to versionbadg.es for the npm version badge SVG
  * [Tests] up to `io.js` `v3.3`, `node` `v4.1`
  * [Tests] add `npm run security` and `npm run eslint`
  * [Dev Deps] update `tape`, `jscs`, `make-arrow-function`, `make-generator-function`, `semver`, `eslint`, `@ljharb/eslint-config`, `nsp`, `covert`

1.2.3 / 2015-02-06
=================
  * Update `is-callable`, `is-number-object`, `is-string`, `is-generator-function`, `tape`, `jscs`
  * Run `travis-ci` tests on `iojs` and `node` v0.12; speed up builds; allow 0.8 failures.

1.2.2 / 2015-01-29
=================
  * Update `is-arrow-function`, `is-callable`, `is-number-object`, `is-string`

1.2.1 / 2015-01-29
=================
  * Use `is-string` and `is-callable` modules.

1.2.0 / 2015-01-28
=================
  * Remove most `Object#toString` checks, to prepare for an ES6 @@toStringTag world where they aren’t reliable.

1.1.1 / 2015-01-20
=================
  * Fix generator function detection in newer v8 / io.js
  * Update `is-arrow-function`, `is-generator-function`, `jscs`, `tape`
  * toString is a reserved word in older browsers

1.1.0 / 2014-12-15
=================
  * Add tests and support for ES6 Symbols, generators, and arrow functions
  * Consider standard functions equal if name/body/arity are all equal.
  * Update `covert`, `tape`, `jscs`
  * Add a bunch of npm scripts

1.0.0 / 2014-08-08
==================
  * Updating `tape`, `covert`
  * Make sure old and unstable nodes don't break Travis
