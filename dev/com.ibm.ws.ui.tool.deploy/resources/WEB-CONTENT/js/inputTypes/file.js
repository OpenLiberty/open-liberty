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

var file = (function() {
    "use strict";

    var FileType = function(inputVariable, lastDeployLocal) {
        var me = this;
        string.stringType.call(this, inputVariable);
        this.fileBrowserAndUploadId = idUtils.getBrowseAndUploadId(inputVariable);
        this.lastDeployLocal = lastDeployLocal;

        // look for existing browser object belonging to this card
        this.browserAndUploadElement = $("#" + this.fileBrowserAndUploadId);
        if (this.browserAndUploadElement.length === 0) {
            // create browserAndUpload for this card
            this.browserAndUploadElement = __createBrowserAndUploadClone(this.fileBrowserAndUploadId);
        }

        var rightPaneId = idUtils.getCardRightPaneId(inputVariable);
        $("#" + rightPaneId).append(this.browserAndUploadElement);
        __hideRightPanelElements(this.inputVariable);
        $('#' + this.fileBrowserAndUploadId).removeClass("hidden");

        __setTitle(this);

        // Set up the toggle for remote/local deployment
        console.log('loading persistence');
        userConfig.load(function(response) {
          var toggleOption = response[me.fileBrowserAndUploadId + "_toggle"];
          __setToggle(me, toggleOption);
        },function(err) {
            __setToggle(me);
        });
        this.setFileUploadStatus(fileUtils.fileUploadStatus.init);
        $("#" + this.fileBrowserAndUploadId + "_fileBrowseButton").attr("focusId", this.id);
    };

    var __createBrowserAndUploadClone = function(id) {
        var newBrowserAndUploadElement = $("#serverBrowseAndUpload").clone().attr("id", id).removeClass("hide");
        newBrowserAndUploadElement.find("#serverBrowseTitle").attr("id", id + "_serverBrowseTitle");
        newBrowserAndUploadElement.find("#serverFileBrowser").attr("id", id + "_serverFileBrowser");
        newBrowserAndUploadElement.find("#fileBrowseStatusIcon").attr("id", id + "_fileBrowseStatusIcon");
        newBrowserAndUploadElement.find("#fileBrowserMessages").attr("id", id + "_fileBrowserMessages");
        newBrowserAndUploadElement.find("#fileBrowseMessage").attr("id", id + "_fileBrowseMessage");
        newBrowserAndUploadElement.find("#fileBrowseButton").removeClass("hide");
        newBrowserAndUploadElement.find("#fileBrowseButton").attr("tabindex", "0");
        newBrowserAndUploadElement.find("#fileBrowseButton").attr("aria-labelledby", id + "_browseLabel");
        newBrowserAndUploadElement.find("#fileBrowseButton").attr("id", id + "_fileBrowseButton");
        newBrowserAndUploadElement.find("#fileUploadSuccess").attr("id", id + "_fileUploadSuccess");
        newBrowserAndUploadElement.find("#fileUploadSuccessMessage").attr("id", id + "_fileUploadSuccessMessage");
        newBrowserAndUploadElement.find("#fileUploadResetButton").attr("id", id + "_fileUploadResetButton");
        newBrowserAndUploadElement.find("#fileUploadFooter").attr("id", id + "_fileUploadFooter");
        newBrowserAndUploadElement.find("#uploadPreviousFile").attr("id", id + "_uploadPreviousFile");
        newBrowserAndUploadElement.find("#browseLabel").attr("id", id + "_browseLabel");
        newBrowserAndUploadElement.find("#browse").attr("id", id + "_browse");

        return newBrowserAndUploadElement;
    };

    FileType.prototype = $.extend ({
        getBrowseAndUploadElement: function() {
            return this.browserAndUploadElement;
        },
        showBrowseAndUploadElement: function() {
            $(this.fileBrowserAndUploadId).removeClass("hidden");
        },
        setFileUploadStatus: function(status, error) {
            this.uploadStatus = status;
            if (error) {
                this.uploadError = error;
            } else {
                if (this.hasOwnProperty("uploadError")) {
                    delete this.uploadError;
                }
            }
        },
        getFileUploadStatus: function(){
            return this.uploadStatus;
        },
        renderFileUpload: function() {
             // hide the cluster list
            __hideRightPanelElements(this.inputVariable);

            // Only show the right side for remote deployments
            if(this.remoteDeployment){
              $('#' + this.fileBrowserAndUploadId).removeClass("hidden invisible");
            }

            __setTitle(this);
            // use focusId attr to let me know whether the browser action is for this input
            $("#" + this.fileBrowserAndUploadId + "_fileBrowseButton").attr("focusId", this.id);
            switch(this.uploadStatus){
            case fileUtils.fileUploadStatus.init:
                fileUtils.renderFileUploadInit(this);
                break;
            case fileUtils.fileUploadStatus.uploading:
                fileUtils.renderFileUploadingMessage($("#" + this.id).val(), this);
                break;
            case fileUtils.fileUploadStatus.uploadSuccess:
                fileUtils.renderFileUploadSuccessMessage($("#" + this.id).val(), this);
                break;
            case fileUtils.fileUploadStatus.uploadFail:
                fileUtils.renderFileUploadFailMessage($("#" + this.id).val(), this.uploadError, null, this);
                break;
            default:
                console.log("status is not recognized");
                break;
            }
        }
    }, string.stringType.prototype);

    FileType.prototype.addInputListener = function() {
        string.stringType.prototype.addInputListener.call(this);

        var me = this;
        var browserId = "#" + this.fileBrowserAndUploadId + "_browse";

     // Remove the file associated with this fileBrowserAndUploadId id when manually typing in a file
        $("#" + this.id).on("input", function(event) {
           fileUtils.disableFile(me.fileBrowserAndUploadId);
        });

        $("#" + this.id).on("focus", function(event) {
            me.renderFileUpload();
        });

        $("#" + this.fileBrowserAndUploadId + "_fileBrowseButton").on("click", function() {
            // multiple file inputs: only handle if the action is for this input
            if ($("#" + me.fileBrowserAndUploadId + "_fileBrowseButton").attr("focusId") === me.id) {
                $(browserId).click();
            }
        });

        $("#" + this.fileBrowserAndUploadId + "_browse").on("change", function(e){
            // multiple file inputs: only handle if the action is for this input
            if ($("#" + me.fileBrowserAndUploadId + "_fileBrowseButton").attr("focusId") === me.id) {
                var files = $(this).prop('files');
                if(files){
                    var deployType = ruleSelectUtils.selectedRule.type;
                    // Check that the deploy rule has a type and if it is Liberty or Node.js then upload the file in a multi-part
                    // request when the user clicks "Deploy", otherwise upload immediately to the controllerand the controller will handle
                    // deploying when the user clicks "Deploy"
                    if(deployType && (deployType === "Liberty" || deployType === "Node.js")){
                        fileUtils.disableFilePathInput(me);
                        fileUtils.setFile(files[0], me);
                    }
                    else{
                        __uploadFile(files[0], me);
                    }
                    // This following line of code will allow same file name to be selected again in the file browser
                    // and trigger the change event.
                    this.value = "";
                }
            }
        });

        $("#" + this.fileBrowserAndUploadId + "_fileUploadResetButton").on("click", function(event) {
            // multiple file inputs: only handle if the action is for this input
            if ($("#" + me.fileBrowserAndUploadId + "_fileBrowseButton").attr("focusId") === me.id) {
                // a change event is fired and will take care of validating the review/deploy when resetting file and filePath input values
                fileUtils.reset(me);
            }
        });

        // set title attribute to show the full title when hover over an overflow title
        $("#" + this.fileBrowserAndUploadId + "_serverBrowseTitle").on('mouseenter', function(){
            var $this = $(this);

            if (this.offsetWidth < this.scrollWidth) {
                $this.attr('title', $this.html());
            } else {
                $this.removeAttr('title');
            }
        });

        // Bind the file drag and drop event to the parameter section so that we can capture the file to upload
        var parameterSection = $("#" + idUtils.getCardId(this.inputVariable));

        // parameterSection.on('dragstart', function(event) {
        //     event.preventDefault();
        //     event.stopPropagation();
        //
        //     console.error(event.type);
        //
        //     // TODO: Make parameter section opaque and show a message that they can drop it here
        //     parameterSection.addClass('underlay');
        //
        //     event.originalEvent.dataTransfer.setData('text', 'test');
        //
        //     // event.originalEvent.dataTransfer.dropEffect = "copy";
        // });

        // Handles dragOver and dragLeave events for dragging files over the parameter section.
        var fileDragEvent = function(event) {
          event.preventDefault();
          event.stopPropagation();

          // Prevent the user from dropping files if the parameter card toggle is switched to use a file located on the controller
          if(this.remoteDeployment){
            // TODO: Make parameter section opaque and show a message that they can drop it here
            parameterSection.addClass('dim');
          }
        };

        // Handles dropping of files onto the parameter section.
        var fileDropEvent = function(event){
          event.preventDefault();
          event.stopPropagation();

          // Prevent the user from dropping files if the parameter card toggle is switched to use a file located on the controller
          if(this.remoteDeployment){
            // multiple file inputs: only handle if the action is for this input
            if ($("#" + me.fileBrowserAndUploadId + "_fileBrowseButton").attr("focusId") === me.id) {
                parameterSection.removeClass('dim');
                var files = event.originalEvent.dataTransfer.files;
                if(files){
                    var deployType = ruleSelectUtils.selectedRule.type;
                    // Check that the deploy rule has a type and if it is Liberty or Node.js then upload the file in a multi-part
                    // request when the user clicks "Deploy", otherwise upload immediately to the controllerand the controller will handle
                    // deploying when the user clicks "Deploy"
                    if(deployType && (deployType === "Liberty" || deployType === "Node.js")){
                        //Only accept one file
                        fileUtils.disableFilePathInput(me);
                        fileUtils.setFile(files[0], me);
                    }
                    else{
                        __uploadFile(files[0], me);
                    }
                }
            }
          }
        };

        // Use jQuery proxy functions to allow for scoping of 'me'
        parameterSection.on('dragover dragleave', $.proxy(fileDragEvent, me));
        parameterSection.off('drop').on("drop", $.proxy(fileDropEvent, me));
    };

    FileType.prototype.validate = function() {
        var valid = true;
        // if file upload is used, there could be existing error from file upload. Use the
        // existing error for validation.
        if ($("input[id='" + this.id + "']").next('p').length !== 0) {
            valid = false;
        }
//        if ($("input[id='" + idUtils.getFilePathInputVariableId(this) + "']").next('p').length !== 0) {
//            valid = false;
//        }
        return valid;
    };

    var __uploadFile = function(file, fileTypeObject) {
        fileTypeObject.selectedPackage = file.name;
        fileUtils.uploadFile(file, fileTypeObject);
    };

    var __setTitle = function(me) {
        var packageType;
        if(ruleSelectUtils.isCustomRule()){
          // Use the input variable's displayName as the browse and upload section title
          packageType = me.getLabelString();
        }
        else{
          // Insert the rule's package type into the browse and upload section, because the input variable's displayName includes 'name' at the end of it
          packageType = ruleSelectUtils.getSelectedPackageType();
        }
        var uploadTitle = utils.formatString(messages.BROWSE_TITLE, [packageType]);
        $("#" + me.fileBrowserAndUploadId + "_serverBrowseTitle").html(uploadTitle);
        $("#" + me.fileBrowserAndUploadId).closest(".parameters").attr("aria-label", utils.formatString(messages.PARAMETERS_FILE_ARIA , [uploadTitle]));
    };

    // Set the deployment type to local, where the package is on the controller already.
    var __toggleLocalDeployment = function(me, initialLoad) {
      if(initialLoad || me.remoteDeployment){
        me.remoteDeployment = false;
        fileUtils.disableFile(me.fileBrowserAndUploadId); // Disable using the previously selected file from this fileBrowserAndUpload section
        userConfig.save(me.fileBrowserAndUploadId + "_toggle", "local");

        me.$localToggle.attr("aria-pressed", "true");
        me.$remoteToggle.attr("aria-pressed", "false");
        me.$localToggle.addClass("searchFilterSelected");
        me.$remoteToggle.removeClass("searchFilterSelected");

        // Hide the right side of the parameter card
        $("#" + me.fileBrowserAndUploadId).addClass('invisible');

        // Disable the file associated with this fileBrowserAndUploadId id
        fileUtils.enableFilePathInput(me);

        var self = $("#" + me.id);

        if(self.val() === "" && me.oldValue && me.oldValue !== ""){
          self.val(me.oldValue);
          me.clearFieldButton.show();
        }

        // Remove input readonly property
        self.removeAttr('readonly');

        // Validate the Deploy tool since the inputs may have changed
        validateUtils.validate();
      }
    };

    // Set the deployment type to remote, where the user needs to upload their package to the controller.
    var __toggleRemoteDeployment = function(me) {
      if(!me.remoteDeployment){
        me.remoteDeployment = true;
        fileUtils.enableFile(me.fileBrowserAndUploadId);
        userConfig.save(me.fileBrowserAndUploadId + "_toggle", "remote");

        me.$localToggle.attr("aria-pressed", "false");
        me.$remoteToggle.attr("aria-pressed", "true");
        me.$localToggle.removeClass("searchFilterSelected");
        me.$remoteToggle.addClass("searchFilterSelected");

        // Show the right side of the parameter card
        $("#" + me.fileBrowserAndUploadId).removeClass('invisible');

        // Remove the file input associated with this fileBrowserAndUploadId id
        fileUtils.disableFilePathInput(me);

        var self = $("#" + me.id);
        me.oldValue = self.val();

        // Check if the user has already selected a file to upload
        var file = fileUtils.getFile(me.fileBrowserAndUploadId);
        if(file){
          var name = file.name;
          self.val(name);
        }
        else{
          self.val("");
          me.clearFieldButton.hide();
        }

        // Set input to readonly
        self.attr('readonly', 'true');

        // Validate the Deploy tool since the inputs may have changed
        validateUtils.validate();
      }
    };

    // Sets up the toggle slider that lets the user choose between using a file already on the controller
    // and uploading a file to deploy. Moving the toggle to upload changes the parameters card to show the file upload
    // section and it hides the field for the package directory because the user doesn't need to specify where the file is if they
    // are uploading the file. By default the toggle is set to use a file on the controller.
    var __setToggle = function(me, toggleOption){
      me.$toggle = $("#" + me.fileBrowserAndUploadId).closest('.parameters').find('.parameterToggle');
      me.$localToggle = me.$toggle.find('.parameterToggleController');
      me.$remoteToggle = me.$toggle.find('.parameterToggleUpload');
      me.$toggle.removeClass('hidden');
      if(toggleOption && toggleOption === "remote"){
        __toggleRemoteDeployment(me);
      }
      // Toggle persistence is either 'local' or there is no persistence. Set it to local by default if no persistence.
      else{
        __toggleLocalDeployment(me, true); // Set default toggle to local
      }

      me.$remoteToggle.on('click', function(){
          __toggleRemoteDeployment(me);
      });
      me.$localToggle.on('click', function(){
          __toggleLocalDeployment(me, false);
      });
    };

    var __hideRightPanelElements = function(inputVariable) {
        var searchListForCard = $('#' + idUtils.getListId(inputVariable));
        if (searchListForCard.length !== 0) {
            searchListForCard.addClass("hidden");
        }
    };

    var __create = function(inputVariable, lastDeployLocal) {
        var newFile = new FileType(inputVariable, lastDeployLocal);
        return newFile;
    };

    return {
        /*
         * code usage example:
         *
         *   if (inputVariable.type === "file") {
         *     typeObject = file.create(inputVariable);
         *   }
         *   inputVariablesContainer.append(typeObject.getLabel());
         *   var input = typeObject.getInput();
         *   // Note: it is important that the input is put in the container first before calling
         *   // setObjectForType
         *   inputVariablesContainer.append(input);
         *   string.setObjectForType(input, typeObject);
         *   typeObject.addInputListener();
         *
         *   validate code example is the same as for string type.
         */
        create: __create
    };

})();
