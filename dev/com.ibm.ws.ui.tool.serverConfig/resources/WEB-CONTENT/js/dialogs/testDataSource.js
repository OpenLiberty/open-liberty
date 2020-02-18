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

 $(document).ready(function() {
    "use strict";


    $("#useApplicationValidationButton").on("click", function(event) {
        // Application authentication mode tab
        event.preventDefault();
        core.showControlById("testUserPassParameters");
        core.hideControlById("testAuthAliasParameter");

        $("#useApplicationValidationButton").removeClass("disabled").addClass("active");
        $("#useContainerValidationButton").addClass("disabled").removeClass("active");
    });


    $("#useContainerValidationButton").on("click", function(event) {
        // Container authentication mode tab
        event.preventDefault();
        core.hideControlById("testUserPassParameters");
        core.showControlById("testAuthAliasParameter");

        $("#useApplicationValidationButton").addClass("disabled").removeClass("active");
        $("#useContainerValidationButton").removeClass("disabled").addClass("active");
    });


    $("#testUsernameInput, #testPasswordInput").on("input", function(event) {
        // Make sure that both username and password are provided at the same time.
        // Do not allow the test button to work until the user fixes the input values.
        var allInputFieldsEmpty =
            $("#testPasswordInput").val() === "" && $("#testUsernameInput").val() === "";
        var allInputFieldsHasTexts =
            $("#testPasswordInput").val() !== "" && $("#testUsernameInput").val() !== "";
        if(allInputFieldsEmpty || allInputFieldsHasTexts) {
            $("#testUserPassParameters").removeClass("has-error");
            $("#dialogTestDatabaseButton").removeClass("disabled");
        } else {
            $("#testUserPassParameters").addClass("has-error");
            $("#dialogTestDatabaseButton").addClass("disabled");
        }
    });


    $("#dialogTestDatabaseButton").on("click", function(event) {
        // The test button

        // Make sure the previous results are removed so that end users
        // do not mistaken the previous results as the current results
        clearAllResponseFields();

        var datasourceId = $("#attribute_id").val();
        var auth = getAuthenticationMethod();

        var authAlias = getAuthenticationAlias();
        var username = getUsername();
        var password = getPassword();

        if ($("#dialogTestDatabaseButton").hasClass("disabled")) {
            // Disable click on the test button if there are parameter input errors
            return false;
        }

        // If password and auth=application or blank, call testDatabaseWithPost.
        // Auth=container will always use GET
        if( username && (auth === "application" || ! auth)) {
            testDatabaseWithPost(datasourceId, auth, authAlias, username, password).done(function(response) {
                // JSON spacing level = 4
                $("#dialogTestDatasourceDescription").val(JSON.stringify(response, null, 4));
                core.showControlById("dialogTestDatasourceJSONContainer");
                if(response && response.successful) {
                    showSuccess();
                } else {
                    showFailed(editorMessages.FAILED);
                }
            });
        } else {
            testDatabaseWithGet(datasourceId, auth, authAlias).done(function(response) {
                // spacing level = 4
                $("#dialogTestDatasourceDescription").val(JSON.stringify(response, null, 4));
                core.showControlById("dialogTestDatasourceJSONContainer");
                if(response && response.successful) {
                    showSuccess();
                } else {
                    showFailed(editorMessages.FAILED);
                }
            });
        }
    });


    $("#dialogDatasourceValidateElement").on("hide.bs.modal", function() {
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
        var none = $('#testNoReferenceCheckbox').prop("checked");
        if(container) {
            return "container";
        } else if(application && ! none) {
            return "application";
        } else if(none) {
            return "";
        } else {
            return ""; // This should never happen.
        }
    };


    var getUsername = function() {
        var testUsernameInput = $("#testUsernameInput").val();
        return testUsernameInput;
    };


    var getPassword = function() {
        var testPasswordInput = $("#testPasswordInput").val();
        return testPasswordInput;
    };


    var getAuthenticationAlias = function() {
        var authenticationAlias = $("#authAliasInput").val();
        return authenticationAlias;
    };


    var testDatabaseWithGet = function(id, auth, authAlias) {
        var queryParameters = "";
        if(auth) {
            queryParameters += "auth=" + auth;
        }
        if(authAlias && auth === "container") {
            if(queryParameters) {
                queryParameters += "&";
            }
            queryParameters += "authAlias=" + authAlias;
        }
        if(queryParameters.length > 0) {
            // Add the question mark when there are query parameters
            queryParameters = "?" + queryParameters;
        }
        var deferred = new $.Deferred();
        $.ajax({
            url: "/ibm/api/validator/dataSource/" + id + queryParameters,
            type: "get",
            dataType: "json",
            success: function(data) {
                deferred.resolve(data);
            },
            error: function(xhr) {
                // If you change this, change testDatabaseWithPost also
                var msg = stringUtils.formatString(editorMessages.FAILED_HTTP_CODE, [xhr.status]);
                showFailed(msg);
                core.hideControlById("dialogTestDatasourceJSONContainer");
                deferred.reject(msg);
            }
        });
        return deferred;
    }; // end of testDatabaseWithGet


    var clearAllParameterFields = function() {
        var allInputFields = $("#dialogDatasourceValidateElement").find("input");
        allInputFields.val("");
    };


    var clearAllResponseFields = function() {
        core.hideControlById("testEndResult");
        core.hideControlById("dialogTestDatasourceJSONContainer");
        $("#dialogTestDatasourceDescription").val("");
    };


    var showSuccess = function() {
        core.showControlById("testEndResult");
        core.showControlById("testDatasourceResponseSuccess");
        core.hideControlById("testDatasourceResponseFailed");
    };


    var showFailed = function(failedMessage) {
        core.showControlById("testEndResult");
        core.hideControlById("testDatasourceResponseSuccess");
        core.showControlById("testDatasourceResponseFailed");
        $("#testDatasourceResponseFailed").text(failedMessage);
    };


    // Should only POST when username/password is supplied
    var testDatabaseWithPost = function(id, auth, authAlias, username, password) {
        var queryParameters = "";
        if(auth) {
            queryParameters += "auth=" + auth;
        }
        if(authAlias && auth === "container") {
            if(queryParameters) {
                queryParameters += "&";
            }
            queryParameters += "authAlias=" + authAlias;
        }
        if(queryParameters.length > 0) {
            // Add the question mark when there are query parameters
            queryParameters = "?" + queryParameters;
        }

        var deferred = new $.Deferred();
        $.ajax({
            url: "/ibm/api/validator/dataSource/" + id + queryParameters,
            type: "post",
            dataType: "json",
            beforeSend: function(jqXHR) {
                if(core.collectiveRoutingRequired() ) {
                    core.applyRestRoutingHeaders(jqXHR);
                }
                if(username) {
                    jqXHR.setRequestHeader("X-Validator-User", username);
                    jqXHR.setRequestHeader("X-Validator-Password", password);
                }
            },
            success: function(data) {
                deferred.resolve(data);
                core.hideControlById("testDatasourceResponseFailed");
            },
            error: function(xhr) {
                // If you change this, change testDatabaseWithGet also
                var msg = stringUtils.formatString(editorMessages.FAILED_HTTP_CODE, [xhr.status]);
                showFailed(msg);
                core.hideControlById("dialogTestDatasourceJSONContainer");
                deferred.reject(msg);
            }
        });
        return deferred;
    }; // end of testDatabaseWithPost

});
