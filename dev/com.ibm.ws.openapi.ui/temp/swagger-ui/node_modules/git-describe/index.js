'use strict';

var _ = require('lodash');
var gitDescribe = require('./lib/git-describe');

module.exports = {

    gitDescribe: function() {
        var gitDescribeArgs = [].slice.call(arguments);
        return new Promise(function(resolve, reject) {
            var userCb = null;
            for (var i = Math.min(3, gitDescribeArgs.length) - 1; i >= 0; i--) {
                if (_.isFunction(gitDescribeArgs[i])) {
                    userCb = gitDescribeArgs[i];
                    gitDescribeArgs[i] = cb;
                    break;
                }
            }
            if (!userCb)
                gitDescribeArgs.push(cb);

            function cb(err, result) {
                if (err) reject(err); else resolve(result);
                if (userCb) userCb(err, result);
            }

            gitDescribe.apply(this, gitDescribeArgs);
        });
    },

    gitDescribeSync: _.partialRight(gitDescribe, null)

};
