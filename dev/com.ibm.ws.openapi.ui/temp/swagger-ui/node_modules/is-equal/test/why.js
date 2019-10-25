'use strict';

/* eslint-disable operator-linebreak */

var test = require('tape');
var isEqualWhy = require('../why');
var isSymbol = require('is-symbol');
var genFn = require('make-generator-function');
var hasGeneratorSupport = typeof genFn === 'function';
var arrowFunctions = require('make-arrow-function').list();
var hasArrowFunctionSupport = arrowFunctions.length > 0;
var objectEntries = require('object.entries');
var forEach = require('foreach');

var collectionsForEach = require('../getCollectionsForEach')();

var fooFn = function fooFn() {};
var functionsHaveNames = fooFn.name === 'fooFn';

var symbolIterator = require('../getSymbolIterator')();

var copyFunction = function (fn) {
	/* eslint-disable no-new-func */
	return Function('return ' + String(fn))();
};

test('primitives', function (t) {
	t.equal('', isEqualWhy(), 'undefineds are equal');
	t.equal('', isEqualWhy(null, null), 'nulls are equal');
	t.equal('', isEqualWhy(true, true), 'trues are equal');
	t.equal('', isEqualWhy(false, false), 'falses are equal');
	t.equal('primitive value of boolean arguments do not match: true !== false', isEqualWhy(true, false), 'true:false is not equal');
	t.equal('primitive value of boolean arguments do not match: false !== true', isEqualWhy(false, true), 'false:true is not equal');
	t.equal('', isEqualWhy('foo', 'foo'), 'strings are equal');
	t.equal('', isEqualWhy(42, 42), 'numbers are equal');
	t.equal('', isEqualWhy(0 / Infinity, -0 / Infinity), 'opposite sign zeroes are equal');
	t.equal('', isEqualWhy(Infinity, Infinity), 'infinities are equal');
	t.end();
});

test('NaN', function (t) {
	t.equal('', isEqualWhy(NaN, NaN), 'NaNs are equal');
	t.end();
});

test('boxed primitives', function (t) {
	t.equal('', isEqualWhy(Object(''), ''), 'Empty String and empty string are equal');
	t.equal('', isEqualWhy(Object('foo'), 'foo'), 'String and string are equal');
	t.equal('', isEqualWhy(Object(true), true), 'Boolean true and boolean true are equal');
	t.equal('', isEqualWhy(Object(false), false), 'Boolean false and boolean false are equal');
	t.equal('', isEqualWhy(true, Object(true)), 'boolean true and Boolean true are equal');
	t.equal('', isEqualWhy(false, Object(false)), 'boolean false and Boolean false are equal');
	t.equal(
		'primitive value of boolean arguments do not match: true !== false',
		isEqualWhy(Object(true), false),
		'Boolean true and boolean false are not equal'
	);
	t.equal(
		'primitive value of boolean arguments do not match: false !== true',
		isEqualWhy(Object(false), true),
		'Boolean false and boolean true are not equal'
	);
	t.equal(
		'primitive value of boolean arguments do not match: false !== true',
		isEqualWhy(false, Object(true)),
		'boolean false and Boolean true are not equal'
	);
	t.equal(
		'primitive value of boolean arguments do not match: true !== false',
		isEqualWhy(true, Object(false)),
		'boolean true and Boolean false are not equal'
	);
	t.equal('', isEqualWhy(Object(42), 42), 'Number and number literal are equal');
	t.end();
});

test('dates', function (t) {
	t.equal('', isEqualWhy(new Date(123), new Date(123)), 'two dates with the same timestamp are equal');
	t.equal(
		'Dates have different time values: 123 !== 456',
		isEqualWhy(new Date(123), new Date(456)),
		'two dates with different timestamp are not equal'
	);
	t.end();
});

