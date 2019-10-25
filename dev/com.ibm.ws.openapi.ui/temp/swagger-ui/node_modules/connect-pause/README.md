Connect Pause
=============

Connect/Express middleware to simulate latency for debugging

Installation
===

    npm install connect-pause

Usage
===

Pause all [Connect](http://github.com/senchalabs/connect) requests:

    var connect = require('connect'),
        pause = require('connect-pause');

    Connect.createServer(
        pause(1000),
        function(req, res, next){
            res.writeHead(200, {'Content-Type':'text/plain'});
            res.end('Waited 1 second');
        }
    ).listen(8080);

Pause all [Express](https://github.com/visionmedia/express) requests:

 	var express = require('express'),
        pause = require('connect-pause');

    var app = express();
    app.use(pause(1000));
	app.get('/', function(req, res){
		res.send('Waited 1 second');
	});
	app.listen(3000);

Pause a single [Express](https://github.com/visionmedia/express) endpoint:

 	var express = require('express'),
        pause = require('connect-pause');

    var app = express();
	app.get('/', pause(1000), function(req, res){
		res.send('Waited 1 second');
	});
	app.listen(3000);

Pass an error to be returned after the delay:

 	var express = require('express'),
        pause = require('connect-pause');

    var app = express();
	app.get('/', pause(1000, new Error('Send 500')), function(req, res){
		res.send('Waited 1 second');
	});
	app.listen(3000);

When to use
===

This middleware is meant to be used while developing and in need to simulate latency for certain requests or all.

License 
===

(The MIT License)

Copyright (c) 2013 Ariel Flesler

Permission is hereby granted, free of charge, to any person obtaining
a copy of this software and associated documentation files (the
'Software'), to deal in the Software without restriction, including
without limitation the rights to use, copy, modify, merge, publish,
distribute, sublicense, and/or sell copies of the Software, and to
permit persons to whom the Software is furnished to do so, subject to
the following conditions:

The above copyright notice and this permission notice shall be
included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED 'AS IS', WITHOUT WARRANTY OF ANY KIND,
EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
