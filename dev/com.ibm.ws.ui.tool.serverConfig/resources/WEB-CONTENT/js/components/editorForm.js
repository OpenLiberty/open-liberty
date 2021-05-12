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

var editorForm = (function() {
    "use strict";

    var renderEditorForm = function(element) {

        // Reset customizations
        customizationManager.reset();

        // Flag used for unrecognized elements
        var elementRecognized = false;

        // Obtain ready only status
        var readOnly = editor.isFileReadOnly();

        // Obtain controls panel
        var editorForm = $("#editorForm");

        // Clear control panel content
        editorForm.empty();

        // Obtain element information
        var elementPath = xmlUtils.getElementPath(element);
        var schemaElement = schemaUtils.getElementFromPath(editor.getSchemaDocumentRootElement(), elementPath);
        var deleteButtonEnablement;
        if(schemaElement !== null && schemaElement !== undefined) {
            var elementDeclaration = schemaUtils.resolveElementDeclaration(schemaElement);
            if(elementDeclaration !== null && elementDeclaration !== undefined) {
                elementRecognized = true;

                // Add element name (as title)
                var elementLabel = element.nodeName;
                if(settings.enhanceLabels) {
                    elementLabel = schemaUtils.getLabel(schemaElement);
                    if(elementLabel === null || elementLabel === undefined) {
                        elementLabel = schemaUtils.getLabel(elementDeclaration);
                    }
                    if(elementLabel === null || elementLabel === undefined) {
                        elementLabel = stringUtils.capitalizeString(element.nodeName);
                    }
                }
                editorForm.append("<div class=\"elementTitle typography_subTitle\">" + elementLabel + "</div>");

                // Add element documentation (if present)
                if(settings.showDescriptions) {
                    var elementDocumentation = null;
                    if(elementPath.length === 1) {
                        elementDocumentation = editorMessages.SERVER_DESCRIPTION;
                    } else {
                        elementDocumentation = schemaUtils.getDocumentation(element);
                    }
                    if(elementDocumentation === null || elementDocumentation === undefined) {
                        elementDocumentation = schemaUtils.getDocumentation(elementDeclaration);
                    }

                    if(elementDocumentation !== null && elementDocumentation !== undefined) {
                        editorForm.append("<div class=\"elementDescription\">" + elementDocumentation + "</div>");
                    }
                }

                // Add "Add child" button (enabled if element supports children)
                var addChildButtonEnablement = (!readOnly && schemaUtils.hasChildElements(elementDeclaration))? "" : "tabindex=\"-1\" disabled=\"disabled\" aria-disabled=\"true\"";
                editorForm.append("<a href=\"#\" draggable=\"false\" role=\"button\" " + addChildButtonEnablement + " id=\"addChildButton\" class=\"btn btn-success\">" + editorMessages.ADD_CHILD + "</a>");

                // Add "Remove" button (enabled if element is not top level)
                deleteButtonEnablement = (!readOnly && element.ownerDocument.documentElement !== element)? "" : "tabindex=\"-1\" disabled=\"disabled\" aria-disabled=\"true\"";

                editorForm.append("<a href=\"#\" draggable=\"false\" role=\"button\" " + deleteButtonEnablement + " id=\"removeButton\" class=\"btn btn-default\">" + editorMessages.REMOVE + "</a>");

                // Add "Test" button only if the element is supported by the validator APIs
                var validatorList = editor.getValidatorList();
                if(validatorList && $.inArray(element.nodeName, validatorList) !== -1) {
                    editorForm.append("<a draggable=\"false\" role=\"button\" id=\"testButton\" class=\"btn btn-default\">" + editorMessages.TEST + "</a>");
                }

                // Create form
                var form = $("<form id=\"editorFormControl\" aria-labelledby=\"editorFormLabel\" role=\"form\" onsubmit=\"return false;\" />");

                // Default attribute group
                var defaultAttributeGroup = $("<div class=\"attributeGroup\"></div>");
                form.append(defaultAttributeGroup);

                // Create attribute groups (when applicable)
                var groupDeclarations = schemaUtils.getGroupDeclarations(elementDeclaration);
                groupDeclarations.forEach(function(groupDeclaration) {
                    var attributeGroup = $("<div id=\"attribute_group_" + groupDeclaration.id + "\" class=\"attributeGroup\"></div>");
                    var attributeGroupHeader = $("<div class=\"attributeGroupHeader\"></div>");
                    attributeGroupHeader.append("<div class=\"attributeGroupTitle typography_subTitle\">" + groupDeclaration.label + "</div>");
                    if(settings.showDescriptions) {
                        attributeGroupHeader.append("<div class=\"attributeGroupDescription\">" + groupDeclaration.description + "</div>");
                    }
                    groupDeclaration.attributeGroupElement = attributeGroup;
                    attributeGroup.append(attributeGroupHeader);
                    form.append(attributeGroup);
                });


                // Process element text value (if present)
                if(schemaUtils.elementContainsText(elementDeclaration)) {

                    var clearElementAction = $("<a role=\"button\" class=\"form-control-feedback clearInputFeedbackAction\" href=\"#\" draggable=\"false\" title=\"" + editorMessages.CLEAR + "\"><img src=\"img/entryfield-clear-D.png\" alt=\"" + editorMessages.CLEAR + "\"></a>");

                    var controlContainer = $("<div style=\"position:relative\" />");

                    var formGroup = $("<div class=\"form-group has-feedback\"/>");
                    var controlLabel = $("<label for=\"element_text_value\">" + editorMessages.VALUE + "</label>");
                    var control = $("<input id=\"element_text_value\" type=\"text\" spellcheck=\"false\" autocorrect=\"off\" autocapitalize=\"off\" class=\"form-control\" />");
                    var elementValue = element.textContent;
                    if(elementValue !== null && elementValue !== undefined && elementValue.length > 0) {
                        control.attr("value", elementValue);
                    } else {
                        clearElementAction.addClass("hidden");
                    }

                    if(readOnly) {
                        control.attr("disabled", "disabled");
                        control.attr("aria-disabled", true);
                        clearElementAction.addClass("hidden");
                    }

                    control.attr("placeholder", editorMessages.EMPTY_STRING_ELEMENT_VALUE);

                    // Obtain type
                    var elementType = schemaUtils.getDataType(schemaElement);

                    // Associate the element type to the control
                    control.data("dataType", elementType);

                    // Handle bidi
                    if(globalization.isBidiEnabled()) {
                        if(globalization.dataTypeRequiresSpecialHandling(elementType)) {
                            // TODO: special handling
                            control[0].value = globalization.createBidiDisplayString(control[0].value, "FILE_PATH");
                            $("#editorForm").on("keyup", control, function(event) {
                                control[0].value = globalization.createBidiDisplayString(control[0].value, "FILE_PATH");
                            });
                        } else {
                            var dirValue = globalization.getBidiTextDirection();
                            if(dirValue === "contextual") {
                                dirValue = globalization.obtainContextualDir(elementValue);
                            }
                            control.attr("dir", dirValue);
                        }
                    }

                    controlContainer.append(control);
                    controlContainer.append(clearElementAction);

                    formGroup.append(controlLabel);
                    formGroup.append(controlContainer);
                    defaultAttributeGroup.append(formGroup);
                }

                // Process attributes (if present)
                var attributes = schemaUtils.getAttributes(elementDeclaration);
                attributes.forEach(function(attribute) {

                    // Obtain attribute name
                    var attributeName = attribute.getAttribute("name");

                    // Obtain attribute value
                    var attributeValue = element.getAttribute(attribute.getAttribute("name"));

                    // Obtain attribute label (defaults to capitalized attribute name if no label is specified)
                    var attributeLabel = null;
                    if(settings.enhanceLabels) {
                        attributeLabel = schemaUtils.getLabel(attribute);
                        if(attributeLabel === null || attributeLabel === undefined) {
                            attributeLabel = stringUtils.capitalizeString(attributeName);
                        }
                    } else {
                        attributeLabel = attributeName;
                    }

                    // Append asterisk suffix for required attributes
                    if(schemaUtils.isRequired(attribute)) {
                        attributeLabel+= "<span class=\"requiredFieldSuffix\">*</span>";
                    }

                    // Crate group and label
                    var formGroup = $("<div class=\"form-group has-feedback\"/>");
                    var controlLabel = $("<label for=\"attribute_" + attributeName + "\">" + attributeLabel + "</label>");

                    // Create control
                    var control = $("<input spellcheck=\"false\" autocorrect=\"off\" autocapitalize=\"off\" id=\"attribute_" + attributeName + "\" type=\"text\" class=\"form-control\">");

                    var attributeDataType = schemaUtils.getDataType(attribute);

                    // Associate the attribute type to the control
                    control.data("dataType", attributeDataType);

                    // Associate the attribute name to the control
                    control.data("attributeName", attributeName);

                    // Add placeholder for default value (if present)
                    var defaultValue = schemaUtils.getDefaultValue(attribute);
                    var attributeVariable = schemaUtils.getVariable(attribute);

                    if(defaultValue !== null && defaultValue !== undefined) {
                        if(attributeVariable !== null && attributeVariable !== undefined) {
                            control.data("clearPlaceholder", stringUtils.formatString(editorMessages.DEFAULT_VALUE_PLACEHOLDER_WITH_VARIABLE, [defaultValue, attributeVariable]));
                        } else {
                            control.data("clearPlaceholder", stringUtils.formatString(editorMessages.DEFAULT_VALUE_PLACEHOLDER, [defaultValue]));
                        }
                    } else if(attributeVariable !== null && attributeVariable !== undefined) {
                        control.data("clearPlaceholder", stringUtils.formatString(editorMessages.VARIABLE_VALUE_PLACEHOLDER, [attributeVariable]));
                    } else {
                        control.data("clearPlaceholder", editorMessages.NO_VALUE);
                    }

                    // Initialize value (if present)
                    if(attributeValue !== null && attributeValue !== undefined) {
                        control.val(attributeValue);
                        control.attr("placeholder", editorMessages.EMPTY_STRING_ATTRIBUTE_VALUE);
                    } else {
                        control.attr("placeholder", control.data("clearPlaceholder"));
                    }

                    // Handle bidi
                    if(globalization.isBidiEnabled()) {
                        if(globalization.dataTypeRequiresSpecialHandling(attributeDataType)) {
                            //TODO: special handling
                            control[0].value = globalization.createBidiDisplayString(control[0].value, "FILE_PATH");
                            $("#editorForm").on("keyup", control, function(event) {
                                control[0].value = globalization.createBidiDisplayString(control[0].value, "FILE_PATH");
                            });
                        } else {
                            var dirValue = globalization.getBidiTextDirection();
                            if(dirValue === "contextual") {
                                dirValue = globalization.obtainContextualDir(attributeValue);
                            }
                            control.attr("dir", dirValue);
                        }
                    }


                    var clearAttributeAction = $("<a role=\"button\" class=\"form-control-feedback clearInputFeedbackAction\" href=\"#\" draggable=\"false\" title=\"" + editorMessages.CLEAR + "\"><img src=\"img/entryfield-clear-D.png\" alt=\"" + editorMessages.CLEAR + "\"></a>");
                    if(attributeValue === null || attributeValue === undefined) {
                        clearAttributeAction.addClass("hidden");
                    }

                    var controlContainer = $("<div style=\"position:relative\" />");

                    controlContainer.append(control);

                    if(readOnly) {
                        control.attr("readonly", true);
                    } else {
                        controlContainer.append(clearAttributeAction);
                    }

                    // Add select button for enumerations (if present)
                    var inputGroup = null;
                    if(!readOnly) {
                        if(schemaUtils.hasPossibleValues(attribute)) {
                            inputGroup = $("<div class=\"input-group\">");
                            var button = $("<a href=\"#\" draggable=\"false\" class=\"btn btn-default editorFormFieldButton dialogEnumerationSelectButton\" role=\"button\">" + editorMessages.SELECT + "</a>");
                            var span = $("<span class=\"input-group-btn\"></span>");
                            span.append(button);
                            inputGroup.append(controlContainer);
                            inputGroup.append(span);
                            button.data("inputControl", control.get(0));
                            button.data("attribute", attribute);
                            control.data("possibleValues", schemaUtils.getPossibleValues(attribute));
                        }
                    }

                    formGroup.append(controlLabel);
                    if(inputGroup !== null && inputGroup !== undefined) {
                        formGroup.append(inputGroup);
                    } else {
                        formGroup.append(controlContainer);
                    }

                    // Add documentation (if present)
                    if(settings.showDescriptions) {
                        var attributeDocumentation = schemaUtils.getDocumentation(attribute);
                        if(attributeDocumentation !== null && attributeDocumentation !== undefined) {
                            var controlError = $("<span class=\"help-block input-error-message input-error-message-inactive\">" + "</span>");
                            formGroup.append(controlError);

                            var controlDocumentation = $("<span class=\"help-block\">" + attributeDocumentation + "</span>");
                            formGroup.append(controlDocumentation);
                        }
                    }

                    // Obtain attribute group
                    var attributeGroup = schemaUtils.getGroup(attribute);

                    // Add attribute to group or form
                    if(attributeGroup !== null && attributeGroup !== undefined) {
                        // Add form to attribute group
                        var targetGroup = $.grep(groupDeclarations, function(group) {return group.id === attributeGroup;});
                        targetGroup[0].attributeGroupElement.append(formGroup);
                    } else {
                        // Add form group to form
                        defaultAttributeGroup.append(formGroup);
                    }

                    // Validate
                    validateInputControl(control);
                });

                // Add form to controls panel
                editorForm.append(form);
            }
        }

        if(!elementRecognized) {

            // Element label
            editorForm.append("<div class=\"typography_subTitle\">" + element.nodeName + "</div>");

            // Add "Add" button
            editorForm.append("<a href=\"#\" role=\"button\" draggable=\"false\" tabindex=\"-1\" disabled=\"disabled\" aria-disabled=\"true\" id=\"addChildButton\" class=\"btn btn-success\">" + editorMessages.ADD_CHILD + "</a>");

            // Add "Remove" button
            deleteButtonEnablement = (!readOnly)? "" : "tabindex=\"-1\" disabled=\"disabled\" aria-disabled=\"true\"";
            editorForm.append("<a href=\"#\" draggable=\"false\" role=\"button\" " + deleteButtonEnablement + " id=\"removeButton\" class=\"btn btn-default\">" + editorMessages.REMOVE + "</a>");

            // Add unrecognized element message
            var sourcePaneLink = "<a href=\"#\" id=\"unrecognizedElementSwitchToSourceLink\" draggable=\"false\">" + editorMessages.SOURCE_PANE + "</a>";
            var errorMessage = stringUtils.formatString(editorMessages.UNRECOGNIZED_ELEMENT, [element.nodeName, sourcePaneLink]);
            editorForm.append("<br><br><p>" + errorMessage + "</p>");
        }

        customizationManager.applyEditorFormCustomization(element);
    };



    var validateInputControl = function(inputJQueryObject, onlyFixValues) {
        var validation = validateInputControlValue(inputJQueryObject);
        var errorMessageControl = $(".input-error-message", inputJQueryObject.parent().parent());
        if(errorMessageControl.length === 0) {
            errorMessageControl = $(".input-error-message", inputJQueryObject.parent().parent().parent());
        }
        if(validation !== null && validation !== undefined) {
            if(!onlyFixValues) {
                inputJQueryObject.css("border-color", "#a91024");
                errorMessageControl.text(validation);
                errorMessageControl.removeClass("input-error-message-inactive");
            }
        } else {
            inputJQueryObject.css("border-color", "");
            errorMessageControl.text("");
            errorMessageControl.addClass("input-error-message-inactive");
        }
    };


    var validateInputControlValue = function(inputJQueryObject) {
        var value = inputJQueryObject.val();

        // Check that the value isn't empty
        if(value !== "" || inputJQueryObject.attr("placeholder") === editorMessages.EMPTY_STRING_ATTRIBUTE_VALUE) {

            // Check that there are no variables in the value
            var regExp = new RegExp("\\${.*}");
                if(!regExp.test(value)) {

                var dataType = inputJQueryObject.data("dataType");

                var possibleValues = inputJQueryObject.data("possibleValues");
                var options = [];
                if(possibleValues !== null && possibleValues !== undefined && possibleValues !== undefined) {
                    for(var i = 0; i < possibleValues.length; i++) {
                        options[i] = possibleValues[i][0];
                        if(possibleValues[i][0] === value) {
                            return null;
                        }
                    }
                    if(dataType === "booleanType" || dataType === "xsd:boolean") {
                        return editorMessages.THE_VALUE_SHOULD_BE_A_BOOLEAN;
                    } else {
                        return stringUtils.formatString(editorMessages.THE_VALUE_SHOULD_BE_AMONG_THE_POSSIBLE_OPTIONS, [options.join(", ")]);
                    }

                } else {

                    // Check data type
                    if((dataType === "longType" || dataType === "shortType" || dataType === "intType" || dataType === "intType") && (isNaN(value) || value.trim().length === 0)) {
                        return editorMessages.THE_VALUE_SHOULD_BE_A_NUMBER;
                    }
                }
            }
        }
        return null;
    };


    var clear = function() {
        $("#editorForm").empty();
    };


    $(document).ready(function() {

        var pendingValidation = null;

        // Handle value changes
        $("#editorForm").on("input", function(event) {
            var skipDocumentDirtyMark = false;
            var selectedElement = $("#editorTree .active").data("element");
            var value = event.target.value;
            var target = $(event.target);

            // Handle bidi
            if(globalization.isBidiEnabled()) {
                if(globalization.dataTypeRequiresSpecialHandling(target.data("dataType"))) {
                    // TODO: special handling
                    event.target.value = globalization.createBidiDisplayString(value, "FILE_PATH");
                    $("#editorForm").on("keyup", target, function(event) {
                        event.target.value = globalization.createBidiDisplayString(value, "FILE_PATH");
                    });
                } else if(globalization.getBidiTextDirection() === "contextual") {
                    target.attr("dir", globalization.obtainContextualDir(value));
                }
            }

            var attributeName = target.data("attributeName");
            if(attributeName !== null && attributeName !== undefined) {
                selectedElement.setAttribute(attributeName, value);
                if(value.length > 0) {
                    target.attr("placeholder", editorMessages.EMPTY_STRING_ATTRIBUTE_VALUE);
                } else {
                    if(target.attr("placeholder") === target.data("clearPlaceholder")) {
                        if(selectedElement.hasAttribute(attributeName)) {
                            selectedElement.removeAttribute(attributeName);
                        } else {
                            skipDocumentDirtyMark = true;
                        }
                    }
                }
            } else {
                // Clear current value
                while (selectedElement.firstChild) {
                    selectedElement.removeChild(selectedElement.firstChild);
                }
                // Assign new value
                var newTextNode = selectedElement.ownerDocument.createTextNode(value);
                selectedElement.appendChild(newTextNode);
            }

            // Update tree
            if(attributeName === null || attributeName === undefined || attributeName === "id" || attributeName === "name" || attributeName === "location") {
                editorTree.updateTreeNodeSuffix();
            }

            // Show/hide clear action
            var clearControl = target.next();
            if(value.length > 0) {
                if(clearControl.is(':animated')) {
                    clearControl.stop();
                    clearControl.css("opacity", "1");
                    clearControl.removeClass("hidden");
                } else if(clearControl.hasClass("hidden")) {
                    clearControl.css("opacity", "0");
                    clearControl.removeClass("hidden");
                    clearControl.animate({
                        opacity: 1
                    }, 2 * editor.ANIMATION_TIME_UNIT);
                }
            } else if(attributeName === null || attributeName === undefined) {
                if(!clearControl.hasClass("hidden")) {
                    clearControl.stop();
                    clearControl.animate({
                        opacity: 0
                    }, 200, function() {
                        clearControl.addClass("hidden");
                    });
                }
            }

            // Update control validation
            validateInputControl(target, true);

            // Mark document as dirty
            if(!skipDocumentDirtyMark) {
                editor.markDocumentAsDirty();
            }

        });

        // Handle validation on focus out
        $("#editorForm").on("blur", "input[type='text']", function(event) {

            // Update control validation
            setTimeout(function() {
                var target = $(event.currentTarget);
                var nextFocus = $(document.activeElement);
                if(target.next().is(nextFocus)) {
                    pendingValidation = target;
                } else {
                    validateInputControl(target);
                }
            }, 25);
        });

        $("#editorForm").on("blur", "a.clearInputFeedbackAction", function(event) {
            if(pendingValidation !== null && pendingValidation !== undefined) {
                validateInputControl(pendingValidation);
                pendingValidation = null;
            }
        });


        // Handle input clear
        $("#editorForm").on("click", "a.clearInputFeedbackAction", function(event) {
            event.preventDefault();
            var control = $(event.currentTarget).prev();
            control.attr("placeholder", control.data("clearPlaceholder"));
            control.val("");
            control.trigger("input");
            $(event.currentTarget).addClass("hidden");
        });

        // Handle add child button
        $("#editorForm").on("click", "#addChildButton", function(event) {
            event.preventDefault();
            // Don't display the modal if the button (actually an anchor) is disabled
            if ($(event.target).attr('disabled') !== "disabled") {
                $("#dialogAddChildElement").modal("show");
            }
        });

        // Handle remove button
        $("#editorForm").on("click", "#removeButton", function(event) {
            event.preventDefault();
            // Don't display the modal if the button (actually an anchor) is disabled
            if ($(event.target).attr('disabled') !== "disabled") {
                $("#dialogRemoveElement").modal("show");
            }
        });

        // Handle remove button
        $("#editorForm").on("click", "#unrecognizedElementSwitchToSourceLink", function(event) {
            event.preventDefault();
            editor.switchToSourceView();
        });

        // Handle test button
        $("#editorForm").on("click", "#testButton", function(event) {
            event.preventDefault();
            $("#dialogDatasourceValidateElement").modal("show");
        });

    });

    return {
        renderEditorForm: renderEditorForm,
        clear: clear
    };

})();
