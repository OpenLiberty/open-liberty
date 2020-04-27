/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

goog.require("inputType");

var password = (function() {
    "use strict";

    var PasswordType = function(inputVariable) {
        string.stringType.call(this, inputVariable);
        this.confirmPasswordId = idUtils.getInputPasswordConfirmationId(this.id);
    };

    PasswordType.prototype = $.extend ({
        getConfirmPasswordLabel: function() {
            var confirmPasswordLabelString;
            //console.log("PasswordType.prototype confirmPasswordLabel is called for id " + this.id);
            if (this.inputVariable.group) {
                confirmPasswordLabelString = utils.formatString(messages.CONFIRM_GROUP_GENERIC_PASSWORD, [string.stringType.prototype.getLabelString.call(this),
                                                                                                          this.inputVariable.group]);
            } else {
                confirmPasswordLabelString = utils.formatString(messages.CONFIRM_GENERIC_PASSWORD, [this.getLabelString()]);
            }
            return $("<label id=\"" + this.confirmPasswordId + "_label\" class=\"securityInputLabel\" for=\"" + this.confirmPasswordId + "\">" +  confirmPasswordLabelString + "</label>");
        },

        getConfirmPasswordInput: function() {
            var input = $("<input type=\"password\" id=\"" + this.confirmPasswordId + "\"></input>");
            input.attr("aria-labelledby", this.confirmPasswordId + "_label");
            // handle bidi
            utils.setBidiTextDirection(input);
            return input;
        }
    }, string.stringType.prototype);

    PasswordType.prototype.getLabelString = function() {
        var labelString = string.stringType.prototype.getLabelString.call(this);
        if (this.inputVariable.group) {
            labelString = utils.formatString(messages.GROUP_GENERIC_PASSWORD, [labelString, this.inputVariable.group]);
        }
        return labelString;
    };

    PasswordType.prototype.getLabel = function() {
        return $("<label id=\"" + this.id + "_label\" class=\"securityInputLabel\" for=\"" + this.id + "\">" +  this.getLabelString() + "</label>");
    };

    PasswordType.prototype.getInput = function() {
        var input = string.stringType.prototype.getInput.call(this);
        input.attr("aria-labelledby", this.id + "_label");
        input.attr("type", "password");
        return input;
    };

    PasswordType.prototype.isEmpty = function() {
        return $("input[id='" + this.id + "']")[0].value ? false : true;
    };

    PasswordType.prototype.validate = function(showError) {
        var invalidMsg = null;
        var passwordInput = $("#" + this.id);
        var confirmPasswordInput = $("#" + this.confirmPasswordId);
//        console.log('password value ', passwordInput.val());
//        console.log('confirm password value ', confirmPasswordInput.val());
        var passwordValue = passwordInput.val();
        var confirmPasswordValue = confirmPasswordInput.val();
        if (passwordValue === "" && confirmPasswordValue === "") {
            // take care of case of two empty passwords
            invalidMsg = "";
        } else if (passwordValue !== confirmPasswordValue) {
            invalidMsg = messages.PASSWORDS_DONT_MATCH;
        }

        if (showError) {
            // clear out existing error
            if ($("#" + this.confirmPasswordId + "_inputDiv").next('p').length !== 0) {
                $("#" + this.confirmPasswordId + "_inputDiv").next('p').remove();
            }
            if ($("#" + this.id + "_inputDiv").next('p').length !== 0) {
                $("#" + this.id + "_inputDiv").next('p').remove();
            }
            validateUtils.changeFieldWarningLabel(this.confirmPasswordId, false);
            validateUtils.changeFieldWarningLabel(this.id, false);

            // if empty passwords or not match passwords
            if (invalidMsg !== null) {
                $("#" + this.confirmPasswordId + "_inputDiv").after("<p id=" + this.confirmPasswordId + "_paragraph>" + invalidMsg + "</p>");
                validateUtils.changeFieldWarningLabel(this.confirmPasswordId, true);

                if (passwordValue === "") {
                    // flag missing password
                    validateUtils.changeFieldWarningLabel(this.id, true);
                }
            }
        }
        return (invalidMsg === null);
    };

    PasswordType.prototype.addInputListener = function() {
        //string.stringType.prototype.addInputListener.call(this);

        $("#" + this.id).on("input", function(event) {
            var valid = validateUtils.validateInputField(this, true);
            validateUtils.validate(valid);
        });

        $("#" + this.confirmPasswordId).on("input", function(event) {
            var valid = validateUtils.validateInputField(this, true);
            validateUtils.validate(valid);
        });
    };

    var __create = function(inputVariable) {
        var newPassword = new PasswordType(inputVariable);
        return newPassword;
    };

    return {
        /*
         * code usage example:
         *   if (inputVariable.type === "password") {
         *     typeObject = password.create(inputVariable);
         *   } else {
         *     typeObject = string.create(inputVariable);
         *   }
         *   inputVariablesContainer.append(typeObject.getLabel());
         *   var input = typeObject.getInput();
         *   // Note: it is important that the input is put in the container first before calling
         *   // setObjectForType and addInputListener
         *   inputVariablesContainer.append(input);
         *   string.setObjectForType(input, typeObject);
         *   if (inputVariable.type === "password") {
         *     inputVariablesContainer.append(typeObject.getConfirmPasswordLabel());
         *     var confirmPasswordInput = typeObject.getConfirmPasswordInput();
         *     inputVariablesContainer.append(confirmPasswordInput);
         *     string.setObjectForType(confirmPasswordInput, typeObject);
         *   }
         *   typeObject.addInputListener();
         *
         *   To validate:
         *    var typeObject = string.retrieveObjectForType(input);
         *    var missingInput = typeObject.isEmpty(input.id);
         *    if (!missingInput) {
         *      valid = typeObject.validate();
         *    }
         */
        passwordType: PasswordType,
        create: __create
    };

})();
