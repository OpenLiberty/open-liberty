/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
/* Simple DOJO require to force a load of the login module */
require([ "jsExplore/mainDashboard", "dojo/domReady!" ], function(mainDashboard) {
  //  console.debug('Explore window.location.hash was: ', window.location.hash);
  //  window.location.hash = window.top.location.hash;
  //  console.debug('Explore window.location.hash is: ', window.location.hash);
  mainDashboard.initPage();
});
