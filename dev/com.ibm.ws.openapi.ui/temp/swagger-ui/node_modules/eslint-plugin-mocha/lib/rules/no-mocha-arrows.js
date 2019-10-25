'use strict';

/**
 * @fileoverview Disallow arrow functions as arguments to Mocha globals
 * @author Paul Melnikow
 */

var R = require('ramda'),
    astUtils = require('../util/ast');

module.exports = function (context) {
    function fixArrowFunction(fixer, fn) {
        var sourceCode = context.getSourceCode(),
            paramsLeftParen = sourceCode.getFirstToken(fn),
            paramsRightParen = sourceCode.getTokenBefore(sourceCode.getTokenBefore(fn.body)),
            paramsFullText =
                sourceCode.text.slice(paramsLeftParen.range[0], paramsRightParen.range[1]),
            functionKeyword = 'function',
            bodyText;

        if (fn.async) {
            // When 'async' specified, take care about the keyword.
            functionKeyword = 'async function';
            // Strip 'async (...)' to ' (...)'
            paramsFullText = paramsFullText.slice(5);
        }

        if (fn.params.length > 0) {
            paramsFullText = '(' + sourceCode.text.slice(fn.params[0].start, R.last(fn.params).end) + ')';
        }

        if (fn.body.type === 'BlockStatement') {
            // When it((...) => { ... }),
            // simply replace '(...) => ' with 'function () '
            return fixer.replaceTextRange(
                [ fn.start, fn.body.start ],
                functionKeyword + paramsFullText + ' '
            );
        }

        bodyText = sourceCode.text.slice(fn.body.range[0], fn.body.range[1]);
        return fixer.replaceTextRange(
            [ fn.start, fn.end ],
            functionKeyword + paramsFullText + ' { return ' + bodyText + '; }'
        );
    }

    return {
        CallExpression: function (node) {
            var name = astUtils.getNodeName(node.callee),
                fnArg;

            if (astUtils.isMochaFunctionCall(node, context.getScope())) {
                fnArg = node.arguments.slice(-1)[0];
                if (fnArg && fnArg.type === 'ArrowFunctionExpression') {
                    context.report({
                        node: node,
                        message: 'Do not pass arrow functions to ' + name + '()',
                        fix: function (fixer) {
                            return fixArrowFunction(fixer, fnArg);
                        }
                    });
                }
            }
        }
    };
};
