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

 var validateConnection = (function() {
    "use strict";

    var VALIDATION_API_URI = "/ibm/api/validation/";
    var CONFIG_API_URI = "/ibm/api/config/";
    var AUTHDATA_ENDPOINT_URI = CONFIG_API_URI + "authData/";
    var JAAS_LOGIN_CONTEXT_ENTRY_ENDPOINT_URI = CONFIG_API_URI + "jaasLoginContextEntry/";
    var CONTAINER_AUTH_ENDPOINT_URI = CONFIG_API_URI + "containerAuthData/";
    var CONNECTION_MANAGER_ENDPOINT_URI = CONFIG_API_URI + "connectionManager/";

    // SHow appropriate tab based on user input.
    var changeTab = function(tabBodyID, tabButtonID) {
        $(".validationTabBody").addClass("hidden");
        $("#" + tabBodyID).removeClass("hidden");

        $(".validationTabs").removeClass("active").addClass("disabled");
        $("#" + tabButtonID).removeClass("disabled").addClass("active");
    };

    // Click event handler for application authentication tab title
    var useApplicationValidationButtonHandler = function(event) {
        // Application authentication mode tab
        if(event){
            event.preventDefault();
        }
        clearAllResponseFields();
        applicationAuthenicationFormValidation();
        changeTab("testUserPassParameters", "useApplicationValidationButton");
    };

    // Click event handler for application authentication tab title
    $("#useApplicationValidationButton").on("click", useApplicationValidationButtonHandler);

    // Click event handler for container authentication tab title
    var useContainerValidationButtonHandler = function(event) {
        // Container authentication mode tab
        if(event){
            event.preventDefault();
        }
        clearAllResponseFields();
        configureContainerAuthSubSections();
        changeTab("testAuthAliasParameter", "useContainerValidationButton");
    };

    // Click event handler for container authentication tab title
    $("#useContainerValidationButton").on("click", useContainerValidationButtonHandler);

    var useNoResourceReferenceButtonHandler = function(event) {
        if(event){
            event.preventDefault();
        }
        clearAllResponseFields();
        noResourceReferenceFormValidation();
        changeTab("testNoResourceReference", "useNoResourceReferenceButton");
    };

    // Click event handler for container authentication tab title
    $("#useNoResourceReferenceButton").on("click", useNoResourceReferenceButtonHandler);


    // Validate application authentication form input
    var applicationAuthenicationFormValidation = function() {
        // Make sure that both username and password are provided at the same time.
        // Do not allow the test button to work until the user fixes the input values.
        var allInputFieldsEmpty =
            $("#testPasswordInput").val() === "" && $("#testUsernameInput").val() === "";
        var allInputFieldsHasTexts =
            $("#testPasswordInput").val() !== "" && $("#testUsernameInput").val() !== "";
        var isTestButtonEnabled = false;
        if(allInputFieldsEmpty || allInputFieldsHasTexts) {
            $("#testUserPassParameters").removeClass("has-error");
            isTestButtonEnabled = true;
        } else {
            $("#testUserPassParameters").addClass("has-error");
            isTestButtonEnabled = false;
        }
        updateTestConnectionButtonState(isTestButtonEnabled);
    };


    // Validate no resource reference form input
    var noResourceReferenceFormValidation = function() {
        // Make sure that both username and password are provided at the same time.
        // Do not allow the test button to work until the user fixes the input values.
        var allInputFieldsEmpty =
            $("#testPasswordInputNoResourceReference").val() === "" && $("#testUsernameInputNoResourceReference").val() === "";
        var allInputFieldsHasTexts =
            $("#testPasswordInputNoResourceReference").val() !== "" && $("#testUsernameInputNoResourceReference").val() !== "";
        var isTestButtonEnabled = false;
        if(allInputFieldsEmpty || allInputFieldsHasTexts) {
            $("#testAuthAliasParameter").removeClass("has-error");
            isTestButtonEnabled = true;
        } else {
            $("#testAuthAliasParameter").addClass("has-error");
            isTestButtonEnabled = false;
        }
        updateTestConnectionButtonState(isTestButtonEnabled);
    };

    
    // Event handler for application authentication form text fields
    $("#testUsernameInput, #testPasswordInput").on("input", applicationAuthenicationFormValidation);


    // Event handler for no resource reference form text fields
    $("#testUsernameInputNoResourceReference, #testPasswordInputNoResourceReference").on("input", noResourceReferenceFormValidation);


    // Click event handler for test connection button
    $("#dialogTestConnectionButton").on("click", function(event) {
        event.preventDefault();
        // Make sure the previous results are removed so that end users
        // do not mistaken the previous results as the current results
        clearAllResponseFields();

        var form = {};
        form.auth = getAuthenticationMethod();
        form.containerAuthType = getContainerAuthType();
        form.authAlias = getAuthenticationAlias();
        form.loginConfigId = getLoginConfigId();
        form.username = getUsername();
        form.password = getPassword();
        form.usernameNoResourceRef = getUsernameNoResourceRef();
        form.passwordNoResourceRef = getPasswordNoResourceRef();
        form.nodeName = $("#dialogTestConnectionButton")[0].dataset.nodeName;
        form.nodeIdString = getNodeIdString(form.nodeName, "testConnection");

        if (isTestConnectionButtonDisabled()) {
            // Disable click on the test button if there are parameter input errors
            return false;
        }

        testConnectionWithGet(form)
            .done(function(response) {
                testConnectionResponseHandler(response, form.nodeIdString);
            });
    });

    // Parse the response and proceed success/failure handler.
    var testConnectionResponseHandler = function(response, nodeIdString) {
        if(nodeIdString === "" && response.length) {
            // Case: When nodeId parameter value is empty string.
            // Navigate through the rosponse array to extract the relevent data object.
            for(var i=0; i<response.length; i++) {
                if(response[i] && response[i].id ==="") {
                    response = response[i];
                    break;
                }
            }
        }

        $("#dialogTestConnectionDescription").val(JSON.stringify(response, null, 4));
        core.showControlById("dialogTestConnectionJSONContainer");
        if(response && response.successful) {
            showSuccess();
        } else {
            showFailed(editorMessages.FAILED);
        }
    };


    $("#dialogValidateConnectionElement").on("hide.bs.modal", function() {
        // Clear all the input fields when the bootstrap modal is hidden.
        clearAllParameterFields();
        clearAllResponseFields();
    });


    // Look for any optional parameters when testing with Container authentication method
    var getContainerMethodParameters = function() {
        var queryAuthMethodParameters = "";
        // Look for authentication alias, custom login module, and login properties
        var authAliasInput = $("#authAliasInput").val();
        var customLoginModuleInput = $("#customLoginModuleInput").val();
        var loginPropertiesInput = $("#loginPropertiesInput").val();

        if(authAliasInput) {
            queryAuthMethodParameters += "authAlias=" + authAliasInput;
        }
        if (customLoginModuleInput) {
            queryAuthMethodParameters += "customLogin=" + authAliasInput;
        }
        if(loginPropertiesInput) {
            queryAuthMethodParameters += "loginProperties=" + authAliasInput;
        }
        return queryAuthMethodParameters;
    };

    // Returns "container" or "application" or ""
    var getAuthenticationMethod = function() {
        var container = $("#useContainerValidationButton").hasClass("active");
        var application = $("#useApplicationValidationButton").hasClass("active");
        var noReference = $("#useNoResourceReferenceButton").hasClass("active");
        if(container) {
            return "container";
        } else if(application) {
            return "application";
        } else if(noReference) {
            return "";
        } else {
            return ""; // This should never happen.
        }
    };

    
    // Read username input.
    var getUsername = function() {
        var testUsernameInput = $("#testUsernameInput").val();
        return testUsernameInput;
    };


    // Read password input.
    var getPassword = function() {
        var testPasswordInput = $("#testPasswordInput").val();
        return testPasswordInput;
    };


    // Read username input.
    var getUsernameNoResourceRef = function() {
        var testUsernameInput = $("#testUsernameInputNoResourceReference").val();
        return testUsernameInput;
    };


    // Read password input.
    var getPasswordNoResourceRef = function() {
        var testPasswordInput = $("#testPasswordInputNoResourceReference").val();
        return testPasswordInput;
    };


    // Read containerAuthType input.
    var getContainerAuthType = function() {
        var checkedElement = $('input[name="containerAuthenticationRadioElement"]:checked');
        var containerAuthType = (checkedElement && checkedElement.length && checkedElement[0].id)? checkedElement[0].id : null;
        containerAuthType = containerAuthType? containerAuthType.split("containerAuthRadio")[1]:null;
        return containerAuthType;
    };


    // Fetch value from carbon dropdown list
    var readValueFromCarbonSelect = function(selector) {
        var value = null;
        if($(selector + ' option:selected') && $(selector + ' option:selected').val){
            value = $(selector + ' option:selected').val();
        }
        return value === editorMessages.CHOOSE_AN_OPTION? null : value;
    };


    // Read authenticationAlias input.
    var getAuthenticationAlias = function() {
        var authenticationAlias = readValueFromCarbonSelect("#authAliasInput");
        return authenticationAlias;
    };


    // Read loginConfigId input.
    var getLoginConfigId = function() {
        var loginConfigId = readValueFromCarbonSelect("#loginConfigId");
        return loginConfigId;
    };


    // Get the number of occurance of current node (without id) among simillar nodes
    var getDefaultNodeID = function(nodeName) {
        var activeTreeNodeElement = $("#editorTree .active");
        var treeNodeArray = $("[data-elementnodename='" + nodeName + "']");
        var id = 0;
        for(var i=0; i<treeNodeArray.length; i++) {
            var current = treeNodeArray[i];
            if($(current).data('elementid') === null) {
                if($(current).is(activeTreeNodeElement)) {
                    break;
                }
                id++;
            }
        }
        return id;
    };

    // Get the node identifier string
    var getNodeIdString = function(nodeName, context) {
        var nodeIdString = $("#attribute_id").val();
        // attribute_id does not always exists
        var placeholder;
        if (nodeIdString) {
            placeholder = $("#attribute_id")[0].placeholder;
        }
        if (!nodeIdString && nodeName) {
            if (placeholder === editorMessages.EMPTY_STRING_ATTRIBUTE_VALUE) { // Case: Id parameter value is left as empty string
                nodeIdString = "";
            } else { // Case: Top-level dataSource without id
                // id is computed based on the order of appearance within server config, starting at 0. like <nodeName>[default-0]
                var id = getDefaultNodeID(nodeName);
                if (context === "testConnection") {
                    nodeIdString = nodeName + "[default-" + id + "]";
                } else {
                    nodeIdString = "default-" + id;
                }
            }
        } else if (nodeIdString && nodeName && nodeIdString.endsWith(".needed-index")) {
            // handle special case when we don't have id for child elment
            // nested case or singleton
            nodeIdString = nodeIdString.substring(0, nodeIdString.indexOf('.needed-index'));
            if (nodeIdString.startsWith("transaction") || !nodeIdString.startsWith(nodeName)) {
                var nodeId = getDefaultNodeID(nodeName);
                if (context === "testConnection") {
                    nodeIdString = nodeIdString + "[default-" + nodeId + "]";
                } else { //fetchConfig
                    nodeIdString = "default-" + nodeId;
                }
            }
        }

        return nodeIdString;
    };


    // Validate configuration using validate endpoint.
    var testConnectionWithGet = function(form) {
        var queryParameters = "";
        var loginConfParameters = null;
        if(form.auth) {
            queryParameters += "auth=" + form.auth;
        }
        if(form.auth === "container") {
            queryParameters = appendQueryParametersForContainerAuthentication(queryParameters, form.authAlias, form.loginConfigId, form.containerAuthType);
            if(form.containerAuthType === "LoginConfig") {
                loginConfParameters = getLoginConfParametersArray();
            }
        }
        if(queryParameters.length > 0) {
            // Add the question mark when there are query parameters
            queryParameters = "?" + queryParameters;
        }

        var deferred = new $.Deferred();
        $.ajax({
            url: VALIDATION_API_URI + form.nodeName + "/" + form.nodeIdString + queryParameters,
            type: "get",
            dataType: "json",
            crossDomain: true,
            headers: {
                "Access-Control-Allow-Origin":"*"
            },
            beforeSend: function(jqXHR) {
                if(core.collectiveRoutingRequired() ) {
                    core.applyRestRoutingHeaders(jqXHR);
                }
                if(loginConfParameters) {
                    jqXHR.setRequestHeader("X-Login-Config-Props", loginConfParameters.toString());
                }
                if(form.auth==="application" && form.username) {
                    jqXHR.setRequestHeader("X-Validation-User", form.username);
                    jqXHR.setRequestHeader("X-Validation-Password", form.password);
                } else if(form.auth ==="" && form.usernameNoResourceRef) {
                    jqXHR.setRequestHeader("X-Validation-User", form.usernameNoResourceRef);
                    jqXHR.setRequestHeader("X-Validation-Password", form.passwordNoResourceRef);
                }
            },
            success: function(data) {
                core.hideControlById("testConnectionResponseFailed");
                deferred.resolve(data);
            },
            error: function(xhr) {
                // If you change this, change testConnectionWithPost also
                var msg = stringUtils.formatString(editorMessages.FAILED_HTTP_CODE, [xhr.status]);
                showFailed(msg);
                core.hideControlById("dialogTestConnectionJSONContainer");
                deferred.reject(msg);
            }
        });
        return deferred;
    }; // end of testConnectionWithGet


    var clearAllParameterFields = function() {
        var allInputFields = $("#dialogValidateConnectionElement").find("input");
        allInputFields.val("");
    };


    var clearAllResponseFields = function() {
        core.hideControlById("testEndResult");
        core.hideControlById("dialogTestConnectionJSONContainer");
        $("#dialogTestConnectionDescription").val("");
    };


    var showSuccess = function() {
        core.showControlById("testEndResult");
        core.showControlById("testConnectionResponseSuccess");
        core.hideControlById("testConnectionResponseFailed");
    };


    var showFailed = function(failedMessage) {
        core.showControlById("testEndResult");
        core.hideControlById("testConnectionResponseSuccess");
        core.showControlById("testConnectionResponseFailed");
        $("#testConnectionResponseFailed").text(failedMessage);
    };


    // Append new parameter to the query string.
    var appendValueToQueryParameter = function(queryParameters, key, value) {
        if(key && value) {
            if(queryParameters) {
                queryParameters += "&";
            }
            queryParameters += key + "=" + value;
        }

        return queryParameters;
    };


    // Construct the payload for container authentication tab.
    var appendQueryParametersForContainerAuthentication = function(queryParameters, authAlias, loginConfigId, containerAuthType) {
        switch (containerAuthType) {
            case "DefaultAuthentication":
            break;

            case "SpecifyAuthentication":
            if(authAlias) {
                queryParameters = appendValueToQueryParameter(queryParameters, "authAlias", authAlias);
            }
            break;

            case "LoginConfig":
            if(loginConfigId) {
                queryParameters = appendValueToQueryParameter(queryParameters, "loginConfig", loginConfigId);
            }
            break;
        }
        return queryParameters;
    };


    // Enable/disable input fields under each section based on the radio selection.
    var configureContainerAuthSubSections = function() {
        clearAllResponseFields(); // Clear responses while switching options.
        var containerAuthType = getContainerAuthType();
        var enableTestButton = false;
        switch (containerAuthType) {
            case "DefaultAuthentication":
                disableSpecifyAuthenticationFields();
                disableLoginConfigFields();
                enableTestButton = true;
            break;

            case "SpecifyAuthentication":
                enableSpecifyAuthentication();
                disableLoginConfigFields();
                enableTestButton = isSpecifyAuthenticationConditionsMet();
            break;

            case "LoginConfig":
                enableLoginConfigFields();
                disableSpecifyAuthenticationFields();
                enableTestButton = isLoginConfigConditionsMet();
            break;
        }

        updateTestConnectionButtonState(enableTestButton);
    };


    // Enable/disable input fields under each section based on the radio selection.
    $(document).on("change", ".containerAuthenticationRadioElement", configureContainerAuthSubSections);


    // Upon changing the value of any input field in the container authentication section. Update test connection button.
    $(document).on("change", "#authAliasInput", configureContainerAuthSubSections);
    $(document).on("change", "#loginConfigId", configureContainerAuthSubSections);
    $(document).on("input", ".loginConfigKey", configureContainerAuthSubSections);
    $(document).on("input", ".loginConfigValue", configureContainerAuthSubSections);


    // Return true if authAliasInput is not empty
    var isSpecifyAuthenticationConditionsMet = function() {
        var enableTestButton;
        if( $("#authAliasInput").val()) {
            enableTestButton = true;
            $("#authAliasInput").removeClass("has-error");
        } else {
            enableTestButton = false;
            $("#authAliasInput").addClass("has-error");
        }
        
        return enableTestButton;
    };


    // Return true if loginConfigId is not empty and loginConfigKey&value pairs are valid if any.
    var isLoginConfigConditionsMet = function() {
        var enableTestButton;
        if( $("#loginConfigId").val()) {
            enableTestButton = true;
            $("#loginConfigId").removeClass("has-error");
        } else {
            enableTestButton = false;
            $("#loginConfigId").addClass("has-error");
        }

        var container = $("#keyValueTableRowValueSection");
        if(container && $(container).children() && $(container).children().length)   {
            var rows = $(container).children().length;
            var keys = $(".loginConfigKey");
            var values = $(".loginConfigValue");
            for(var i=0; i<rows; i++) {
                var key = $(keys[i]).val();
                var value = $(values[i]).val();
                if((key && !value) || (!key && value)) {
                    enableTestButton = false;
                    $(keys[i]).addClass("has-error");
                    $(values[i]).addClass("has-error");
                    break;
                } else {
                    $(keys[i]).removeClass("has-error");
                    $(values[i]).removeClass("has-error");
                }
            }
        }

        return enableTestButton;
    };


    // Enable/disable test connection button based on the boolean input.
    var updateTestConnectionButtonState = function(enableTestButton) {
        if(enableTestButton){
            $("#dialogTestConnectionButton").removeAttr('disabled');
        } else {
            $("#dialogTestConnectionButton").attr('disabled','disabled');
        }
    };

    
    // Returns true if testconnection button is disabled.
    var isTestConnectionButtonDisabled = function() {
        var state = $("#dialogTestConnectionButton").attr('disabled');
        return state && state === "disabled"? true: false;
    };


    // Disable input fields under specify authenication section.
    var disableSpecifyAuthenticationFields = function() {
        $("#authAliasInput").prop("disabled", true);
    };


    // Disable input fields under login configuration section.
    var disableLoginConfigFields = function() {
        $("#loginConfigId").prop("disabled", true);
        $(".loginConfigKey").prop("disabled", true);
        $(".loginConfigValue").prop("disabled", true);
        $(".keyValueTableItemActionIcon").addClass("keyValueTableItemActionIconDisabled");
        $(".keyValueTableItemActionIcon").prop("disabled", true);
    };


    // Enable input fields under specify authenication section.
    var enableSpecifyAuthentication = function() {
        $("#authAliasInput").prop("disabled", false);
    };


    // Enable input fields under login configuration section.
    var enableLoginConfigFields = function() {
        $("#loginConfigId").prop("disabled", false);
        $(".loginConfigKey").prop("disabled", false);
        $(".loginConfigValue").prop("disabled", false);
        $(".keyValueTableItemActionIcon").removeClass("keyValueTableItemActionIconDisabled");
        $(".keyValueTableItemActionIcon").prop("disabled", false);
    };


    /** Login config Parameter Table logic - start **/
    var addNewRowToKeyValueTable = function(event){
        var container = $("#keyValueTableRowValueSection");
        var newRow = '<div class="keyValueTableRow keyValueTableRowValue">' +
                '<div class="keyValueTableItemKey">' +
                    '<label class="bx--label">' + editorMessages.KEY + '</label>' +
                    '<input type="text" class="form-control loginConfigKey bx--text-input bx--text-input--light" placeholder="' + editorMessages.NO_VALUE + '" aria-labelledby="betaLabel" title="' + editorMessages.KEY + '">' +
                '</div>' +
                '<div class="keyValueTableItemValue">' +
                    '<label class="bx--label">' + editorMessages.VALUE + '</label>' +
                    '<input type="text" class="form-control loginConfigValue bx--text-input bx--text-input--light" placeholder="' + editorMessages.NO_VALUE + '" aria-labelledby="betaLabel" title="' + editorMessages.VALUE + '">' +
                '</div>' +
                '<div class="keyValueTableItemAction">' +
                    '<button type="button" class="keyValueTableItemActionIcon keyValueTableItemActionIconRemove bx--tag">' + editorMessages.REMOVE + '</button>' +
                '</div>' +
            '</div>';
        $(container).append(newRow);
        var lastChild = $(container)[0].lastChild;
        $(lastChild).find(".loginConfigKey").focus();
    };


    // Click action for Add parameter button under login config section in container tab.
    $(".keyValueTableItemActionIconAdd").on("click", addNewRowToKeyValueTable);


    // Click action for remove parameter button under login config section in container tab.
    $(document).on("click", ".keyValueTableItemActionIconRemove", function(event) {
        $(event.target).closest(".keyValueTableRowValue").remove();
    });


    // Remove extra rows from loginConfigPropertiesTable
    var resetLoginConfigPropertiesTable = function() {
        $(".keyValueTableRowValue").remove();
        // addNewRowToKeyValueTable();
    };


    // Remove values from all input fields in the model
    var resetInputFields = function() {
        // Remove all Error notifications
        $("#dialogValidateConnectionElement .has-error").removeClass("has-error");

        // Clear all input fields
        $("#authAliasInput").val("");
        $("#loginConfigId").val("");
        $("#testUsernameInput").val("");
        $("#testPasswordInput").val("");
        $("#testUsernameInputNoResourceReference").val("");
        $("#testPasswordInputNoResourceReference").val("");

        // Clear all autosuggest lists
        resetCarbonSelect("#authAliasInput");
        resetCarbonSelect("#loginConfigId");
        $("#testUsernameInputList").empty();
        $("#testUsernameInputListNoResourceReference").empty();
    };


    // Clear all options from Carbon dropdown list
    var resetCarbonSelect = function(selector) {
        var CARBON_SELECT_DEFAULT_OPTION = "<option disabled selected>" + editorMessages.CHOOSE_AN_OPTION + "</option>";
        $(selector).empty();
        $(selector).append(CARBON_SELECT_DEFAULT_OPTION);
    };


    // Fetch all valid login parameter inputs
    var getLoginConfParametersArray  = function() {
        var loginConfArray = [];
        var container = $("#keyValueTableRowValueSection");
        if(container && $(container).children() && $(container).children().length)   {
            var rows = $(container).children().length;
            var keys = $(".loginConfigKey");
            var values = $(".loginConfigValue");
            for(var i=0; i<rows; i++) {
                var key = $(keys[i]).val();
                var value = $(values[i]).val();
                if(key && value) {
                    loginConfArray.push(key + "=" + value);
                }
            }
        }
        return loginConfArray.length>0 ? loginConfArray : null;
    };
    /** Login config Parameter Table logic - end **/


    var showLoginConfigSection = function() {
        $(".containerAuthenticationRowLoginConfigSection").show();
    };


    var hideLoginConfigSection = function() {
        $(".containerAuthenticationRowLoginConfigSection").hide();
    };


    var showSpecifyAuthSection = function(){
        $(".containerAuthenticationRowSpecifyAuthSection").show();
    };


    var hideSpecifyAuthSection = function() {
        $(".containerAuthenticationRowSpecifyAuthSection").hide();
    };


    // Fetch data from backend server.
    // Update autosuggest lists for all input fields in the model.
    var fetchDataFromServer = function(isAuthAliasEnabled, isLoginConfigEnabled, isApplicationAuthenticationEnabled) {
        var nodeName = $("#dialogTestConnectionButton")[0].dataset.nodeName;
        var nodeIdString = getNodeIdString(nodeName, "fetchConfig");
        var deferred = new $.Deferred();
        
        var updateAuthConfigDataPromise = updateAuthConfigData(isAuthAliasEnabled);
        
        $.when(updateAuthConfigDataPromise)
            .then(function(){
                if(!isLoginConfigEnabled) {
                    return;
                }
                return updateLoginConfigData(isLoginConfigEnabled);
            })
            .then(function(){
                if(!isApplicationAuthenticationEnabled){
                    return;
                }
                return updateApplicationAuthenticationData(nodeName, nodeIdString);
            })
            .then(function(){
                return updateNoResporceReferenceData(nodeName, nodeIdString, isApplicationAuthenticationEnabled);
            })
            .then(function(){
                deferred.resolve();
            });

        return deferred;
    };

    // XHR template to perform the ajax calls with promise
    var getDefferedXHR = function(uri, successCallback, failureCallback) {
        var deferred = new $.Deferred();

        $.ajax({
            url: uri,
            type: "get",
            dataType: "json",
            contentType: "application/json",
            crossDomain: true,
            headers: {          
                Accept: "application/json"   ,
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
            error: function(xhr) {
                var result = failureCallback(xhr);
                deferred.resolve(result);
            }
        });

        return deferred; 
    };

    // Append an option to carbon dropdown list.
    var appendToCarbonSelect = function(selector, value) {
        var listItem = '<option value="' + value + '" class="bx--select-option">' + value + '</option>';
        $(selector).append(listItem);
    };


    // Fetch authConfig info and update it in the UI
    var updateAuthConfigData = function(isAuthAliasEnabled) {
        if(!isAuthAliasEnabled) {
            var deferred = new $.Deferred();
            deferred.resolve();
            return deferred;
        }

        var uri = AUTHDATA_ENDPOINT_URI;
            var results = [];

            var successCallback = function(data) {
            if(data && data.length){
                $.each(data, function( index, value ) {
                    if(value.id) {
                        var current = value.id;
                        if(results.indexOf(current) === -1){
                            results.push(current);
                            appendToCarbonSelect("#authAliasInput", current);
                        }
                    }
                });

                // If only one option exists, then make it default selected.
                if(results.length === 1 && results[0]) {
                    $("#authAliasInput").val(results[0]);
                }

                if(results.length) {
                    showSpecifyAuthSection();
                }
            }
        };

        var failureCallback = function() {};

        return getDefferedXHR(uri, successCallback, failureCallback);   
    };

    // Fetch loginConfig info and update it in the UI
    var updateLoginConfigData =  function(isLoginConfigEnabled) {
        var uri = JAAS_LOGIN_CONTEXT_ENTRY_ENDPOINT_URI;

        var successCallback = function(data) {
            var results = [];
            if(data && data.length){
                $.each(data, function( index, value ) {
                    if(value.uid) {
                        var current = value.uid;
                        if(results.indexOf(current) === -1){
                            results.push(current);
                            appendToCarbonSelect("#loginConfigId", current);
                        }
                    }
                });

                // If only one option exists, then make it default selected.
                if(results.length === 1 && results[0]) {
                    $("#loginConfigId").val(results[0]);
                }

                if(isLoginConfigEnabled && results.length) {
                    showLoginConfigSection();
                }
            }
        };
        var failureCallback = function() {};

        return getDefferedXHR(uri, successCallback, failureCallback);   
    };

    
    // Fetch user name info and update it in the application authentication UI
    var updateApplicationAuthenticationData =  function(nodeName, nodeIdString) {
        var uri = CONTAINER_AUTH_ENDPOINT_URI;

        var successCallback = function(data) {

            if(data && data.length){
                for(var i=0; i<data.length; i++) {
                    if(data[i] && data[i].uid && data[i].uid.indexOf(nodeName + "[" + nodeIdString + "]/containerAuthData[")>-1 && data[i].user) {        
                        var listItem = '<option value="' + data[i].user + '">';
                        $("#testUsernameInputList").append(listItem);
                        $("#testUsernameInputListNoResourceReference").append(listItem);
                        // return;
                    }
                }
            }
        };

        var failureCallback = function() {};

        return getDefferedXHR(uri, successCallback, failureCallback);   
    };

    var updateNoResporceReferenceData = function(nodeName, nodeIdString, isApplicationAuthenticationEnabled) {

        var embeddedDataPromise = fetchEmbeddedConnectionManagerData(nodeName, nodeIdString);
        
        $.when(embeddedDataPromise)
            .then(function(isConnectionManagerAvailable){
                if(isConnectionManagerAvailable) {
                    return isConnectionManagerAvailable;
                } else {
                    return fetchNonEmbeddedConnectionManagerData(nodeName, nodeIdString);
                }
            })
            .then(function(isConnectionManagerAvailable){
                if(isConnectionManagerAvailable) {
                    $("#NoResourceReferenceNotice").show();
                    $("#NoResourceReferenceUserCredentials").hide();
                } else {
                    $("#NoResourceReferenceNotice").hide();
                    $("#NoResourceReferenceUserCredentials").show();
                    if(isApplicationAuthenticationEnabled) {
                        $("#noResourceReferenceInput").show();
                    } else {
                        $("#noResourceReferenceInput").hide();
                    }
                }
            });

            
    };


    // Does current node has any connection manager as subnode by checking from the connection manager list.
    var fetchEmbeddedConnectionManagerData = function(nodeName, nodeIdString) {
        var uri = CONNECTION_MANAGER_ENDPOINT_URI;

        var successCallback = function(data) {
            if(data && data.length){
                for(var i=0; i<data.length; i++) {
                    var current = data[i];
                    if(current &&
                        current.uid &&
                        current.uid.indexOf(nodeName + "[" + nodeIdString + "]/connectionManager[")>-1 &&
                        current.enableContainerAuthForDirectLookups === true) {
                            return true;
                    }
                }
            }
            return false;
        };

        var failureCallback = function() {};

        return getDefferedXHR(uri, successCallback, failureCallback);   
    };


    // Check whether any connection manager nodes are linked to the current node as connectionManagerRef.
    var fetchNonEmbeddedConnectionManagerData = function(nodeName, nodeIdString) {
        var uri = CONFIG_API_URI + nodeName + "/" + nodeIdString;

        var successCallback = function(data) {
            if(data && data.connectionManagerRef && data.connectionManagerRef.enableContainerAuthForDirectLookups === true) {
                return true;
            }
            return false;
        };

        var failureCallback = function() {};

        return getDefferedXHR(uri, successCallback, failureCallback);   
    };

    // Update the UI according to the node type.
    var renderTestConfigurationDialog = function(dataset) {
        var nodeName = dataset.nodename;
        var isNoResourceReferenceEnabled = dataset.auth === "true";
        var isAuthAliasEnabled = dataset.authalias === "true";
        var isLoginConfigEnabled = dataset.loginconfig === "true";
        var isLoginConfigParametersEnabled = dataset.loginconfigparameters === "true";
        var isApplicationAuthenticationEnabled = dataset.applicationauthentication === "true";
        
        if(isApplicationAuthenticationEnabled || isNoResourceReferenceEnabled) {
            $("#authModeTabs").show();
            if(isApplicationAuthenticationEnabled) {
                $('#useApplicationValidationButton').show();
            } else {
                $('#useApplicationValidationButton').hide();
            }
        } else {
            $("#authModeTabs").hide();
        }

        if(isLoginConfigParametersEnabled) {
            $("#keyValueTable").show();
        } else {
            $("#keyValueTable").hide();
        }
        
        // Update nodeName
        $("#dialogTestConnectionButton")[0].dataset.nodeName = nodeName;

        // Set default value as Default authentication
        $("#containerAuthRadioDefaultAuthentication").prop('checked', true);

        // Show only default auth, Later stages these sections will be displayed if they have valid values
        hideSpecifyAuthSection();
        hideLoginConfigSection();

        // Remove extra rows from loginConfigPropertiesTable
        resetLoginConfigPropertiesTable();

        // Remove values from all input fields in the model
        resetInputFields();

        // Enable/disable input fields under each section based on the radio selection.
        configureContainerAuthSubSections();

        // Set container auth tab as default.
        useContainerValidationButtonHandler();

        fetchDataFromServer(isAuthAliasEnabled, isLoginConfigEnabled, isApplicationAuthenticationEnabled)
            .done(function(){
                // Display dialog
                $("#dialogValidateConnectionElement").modal("show");
            });
    };

    // Handle test button
    $("#editorForm").on("click", "#testButton", function(event) {
        event.preventDefault();
        if(event.target && event.target.dataset && event.target.dataset.nodename && !$(event.target).hasClass("unsavedNodeTestConnectionButton")) {
            var dataset = event.target.dataset;
            renderTestConfigurationDialog(dataset);
        }
    });
})();