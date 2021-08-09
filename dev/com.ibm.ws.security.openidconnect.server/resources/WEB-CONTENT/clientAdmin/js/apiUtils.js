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
            var urlToMatch = ".*/oidc/endpoint/([\\s\\S]*)/clientManagement";
            var regExpToMatch = new RegExp(urlToMatch, "g");
            var groups = regExpToMatch.exec(pathname);
            oauthProvider = groups[1];
        }
    };

    var __getInputFields = function() {
        __initOauthProvider();
         var deferred = new $.Deferred();

         $.ajax({
             url: "/oidc/endpoint/" + oauthProvider + "/clientMetatype",
             dataType: "json",
             success: function(response) {
                 deferred.resolve(response);
             },
             error: function(jqXHR) {
                 deferred.reject(jqXHR);
             }
         });

         return deferred;
    };

    var __getAllClients = function() {
        __initOauthProvider();
        var deferred = new $.Deferred();

        $.ajax({
            url: "/oidc/endpoint/" + oauthProvider + "/registration/",
            dataType: "json",
            success: function(response) {
                deferred.resolve(response);
            },
            error: function(jqXHR) {
                deferred.reject(jqXHR);
            }
        });

        return deferred;
    };

    var __getClient = function(clientId) {
        __initOauthProvider();
        var deferred = new $.Deferred();

        $.ajax({
            url: "/oidc/endpoint/" + oauthProvider + "/registration/" + clientId,
            dataType: "json",
            success: function(response) {
                deferred.resolve(response);
            },
            error: function(jqXHR) {
                deferred.reject(jqXHR);
            }
        });

        return deferred;
    };

    var __addClient = function(clientObject) {
        __initOauthProvider();
        var deferred = new $.Deferred();

        $.ajax({
            url: "/oidc/endpoint/" + oauthProvider + "/registration",
            type: "POST",
            dataType: "json",
            contentType: "application/json",
            data: JSON.stringify(clientObject),
            success: function(response) {
                deferred.resolve(response);
            },
            error: function(jqXHR) {
                deferred.reject(jqXHR);
            }
        });

        return deferred;
    };

    var __editClient = function(clientObject) {
        var deferred = new $.Deferred();

        $.ajax({
            url: "/oidc/endpoint/" + oauthProvider + "/registration/" + clientObject.client_id,
            type: "PUT",
            dataType: "json",
            contentType: "application/json",
            data: JSON.stringify(clientObject),
            success: function(response) {
                deferred.resolve(response);
            },
            error: function(jqXHR) {
                deferred.reject(jqXHR);
            }
        });

        return deferred;
    };

    var __deleteClient = function(clientId) {
        __initOauthProvider();
        var deferred = new $.Deferred();

        $.ajax({
            url: "/oidc/endpoint/" + oauthProvider + "/registration/" + clientId,
            type: "DELETE",
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
        getInputFields: __getInputFields,
        getAllClients: __getAllClients,
        getClient: __getClient,
        addClient: __addClient,
        editClient: __editClient,
        deleteClient: __deleteClient
    };

})();
