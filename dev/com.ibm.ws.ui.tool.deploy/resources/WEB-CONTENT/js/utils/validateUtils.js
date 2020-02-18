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

var validateUtils = (function() {
    "use strict";

    // If false is passed into inputValid parameter because of specific cases where the input isn't valid, then it will default to disabling the deploy button
    var __validate = function(inputValid, showError) {
        var readyToDeploy = (inputValid !== undefined) ? inputValid : true;
        var reviewContent = $("#reviewContent");
        reviewContent.empty();

        // Check for input parameters except for security card
        var inputs = $(".inputVariablesContainer input");
        for(var i = 0; i < inputs.length; i++) {
            var input = inputs.get(i);
            if($(input).is(":visible")){
              if (showError) {
                  if (!__validateInputField(input, showError)) {
                      readyToDeploy = false;
                      break;
                  }
              } else {
                  // factor in an existing input validation error
                  if((!input.placeholder && !input.value) || __checkIfInputHasError(input.id)) {
                      readyToDeploy = false;
                      break;
                  } else {
                      // need to clear any error that may be put there by earlier validation
                      __changeFieldWarningLabel(input.id, false);
                      __removeInputErrorMessage(input.id);
                  }
              }
            }
        }

        if(hostUtils.getSelectedHosts().length === 0){
            readyToDeploy = false;
        }

        // Check security credentials for mistakes
        // Note: It is important to put __validateSecurityInputs call first. Otherwise, if the code is
        // valid = valid && __validateSecurityInputs(handleRequiredField);
        // _validateSecurityInputs will not be even called if valid is set to false by the earlier codes
        // to validate the regular input parameters.
        readyToDeploy = __validateSecurityInputs(showError) && readyToDeploy;

//        var securityInputs = $("#securityInputContainer input, #securityInputConfirmContainer input");
//        if(readyToDeploy && securityInputs.length > 0){
//            for(var i = 0; i<securityInputs.length; i++){
//                var input = securityInputs.get(i);
//                if(!input.value) {
//                    readyToDeploy = false;
//                    break;
//                }
//            }
//        }

        var ariaLabel = "";

        // Update review message and deploy button enablement
        if(readyToDeploy) {
            $("#deployButton").removeAttr("disabled");
            $("#deployButton").attr("aria-disabled", "false");
            $("#deployButton").prop("tabindex", "0");
            $("#deployButton").off('keydown').on("keydown", function(e) {
                if(e.which === 13){ //Enter key
                    $('#deployButton').click();
                }
            });
            $("#reviewComplete").removeClass("hidden");

            var readyToDeployMessage = "";

            var reviewComplete = $("#reviewComplete");
            reviewComplete.empty();

            // Obtain selected rule
            var selectedRule = ruleSelectUtils.getSelectedRule();

            switch(selectedRule.id){
            case "Node.js Server Rule":
                readyToDeployMessage = utils.formatString(messages.READY_TO_DEPLOY_SERVER, ["<span style='font-family: helvneuebold; color: #777677'>" + messages.READY_FOR_DEPLOYMENT + "</span>"]);
                ariaLabel = utils.formatString(messages.READY_TO_DEPLOY_SERVER, [messages.READY_FOR_DEPLOYMENT]);
                break;
            case "Liberty Docker Rule":
                readyToDeployMessage = utils.formatString(messages.READY_TO_DEPLOY_DOCKER, ["<span style='font-family: helvneuebold; color: #777677'>" + messages.READY_FOR_DEPLOYMENT + "</span>"]);
                ariaLabel = utils.formatString(messages.READY_TO_DEPLOY_DOCKER, [messages.READY_FOR_DEPLOYMENT]);
                break;
            default:
                readyToDeployMessage = utils.formatString(messages.READY_TO_DEPLOY, ["<span style='font-family: helvneuebold; color: #777677'>" + messages.READY_FOR_DEPLOYMENT_CAPS + "</span>"]);
                ariaLabel = utils.formatString(messages.READY_TO_DEPLOY, [messages.READY_FOR_DEPLOYMENT_CAPS]);
            }
            $("#review").attr("aria-label", ariaLabel);
            $("#reviewComplete").html(readyToDeployMessage);
        }
        else {
            ariaLabel = utils.formatString(messages.REVIEW_AND_DEPLOY_MESSAGE, [messages.REVIEW_AND_DEPLOY_INCOMPLETE_FIELDS]);
            $("#review").attr("aria-label", ariaLabel);
            var messageContent = utils.formatString(messages.REVIEW_AND_DEPLOY_MESSAGE, ["<a href=\"#\" id=\"showMissingParameters\" class='link'>" + messages.REVIEW_AND_DEPLOY_INCOMPLETE_FIELDS + "</a>"]);
            var message = $("<div>" + messageContent + "</div>");
            reviewContent.append(message);

            $("#deployButton").attr("disabled", "disabled");
            $("#deployButton").attr("aria-disabled", "true");
            $("#reviewComplete").addClass("hidden");
        }
    };

    /*
     * Checks whether the container name entered is a valid Docker container name
       Return: {boolean} true if valid / false if not valid
     */
    var __validateContainerName = function(inputVariable){
      var valid = false;
      var inputValue = $("#" + inputVariable.id).val();

      // Remove existing errors if there is no value entered.
      if(!inputValue){
        __removeInputErrorMessage(inputVariable.id);
      }
      else{
        // Validate whether this is a valid Docker container based on Docker's own guidelines.
        valid = /^[a-zA-Z0-9][a-zA-Z0-9_.-]*$/.test(inputValue);
        if(valid){
          __removeInputErrorMessage(inputVariable.id);
        }
        else{
          __addInputErrorMessage(inputVariable.id, messages.DOCKER_INVALID_CONTAINER__NAME_ERROR);
        }
      }
      return valid;
    };

    /*
     * Checks the Security card for errors and handle them if present by displaying the right message to the user
     * Return: true if no errors are present
     *            false if there is an error
     */
    var __validateSecurityInputs = function(showError){
        var valid = true;
        var securityInputs = $("#securityInputContainer input");
        for(var i = 0; i < securityInputs.length; i++) {
            var input = securityInputs.get(i);
             if (!__validateInputField(input, showError)) {
                    valid = false;
             }
        }

        return valid;
    };

    /*
     * Validate one specific input parameter
     */
    var __validateInputField = function(input, showError) {
        // perform input validation based on its type. Optionally handle error display if requested.
        var typeObject = string.retrieveObjectForType(input);
        var missingInput = typeObject.isEmpty();
        var valid = true;
        if (!missingInput || typeObject instanceof password.passwordType) {
            if(typeObject.inputVariable.name === "containerName"){
              valid = __validateContainerName(typeObject.inputVariable);
            }
            else{
              valid = typeObject.validate(showError);
            }
        }

        if (!(typeObject instanceof password.passwordType)) {
            if (showError) {
                if (valid && !missingInput) {
                    __changeFieldWarningLabel(input.id, false);
                    __removeInputErrorMessage(input.id);
                }

                if (!valid || missingInput) {
                    // add warning label and *
                    __changeFieldWarningLabel(input.id, true);
                }
            }
        }

//        console.log("return ", valid && !missingInput);
        return valid && !missingInput;
    };

    var __changeFieldWarningLabel = function(id, addWarning) {
        var label = $("#" + id +"_inputDiv");
        if (addWarning) {
            label.addClass("inputError");
        } else {
            label.removeClass("inputError");
        }
    };

    var __checkIfInputHasError = function(idForError) {
        if ($("#" + idForError + "_inputDiv").next('p').length !== 0) {
            return true;
        }
        return false;
    };

    var __removeInputErrorMessage = function(idForError) {
        if ($("#" + idForError + "_inputDiv").next('p').length !== 0) {
            $("#" + idForError + "_inputDiv").next('p').remove();
        }

        $("#" + idForError + "_inputDiv").removeClass("inputWithError");
        $("label[id='" + idForError + "_label']").removeClass("labelWithInputError");

        if($("#" + idForError + "_description").html()){
            $("#" + idForError + "_description").removeClass("parametersInputDescriptionWithError");
        }

        __changeFieldWarningLabel(idForError, false);
    };

    var __addInputErrorMessage = function(idForError, errorMsg) {
        __removeInputErrorMessage(idForError);

        $("#" + idForError + "_inputDiv").after("<p id=" + idForError + "_paragraph>" + errorMsg + "</p>");
        $("#" + idForError + "_inputDiv").addClass("inputWithError");

        if($("#" + idForError + "_description").html()){
            $("#" + idForError + "_description").addClass("parametersInputDescriptionWithError");
        }
        else{
            $("#" + idForError + "_paragraph").addClass("errorWithNoDescription");
        }

        __changeFieldWarningLabel(idForError, true);
    };

    return {
        validate: __validate,
        validateContainerName: __validateContainerName,
        addInputErrorMessage: __addInputErrorMessage,
        changeFieldWarningLabel: __changeFieldWarningLabel,
        checkIfInputHasError: __checkIfInputHasError,
        removeInputErrorMessage: __removeInputErrorMessage,
        validateInputField: __validateInputField
    };

})();
