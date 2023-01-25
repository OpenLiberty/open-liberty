/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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

 var validationUtils = (function() {
    "use strict";

    var VALIDATION_API_SCHEMA_URI = "/ibm/api/platform/validation/";
    var VALIDATION_URI_PREFIX = "/validation/";
    var VALIDATION_URI_SUFFIX = "/{uid}";
    var REFERENCE_PROPERTY_KEY = "$ref";
    var PARAMETER_URI_PREFIX = "#/components/parameters/";

    var AUTH = "auth";
    var AUTH_ALIAS = "authAlias";
    var LOGIN_CONFIG = "loginConfig";
    var LOGIN_CONFIG_PARAMETERS = "X-Login-Config-Props";
    var APPLICATION_AUTHENTICATION = {
        USER: "X-Validation-User",
        PASSWORD: "X-Validation-Password"
    };

    // validation datastore object
    var validationMetaDataObject = {
        isEnabled: false,
        metaData: {}
    };


    // Read validation api schema and extract metadata that are needed for enabling the test connection feature
    var retrieveSchema = function() {
        validationMetaDataObject.isEnabled = false;
        validationMetaDataObject.metaData = {};

        return getValidationApiSchema()
            .then(function(result){
                validationMetaDataObject.isEnabled = false;
                if(result && result.success) {
                    var metaDataObject = {};
                    var data = result.data;
                    var isValidationEnabled = false;
                    if(data && data.paths) {
                        $.each(data.paths, function( key, nodeMetaData ) {
                            var nodeName = extractNodeNameFromValidationUri(key);
                            if(nodeName) {
                                if(!metaDataObject[nodeName]) {
                                    metaDataObject[nodeName] = {};
                                }
                                metaDataObject = appendParametersToNodeMetaData(nodeName, nodeMetaData, metaDataObject);
                                if(metaDataObject[nodeName].isEnabled) {
                                    isValidationEnabled = true;
                                }
                            }
                        });
                    }

                    if(isValidationEnabled) {
                        validationMetaDataObject.isEnabled = true;
                        validationMetaDataObject.metaData = metaDataObject;
                    }
                }

                return validationMetaDataObject;
            });
    };

    
    // Fetch authConfig info and update it in the UI
    var getValidationApiSchema = function() {
        var successCallback = function(data) {
            return {
                success: true,
                data: data
            };
        };

        var failureCallback = function(data) {
            return {
                success: false,
                data: data
            };
        };

        return getDefferedXHR(VALIDATION_API_SCHEMA_URI, successCallback, failureCallback);   
    };

    // XHR template to perform the ajax calls with promise
    var getDefferedXHR = function(uri, successCallback, failureCallback) {
        var deferred = new $.Deferred();

        $.ajax({
            url: uri,
            type: "GET",
            dataType: "json",
            contentType: "application/json",
            crossDomain: true,
            data: "format=json",
            headers: {          
                Accept: "application/json",
                "Access-Control-Allow-Origin":"*"
            },
            beforeSend: function(jqXHR) {
                if(core.collectiveRoutingRequired() ) {
                    core.applyRestRoutingHeaders(jqXHR);
                }
            },
            success: function(data) {
                var result = successCallback(data);
                deferred.resolve(result);
            },
            error: function(xhr, status, error) {
                var result = failureCallback(xhr);
                console.warn("status="+ status + ", error=" + error);
                deferred.resolve(result);
            }
        });

        return deferred; 
    };


    var extractNodeNameFromValidationUri = function(validationUri) {
        return validationUri.replace(VALIDATION_URI_PREFIX, "").replace(VALIDATION_URI_SUFFIX, "").replace("/", "");
    };


    var appendParametersToNodeMetaData = function(nodeName, nodeMetadata, metaDataObject) {
        var parametersObject = {};
        var isValidationEnabledForNode = false;
        if(nodeMetadata && nodeMetadata.get && nodeMetadata.get.parameters && nodeMetadata.get.parameters.length) {
            var parameters = nodeMetadata.get.parameters;
            for(var i=0; i<parameters.length; i++) {
                if(parameters[i] && parameters[i][REFERENCE_PROPERTY_KEY]) {
                    var parameterName = extractParameterNameFromUri(parameters[i][REFERENCE_PROPERTY_KEY]);
                    if(parameterName && !parametersObject[parameterName]) {
                        parametersObject[parameterName] = true;
                        isValidationEnabledForNode = true;
                    }
                }
            }
        }

        if(isValidationEnabledForNode) {
            metaDataObject[nodeName].parameters = parametersObject;
            metaDataObject[nodeName].isEnabled = true;
        }

        return metaDataObject;
    };


    // Retrive the parameter name from the uri string
    var extractParameterNameFromUri = function(uri) {
        return uri.replace(PARAMETER_URI_PREFIX, "").replace("/", "");
    };


    // Check whether the validation API is enabled.
    var isValidationEnabled = function() {
        return validationMetaDataObject.isEnabled && validationMetaDataObject.metaData;
    };


    // Check whether the node is supported by validation API.
    var isSupportedNode = function(nodeName) {
        if(nodeName && isValidationEnabled() && isPropertyAvailable(validationMetaDataObject.metaData, nodeName) && validationMetaDataObject.metaData[nodeName].isEnabled) {
            return true;
        }

        return false;
    };

    var isPropertyAvailable = function (object, key) {
        return object[key] ? true: false;
    };

    // Set validation metadata properties for the model window to use
    var getMetaDataStringForTestButton = function (nodeName) {
        var validationMetaDataParameters = "";
        var auth = false;
        var authAlias = false;
        var loginConfig = false;
        var loginConfigParameters = false;
        var applicationAuthentication = false;

        if(isSupportedNode(nodeName)) {
            var nodeMetaData = validationMetaDataObject.metaData[nodeName];
            var parametersObj = nodeMetaData.parameters;

            if(parametersObj) {
                auth = isPropertyAvailable(parametersObj, AUTH);
                authAlias = isPropertyAvailable(parametersObj, AUTH_ALIAS);
                loginConfig = isPropertyAvailable(parametersObj, LOGIN_CONFIG);
                loginConfigParameters = isPropertyAvailable(parametersObj, LOGIN_CONFIG_PARAMETERS);
                applicationAuthentication = isPropertyAvailable(parametersObj, APPLICATION_AUTHENTICATION.USER) && isPropertyAvailable(parametersObj, APPLICATION_AUTHENTICATION.PASSWORD);
            }
        }
        
        validationMetaDataParameters += " data-nodeName='" + nodeName + "'";
        validationMetaDataParameters += " data-auth='" + auth + "'";
        validationMetaDataParameters += " data-authalias='" + authAlias + "'";
        validationMetaDataParameters += " data-loginconfig='" + loginConfig + "'";
        validationMetaDataParameters += " data-loginconfigparameters='" + loginConfigParameters + "'";
        validationMetaDataParameters += " data-applicationauthentication='" + applicationAuthentication + "'";
        validationMetaDataParameters += " ";

        return validationMetaDataParameters;
    };

    // Append nodeID and nodeName to the element
    var getMetadataStringForTreeNode = function(nodeName, elementId) {
        var elementMetadataString = "";
        
        if(nodeName && isSupportedNode(nodeName)) {
            if(elementId === null) {
                elementId = "null";
            }

            elementMetadataString +=  " data-elementid=\'" + elementId + "\' ";
            elementMetadataString +=  " data-elementnodename=\'" + nodeName + "\' ";
        }
        return elementMetadataString;
    };


    // enable TestConnectionButton if available in the current form
    var enableTestConnectionButton = function() {
        $(".unsavedNodeTestConnectionButton").removeClass("unsavedNodeTestConnectionButton");
    };


    // disable TestConnectionButton if available in the current form
    var disableTestConnectionButton = function() {
        var testButton = $("#testButton");
        if(testButton && testButton.length) {
            $(testButton).addClass("unsavedNodeTestConnectionButton");
        }
    };


    // enable/disable test connection button based on the current node state.
    var updateTestConnectionButtonStatus = function() {
        if($(".editorTreeNode.active").hasClass("unsavedElement")) {
            disableTestConnectionButton();
        } else {
            enableTestConnectionButton();
        }
    };


    return {
        retrieveSchema: retrieveSchema,
        isSupportedNode: isSupportedNode,
        getMetaDataStringForTestButton: getMetaDataStringForTestButton,
        getMetadataStringForTreeNode: getMetadataStringForTreeNode,
        enableTestConnectionButton: enableTestConnectionButton,
        disableTestConnectionButton: disableTestConnectionButton,
        updateTestConnectionButtonStatus: updateTestConnectionButtonStatus
    };

})();