test('regexes', function (t) {
	t.equal('', isEqualWhy(/a/g, /a/g), 'two regex literals are equal');
	t.equal(
		'regular expressions differ: /a/g !== /b/g',
		isEqualWhy(/a/g, /b/g),
		'two different regex literals are not equal'
	);
	t.equal(
		'regular expressions differ: /a/i !== /a/g',
		isEqualWhy(/a/i, /a/g),
		'two different regex literals (same source, diff flags) are not equal'
	);
	t.equal('', isEqualWhy(new RegExp('a', 'g'), new RegExp('a', 'g')), 'two regex objects are equal');
	t.equal(
		'regular expressions differ: /a/g !== /b/g',
		isEqualWhy(new RegExp('a', 'g'), new RegExp('b', 'g')),
		'two different regex objects are equal'
	);
	t.equal('', isEqualWhy(new RegExp('a', 'g'), /a/g), 'regex object and literal, same content, are equal');
	t.equal(
		'regular expressions differ: /a/g !== /b/g',
		isEqualWhy(new RegExp('a', 'g'), /b/g),
		'regex object and literal, different content, are not equal'
	);
	t.end();
});

test('arrays', function (t) {
	t.equal('', isEqualWhy([], []), 'empty arrays are equal');
	t.equal('', isEqualWhy([1, 2, 3], [1, 2, 3]), 'same arrays are equal');
	t.equal(
		'numbers are different: 3 !== 1',
		isEqualWhy([1, 2, 3], [3, 2, 1]),
		'arrays in different order with same values are not equal'
	);
	t.equal(
		'arrays have different length: 2 !== 3',
		isEqualWhy([1, 2], [1, 2, 3]),
		'arrays with different lengths are not equal'
	);
	t.equal(
		'arrays have different length: 3 !== 2',
		isEqualWhy([1, 2, 3], [1, 2]),
		'arrays with different lengths are not equal'
	);

	t.test('nested values', function (st) {
		st.equal('', isEqualWhy([[1, 2], [2, 3], [3, 4]], [[1, 2], [2, 3], [3, 4]]), 'arrays with same array values are equal');
		st.end();
	});

	t.test('nested objects', function (st) {
		var arr1 = [
			{ a: 0, b: '1', c: false },
			{ a: 1, b: '2', c: false }
		];
		var arr2 = [
			{ a: 0, b: '1', c: true },
			{ a: 1, b: '2', c: false }
		];
		st.equal(
			isEqualWhy(arr1[0], arr2[0]),
			'value at key "c" differs: primitive value of boolean arguments do not match: false !== true',
			'array items 0 are not equal'
		);
		st.equal(
			isEqualWhy(arr1[1], arr2[1]),
			'',
			'array items 1 are equal'
		);
		st.equal(
			isEqualWhy(arr1, arr2),
			'value at key "c" differs: primitive value of boolean arguments do not match: false !== true',
			'two arrays with nested inequal objects are not equal'
		);

		st.end();
	});

	t.end();
});

