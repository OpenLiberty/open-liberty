/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
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

    var oauthProvider;

    var __initOauthProvider = function() {
        if (!oauthProvider) {
            var pathname = window.location.pathname;
            var urlToMatch = ".*/oidc/endpoint/([\\s\\S]*)/usersTokenManagement";
            var regExpToMatch = new RegExp(urlToMatch, "g");
            var groups = regExpToMatch.exec(pathname);
            oauthProvider = groups[1];
        }
    };

    var getAccountAppPasswords = function(userID) {
        __initOauthProvider();
        var deferred = new $.Deferred();

        $.ajax({
            url: "/oidc/endpoint/" + oauthProvider + "/app-passwords",
            dataType: "json",
            data: {user_id: encodeURIComponent(userID)},
            headers: {
                "Authorization": window.globalAuthHeader,   // Basic clientID:clientSecret
                "access_token" : window.globalAccessToken   // The OAuth access_token acquired in OIDC login,
                                                            // which identifies an authenticated user.
            },
            success: function(response) {
                deferred.resolve(response);
            },
            error: function(jqXHR) {
                // Ajax request failed.
                console.log('Error on GET for app-passwords: ' + jqXHR.responseText);        
                deferred.reject(jqXHR);
            }
        });
        return deferred;
    };

    var getAccountAppTokens = function(userID) {
        __initOauthProvider();
         var deferred = new $.Deferred();

        $.ajax({
            url: "/oidc/endpoint/" + oauthProvider + "/app-tokens",
            dataType: "json",
            data: {user_id: encodeURIComponent(userID)},
            headers: {
                "Authorization": window.globalAuthHeader,   // Basic clientID:clientSecret
                "access_token" : window.globalAccessToken   // The OAuth access_token acquired in OIDC login,
                                                            // which identifies an authenticated user.
            },
            success: function(response) {
                deferred.resolve(response);
            },
            error: function(jqXHR) {
                // Ajax request failed.
                console.log('Error on GET for app-tokens: ' + jqXHR.responseText);        
                deferred.reject(jqXHR);
            }
        });
        return deferred;
    };

    var deleteAcctAppPasswordToken = function(authID, authType, userID) {
        __initOauthProvider(); 
        var deferred = new $.Deferred();
        var authTypes = authType + 's';

        $.ajax({
            url: "/oidc/endpoint/" + oauthProvider + "/" + authTypes + "/" + authID + "?user_id=" + encodeURIComponent(userID),
            type: "DELETE",
            contentType: "application/x-www-form-urlencoded",
            headers: {
                "Authorization": window.globalAuthHeader,   // Basic clientID:clientSecret
                "access_token" : window.globalAccessToken   // The OAuth access_token acquired in OIDC login,
                                                            // which identifies an authenticated user.
            },
            success: function(response) {
                deferred.resolve(response);
            },                                 
            error: function(jqXHR) {
                deferred.reject(jqXHR);
            }
        });
        return deferred;
    };

    var deleteAllAppPasswordsTokens = function(userID, authType) {
        __initOauthProvider();
        var deferred = new $.Deferred();
        var authTypes = authType + 's';

        $.ajax({
            url: "/oidc/endpoint/" + oauthProvider + "/" + authTypes + "?user_id=" + encodeURIComponent(userID),
            type: "DELETE",
            accept: "application/json",
            headers: {
                "Authorization": window.globalAuthHeader,   // Basic clientID:clientSecret
                "access_token" : window.globalAccessToken   // The OAuth access_token acquired in OIDC login,
                                                            // which identifies an authenticated user.
            },
            success: function(response) {
                deferred.resolve(response);
            },                                 
            error: function(jqXHR) {
                deferred.reject(jqXHR);
            }
        });
        return deferred;
    };

    var deleteSelectedAppPasswordsTokens = function(authID, authType, name, userID) {
        __initOauthProvider();

        var deferred = new $.Deferred();
        var authTypes = authType + 's';

        $.ajax({
            url: "/oidc/endpoint/" + oauthProvider + "/" + authTypes + "/" + authID + "?user_id=" + encodeURIComponent(userID),
            type: "DELETE",
            contentType: "application/x-www-form-urlencoded",
            headers: {
                "Authorization": window.globalAuthHeader,   // Basic clientID:clientSecret
                "access_token" : window.globalAccessToken   // The OAuth access_token acquired in OIDC login,
                                                            // which identifies an authenticated user.
            },
            success: function(response) {
                table.deleteTableRow(authID);
                deferred.resolve();
            },                                 
            error: function(jqXHR) {
                // Record the authentication that had the error and return it for processing
                var response = {status: "failure",
                                authType: authType,
                                authID: authID,
                                name: name
                                };
                deferred.resolve(response);
            }
        });
        return deferred;
    }; 

    return {
        getAccountAppPasswords: getAccountAppPasswords,
        getAccountAppTokens: getAccountAppTokens,
        deleteAcctAppPasswordToken: deleteAcctAppPasswordToken,
        deleteAllAppPasswordsTokens: deleteAllAppPasswordsTokens,
        deleteSelectedAppPasswordsTokens: deleteSelectedAppPasswordsTokens
    };

})();
