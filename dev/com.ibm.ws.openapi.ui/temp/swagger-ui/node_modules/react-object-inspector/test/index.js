// http://kentor.me/posts/testing-react-and-flux-applications-with-karma-and-webpack/
// ES5 shims for Function.prototype.bind, Object.prototype.keys, etc.
// require('core-js/es5');
require('es5-shim');

var context = require.context('./', true, /-test\.js$/);
context.keys().forEach(context);
