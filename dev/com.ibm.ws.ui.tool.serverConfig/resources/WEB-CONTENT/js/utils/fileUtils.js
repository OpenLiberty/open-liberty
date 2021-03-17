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

var fileUtils = (function() {
    "use strict";

    var serverVariables = [
    {    name: "wlp.install.dir",
        resolvedPath: null
    },
    {    name: "wlp.user.dir",
        resolvedPath: null
    },
    {    name: "server.config.dir",
        resolvedPath: null
    },
    {    name: "server.output.dir",
        resolvedPath: null
    },
    {    name: "shared.app.dir",
        resolvedPath: null
    },
    {    name: "shared.config.dir",
        resolvedPath: null
    },
    {    name: "shared.resource.dir",
        resolvedPath: null
    } ];
    
    var serverWritePaths = null;
    

    var resolveIncludeLocations = function(contextLocation, includeLocations) {
        var deferred = new $.Deferred();
            var resolvedConfigurationFiles = [];
            var deferredCounter = 0;
            
            var resolveFunction = function(index, fileLocation) {
                resolvedConfigurationFiles[index] = {
                        location: fileLocation
                };
                resolveIncludeLocation(contextLocation, fileLocation).done(function(metadata) {
                    resolvedConfigurationFiles[index].resolvedLocation = metadata.fileName;
                    resolvedConfigurationFiles[index].editable = metadata.readOnly === "false" && isPathWritable(metadata.fileName);
                }).fail(function() {
                    resolvedConfigurationFiles[index].resolvedLocation = null;
                    resolvedConfigurationFiles[index].editable = false;
                }).always(function() {
                    deferredCounter--;
                    if(deferredCounter === 0) {
                        deferred.resolve(resolvedConfigurationFiles);
                    }
                });
            };
            
            for(var i = 0; i < includeLocations.length; i ++) {
                deferredCounter++;
                var fileLocation = includeLocations[i];
                resolveFunction(i, fileLocation);
            }
        return deferred;
    };


    var resolveIncludeLocation = function(contextLocation, fileLocation) {
        var deferred = new $.Deferred();
        var retrieveFileMetadataPromise = retrieveFileMetadata(contextLocation + "/" + fileLocation).done(function(metadata) {
            deferred.resolve(metadata);
        }).fail(function(textStatus) {
            if(textStatus === "abort") {
                return;
            }
            retrieveFileMetadataPromise = retrieveFileMetadata("${server.config.dir}" + fileLocation).done(function(metadata) {
                deferred.resolve(metadata);
            }).fail(function(textStatus) {
                if(textStatus === "abort") {
                    return;
                }
                retrieveFileMetadataPromise = retrieveFileMetadata("${shared.config.dir}" + fileLocation).done(function(metadata) {
                    deferred.resolve(metadata);
                }).fail(function(textStatus) {
                    if(textStatus === "abort") {
                        return;
                    }
                    retrieveFileMetadataPromise = retrieveFileMetadata(fileLocation).done(function(metadata) {
                        deferred.resolve(metadata);
                    }).fail(function(textStatus) {
                        if(textStatus === "abort") {
                            return;
                        }
                        deferred.reject();
                    });
                });
            });
        });
        deferred.abort = function() {
            retrieveFileMetadataPromise.abort();
        };
        return deferred;
    };

    
    var retrieveFile = function(filePath) {
        var deferred = new $.Deferred();
        var retrieveFileMetadataPromise = retrieveFileMetadata(filePath);
        var retrieveFileContentPromise = retrieveFileContent(filePath);
        $.when(retrieveFileMetadataPromise, retrieveFileContentPromise).done(function(fileMetadata, fileContent) {
            var file = {
                path: applyVariablesToFilePath(filePath),
                resolvedPath: fileMetadata.fileName,
                content: fileContent,
                isReadOnly: fileMetadata.readOnly === "true"? true : !isPathWritable(fileMetadata.fileName)
            };
            deferred.resolve(file);
        }).fail(function(error) {
            deferred.reject();
        });
        return deferred;
    };
    

    var retrieveFileContent = function(filePath) {
        var deferred = new $.Deferred();

        $.ajax({
            url: "/IBMJMXConnectorREST/file/" + encodeURIComponent(filePath),
            dataType: "text",
            beforeSend: core.applyJmxRoutingHeaders,
            cache: false,
            success: function(response) {
                deferred.resolve(response);
            },
            error: function(jqXHR) {
                deferred.reject(jqXHR);
            }
        });
        return deferred;
    };
    
    
    var retrieveFileMetadata = function(filePath) {
        var deferred = new $.Deferred();
        var ajaxPromise = null;

        if(core.collectiveRoutingRequired()) {
            
            // Get metadata from collective member
            var encodedCollectiveServerId = core.encodeServerId(core.getCollectiveServerId());
            ajaxPromise = $.ajax({
                url: "/ibm/api/collective/v1/servers/" + encodedCollectiveServerId + "/fileservice/" + encodeURIComponent(filePath),
                dataType: "json",
                success: function(response) {
                    if(response.resolvedPath !== null && response.resolvedPath !== undefined && response.readOnly !== null && response.readOnly !== undefined) {
                        var metadata = {
                                "fileName": response.resolvedPath,
                                "readOnly": response.readOnly.toString()
                        };
                        deferred.resolve(metadata);
                    } else {
                        deferred.reject();
                    }
                },
                error: function(jqXHR) {
                    deferred.reject(jqXHR);
                }
            });
            
            
        } else {
            
            // Get metadata from local server
            ajaxPromise = $.ajax({
                url: "/IBMJMXConnectorREST/mbeans/WebSphere%3Afeature%3DrestConnector%2Ctype%3DFileService%2Cname%3DFileService/operations/getMetaData",
                type: "POST",
                contentType: "application/json",
                data: "{\"params\":[{\"value\":\"" + filePath.replace(/\\/g, "\\\\") + "\",\"type\": \"java.lang.String\"},{\"value\": \"r\",\"type\":\"java.lang.String\"}],\"signature\":[\"java.lang.String\",\"java.lang.String\"]}",
                success: function(response) {
                    if(response.value !== null && response.value !== undefined) {
                        deferred.resolve(response.value);
                    } else {
                        deferred.reject();
                    }
                },
                error: function(jqXHR) {
                    deferred.reject(jqXHR);
                }
            });
        }
        deferred.abort = function() {
            if(ajaxPromise !== null && ajaxPromise !== undefined) {
                ajaxPromise.abort();
            }
        };
        return deferred;
    };
    
    
    var isPathWritable = function(path) {
        if(serverWritePaths !== null && serverWritePaths !== undefined) {
            for(var i = 0; i < serverWritePaths.length; i ++) {
                if(path.indexOf(serverWritePaths[i]) === 0) {
                    return true;
                }
            }
        }
        return false;
    };
    
    
    var applyVariablesToFilePath = function(filePath) {
        for(var i = 0; i < serverVariables.length; i++) {
            var variableResolvedPath = serverVariables[i].resolvedPath;
            if(variableResolvedPath !== null && variableResolvedPath !== undefined) {
                if(filePath.indexOf(serverVariables[i].resolvedPath) === 0) {
                    return "${" + serverVariables[i].name + "}" + filePath.substring(variableResolvedPath.length);
                }
            }
        }
        return filePath;
    };
    
    
    var retrieveServerVariableValues = function() {
        var deferred = new $.Deferred();
        var counter = 0;
        serverVariables.forEach(function(variable) {
            //TODO: trailing slash shouldn't be included, it's added here due to a bug in the collective metadata service
            retrieveFileMetadata("${" + variable.name + "}/").done(function(metadata) {
                if(metadata.fileName.charAt(metadata.fileName.length - 1) === "/") {
                    metadata.fileName = metadata.fileName.substring(0, metadata.fileName.length -1);
                }
                variable.resolvedPath = metadata.fileName;
            }).fail(function(jqXHR) {
                variable.resolvedPath = "UNDEFINED";
                console.warn('Server variable "' + variable.name + '" could not be retrieved.');
            }).always(function() {
                counter++;
                if(counter === serverVariables.length) {
                    serverVariables.sort(function(a, b) {
                        return b.resolvedPath.length - a.resolvedPath.length;
                    });
                    deferred.resolve();
                }
            });
        });
        return deferred;
    };
    
    
    var retrieveServerWritePaths = function() {
        var deferred = new $.Deferred();
        
        var url = null;
        var collectiveRoutingRequired = core.collectiveRoutingRequired();
        if(collectiveRoutingRequired) {
            var encodedCollectiveServerId = core.encodeServerId(core.getCollectiveServerId());
            url = "/ibm/api/collective/v1/servers/" + encodedCollectiveServerId + "/fileservice";
        } else {
            url = "/IBMJMXConnectorREST/mbeans/WebSphere%3Afeature%3DrestConnector%2Ctype%3DFileService%2Cname%3DFileService/attributes/WriteList";
        }
        
        $.ajax({
            url: url,
            dataType: "json",
            success: function(response) {
                if(collectiveRoutingRequired) {
                    serverWritePaths = response.writeDirs;
                } else {
                    serverWritePaths = response.value;
                }
                deferred.resolve();
            },
            error: function(jqXHR) {
                deferred.reject(jqXHR);
            }
        });
        
        return deferred;
    };
    
    
    var retrieveSchemaFile = function() {
        var deferred = new $.Deferred();
        
        var localStorageKey = "com.ibm.ws.sce.cached_schema_";
        var localStorageLocaleKey = "com.ibm.ws.sce.cached_schema_locale_";
        
        // Obtain browser language
        var locale = globalization.getLanguageCode();
        
        // Ensure locale format is the one accepted by the server runtime
        locale = locale.replace("-", "_");

        if(core.collectiveRoutingRequired()) {
            var serverId = core.getCollectiveServerId();
            localStorageKey = localStorageKey + "remote_" + serverId.substring(0, serverId.lastIndexOf(","));
        } else {
            localStorageKey = localStorageKey + "local_" + window.location.hostname;
        }
        
        var schemaFromLocalStorage = localStorage.getItem(localStorageKey);
        var localeFromLocalStorage = localStorage.getItem(localStorageLocaleKey);
        if(schemaFromLocalStorage !== null && schemaFromLocalStorage !== undefined && locale === localeFromLocalStorage ) {
            deferred.resolve(schemaFromLocalStorage);
        } else {
            retrieveSchemaFileFromServer().done(function(schemaFromServer) {
                var retry;
                do {
                    retry = false;
                    try {
                        localStorage.setItem(localStorageKey, schemaFromServer);
                        localStorage.setItem(localStorageLocaleKey, locale);
                    } catch(error) {
                        if(localStorage.length > 0) {
                            localStorage.removeItem(localStorage.key(0));
                            retry = true;
                        }
                    }
                } while (retry);
                deferred.resolve(schemaFromServer);
            }).fail(function(jqXHR) {
                deferred.reject(jqXHR);
            });
        }
        return deferred;
    };
    
    
    var retrieveSchemaFileFromServer = function() {
        var deferred = new $.Deferred();
        
        // Obtain browser language
        var locale = globalization.getLanguageCode();
        
        // Ensure locale format is the one accepted by the server runtime
        locale = locale.replace("-", "_");
        
        if(core.collectiveRoutingRequired()) {

            // Get metadata from collective member
            var serverId = core.getCollectiveServerId();
            var hostName = serverId.substring(0, serverId.indexOf(","));
            var encodedCollectiveServerId = core.encodeServerId(serverId);

            // Retrieve install dir
            $.ajax({
                url: "/ibm/api/collective/v1/servers/" + encodedCollectiveServerId
            }).done(function(data) {
                var installDir = data.wlpInstallDir;

                // Retrieve schema from remote server
                $.ajax({
                    url: "/ibm/api/collective/v1/runtimes/" + hostName + "," + encodeURIComponent(installDir) + "/schema?locale=" + locale + "&compactOutput=true&forceGeneration=true",
                    dataType: "text"
                }).done(function(schema) {
                    deferred.resolve(schema);
                }).fail(function(jqXHR) {
                    deferred.reject(jqXHR);
                });
            }).fail(function(jqXHR) {
                deferred.reject(jqXHR);
            });            
            
        } else {
            
            // Retrieve schema from local server
            $.ajax({
                url: "/IBMJMXConnectorREST/mbeans/WebSphere%3Aname%3Dcom.ibm.ws.config.serverSchemaGenerator/operations/generateInstallSchema",
                type: "POST",
                contentType: "application/json",
                data: "{\"params\":[{\"value\": null, \"type\": \"java.lang.String\" },{\"value\": null, \"type\": \"java.lang.String\" },{\"value\": null, \"type\": \"java.lang.String\" },{\"value\": \"" + locale + "\", \"type\": \"java.lang.String\" }, {\"value\": \"true\", \"type\": \"java.lang.Boolean\"}], \"signature\": [ \"java.lang.String\", \"java.lang.String\", \"java.lang.String\", \"java.lang.String\", \"boolean\" ]}",
                success: function(responseData) {
                    var filePath = responseData.value.keyFilePath;
                    $.ajax({url: "/IBMJMXConnectorREST/file/" + encodeURIComponent(filePath),
                        dataType: "text",
                        cache: false,
                        success: function(responseData) {
                            // Delete temporary file
                            $.ajax({
                                url: "/IBMJMXConnectorREST/file/" + encodeURIComponent(filePath),
                                type: "DELETE"
                            });
                            deferred.resolve(responseData);
                        },
                        error: function(jqXHR) {
                            deferred.reject(jqXHR);
                        }
                    });
                }, error: function(jqXHR) {
                    deferred.reject(jqXHR);
                }
            });
        }

        return deferred;
    };
    
    var retrieveFeatureList = function() {
        var deferred = new $.Deferred();
        
        var localStorageKey = "com.ibm.ws.sce.cached_feature_list_";
        var localStorageLocaleKey = "com.ibm.ws.sce.cached_feature_locale_";        
        
        // Obtain browser language
        var locale = globalization.getLanguageCode();
        
        // Ensure locale format is the one accepted by the server runtime
        locale = locale.replace("-", "_");
        
        if(core.collectiveRoutingRequired()) {
            var serverId = core.getCollectiveServerId();
            localStorageKey = localStorageKey + "remote_" + serverId.substring(0, serverId.lastIndexOf(","));
        } else {
            localStorageKey = localStorageKey + "local_" + window.location.hostname;
        }
        
        var featureListFromLocalStorage = localStorage.getItem(localStorageKey);        
        var localeFromLocalStorage = localStorage.getItem(localStorageLocaleKey);
        if(featureListFromLocalStorage !== null && featureListFromLocalStorage !== undefined && locale === localeFromLocalStorage ) {
            var featureList = JSON.parse(featureListFromLocalStorage);
            deferred.resolve(featureList);
        } else {
            retrieveFeatureListFromServer().done(function(featureListDocument) {
                
                if(featureListDocument !== null && featureListDocument !== undefined) {
                
                    var featureList = {};
                    
                    for(var child = featureListDocument.firstChild.firstChild; child !== null && child !== undefined; child = child.nextSibling) {
                        if(child.nodeType === 1 && child.nodeName === "feature") {
                            
                            var featureName = child.getAttribute("name");
                            
                            var feature = {};
                            feature.displayName = "";
                            feature.description = "";
                            feature.enables = [];
                            
                            for(var grandChild = child.firstChild; grandChild !== null && grandChild !== undefined; grandChild = grandChild.nextSibling) {
                                if(grandChild.nodeType === 1) {
                                    switch(grandChild.nodeName) {
                                    case "displayName":
                                        feature.displayName = grandChild.firstChild.nodeValue;
                                        break;
                                    case "description":
                                        feature.description = grandChild.firstChild.nodeValue;
                                        break;
                                    case "enables":
                                        feature.enables.push(grandChild.firstChild.nodeValue);
                                    }
                                    
                                }
                            }
                            featureList[featureName] = feature;
                        }
                    }
                    
                    if(featureList === null || featureList === undefined) {
                        localStorage.removeItem(localStorageKey);
                    } else {                    
                        try {
                            var serializedFeatureList = JSON.stringify(featureList);
                            localStorage.setItem(localStorageKey, serializedFeatureList);
                            localStorage.setItem(localStorageLocaleKey, locale);
                        } catch(error) {
                            // Browser caching fail
                        }
                    }
    
                    deferred.resolve(featureList);
                    
                } else {
                    deferred.resolve(null);
                }
                
            }).fail(function(jqXHR) {
                deferred.reject(jqXHR);
            });
        }
        return deferred;
    };
    

        
    var retrieveFeatureListFromServer = function() {
        var deferred = new $.Deferred();
        
        var locale = globalization.getLanguageCode();
        
        // Ensure locale format is the one accepted by the server runtime
        locale = locale.replace("-", "_");
        
        if(core.collectiveRoutingRequired()) {
            
            // No featureList API for collective is currently available
            deferred.resolve(null);
            
        } else {
            
            // Retrieve feature list from local server
            $.ajax({
                url: "/IBMJMXConnectorREST/mbeans/WebSphere%3Aname%3Dcom.ibm.websphere.config.mbeans.FeatureListMBean/operations/generate",
                type: "POST",
                contentType: "application/json",
                data: "{\"params\":[{\"value\": \"utf-8\", \"type\": \"java.lang.String\" },{\"value\": \"" + locale + "\", \"type\": \"java.lang.String\" }, {\"value\": null, \"type\": \"java.lang.String\" }], \"signature\": [ \"java.lang.String\", \"java.lang.String\", \"java.lang.String\"]}",
                success: function(responseData) {
                    var filePath = responseData.value.keyFilePath;
                    $.ajax({url: "/IBMJMXConnectorREST/file/" + encodeURIComponent(filePath),
                        //dataType: "text",
                        cache: false,
                        success: function(responseData) {
                            // Delete temporary file
                            $.ajax({
                                url: "/IBMJMXConnectorREST/file/" + encodeURIComponent(filePath),
                                type: "DELETE"
                            });
                            deferred.resolve(responseData);
                        },
                        error: function(jqXHR) {
                            deferred.reject(jqXHR);
                        }
                    });
                }, error: function(data) {
                    deferred.reject();
                }
            });
        }
        return deferred;
    };
    
    var isServerReadOnly = function() {
        return serverWritePaths === null || serverWritePaths === undefined || serverWritePaths.length === 0;
    };
    
    
    var getLastSlashIndex = function(value) {
        var index = value.lastIndexOf("/");
        if(index === -1) {
            index = value.lastIndexOf("\\");
        }
        return index;
    };
    
    
    var getPathFromFilePath = function(filePath) {
        return filePath.substring(0, getLastSlashIndex(filePath));
    };
    
    
    var getFileFromFilePath = function(filePath) {
        return filePath.substring(getLastSlashIndex(filePath) + 1);
    };

    
    var retrieveFileList = function(directory, includeDirectories, suffix) {
        var deferred = new $.Deferred();
        
        if(core.collectiveRoutingRequired()) {
            
            // Collective
            var encodedCollectiveServerId = core.encodeServerId(core.getCollectiveServerId());
            $.ajax({
                url: "/ibm/api/collective/v1/servers/" + encodedCollectiveServerId + "/fileservice/" + encodeURIComponent(directory) + "/children",
                success: function(response) {
                    if(response !== null && response !== undefined) {
                        var fileList = [];
                        for(var i = 0; i < response.length; i++) {
                            var fileEntry = response[i];
                            if(!fileEntry.isDirectory || includeDirectories) {                            
                                var fileName = fileEntry.path.substring(fileEntry.path.lastIndexOf("/") + 1);
                                if(suffix) {
                                    var index = fileName.toLowerCase().lastIndexOf(suffix);
                                    if(index !== -1 && fileName.length - index === suffix.length) {
                                        fileList.push(fileName);
                                    }
                                } else {
                                    fileList.push(fileName);
                                }
                            }
                        }
                        deferred.resolve(fileList);
                    } else {
                        deferred.reject();
                    }
                },
                error: function(jqXHR, textStatus) {
                    deferred.reject(textStatus);
                }
            });

        } else {

            // Local
            $.ajax({
                url: "/IBMJMXConnectorREST/mbeans/WebSphere%3Afeature%3DrestConnector%2Cname%3DFileService%2Ctype%3DFileService/operations/getDirectoryEntries",
                type: "POST",
                contentType: "application/json",
                data: "{\"params\": [{\"value\": \"" + directory + "\", \"type\": \"java.lang.String\" }, {\"value\": \"false\", \"type\": \"java.lang.Boolean\"}, {\"value\": \"d\", \"type\": \"java.lang.String\"}],\"signature\": [ \"java.lang.String\", \"boolean\",  \"java.lang.String\" ]}",
                success: function(response) {
                    if(response.value !== null && response.value !== undefined) {
                        var fileList = [];
                        for(var i = 0; i < response.value.length; i++) {
                            var fileEntry = response.value[i];
                            if(fileEntry.directory === "false" || includeDirectories) {                            
                                var fileName = fileEntry.fileName.substring(fileEntry.fileName.lastIndexOf("/") + 1);
                                if(suffix) {
                                    var index = fileName.toLowerCase().lastIndexOf(suffix);
                                    if(index !== -1 && fileName.length - index === suffix.length) {
                                        fileList.push(fileName);
                                    }
                                } else {
                                    fileList.push(fileName);
                                }
                            }
                        }
                        deferred.resolve(fileList);
                    } else {
                        deferred.reject();
                    }
                },
                error: function(jqXHR, textStatus) {
                    deferred.reject(textStatus);
                }
            });
        }
        return deferred;
    };
    
    
    var fileExists = function(filePath) {
        var deferred = new $.Deferred();
        retrieveFileMetadata(filePath).done(function() {
            deferred.resolve(true);
        }).fail(function() {
            deferred.resolve(false);
        });
        return deferred;
    };

    
    return {
        retrieveFile: retrieveFile,
        retrieveFileContent: retrieveFileContent,
        retrieveServerVariableValues: retrieveServerVariableValues,
        retrieveServerWritePaths: retrieveServerWritePaths,
        retrieveSchemaFile: retrieveSchemaFile,
        retrieveFeatureList: retrieveFeatureList,
        resolveIncludeLocations: resolveIncludeLocations,
        resolveIncludeLocation: resolveIncludeLocation,
        applyVariablesToFilePath: applyVariablesToFilePath,
        isServerReadOnly: isServerReadOnly,
        getPathFromFilePath: getPathFromFilePath,
        getFileFromFilePath: getFileFromFilePath,
        isPathWritable: isPathWritable,
        retrieveFileList: retrieveFileList,
        fileExists: fileExists
    };

})();
