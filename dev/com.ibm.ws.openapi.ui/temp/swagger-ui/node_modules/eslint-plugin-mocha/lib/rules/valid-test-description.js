'use strict';

/**
 * @fileoverview Match test descriptions to match a pre-configured regular expression
 * @author Alexander Afanasyev
 */

var defaultTestNames = [ 'it', 'test', 'specify' ],
    astUtils = require('../util/ast');

module.exports = function (context) {
    var pattern = context.options[0] ? new RegExp(context.options[0]) : /^should/,
        testNames = context.options[1] ? context.options[1] : defaultTestNames;

    function isTest(node) {
        return node.callee && node.callee.name && testNames.indexOf(node.callee.name) > -1;
    }

    function hasValidTestDescription(mochaCallExpression) {
        var args = mochaCallExpression.arguments,
            testDescriptionArgument = args[0];

        if (astUtils.isStringLiteral(testDescriptionArgument)) {
            return pattern.test(testDescriptionArgument.value);
        }

        return true;
    }

    function hasValidOrNoTestDescription(mochaCallExpression) {
        var args = mochaCallExpression.arguments,
            hasNoTestDescription = args.length === 0;

        return hasNoTestDescription || hasValidTestDescription(mochaCallExpression);
    }

    return {
        CallExpression: function (node) {
            var callee = node.callee;

            if (isTest(node)) {
                if (!hasValidOrNoTestDescription(node)) {
                    context.report(node, 'Invalid "' + callee.name + '()" description found.');
                }
            }
        }
    };
};
