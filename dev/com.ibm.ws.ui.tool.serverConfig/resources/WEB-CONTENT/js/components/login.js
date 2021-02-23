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

var login = (function() {
    "use strict";

    // Authentication credentials
    var user = "user";
    var password = "password";

    var authenticate = function() {

        // Check connection to server
        checkServer().done(function() {

            // Handle bidi
            globalization.retrieveBidiPreference().always(function() {

                core.startup();

            });

        }).fail(function() {

            // Hide progress
            core.hideControlById("progress", false, true);

            // Show login controls
            displayLoginForm();

        });
    };


    var checkServer = function() {
        return $.ajax({
            url: "/IBMJMXConnectorREST",
            async: false,
            username: user,
            password: password
        });
    };


    var displayLoginForm = function() {

        // Update title
        core.setNavbarTitleText(editorMessages.SIGN_IN);

        // Enable login controls
        $("#loginForm > *").removeAttr("disabled");

        // Clear password from control
        $("#loginFormPassword").val("");

        // Show login
        core.showControlById("login");

        // Update login focus
        updateLoginFocus();
    };


    var updateLoginFocus = function() {
        // Update value so it's focused at the end of the text
        var loginFormUsername = $("#loginFormUsername");
        var value = loginFormUsername.val();
        loginFormUsername.focus();
        loginFormUsername.get(0).setSelectionRange(value.length, value.length);
    };


    var signOut = function() {
        return $.ajax({
            url: "ibm_security_logout",
            type: "POST",
            username: "_",
            password: "_",
            async: false,
            data: "logoutExitPage=index.jsp",
            contentType: "application/x-www-form-urlencoded"
        });
    };


    $(document).ready(function() {

        // Include authentication information on all requests
        $.ajaxPrefilter(function(options, originalOptions, jqXHR) {
            options.user = user;
            options.password = password;
        });


        // Handle login form
        $("#loginForm").on("submit", function(event) {
            event.preventDefault();

            // Hide error messages
            core.hideControlById("loginErrorMessagesMissingUserName", false);
            core.hideControlById("loginErrorMessagesLoginFailed", false);

            // Obtain user name
            var userName = $("#loginFormUsername").val();
            if(userName.length > 0) {

                // Show progress
                core.showControlById("progress", true);

                // Update credentials
                user = userName;
                password = $("#loginFormPassword").val();

                // Disable controls while loading
                $("#loginForm > *").attr("disabled", "disabled");

                // Check connection to server
                checkServer().done(function() {

                    // Hide login
                    core.hideControlById("login", false);

                    // Handle bidi
                    globalization.retrieveBidiPreference().always(function() {

                        // Start
                        core.startup();

                    });

                }).fail(function() {

                    // Hide progress
                    core.hideControlById("progress", false, true);

                    // Indicate failed login error
                    core.showControlById("loginErrorMessagesLoginFailed", true);
                    $("#loginForm > *").removeAttr("disabled");
                });

            } else {
                // Indicate missing user name error
                core.showControlById("loginErrorMessagesMissingUserName", true);
            }
        });


        $("#navbarSignOutButton").on("click", function(event) {
            event.preventDefault();
            core.hideControlById("navbarSignOutSection");
            core.showControlById("navbarSignOutPromptSection", true);
        });


        $("#navbarSignOutNoButton").on("click", function(event) {
            event.preventDefault();
            core.hideControlById("navbarSignOutPromptSection", false);
            core.showControlById("navbarSignOutSection", true);
        });


        $("#navbarSignOutYesButton").on("click", function(event) {
              event.preventDefault();

              core.showControlById("progress", true);

              signOut().done(function() {

                  // Reset credentials
                  user = "user";
                  password = "password";

                  // Hide progress
                  core.hideControlById("progress", false, true);

                  // Hide sign out prompt
                  core.hideControlById("navbarSignOutPromptSection");

                  // Hide server explorer
                  core.hideControlById("serverExplorer");

                  // Hide file explorer
                  core.hideControlById("fileExplorer");

                  // Show login controls
                  displayLoginForm();

              }).fail(function() {

                  // Hide progress
                  core.hideControlById("progress");

                  // Show error message
                  core.renderMessage(editorMessages.SIGN_OUT_ERROR, "danger", true, true);

              });
        });


        $("#loginSubmit").on("mousedown", function(event) {
            if("WebkitAppearance" in document.documentElement.style) {
                $(this).css("outline", "none");
            }
        });

        $("#loginSubmit").on("keydown", function(event) {
            if("WebkitAppearance" in document.documentElement.style) {
                $(this).css("outline", "");
            }
        });

    });

    return {
        authenticate: authenticate
    };

})();
