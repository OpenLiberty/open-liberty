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
    reporters: [ "html", "console" ],
    excludeInstrumentation: /(?:dojo|intern|istanbul|reporters|unittest)(?:\\|\/)/,
    suites: [ "unittest/all.js" ],
  	forDebug: console.log("Customized intern config for test runner loaded successfully!"),
    // Configuration options for the module loader; any AMD configuration options supported by the Dojo loader can be
    // used here
    loader: {
      baseUrl: '../',
      // Packages that should be registered with the loader in each testing environment
      packages: [
                 { name: 'dojox', location: 'dojox',  main:'main' },
                 { name: 'resources', location: 'jsExplore/resources' },
                 { name: 'stats', location: 'jsExplore/resources/stats' },
                 { name: 'nls', location: 'jsExplore/nls' },
                 { name: 'widgets', location: 'jsExplore/widgets' },
                 { name: 'views', location: 'jsExplore/views' },
                ]
    },

    useLoader: {
      'host-browser': '../intern/node_modules/dojo/dojo.js'
    }
});