test('objects', function (t) {
	t.test('prototypes', function (st) {
		var F = function F() {
			this.foo = 42;
		};
		var G = function G() {};
		G.prototype = new F();
		G.prototype.constructor = G;
		var H = function H() {};
		H.prototype = G.prototype;
		var I = function I() {};

		var f1 = new F();
		var f2 = new F();
		var g1 = new G();
		var h1 = new H();
		var i1 = new I();

		st.equal(isEqualWhy(f1, f2), '', 'two instances of the same thing are equal');

		st.equal(isEqualWhy(g1, h1), '', 'two instances of different things with the same prototype are equal');
		st.equal(
			isEqualWhy(f1, i1),
			'arguments have a different [[Prototype]]',
			'two instances of different things with a different prototype are not equal'
		);

		var isParentEqualToChild = isEqualWhy(f1, g1);
		st.equal(
			isParentEqualToChild,
			'arguments have a different [[Prototype]]',
			'two instances of a parent and child are not equal'
		);
		var isChildEqualToParent = isEqualWhy(g1, f1);
		st.equal(
			isChildEqualToParent,
			'arguments have a different [[Prototype]]',
			'two instances of a child and parent are not equal'
		);

		g1.foo = 'bar';
		var g2 = new G();
		st.equal(
			isEqualWhy(g1, g2),
			'first argument has key "foo"; second does not',
			'two instances of the same thing with different properties are not equal'
		);
		st.equal(
			isEqualWhy(g2, g1),
			'second argument has key "foo"; first does not',
			'two instances of the same thing with different properties are not equal'
		);
		st.end();
	});

	t.test('literals', function (st) {
		var a = { foo: 42 };
		var b = { foo: 42 };
		st.equal('', isEqualWhy(a, a), 'same hash is equal to itself');
		st.equal('', isEqualWhy(a, b), 'two similar hashes are equal');
		st.equal('', isEqualWhy({ nested: a }, { nested: a }), 'similar hashes with same nested hash are equal');
		st.equal('', isEqualWhy({ nested: a }, { nested: b }), 'similar hashes with similar nested hash are equal');

		st.equal(
			isEqualWhy({ a: 42, b: 0 }, { a: 42 }),
			'first argument has key "b"; second does not',
			'second hash missing a key is not equal'
		);
		st.equal(
			isEqualWhy({ a: 42 }, { a: 42, b: 0 }),
			'second argument has key "b"; first does not',
			'first hash missing a key is not equal'
		);

		st.equal(
			isEqualWhy({ a: 1 }, { a: 2 }),
			'value at key "a" differs: numbers are different: 1 !== 2',
			'two objects with equal keys but inequal values are not equal'
		);
		st.equal(
			isEqualWhy({ c: 1 }, { a: 1 }),
			'first argument has key "c"; second does not',
			'two objects with inequal keys but same values are not equal'
		);

		var obj1 = { a: 0, b: '1', c: false };
		var obj2 = { a: 0, b: '1', c: true };
		st.equal(
			isEqualWhy(obj1, obj2),
			'value at key "c" differs: primitive value of boolean arguments do not match: false !== true',
			'two objects with inequal boolean keys are not equal'
		);
		st.end();
	});

	t.test('key ordering', function (st) {
		var a = { a: 1, b: 2 };
		var b = { b: 2 };
		b.a = 1;
		st.equal('', isEqualWhy(a, b), 'objects with different key orderings but same contents are equal');
		st.end();
	});

	t.end();
});

