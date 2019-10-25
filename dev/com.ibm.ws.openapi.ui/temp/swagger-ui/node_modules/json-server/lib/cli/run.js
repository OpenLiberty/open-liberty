'use strict';

var fs = require('fs');
var path = require('path');
var jph = require('json-parse-helpfulerror');
var _ = require('lodash');
var chalk = require('chalk');
var enableDestroy = require('server-destroy');
var pause = require('connect-pause');
var is = require('./utils/is');
var load = require('./utils/load');
var example = require('./example.json');
var jsonServer = require('../server');

function prettyPrint(argv, object, rules) {
  var host = argv.host === '0.0.0.0' ? 'localhost' : argv.host;
  var port = argv.port;
  var root = `http://${host}:${port}`;

  console.log();
  console.log(chalk.bold('  Resources'));
  for (var prop in object) {
    console.log(`  ${root}/${prop}`);
  }

  if (rules) {
    console.log();
    console.log(chalk.bold('  Other routes'));
    for (var rule in rules) {
      console.log(`  ${rule} -> ${rules[rule]}`);
    }
  }

  console.log();
  console.log(chalk.bold('  Home'));
  console.log(`  ${root}`);
  console.log();
}

function createApp(source, object, routes, middlewares, argv) {
  var app = jsonServer.create();

  var router = void 0;

  var foreignKeySuffix = argv.foreignKeySuffix;

  try {
    router = jsonServer.router(is.JSON(source) ? source : object, foreignKeySuffix ? { foreignKeySuffix } : undefined);
  } catch (e) {
    console.log();
    console.error(chalk.red(e.message.replace(/^/gm, '  ')));
    process.exit(1);
  }

  var defaultsOpts = {
    logger: !argv.quiet,
    readOnly: argv.readOnly,
    noCors: argv.noCors,
    noGzip: argv.noGzip
  };

  if (argv.static) {
    defaultsOpts.static = path.join(process.cwd(), argv.static);
  }

  var defaults = jsonServer.defaults(defaultsOpts);
  app.use(defaults);

  if (routes) {
    var rewriter = jsonServer.rewriter(routes);
    app.use(rewriter);
  }

  if (middlewares) {
    app.use(middlewares);
  }

  if (argv.delay) {
    app.use(pause(argv.delay));
  }

  router.db._.id = argv.id;
  app.db = router.db;
  app.use(router);

  return app;
}

module.exports = function (argv) {
  var source = argv._[0];
  var app = void 0;
  var server = void 0;

  if (!fs.existsSync(argv.snapshots)) {
    console.log(`Error: snapshots directory ${argv.snapshots} doesn't exist`);
    process.exit(1);
  }

  // noop log fn
  if (argv.quiet) {
    console.log = function () {};
  }

  console.log();
  console.log(chalk.cyan('  \\{^_^}/ hi!'));

  function start(cb) {
    console.log();

    // Be nice and create a default db.json if it doesn't exist
    if (is.JSON(source) && !fs.existsSync(source)) {
      console.log(chalk.yellow(`  Oops, ${source} doesn't seem to exist`));
      console.log(chalk.yellow(`  Creating ${source} with some default data`));
      console.log();
      fs.writeFileSync(source, JSON.stringify(example, null, 2));
    }

    console.log(chalk.gray('  Loading', source));

    // Load JSON, JS or HTTP database
    load(source, function (err, data) {
      if (err) throw err;

      // Load additional routes
      var routes = void 0;
      if (argv.routes) {
        console.log(chalk.gray('  Loading', argv.routes));
        routes = JSON.parse(fs.readFileSync(argv.routes));
      }

      // Load middlewares
      var middlewares = void 0;
      if (argv.middlewares) {
        middlewares = argv.middlewares.map(function (m) {
          console.log(chalk.gray('  Loading', m));
          return require(path.resolve(m));
        });
      }

      // Done
      console.log(chalk.gray('  Done'));

      // Create app and server
      app = createApp(source, data, routes, middlewares, argv);
      server = app.listen(argv.port, argv.host);

      // Enhance with a destroy function
      enableDestroy(server);

      // Display server informations
      prettyPrint(argv, data, routes);

      cb && cb();
    });
  }

  // Start server
  start(function () {
    // Snapshot
    console.log(chalk.gray('  Type s + enter at any time to create a snapshot of the database'));

    // Support nohup
    // https://github.com/typicode/json-server/issues/221
    process.stdin.on('error', function () {
      console.log(`  Error, can't read from stdin`);
      console.log(`  Creating a snapshot from the CLI won't be possible`);
    });
    process.stdin.setEncoding('utf8');
    process.stdin.on('data', function (chunk) {
      if (chunk.trim().toLowerCase() === 's') {
        var filename = `db-${Date.now()}.json`;
        var file = path.join(argv.snapshots, filename);
        var state = app.db.getState();
        fs.writeFileSync(file, JSON.stringify(state, null, 2), 'utf-8');
        console.log(`  Saved snapshot to ${path.relative(process.cwd(), file)}\n`);
      }
    });

    // Watch files
    if (argv.watch) {
      console.log(chalk.gray('  Watching...'));
      console.log();
      var _source = argv._[0];

      // Can't watch URL
      if (is.URL(_source)) throw new Error("Can't watch URL");

      // Watch .js or .json file
      // Since lowdb uses atomic writing, directory is watched instead of file
      var watchedDir = path.dirname(_source);
      var readError = false;
      fs.watch(watchedDir, function (event, file) {
        // https://github.com/typicode/json-server/issues/420
        // file can be null
        if (file) {
          var watchedFile = path.resolve(watchedDir, file);
          if (watchedFile === path.resolve(_source)) {
            if (is.JSON(watchedFile)) {
              var obj = void 0;
              try {
                obj = jph.parse(fs.readFileSync(watchedFile));
                if (readError) {
                  console.log(chalk.green(`  Read error has been fixed :)`));
                  readError = false;
                }
              } catch (e) {
                readError = true;
                console.log(chalk.red(`  Error reading ${watchedFile}`));
                console.error(e.message);
                return;
              }

              // Compare .json file content with in memory database
              var isDatabaseDifferent = !_.isEqual(obj, app.db.getState());
              if (isDatabaseDifferent) {
                console.log(chalk.gray(`  ${_source} has changed, reloading...`));
                server && server.destroy();
                start();
              }
            }
          }
        }
      });

      // Watch routes
      if (argv.routes) {
        var _watchedDir = path.dirname(argv.routes);
        fs.watch(_watchedDir, function (event, file) {
          if (file) {
            var watchedFile = path.resolve(_watchedDir, file);
            if (watchedFile === path.resolve(argv.routes)) {
              console.log(chalk.gray(`  ${argv.routes} has changed, reloading...`));
              server && server.destroy();
              start();
            }
          }
        });
      }
    }
  });
};