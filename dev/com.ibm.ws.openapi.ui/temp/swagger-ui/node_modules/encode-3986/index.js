'use strict'

var encode = function (string) {
  return encodeURIComponent(string).replace(/[!'()*]/g, function (c) {
    return '%' + c.charCodeAt(0).toString(16).toUpperCase()
  })
}

module.exports = encode
