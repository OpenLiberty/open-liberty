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
            var urlToMatch = ".*/oidc/endpoint/([\\s\\S]*)/personalTokenManagement";
            var regExpToMatch = new RegExp(urlToMatch, "g");
            var groups = regExpToMatch.exec(pathname);
            oauthProvider = groups[1];
        }
    };

    var __getAcctAppPasswords = function() {
        __initOauthProvider();
        var deferred = new $.Deferred();

        $.ajax({
            url: "/oidc/endpoint/" + oauthProvider + "/app-passwords",
            dataType: "json",
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

    var __getAcctAppTokens = function() {
        __initOauthProvider();
         var deferred = new $.Deferred();

        $.ajax({
            url: "/oidc/endpoint/" + oauthProvider + "/app-tokens",
            dataType: "json",
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

    var __addAcctAppPasswordToken = function(app_name, authType) {
        __initOauthProvider();
         var deferred = new $.Deferred();
        var authTypes = authType + 's';

        $.ajax({
            url: "/oidc/endpoint/" + oauthProvider + "/" + authTypes,
            type: "POST",
            contentType: "application/x-www-form-urlencoded",
            accepts: { json: "application/json" },
            headers: {
                "Authorization": window.globalAuthHeader,   // Basic clientID:clientSecret
                "access_token" : window.globalAccessToken   // The OAuth access_token acquired in OIDC login,
                                                            // which identifies an authenticated user.
            },
            data: "app_name=" + encodeURIComponent(app_name),    // User friendly name to identify a client application that uses this app-token/password
            success: function(response) {
                // appp-password: {app_id: "tGzv3JOkF0XG5Qx2TlKWIA“,
                //                 app_password:"2YotnFZFEjr1zCsicMWpAA",
                //                 expires_at:167983600,
                //                 created_at:167783600}
                //     app-token: {token_id: "tGzv3JOkF0XG5Qx2TlKWIA",
                //                 app_token: "2YotnFZFEjr1zCsicMWpAA",
                //                 created_at: 1557195997892,
                //                 expires_at: 1564971997892}
                deferred.resolve(response);
            },
            error: function(jqXHR) {
                deferred.reject(jqXHR);
            }
        });

        return deferred;
    };

    var __deleteAcctAppPasswordToken = function(authID, authType) {
        __initOauthProvider();        
        var deferred = new $.Deferred();
        var authTypes = authType + 's';

        $.ajax({
            url: "/oidc/endpoint/" + oauthProvider + "/" + authTypes + "/" + authID,
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

    return {
        getAcctAppPasswords: __getAcctAppPasswords,
        getAcctAppTokens: __getAcctAppTokens,
        addAcctAppPasswordToken: __addAcctAppPasswordToken,
        deleteAcctAppPasswordToken: __deleteAcctAppPasswordToken
    };

})();
