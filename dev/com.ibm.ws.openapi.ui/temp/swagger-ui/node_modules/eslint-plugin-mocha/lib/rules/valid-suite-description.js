'use strict';

/**
 * @fileoverview Match suite descriptions to match a pre-configured regular expression
 * @author Alexander Afanasyev
 */

var defaultSuiteNames = [ 'describe', 'context', 'suite' ],
    astUtils = require('../util/ast');

module.exports = function (context) {
    var pattern = new RegExp(context.options[0]),
        suiteNames = context.options[1] ? context.options[1] : defaultSuiteNames;

    function isSuite(node) {
        return node.callee && node.callee.name && suiteNames.indexOf(node.callee.name) > -1;
    }

    function hasValidSuiteDescription(mochaCallExpression) {
        var args = mochaCallExpression.arguments,
            description = args[0];

        if (astUtils.isStringLiteral(description)) {
            return pattern.test(description.value);
        }

        return true;
    }

    function hasValidOrNoSuiteDescription(mochaCallExpression) {
        var args = mochaCallExpression.arguments,
            hasNoSuiteDescription = args.length === 0;

        return hasNoSuiteDescription || hasValidSuiteDescription(mochaCallExpression);
    }

    return {
        CallExpression: function (node) {
            var callee = node.callee;

            if (isSuite(node)) {
                if (!hasValidOrNoSuiteDescription(node)) {
                    context.report(node, 'Invalid "' + callee.name + '()" description found.');
                }
            }
        }
    };
};
