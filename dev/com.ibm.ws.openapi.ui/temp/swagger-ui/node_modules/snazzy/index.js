module.exports = CompactToStylishStream

var chalk = require('chalk')
var inherits = require('inherits')
var stream = require('readable-stream')
var standardJson = require('standard-json')
var table = require('text-table')

inherits(CompactToStylishStream, stream.Transform)

function CompactToStylishStream (opts) {
  if (!(this instanceof CompactToStylishStream)) {
    return new CompactToStylishStream(opts)
  }
  stream.Transform.call(this, opts)

  this._buffer = []
}

CompactToStylishStream.prototype._transform = function (chunk, encoding, cb) {
  this._buffer.push(chunk)
  cb(null)
}

CompactToStylishStream.prototype._flush = function (cb) {
  var lines = Buffer.concat(this._buffer).toString()
  var jsonResults = standardJson(lines, {noisey: true})
  var output = processResults(jsonResults)
  this.push(output)

  this.exitCode = output === '' ? 0 : 1
  cb(null)
}

/**
 * Given a word and a count, append an s if count is not one.
 * @param {string} word A word in its singular form.
 * @param {int} count A number controlling whether word should be pluralized.
 * @returns {string} The original word with an s on the end if count is not one.
 */
function pluralize (word, count) {
  return (count === 1 ? word : word + 's')
}

function processResults (results) {
  var output = '\n'
  var total = 0

  results.forEach(function (result) {
    var messages = result.messages

    if (messages.length === 0) {
      return
    }

    total += messages.length
    output += chalk.underline(result.filePath) + '\n'

    output += table(
      messages.map(function (message) {
        var messageType

        messageType = chalk.red('error')

        return [
          '',
          message.line || 0,
          message.column || 0,
          messageType,
          message.message.replace(/\.$/, ''),
          chalk.dim(message.ruleId || '')
        ]
      }),
      {
        align: ['', 'r', 'l'],
        stringLength: function (str) {
          return chalk.stripColor(str).length
        }
      }
    ).split('\n').map(function (el) {
      return el.replace(/(\d+)\s+(\d+)/, function (m, p1, p2) {
        return chalk.dim(p1 + ':' + p2)
      })
    }).join('\n') + '\n\n'
  })

  if (total > 0) {
    output += chalk.red.bold([
      '\u2716 ', total, pluralize(' problem', total), '\n'
    ].join(''))
  }

  return total > 0 ? output : ''
}
