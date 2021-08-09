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

var acctMgr = (function() {
    "use strict";

    var generateNewAuthentication = function() {
        // Clean up any error indicators
        utils.cleanUpErrorFields();

        var providedName = $("#name").val().trim();
        var generateType = $("input[name='authType']:checked").val();

        apiUtils.addAcctAppPasswordToken(providedName, generateType).done(function (response) {

            // Add new entry as table row
            table.addTableRow(table.convertResponseForTable(response, generateType, utils.encodeData(providedName)));

            var authenticationValue;
            if (generateType === "app-password") {
                authenticationValue = response.app_password;
            } else {
                authenticationValue = response.app_token;
            }

            // Display returned app-password or app-token on dialog for user to copy
            var $addNewDlg = $('.tool_modal_container.ss_authenticate');

            // Disable input fields so user cannot change them.
            $addNewDlg.find('input#name').prop("readonly", true).addClass('readonly');
            if (generateType === 'app-password') {
                $addNewDlg.find("#rb_app_token").prop("disabled", true);
            } else {
                $addNewDlg.find("#rb_app_password").prop("disabled", true);
            }

            // Display the generated authentication value and enable user to copy it to clipboard
            var $authValue = $addNewDlg.find('.authValueDiv');
            var generatedLabel, copyTitle;
            // Set up more definitive labels for the copy function for accessibility.
            if (generateType === 'app-password') {
                generatedLabel = messages.GENERATED_APP_PASSWORD;
                copyTitle = messages.COPY_APP_PASSWORD;
            } else {
                generatedLabel = messages.GENERATED_APP_TOKEN;
                copyTitle = messages.COPY_APP_TOKEN;
            }
            $authValue.find("#auth_value").val(authenticationValue).attr('aria-label', generatedLabel);
            $authValue.find(".tool_modal_field_copy_button").prop("disabled", false);
            $authValue.find(".tool_modal_field_copy_button>img").prop('title', copyTitle).prop('alt', copyTitle);

            // Switch out the buttons so only 'Done' is showing.
            $addNewDlg.find('.tool_modal_cancel_button').addClass('hidden');
            $addNewDlg.find('.tool_modal_generate_button').addClass('hidden');
            $addNewDlg.find('.tool_modal_done_button').removeClass('hidden');
            // Set focus to the copy button
            $("#auth_value_copy").get(0).focus();

            // Re-sort the table to put the new client in the correct location.
            // If direction is "none" then the table has not yet been sorted, so leave new
            // entry where it is.
            var direction = tableUtils.currentSortDirection('table_name_column');
            if (direction !== "none") {
                tableUtils.sortTableWithFilterName(table.tableId, direction === "ascending");
            }
            // Leave the user on the same page
            tableUtils.switchPage(table.tableId, tableUtils.currentPage());
            utils.stopProcessingSpinner();
        }).fail(function(errResponse) {
            console.log("add authentication failed for " + providedName);
            console.log("errResponse.status: " + errResponse.status + ":   errResponse.statusText: " + errResponse.statusText);
            // Commented out below since a 400 with error "invalid_request" can be issued for other errors too
            // like when you need to login again.  Therefore, we can't accurately post this message here.
            // if (errResponse.status === 400) {
            //     if (errResponse.responseJSON.error && errResponse.responseJSON.error === "invalid_request") {
            //         // Show error in name field - only input field we can get an error on
            //         __showFieldInputError("name", generateType);
            //         return;     // Only field we know we can get an error on
            //     }
            // } 
            // Something else happended with the request.  Put up the generic error message.
            var generateTypeTitle = generateType === 'app-password' ? 'App-Password' : 'App-Token';
            var errTitle = utils.formatString(messages.GENERIC_GENERATE_FAIL, [generateTypeTitle]);
            var errDescription = "";
            if (errResponse.responseJSON && errResponse.responseJSON.error_description) {
                errDescription = errResponse.responseJSON.error_description;
            } else {
                // Display a generic error message...
                errDescription = utils.formatString(messages.GENERIC_GENERATE_FAIL_MSG, [generateType, utils.encodeData(providedName)]);
            }
            utils.showResultsDialog(true, errTitle, errDescription, true, true, false, table.reshowAddRegenDialog);
        });
    };

    var __showFieldInputError = function(fieldName, authType) {
        utils.stopProcessingSpinner();
        var $field = $('#' + fieldName);

        if ($field.length !== 0) {
            var errorMessage = "";
            if (fieldName === "name") {
                errorMessage = utils.formatString(messages["ERR_" + fieldName.toUpperCase()], [authType]);
            }
             
            // Set the field with a red outline to indicate it has an error
            $field.addClass("tool_field_error");
            // Add red exclamation to the end of the field's label
            var errExclamation = $('<img id="' + fieldName + '_show_error" src="../../WEB-CONTENT/common/images/errorExclamation.svg" class="tool_err_img" data-externalizedStringAlt="" aria-hidden=true>');
            errExclamation.insertAfter($field.siblings('label.tool_modal_body_field_label'));
            // Add red error message underneath the field
            var errMessage = $('<div id="' + fieldName + '_err_message" class="tool_err_msg">' + errorMessage + '</div>');
            $field.parent().append(errMessage);
        }
    };

    var __initAccountManager = function() {
        globalization.retrieveExternalizedStrings('accountManager').done(function () {

            var initDataRequests = [];
            initDataRequests.push(apiUtils.getAcctAppPasswords());
            initDataRequests.push(apiUtils.getAcctAppTokens());

            var app_passwords = [], app_tokens = [];

            $.when.apply($, initDataRequests).then(function() {
                // The args passed to the done callback provide the resolved
                // values for each of the deferreds passed to $.when() and they 
                // match the order the deferreds were passed to .when(). A deferred
                // resolved with no value will have an arg value of undefined.                
            
                // Turn the arguments list into an array
                var returnedRequests = Array.prototype.slice.call(arguments);
                var firstResponse = returnedRequests[0];
                if (firstResponse) {
                    app_passwords = firstResponse["app-passwords"];
                }
                var secondResponse = returnedRequests[1];
                if (secondResponse) {
                    app_tokens = secondResponse["app-tokens"];
                }

                for (var i = 0; i < app_tokens.length; i++) {
                    var tokenauthData = app_tokens[i];
                    console.log(tokenauthData);
                    table.addTableRow(table.convertResponseForTable(tokenauthData, "app-token"));
                }
                for (var j = 0; j < app_passwords.length; j++) {
                    var pwauthData = app_passwords[j];
                    console.log(pwauthData);
                    table.addTableRow(table.convertResponseForTable(pwauthData, "app-password"));
                }

                var numAuthentications = app_tokens.length + app_passwords.length;
                if (numAuthentications === 0) {
                    // Post a no data message in the table
                    tableUtils.addNoDataMessage(table.tableId);
                }

                tableUtils.updateTablePagingInfo(numAuthentications);
                utils.stopProcessingSpinner();

            }, function() {
                utils.showResultsDialog(true, messages.GENERIC_FETCH_FAIL, messages.GENERIC_FETCH_FAIL_MSG, false, false, true);

                // Reset the table contents to 'no results' message
                tableUtils.addNoDataMessage(table.tableId);                
            });   

            if (!window.globalAppPasswordsAllowed && !window.globalAppTokensAllowed) {
                // Disable the 'Add new' button since it will not work
                $('#add_new_authentication').prop('disabled', true);
            }

            var $headerRow = $('#' + table.tableId).find('tr').first();
            tableUtils.initTableKeyTraversing(table.tableId, $headerRow);
            tableUtils.initTableSorting('table_name_column');
            tableUtils.initTablePaging();

            // Add registration onClick event
            $("#add_new_authentication").click(function() {
                utils.saveFocus($(this));   // Save off button to return focus
                                            // to it when dialog is dismissed.

                // Make sure other dialogs are not showing
                $('.tool_modal_container').addClass('hidden');

                // Remove any existing errors from the dialog
                utils.cleanUpErrorFields();

                // Set correct title
                var $addNewDlg = $('.tool_modal_container.ss_authenticate');
                $addNewDlg.find('.tool_modal_title').html(messages.ADD_NEW_TITLE);

                // Display input field; hide informational fields shown on Regenerate dlg
                $addNewDlg.find('.tool_modal_body_field').removeClass('hidden');
                $addNewDlg.find('.tool_modal_body_info').addClass('hidden');

                // Enable the name input field and initialize it to blank.
                $addNewDlg.find('input#name').val("").prop("readonly", false).removeClass('readonly');

                // Enable type radio buttons
                var $authType = $addNewDlg.find('#authType');
                if (window.globalAppPasswordsAllowed) {
                    // Set app-password as the default type.
                    $authType.find("#rb_app_password").prop("checked", true).attr('aria-checked', true).prop("disabled", false);
                } else {
                    // appPasswordAllowed is not configured in the client.  Disable the
                    // app-password radio button and set tooltip to indicate that the 
                    // client is not configured to allow app-passwords.
                    $authType.find("#rb_app_password").prop("checked", false).attr('aria-checked', false).prop("disabled", true);
                    // Set tooltip on radio button's label.
                    $authType.find("label[for='rb_app_password']").attr('title', messages.APP_PASSWORD_NOT_CONFIGURED);
                }

                if (window.globalAppTokensAllowed) {
                    if (window.globalAppPasswordsAllowed) {
                        // Both app-passwords and app-tokens are configured.  Display the
                        // app-token radio button as enabled, but not selected.
                        $authType.find("#rb_app_token").prop("checked", false).attr('aria-checked', false).prop("disabled", false);
                    } else {
                        // Only app-tokens are configured.  Set app-token as the default type.
                        $authType.find("#rb_app_token").prop("checked", true).attr('aria-checked', true).prop("disabled", false);
                    }
                } else {
                    // appTokenAllowed is not configured in the client.  Disable the
                    // app-token radio button and set tooltip to indicate that the 
                    // client is not configured to allow app-tokens.
                    $authType.find("#rb_app_token").prop("checked", false).attr('aria-checked', false).prop("disabled", true);
                    // Set tooltip on radio button's label.
                    $authType.find("label[for='rb_app_token']").attr('title', messages.APP_TOKEN_NOT_CONFIGURED);
                }

                // Reset the auth value field to "Not generated"
                $addNewDlg.find("#auth_value").val("").attr({"placeholder": messages.NOT_GENERATED_PLACEHOLDER, "aria-label": messages.AUTHENTICAION_GENERATED});
                $addNewDlg.find('.tool_modal_field_copy_button').prop("disabled", true);
                $addNewDlg.find(".tool_modal_field_copy_button>img").prop('title', messages.COPY_TO_CLIPBOARD).prop('alt', messages.COPY_TO_CLIPBOARD);

                // Enable the correct actions
                $addNewDlg.find('.tool_modal_cancel_button').removeClass('hidden');
                $addNewDlg.find('.tool_modal_generate_button').off('click');
                if (window.globalAppPasswordsAllowed || window.globalAppTokensAllowed) {
                    $addNewDlg.find('.tool_modal_generate_button').on('click', function() {
                        utils.startProcessingSpinner('add_regen_processing');
                        generateNewAuthentication();
                    }).removeClass('hidden').prop('disabled', true);
                } else {
                    // Neither app-passwords or app-tokens are configured on client so don't 
                    // allow any creation of new ones.
                    $addNewDlg.find('.tool_modal_generate_button').removeClass('hidden').prop('disabled', true);
                }
                $addNewDlg.find('.tool_modal_done_button').addClass('hidden');

                $addNewDlg.removeClass("hidden");
                $addNewDlg.find('input#name').get(0).focus();
            });

            // Validate input prior to enabling Generate button
            $('input#name').on('input', function() {
                var fldValue = $(this).val();
                // One of the Authentication Type radio buttons should be selected.
                var rbselected = $("input[name='authType']:checked").length > 0;
                if (!fldValue || !rbselected) {
                    $('.tool_modal_generate_button').prop('disabled', true);
                } else {
                    $('.tool_modal_generate_button').prop('disabled', false);
                }
            });

            $('.tool_modal_radio_button').on('click', function() {
                var $selectedrb = $(this);
                $selectedrb.prop("checked", true).attr('aria-checked', true);
                if ($selectedrb.val() === 'app-password') {
                    $('#rb_app_token').prop("checked", false).attr('aria-checked', false);
                } else {
                    $('#rb_app_password').prop("checked", false).attr('aria-checked', false);
                }    
            }).on('focus', function() {
                // Find the associated label element
                var rbID = this.id;
                $("label[for='" + rbID + "']").find('.tool_modal_radio_button_appearance').addClass('radio_button_focus');
            }).on('blur', function() {
                // Find the associated label element
                var rbID = this.id;
                $("label[for='" + rbID + "']").find('.tool_modal_radio_button_appearance').removeClass('radio_button_focus');
            });

            $(".tool_modal_field_copy_button").click(function(event) {
                event.preventDefault();
                utils.copyToClipboard(this, function () {
                    // Place the 'Copy to clipboard' message above the authDiv aligned to the
                    // right border
                    var current_target_object = $(event.currentTarget);
                    var parentDiv = current_target_object.parent();    // enclosing auth div
                    var newTop = parentDiv.offset().top - 16;
                    var msgWidth = $('#copied_confirmation').width();
                    var newLeft = parentDiv.offset().left + parentDiv.width() - msgWidth - 2;
                    $('#copied_confirmation').css({
                        top: newTop,
                        left: newLeft
                    }).stop().fadeIn().delay(200).fadeOut();
                    event.currentTarget.focus();
                });
            });
            // Handle enter key presses on the copy to clipboard button
            $(".tool_modal_field_copy_button").on('keydown', function(event) {
                var key = event.which || event.keyCode;
                if (key === 13) {    // Enter key
                    $(this).trigger('click');
                }
            });

            utils.initModalDialogKeystrokes();
            
            if (globalization.isBidiEnabled()) {
                bidiUtils.setupToolBidi();
            }

        });
    };

    $(document).ready(function() {
        if (self !== top) {
            // Since the content is displayed in an iframe, assume it's thru admin center. Look for bidi preference.
            globalization.retrieveBidiPreference().always(function() {
                $('.tool_container').removeClass('hide');
                __initAccountManager();
            });
        } else {
            utils.initLogout();
            $('.tool_container').removeClass('hide');
            __initAccountManager();
        }
    });
})();
