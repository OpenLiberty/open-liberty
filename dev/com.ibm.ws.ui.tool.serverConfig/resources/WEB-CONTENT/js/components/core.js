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

goog.provide("core");
goog.require("constants");

var core = (function() {
    "use strict";
    
    // Flag indicates if running in a collective environment
    // Possible values: constants constants.ENVIRONMENT_COLLECTIVE, constants.ENVIRONMENT_LOCAL, or null (not determined)
    var environment = null;
    
    // Id of the server that hosts the tool
    var hostServerId = null;
    
    // Server id (when working in a collective environment)
    var serverId = null;

    // The full details of a collective server from the collective REST APIs
    var collectiveServerData = null;
    
    //There was an error thrown last time
    var errorOnLastLoad = false;
    
    // List of servers that have been checked
    var checkedServerIds = [];
    
    // Flag to keep track of server data initialization
    var serverDataInitialized = false;
    
    // Server schema in text format
    var serverSchemaInTextFormat = null;
    
    // List of server features
    var featureList = null;

    // The base uri for the validator feature
    var validatorUri = "/ibm/api/validator";

    // List of server configuration validators
    var validatorList = null;

    // See if the collective member is running.
    // This variable is only used in a collective environment, not standalone server
    var collectiveServerIsRunning = null;
    
    // Flag indicated if config tool is embedded
    // Possible values: constants constants.EMBEDDED_ADMIN_CENTER, constants.EMBEDDED_EXPLORE_TOOL, or null (not embedded)
    var embedded = (window.frameElement !== null && window.frameElement !== undefined) ? (window.frameElement.id === "exploreContainerForConfigTool"? constants.EMBEDDED_EXPLORE_TOOL : constants.EMBEDDED_ADMIN_CENTER) : null;

    // Flag for blocking UI
    var uiBlocked = false;

    var startup = function() {
        
        // Update title
        setNavbarTitleText(editorMessages.LOADING);
        
        // Hide all controls
        hideAllControls();
        
        // Show progress
        showControlById("progress", true);
        
        // Obtain information from URL
        var dataFromURL = getDataFromURLHash();
        
        // Check if running in a collective environment
        checkCollectiveEnvironment().done(function() {
            
            // Obtain host server id
            retrieveHostServerId().done(function(retrievedHostServerId) {
                
                hostServerId = retrievedHostServerId;
                
                if(dataFromURL.serverId !== null && dataFromURL.serverId !== undefined) {
                    
                    // Check if the server id provided is valid
                    checkServerId(dataFromURL.serverId).done(function() {
                        
                        var forceServerRefresh = false;
                        
                        if( (serverId !== null && serverId !== undefined && serverId !== dataFromURL.serverId) || errorOnLastLoad) {
                            forceServerRefresh = true;
                        }
                        
                        // Update serverId
                        serverId = dataFromURL.serverId;
                        
                        // Open or list files
                        openOrListFiles(dataFromURL.filePath, forceServerRefresh);
                        
                    }).fail(function() {
                        
                        // Show error message
                        renderServerNotFoundErrorMessage(dataFromURL.serverId);
                        
                        // Hide progress
                        hideControlById("progress");
                        
                        // Update title
                        setNavbarTitleText(editorMessages.ERROR);
                        
                        // Show change server button
                        if(embedded !== constants.EMBEDDED_EXPLORE_TOOL) {
                            showControlById("navbarChangeServerSection");
                        }
                        
                    });
                } else if(dataFromURL.filePath !== null && dataFromURL.filePath !== undefined) {
                    
                    // Local reference in collective environment
                    openOrListFiles(dataFromURL.filePath, false);
                    
                } else {
                    
                    // Render list of servers
                    renderServerList();    
                }
                
            }).fail(function() {
                
                // Show error message
                core.renderMessage(editorMessages.COULD_NOT_RETRIEVE_SERVER_IDENTIFICATION, "danger");
                
                // Hide progress
                hideControlById("progress");
                
                // Update title
                setNavbarTitleText(editorMessages.ERROR);
                
            });
            
        }).fail(function() {
            // Standalone server
            
            if(dataFromURL.serverId !== null && dataFromURL.serverId !== undefined) {
                
                // Update title
                setNavbarTitleText("");
                
                // Hide progress
                core.hideControlById("progress", false, false);
                
                // Show error message (server referenced but not in a collective environment)
                renderCannotAccessRemoteServerFromNoneCollectiveControllerMessage(dataFromURL.serverId);
                
            } else {
                
                // Open or list files
                openOrListFiles(dataFromURL.filePath, false);
                
            }
        });
    };
    
    
    var checkCollectiveEnvironment = function() {
        var deferred = new $.Deferred();
        if(environment === null || environment === undefined) {
            collectiveUtils.checkCollectiveEnvironment().done(function() {
                
                // Update collective flag
                environment = constants.ENVIRONMENT_COLLECTIVE;
                
                deferred.resolve();
            }).fail(function() {
                
                // Update collective flag
                environment = constants.ENVIRONMENT_LOCAL;
                
                deferred.reject();
            });
        } else if(environment === constants.ENVIRONMENT_COLLECTIVE) {
            deferred.resolve();
        } else if(environment === constants.ENVIRONMENT_LOCAL) {
            deferred.reject();
        }
        return deferred;
    };    
    
    
    var retrieveHostServerId = function() {
        if(hostServerId !== null && hostServerId !== undefined) {
            return new $.Deferred().resolve(hostServerId);
        } else {
            return collectiveUtils.retrieveHostServerId();
        }
    };
    
    
    var checkServerId = function(serverId) {
        var deferred = new $.Deferred();
        if(checkedServerIds.indexOf(serverId) !== -1) {
            deferred.resolve();
        } else {
            collectiveUtils.checkServerId(serverId).done(function() {
                checkedServerIds.push(serverId);
                deferred.resolve();
            }).fail(function() {
                deferred.reject();
            });
        }
        return deferred;
    };

    
    var openOrListFiles = function(filePath, forceServerRefresh) {
        
        // Initialize server information
        retrieveServerData(forceServerRefresh).done(function() {
            
            if(filePath !== null && filePath !== undefined) {
                // Attempt to retrieve file
                fileUtils.retrieveFile(filePath).done(function(file) {
                    // Check to see if Beta validator feature is enabled so we can expose the validator test button in server config
                    var getListOfValidatorsPromise = getListOfValidators();
                    $.when(getListOfValidatorsPromise).done(function(list) {
                        validatorList = list;
                        openFileForEditing(file);
                    }).fail(function() {
                        // TODO: Determine how to alert end user that validators could not be determiend
                        var errorMessage = "Problem getting list of supported server configuration validators"; // TODO: PII
                        console.error(errorMessage);
                        validatorList = null;
                         // We still want server configuration to work without the validator test buttons
                        openFileForEditing(file);
                    });
                }).fail(function() {
                    
                    // Show error message
                    var errorMessage = stringUtils.formatString(editorMessages.FILE_NOT_FOUND_REPLACE, [filePath]);
                    renderMessage(errorMessage, "danger", true);

                    // List files
                    renderFileList();
                    
                });
            } else {
                
                // List files
                renderFileList();
            }
        }).fail(function(jqXHR) {
            
            // Update title
            setNavbarTitleText("");
            
            // Hide progress
            core.hideControlById("progress", false, false);
            
            // If the error occurred on a collective member, show extra help information
            if (environment === constants.ENVIRONMENT_COLLECTIVE ) {
                collectiveUtils.checkServerId(serverId).done(function(server) {
                    if (!server.isCollectiveController){
                        var collectiveMemberTroubleshooting = "<strong>" + editorMessages.REQUIRED_ACTIONS + "</strong><br><ul><li>" + editorMessages.RUN_UPDATE_HOST +
                                // Currently we cannot guarantee that the URLs in KC will not change so do not link to them.  Also, we can't check if the links are accessible due to Same Origin Policy
//                                "<ul><li>" + editorMessages.MORE_INFORMATION +"<a href='https://www.ibm.com/support/knowledgecenter/SSAW57_liberty/com.ibm.websphere.wlp.nd.doc/ae/tagt_wlp_registerhost.html'>" + editorMessages.REGISTERING_HOST_LINK + "</a></li></ul></li>" +
                            "<li>" + editorMessages.CONIFGURED_SSH_RXA + 
                                // Currently we cannot guarantee that the URLs in KC will not change so do not link to them.  Also, we can't check if the links are accessible due to Same Origin Policy
//                                "<ul><li>" + editorMessages.MORE_INFORMATION + stringUtils.formatString(editorMessages.TWO_LINKS, ["<a href='https://www.ibm.com/support/knowledgecenter/SSAW57_liberty/com.ibm.websphere.wlp.nd.doc/ae/tagt_wlp_configure_collective.html'>" + editorMessages.CONFIGURING_COLLECTIVE_LINK + "</a>", "<a href=https://www.ibm.com/support/knowledgecenter/SSAW57_liberty/com.ibm.websphere.wlp.nd.doc/ae/twlp_set_rxa.html>" + editorMessages.CONFIGURING_RXA_LINK + "</a>"]) + "</li></ul></li>" +
                            "<li>" + editorMessages.CONFIGRUED_READ_DIR + 
                                "<ul><li>" + editorMessages.DEFAULT_READ_DIR + "</li></ul></li>" +
                            "<li>" + editorMessages.PUBLISHED_READ_DIR + "</li>" + 
                            "<li>" + editorMessages.JAVA_AVAILABLE + 
                                "<ul><li>" + editorMessages.HOST_JAVA_HOME + "</li>" +
                                "<li>" + editorMessages.LINK_JAVA + "</li>" +
                                "<li>" +editorMessages.JAVA_ON_PATH + "</li></ul></li>" +
                            "</li></ul>"; 
                        
                        renderMessage(collectiveMemberTroubleshooting, "warning", false, false, null, true);
                    }
                });
            }

            // Show error message
            renderMessage(editorMessages.ERROR_RETRIEVING_SERVER_INFORMATION, "danger", false, false, jqXHR);

            // Show change server option if running in a collective environment and not embedded in the explore tool
            if(environment === constants.ENVIRONMENT_COLLECTIVE && embedded !== constants.EMBEDDED_EXPLORE_TOOL) {
                core.showControlById("navbarChangeServerSection", false);
            }
        });
    };

    
    var renderFileList = function() {
        
        fileExplorer.renderFileExplorer().done(function() {

            // Show file explorer
            core.showControlById("fileExplorer", false);
            
        }).fail(function() {
            
            // Show error message
            renderMessage(editorMessages.FILE_ACCESS_ERROR_MESSAGE, "danger", false);
            
        }).always(function() {
            
            // Update title
            if(environment === constants.ENVIRONMENT_COLLECTIVE && embedded !== constants.EMBEDDED_EXPLORE_TOOL) {
                setNavbarTitleText(serverId.substring(serverId.lastIndexOf(",") + 1));
            } else {
                setNavbarTitleText(editorMessages.CONFIGURATION_FILES);    
            }
            
            // Hide progress
            core.hideControlById("progress", false, false);
            
            // Show change server button on navigation bar
            if(environment === constants.ENVIRONMENT_COLLECTIVE && embedded !== constants.EMBEDDED_EXPLORE_TOOL) {
                    
                core.showControlById("navbarChangeServerSection", false);
                
            } else {
                
                // Show sign out button on navigation bar
                if(!isInIFrame()) {
                    core.showControlById("navbarSignOutSection", false);
                }
            }
            
            // Focus title for accessibility compliance
            $("#navigationBarTitle").focus();
            
        });
    };
    
    
    var renderServerNotFoundErrorMessage = function(serverId) {
        // show error message of server not found
        var firstCommaIndex = serverId.indexOf(",");
        var lastCommaIndex = serverId.lastIndexOf(",");

        var serverName = serverId.substring(lastCommaIndex + 1, serverId.length);
        var hostName = serverId.substring(0, firstCommaIndex);
        var userDir = serverId.substring(firstCommaIndex + 1, lastCommaIndex);

        var errorMessage = stringUtils.formatString(editorMessages.SERVER_NOT_FOUND, [serverName, hostName, userDir]);
        core.renderMessage(errorMessage, "danger");    
    };

    
    var renderCannotAccessRemoteServerFromNoneCollectiveControllerMessage = function() {
        var link = "<a href=\"\" draggable=\"false\" onclick=\"core.run('');\">" + editorMessages.HERE + "</a>";
        var errorMessage = stringUtils.formatString(editorMessages.ERROR_NOT_IN_COLLECTIVE_ENVIRONMENT, [link]);    
        renderMessage(errorMessage, "danger", true);
    };


    var renderServerList = function() {
        serverExplorer.renderServerExplorer().done(function() {
            
            // Update title
            setNavbarTitleText(editorMessages.SELECT_SERVER);
            
            // Show sign out button on navigation bar
            if(!isInIFrame()) {
                showControlById("navbarSignOutSection", false);
            }
            
            // Show server explorer
            showControlById("serverExplorer", false);
            
        }).fail(function() {
            
            // Show error message
            renderMessage(editorMessages.ERROR_ACCESSING_SERVER_LIST, "danger", true);
        }).always(function() {
            
            // Hide progress
            core.hideControlById("progress", false, false);
            
            // Focus title for accessibility compliance
            $("#navigationBarTitle").focus();
            
            // Make sure the warning message is displayed in view on inital load
            serverExplorer.makeFooterVisible();
        });
    };
    
    
    var openFileForEditing = function(file) {
        
        // Obtain file name for title
        var index = file.path.lastIndexOf("/");
        if(index === -1) {
            index = file.path.lastIndexOf("\\");
        }
        var fileName = file.path.substring(index + 1);
        
        // Update title
        setNavbarTitleText(fileName);
        
        // Show editor buttons on navigation bar
        showControlById("navbarEditorSection", false);
        
        editor.openFileForEditing(file, serverSchemaInTextFormat, featureList, validatorList);
        
        showControlById("editor", false);
        
        // Hide progress
        core.hideControlById("progress", false, false);
        
        // Focus title for accessibility compliance
        $("#navigationBarTitle").focus();
    };
    
    
    var getDataFromURLHash = function() {
        
        var serverId = null;
        var filePath = null;

        var hash = null;
        if(embedded === constants.EMBEDDED_EXPLORE_TOOL) {
            hash = window.location.hash;
        } else {
            hash = window.top.location.hash;
        }
        
        if(hash !== null && hash !== undefined && hash.length > 14 && hash.substring(0, constants.SERVER_CONFIG_HASH.length + 2) === "#" + constants.SERVER_CONFIG_HASH + "/") {
                
            var value = hash.substring(14);
            var lastcommaIndex = value.lastIndexOf(",");
            
            if(lastcommaIndex > 0) {
                var slashIndex = value.indexOf("/", lastcommaIndex + 1);
                if(slashIndex !== -1) {
                    serverId = value.substring(0, slashIndex);
                    filePath = value.length > slashIndex + 1? value.substring(slashIndex + 1) : null;
                } else {
                    serverId = value;
                }
            } else {
                filePath = value;
            }
        }
        return {
            serverId: serverId,
            filePath: filePath
        };

    };

    
    var setNavbarTitleText = function(title) {
        $("#navigationBarTitle").text(title);
    };

    
    /**
     * messageType: "success" or "info" (default) or "warning" or "danger" 
     */
    var renderMessage = function(messageText, messageType, dismissable, clearPreviousMessages, jqXHR, append) {
        var messageContainer = $("#messageContainer");
        if(clearPreviousMessages) {
            messageContainer.empty();
        }
        var alert = $("<div class=\"alert\" role=\"alert\" />");    
        if(!messageType) {
            messageType = "info";
        }
        alert.addClass("alert-" + messageType);
        if(dismissable) {
            alert.addClass("alert-dismissable");
            alert.append("<button type=\"button\" role=\"button\" class=\"close\" data-dismiss=\"alert\" aria-label=\"Close\"><span aria-hidden=\"true\">&times;</span></button>");
        }
        alert.append(messageText);
        if(jqXHR && jqXHR.responseText) {
            alert.append("<br><br>");
            alert.append(jqXHR.responseText);
        }
        if (append) {
            messageContainer.append(alert);
        } else {
            messageContainer.prepend(alert);
        }
    };
    
    
    var showControlById = function(id, animate, timeUnits) {
        if(!timeUnits) {
            timeUnits = 2;
        }
        var deferred = new $.Deferred();
        var control = $("#" +  id);
        control.stop();
        if(animate) {
            control.css("opacity", "0");
            control.removeClass("hidden");
            control.animate({
                opacity: 1
            }, timeUnits * constants.ANIMATION_TIME_UNIT, function() {
                deferred.resolve();
            });
        } else {
            control.stop();
            control.css("opacity", "1");
            control.removeClass("hidden");
            deferred.resolve();
        }
        return deferred;
    };
    
    
    var hideControlById = function(id, animate, preserveSpace) {
        var deferred = new $.Deferred();
        var control = $("#" +  id);
        control.stop();
        if(animate) {
            control.animate({
                opacity: 0
            }, 3 * constants.ANIMATION_TIME_UNIT, function() {
                if(!preserveSpace) {
                    control.addClass("hidden");
                }
                deferred.resolve();
            });
        } else {
            if(preserveSpace) {
                control.css("opacity", "0");
            } else {
                control.addClass("hidden");
            }
            deferred.resolve();
        }
        return deferred;
    };
    
    
    var hideAllControls = function() {
        $("#messageContainer").empty();
        hideControlById("navbarChangeServerSection", false, false);
        hideControlById("navbarEditorSection", false, false);
        hideControlById("navbarSignOutSection", false, false);
        hideControlById("serverExplorer", false, false);
        hideControlById("fileExplorer", false, false);
        hideControlById("editor", false, false);
        hideControlById("navbarEditorChangesSavedMessage", false, false);
    };
    
    
    var retrieveServerData = function(forceServerRefresh) {
        var deferred = new $.Deferred();
        if(!serverDataInitialized || forceServerRefresh) {
            var retrieveServerWritePathsPromise = fileUtils.retrieveServerWritePaths();
            var retrieveServerVariableValuesPromise = fileUtils.retrieveServerVariableValues();
            var retrieveSchemaFilePromise = fileUtils.retrieveSchemaFile();
            var retrieveFeatureListPromise = fileUtils.retrieveFeatureList();
            $.when(retrieveServerWritePathsPromise, retrieveServerVariableValuesPromise, retrieveSchemaFilePromise, retrieveFeatureListPromise).done(function(serverWritePaths, serverVariableValues, schemaFile, serverFeatureList) {
                serverDataInitialized = true;
                serverSchemaInTextFormat = schemaFile;
                featureList = serverFeatureList;
                deferred.resolve();
            }).fail(function(jqXHR) {
                errorOnLastLoad = true;
                deferred.reject(jqXHR);
            });
        } else {
            deferred.resolve();
        }
        return deferred;
    };

    
    var updateNavigation = function(hash) {
        if(embedded !== constants.EMBEDDED_EXPLORE_TOOL) {
            window.top.history.pushState(null, null, hash);
        }
    };

    
    var getFileURL = function(filePath) {
        var baseURL = window.top.location.href.split("#")[0];
        if(embedded === constants.EMBEDDED_EXPLORE_TOOL) {
            if(environment === constants.ENVIRONMENT_COLLECTIVE && serverId !== null && serverId !== undefined) {
                return baseURL + "#" + constants.EXPLORE_TOOL_HASH  + "/" + constants.EXPLORE_TOOL_SERVERS_SEGMENT + "/" + serverId + "/" + constants.EXPLORE_TOOL_CONFIGURE_SEGMENT + "/" + filePath;
            } else {
                return baseURL + "#" + constants.EXPLORE_TOOL_HASH + "/" + constants.EXPLORE_TOOL_CONFIGURE_SEGMENT + "/" + filePath;
            }
        } else {
            var hash = "#" + constants.SERVER_CONFIG_HASH + "/";
            if(environment === constants.ENVIRONMENT_COLLECTIVE && serverId !== null && serverId !== undefined) {
                hash = hash + serverId + "/";
            }
            return baseURL + hash + filePath;
        }
    };

    
    var setFile = function(filePath) {
        if(embedded === constants.EMBEDDED_EXPLORE_TOOL) {
            if(environment === constants.ENVIRONMENT_COLLECTIVE) {
//                window.top.history.pushState(null, null, "#" + constants.EXPLORE_TOOL_HASH  + "/" + constants.EXPLORE_TOOL_SERVERS_SEGMENT + "/" + serverId + "/" + constants.EXPLORE_TOOL_CONFIGURE_SEGMENT  + "/" + filePath);
                window.top.location.hash = "#" + constants.EXPLORE_TOOL_HASH  + "/" + constants.EXPLORE_TOOL_SERVERS_SEGMENT + "/" + serverId + "/" + constants.EXPLORE_TOOL_CONFIGURE_SEGMENT  + "/" + filePath;
            } else {
//                window.top.history.pushState(null, null, "#" + constants.EXPLORE_TOOL_HASH + "/" + constants.EXPLORE_TOOL_CONFIGURE_SEGMENT + "/" + filePath);
                window.top.location.hash = "#" + constants.EXPLORE_TOOL_HASH + "/" + constants.EXPLORE_TOOL_CONFIGURE_SEGMENT + "/" + filePath;
            }
        }
        
        var hash = "#" + constants.SERVER_CONFIG_HASH + "/";
        if(environment === constants.ENVIRONMENT_COLLECTIVE && serverId !== null && serverId !== undefined) {
            hash = hash + serverId + "/";
        }
        hash = hash + filePath;
        updateNavigation(hash);
        startup();
    };
    
    
    var clearFile = function() {
        if(embedded === constants.EMBEDDED_EXPLORE_TOOL) {
            if(environment === constants.ENVIRONMENT_COLLECTIVE) {
//                window.top.history.pushState(null, null, "#" + constants.EXPLORE_TOOL_HASH + "/" + constants.EXPLORE_TOOL_SERVERS_SEGMENT + "/" + serverId + "/" + constants.EXPLORE_TOOL_CONFIGURE_SEGMENT );
                window.top.location.hash = "#" + constants.EXPLORE_TOOL_HASH + "/" + constants.EXPLORE_TOOL_SERVERS_SEGMENT + "/" + serverId + "/" + constants.EXPLORE_TOOL_CONFIGURE_SEGMENT ;
            } else {
//                window.top.history.pushState(null, null, "#" + constants.EXPLORE_TOOL_HASH + "/" + constants.EXPLORE_TOOL_CONFIGURE_SEGMENT);
                window.top.location.hash = "#" + constants.EXPLORE_TOOL_HASH + "/" + constants.EXPLORE_TOOL_CONFIGURE_SEGMENT;
            }
        }
        
        var hash = "#" + constants.SERVER_CONFIG_HASH + "/";
        if(environment === constants.ENVIRONMENT_COLLECTIVE && serverId !== null && serverId !== undefined) {
            hash = hash + serverId;
        }
        updateNavigation(hash);
        startup();
    };
    
    
    var setServer = function(serverId) {
        updateNavigation("#" + constants.SERVER_CONFIG_HASH + "/" + serverId);
        startup();
    };
    
    
    var clearServer = function() {
        updateNavigation("#" + constants.SERVER_CONFIG_HASH + "/");
        startup();
    };

    
    var applyJmxRoutingHeaders = function(xhr) {
        if(core.collectiveRoutingRequired()) {
            
            var serverId = core.getCollectiveServerId();
            
            var firstCommaIndex = serverId.indexOf(",");
            var lastCommaIndex = serverId.lastIndexOf(",");
    
            var hostName = serverId.substring(0, firstCommaIndex);
            var userDir = serverId.substring(firstCommaIndex + 1, lastCommaIndex);
            var serverName = serverId.substring(lastCommaIndex + 1, serverId.length);
            
            xhr.setRequestHeader("com.ibm.websphere.jmx.connector.rest.routing.hostName", hostName);
            xhr.setRequestHeader("com.ibm.websphere.jmx.connector.rest.routing.serverName", serverName);
            xhr.setRequestHeader("com.ibm.websphere.jmx.connector.rest.routing.serverUserDir", userDir);
        }        
    };


    // Apply the three custom headers for REST API calls to roots that start with /ibm/api/
    var applyRestRoutingHeaders = function(xhr) {
        var hostName = collectiveServerData.host;
        var serverUserDir = collectiveServerData.wlpUserDir;
        var serverName = collectiveServerData.name;

        xhr.setRequestHeader("collective.hostNames", hostName);
        xhr.setRequestHeader("collective.serverNames", serverName);
        xhr.setRequestHeader("collective.serverUserDirs", serverUserDir);
    };
    
    
    var centerDialog = function(modalElement) {
        var modal = $(modalElement);
        var dialog = modal.find(".modal-dialog");
        modal.css("display", "block");
        dialog.css("margin-top", 40 + Math.max(0, ($(window).height() - dialog.height()) / 4));
    };
    
    
    var setUIBlocked = function(blocked) {
        uiBlocked = blocked;
        if(uiBlocked) {
            showControlById("uiBlock");
        } else {
            hideControlById("uiBlock");
        }
    };
    
    
    var isInIFrame = function() {
        return window.frameElement !== null && window.frameElement !== undefined;
    };


    // Ask the server what configuration validators are available
    // return null if unable to find the validators
    var getListOfValidators = function() {
        var deferred = new $.Deferred();
        
        var isServerRunningPromise = isServerRunning(serverId);
        $.when(isServerRunningPromise).done(function(isRunning){
            if(! isRunning) {
                deferred.resolve(null);
            }
            var uri = "/ibm/api/docs?root=" + validatorUri;
            $.ajax({
                url: uri,
                type: 'GET',
                dataType: 'json',
                beforeSend: collectiveRoutingRequired() ? applyRestRoutingHeaders: null,
                success: function(data) {
                    var elementWithValidators = parseOutValidators(data);
                    deferred.resolve(elementWithValidators);
                },
                error: function() {
                    deferred.resolve(null);
                }
            });
        });
        return deferred;
    };


    // Parse out the elements that are supposed by the validator APIs
    // We want only the validators that can be performed against single configuration elements
    // There may be validators that automatically validate all the elements in the server's 
    // configuration which we do not want
    // return null if unable to find the validators
    var parseOutValidators = function(data) {
        if(!data || !data.paths) {
            // input check to make sure paths exist in the apiDiscovery payload for validator docs
            return null;
        }
        var listOfValidators = [];
        var listOfValidatorApis = Object.keys(data.paths);
        for(var i = 0; i < listOfValidatorApis.length; i++) {
            // We want to parse the string right after the base validator URI
            // to build a list of types that can be validated by the validator feature
            var uri = listOfValidatorApis[i];
            if(uri.indexOf(validatorUri) === -1) {
                continue; // skip this loop iteration if unsupported api
            }
            var temp = uri.substring(validatorUri.length);
            var uriParts = temp.split("/"); // index 0 should be blank
            var resourceType = uriParts[1];
            var individualResource = uriParts[2];
            if($.inArray(resourceType, listOfValidators) === -1 && individualResource) {
                // Only add if the resource has a validator and the validator can be used against individual
                // resources (vs. the entire server configuration);
                // TODO: This is not always true.  Do not have to have a singleton to indicate singleton support.
                if(individualResource.search("{.*}") === 0) {
                    // Typically REST APIs are formulated as 
                    // /<resourceType>/<individual resource identification>/...
                    // Make sure we see a parameterized uri part after the resource type in the REST API
                    // so that we know the validator can be performed on a single resource in the configuration.
                    listOfValidators.push(resourceType);
                }
            }
        }
        return listOfValidators;
    };


    // Parse the api docs json for HTTP methods and their parameters
    var getListOfHttpMethodsAndParameters = function(apiDocs) {
        var listOfHttpMethods = [];
        // TODO: Implement
        return listOfHttpMethods;
    };
    
    
    var encodeServerId = function(serverId) {
        var firstCommaIndex = serverId.indexOf(",");
        var lastCommaIndex = serverId.lastIndexOf(",");

        var hostName = serverId.substring(0, firstCommaIndex);
        var userDir = serverId.substring(firstCommaIndex + 1, lastCommaIndex);
        var serverName = serverId.substring(lastCommaIndex + 1, serverId.length);
        
        return hostName + "," + encodeURIComponent(userDir) + "," + serverName;
    };


    var isServerRunning = function(hostAndServerId) {
        if(environment === constants.ENVIRONMENT_LOCAL) {
            return isStandaloneServerRunning();
        } else if (environment === constants.ENVIRONMENT_COLLECTIVE) {
            return isCollectiveServerRunning(hostAndServerId);
        } else {
            // Unknown environment, return false since we do not know 
            // the status of the server
            var deferred = new $.Deferred();
            deferred.resolve(false);
            return deferred;
        }
    };


    var isStandaloneServerRunning = function() {
        var deferred = new $.Deferred();
        var serverInfoUri = "/IBMJMXConnectorREST/mbeans/WebSphere%3Afeature%3Dkernel%2Cname%3DServerInfo/attributes";
        $.ajax({
            url: serverInfoUri,
            type: "get",
            dataType: "json",
            success: function() {
                deferred.resolve(true);
            },
            error: function() {
                deferred.resolve(false);
            }
        });
        return deferred;

    };


    // When looking at a server configuration in a collective environment, 
    // return true if that server is running, false otherwise
    var isCollectiveServerRunning = function(hostAndServerId) {
        var deferred = new $.Deferred();
        var serverUri = "/ibm/api/collective/v1/servers/" + hostAndServerId;
        $.getJSON(serverUri, function(data) {
            var isServerStarted = false;
            collectiveServerData = data;
            var serverStatus = collectiveServerData.state;
            if(serverStatus === constants.STARTED) {
                isServerStarted = true;
            }
            deferred.resolve(isServerStarted);
        })
        .fail(function() {
            // If the controller cannot tell us if the server is running,
            // then we should just assume the server status is unknown.
            deferred.resolve(false);
        });
        return deferred;
    };


    var collectiveRoutingRequired = function() {
        return environment === constants.ENVIRONMENT_COLLECTIVE && serverId !== hostServerId;
    };


    $(document).ready(function() {
        
        $(window).on("popstate", function() {
            var skip = false;
            if(embedded !== null && embedded !== undefined && window.top.location.hash.length === 0) {
                skip = true;
            }
            if(!skip) {
                if(embedded === constants.EMBEDDED_EXPLORE_TOOL) {
                    var filePath = getDataFromURLHash().filePath;
                    if(environment === constants.ENVIRONMENT_COLLECTIVE) {
//                        window.top.history.pushState(null, null, "#" + constants.EXPLORE_TOOL_HASH + "/" + constants.EXPLORE_TOOL_SERVERS_SEGMENT + "/" + serverId + "/" + constants.EXPLORE_TOOL_CONFIGURE_SEGMENT + (filePath !== null && filePath !== undefined? "/" + filePath : ""));
                        window.top.location.hash = "#" + constants.EXPLORE_TOOL_HASH + "/" + constants.EXPLORE_TOOL_SERVERS_SEGMENT + "/" + serverId + "/" + constants.EXPLORE_TOOL_CONFIGURE_SEGMENT + (filePath !== null && filePath !== undefined? "/" + filePath : "");
                    } else {
//                        window.top.history.pushState(null, null, "#" + constants.EXPLORE_TOOL_HASH  + "/" + constants.EXPLORE_TOOL_CONFIGURE_SEGMENT + (filePath !== null && filePath !== undefined? "/" + filePath : ""));
                        window.top.location.hash = "#" + constants.EXPLORE_TOOL_HASH  + "/" + constants.EXPLORE_TOOL_CONFIGURE_SEGMENT + (filePath !== null && filePath !== undefined? "/" + filePath : "");
                    }
                } 
                startup();
            }
        });    
        
        $(window).on("keydown", function(event) {
            if(uiBlocked && event.keyCode !== 116) {
                return false;
            }
        });
        
    });
    

    return {
        messages: null,
        startup: startup,
        renderMessage: renderMessage,
        showControlById: showControlById,
        hideControlById: hideControlById,
        setNavbarTitleText: setNavbarTitleText,
        centerDialog: centerDialog,
        setServer: setServer,
        clearServer: clearServer,        
        setFile: setFile,
        clearFile: clearFile,
        setUIBlocked: setUIBlocked,
        getFileURL: getFileURL,
        collectiveRoutingRequired: collectiveRoutingRequired,
        getCollectiveServerId: function() {
            return serverId;
        },
        isCollectiveServerRunning: function() {
            return collectiveServerIsRunning;
        },
        encodeServerId: encodeServerId,
        applyJmxRoutingHeaders: applyJmxRoutingHeaders,
        applyRestRoutingHeaders: applyRestRoutingHeaders
    };
    
})();
