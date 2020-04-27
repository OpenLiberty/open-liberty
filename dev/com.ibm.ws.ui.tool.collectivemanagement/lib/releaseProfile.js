var dojoVersion = '1.12.2';
var gridxVersion = '1.3.9';
var idxVersion = '1.5.2.6';

var profile = {
  action : 'release',
  basePath : "..",
  releaseDir : "build/dojo",
  buildTimestamp : timestamp(),
  cssOptimize : "comments",
  stripConsole : "all",
  optimize : false, // Values: falsy, comment.keeplines, comment, shrinksafe.keeplines, shrinksafe, closure.keepLines, closure
  // layerOptimize is overwritten in ant_build.js\public_imports\internal_imports\dojo.xml to use shrinksafe for local & personal builds, and closure otherwise 
  layerOptimize : 'closure', // Values: falsy, comment.keeplines, comment, shrinksafe.keeplines, shrinksafe, closure.keepLines, closure
  useSourceMaps: false,
  selectorEngine : 'lite',
  mini : true,
  localeList : "cs,de,es,fr,hu,it,ja,ko,pl,pt-br,ro,ru,zh,zh-tw",

  defaultConfig : {
    hasCache : {
      "dojo-built" : 1,
      "dojo-loader" : 1,
      "dom" : 1,
      "host-browser" : 1,
      "config-selectorEngine" : "lite"
    },
    async : 1
  },

  packages : [ {
      name : 'dojo',
      location : '../ant_build.js/lib/dojo/dojo-' + dojoVersion + '/dojo'
    }, {
      name : 'dijit',
      location : '../ant_build.js/lib/dojo/dojo-' + dojoVersion + '/dijit'
    }, {
      name : 'dojox',
      location : '../ant_build.js/lib/dojo/dojo-' + dojoVersion + '/dojox'
    }, {
      name : 'gridx',
      location : '../ant_build.js/lib/dojo/gridx-' + gridxVersion + '/gridx'
    }, {
      name : 'idx',
      location : '../ant_build.js/lib/dojo/idx-' + idxVersion + '/idx'
  }, {
    name : 'js',
    location : '../com.ibm.ws.ui/resources/WEB-CONTENT/js',
    resourceTags : {
      amd : function(filename, mid) {
        return /\.js$/.test(filename);
      }
    }
  }, {
	    name : 'login',
	    location : '../com.ibm.ws.ui/resources/WEB-CONTENT/login',
	    resourceTags : {
	      amd : function(filename, mid) {
	        return /\.js$/.test(filename);
	      }
	    }
  }, {
    name : 'jsCM',
    location : 'resources/WEB-CONTENT/jsCM',
    resourceTags : {
      amd : function(filename, mid) {
        return /\.jsCM$/.test(filename);
      }
    }
  }, {
    name : 'css',
    location : 'resources/WEB-CONTENT/css'
  } ],
  staticHasFeatures : {
      "config-deferredInstrumentation" : 0,
      "config-dojo-loader-catches" : 0,
      "config-stripStrict" : 0,
      "config-tlmSiblingOfDojo" : 0,
      "dojo-amd-factory-scan" : 0,
      "dojo-bidi" : 1,
      "dojo-cdn" : 0,
      "dojo-combo-api" : 0,
      "dojo-config-api" : 1,
      "dojo-config-require" : 0,
      "dojo-debug-messages" : 0,
      "dojo-dom-ready-api" : 1,
      "dojo-firebug" : 0,
      "dojo-guarantee-console" : 0,
      "dojo-has-api" : 1,
      "dojo-inject-api" : 1,
      "dojo-loader" : 1,
      "dojo-log-api" : 0,
      "dojo-modulePaths" : 0,
      "dojo-moduleUrl" : 0,
      "dojo-publish-privates" : 0,
      "dojo-requirejs-api" : 0,
      "dojo-sniff" : 1,
      "dojo-sync-loader" : 0,
      "dojo-test-sniff" : 0,
      "dojo-timeout-api" : 0,
      "dojo-trace-api" : 0,
      "dojo-undef-api" : 0,
      "dojo-v1x-i18n-Api" : 1,
      "dom" : 1,
      "extend-dojo" : 1,
      "host-browser" : 1,
      "ie-event-behavior" : 0
  },
  layers : {
    'dojo/dojo' : {
      copyright: '../ant_build.js/legal/javascript_copyright.txt',
      include : [ 'jsCM/getInfo-init', 'dojo/i18n' ],
      customBase : true,
      boot : true,
    },

  }
// end layers
};

//Disable Rhino's logging to get a speed boost
if (typeof Packages !== 'undefined' && Packages.com.google.javascript.jscomp.Compiler) {
 Packages.com.google.javascript.jscomp.Compiler.setLoggingLevel(Packages.java.util.logging.Level.OFF);
}

function timestamp() {
 var d = new Date();
 return d.getFullYear() + '-' + (d.getMonth() + 1) + "-" + d.getDate() + "-" + d.getHours() + ':' + d.getMinutes() + ":" + d.getSeconds();
}
