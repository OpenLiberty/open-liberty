/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

define(['dojo/request/xhr', 'dojo/i18n!../nls/explorerMessages'], function(xhr, i18n) {
  var deployFeatureAvailable = null;
  var configFeatureAvailable = null;

  return {
    isDeployAvailable: function() {
      if (deployFeatureAvailable === null) {
        xhr('../deploy-1.0/feature', {
          sync : true
        }).then(function(data) {
          deployFeatureAvailable = true;
        }, function(err) {
          console.error(i18n.NO_DEPLOY_RESOURCE);
          deployFeatureAvailable = false;
        });
      }
      return deployFeatureAvailable;
    },
    isConfigAvailable: function() {
      if (configFeatureAvailable === null) {
        xhr(window.location.protocol + "//" + window.location.host + '/ibm/adminCenter/serverConfig-1.0/feature', {
          sync : true
        }).then(function(data) {
          console.log("Server Config is available");
          configFeatureAvailable = true;
        }, function(err) {
          console.log("Server Config is not available");
          configFeatureAvailable = false;
        });
      }
      return configFeatureAvailable;
    }
  };

});