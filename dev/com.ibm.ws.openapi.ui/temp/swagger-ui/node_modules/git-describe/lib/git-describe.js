'use strict';

var _ = require('lodash');
var child_process = require('child_process');
try {
    var semver = require('semver');
} catch (err) {
    semver = null;
}

module.exports = function gitDescribe(directory, options, cb) {
    if (_.isFunction(options)) {
        cb = options;
        options = {};
    } else if (_.isFunction(directory)) {
        cb = directory;
        directory = undefined;
        options = {};
    }
    if (!_.isString(directory) && _.isObject(directory)) {
        options = directory;
        directory = undefined;
    }
    options = _.defaults({}, options, {
        dirtySemver: true,
        longSemver: false,
        requireAnnotated: false,
        match: 'v[0-9]*',
        customArguments: []
    });
    options.dirtyMark =
        (_.isString(options.dirtyMark) && !_.isEmpty(options.dirtyMark)) ?
        options.dirtyMark : '-dirty';

    var resultParser = _.partialRight(parseDescription, options);
    var resultHandler = _.partialRight(handleProcessResult, resultParser);
    var execFunc = _.isFunction(cb) ?
        child_process.execFile : child_process.spawnSync;
    var execArgs = createExecArgs(directory, options);
    execFunc = _.partial(execFunc, 'git', execArgs, {});

    if (_.isFunction(cb)) {
        execFunc(_.partialRight(resultHandler, cb));
    } else {
        var result = execFunc();
        return resultHandler(result, result.stdout, result.stderr, null);
    }
};

function createExecArgs(directory, options) {
    var execArgs = [];
    if (_.isString(directory))
        execArgs = execArgs.concat(['-C', directory]);
    execArgs = execArgs.concat([
        'describe', '--long', '--dirty=' + options.dirtyMark, '--always'
    ]);
    if (options.requireAnnotated === false)
        execArgs.push('--tags');
    if (_.isString(options.match))
        execArgs = execArgs.concat(['--match', options.match]);
    if (_.isArray(options.customArguments))
        execArgs = execArgs.concat(options.customArguments);
    return execArgs;
}

function handleProcessResult(result, stdout, stderr, cb, parser) {
    if (result && result.status !== 0) {
        var code = result.status || result.code;
        var errMsg = 'Git returned with status ' + code + ': ' +
            stderr.toString().trim();
        switch (code) {
        case 'ENOENT':
            errMsg = 'Git executable not found in PATH (' + process.env.PATH + ')';
            break;
        }
        var err = new Error(errMsg);
        if (_.isFunction(cb)) {
            cb(err);
            return;
        }
        throw err;
    }
    var parsedResult = parser(stdout.toString().trim());
    if (_.isFunction(cb)) {
        cb(null, parsedResult);
        return;
    }
    return parsedResult;
}

function parseDescription(description, options) {
    var output = {
        dirty: false,
        raw: description,
        hash: null, distance: null, tag: null, semver: null, suffix: null,
        semverString: null
    };

    if (_.endsWith(description, options.dirtyMark)) {
        output.dirty = true;
        description =
            description.substring(0, description.indexOf(options.dirtyMark));
    }

    var tokens = description.split('-');
    var suffixTokens = [];
    output.hash = tokens.pop();
    suffixTokens.unshift(output.hash);
    // Skip this part in the --always fallback case (i.e. no tags found)
    if (!_.isEmpty(tokens)) {
        output.distance = Number(tokens.pop());
        suffixTokens.unshift(output.distance);
        output.tag = tokens.join('-');
        output.semver = semver ? semver.parse(output.tag) : null;
        output.semverString = '';
        var build = '';
        var appendDirty = options.dirtySemver && output.dirty;
        if (output.distance || options.longSemver || appendDirty) {
            build = String(output.distance) + '.' + output.hash;
            if (appendDirty)
                build += '.' + options.dirtyMark.replace(/[^a-z0-9.]/ig, '');
        }
        if (output.semver) {
		    output.semverString = output.semver.format();
            if (build)
                output.semverString += '+' + build;
        }
    }
    output.suffix = suffixTokens.join('-');
    if (output.dirty) output.suffix += options.dirtyMark;
    output.toString = function() { return output.raw; };
    return output;
}
