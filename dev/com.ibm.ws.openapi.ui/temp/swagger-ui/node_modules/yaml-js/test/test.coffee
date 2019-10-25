yaml = require '..'
require '../register'

spec =
  generic:    require './yaml-spec/spec'
  javascript: require './yaml-spec/platform/javascript'

each_test = (impl) ->
  for type, suite of spec
    for name, tests of suite
      describe name, ->
        for test, i in tests then do (test) ->
          it "##{i + 1}", ->
            impl.call test

expect_equal = (a, b) ->
  if a != a
    # NaN is the only value that does not equal itself, so if `a !== a` and `b !== b` then
    # a and b are NaN (and therefore equal...)
    expect( b ).not.to.equal b
  else
    expect( b ).to.deep.equal a

describe 'load', ->
  each_test ->
    expect_equal @result, yaml.load(@yaml)

describe 'emit', ->
  each_test ->
    expect_equal @result, yaml.load(yaml.emit yaml.parse @yaml)

describe 'serialize', ->
  each_test ->
    expect_equal @result, yaml.load(yaml.serialize yaml.compose @yaml)

describe 'dump', ->
  each_test ->
    expect_equal @result, yaml.load(yaml.dump yaml.load @yaml)

describe 'dump (formatting)', ->
  [ examples ] = require './format-spec'
  for { input, output }, i in examples then do (input, output, i) ->
    for [ options, result ], j in output then do (options, result, j) ->
      it "input #{i} options #{j}", ->
        expect_equal yaml.dump(yaml.load(input), null, null, options), result