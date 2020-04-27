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

goog.provide("inputType");

var string = (function() {
    "use strict";
    var typeObjectKey = "typeObject";

    var StringType = function(inputVariable) {
        this.id = idUtils.getInputId(inputVariable);
        this.inputVariable = inputVariable;
        this.inputValue = null;
    };
    StringType.prototype = {
        getLabelString: function() {
            var labelString = this.inputVariable.name;
            if (this.inputVariable.displayName) {
                labelString = this.inputVariable.displayName;
            }
            return labelString;
        },
        getLabel: function() {
            return $("<label id=\"" + this.id + "_label\" class=\"parametersInputLabel\" for=\"" + this.id + "\">" +  this.getLabelString() + "</label>");
        },
        getInput: function() {
            //console.log("StringType.prototype input is called for id " + this.id);
            var input = $("<input id=\"" + this.id + "\" class=\"inputVariable\" type=\"text\" autocomplete=\"off\">");
            input.attr("aria-label", this.getLabelString());
            if(this.inputVariable.description && this.inputVariable.description !== ""){
                input.attr("aria-label", input.attr("aria-label") + " " + this.inputVariable.description);
            }
            if(this.inputVariable.defaultValue) {
                var placeHolderValue = this.inputVariable.defaultValue;
                if (globalization.isBidiEnabled()) {
                    placeHolderValue = globalization.createBidiDisplayString(this.inputVariable.defaultValue, "FILE_PATH");
                }
                // all place holder default should be structured text
                input.attr("placeholder", placeHolderValue);
            }
            // text direction should not apply to file and filePath
            if (!globalization.dataTypeRequiresSpecialHandling(this.inputVariable.type)) {
                // Handle bidi
                utils.setBidiTextDirection(input);
            }
            return input;
        },
        getDescription: function() {
            var description = $("<span id='" + this.id + "_description' class='parametersInputDescription'></span>")[0];

            if(this.inputVariable.description && this.inputVariable.description !== ""){
                description.innerHTML = this.inputVariable.description;
                description.title = this.inputVariable.description;
            }
            else{
                $(description).addClass("hidden");
            }
            utils.setBidiTextDirection(description, true);
            return description;
        },
        isEmpty: function() {
            var isEmpty = $("input[id='" + this.id + "']")[0].value ? false : true;
            var hasPlaceHolder = $("input[id='" + this.id + "']")[0].placeholder ? true : false;
            return isEmpty && !hasPlaceHolder;
        },
        validate: function() {
            var valid = true;
            if (this.inputVariable.type === "filePath") {
                // if filePath is tagged as error thru fileUpload and user hasn't changed the value, still flagged as invalid
                if ($("input[id='" + this.id + "']").next('p').length !== 0) {
                    //console.log("---------- filePath with error already flagged, return not valid");
                    valid = false;
                }
            }
            return valid;
        },
        addInputListener: function() {
            var me = this;
            if (globalization.dataTypeRequiresSpecialHandling(this.inputVariable.type)) {
                // this is the path for file and filePath
                $("#" + this.id).on("input", function(event) {
                    var val = $("#" + this.id).val();
                    if (val !== "") {
                      me.clearFieldButton.show();
                    } else {
                      me.clearFieldButton.hide();
                    }

                    // clear out existing error first
                    validateUtils.removeInputErrorMessage(this.id);
                    // validate only if actual input is provided vs through browse/drag and drop
                    //validateUtils.validateInputField(this, false);
                    validateUtils.validate();
                });
                if (globalization.isBidiEnabled()) {
                    // special handling is required for filePath
                   __displayBidiString(this);
                    $("#" + this.id).on("keyup", function(event) {
                        __displayBidiString(me);
                    });
                }
                // need this event when input is provided thru the browser button and manually trigger
                // a change event
                $("#" + this.id).on("change", function(event) {
                    var val = $("#" + this.id).val();
                    if (val !== "") {
                      me.clearFieldButton.show();
                    } else {
                      me.clearFieldButton.hide();
                    }
                    //clear out existing error first
                    validateUtils.removeInputErrorMessage(this.id);
                    if (globalization.isBidiEnabled()) {
                        __displayBidiString(me);
                    }
                    validateUtils.validate();
                });
            } else {
                // put in listener for inputs that need to know whether a manual input has entered
                $("#" + this.id).on("change", function(event) {
                    var val = $("#" + this.id).val();
                    if (val !== "") {
                      me.clearFieldButton.show();
                    } else {
                      me.clearFieldButton.hide();
                    }
                    me.inputValue = $("#" + me.id).val();
                });
                var onInput = function(event){
                  var val = $("#" + this.id).val();
                  if (val !== "") {
                    me.clearFieldButton.show();
                  } else {
                    me.clearFieldButton.hide();
                    me.inputValue = $("#" + me.id).val();
                  }

                  if(me.inputVariable.name === "containerName"){
                    validateUtils.validateContainerName(me);
                  }
                  validateUtils.validate(); // Checks the form inputs to see if the deploy button should be enabled
                };
                $("#" + this.id).on("input", $.proxy(onInput, me)); // Bind closure "me" to onInput
            }
        },
        setDependencyListener: function(){
            var me = this;
            var defaultValue = this.inputVariable.defaultValue;
            if(defaultValue){
                var leftBraceIndex = defaultValue.indexOf('${');
                var rightBraceIndex = defaultValue.indexOf('}');
                var dependencyName;
                if(leftBraceIndex >= 0 && rightBraceIndex >= 0 && rightBraceIndex < defaultValue.length-1){
                    dependencyName = defaultValue.substring(leftBraceIndex + 2, rightBraceIndex);
                    var inputDependencyId = this.id.substring(0, this.id.lastIndexOf("_") + 1) + dependencyName; // Add the dependency using the current group = (group + _inputVariable + _ + dependentId)

                    // Only change field if it is not already set. And check if dependency field has an error warning, if it doesn't then set this field's value when the dependency changes
                    $("#" + inputDependencyId).on("input change", function(event){
                        var dependencyInput = event.currentTarget;
                        var currentValue = $("#" + me.id).val();
                        var dependencyCurrentValue = $("#" + this.id).val(); // Make sure this value isn't blank

                        // if manual input hasn't performed or manual input has reset to display placeholder again
                        // then update the value
                        if(dependencyCurrentValue !== "" &&
                           (me.inputValue === "" || me.inputValue === null)  &&
                           !validateUtils.checkIfInputHasError(this.id)){
                            var substituteValue = defaultValue.substring(rightBraceIndex + 1);
                            var valueToSet = $("#" + this.id).val() + substituteValue;
                            $("#" + me.id).val(valueToSet);
                            $("#" + me.id).trigger("input"); // Trigger input so the 'x' buttons to clear the inputs appear
                            $(dependencyInput).focus(); // Reset focus back to the original input changed
                        } else if (dependencyCurrentValue === "" && (me.inputValue === "" || me.inputValue === null)) {
                            var myInput = $("#" + me.id);
                            myInput.val("");
                            // Hide the 'x' clear button
                            var clearFieldButton = myInput.siblings(".parameterClearFieldButton");
                            clearFieldButton.hide();
                        }
                    });
                }
            }
        },
        addClearButtonListener: function() {
          var me = this;
          var input = $("#" + me.id);
          var inputDiv = input.parent();
          var anchor = inputDiv.find('a');
          me.clearFieldButton = anchor;

          // Hide the button if the input has no valueToSet
          if(!input.val()){
            anchor.hide();
          }

          // Add listener for the 'X' SVG to clear the input field when they click it
          anchor.on("click", function(event){
            event.preventDefault();
            var input = $("#" + me.id);
            // Clear the file associated with this input if the input type is a file
            if(me.inputVariable.type === "file"){
              fileUtils.removeFile(me.fileBrowserAndUploadId);
            }
            input.val("");
            input.trigger("input");
            me.clearFieldButton.hide();
          });

          anchor.on("keydown", function(event){
            event.stopPropagation();
            var self = event.currentTarget;
            if(event.keyCode === 13){
              self.click();
            }
          });

          inputDiv.on("mouseenter focusin", "a", function(event) {
            event.preventDefault();
            var self = event.currentTarget;
            if(self.children){
              $(self.children[0]).remove();
              var img = imgUtils.getSVGSmallObject('blue-selected');
              img.setAttribute("pointer-events", "none");
              $(self).append(img);
            }
          });

          inputDiv.on("mouseleave focusout", "a", function(event) {
            event.preventDefault();
            var self = event.currentTarget;
            if(self.children){
              $(self.children[0]).remove();
              var img = imgUtils.getSVGSmallObject('gray-selected');
              img.setAttribute("pointer-events", "none");
              $(self).append(img);
            }
          });
        }
    };

    var __displayBidiString = function(me) {
        if (globalization.dataTypeRequiresSpecialHandling(me.inputVariable.type)) {
            var input = $("#" + me.id);
            if (input.val() !== "" && input.val() !== undefined) {
                var bidiDisplayString = globalization.createBidiDisplayString(input.val(), "FILE_PATH");
                input.val(bidiDisplayString);
            }
        }
    };

    var __create = function(inputVariable) {
        var newString = new StringType(inputVariable);
        return newString;
    };

    var __retrieveObjectForType = function(inputDOM) {
        //console.log("__retrieveObjectForType", $("input[id='" + inputDOM.id + "']"));
        return $("input[id='" + inputDOM.id + "']").data(typeObjectKey);
    };

    var __setObjectForType = function(input, typeObject) {
        // Note: input has to be in the DOM before data could be set
        //console.log("__setObjectForType", $("input[id='" + input.attr("id") + "']"));
        $("input[id='" + input.attr("id") + "']").data(typeObjectKey, typeObject);
    };

    return {
        /*
         * code usage example:
         *   typeObject = string.create(inputVariable);
         *   inputVariablesContainer.append(typeObject.getLabel());
         *   var input = typeObject.getInput();
         *   // Note: it is important that the input is put in the container first before calling
         *   // setObjectForType and addInputListener
         *   inputVariablesContainer.append(input);
         *   string.setObjectForType(input, typeObject);
         *   typeObject.addInputListener();
         *
         *   To validate:
         *    var typeObject = string.retrieveObjectForType(input);
         *    var missingInput = typeObject.isEmpty(input.id); // always pass in the id for extended type
         *    if (!missingInput) {
         *      valid = typeObject.validate();
         *    }
         */
        stringType: StringType,
        create: __create,
        retrieveObjectForType: __retrieveObjectForType,
        setObjectForType: __setObjectForType
    };

})();
