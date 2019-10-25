# git-describe

[![npm version](https://img.shields.io/npm/v/git-describe.svg)](https://www.npmjs.com/package/git-describe)
[![npm license](https://img.shields.io/npm/l/git-describe.svg)](https://www.npmjs.com/package/git-describe)
[![travis build](https://travis-ci.org/tvdstaaij/node-git-describe.svg?branch=master)](https://travis-ci.org/tvdstaaij/node-git-describe)
[![vulnerabilities](https://snyk.io/test/npm/git-describe/badge.svg)](https://snyk.io/test/npm/git-describe)

This Node.js module runs [`git describe`][1] on the working directory or any
other directory and parses the output to individual components. Additionally,
if your tags follow [semantic versioning][2] the semver will be parsed and
supplemented with the git-specific information as build metadata.

## Installation

Available from npm:
`npm install git-describe`

Tests are not included in the npm package — 
clone the git repository to run tests (Node.js 4+ required).

As of version 4.0.0, `semver` is an optional dependency that does not have to 
be installed if you do not require semver functionality.

Note that the `git` executable must be in the system's executable path for this module to function.

## Usage

The module exposes two functions: 

* `gitDescribe(directory, options, cb) -> Promise`
* `gitDescribeSync(directory, options) -> Object`

The only difference is that `gitDescribe` has an asynchronous API 
(either the callback argument or the returned promise can be used), whilst 
`gitDescribeSync` is fully synchronous 
(blocks until the git executable returns and throws an `Error` on failure).

Both functions can take a `directory` string (defaults to working directory)
and an `options` object. Either or both arguments can be omitted.

```javascript
const {gitDescribe, gitDescribeSync} = require('git-describe');

// Target working directory
const gitInfo = gitDescribeSync();

// Target the directory of the calling script
// Recommended when you want to target the repo your app resides in
const gitInfo = gitDescribeSync(__dirname);

// With options (see below)
const gitInfo = gitDescribeSync(__dirname, {
    longSemver: true,
    dirtySemver: false
});

// Another example: working directory, use 16 character commit hash abbreviation
const gitInfo = gitDescribeSync({
    customArguments: ['--abbrev=16']
});

// Asynchronous with promise
gitDescribe(__dirname)
    .then((gitInfo) => console.dir(gitInfo))
    .catch((err) => console.error(err));

// Asynchronous with node-style callback
gitDescribe(__dirname, (err, gitInfo) => {
    if (err)
        return console.error(err);
    console.dir(gitInfo);
});
```

## Example output

```javascript
{ 
    dirty: false,
    hash: 'g3c9c15b',
    distance: 6,
    tag: 'v2.1.0-beta',
    semver: SemVer, // SemVer instance, see https://github.com/npm/node-semver
    suffix: '6-g3c9c15b',
    raw: 'v2.1.0-beta-6-g3c9c15b',
    semverString: '2.1.0-beta+6.g3c9c15b'
}
```

## Options

Option             | Default     | Description
------------------ | ----------- | -----------
`dirtyMark`        | `'-dirty'`  | Dirty mark to use if repo state is dirty (see git describe's `--dirty`).
`dirtySemver`      | `true`      | Appends the dirty mark to `semverString` if repo state is dirty.
`longSemver`       | `false`     | Always adds commit distance and hash to `semverString` (similar to git describe's `--long`).
`requireAnnotated` | `false`     | Uses `--tags` if false, so that simple git tags are allowed.
`match`            | `'v[0-9]*'` | Uses `--match` to filter tag names. By default only tags resembling a version number are considered.
`customArguments`  | `[]`        | Array of additional arguments to pass to `git describe`. Not all arguments are useful and some may even break the library, but things like `--abbrev` and `--candidates` should be safe to add.

[1]: https://git-scm.com/docs/git-describe
[2]: http://semver.org/
