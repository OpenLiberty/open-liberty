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

var clientInputDialog = (function() {
    "use strict";

    var init = function() {
        // Get the dynamic list of input fields for the oauth client add/edit dialog
        apiUtils.getInputFields().done(function (response) {
        	var data = response;
            var $addEditModal = $('#add_edit_client_modal .tool_modal_body');

            if (data.length !== 0) {
                for (var x=0; x<data.length; x++) {
                    // Create the fields in the add/edit client dialog.
                    var field = data[x];
                    field = __createInputField(field);
                    $addEditModal.append(field);
                }
            }

            // Enable the Add New client button.
            $('#add_new_client').prop('disabled', false);

            // Array 'Add element' buttons
            $('.tool_modal_array_add_ele').click(function() {
                var $array = $(this).closest('.tool_modal_array').find('table');
                var newRow = __createArrayEntry();
                $array.append(newRow);
                var $newRow = $array.find('tr').last();
                __initArrayRow($newRow);
                $newRow.find('input').focus();
            });

            // Array 'input' fields
            $('.tool_modal_array .tool_modal_table tbody tr').each(function() {
                __initArrayRow($(this));
            });

            // Multi-selection dropdown field
            $('.tool_multiSelect_field').on('click keydown', (function(event) {
                var displayHideList = false;
                var $selectionListButton = $(this);
                if (event.type === "keydown") {
                    var key = event.which || event.keyCode;
                    if (key === 13 || key === 0 || key === 32) {    // Enter or space bar
                        event.stopPropagation();  // Stop tool table from processing enter too
                        event.preventDefault();
                        // Toggle displaying the list.
                        displayHideList = true;
                    }
                
                    if (key === 27 ) {   // Escape
                        // Close the dropdown list
                        event.stopPropagation();  // Just close the list, don't exit dialog
                        event.preventDefault();
                        __displaySelectionList($selectionListButton, false);
                        $(this).focus();
                    } else if (key === 40  ||    // Down arrow
                               key === 38) {     // Up arrow
                        event.stopPropagation();
                        event.preventDefault();
                        // Open the list and focus on the first element
                        __displaySelectionList($selectionListButton, true);
                        $selectionListButton.siblings('.tool_multiSelect_dropdown').find('.tool_multiSelect_menuItem').first().focus();
                    }
                } else {  // event.type was an onClick
                    displayHideList = true;
                }

                if (displayHideList) {           // Toggle the dropdown list display
                    var $dropDownList = $selectionListButton.siblings('.tool_multiSelect_dropdown');
                    if ($dropDownList.css('display') === 'none'){
                        // Close all other selection lists
                        var $allSelectionListButtons = $('#add_edit_client_modal .tool_multiSelect_field');
                        for (var i=0; i<$allSelectionListButtons.length; i++) {
                            var $listButton = $($allSelectionListButtons[i]);
                            __displaySelectionList($listButton, false);
                        }

                        // Display THIS selection list
                        __displaySelectionList($selectionListButton, true);
                    } else {
                        // List is showing....so hide it now
                        __displaySelectionList($selectionListButton, false);
                    }
                }
            })).on('focus', function() {
                var $selectionListButton = $(this);
                var $dropDownList = $selectionListButton.siblings('.tool_multiSelect_dropdown');
                if ($dropDownList.css('display') === 'none') {
                    // Receiving focus....ensure other lists are not expanded.
                    var $allSelectionListButtons = $('#add_edit_client_modal .tool_multiSelect_field');
                    for (var i=0; i<$allSelectionListButtons.length; i++) {
                        var $listButton = $($allSelectionListButtons[i]);
                        __displaySelectionList($listButton, false);
                    }
                }
            });

            // Multi-select dropdown reset button
            $('.tool_multiSelect_selection').on('click keydown', (function(event) {
                // Clear out all selections and set the appropriate status
                var reset = true;

                if (event.type === "keydown") {
                    var key = event.which || event.keyCode;
                    if (key !== 13  &&    // Enter key pressed
                        key !== 32) {     // Space key pressed
                        reset = false;
                    }
                }

                if (reset) {
                    event.stopPropagation();
                    event.preventDefault();
                    var $dropDownList = $(this).parent().siblings('.tool_multiSelect_dropdown');
                    // Un-select ALL selections.
                    $dropDownList.find('input:checked').prop('checked', false).attr('aria-checked', false);
                    $dropDownList.find('.tool_multiSelect_menuItem').attr('aria-selected', false);
                    // Update the list of selected elements from the selection list
                    __updateSelectionValues($dropDownList);
                }
            }));

            // Multi-select dropdown list checkbox menu item
            $('.tool_multiSelect_dropdown>.tool_multiSelect_menuItem').on('click keydown', function(event) {
                // Toggle the checked state of the checkbox
                var toggleCheckbox = true;
                if (event.type === "keydown") {
                    var key = event.which || event.keyCode;
                    if (key !==0 && key !== 32) {        // Space key toggles a checkbox
                        toggleCheckbox = false;
                    }
                
                    if (key === 27 ) {   // Escape
                        // Close the dropdown list
                        event.stopPropagation();         // Just close the list, don't exit dialog
                        event.preventDefault();
                        var $selectionListButton = $(this).parent().siblings('.tool_multiSelect_field');
                        __displaySelectionList($selectionListButton, false);
                        $selectionListButton.focus();
                    } else if (key === 38) {             // Up arrow....advance focus up one element
                        event.stopPropagation();
                        event.preventDefault();
                        __focusNextSelectionInList(this, false);
                    } else if (key === 40) {             // Down arrow....move focus down one element
                        event.stopPropagation();
                        event.preventDefault(); 
                        __focusNextSelectionInList(this, true);
                    }
                }

                if (toggleCheckbox) {      // Toggle checked state of the input checkbox
                    event.stopPropagation();
                    event.preventDefault();
                    var $checkbox = $(this).find("input[type='checkbox']");
                    if ($checkbox.prop("checked")) {
                        $checkbox.prop("checked", false);
                        $checkbox.attr("aria-checked", false);
                        $(this).attr('aria-selected', false).focus();
                    } else {
                        $checkbox.prop("checked", true);
                        $checkbox.attr("aria-checked", true);
                        $(this).attr('aria-selected', true).focus();
                    }
    
                    // Update the list of checked values
                    __updateSelectionValues($(this).parent());
                }
            });
            // Close the dropdown list when user selects any other field or button
            $('#add_edit_client_modal .tool_modal_body_field:not(:has(.tool_multiSelect_listbox)), #add_edit_client_modal .tool_modal_action_button').on('click', function() {
                var $selectionListButtons = $('.tool_multiSelect_field');
                for (var i=0; i<$selectionListButtons.length; i++) {
                    var $selectionListButton = $($selectionListButtons[i]);
                    __displaySelectionList($selectionListButton, false);
                }
            });
            $('#add_edit_client_modal .tool_modal_body_field_input, #add_edit_client_modal .tool_modal_array td input, #add_edit_client_modal .tool_modal_radio_button').on('focus', function() {
                var $selectionListButtons = $('.tool_multiSelect_field');
                for (var i=0; i<$selectionListButtons.length; i++) {
                    var $selectionListButton = $($selectionListButtons[i]);
                    __displaySelectionList($selectionListButton, false);
                }
            });

            // Single checkbox
            $('#add_edit_client_modal .tool_single_checkbox').on('click keydown', function(event) {
                var toggleCheckbox = true;
                if (event.type === "keydown") {
                    var key = event.which || event.keyCode;
                    if (key !== 0 && key !== 32) {      // Space key toggles a checkbox
                        toggleCheckbox = false;
                    }
                }

                if (toggleCheckbox) {
                    // Toggle the checked state of the input element
                    var $checkbox = $(this).find("input[type='checkbox']");
                    if ($checkbox.prop("checked")) {
                        $checkbox.prop("checked", false).attr('aria-checked', false);
                    } else {
                        $checkbox.prop("checked", true).attr('aria-checked', true);
                    }
                }
            });

            // Radio buttons - groups and T/F Booleans
            $('#add_edit_client_modal .tool_modal_radio_button').on('click', function() {
                var $selectedrb = $(this);
                var rbName = this.name;
                $selectedrb.attr('aria-checked', true);
                $("#add_edit_client_modal .tool_modal_radio_button[name='" + rbName + "']:not(:checked)").attr('aria-checked', false);
            }).on('focus', function() {
                // Find the associated label element
                var rbID = this.id;
                $("label[for='" + rbID + "']").find('.tool_modal_radio_button_appearance').addClass('radio_button_focus');
            }).on('blur', function() {
                // Find the associated label element
                var rbID = this.id;
                $("label[for='" + rbID + "']").find('.tool_modal_radio_button_appearance').removeClass('radio_button_focus');
            });

            $(".tool_modal_register_button").click(function() {
                utils.startProcessingSpinner('add_edit_processing');

                var clientData = __getClientRegistrationData();
                var client_name = clientData.client_name ? clientData.client_name : "";

                // Remove any existing errors from the dialog
                utils.cleanUpErrorFields();

                apiUtils.addClient(clientData).done(function(response) {
                    // Add row to the table
                    var filterValue = $('#filter_client_name').val();
                    table.createTableRow(response, filterValue);

                    // Set up the Copy client secret dialog
                    $('#client_secret_modal_title').text(messages.MODAL_SECRET_REGISTER_TITLE);
                    $('#client_secret_modal_description').text(messages.REGISTRATION_SAVED);
                    $("#gend_client_id").closest('.tool_modal_body_field').removeClass('hidden');
                    $("#gend_client_id").val(response.client_id);
                    $("#gend_client_secret").val(response.client_secret);

                    // Hide the other dialogs and open the Copy client secret dialog
                    $(".tool_modal_container").addClass('hidden');
                    $("#client_secret_modal").removeClass('hidden');
                    $("#gend_client_id_copy").get(0).focus();

                    // Re-sort the table to put the new client in the correct location.
                    // If direction is "none" then the table has not yet been sorted, so leave new
                    // entry where it is.
                    var direction = tableUtils.currentSortDirection('table_client_name_column');
                    if (direction !== "none") {
                        tableUtils.sortTableWithFilterName(table.tableId, direction === "ascending");
                    }
                    // Leave the user on the same page
                    tableUtils.switchPage(table.tableId, tableUtils.currentPage());
                    utils.stopProcessingSpinner();
                })
                .fail(function(errResponse) {
                    console.log("add client failed for " + client_name);
                    console.log("errResponse.status: " + errResponse.status + ":   errResponse.statusText: " + errResponse.statusText);
                    // if (errResponse.status === 400) {
                    //     if (errResponse.responseJSON.error && errResponse.responseJSON.error === "invalid_redirect_uri") {
                    //         // Show error in redirect_uri field
                    //         __showFieldInputError(errResponse.responseJSON.error);
                    //         return;     // Only field we know we can get an error on
                    //     }
                    // } 
                    // Something happended with the request.  Put up error message.
                    var errTitle = messages.GENERIC_REGISTER_FAIL;
                    var errDescription = "";
                    if (errResponse.responseJSON && errResponse.responseJSON.error_description) {
                        errDescription = errResponse.responseJSON.error_description;
                    } else {
                        // Display a generic error message...
                        // insert bidi text direction to the client name
                        var errClientName = bidiUtils.getDOMSpanWithBidiTextDirection(utils.encodeData(client_name));
                        errDescription = utils.formatString(messages.GENERIC_REGISTER_FAIL_MSG, [errClientName]);
                    }
                    utils.showResultsDialog(true, errTitle, errDescription, true, true, false, __reshowAddEditDialog);
                });                      
            });

            $(".tool_modal_update_button").click(function() {
                utils.startProcessingSpinner('add_edit_processing');

                var clientId = $('input#client_id').val();
                var clientData = __getClientRegistrationData();
                clientData.client_id = clientId;
                var client_name = $('input#client_name').val().trim();

                // Remove any existing errors from the dialog
                utils.cleanUpErrorFields();

                apiUtils.editClient(clientData).done(function (response) {
                    // update row in the table
                    table.updateTableRow(response.client_id, response.client_name);

                    // Hide the other dialogs
                    $(".tool_modal_container").addClass('hidden');

                    if (response.client_secret && response.client_secret !== "*") {
                        // A new client_secret was generated for this client
                        // Display the secret in the dialog for the user to copy

                        // Set up the Copy client secret dialog
                        $('#client_secret_modal_title').text(messages.MODAL_SECRET_UPDATED_TITLE);
                        $('#client_secret_modal_description').text(messages.REGISTRATION_UPDATED);
                        $("#gend_client_id").closest('.tool_modal_body_field').addClass('hidden');
                        $("#gend_client_secret").val(response.client_secret);

                        utils.stopProcessingSpinner();

                        // Open the Copy client secret dialog
                        $("#client_secret_modal").removeClass('hidden');
                        $("#gend_client_secret_copy").get(0).focus();
                    } else {
                        // Show an update successful confirmation dialog
                        var successTitle = messages.MODAL_SECRET_UPDATED_TITLE;
                        // insert bidi text direction to the client name
                        var successClientName = bidiUtils.getDOMSpanWithBidiTextDirection(client_name);
                        var successUpdateMsg = utils.formatString(messages.REGISTRATION_UPDATED_NOSECRET, [successClientName]);
                        utils.showResultsDialog(false, successTitle, successUpdateMsg, false, false, true);
                    }
                })
                .fail(function(errResponse) {
                    console.log("update client failed for " + client_name);
                    console.log("errResponse.status: " + errResponse.status + ":   errResponse.statusText: " + errResponse.statusText);
                    // if (errResponse.status === 400) {
                    //     if (errResponse.responseJSON.error && errResponse.responseJSON.error === "invalid_redirect_uri") {
                    //         // Show error in redirect_uri field
                    //         __showFieldInputError(errResponse.responseJSON.error);
                    //         return;     // Only field we know we can get an error on
                    //     }
                    // }
                    // insert bidi text direction to the client name
                    var msgClientName = bidiUtils.getDOMSpanWithBidiTextDirection(utils.encodeData(client_name));
                    if (errResponse.status === 404) {
                        if (errResponse.responseJSON.error && errResponse.responseJSON.error === "invalid_client") {
                            // Updating an OAuth client that no longer exists....message the user and delete
                            // the row from the table.
                            console.log("client no longer exists, remove row");
                            table.deleteTableRow(clientId);

                            var missingTitle = messages.GENERIC_MISSING_CLIENT;
                            var missingDescription = utils.formatString(messages.GENERIC_MISSING_CLIENT_MSG, [msgClientName, clientId]);
                            utils.showResultsDialog(true, missingTitle, missingDescription, false, false, true);
                            return;
                        }
                    } 
                    // Something else happened with the request.  Put up an error message...
                    var errTitle = messages.GENERIC_UPDATE_FAIL;
                    var errDescription = "";
                    if (errResponse.responseJSON && errResponse.responseJSON.error_description) {
                        errDescription = errResponse.responseJSON.error_description;
                    } else {
                        // Display a generic error message...
                        errDescription = utils.formatString(messages.GENERIC_UPDATE_FAIL_MSG, [msgClientName]);
                    }
                    utils.showResultsDialog(true, errTitle, errDescription, true, true, false, __reshowAddEditDialog);
                });              
            });

            $(".tool_modal_field_copy_button").click(function(event) {
                event.preventDefault();
                utils.copyToClipboard(this, function() {
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
        }).fail(function (errResponse) {   // Failied to retrieve input fields
            // Nothing to do here....the 'Add New' button remains disabled.    
        });
    };

    var __createInputField = function(fieldData) {
        var fldId = fieldData.requestParameterName ? fieldData.requestParameterName : fieldData.id;
        var fldType = fieldData.type;

        if (!fldId) {
            console.error("No matching parameter in Registration API for " + fieldData.id);
            return null;
        }
        var cardinality = fieldData.cardinality && fieldData.cardinality > 0 ? fieldData.cardinality: undefined;
        var options = fieldData.options && fieldData.options.length > 0 ? fieldData.options: undefined;

        var modal_body_field;
        if (cardinality && !options) {
            modal_body_field = __createArrayList(fieldData);
        } else if (cardinality && options) {
            modal_body_field = __createMultiSelectionList(fieldData);
        } else if (options) {
            if (options.length > 1) {
                modal_body_field = __createRadioButtonGroup(fieldData);
            } else {
                modal_body_field = __createCheckboxItem(fieldData);
            }
        } else if (fldType === 'Boolean') {
            modal_body_field = __createBooleanRadioButton(fieldData);
        } else {
            // if (fieldData.seperationChar || fieldData.separationChar) {              
            //     console.log("Multi-value text field separated by character.  May need directional message");
            // }
            modal_body_field = __createTextInputField(fieldData);
        }
        return modal_body_field;
    };

    var __createTextInputField = function(fieldData) {
        var fldId = fieldData.requestParameterName ? fieldData.requestParameterName : fieldData.id;
        var fldName = fieldData.name;
        var fldDescription = fieldData.description;
        var fldRequired = fieldData.required ? 'requiredfield': '';
        var fldDefaultVal = fieldData.default;
        var fldDefault = fieldData.default ? " data-default='" + fieldData.default + "' ": '';
        var dirValue = bidiUtils.getDOMBidiTextDirection();

        // Special case names for UI purposes
        switch (fldId) {
            case 'client_secret':
                fldName = messages.CLIENT_SECRET;
                break;
            case 'client_id':
                fldName = messages.CLIENT_ID;
                break;
            case 'client_name':
                fldName = messages.CLIENT_NAME;
                break;
        }

        var field =
            "<div class='tool_modal_body_field'>" +
            "   <label class='tool_modal_body_field_label' for=" + fldId + ">" + fldName +
            "      <div class='tool_modal_body_field_helper_text'>" + fldDescription + "</div>";
        if (fldId === 'client_secret') {     // Special case for the UI to include instructional statement for client_secret
            field += "      <div id='regen_client_secret_dir' class='tool_modal_body_field_helper_text hidden'>" + messages.REGENERATE_CLIENT_SECRET + "</div>";
        }
        field +=  "      <input id=" + fldId + " type='text' class='tool_modal_body_field_input'" + fldRequired + " value='" + fldDefaultVal + "'" + fldDefault + dirValue + ">";
        field +=  "   </label></div>";

        
        return field;
    };

    var __createMultiSelectionList = function(fieldData) {
        var fldId = fieldData.requestParameterName ? fieldData.requestParameterName : fieldData.id;
        var fldName = fieldData.name;
        var fldDescription = fieldData.description;
        var fldRequired = fieldData.required ? 'requiredfield': '';
        var fldOptions = fieldData.options;   // Each option is an object:  { label: xxx, value: xxx }
        var fldDefault = fieldData.default ? fieldData.default : [];  // Array of option's value that should be preselected.

        // Strings used within widget
        var resetButtonTitle = utils.formatString(messages.RESET_SELECTION, [fldName]);
        var numberSelectedTitle = utils.formatString(messages.NUMBER_SELECTED, [fldName]);
        var openListString = utils.formatString(messages.OPEN_LIST, [fldName]);

        var field = 
            "<div class='tool_modal_body_field'>" +
            "   <label id='" + fldId + "_selection_label' class='tool_modal_body_field_label'>" + fldName + "</label>" +
            "   <div class='tool_modal_body_field_helper_text'>" + fldDescription + "</div>" +
            "   <div id='" + fldId + "' class='tool_multiSelect_listbox'>" +
            "       <div id='" + fldId + "_list_show' class='tool_multiSelect_field' tabindex='0' type='button' aria-haspopup='listbox' aria-labelledby='" + fldId + "_selection_label' role='button'>" +
//            Removing the reset button from the drop down selection list until Carbon responds to the DAP violation associated with this widget.
//               See     https://github.com/carbon-design-system/carbon/issues/3114        
//            "           <div id='" + fldId + "_reset' class='tool_multiSelect_selection' type='button' tabindex='0' title='" + resetButtonTitle + "' role='button'>" +
//            "               <span title='" + numberSelectedTitle + "'></span>" +
//            "               <svg focusable='false' preserveAspectRatio='xMidYMid meet' style='will-change: transform;' role='img' alt='" + resetButtonTitle + "' title='" + resetButtonTitle + "' xmlns='http://www.w3.org/2000/svg' width='16' height='16' viewBox='0 0 16 16' aria-hidden='true'><path d='M12 4.7l-.7-.7L8 7.3 4.7 4l-.7.7L7.3 8 4 11.3l.7.7L8 8.7l3.3 3.3.7-.7L8.7 8z'></path></svg>" +
//            "           </div>" + 
            "           <span id='" + fldId + "_selection' class='tool_multiSelect_selection_text' " + fldRequired + "></span>" + 
            "           <div class='tool_multiSelect_menu_icon'>" +
            "               <img src='../../WEB-CONTENT/common/images/caretDown.svg' alt='" + openListString + "' title='" + openListString + "'>" +
            "           </div>" +
            "       </div>" + 
            "       <div id='" + fldId + "_list' class='tool_multiSelect_dropdown hidden' role='listbox' tabindex='-1' aria-labelledby='" + fldId + "_selection_label' aria-multiselectable=true>";

        for (var x=0; x < fldOptions.length; x++) {
            // Add an option element for every option provided
            var option = fldOptions[x];
            var optionId = option.value.replace(/\s/g, '__');
            var defaultValue = fldDefault.indexOf(option.value) > -1;
            field +=
                "           <div class='tool_multiSelect_menuItem' role=option tabindex='-1' data-default=" + defaultValue + ">" +
                "               <div class='tool_multiSelect_menuItem_option'>" +
                "                   <div class='tool_checkbox_wrapper'>" +
                "                       <input id='" + optionId + "' class='tool_checkbox' type='checkbox' role='checkbox' aria-checked='false' value='" + option.value + "'>" +
                "                       <label class='tool_checkbox_label' for='" + optionId + "'>" + option.value + "</label>" +
                "                   </div>" +
                "               </div>" + 
                "           </div>";
        }    

        // Wrap it up!
        field +=
            "       </div>" + 
            "   </div>" +
            "</div>";

        return field;
    };

    var __createRadioButtonGroup = function(fieldData) {
        var fldId = fieldData.requestParameterName ? fieldData.requestParameterName : fieldData.id;
        var fldName = fieldData.name;
        var fldDescription = fieldData.description;
        var fldOptions = fieldData.options;   // Each option is an object:  { label: xxx, value: xxx }
        var fldDefault = fieldData.default ? fieldData.default[0] : "";  // Option value that should be preselected.

        var field = 
            "<div id='" + fldId + "' class='tool_modal_body_field tool_modal_radio_button_group'>" +
            "   <fieldset class='tool_modal_radio_button_fieldset'>" +
            "       <legend class='tool_modal_body_field_label'>" + fldName + "</legend>" +
            "       <div class='tool_modal_body_field_helper_text'>" + fldDescription + "</div>";

        for (var x=0; x<fldOptions.length; x++) {
            var option = fldOptions[x];
            var optionId = 'rb_' + option.value.replace(/\s/g, '__');
            var defaultValue = option.value === fldDefault;
            field +=
                "       <input id='" + optionId + "' class='tool_modal_radio_button' type='radio' role='radio' aria-checked='false' name='" + fldId + "' value='" + option.value + "' data-default=" + defaultValue + ">" +
                "       <label for='" + optionId + "' class='tool_modal_radio_button_label'>" +
                "           <span class='tool_modal_radio_button_appearance'></span>" + 
                "           " + option.value + 
                "       </label>";
        }

        // Wrap it up!
        field +=
            "   </fieldset>" +
            "</div>";

        return field;            
    };

    var __createCheckboxItem = function(fieldData) {
        var fldId = fieldData.requestParameterName ? fieldData.requestParameterName : fieldData.id;
        var fldName = fieldData.name;
        var fldDescription = fieldData.description;
        var fldOptionLabel = fieldData.options[0].label;   // Should only be one option value for this field.
        var fldOptionValue = fieldData.options[0].value;
        var fldDefault = fieldData.default ? fieldData.default[0] : "";  // Should this value be preselected.

        var field = 
            "<div class='tool_modal_body_field'>" +
            "   <label class='tool_modal_body_field_label'>" + fldName + "</label>" +
            "   <div id='" + fldId + "' class='tool_checkbox_wrapper tool_single_checkbox'>" +
            "       <input id='" + fldId + "_box' class='tool_checkbox' type='checkbox' role='checkbox' aria-checked='false' value='" + fldOptionValue + "'>" +
            "       <label class='tool_checkbox_label' for='" + fldId + "_box'>" + fldOptionLabel + "</label>" +
            "   </div>" + 
            "</div>";

        return field;            
    };

    var __createBooleanRadioButton = function(fieldData) {
        var fldId = fieldData.requestParameterName ? fieldData.requestParameterName : fieldData.id;
        var fldName = fieldData.name;
        var fldDescription = fieldData.description;
        var fldDefault = fieldData.default[0];

        var field = 
            "<div id='" + fldId + "' class='tool_modal_body_field tool_modal_boolean'>" +
            "   <fieldset class='tool_modal_radio_button_fieldset'>" +
            "       <legend class='tool_modal_body_field_label'>" + fldName + "</legend>" +
            "       <div class='tool_modal_body_field_helper_text'>" + fldDescription + "</div>" +
            "       <div class='tool_modal_radio_button_horizontal_wrapper'>" +
            "           <input id='" + fldId + "-true' class='tool_modal_radio_button' type='radio' role='radio' aria-checked='false' name='" + fldId + "' value='true'";
        field += fldDefault === 'true' ? " data-default='true'" : " data-default='false'";
        field += ">" +
            "           <label for='" + fldId + "-true' class='tool_modal_radio_button_label'>" +
            "               <span class='tool_modal_radio_button_appearance'></span>" + messages.TRUE + 
            "           </label>" +
            "           <input id='" + fldId + "-false' class='tool_modal_radio_button' type='radio' role='radio' aria-checked='false' name='" + fldId + "' value='false'";
        field += fldDefault === 'false' ? " data-default='true'" : " data-default='false'";
        field += ">" +
            "           <label for='" + fldId + "-false' class='tool_modal_radio_button_label'>" +
            "               <span class='tool_modal_radio_button_appearance'></span>" + messages.FALSE +
            "           </label>" +
            "       </div>" +
            "   </fieldset>" +
            "</div>";

        return field;            
    };

    var __createArrayList = function(fieldData) {
        var fldId = fieldData.requestParameterName ? fieldData.requestParameterName : fieldData.id;
        var fldName = fieldData.name;
        var fldDescription = fieldData.description;

        var field = 
            "<div class='tool_modal_body_field'>" +
            "   <label id='" + fldId + "_label' class='tool_modal_body_field_label'>" + fldName + "</label>" +
            "   <div class='tool_modal_body_field_helper_text'>" + fldDescription + "</div>" +
            "   <div class='tool_modal_array'>" +
            "       <table id='" + fldId + "' class='tool_modal_table' aria-labelledby='" + fldId + "_label'>" +
            "           <thead>" +
            "               <th>" + messages.ADD_VALUE + "</th><th></th>" +
            "           </thead>" +
            "           <tbody>";

        field += __createArrayEntry();
        
        field += 
            "           </tbody>" +
            "       </table>" + 
            "       <div class='tool_modal_array_button_div tool_modal_add_array_button_div'>" +
            "           <button class='tool_modal_array_button tool_modal_array_add_ele' type='button'" + " aria-label='" + messages.ADD_VALUE + "'>" +
            "               <img src='../../WEB-CONTENT/common/images/addEntry.svg' alt='" + messages.ADD_VALUE + "' title='" +  messages.ADD_VALUE +"'>" +
            "           </button>" +
            "       </div>" +
            "   </div>" +
            "</div>";

        return field;

    };

    var __createArrayEntry = function(value) {
        var dirValue = bidiUtils.getDOMBidiTextDirection();
        if (!value){
            value = "";
        }

        var arrayRow =
            "<tr>" +
            "   <td><input type='text' value='" + value + "' title='" + value + "' placeholder='" + messages.ENTER_PLACEHOLDER + "' " + dirValue + "></input></td>" +
            "   <td><div class='tool_modal_array_button_div'><button class='tool_modal_array_button tool_modal_array_remove_ele' type='button' aria-label='" + messages.REMOVE_VALUE + "'>" +
            "       <img src='../../WEB-CONTENT/common/images/removeEntry.svg' alt='" + messages.REMOVE_VALUE + "' title='" +  messages.REMOVE_VALUE +"'></button>" + 
            "   </div></td>" +
            "</tr>";

        return arrayRow;
    };

    var __initArrayRow = function($row) {
        var $inputFld = $row.find('input');
        // Set aria-label on input field because it doesn't have an associated label
        // or header element. Our array is a table without a header.
        var label = $row.closest('.tool_modal_array').siblings('label')[0].innerHTML;
        $inputFld.attr('aria-label', label);
        $inputFld.on("keyup", function() {
            // Entries in this field may be long....so keep the title up-to-date with the 
            // current value as it is entered so user can hover to see the whole value
            $(this).prop("title", $(this).val());
        });

        var $removeButton = $row.find('.tool_modal_array_remove_ele');
        $removeButton.click(function() {
            var $table = $(this).closest('.tool_modal_table');
            var numEntries = $table.find('tbody tr').length;

            // Remove the row from the 'array'
            $(this).closest('tr').remove();

            // If we removed the last row of the table, create a new empty one
            if (numEntries === 1) {
                var newRow = __createArrayEntry();
                $table.append(newRow);

                var $newRow = $table.find('tbody tr').last();  // Row just added
                __initArrayRow($newRow);
            }
        });    
    };

    var __clearModalBodyFields = function() {
        // Clear all input text fields
        $("#add_edit_client_modal .tool_modal_body_field_input").each(function() {
            $(this).val("");
        });

        // Uncheck checkboxes
        $('.tool_checkbox').prop('checked', false).attr('aria-checked', false);

        // Reset all arrays by deleting the rows and adding back an empty row in each.
        var $arrayFields = $("#add_edit_client_modal .tool_modal_array");
        for (var x=0; x<$arrayFields.length; x++) {
            var $array = $($arrayFields[x]);
            var $table = $array.find('.tool_modal_table tbody');
            $table.find('tr').remove();
            var newRow = __createArrayEntry();
            $table.append(newRow);
            __initArrayRow($table.find('tr').last());
        }

        // For multi-selection lists, deselect all checkboxes and hide the selection list
        $('#add_edit_client_modal .tool_multiSelect_dropdown input:checkbox').prop('checked', false).attr('aria-checked', false);
        $('#add_edit_client_modal .tool_multiSelect_dropdown>.tool_multiSelect_menuItem').attr('aria-selected', false);
        var $selectionListButtons = $('#add_edit_client_modal .tool_multiSelect_field');
        for (var i=0; i<$selectionListButtons.length; i++) {
            var $selectionListButton = $($selectionListButtons[i]);
            __displaySelectionList($selectionListButton, false);
        }

        // Radio-buttons ... select the default button
        $("#add_edit_client_modal .tool_modal_radio_button[data-default='true']").prop('checked', true).attr('aria-checked', true);
        $("#add_edit_client_modal .tool_modal_radio_button[data-default='false']").prop('checked', false).attr('aria-checked', false);

        // Clean up any error indicators
        utils.cleanUpErrorFields();
    };

    var __setFieldDefaults = function() {
        // First, clear all currently selected values.
        __clearModalBodyFields();

        // Input text fields ... assign default value
        $("#add_edit_client_modal .tool_modal_body_field_input[data-default]").each(function() {
            var defaultVal = $(this).attr('data-default');
            $(this).val(defaultVal);
        });

        // Array fields ... no known defaults set

        // Multi-selection lists ... check default values and identify selections on drop-down button
        $("#add_edit_client_modal .tool_multiSelect_menuItem[data-default='true']").attr('aria-selected', true).find("input.tool_checkbox").prop('checked', true).attr('aria-checked', true);
        $("#add_edit_client_modal .tool_multiSelect_dropdown").each(function() {
            __updateSelectionValues($(this));
        });
    };

    /**
     * Display or Hide the multiSelection List
     * @param {*} $multiSelectButton - The button you select to expand/contract a multi-selection list
     * @param boolean display - true to shown the list
     *                        - false to hide the list
//     * @param {*} init 
     */
    var __displaySelectionList = function($multiSelectButton, display) {
        // Get parameter id associated with this list for message details
        var labelFld = $multiSelectButton.attr('aria-labelledby');
        var fldName = $('#' + labelFld)[0].innerHTML;
        var closeMsg = utils.formatString(messages.CLOSE_LIST, [fldName]);
        var openMsg = utils.formatString(messages.OPEN_LIST, [fldName]);
        if (display) {
            $multiSelectButton.find('.tool_multiSelect_menu_icon img').prop('src', "../../WEB-CONTENT/common/images/caretUp.svg").attr({'alt': closeMsg, 'title': closeMsg});
            $multiSelectButton.siblings('.tool_multiSelect_dropdown').removeClass('hidden');
            $multiSelectButton.attr('aria-expanded', true);   
        } else {    // Hide the selection list
            $multiSelectButton.find('.tool_multiSelect_menu_icon img').prop('src', "../../WEB-CONTENT/common/images/caretDown.svg").attr({'alt': openMsg, 'title': openMsg});
            $multiSelectButton.siblings('.tool_multiSelect_dropdown').addClass('hidden');
            $multiSelectButton.attr('aria-expanded', false);
        }
    };

    var __updateSelectionValues = function($dropDownList) {
        var checkedValues = messages.NONE_SELECTED;
        var selectedVals = $dropDownList.find('input:checkbox:checked').map(function() {
            return this.value;
        }).get();
        if (selectedVals.length > 0) {
            checkedValues = selectedVals.join(", ");
        }

        var $dropDownSelectionButton = $dropDownList.siblings('.tool_multiSelect_field');
        $dropDownSelectionButton.find(".tool_multiSelect_selection>span").html(selectedVals.length);
        $dropDownSelectionButton.find(".tool_multiSelect_selection_text").html(checkedValues).prop("title", checkedValues);
    };

    /**
     * Traverse a multi-selection list with the up or down arrow keys
     * 
     * @param {*} element - DOM Element that was in focus when the arrow key was pressed
     * @param boolean focusNext - true to move to the next element (below) in the list;
     *                            false to move to the element above in the list
     */
    var __focusNextSelectionInList = function(element, focusNext) {
        // Selection list checkbox wrapper <div> was given a tabindex = -1 so we could set focus to it
        var $element = $(element);

        // Adjust index for focus direction
        if (focusNext) {
            // Arrowing down the list ... move focus to the next element until hit the bottom of the list
            $element.next().focus();
        } else {
            // Arrowing up the list ... move focus to the previous element until hit the top of the list
            $element.prev().focus();
        }
    };

    /**
     * NOTE:  ALL FIELDS ARE NOW OPTIONAL.  COMMENTING OUT THIS CODE.
     * Displays or hides the 'At least one grant type must be selected' message and
     * marks the Grant types selection field accordingly.  
     * 
     * @param boolean displayErr - true indicates to display the error 
     *                           - false indicates to remove the error
     */
/*     var __notifyNoGrantTypeSelected = function(displayErr) {
        if (displayErr) {
            // Add red border around grant types field
            $('#grant_types_list_show').addClass('tool_field_error');

            // Add red exclamation to the Grant Type label above the drop down button
            if ($('#grant_types_list_show_error').length !== 0 ) {
                // Red exclamation icon for field exists...display it
                $('#grant_types_list_show_error').removeClass("hidden");
            } else {
                // Create the image and insert it into the field
                var errExclamation = $('<img id="grant_types_list_show_error" src="../../WEB-CONTENT/common/images/errorExclamation.svg" class="tool_err_img" data-externalizedStringAlt="" aria-hidden=true>');
                errExclamation.insertAfter('#grant_types_selection_label');
            }

            // Add red error message underneath the field
            if ($('#grant_types_list_err_message').length !== 0) {
                // Error message div exists..display it
                $("#grant_types_list_err_message").removeClass("hidden");
            } else {
                // Create the message div and insert it directly under the Grant types field
                var errMessage = $('<div id="grant_types_list_err_message" class="tool_err_msg">' + messages.ERR_MULTISELECT_GRANT_TYPES + '</div>');
                $("#grant_types_selection").parent().append(errMessage);
            }
        } else {
            // Hide any error indication for the Grant types selection list
            $('#grant_types_list_show').removeClass('tool_field_error');
            $('#grant_types_list_show_error').addClass("hidden");
            $('#grant_types_list_err_message').addClass("hidden");
        }
    }; */

    var __getClientRegistrationData = function() {
        var clientData = {};

        $('#add_edit_client_modal .tool_modal_body_field_input').each(function() {
            var id = this.id;
            var inputVal = this.value.trim() || "";
            clientData[id] = inputVal;
        });

        $("#add_edit_client_modal .tool_modal_array .tool_modal_table").each(function() {
            var $table = $(this).find('tbody');
            var $rows = $table.find('tr');
            var id = this.id;
            var arrVals = [];

            for (var arrX=0; arrX<$rows.length; arrX++) {
                // For each value entered, add an entry in arrVals.
                var $row = $($rows[arrX]);
                var arrVal = $row.find('input').val().trim();
                if (arrVal) {
                    arrVals.push(arrVal);
                }
            }
            clientData[id] = arrVals;
        });

        $("#add_edit_client_modal .tool_multiSelect_listbox").each(function() {
            var $dropDownList = $(this).find('.tool_multiSelect_dropdown');
            var id = this.id;

            var selectedVals = $dropDownList.find('input:checkbox:checked').map(function() {
                return this.value;
            }).get();
            if (selectedVals.length === 0) {
                selectedVals = [];
            }
            clientData[id] = selectedVals;
        });

        $("#add_edit_client_modal .tool_modal_radio_button_group").each(function() {
            var $rbGroup = $(this);
            var id = this.id;
            var rbGroupVal = $rbGroup.find('input:radio:checked').val();
            clientData[id] = rbGroupVal;
        });

        $("#add_edit_client_modal .tool_modal_boolean").each(function() {
            var $boolean = $(this);
            var id = this.id;
            var booleanVal = $boolean.find('input:radio:checked').val() === 'true';
            clientData[id] = booleanVal;
        });

        $('.tool_single_checkbox').each(function() {
            var $checkbox = $(this).find("input[type='checkbox']");
            var id = this.id;
            if ($checkbox.prop('checked')) {
                var checkboxVal = $checkbox.val();
                clientData[id] = checkboxVal;
            }
        });

        return clientData;
    };

    // NOTE:  With dynamically created input fields we no longer had any 
    //        required fields.  However, in case we change our mind about things
    //        like 'Scope' which will cause an error at runtime if not filled in,
    //        leaving this code in here as a reference.
    //        Required fields were set up with a 'requiredfields' attribute in the 
    //        DOM.
    //        __setUpValidation would be called from the setupxxxDialog() methods 
    //        below.  __validateRequiredInputs would also be called from 
    //        setupEditClientDialog() to validate the input returned on the client
    //        from the server, and from any place that one of the required fields 
    //        were updated.
    /**
     * Validate that the required fields have a value and enable the dialog's action
     * button accordingly.
     */
    // var __validateRequiredInputs = function() {
    //     // Required input fields have the attribute 'requiredfields'.  We could not use 'required' because
    //     // firefox will pick up on that keyword and outline the entry field in red.
    //     var isReady = true;
    //     var $requiredInputFields = $('#add_edit_client_modal').find('*[requiredfield]');
    //     $requiredInputFields.each(function() {
    //         var value = $(this).val();
    //         if (this.tagName.toUpperCase() === "SPAN") {
    //             value = this.innerHTML;    // requiredfield for Grant types is a span
    //             if (value === messages.GRANT_TYPES_NONE_SELECTED) {
    //                 value = "";
    //             }
    //         }
    //         if (!value) {
    //             isReady = false;
    //             return false;
    //         }
    //     });
    //     if (isReady) {
    //         if (__isAddClient) {
    //             $('.tool_modal_register_button').prop('disabled', false);
    //         } else {
    //             $('.tool_modal_update_button').prop('disabled', false);
    //         }
    //     } else {
    //         if (__isAddClient) {
    //             $('.tool_modal_register_button').prop('disabled', true);
    //         } else {
    //             $('.tool_modal_update_button').prop('disabled', true);
    //         }
    //     }
    // };


    // var __setupValidation = function(isAddClient) {
    //     __isAddClient = isAddClient;
    //     if (__initValidation) {
    //         return;
    //     }
    //     __initValidation = true;

    //     // Required input fields have the attribute 'requiredfields'.  We could not use 'required' because
    //     // firefox will pick up on that keyword and outline the entry field in red.
    //     var $requiredInputFields = $('#add_edit_client_modal .tool_modal_body_field_input[requiredField]');
    //     $requiredInputFields.each(function() {
    //         console.log('required is set for ' + this.id);
    //         $(this).on('input', function() {
    //             __validateRequiredInputs();
    //         });
    //     });
    // };

    // This function is to add special uincode to display bidi characters.
    // The only input field that that may require bidi display handling is URL. 
    // However, we are not going to enable the special handling until BIDI testing
    // results in a defect/issue. Hopefully with the help of the BIDI tester we will
    // know exactly how to handle URL.
    var __setupBidiDisplay = function() {
        var $valueTypeInputFields = $('#add_edit_client_modal .tool_modal_body_field_input[valueType]');
        $valueTypeInputFields.each(function() {
            if (globalization.dataTypeRequiresSpecialHandling($(this).attr('valueType')) &&
                globalization.isBidiEnabled()) {
                // input value type requires special bidi handling
                bidiUtils.displayBidiString($(this));
                $(this).on("keyup", function() {
                    bidiUtils.displayBidiString($(this));
                });
            }
        });
    };

    // NOTE: We are no longer posting error messages on individual fields.  But
    //       leaving this code here for future use to see how an error can be shown.
    // var __showFieldInputError = function(errorFieldName) {
    //     utils.stopProcessingSpinner();
    //     var fieldName = errorFieldName;
    //     if (errorFieldName === "invalid_redirect_uri") {
    //         fieldName = "redirect_uris";
    //     }

    //     var $field = $('#' + fieldName);

    //     if ($field.length !== 0) {
    //         var errorMessage = "";
    //         if (fieldName === "redirect_uris") {
    //             errorMessage = messages["ERR_" + fieldName.toUpperCase()];
    //         }
             
    //         // Set the field with a red outline to indicate it has an error
    //         $field.addClass("tool_field_error");
    //         // Add red exclamation to the end of the field's label
    //         var errExclamation = $('<img id="' + fieldName + '_show_error" src="../../WEB-CONTENT/common/images/errorExclamation.svg" class="tool_err_img" data-externalizedStringAlt="" aria-hidden=true>');
    //         errExclamation.insertAfter($field.siblings('label.tool_modal_body_field_label'));
    //         // Add red error message underneath the field
    //         var errMessage = $('<div id="' + fieldName + '_err_message" class="tool_err_msg">' + errorMessage + '</div>');
    //         $field.parent().append(errMessage);
    //     }
    // };

    var __reshowAddEditDialog = function() {
         // Close other dialogs
         $(".tool_modal_container").addClass('hidden');
 
         // Show the existing Add/Edit dialog
        $('#add_edit_client_modal').removeClass('hidden');
        $("#addEditCancel").get(0).focus();
    };

    var setupNewClientDialog = function() {
        $('#add_edit_client_modal .tool_modal_title').text(messages.MODAL_REGISTER_TITLE);

        __setFieldDefaults();

        // Show the Client ID field in case they want to create their own.
        $('#client_id').closest('.tool_modal_body_field').removeClass('hidden');

        // For the client_secret, hide the regenerate instructions.   
        $('#regen_client_secret_dir').addClass('hidden');

        // Enable appropriate action buttons on add/edit modal dialog
        $('#add_edit_client_modal .tool_modal_update_button').addClass('hidden');
        $('#add_edit_client_modal .tool_modal_register_button').removeClass('hidden');

        utils.stopProcessingSpinner();
        $("#add_edit_client_modal").removeClass("hidden");
        $('#add_edit_client_modal .tool_modal_body_field_input').closest('.tool_modal_body_field:not(.hidden)').first().find('.tool_modal_body_field_input').focus();
        __setupBidiDisplay();
    };

    var setupEditClientDialog = function(response) {
        $('#add_edit_client_modal .tool_modal_title').text(messages.MODAL_EDIT_TITLE);

        __clearModalBodyFields();

        // Fill in the dialog with the current values of the OAuth client
        $('#add_edit_client_modal .tool_modal_body_field_input').each(function() {
            var id = this.id;
            var inputVal = response[id] ? response[id] : "";
            $("input#" + id).val(inputVal);
        });

        $("#add_edit_client_modal .tool_modal_array .tool_modal_table").each(function() {
            var $table = $(this).find('tbody');
            var $lastRow = $table.find('tr').last();
            var id = this.id;
            var arrVals = response[id] ? response[id] : [];

            for (var arrX=0; arrX<arrVals.length; arrX++) {
                // For each value returned, create an input for the value in the Array table.
                var row = __createArrayEntry(arrVals[arrX]);
                var $row = $(row);
                $lastRow.before($row);
                __initArrayRow($row);
            }
        });

        $("#add_edit_client_modal .tool_multiSelect_listbox").each(function() {
            var $dropDownList = $(this).find('.tool_multiSelect_dropdown');
            var id = this.id;
            var listboxVals = response[id] ? response[id] : [];

            for (var listboxX=0; listboxX<listboxVals.length; listboxX++) {
                // For each value returned, find its corresponding checkbox and select it.
                var $checkbox = $dropDownList.find("input[type='checkbox'][value='" + listboxVals[listboxX] + "']");
                $checkbox.prop('checked', true).attr('aria-checked', true).closest('.tool_multiSelect_menuItem').attr('aria-selected', true);
            }
            __updateSelectionValues($dropDownList);
        });

        $("#add_edit_client_modal .tool_modal_radio_button_group").each(function() {
            var $rbGroup = $(this);
            var id = this.id;
            var rbGroupVal = response[id] ? response[id] : "";

            if (rbGroupVal) {
                $rbGroup.find("input[type='radio'][name='" + id + "'][value='" + rbGroupVal + "']").click();  
            }
        });

        $("#add_edit_client_modal .tool_modal_boolean").each(function() {
            var $boolean = $(this);
            var id = this.id;
            // Get value.  If not specified, set to the default value.
            var booleanVal = response[id] ? response[id] : ($boolean.find(".tool_modal_radio_button_fieldset input[data-default='true']").val() === 'true');

            $boolean.find("input[type='radio'][name='" + id + "'][value='" + booleanVal + "']").click();
        });

        $('.tool_single_checkbox').each(function() {
            var $checkbox = $(this).find("input[type='checkbox']");
            var id = this.id;
            var fieldVal = response[id] ? response[id] : "";
            var checkboxVal = $checkbox.val();

            if (fieldVal === checkboxVal) {
                // Select the checkbox
                $(this).click();
            }
        });

        // Hide the Client ID field since it cannot be changed.
        $('#client_id').closest('.tool_modal_body_field').addClass('hidden');

        // Show the Regenerate client secret directions during update processing.
        // Prefill the field with a '*' to indicate no change to the client secret.    
        $('#regen_client_secret_dir').removeClass('hidden');
        $('#cient_secret').val('*');
        
        // Enable appropriate action buttons on add/edit modal dialog
        $('#add_edit_client_modal .tool_modal_update_button').removeClass('hidden');
        $('#add_edit_client_modal .tool_modal_register_button').addClass('hidden');

        __setupBidiDisplay();
        utils.stopProcessingSpinner();
        $("#add_edit_client_modal").removeClass("hidden");
        $('#add_edit_client_modal .tool_modal_body_field_input').closest('.tool_modal_body_field:not(.hidden)').first().find('.tool_modal_body_field_input').focus();
    };

    return {
        init: init,
        setupEditClientDialog: setupEditClientDialog,
        setupNewClientDialog: setupNewClientDialog
    };

})();