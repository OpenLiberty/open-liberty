#!/usr/bin/env node

var CompactToStylishStream = require('../')
var cp = require('child_process')
var minimist = require('minimist')
var path = require('path')

var STANDARD_CMD = path.join(require.resolve('standard'), '../../.bin/standard')
if (process.platform === 'win32') STANDARD_CMD += '.cmd'

var argv = minimist(process.argv.slice(2), {
  boolean: [
    'stdin'
  ]
})

var snazzy = new CompactToStylishStream()

// Set the process exit code based on whether snazzy found errors.
process.on('exit', function (code) {
  if (code === 0 && snazzy.exitCode !== 0) {
    process.exit(snazzy.exitCode)
  }
})
process.stdout.on('error', function () {})

if (!process.stdin.isTTY || argv._[0] === '-' || argv.stdin) {
  process.stdin.pipe(snazzy).pipe(process.stdout)
} else {
  var args = process.argv.slice(2)
  var standard = cp.spawn(STANDARD_CMD, args)
  standard.stderr.pipe(process.stderr)
  standard.stdout.pipe(snazzy).pipe(process.stdout)

  // This only runs if snazzy finds no errors AND `standard` exited with a
  // non-zero code. That means something weird happened, so set exit code to
  // non-zero.
  var standardCode
  standard.on('exit', function (code) { standardCode = code })
  process.on('exit', function (code) {
    if (code === 0 && standardCode !== 0) {
      console.error('Unexpected exit from the `standard` command')
      process.exit(standardCode)
    }
  })
}
