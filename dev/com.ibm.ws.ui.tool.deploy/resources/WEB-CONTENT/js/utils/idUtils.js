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

var idUtils = (function() {
    "use strict";

    var __getGroupId = function(groupName) {
        if (groupName) {
            return encodeURIComponent(groupName);
        } else {
            return "default";
        }
    };

    var __getInputId = function(inputVariable) {
        var name = inputVariable.name;
        name = name.replace(/ /g, "_");
        return __getGroupId(inputVariable.group) + "_inputVariable_" + name;
    };

    var __getInputPasswordConfirmationId = function(passwordId) {
        return passwordId + "Confirmation";
    };

    var __getCardId = function(inputVariable) {
        return "parameters_" + __getGroupId(inputVariable.group) + "_card";
    };

    var __getBrowseAndUploadId = function(inputVariable) {
        return __getGroupId(inputVariable.group) + "_browseAndUpload";
    };

    var __getDockerImagesId = function(inputVariable){
        return __getGroupId(inputVariable.group) + "_dockerImageSearch";
    };

    var __getListId = function(inputVariable){
        return __getGroupId(inputVariable.group) + "_listSearch";
    };

    var __getCardRightPaneId = function(inputVariable) {
        return __getGroupId(inputVariable.group) + "_parametersRightPane";
    };

    var __getCardInputContainerId = function(inputVariable) {
        return __getGroupId(inputVariable.group) + "_inputVariablesContainer";
    };

    var __getFilePathInputVariableId = function(fileTypeObject) {
        var inputs = $("#" + __getCardInputContainerId(fileTypeObject.inputVariable) + " input");
        var id = null;
        var i;
        var foundFilePathObject = [];
        for(i = 0; i < inputs.length; i++) {
            var input = inputs.get(i);
            var typeObject = string.retrieveObjectForType(input);

            if (typeObject.inputVariable.type === "filePath") {
                id = typeObject.id;
                foundFilePathObject.push(typeObject);
            }
        }
        if (foundFilePathObject.length > 1) {
            id = null;
            // more than one filePath input is there in the card
            for (i = 0; i < foundFilePathObject.length; i++) {
                // When multiple filePath inputs are in a card, the filePath
                // input associated with the file input has to follow a naming
                // guideline:
                //   eg. ServerPackage
                //       ServerPackageDir
                if (foundFilePathObject[i].inputVariable.name.indexOf(fileTypeObject.inputVariable.name) === 0) {
                    id = foundFilePathObject[i].id;
                    break;
                }
            }
        }

        return id;
    };

    var __getFilePathInputLabelId = function(fileTypeObject){
        return __getFilePathInputVariableId(fileTypeObject) + "_label";
    };

    var __getFilePathDescriptionId = function(fileTypeObject){
        return __getFilePathInputVariableId(fileTypeObject) + "_description";
    };

    var __getClearFieldButtonId = function(typeObject) {
      return typeObject.id + "_clearFieldButton";
    };

    var __getInputDivId = function(typeObject) {
     return typeObject.id + "_inputDiv";
    };

    // Used to get the file path input id from the file type parameter
    var __getFilePathInputDivId = function(fileTypeObject) {
      return __getFilePathInputVariableId(fileTypeObject) + "_inputDiv";
    };

    var __getFilePathClearFieldButtonId = function(typeObject) {
      return __getFilePathInputVariableId(typeObject) + "_clearFieldButton";
    };

    return {
        getBrowseAndUploadId: __getBrowseAndUploadId,
        getCardId: __getCardId,
        getCardRightPaneId: __getCardRightPaneId,
        getCardInputContainerId: __getCardInputContainerId,
        getListId: __getListId,
        getFilePathInputVariableId: __getFilePathInputVariableId,
        getFilePathInputLabelId: __getFilePathInputLabelId,
        getFilePathDescriptionId: __getFilePathDescriptionId,
        getDockerImagesId: __getDockerImagesId,
        getGroupId: __getGroupId,
        getInputId: __getInputId,
        getInputPasswordConfirmationId: __getInputPasswordConfirmationId,
        getClearFieldButtonId: __getClearFieldButtonId,
        getInputDivId: __getInputDivId,
        getFilePathInputDivId: __getFilePathInputDivId,
        getFilePathClearFieldButtonId: __getFilePathClearFieldButtonId
    };

})();