test('functions', function (t) {
	var f1 = Object(function f() { /* SOME STUFF */ return 1; });
	var f2 = Object(function f() { /* SOME STUFF */ return 1; });
	var f3 = Object(function f() { /* SOME DIFFERENT STUFF */ return 2; });
	var g = Object(function g() { /* SOME STUFF */ return 1; });
	var anon1 = Object(function () { /* ANONYMOUS! */ return 'anon'; });
	var anon2 = Object(function () { /* ANONYMOUS! */ return 'anon'; });
	/* jscs: disable */
	/* eslint-disable space-before-function-paren */
	/* eslint-disable space-before-blocks */
	var fnNoSpace = Object(function(){});
	/* eslint-enable space-before-blocks */
	/* eslint-enable space-before-function-paren */
	/* jscs: enable */
	var fnWithSpaceBeforeBody = Object(function () {});
	var emptyFnWithName = Object(function a() {});
	/* eslint-disable no-unused-vars */
	var emptyFnOneArg = Object(function (a) {});
	var anon1withArg = Object(function (a) { /* ANONYMOUS! */ return 'anon'; });
	/* eslint-enable no-unused-vars */

	/* for code coverage */
	f1();
	f2();
	f3();
	g();
	anon1();
	anon2();
	/* end for code coverage */

	t.equal('', isEqualWhy(f1, f1), 'same function is equal to itself');
	t.equal('', isEqualWhy(anon1, anon1), 'same anon function is equal to itself');
	t.equal(
		isEqualWhy(anon1, anon1withArg),
		'Function lengths differ: 0 !== 1',
		'similar anon function with different lengths are not equal'
	);

	if (functionsHaveNames) {
		t.equal(
			'Function names differ: "f" !== "g"',
			isEqualWhy(f1, g),
			'functions with different names but same implementations are not equal'
		);
	} else {
		t.comment('** function names not supported **');
		t.equal(
			'Function string representations differ',
			isEqualWhy(f1, g),
			'functions with different names but same implementations are not equal'
		);
	}
	t.equal('', isEqualWhy(f1, f2), 'functions with same names but same implementations are equal');
	t.equal(
		isEqualWhy(f1, f3),
		'Function string representations differ',
		'functions with same names but different implementations are not equal'
	);
	t.equal('', isEqualWhy(anon1, anon2), 'anon functions with same implementations are equal');

	t.equal('', isEqualWhy(fnNoSpace, fnWithSpaceBeforeBody), 'functions with same arity/name/body are equal despite whitespace between signature and body');
	if (functionsHaveNames) {
		t.equal(
			isEqualWhy(emptyFnWithName, fnNoSpace),
			'Function names differ: "a" !== ""',
			'functions with same arity/body, diff name, are not equal'
		);
	} else {
		t.comment('** function names not supported **');
		t.equal(
			isEqualWhy(emptyFnWithName, fnNoSpace),
			'Function string representations differ',
			'functions with same arity/body, diff name, are not equal'
		);
	}
	t.equal(
		isEqualWhy(emptyFnOneArg, fnNoSpace),
		'Function lengths differ: ' + emptyFnOneArg.length + ' !== ' + fnNoSpace.length,
		'functions with same name/body, diff arity, are not equal'
	);

	t.test('generators', { skip: !hasGeneratorSupport }, function (st) {
		/* eslint-disable no-new-func */
		var genFnStar = Function('return function* () {};')();
		var genFnSpaceStar = Function('return function *() {};')();
		var genNoSpaces = Function('return function*(){};')();
		var reasonsMap = {
			'second argument is a Generator; first is not': true,
			'toStringTag is not the same: [object Function] !== [object GeneratorFunction]': true
		};
		var reasons = objectEntries(reasonsMap);
		var actual = isEqualWhy(fnNoSpace, genNoSpaces);
		reasonsMap[actual] = true;
		st.deepEqual(objectEntries(reasonsMap), reasons, 'generator and fn that are otherwise identical are not equal');

		var generators = [genFnStar, genFnSpaceStar, genNoSpaces];
		forEach(generators, function (generator) {
			st.equal('', isEqualWhy(generator, generator), generator + ' is equal to itself');
			st.equal('', isEqualWhy(generator, copyFunction(generator)), generator + ' is equal to copyFunction(generator)');
		});
		st.end();
	});

	t.test('arrow functions', { skip: !hasArrowFunctionSupport }, function (st) {
		forEach(arrowFunctions, function (fn) {
			st.equal(
				'second argument is an Arrow function; first is not',
				isEqualWhy(fnNoSpace, fn),
				fn + ' not equal to ' + fnNoSpace
			);
			st.equal('', isEqualWhy(fn, fn), fn + ' equal to itself');
			st.equal('', isEqualWhy(fn, copyFunction(fn)), fn + ' equal to copyFunction(fn)');
		});
		st.end();
	});

	t.end();
});

var hasSymbols = typeof Symbol === 'function' && isSymbol(Symbol('foo'));
test('symbols', { skip: !hasSymbols }, function (t) {
	var foo = 'foo';
	var fooSym = Symbol(foo);
	var objectFooSym = Object(fooSym);
	t.equal('', isEqualWhy(fooSym, fooSym), 'Symbol("foo") is equal to itself');
	t.equal('', isEqualWhy(fooSym, objectFooSym), 'Symbol("foo") is equal to the object form of itself');
	t.equal(
		'first Symbol value !== second Symbol value',
		isEqualWhy(Symbol(foo), Symbol(foo)),
		'Symbol("foo") is not equal to Symbol("foo"), even when the string is the same instance'
	);
	t.equal(
		'first Symbol value !== second Symbol value',
		isEqualWhy(Symbol(foo), Object(Symbol(foo))),
		'Symbol("foo") is not equal to Object(Symbol("foo")), even when the string is the same instance'
	);

	t.test('arrays containing symbols', function (st) {
		st.equal(
			'',
			isEqualWhy([fooSym], [fooSym]),
			'Arrays each containing the same instance of Symbol("foo") are equal'
		);

		st.equal(
			'first Symbol value !== second Symbol value',
			isEqualWhy([Symbol(foo)], [Object(Symbol(foo))]),
			'An array containing Symbol("foo") is not equal to Object(Symbol("foo")), even when the string is the same instance'
		);

		st.end();
	});

	t.end();
});

