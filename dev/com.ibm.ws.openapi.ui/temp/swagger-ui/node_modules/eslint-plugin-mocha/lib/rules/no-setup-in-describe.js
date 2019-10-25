'use strict';

var astUtils = require('../util/ast'),
    additionalSuiteNames = require('../util/settings').additionalSuiteNames;

module.exports = function noSetupInDescribe(context) {
    var nesting = [],
        settings = context.settings,
        FUNCTION = 1,
        DESCRIBE = 2,
        // "Pure" nodes are hooks (like `beforeEach`) or `it` calls
        PURE = 3;

    function isPureNode(node) {
        return astUtils.isHookIdentifier(node) || astUtils.isTestCase(node);
    }

    function reportCallExpression(callExpression) {
        var message = 'Unexpected function call in describe block.';

        context.report({
            message: message,
            node: callExpression.callee
        });
    }

    function reportMemberExpression(memberExpression) {
        var message = 'Unexpected member expression in describe block. ' +
            'Member expressions may call functions via getters.';

        context.report({
            message: message,
            node: memberExpression
        });
    }

    function isNestedInDescribeBlock() {
        return nesting.length &&
                nesting.indexOf(PURE) === -1 &&
                nesting.lastIndexOf(FUNCTION) < nesting.lastIndexOf(DESCRIBE);
    }

    function handleCallExpressionInDescribe(node) {
        if (isPureNode(node)) {
            nesting.push(PURE);
        } else if (isNestedInDescribeBlock()) {
            reportCallExpression(node);
        }
    }

    return {
        CallExpression: function (node) {
            var isDescribe = astUtils.isDescribe(node, additionalSuiteNames(settings));
            if (isDescribe) {
                nesting.push(DESCRIBE);
                return;
            }
            // don't process anything else if the first describe hasn't been processed
            if (!nesting.length) {
                return;
            }
            handleCallExpressionInDescribe(node);
        },

        'CallExpression:exit': function (node) {
            if (astUtils.isDescribe(node) || nesting.length && isPureNode(node)) {
                nesting.pop();
            }
        },

        MemberExpression: function (node) {
            if (isNestedInDescribeBlock()) {
                reportMemberExpression(node);
            }
        },

        FunctionDeclaration: function () {
            if (nesting.length) {
                nesting.push(FUNCTION);
            }
        },
        'FunctionDeclaration:exit': function () {
            if (nesting.length) {
                nesting.pop();
            }
        },

        ArrowFunctionExpression: function () {
            if (nesting.length) {
                nesting.push(FUNCTION);
            }
        },
        'ArrowFunctionExpression:exit': function () {
            if (nesting.length) {
                nesting.pop();
            }
        }
    };
};
