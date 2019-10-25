'use strict';

var astUtils = require('../util/ast');

module.exports = function (context) {
    function isGlobalScope(scope) {
        return scope.type === 'global' || scope.type === 'module';
    }

    return {
        CallExpression: function (node) {
            var callee = node.callee,
                scope = context.getScope();

            if (astUtils.isTestCase(node) && isGlobalScope(scope)) {
                context.report(callee, 'Unexpected global mocha test.');
            }
        }
    };
};