var genericIterator = function (obj) {
	var entries = objectEntries(obj);
	return function iterator() {
		return {
			next: function () {
				return {
					done: entries.length === 0,
					value: entries.shift()
				};
			}
		};
	};
};

test('iterables', function (t) {
	t.test('Maps', { skip: !collectionsForEach.Map }, function (mt) {
		var a = new Map();
		a.set('a', 'b');
		a.set('c', 'd');
		var b = new Map();
		b.set('a', 'b');
		b.set('c', 'd');
		var c = new Map();
		c.set('a', 'b');

		mt.equal(
			isEqualWhy(a, b),
			'',
			'equal Maps (a, b) are equal'
		);
		mt.equal(
			isEqualWhy(b, a),
			'',
			'equal Maps (b, a) are equal'
		);
		mt.equal(
			isEqualWhy(a, c),
			symbolIterator
				? 'second argument finished iterating before first'
				: 'Collection entries differ: arrays have different length: 2 !== 1',
			'unequal Maps (a, c) are not equal'
		);
		mt.equal(
			isEqualWhy(b, c),
			symbolIterator
				? 'second argument finished iterating before first'
				: 'Collection entries differ: arrays have different length: 2 !== 1',
			'unequal Maps (b, c) are not equal'
		);
		mt.equal(
			isEqualWhy(c, a),
			symbolIterator
				? 'first argument finished iterating before second'
				: 'Collection entries differ: arrays have different length: 1 !== 2',
			'unequal Maps (c, a) are not equal'
		);
		mt.equal(
			isEqualWhy(c, b),
			symbolIterator
				? 'first argument finished iterating before second'
				: 'Collection entries differ: arrays have different length: 1 !== 2',
			'unequal Maps (c, b) are not equal'
		);

		mt.end();
	});

	t.test('Sets', { skip: !collectionsForEach.Set }, function (st) {
		var a = new Set();
		a.add('a');
		a.add('b');
		var b = new Set();
		b.add('a');
		b.add('b');
		var c = new Set();
		c.add('a');

		st.equal('', isEqualWhy(a, b), 'equal Set (a, b) are equal');
		st.equal('', isEqualWhy(b, a), 'equal Set (b, a) are equal');
		st.equal(
			isEqualWhy(a, c),
			symbolIterator
				? 'second argument finished iterating before first'
				: 'Collection entries differ: arrays have different length: 2 !== 1',
			'unequal Set (a, c) are not equal'
		);
		st.equal(
			isEqualWhy(b, c),
			symbolIterator
				? 'second argument finished iterating before first'
				: 'Collection entries differ: arrays have different length: 2 !== 1',
			'unequal Set (b, c) are not equal'
		);
		st.equal(
			isEqualWhy(c, a),
			symbolIterator
				? 'first argument finished iterating before second'
				: 'Collection entries differ: arrays have different length: 1 !== 2',
			'unequal Set (c, a) are not equal'
		);
		st.equal(
			isEqualWhy(c, b),
			symbolIterator
				? 'first argument finished iterating before second'
				: 'Collection entries differ: arrays have different length: 1 !== 2',
			'unequal Set (b, c) are not equal'
		);

		st.test('Sets with strings as iterables', function (sst) {
			var ab;
			// eslint-disable-next-line max-statements-per-line
			try { ab = new Set('ab'); } catch (e) { ab = new Set(); } // node 0.12 throws when given a string
			if (ab.size !== 2) {
				// work around IE 11 (and others) bug accepting iterables
				ab.add('a');
				ab.add('b');
			}
			var ac;
			// eslint-disable-next-line max-statements-per-line
			try { ac = new Set('ac'); } catch (e) { ac = new Set(); } // node 0.12 throws when given a string
			if (ac.size !== 2) {
				// work around IE 11 (and others) bug accepting iterables
				ac.add('a');
				ac.add('c');
			}
			st.equal(
				isEqualWhy(ab, ac),
				symbolIterator
					? 'iteration results are not equal: value at key "value" differs: string values are different: "b" !== "c"'
					: 'Collection entries differ: string values are different: "b" !== "c"',
				'Sets initially populated with different strings are not equal'
			);
			sst.end();
		});

		st.end();
	});

	var obj = { a: { aa: true }, b: [2] };
	t.test('generic iterables', { skip: !symbolIterator }, function (it) {
		var a = { foo: 'bar' };
		var b = { bar: 'baz' };

		it.equal(isEqualWhy(a, b), 'first argument has key "foo"; second does not', 'normal a and normal b are not equal');

		a[symbolIterator] = genericIterator(obj);
		it.equal(isEqualWhy(a, b), 'first argument is iterable; second is not', 'iterable a / normal b are not equal');
		it.equal(isEqualWhy(b, a), 'second argument is iterable; first is not', 'iterable b / normal a are not equal');
		it.equal(
			isEqualWhy(a, obj),
			'first argument is iterable; second is not',
			'iterable a / normal obj are not equal'
		);
		it.equal(
			isEqualWhy(obj, a),
			'second argument is iterable; first is not',
			'normal obj / iterable a are not equal'
		);

		b[symbolIterator] = genericIterator(obj);
		it.equal(isEqualWhy(a, b), '', 'iterable a / iterable b are equal');
		it.equal(isEqualWhy(b, a), '', 'iterable b / iterable a are equal');
		it.equal(
			isEqualWhy(b, obj),
			'first argument is iterable; second is not',
			'iterable b and normal obj are not equal'
		);
		it.equal(
			isEqualWhy(obj, b),
			'second argument is iterable; first is not',
			'normal obj / iterable b are not equal'
		);

		it.end();
	});

	t.test('unequal iterables', { skip: !symbolIterator }, function (it) {
		var c = {};
		c[symbolIterator] = genericIterator({});
		var d = {};
		d[symbolIterator] = genericIterator(obj);

		it.equal(
			isEqualWhy(c, d),
			'first argument finished iterating before second',
			'iterable c / iterable d are not equal'
		);
		it.equal(
			isEqualWhy(d, c),
			'second argument finished iterating before first',
			'iterable d / iterable c are not equal'
		);

		it.end();
	});

	t.end();
});

