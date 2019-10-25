module.exports = jsonify

function jsonify (rawtext, opts) {
  opts = opts || { noisey: false }
  var lines = rawtext.split('\n')
  if (lines[lines.length - 1] === '') lines.pop()

  var results = []
  var resultMap = {}
  lines.forEach(function (line) {
    var re = /\s*([A-Za-z]:)?([^:]+):([^:]+):([^:]+): (.*?)( \((.*)\))?$/.exec(line)
    if (!re) return opts.noisey ? console.error(line) : null
    if (re[1] === undefined) re[1] = ''

    var filePath = re[1] + re[2]

    var result = resultMap[filePath]
    if (!result) {
      result = resultMap[filePath] = {
        filePath: re[1] + re[2],
        messages: []
      }
      results.push(result)
    }

    result.messages.push({
      line: re[3],
      column: re[4],
      message: re[5].trim(),
      ruleId: re[7]
    })
  })

  return results
}
