'use strict';

// Make a value ready for JSON.stringify() / process.send()
module.exports = function serializeError(value) {
	if (typeof value === 'object') {
		var serialized = destroyCircular(value, []);

		if (typeof value.name === 'string') {
			serialized.name = value.name;
		}

		if (typeof value.message === 'string') {
			serialized.message = value.message;
		}

		if (typeof value.stack === 'string') {
			serialized.stack = value.stack;
		}

		return serialized;
	}

	// People sometimes throw things besides Error objects, so...

	if (typeof value === 'function') {
		// JSON.stringify discards functions. We do to, unless a function is thrown directly.
		return '[Function: ' + (value.name || 'anonymous') + ']';
	}

	return value;
};

// https://www.npmjs.com/package/destroy-circular
function destroyCircular(from, seen) {
	var to;
	if (Array.isArray(from)) {
		to = [];
	} else {
		to = {};
	}

	seen.push(from);

	Object.keys(from).forEach(function (key) {
		var value = from[key];

		if (typeof value === 'function') {
			return;
		}

		if (!value || typeof value !== 'object') {
			to[key] = value;
			return;
		}

		if (seen.indexOf(from[key]) === -1) {
			to[key] = destroyCircular(from[key], seen.slice(0));
			return;
		}

		to[key] = '[Circular]';
	});

	return to;
}
