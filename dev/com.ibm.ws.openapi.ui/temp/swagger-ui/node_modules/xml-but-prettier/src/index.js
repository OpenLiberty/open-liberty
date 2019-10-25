'use strict';

const repeat = require('repeat-string');

const splitOnTags = str => str.split(/(<\/?[^>]+>)/g).filter(line => line.trim() !== '');
const isTag = str => /<[^>!]+>/.test(str);
const isClosingTag = str => /<\/+[^>]+>/.test(str);
const isSelfClosingTag = str => /<[^>]+\/>/.test(str);
const isOpeningTag = str => isTag(str) && !isClosingTag(str) && !isSelfClosingTag(str);

module.exports = (xml, config = {}) => {
  let { indentor, textNodesOnSameLine } = config
  let depth = 0
  const indicesToRemove = []
  indentor = indentor || '    '

  const rawResult = lexer(xml).map((element, i, arr) => {
    const { value, type } = element
    if (type === 'ClosingTag') {
      depth--;
    }

    let indentation = repeat(indentor, depth)
    let line = indentation + value;

    if (type === 'OpeningTag') {
      depth++;
    }

    if(textNodesOnSameLine) {
      // Lookbehind for [OpeningTag][Text][ClosingTag]
      const oneBefore = arr[i-1]
      const twoBefore = arr[i-2]

      if(type === "ClosingTag" && oneBefore.type === "Text" && twoBefore.type === "OpeningTag") {
        // collapse into a single line
        line = `${indentation}${twoBefore.value}${oneBefore.value}${value}`
        indicesToRemove.push(i-2, i-1)
      }
    }

    return line;
  })

  indicesToRemove.forEach(idx => rawResult[idx] = null)

  return rawResult
    .filter(val => !!val)
    .join('\n')
};

function lexer(xmlStr) {
  const values = splitOnTags(xmlStr)
  return values.map(value => {
    return {
      value,
      type: getType(value)
    }
  })
}

// Helpers

function getType(str) {
  if(isClosingTag(str)) {
    return 'ClosingTag'
  }

  if(isOpeningTag(str)) {
    return 'OpeningTag'
  }

  if(isSelfClosingTag(str)) {
    return 'SelfClosingTag'
  }

  return 'Text'
}
