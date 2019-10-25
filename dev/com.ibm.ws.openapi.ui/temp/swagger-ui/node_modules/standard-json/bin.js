#!/usr/bin/env node

var makeJson = require('./index.js')
var concat = require('concat-stream')

process.stdout.on('error', function () {})

var stream = process.stdin

var concatStream = concat({ encoding: 'string' }, function (data) {
  var output = makeJson(data)
  process.exitCode = output.length ? 1 : 0
  console.log(JSON.stringify(output))
})
stream.pipe(concatStream)

stream.on('error', handleError)

function handleError (err) {
  console.error(err)
  process.exit(1)
}
