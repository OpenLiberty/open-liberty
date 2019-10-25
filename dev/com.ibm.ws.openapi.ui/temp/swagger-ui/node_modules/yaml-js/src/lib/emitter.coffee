events      = require './events'
util        = require './util'
{YAMLError} = require './errors'

class @EmitterError extends YAMLError

###
Emitter expects events obeying the following grammar:

stream   ::= STREAM-START document* STREAM-END
document ::= DOCUMENT-START node DOCUMENT-END
node     ::= SCALA | sequence | mapping
sequence ::= SEQUENCE-START node* SEQUENCE-END
mapping  ::= MAPPING-START (node node)* MAPPING-END
###
class @Emitter
  C_WHITESPACE = '\0 \t\r\n\x85\u2028\u2029'

  DEFAULT_TAG_PREFIXES =
    '!'                 : '!'
    'tag:yaml.org,2002:': '!!'

  ESCAPE_REPLACEMENTS =
    '\0'    :   '0'
    '\x07'  :   'a'
    '\x08'  :   'b'
    '\x09'  :   't'
    '\x0A'  :   'n'
    '\x0B'  :   'v'
    '\x0C'  :   'f'
    '\x0D'  :   'r'
    '\x1B'  :   'e'
    '"'     :   '"'
    '\\'    :   '\\'
    '\x85'  :   'N'
    '\xA0'  :   '_'
    '\u2028':   'L'
    '\u2029':   'P'

  constructor: (@stream, options) ->
    # Encoding can be overriden by STREAM-START
    @encoding = null

    # Emitter is a state machine with a stack of states to handle nested structures.
    @states = []
    @state  = @expect_stream_start

    # Current event and the event queue
    @events = []
    @event  = null

    # The current indentation level and the stack of previous indents.
    @indents = []
    @indent  = null

    # Flow level.
    @flow_level = 0

    # Contexts.
    @root_context       = false
    @sequence_context   = false
    @mapping_context    = false
    @simple_key_context = false

    # Characteristics of the last emitted character:
    # - current position.
    # - is it a whitespace?
    # - is it an indentation character (indentation space, '-', '?', or ':')?
    @line        = 0
    @column      = 0
    @whitespace  = true
    @indentation = true

    # Whether the document requires an explicit document indicator.
    @open_ended = false

    # Formatting details
    { @canonical, @allow_unicode } = options
    @canonical      ?= false
    @allow_unicode  ?= true
    @best_indent     = if 1 < options.indent and options.indent < 10   then options.indent     else 2
    @best_width      = if options.width > @indent * 2                  then options.width      else 80
    @best_line_break = if options.line_break in [ '\r', '\n', '\r\n' ] then options.line_break else '\n'

    # Tag prefixes.
    @tag_prefixes = null

    # Prepared anchor and tag
    @prepared_anchor = null
    @prepared_tag    = null

    # Scalar analysis and style.
    @analysis = null
    @style    = null

  ###
  Reset the state attributes (to clear self-references)
  ###
  dispose: ->
    @states = []
    @state  = null

  emit: (event) ->
    @events.push event
    until @need_more_events()
      @event = @events.shift()
      @state()
      @event = null

  ###
  In some cases, we wait for a few next events before emitting.
  ###
  need_more_events: ->
    return true if @events.length is 0

    event = @events[0]
    if      event instanceof events.DocumentStartEvent then @need_events 1
    else if event instanceof events.SequenceStartEvent then @need_events 2
    else if event instanceof events.MappingStartEvent  then @need_events 3
    else                                               false

  need_events: (count) ->
    level = 0
    for event in @events[1..]
      if event instanceof events.DocumentStartEvent or event instanceof events.CollectionStartEvent
        level++
      else if event instanceof events.DocumentEndEvent or event instanceof events.CollectionEndEvent
        level--
      else if event instanceof events.StreamEndEvent
        level = -1
      return false if level < 0
    @events.length < count + 1

  increase_indent: (options = {}) ->
    @indents.push @indent
    if not @indent?
      @indent = if options.flow then @best_indent else 0
    else if not options.indentless
      @indent += @best_indent

  # Stream states

  expect_stream_start: ->
    if @event instanceof events.StreamStartEvent
      @encoding = @event.encoding if @event.encoding and 'encoding' not of @stream
      @write_stream_start()
      @state = @expect_first_document_start
    else
      @error 'expected StreamStartEvent, but got', @event

  expect_nothing: ->
    @error 'expected nothing, but got', @event

  # Document states

  expect_first_document_start: ->
    @expect_document_start true

  expect_document_start: (first = false) ->
    if @event instanceof events.DocumentStartEvent
      if (@event.version or @event.tags) and @open_ended
        @write_indicator '...', true
        @write_indent()

      if @event.version
        @write_version_directive @prepare_version @event.version

      @tag_prefixes = util.clone DEFAULT_TAG_PREFIXES
      if @event.tags
        for handle in (k for own k of @event.tags).sort()
          prefix                = @event.tags[handle]
          @tag_prefixes[prefix] = handle
          @write_tag_directive @prepare_tag_handle(handle), @prepare_tag_prefix(prefix)

      explicit = not first or @event.explicit or @canonical or @event.version or @event.tags or @check_empty_document()
      if explicit
        @write_indent()
        @write_indicator '---', true
        @write_indent() if @canonical

      @state = @expect_document_root
    else if @event instanceof events.StreamEndEvent
      if @open_ended
        @write_indicator '...', true
        @write_indent()
      @write_stream_end()
      @state = @expect_nothing
    else
      @error 'expected DocumentStartEvent, but got', @event

  expect_document_end: ->
    if @event instanceof events.DocumentEndEvent
      @write_indent()
      if @event.explicit
        @write_indicator '...', true
        @write_indent()
      @flush_stream()
      @state = @expect_document_start
    else
      @error 'expected DocumentEndEvent, but got', @event

  expect_document_root: ->
    @states.push @expect_document_end
    @expect_node root: true

  # Node states

  expect_node: (expect = {}) ->
    @root_context       = !!expect.root
    @sequence_context   = !!expect.sequence
    @mapping_context    = !!expect.mapping
    @simple_key_context = !!expect.simple_key

    if @event instanceof events.AliasEvent
      @expect_alias()
    else if @event instanceof events.ScalarEvent or @event instanceof events.CollectionStartEvent
      @process_anchor '&'
      @process_tag()

      if @event instanceof events.ScalarEvent
        @expect_scalar()
      else if @event instanceof events.SequenceStartEvent
        if @flow_level or @canonical or @event.flow_style or @check_empty_sequence()
          @expect_flow_sequence()
        else
          @expect_block_sequence()
      else if @event instanceof events.MappingStartEvent
        if @flow_level or @canonical or @event.flow_style or @check_empty_mapping()
          @expect_flow_mapping()
        else
          @expect_block_mapping()
    else
      @error 'expected NodeEvent, but got', @event

  expect_alias: ->
    @error 'anchor is not specified for alias' unless @event.anchor
    @process_anchor '*'
    @state = @states.pop()

  expect_scalar: ->
    @increase_indent flow: true
    @process_scalar()
    @indent = @indents.pop()
    @state  = @states.pop()

  # Flow sequence states

  expect_flow_sequence: ->
    @write_indicator '[', true, whitespace: true
    @flow_level++
    @increase_indent flow: true
    @state = @expect_first_flow_sequence_item

  expect_first_flow_sequence_item: ->
    if @event instanceof events.SequenceEndEvent
      @indent = @indents.pop()
      @flow_level--
      @write_indicator ']', false
      @state = @states.pop()
    else
      @write_indent() if @canonical or @column > @best_width
      @states.push @expect_flow_sequence_item
      @expect_node sequence: true

  expect_flow_sequence_item: ->
    if @event instanceof events.SequenceEndEvent
      @indent = @indents.pop()
      @flow_level--
      if @canonical
        @write_indicator ',', false
        @write_indent()
      @write_indicator ']', false
      @state = @states.pop()
    else
      @write_indicator ',', false
      @write_indent() if @canonical or @column > @best_width
      @states.push @expect_flow_sequence_item
      @expect_node sequence: true

  # Flow mapping states

  expect_flow_mapping: ->
    @write_indicator '{', true, whitespace: true
    @flow_level++
    @increase_indent flow: true
    @state = @expect_first_flow_mapping_key

  expect_first_flow_mapping_key: ->
    if @event instanceof events.MappingEndEvent
      @indent = @indents.pop()
      @flow_level--
      @write_indicator '}', false
      @state = @states.pop()
    else
      @write_indent() if @canonical or @column > @best_width

      if not @canonical and @check_simple_key()
        @states.push @expect_flow_mapping_simple_value
        @expect_node mapping: true, simple_key: true
      else
        @write_indicator '?', true
        @states.push @expect_flow_mapping_value
        @expect_node mapping: true

  expect_flow_mapping_key: ->
    if @event instanceof events.MappingEndEvent
      @indent = @indents.pop()
      @flow_level--
      if @canonical
        @write_indicator ',', false
        @write_indent()
      @write_indicator '}', false
      @state = @states.pop()
    else
      @write_indicator ',', false
      @write_indent() if @canonical or @column > @best_width

      if not @canonical and @check_simple_key()
        @states.push @expect_flow_mapping_simple_value
        @expect_node mapping: true, simple_key: true
      else
        @write_indicator '?', true
        @states.push @expect_flow_mapping_value
        @expect_node mapping: true

  expect_flow_mapping_simple_value: ->
    @write_indicator ':', false
    @states.push @expect_flow_mapping_key
    @expect_node mapping: true

  expect_flow_mapping_value: ->
    @write_indent() if @canonical or @column > @best_width
    @write_indicator ':', true
    @states.push @expect_flow_mapping_key
    @expect_node mapping: true

  # Block sequence states

  expect_block_sequence: ->
    indentless = @mapping_context and not @indentation
    @increase_indent { indentless }
    @state = @expect_first_block_sequence_item

  expect_first_block_sequence_item: ->
    @expect_block_sequence_item true

  expect_block_sequence_item: (first = false) ->
    if not first and @event instanceof events.SequenceEndEvent
      @indent = @indents.pop()
      @state  = @states.pop()
    else
      @write_indent()
      @write_indicator '-', true, indentation: true
      @states.push @expect_block_sequence_item
      @expect_node sequence: true

  # Block mapping states

  expect_block_mapping: ->
    @increase_indent()
    @state = @expect_first_block_mapping_key

  expect_first_block_mapping_key: ->
    @expect_block_mapping_key true

  expect_block_mapping_key: (first = false) ->
    if not first and @event instanceof events.MappingEndEvent
      @indent = @indents.pop()
      @state  = @states.pop()
    else
      @write_indent()
      if @check_simple_key()
        @states.push @expect_block_mapping_simple_value
        @expect_node mapping: true, simple_key: true
      else
        @write_indicator '?', true, indentation: true
        @states.push @expect_block_mapping_value
        @expect_node mapping: true

  expect_block_mapping_simple_value: ->
    @write_indicator ':', false
    @states.push @expect_block_mapping_key
    @expect_node mapping: true

  expect_block_mapping_value: ->
    @write_indent()
    @write_indicator ':', true, indentation: true
    @states.push @expect_block_mapping_key
    @expect_node mapping: true

  # Checkers

  check_empty_document: ->
    return false if @event not instanceof events.DocumentStartEvent or @events.length is 0

    event = @events[0]
    event instanceof events.ScalarEvent and not event.anchor? and not event.tag? and event.implicit and event.value is ''

  check_empty_sequence: ->
    @event instanceof events.SequenceStartEvent and @events[0] instanceof events.SequenceEndEvent

  check_empty_mapping: ->
    @event instanceof events.MappingStartEvent  and @events[0] instanceof events.MappingEndEvent

  check_simple_key: ->
    length = 0
    if @event instanceof events.NodeEvent and @event.anchor?
      @prepared_anchor ?= @prepare_anchor @event.anchor
      length += @prepared_anchor.length
    if @event.tag? and (@event instanceof events.ScalarEvent or @event instanceof events.CollectionStartEvent)
      @prepared_tag ?= @prepare_tag @event.tag
      length += @prepared_tag.length
    if @event instanceof events.ScalarEvent
      @analysis ?= @analyze_scalar @event.value
      length += @analysis.scalar.length

    length < 128 and (
      @event instanceof events.AliasEvent or
      (@event instanceof events.ScalarEvent and not @analysis.empty and not @analysis.multiline) or
      @check_empty_sequence() or @check_empty_mapping()
    )

  # Anchor, Tag and Scalar processors

  process_anchor: (indicator) ->
    unless @event.anchor?
      @prepared_anchor = null
      return

    @prepared_anchor ?= @prepare_anchor @event.anchor
    @write_indicator "#{indicator}#{@prepared_anchor}", true if @prepared_anchor
    @prepared_anchor = null

  process_tag: ->
    tag = @event.tag
    if @event instanceof events.ScalarEvent
      @style ?= @choose_scalar_style()
      if (not @canonical or not tag?) and ((@style is '' and @event.implicit[0]) or \
          (@style isnt '' and @event.implicit[1]))
        @prepared_tag = null
        return
      if @event.implicit[0] and not tag?
        tag = '!'
        @prepared_tag = null
    else if (not @canonical or not tag?) and @event.implicit
      @prepared_tag = null
      return

    @error 'tag is not specified' if not tag?
    @prepared_tag ?= @prepare_tag tag
    @write_indicator @prepared_tag, true
    @prepared_tag = null

  process_scalar: ->
    @analysis ?= @analyze_scalar @event.value
    @style    ?= @choose_scalar_style()

    split = not @simple_key_context
    switch @style
      when '"' then @write_double_quoted @analysis.scalar, split
      when "'" then @write_single_quoted @analysis.scalar, split
      when '>' then @write_folded @analysis.scalar
      when '|' then @write_literal @analysis.scalar
      else          @write_plain @analysis.scalar, split

    @analysis = null
    @style    = null

  choose_scalar_style: ->
    @analysis ?= @analyze_scalar @event.value

    return '"' if @event.style is '"' or @canonical

    return '' if not @event.style and @event.implicit[0] \
      and not (@simple_key_context and (@analysis.empty or @analysis.multiline)) \
      and ((@flow_level and @analysis.allow_flow_plain) \
      or (not @flow_level and @analysis.allow_block_plain))

    return @event.style if @event.style and @event.style in '|>' and not @flow_level \
      and not @simple_key_context and @analysis.allow_block

    return "'" if (not @event.style or @event.style is "'") and @analysis.allow_single_quoted \
      and not (@simple_key_context and @analysis.multiline)

    return '"'

  # Analyzers

  prepare_version: ([ major, minor ]) ->
    version = "#{major}.#{minor}"
    if major is 1 then version else @error 'unsupported YAML version', version

  prepare_tag_handle: (handle) ->
    unless handle
      @error 'tag handle must not be empty'
    if handle[0] isnt '!' or handle[-1..] isnt '!'
      @error "tag handle must start and end with '!':", handle
    for char in handle[1...-1]
      unless '0' <= char <= '9' or 'A' <= char <= 'Z' or 'a' <= char <= 'z' or char in '-_'
        @error "invalid character '#{char}' in the tag handle:", handle

    handle

  prepare_tag_prefix: (prefix) ->
    @error 'tag prefix must not be empty' unless prefix

    chunks = []
    start  = 0
    end    = +(prefix[0] is '!')
    while end < prefix.length
      char = prefix[end]
      if '0' <= char <= '9' or 'A' <= char <= 'Z' or 'a' <= char <= 'z' or char in '-;/?!:@&=+$,_.~*\'()[]'
        end++
      else
        chunks.push prefix[start...end] if start < end
        start = end = end + 1
        chunks.push char
    chunks.push prefix[start...end] if start < end
    chunks.join ''

  prepare_tag: (tag) ->
    @error 'tag must not be empty' unless tag
    return tag if tag is '!'

    handle = null
    suffix = tag
    for prefix in (k for own k of @tag_prefixes).sort()
      if tag.indexOf(prefix) is 0 and (prefix is '!' or prefix.length < tag.length)
        handle = @tag_prefixes[prefix]
        suffix = tag[prefix.length..]

    chunks = []
    start  = end = 0
    while end < suffix.length
      char = suffix[end]
      if '0' <= char <= '9' or 'A' <= char <= 'Z' or 'a' <= char <= 'z' or \
          char in '-;/?!:@&=+$,_.~*\'()[]' or (char is '!' and handle isnt '!')
        end++
      else
        chunks.push suffix[start...end] if start < end
        start = end = end + 1
        chunks.push char
    chunks.push suffix[start...end] if start < end

    suffix_text = chunks.join ''
    if handle then "#{handle}#{suffix_text}" else "!<#{suffix_text}>"

  prepare_anchor: (anchor) ->
    @error 'anchor must not be empty' unless anchor
    for char in anchor
      unless '0' <= char <= '9' or 'A' <= char <= 'Z' or 'a' <= char <= 'z' or char in '-_'
        @error "invalid character '#{char}' in the anchor:", anchor
    anchor

  analyze_scalar: (scalar) ->
    # Empty scalar is a special case.
    unless scalar
      new ScalarAnalysis(scalar, true, false, false, true, true, true, false)

    # Indicators and special characters.
    block_indicators   = false
    flow_indicators    = false
    line_breaks        = false
    special_characters = false
    unicode_characters = false

    # Important whitespace combinations
    leading_space  = false
    leading_break  = false
    trailing_space = false
    trailing_break = false
    break_space    = false
    space_break    = false

    # Check document indicators.
    if scalar.indexOf('---') is 0 or scalar.indexOf('...') is 0
      block_indicators = true
      flow_indicators  = true

    # First character or preceded by a whitespace.
    preceded_by_whitespace = true

    # Last character or followed by a whitespace.
    followed_by_whitespace = scalar.length is 1 or scalar[1] in '\0 \t\r\n\x85\u2028\u2029'

    # The previous character is a space.
    previous_space = false

    # The previous character is a break
    previous_break = false

    index = 0
    for char, index in scalar
      # Check for indicators.
      if index is 0
        # Leading indicators are special characters.
        if char in '#,[]{}&*!|>\'"%@`' or (char is '-' and followed_by_whitespace)
          flow_indicators  = true
          block_indicators = true
        else if char in '?:'
          flow_indicators  = true
          block_indicators = true if followed_by_whitespace
      else
        # Some indicators cannot appear within a scalar as well.
        if char in ',?[]{}'
          flow_indicators = true
        else if char is ':'
          flow_indicators  = true
          block_indicators = true if followed_by_whitespace
        else if char is '#' and preceded_by_whitespace
          flow_indicators  = true
          block_indicators = true

      # Check for line breaks, special, and unicode characters.
      if char in '\n\x85\u2028\u2029'
        line_breaks = true
      unless char is '\n' or '\x20' <= char <= '\x7e'
        if char isnt '\uFEFF' and (char is '\x85' or '\xA0' <= char <= '\uD7FF' or '\uE000' <= char <= '\uFFFD')
          unicode_characters = true
          special_characters = true unless @allow_unicode
        else
          special_characters = true

      # Detect important whitespace combinations.
      if char is ' '
        leading_space  = true if index is 0
        trailing_space = true if index == scalar.length - 1
        break_space    = true if previous_break
        previous_break = false
        previous_space = true
      else if char in '\n\x85\u2028\u2029'
        leading_break  = true if index is 0
        trailing_break = true if index == scalar.length - 1
        space_break    = true if previous_space
        previous_break = true
        previous_space = false
      else
        previous_break = false
        previous_space = false

      # Prepare for the next character.
      preceded_by_whitespace = char in C_WHITESPACE
      followed_by_whitespace = index + 2 >= scalar.length or scalar[index + 2] in C_WHITESPACE

    # Let's decide what styles are allowed.
    allow_flow_plain    = true
    allow_block_plain   = true
    allow_single_quoted = true
    allow_double_quoted = true
    allow_block         = true

    # Leading and trailing whitespaces are bad for plain scalars.
    if leading_space or leading_break or trailing_space or trailing_break
      allow_flow_plain = allow_block_plain = false

    # We do not permit trailing spaces for block scalars.
    if trailing_space
      allow_block = false

    # Spaces at the beginning of a new line are only acceptable for block scalars.
    if break_space
      allow_flow_plain = allow_block_plain = allow_single_quoted = false

    # Spaces followed by breaks, as well as special character are only allowed for double quoted
    # scalars.
    if space_break or special_characters
      allow_flow_plain = allow_block_plain = allow_single_quoted = allow_block = false

    # Although the plain scalar writer supports breaks, we never emit multiline plain scalars.
    if line_breaks
      allow_flow_plain = allow_block_plain = false

    # Flow indicators are forbidden for flow plain scalars.
    if flow_indicators
      allow_flow_plain = false

    # Block indicators are forbidden for block plain scalars.
    if block_indicators
      allow_block_plain = false

    new ScalarAnalysis scalar, false, line_breaks, allow_flow_plain, allow_block_plain,
      allow_single_quoted, allow_double_quoted, allow_block

  # Writers

  ###
  Write BOM if needed.
  ###
  write_stream_start: ->
    if @encoding and @encoding.indexOf('utf-16') is 0
      @stream.write '\uFEFF', @encoding

  write_stream_end: ->
    @flush_stream()

  write_indicator: (indicator, need_whitespace, options = {}) ->
    data = if @whitespace or not need_whitespace
      indicator
    else
      ' ' + indicator

    @whitespace     = !!options.whitespace
    @indentation and= !!options.indentation
    @column        += data.length
    @open_ended     = false
    @stream.write data, @encoding

  write_indent: ->
    indent = @indent ? 0
    if not @indentation or @column > indent or (@column == indent and not @whitespace)
      @write_line_break()
    if @column < indent
      @whitespace = true
      data    = new Array(indent - @column + 1).join ' '
      @column = indent
      @stream.write data, @encoding

  write_line_break: (data) ->
    @whitespace  = true
    @indentation = true
    @line       += 1
    @column      = 0
    @stream.write data ? @best_line_break, @encoding

  write_version_directive: (version_text) ->
    @stream.write "%YAML #{version_text}", @encoding
    @write_line_break()

  write_tag_directive: (handle_text, prefix_text) ->
    @stream.write "%TAG #{handle_text} #{prefix_text}", @encoding
    @write_line_break()

  write_single_quoted: (text, split = true) ->
    @write_indicator "'", true
    spaces = false
    breaks = false
    start  = end = 0
    while end <= text.length
      char = text[end]
      if spaces
        if not char? or char isnt ' '
          if start + 1 == end and @column > @best_width and split and start isnt 0 and end != text.length
            @write_indent()
          else
            data     = text[start...end]
            @column += data.length
            @stream.write data, @encoding
          start = end
      else if breaks
        if not char? or char not in '\n\x85\u2028\u2029'
          @write_line_break() if text[start] is '\n'
          for br in text[start...end]
            if br is '\n'
              @write_line_break()
            else
              @write_line_break br
          @write_indent()
          start = end
      else if (not char? or char in ' \n\x85\u2028\u2029' or char is "'") and start < end
        data     = text[start...end]
        @column += data.length
        @stream.write data, @encoding
        start = end

      if char is "'"
        @column += 2
        @stream.write "''", @encoding
        start = end + 1

      if char?
        spaces = char is ' '
        breaks = char in '\n\x85\u2028\u2029'

      end++
    @write_indicator "'", false

  write_double_quoted: (text, split = true) ->
    @write_indicator '"', true
    start = end = 0
    while end <= text.length
      char = text[end]
      if not char? or char in '"\\\x85\u2028\u2029\uFEFF' or \
         not ('\x20' <= char <= '\x7E' or (@allow_unicode and \
             ('\xA0' <= char <= '\uD7FF' or '\uE000' <= char <= '\uFFFD')))
        if start < end
          data     = text[start...end]
          @column += data.length
          @stream.write data, @encoding
          start = end
        if char?
          data = if char of ESCAPE_REPLACEMENTS
            '\\' + ESCAPE_REPLACEMENTS[char]
          else if char <= '\xFF'
            "\\x#{util.pad_left util.to_hex(char), '0', 2}"
          else if char <= '\uFFFF'
            "\\u#{util.pad_left util.to_hex(char), '0', 4}"
          else
            "\\U#{util.pad_left util.to_hex(char), '0', 16}"
          @column += data.length
          @stream.write data, @encoding
          start = end + 1
      if split and 0 < end < text.length - 1 and (char is ' ' or start >= end) and \
         @column + (end - start) > @best_width
        data     = "#{text[start...end]}\\"
        start    = end if start < end
        @column += data.length
        @stream.write data, @encoding
        @write_indent()
        @whitespace  = false
        @indentation = false
        if text[start] is ' '
          data = '\\'
          @column += data.length
          @stream.write data, @encoding
      end++
    @write_indicator '"', false

  write_folded: (text) ->
    hints = @determine_block_hints text
    @write_indicator ">#{hints}", true
    @open_ended = true if hints[-1..] is '+'
    @write_line_break()
    leading_space = true
    breaks        = true
    spaces        = false
    start         = end = 0
    while end <= text.length
      char = text[end]
      if breaks
        if not char? or char not in '\n\x85\u2028\u2029'
          if not leading_space and char? and char isnt ' ' and text[start] is '\n'
            @write_line_break()
          leading_space = char is ' '
          for br in text[start...end]
            if br is '\n'
              @write_line_break()
            else
              @write_line_break br
          @write_indent() if char?
          start = end
      else if spaces
        if char isnt ' '
          if start + 1 == end and @column > @best_width
            @write_indent()
          else
            data     = text[start...end]
            @column += data.length
            @stream.write data, @encoding
          start = end
      else if not char? or char in ' \n\x85\u2028\u2029'
        data     = text[start...end]
        @column += data.length
        @stream.write data, @encoding
        @write_line_break() if not char?
        start    = end

      if char?
        breaks = char in '\n\x85\u2028\u2029'
        spaces = char is ' '

      end++

  write_literal: (text) ->
    hints = @determine_block_hints text
    @write_indicator "|#{hints}", true
    @open_ended = true if hints[-1..] is '+'
    @write_line_break()
    breaks = true
    start  = end = 0
    while end <= text.length
      char = text[end]
      if breaks
        if not char? or char not in '\n\x85\u2028\u2029'
          for br in text[start...end]
            if br is '\n'
              @write_line_break()
            else
              @write_line_break br
          @write_indent() if char?
          start = end
      else
        if not char? or char in '\n\x85\u2028\u2029'
          data = text[start...end]
          @stream.write data, @encoding
          @write_line_break() if not char?
          start = end


      breaks = char in '\n\x85\u2028\u2029' if char?
      end++

  write_plain: (text, split = true) ->
    unless text then return

    @open_ended = true if @root_context

    unless @whitespace
      data = ' '
      @column += data.length
      @stream.write data, @encoding

    @whitespace  = false
    @indentation = false
    spaces       = false
    breaks       = false
    start        = end = 0
    while end <= text.length
      char = text[end]
      if spaces
        if char isnt ' '
          if start + 1 == end and @column > @best_width and split
            @write_indent()
            @whitespace  = false
            @indentation = false
          else
            data     = text[start...end]
            @column += data.length
            @stream.write data, @encoding
          start = end
      else if breaks
        if char not in '\n\x85\u2028\u2029'
          @write_line_break() if text[start] is '\n'
          for br in text[start...end]
            if br is '\n'
              @write_line_break()
            else
              @write_line_break br
          @write_indent()
          @whitespace  = false
          @indentation = false
          start        = end
      else
        if not char? or char in ' \n\x85\u2028\u2029'
          data     = text[start...end]
          @column += data.length
          @stream.write data, @encoding
          start    = end

      if char?
        spaces = char is ' '
        breaks = char in '\n\x85\u2028\u2029'

      end++

  determine_block_hints: (text) ->
    hints = ''
    [ first, ..., penultimate, last ] = text
    if first in ' \n\x85\u2028\u2029'
      hints += @best_indent
    if last not in '\n\x85\u2028\u2029'
      hints += '-'
    else if text.length is 1 or penultimate in '\n\x85\u2028\u2029'
      hints += '+'
    hints

  flush_stream: ->
    @stream.flush?()

  ###
  Helper for common error pattern.
  ###
  error: (message, context) ->
    context = context?.constructor?.name ? util.inspect context if context
    throw new exports.EmitterError "#{message}#{if context then " #{context}" else ''}"

class ScalarAnalysis
  constructor: (@scalar, @empty, @multiline, @allow_flow_plain, @allow_block_plain,
      @allow_single_quoted, @allow_double_quoted, @allow_block) ->