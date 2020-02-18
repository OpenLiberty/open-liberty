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

var fileUtils = (function(){
    "use strict";

    var fileUploadInProgress = [];
    var __selectedPackages = [];
    var __fileId = 0; // internal id to provide a way to tell which upload is for which when multiple uploads
                      // of the same file name are in progress
    var _files = {};

    var __fileUploadStatus = {
            init: 1,
            uploading: 2,
            uploadSuccess: 3,
            uploadFail: 4
   };

    var __renderFileBrowse = function(){
        // this is called during setup. It is ok to setup the elements in the template to be cloned.
        var fileBrowserSpan = $(".fileBrowseMessage");
        var browseMessage = utils.formatString(messages.STRONGLOOP_BROWSE, ["<input type='button' id='fileBrowseButton'  class='fileBrowseButton' aria-labelledby='browseLabel' value='" + messages.BROWSE_INSERT + "'>"]);
        fileBrowserSpan.append(browseMessage);
    };

    // Map the browse and upload section id to its file
    // If need to map its card id to the file then use files fileTypeObject.browserAndUploadElement[0].id instead
    var __setFile = function(file, fileTypeObject){
        var fileObject = {};
        fileObject._file = file;
        fileObject._disabled = false; // Set disabled to false so we upload the file when the user clicks deploy
        _files[fileTypeObject.fileBrowserAndUploadId] = fileObject;
        __setFileValue(fileTypeObject, file.name);
    };

    // Remove file when the user clears the file input field
    var __removeFile = function(fileBrowserAndUploadId){
      if(_files[fileBrowserAndUploadId]){
        _files[fileBrowserAndUploadId] = null;
      }
    };

    var __disableFile = function(fileBrowserAndUploadId){
        if(_files[fileBrowserAndUploadId]){
          // Set the disabled property for this browse and upload card to true so we don't send it in the deploy service request when the user
          // clicks the deploy button.
          _files[fileBrowserAndUploadId]._disabled = true;
        }
    };

    var __enableFile = function(fileBrowserAndUploadId){
      if(_files[fileBrowserAndUploadId]){
        // Set the disabled property for this browse and upload card to false to upload the file when the user clicks deploy
        _files[fileBrowserAndUploadId]._disabled = false;
      }
    };

    var __getFile = function(fileBrowserAndUploadId){
        var file;
        var fileObject = _files[fileBrowserAndUploadId];
        if(fileObject && !fileObject._disabled){
          file = _files[fileBrowserAndUploadId]._file;
        }
        return file;
    };

    /*
       Input: fileTypeObject: A file object
       Shows the file's filePath field
    */
    var __enableFilePathInput = function(fileTypeObject){
      var $inputDiv = $("#" + idUtils.getFilePathInputDivId(fileTypeObject));
      var $filePathInput = $("#" + idUtils.getFilePathInputVariableId(fileTypeObject));
      var $label = $("#" + idUtils.getFilePathInputLabelId(fileTypeObject));
      var $description = $("#" + idUtils.getFilePathDescriptionId(fileTypeObject));
      var $clearFieldButton = $("#" + idUtils.getFilePathClearFieldButtonId(fileTypeObject));

      // Show the input, displayName, and label
      $inputDiv.show();
      $label.show();
      $description.show();
      if($filePathInput.val()){
        // Only show the clear field button when there is non-empty input
        $clearFieldButton.show();
      }
    };

    /*
       Input: fileTypeObject: A file object
       This is used for remote file deployments where the file path isn't needed. The file path is hidden until the user toggles the parameterToggle
       card to do remote deployments again.
    */
    var __disableFilePathInput = function(fileTypeObject){
        var $inputDiv = $("#" + idUtils.getFilePathInputDivId(fileTypeObject));
        var $label = $("#" + idUtils.getFilePathInputLabelId(fileTypeObject));
        var $description = $("#" + idUtils.getFilePathDescriptionId(fileTypeObject));
        var $clearFieldButton = $("#" + idUtils.getFilePathClearFieldButtonId(fileTypeObject));

        // Hide tne input, displayName, and label
        $inputDiv.hide();
        $label.hide();
        $description.hide();
        $clearFieldButton.hide();
    };

    /*
        Focuses the file path input on the given card.
        Input: fileTypeObject: A file object
        @Return boolean whether or not the input was focused.
    */
    var __focusFilePathInput = function(fileTypeObject){
      var filePathInputId = idUtils.getFilePathInputVariableId(fileTypeObject);
      var filePathInput = $("#" + filePathInputId);
      if(filePathInput){
        filePathInput.focus();
        return true;
      }
      return false;
    };

    var __uploadFile = function(file, fileTypeObject){
        var fileUpload = {
                fileName: file.name,
                fileId: __fileId
        };
        __fileId++;

        // To perform upload, writeDir permission has to be set in controller server.xml.
        // Without the permission, file upload would fail.
        apiUtils.retrieveControllerWritePaths().done(function(response) {
            var browserAndUploadRootId = fileTypeObject.fileBrowserAndUploadId;

            if (response.value.length === 0) {
                // if writeDirs is not set, issue an error
                __renderFileUploadFailMessage(fileUpload.fileName, messages.FILE_UPLOAD_WRITEDIRS_EMPTY_ERROR, fileTypeObject.id, fileTypeObject, true);
            } else {
                var uploadImagesDir = __getUploadImagePath(response.value, fileTypeObject);
                //console.log("uploadImagesDir: ", uploadImagesDir);

                if (uploadImagesDir) {
                    __setFilePathValue(fileTypeObject, uploadImagesDir);
                    __setFileValue(fileTypeObject, file.name);
                    fileTypeObject.fileUpload = fileUpload;

                    fileUploadInProgress.push(fileUpload);
                    __renderFileUploadingMessage(fileUpload.fileName, fileTypeObject, true);
                    $("#" + browserAndUploadRootId + "_fileUploadSuccess").show();

                    apiUtils.postFileToController(file, uploadImagesDir).done(function() {
                        if (!fileUpload.cancelUpload) {
                            __checkUploadProgress(fileUpload, true, fileTypeObject);
                        } else {
                            // path when cancel is clicked during uploading
                            // upload is finished anyway but just won't show the complete status
                            fileUpload.cancelUpload = null;
                            __removeFileUploadInProgress(fileUpload);
                        }
                    }).fail(function(error) {
                        __checkUploadProgress(fileUpload, false, fileTypeObject);
                    });
                } else {
                    // path when filePath is specified but is not a valid writeDir path
                    var idForError = fileTypeObject.id;
                    var filePathId = idUtils.getFilePathInputVariableId(fileTypeObject);
                    if ($("#" + filePathId).length > 0) {
                        idForError = filePathId;
                    }

                    __renderFileUploadFailMessage(fileUpload.fileName, messages.FILE_UPLOAD_WRITEDIRS_PATH_ERROR, idForError, fileTypeObject, true);
                }
            }
        });
    };

    /*
     * isSuccess is overloaded to cover different paths:
     * - when it is null, it covers the path calling when deploy is clicked and there is still file upload in progress
     * - when it is true, it covers the upload successful path.
     *      With this path, the deploy may or may not have been initiated. The file upload will be updated
     *      with the confirmation check if there is only one file input in the card or there are more
     *      than one file inputs in the card and the completed file load is for the upload currently
     *      displayed on the card.
     *      If deploy has been initiated and there is still file upload in progress, then wait until
     *      all the uploads are done before starting the deploy process.
     * - when it is false, it covers the fail to upload path.
     *      The logic is similar to the upload successful path.
     */
    var __checkUploadProgress = function(fileUpload, isSuccess, fileTypeObject) {
        if (isSuccess === null) {
            if (__getUploadInProgressCount() > 0) {
                deployProgress.displayDeployModal();
                __displayFileUploadInProgress();
            }
        } else if (isSuccess) {
            var body = fileUpload.body;
            // always update file upload status on the parameter card if the file upload is initiated from the displayed browser action
            if (__isInputInFocus(fileTypeObject)) {
                __renderFileUploadSuccessMessage(fileUpload.fileName, fileTypeObject, true);
            } else {
                // update the upload status for an upload that is not in focus. This will only happen if there is
                // more than one file input type in the same card.
                __setFileUploadStatus(__fileUploadStatus.uploadSuccess, fileTypeObject);
            }
            if (body) {
                // path when deploy button has clicked
                __removeFileUploadInProgress(fileUpload);

                if (__getUploadInProgressCount() === 0) {
                    $("#deployProgress").removeClass("deployProgressDim");
                    var filePath = fileUtils.getFilePathInputValue(fileTypeObject);
                    deployProgress.displayDeploymentMessage();
                    deployProgress.allowDeployment(body, false, null, null);
                } else {
                    // wait for other uploads to finish
                    __displayFileUploadInProgress();
                }
            } else {
                //console.log("Upload successfully with browser panel");
                __removeFileUploadInProgress(fileUpload);
            }

        } else {
            // always upload file upload status on the parameter card if the file upload is initiated from the displayed browser action
            if (__isInputInFocus(fileTypeObject)) {
                __renderFileUploadFailMessage(fileUpload.fileName, messages.FILE_UPLOAD_WRITEDIRS_PATH_ERROR, null, fileTypeObject, true);
            } else {
                // update the upload status for an upload that is not in focus
                __setFileUploadStatus(__fileUploadStatus.uploadFail, fileTypeObject, messages.FILE_UPLOAD_WRITEDIRS_PATH_ERROR);
            }
            if (fileUpload.body) {
                // path when deploy button has clicked
                deployProgress.displayDeployModal();
                deployProgress.displayDeploymentFailed(messages.UPLOAD_FAILED);
            }

            __removeFileUploadInProgress(fileUpload);
        }
    };

    /*
     * If a filePath input is not provided, then use the first writeDir returned from the REST API.
     * Otherwise, validate the provided filePath input with the existing writeDirs.
     */
    var __getUploadImagePath = function(writeDirs, fileTypeObject) {
         var filePathValue = __getFilePathInputValue(fileTypeObject);
         if (!filePathValue) {
             if (writeDirs.length > 0) {
                 filePathValue = writeDirs[0];
                 //console.log("---------- returning the first writeDirs value", filePathValue);
             }
         } else {
             var match = false;
             // validate with existing writeDirs
             for (var i = 0; i < writeDirs.length; i++) {
                 // it is a valid input as long as the specified path starts with one of the existing writeDirs path
                 if (filePathValue.indexOf(writeDirs[i]) !== -1) {
                     //console.log("a match is found");
                     match = true;
                     break;
                 }
             }
             if (!match) {
                 //console.log("no match for writeDirs");
                 // ToDo: should we switch to the first writeDir if it is setup?
                 filePathValue = null;
             }
         }
         return filePathValue;
    };

    var __getFilePathInputValue = function(fileTypeObject) {
        var id = idUtils.getFilePathInputVariableId(fileTypeObject);
        var pathValue = null;
        if (id) {
            pathValue = $("#" + id).val();
        }

        return pathValue;
    };

    var __displayFileUploadInProgress = function() {
        var filenames = "";
        for (var i = 0; i < fileUploadInProgress.length; i++) {
            filenames += fileUploadInProgress[i].fileName + " ";
        }

        __displayFileUploadStatus(filenames);
    };

    var __removeFileUploadInProgress = function(fileUploadToBeRemoved) {
        var foundIndex = -1;
        for (var i = 0; i < fileUploadInProgress.length; i++) {
            if ((fileUploadToBeRemoved.fileName === fileUploadInProgress[i].fileName) &&
                (fileUploadToBeRemoved.fileId === fileUploadInProgress[i].fileId)) {
                foundIndex = i;
                break;
            }
        }
        if (foundIndex > -1) {
            fileUploadInProgress.splice(foundIndex, 1);
        }
    };

    var __getSelectedPackages = function() {
        var packages = "";
        for (var i = 0; i < __selectedPackages.length; i++) {
            packages += __selectedPackages[i] + " ";
        }
        return packages;
    };

    /*
     * This method is called when deploy button is clicked by deployTool.js
     */
    var __setSelectedPackages = function(packages) {
        __selectedPackages = packages;
    };

    var __renderFileUploadInit = function(fileTypeObject){
        var rootId = "#" + fileTypeObject.fileBrowserAndUploadId + "_";
        $(rootId + "fileBrowseMessage").show();
        $(rootId + "uploadPreviousFile").show();
        $(rootId + "fileUploadSuccess").hide();
        $(rootId + "fileUploadResetButton").html("");
        __removeUploadingIcon(rootId);
        $(rootId + "fileBrowseStatusIcon").empty().append(imgUtils.getSVGSmallObject('upload'));
    };

    var __renderFileUploadMessages = function(msg, buttonMsg, fileBrowserAndUploadId) {
        var rootId = "#" + fileBrowserAndUploadId + "_";
        $(rootId + "fileUploadSuccessMessage").html(msg);

        if (buttonMsg) {
            $(rootId + "fileUploadResetButton").html("<a id='resetButton' tabindex='0' aria-label='" + buttonMsg + "'>" + buttonMsg + "</a>");
            $(rootId + "fileUploadResetButton").attr("aria-label", buttonMsg);
            $(rootId + "fileBrowseMessage").hide();
            $(rootId + "uploadPreviousFile").hide();
        } else {
            $(rootId + "fileUploadResetButton").html("");
            $(rootId + "fileBrowseMessage").show();
            $(rootId + "uploadPreviousFile").show();
        }
        $(rootId + "fileUploadSuccess").show();
    };

    var __renderFileUploadingMessage = function(fileName, fileTypeObject, setStatus) {
        $("#" + fileTypeObject.fileBrowserAndUploadId + "_fileBrowseStatusIcon").empty().addClass('loader');
        var displayFileName = __getDisplayFileName(fileName);
        var uploadingMessage = utils.formatString(messages.IS_UPLOADING, ["<b>" + displayFileName + "</b>"]);
        __renderFileUploadMessages(uploadingMessage, messages.CANCEL, fileTypeObject.fileBrowserAndUploadId);

        if (setStatus) {
            __setFileUploadStatus(__fileUploadStatus.uploading, fileTypeObject);

            if (fileTypeObject.fileUpload) {
                fileTypeObject.fileUpload.isCancel = true;
            }
        }
    };

    var __renderFileUploadSuccessMessage = function(fileName, fileTypeObject, setStatus) {
        var rootId = "#" + fileTypeObject.fileBrowserAndUploadId + "_";
        __removeUploadingIcon(rootId);

        if (setStatus) {
            $(rootId + "fileBrowseStatusIcon").empty().append(imgUtils.getSVGSmallObject('blueCheck'));
            // Fade to gray after 3 seconds
            setTimeout(function(){
                // do the fading to gray only if the input focus is still there
                if (__isInputInFocus(fileTypeObject) &&
                        (fileTypeObject.getFileUploadStatus() === __fileUploadStatus.uploadSuccess)) {
                    // Make sure that the file upload has not been reset
                    $(rootId + "fileBrowseStatusIcon").empty().append(imgUtils.getSVGSmallObject('grayCheck'));
                }
            }, 3000);
        } else {
            $(rootId + "fileBrowseStatusIcon").empty().append(imgUtils.getSVGSmallObject('grayCheck'));
        }

        var displayFileName = __getDisplayFileName(fileName);
        var successMessage = utils.formatString(messages.UPLOAD_SUCCESSFUL, ["<b>" + displayFileName + "</b>"]);
        __renderFileUploadMessages(successMessage, messages.RESET, fileTypeObject.fileBrowserAndUploadId);
        if (setStatus) {
            __setFileUploadStatus(__fileUploadStatus.uploadSuccess, fileTypeObject);
            __deleteFileUpload(fileTypeObject);
        }
    };

    var __renderFileUploadFailMessage = function(fileName, error, idForError, fileTypeObject, setStatus) {
        var rootId = "#" + fileTypeObject.fileBrowserAndUploadId + "_";
        __removeUploadingIcon(rootId);

        if (setStatus) {
            $(rootId + "fileBrowseStatusIcon").empty().append(imgUtils.getSVGSmallObject('redX'));
            // Fade to gray after 3 seconds
            setTimeout(function(){
                // do the fading to gray only if the input focus is still there
                if (__isInputInFocus(fileTypeObject) &&
                        (fileTypeObject.getFileUploadStatus() === __fileUploadStatus.uploadFail)) {
                    // Make sure that the file upload has not been reset
                    $(rootId + "fileBrowseStatusIcon").empty().append(imgUtils.getSVGSmallObject('grayX'));
                }
            }, 3000);
        } else {
            $(rootId + "fileBrowseStatusIcon").empty().append(imgUtils.getSVGSmallObject('grayX'));
        }

        var failMessage = messages.UPLOAD_FAILED;
        if (error) {
            failMessage = error;
        }
        __renderFileUploadMessages(failMessage, "", fileTypeObject.fileBrowserAndUploadId);

        if (idForError) {
            validateUtils.addInputErrorMessage(idForError, failMessage);
        }
        if (setStatus) {
            __setFileUploadStatus(__fileUploadStatus.uploadFail, fileTypeObject, failMessage);
            __deleteFileUpload(fileTypeObject);
        }
    };

    var __removeUploadingIcon = function(rootId) {
        $(rootId + "fileBrowseStatusIcon").empty().removeClass('loader');
    };

    // File still uploading when user clicks deploy, show percentage of file upload and then switch to success or error message
    var __displayFileUploadStatus = function(fileNames){
        $("#deployErrors").hide();
        $("#deployProgressCircleText").html(messages.UPLOADING);
        $("#deployProgressCircle, #deployProgressCircleText").removeClass("hidden");
        $("#deployProgressCircle").addClass('deploySpinner');

        $("#deploymentMessageTitle").html(messages.DEPLOY_FILE_UPLOADING);
        var fileUploadingMessage = "<b>" + messages.DEPLOY_UPLOADING_MESSAGE + "</b><br /><br />" +
         utils.formatString(messages.DEPLOY_AFTER_UPLOADED_MESSAGE, ["<b>" + fileNames + "</b>"]);
        $("#deploymentMessageContent").html(fileUploadingMessage);

        // ToDo: need to talk with design on progress indicator as it is uploading to the controller and no progress indicator provided
//        var fileUploadMessage = utils.formatString(messages.DEPLOY_FILE_UPLOAD_PERCENT, [fileNames, "50"]);
//        $("#deployMessageButtonContent").html(fileUploadMessage);
//        $("#deployMessageButton").show();
        $("#deployProgress").addClass("deployProgressDim");
    };

    var __setFileUploadStatus = function(status, fileTypeObject, errorMsg) {
        if (errorMsg) {
            fileTypeObject.setFileUploadStatus(status, errorMsg);
        } else {
            fileTypeObject.setFileUploadStatus(status);
        }
    };

    var __deleteFileUpload = function(fileTypeObject) {
        if (fileTypeObject.hasOwnProperty("fileUpload")) {
            delete fileTypeObject.fileUpload;
        }
    };

    var __reset = function(fileTypeObject){
        var rootId = "#" + fileTypeObject.fileBrowserAndUploadId + "_";
        __removeUploadingIcon(rootId);
        __renderFileUploadInit(fileTypeObject);

        var fileUpload = fileTypeObject.fileUpload;
        if (fileUpload && fileUpload.isCancel) {
            fileUpload.isCancel = null;
            fileUpload.cancelUpload = true;
        }
        __deleteFileUpload(fileTypeObject);

        // clear values and errors
        validateUtils.removeInputErrorMessage(fileTypeObject.id);
        validateUtils.removeInputErrorMessage(idUtils.getFilePathInputVariableId(fileTypeObject));
        __setFileValue(fileTypeObject, "");
        __setFilePathValue(fileTypeObject, "");
        __setFileUploadStatus(__fileUploadStatus.init, fileTypeObject);
    };

    var __setFilePathValue = function(fileTypeObject, value) {
        var id = idUtils.getFilePathInputVariableId(fileTypeObject);
        if (id) {
            $("#" + id).val(value).trigger("change");
        }
        validateUtils.changeFieldWarningLabel(id, false);
    };

    var __setFileValue = function(fileTypeObject, value) {
        $("#" + fileTypeObject.id).val(value).trigger("change");
         validateUtils.changeFieldWarningLabel(fileTypeObject.id, false);
    };

    var __setDeploymentBody = function(body) {
        for (var i = 0; i < fileUploadInProgress.length; i++) {
            fileUploadInProgress[i].body = body;
        }
    };

    var __getUploadInProgressCount = function() {
        var uploadCount = 0;
        for (var i = 0; i < fileUploadInProgress.length; i++) {
            if (fileUploadInProgress[i].hasOwnProperty("cancelUpload") && fileUploadInProgress[i].cancelUpload) {
                // do nothing as this is a cancel upload
                console.log("cancel upload");
            } else {
                uploadCount++;
            }
        }
        return uploadCount;
    };

    var __getDisplayFileName = function(fileName) {
        var displayFileName = fileName;
        if (globalization.isBidiEnabled()) {
            displayFileName = globalization.createBidiDisplayString(fileName, "FILE_PATH");
        }
        return displayFileName;
    };

    var __isInputInFocus = function(fileTypeObject) {
        return  ($("#" + fileTypeObject.fileBrowserAndUploadId + "_fileBrowseButton").attr("focusId") === fileTypeObject.id);
    };

    return {
        checkUploadProgress: __checkUploadProgress,
        getFilePathInputValue: __getFilePathInputValue,
        getSelectedPackages: __getSelectedPackages,
        getUploadInProgressCount: __getUploadInProgressCount,
        renderFileBrowse: __renderFileBrowse,
        renderFileUploadingMessage: __renderFileUploadingMessage,
        renderFileUploadFailMessage: __renderFileUploadFailMessage,
        renderFileUploadInit: __renderFileUploadInit,
        renderFileUploadSuccessMessage: __renderFileUploadSuccessMessage,
        reset: __reset,
        setDeploymentBody: __setDeploymentBody,
        setSelectedPackages: __setSelectedPackages,
        fileUploadStatus: __fileUploadStatus,
        displayFileUploadStatus: __displayFileUploadStatus,
        uploadFile: __uploadFile,
        getFile: __getFile,
        enableFilePathInput: __enableFilePathInput,
        disableFilePathInput: __disableFilePathInput,
        focusFilePathInput: __focusFilePathInput,
        setFile: __setFile,
        removeFile: __removeFile,
        disableFile: __disableFile,
        enableFile: __enableFile
    };
})();
