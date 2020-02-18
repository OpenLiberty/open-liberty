/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

var apiUtils = (function() {
    "use strict";
    
    // Set cache to false for all the API calls since none of them should cache the responses since they can all dynamically change
    $.ajaxSetup({
        cache: false
      });

    var __retrieveControllerWritePaths = function() {
        console.log("in retrieveControllerWritePaths");
        var deferred = new $.Deferred();

        $.ajax({
            url: "/IBMJMXConnectorREST/mbeans/WebSphere%3Afeature%3DrestConnector%2Ctype%3DFileService%2Cname%3DFileService/attributes/WriteList",
            dataType: "json",
            success: function(response) {
                console.log("response", response);
                deferred.resolve(response);
            },
            error: function(jqXHR) {
                deferred.reject(jqXHR);
            }
        });

        return deferred;
    };

    var __postFileToController = function(file, uploadImagesDir){
        var deferred = new $.Deferred();
        if (uploadImagesDir.indexOf("/", uploadImagesDir.length - 1) === -1) {
            uploadImagesDir = uploadImagesDir + "/";
        }
        var uploadFile = uploadImagesDir + file.name; // the complete path has to be constructed here but not during the call to encodeURIComponent

        var ajaxPromise = $.ajax({
            url: "/IBMJMXConnectorREST/file/" + encodeURIComponent(uploadFile), // + "?expandOnCompletion=true",
            type: "POST",
            contentType: "application/octet-stream",
            dataType: "json",
            local: false,
            processData: false,
            data: file,
            success: function() {
                console.log("successfully uploading the file");
                deferred.resolve();
            },
            error: function(jqXHR, textStatus, error) {
                console.log("fail to upload the file");
                deferred.reject(error);
            }
        });
        return deferred;
    };

    var __retrieveDeploymentRules = function() {
        var deferred = new $.Deferred();
        var ajaxPromise = $.ajax({
            url: "/ibm/api/collective/v1/deployment/rule",
            success: function(response) {
                if(response.rules !== null) {
                    deferred.resolve(response.rules); //return deployment rules
                } else {
                    deferred.reject();
                }
            },
            error: function(jqXHR, textStatus) {
                deferred.reject(textStatus);
            }
        });
        return deferred;
    };

    var __buildDeploymentHeaders = function(deployRule, hostNames) {
        var headers = {};
        if(deployRule === "Liberty"){
            headers = { 'com.ibm.websphere.jmx.connector.rest.postTransferAction': 'com.ibm.websphere.jmx.connector.rest.postTransferAction.findServerName',
                    'com.ibm.websphere.jmx.connector.rest.postTransferAction.options': 'deploy',
                    'com.ibm.websphere.collective.hostNames': hostNames
            };
        }
        else if(deployRule === "Node.js"){
            headers = {'com.ibm.websphere.collective.hostNames': hostNames};
        }
        return headers;
    };

    var __postDeployment = function(body, needToUpload, filePath, hostNames) {
        console.log("-------------- body", body);
        var deferred = new $.Deferred();
        var ajaxPromise;

        /*
         * Server package is already on the controller
         * Send a simple HTTP POST request containing the input variables to deploy the server to all of the target hosts
         */
        if(!needToUpload){
            ajaxPromise = $.ajax({
                url: "/ibm/api/collective/v1/deployment/deploy",
                type: "POST",
                contentType: "application/json",
                data: JSON.stringify(body),
                success: function(response) {
//                  deployProgress.displayDeployModal();
                    if(response.id !== null) {
                        deferred.resolve(response.id);
                    } else {
                        deferred.reject();
                    }
                },
                error: function(jqXHR, textStatus, errorThrown) {

                    deferred.reject(jqXHR);
                }
            });
        }

        /*
         * Need to upload the package to the controller
         * Send a multi-part request containing both the file and also the deployment variables in json format
         */
        else{
            var formData = new FormData();
            formData.append('file', body.file);
            formData.append('data', JSON.stringify(body.data));

            var rule = ruleSelectUtils.selectedRule.type;
            ajaxPromise = $.ajax({
                  // Todo: filePath should be in request but need to figure out why it's null
                  url: "/ibm/api/collective/v1/deployment/deploy?expandOnCompletion=true&local=false&filePath=" + filePath,
                  type: "POST",
                  contentType: false, // Prevent jQuery from adding a contentType so that the collective rest handler can process the data with a boundary
                  data: formData,
                  processData: false, // Prevents jQuery from converting the data into a string which will fail because it includes a file
                  headers: __buildDeploymentHeaders(rule, hostNames),
                  success: function(response) {
                      if(response.id !== null) {
                          deferred.resolve(response.id);
                      } else {
                          deferred.reject();
                      }
                  },
                  error: function(jqXHR, textStatus, errorThrown) {
                      deferred.reject(jqXHR);
                  }
              });
        }
        return deferred;
    };

    // assuming deployProgress is available as a js to display the deployment progress
    var __retrieveDeploymentResults = function(deployment) {
        var deferred = new $.Deferred();
        // Check for progress using the token received from the initial deploy api call
        var ajaxPromise = $.ajax({
            url: "/ibm/api/collective/v1/deployment/" + deployment.id + "/results",
            success: function(response) {
                deferred.resolve(response);
            },
            error: function(jqXHR, textStatus, error) {
                deferred.reject(error);
            }
        });
        return deferred;
    };

    var __retrieveDockerImages = function () {
        var deferred = new $.Deferred();

        var ajaxPromise = $.ajax({
            url: "/ibm/api/collective/v1/images/docker/",
            success: function(response) {
                deferred.resolve(response);
            },
            error: function(jqXHR, textStatus) {
                deferred.reject(jqXHR);
            }
        });
        return deferred;
    };

    var __retrieveHostList = function() {
        var deferred = new $.Deferred();
        var ajaxPromise = $.ajax({
            url: "/ibm/api/collective/v1/search?type=host",
            success: function(response) {
                if(response.hosts.list !== null) {
                    hostUtils.setHosts(response.hosts.list);
                    deferred.resolve();
                } else {
                    deferred.reject();
                }
            },
            error: function(jqXHR, textStatus) {
                deferred.reject(textStatus);
            }
        });
        return deferred;
    };

    var __retrieveClusters = function(type) {
        var deferred = new $.Deferred();
        var searchAttribute = "?type=server&";
        if (type === clusterUtils.ruleType.docker) {
            searchAttribute += "container=docker";
        } else if (type === clusterUtils.ruleType.nodejs) {
            searchAttribute += "runtimeType=Node.js";
        } else if (type === clusterUtils.ruleType.liberty) {
            searchAttribute += "runtimeType=Liberty";
        }
        var ajaxPromise = $.ajax({
            url: "/ibm/api/collective/v1/search/" + searchAttribute,
            success: function(response) {
                var clusters = [];
                if (response.hasOwnProperty("servers")) {
                    if (response.servers.hasOwnProperty("list")) {
                        response.servers.list.forEach(function(server) {
                            if (server.hasOwnProperty("cluster")) {
                                clusters.push(server.cluster);
                            }
                        });
                    }
                }
                deferred.resolve(clusters);
            },
            error: function(jqXHR, textStatus) {
                deferred.reject(textStatus);
            }
        });
        return deferred;
    };


    return {
        retrieveControllerWritePaths: __retrieveControllerWritePaths,
        retrieveDeploymentRules: __retrieveDeploymentRules,
        retrieveDeploymentResults: __retrieveDeploymentResults,
        retrieveDockerImages: __retrieveDockerImages,
        retrieveHostList: __retrieveHostList,
        retrieveClusters: __retrieveClusters,
        postFileToController: __postFileToController,
        postDeployment: __postDeployment
    };

})();