var Circular = function Circular() {
	this.circularRef = this;
};
test('circular references', function (t) {
	var a = new Circular();
	var b = new Circular();
	t.equal(
		isEqualWhy(a, b),
		'',
		'two circular referencing instances are equal'
	);

	var c = {};
	var d = {};
	c.c = c;
	d.d = d;
	t.equal(
		isEqualWhy(c, d),
		'first argument has key "c"; second does not',
		'two objects with different circular references are not equal'
	);

	var e = {};
	var f = {};
	e.e = e;
	f.e = null;
	t.equal(
		isEqualWhy(e, f),
		'first argument has a circular reference at key "e"; second does not',
		'two objects without corresponding circular references are not equal'
	);

	t.equal(
		isEqualWhy(f, e),
		'second argument has a circular reference at key "e"; first does not',
		'two objects without corresponding circular references are not equal'
	);

	t.test('false positives', function (st) {
		st.equal(
			isEqualWhy({ bar: { baz: 'abc' } }, { bar: { baz: null } }),
			'value at key "bar" differs: value at key "baz" differs: abc !== null',
			'two nested structures with a string vs null key are not equal'
		);

		st.equal(
			isEqualWhy({ bar: { baz: 'abc' } }, { bar: { baz: undefined } }),
			'value at key "bar" differs: value at key "baz" differs: abc !== undefined',
			'two nested structures with a string vs null key are not equal'
		);

		st.equal(
			isEqualWhy({ bar: { baz: 'abc' } }, { bar: { baz: '' } }),
			'value at key "bar" differs: value at key "baz" differs: string values are different: "abc" !== ""',
			'two nested structures with different string keys are not equal'
		);

		st.end();
	});

	t.end();
});
