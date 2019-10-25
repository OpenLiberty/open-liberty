###
A small class to stand-in for a stream when you simply want to write to a string.
###
class @StringStream
  constructor: ->
    @string = ''

  write: (chunk) ->
    @string += chunk

@clone = (obj) =>
  @extend {}, obj

@extend = (destination, sources...) ->
  destination[k] = v for k, v of source for source in sources
  destination

@is_empty = (obj) ->
  return obj.length is 0 if Array.isArray(obj) or typeof obj is 'string'
  return false for own key of obj
  return true

@inspect = require('util')?.inspect ? global.inspect ? (a) -> "#{a}"

@pad_left = (str, char, length) ->
  str = String str
  if str.length >= length
    str
  else if str.length + 1 == length
    "#{char}#{str}"
  else
    "#{new Array(length - str.length + 1).join char}#{str}"

@to_hex = (num) ->
  num    = num.charCodeAt 0 if typeof num is 'string'
  num.toString 16