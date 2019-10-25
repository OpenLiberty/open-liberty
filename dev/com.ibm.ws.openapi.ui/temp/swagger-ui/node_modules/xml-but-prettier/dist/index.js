'use strict';

var repeat = require('repeat-string');

var splitOnTags = function splitOnTags(str) {
  return str.split(/(<\/?[^>]+>)/g).filter(function (line) {
    return line.trim() !== '';
  });
};
var isTag = function isTag(str) {
  return (/<[^>!]+>/.test(str)
  );
};
var isClosingTag = function isClosingTag(str) {
  return (/<\/+[^>]+>/.test(str)
  );
};
var isSelfClosingTag = function isSelfClosingTag(str) {
  return (/<[^>]+\/>/.test(str)
  );
};
var isOpeningTag = function isOpeningTag(str) {
  return isTag(str) && !isClosingTag(str) && !isSelfClosingTag(str);
};

module.exports = function (xml) {
  var config = arguments.length > 1 && arguments[1] !== undefined ? arguments[1] : {};
  var indentor = config.indentor,
      textNodesOnSameLine = config.textNodesOnSameLine;

  var depth = 0;
  var indicesToRemove = [];
  indentor = indentor || '    ';

  var rawResult = lexer(xml).map(function (element, i, arr) {
    var value = element.value,
        type = element.type;

    if (type === 'ClosingTag') {
      depth--;
    }

    var indentation = repeat(indentor, depth);
    var line = indentation + value;

    if (type === 'OpeningTag') {
      depth++;
    }

    if (textNodesOnSameLine) {
      // Lookbehind for [OpeningTag][Text][ClosingTag]
      var oneBefore = arr[i - 1];
      var twoBefore = arr[i - 2];

      if (type === "ClosingTag" && oneBefore.type === "Text" && twoBefore.type === "OpeningTag") {
        // collapse into a single line
        line = '' + indentation + twoBefore.value + oneBefore.value + value;
        indicesToRemove.push(i - 2, i - 1);
      }
    }

    return line;
  });

  indicesToRemove.forEach(function (idx) {
    return rawResult[idx] = null;
  });

  return rawResult.filter(function (val) {
    return !!val;
  }).join('\n');
};

function lexer(xmlStr) {
  var values = splitOnTags(xmlStr);
  return values.map(function (value) {
    return {
      value: value,
      type: getType(value)
    };
  });
}

// Helpers

function getType(str) {
  if (isClosingTag(str)) {
    return 'ClosingTag';
  }

  if (isOpeningTag(str)) {
    return 'OpeningTag';
  }

  if (isSelfClosingTag(str)) {
    return 'SelfClosingTag';
  }

  return 'Text';
}