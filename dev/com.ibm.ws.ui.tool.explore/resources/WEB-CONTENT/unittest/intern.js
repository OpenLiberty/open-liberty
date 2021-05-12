/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
define({
    // TODO: When we launch with intern-runner, re-enable these reporters
    //reporters: [ "junit", "console", "lcovhtml", "runner" ],
    reporters: [ "lcovhtml", "console", "runner" ],
    excludeInstrumentation: /(?:dojo|intern|istanbul|reporters|unittest)(?:\\|\/)/,
    suites: [ "unittest/all.js" ],
  	forDebug: console.log("Customized intern config for test runner loaded successfully!"),
    // Configuration options for the module loader; any AMD configuration options supported by the Dojo loader can be
    // used here
    loader: {
      // baseUrl is the parent directory of com.ibm.ws.ui.tool.explore, com.ibm.ws.ui, ant_build.js, etc,
      // relative to location where intern-runner with this config file is launched:
      // com.ibm.ws.ui.tool.explore/resources/WEB-CONTENT
      baseUrl: '../../../',
      // Packages that should be registered with the loader in each testing environment
      packages: [
                 { name: 'unittest', location: 'com.ibm.ws.ui.tool.explore/resources/WEB-CONTENT/unittest' },
                 { name: 'jsExplore', location: 'com.ibm.ws.ui.tool.explore/resources/WEB-CONTENT/jsExplore' },
                 { name: 'resources', location: 'com.ibm.ws.ui.tool.explore/resources/WEB-CONTENT/jsExplore/resources' },
                 { name: 'stats', location: 'com.ibm.ws.ui.tool.explore/resources/WEB-CONTENT/jsExplore/resources/stats' },
                 { name: 'nls', location: 'com.ibm.ws.ui.tool.explore/resources/WEB-CONTENT/jsExplore/nls' },
                 { name: 'resources/nls', location: 'com.ibm.ws.ui.tool.explore/resources/WEB-CONTENT/jsExplore/resources/nls' },
                 { name: 'widgets', location: 'com.ibm.ws.ui.tool.explore/resources/WEB-CONTENT/jsExplore/widgets' },
                 { name: 'views', location: 'com.ibm.ws.ui.tool.explore/resources/WEB-CONTENT/jsExplore/views' },
                 { name: 'js', location: 'com.ibm.ws.ui/resources/WEB-CONTENT/js'},
                 { name: 'dojo', location: 'com.ibm.ws.ui.tool.explore/build/dojo/dojo'},
                 { name: 'dojox', location: 'com.ibm.ws.ui.tool.explore/build/dojo/dojox'},
                 { name: 'dijit', location: 'com.ibm.ws.ui.tool.explore/build/dojo/dijit'}
                ],
    },
    
    useLoader: {
      //'host-browser': 'node_modules/dojo/dojo.js'
    },
    //leaveRemoteOpen: true,
    tunnel: 'NullTunnel',
    useSauceConnect: false,
    webdriver: {
      host: 'localhost',
      port: 4444
    },
    environments: [
                   {
                     browserName: 'chrome'
                       //browserName: 'firefox'
                     //browserName: 'phantomjs'
                     /*browserName: 'chrome',
                       version: [ '23', '24' ],
                       platform: [ 'Linux', 'Mac OS 10.8' ]*/
                   }
               ]
});