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

goog.require("listType");

var docker = (function(){
    "use strict";

    var DockerType = function(inputVariable) {
        list.listType.call(this, inputVariable);

        $('#' + this.listSearchId + "_listSearchField").attr("placeholder", messages.SEARCH_IMAGES);
        $('#' + this.listSearchId + "_listSearchFieldLabel").text(messages.SEARCH_IMAGES);
        $("#" + this.listSearchId).attr("focusId", this.id);

        // When there is no docker configuration provided, the revised design is to not display the
        // docker image list on the right panel. This means not even displaying a message to indicate
        // no image is found.
        dockerUtils.retrieveDockerImages(this.listSearchId, this.id, this);
    };

    DockerType.prototype = $.extend ({},
            list.listType.prototype);

    DockerType.prototype.addInputListener = function() {
        list.listType.prototype.addInputListener.call(this);
        var me = this;

        $("#" + this.id).on("focus", function(event) {
            if (!dockerUtils.isLocal(me.listSearchId)) {
                $("#" + me.listSearchId).attr("focusId", me.id);
                 switch(me.loadingStatus) {
                 case me.waitStatus.loading:
                     $('#' + this.listSearchId + "_listSearchField").attr("placeholder", messages.SEARCH_IMAGES);
                     $('#' + this.listSearchId + "_listSearchFieldLabel").text(messages.SEARCH_IMAGES);
                     me.displayLoading();
                     break;
                 case me.waitStatus.done:
                     me.renderDockerImages();

                     // Remove spinner if its still there and images are loaded
//                     if($('#' + me.listSearchId + "_listViewList").children().length > 0){
//                         $('#' + me.listSearchId).find('.loader').removeClass("loader");
//                     }
                        // Remove spinner as it is possible to have no list item or no matching list item based on the input
                     loadingUtils.removeLoader(me.listSearchId);
                     break;
                default:
                    console.log("not a recognized status");
                    break;
                }
            }
        });

        $("#" + this.id).on("input", function(event) {
            // Clear the Docker image filter so that the search doesn't override that filter
            $("#" + me.listSearchId + "_listSearchField").val("");

            dockerUtils.renderDockerImages(me.listSearchId, $(this).val());

            // Validation of image is removed as we allow user to enter a docker image not in the list due to
            // bugs in the docker registry API
            var valid = dockerUtils.checkIfImageExists(me.listSearchId, $("#" + me.listInputId).val());

            if(valid){
                dockerUtils.setSelectedImage($(this).val(), me.listSearchId);
            }

            // Checks if the form is complete and shows the deploy button if so
            validateUtils.validate(valid);
        });
    };

//    DockerType.prototype.validate = function() {
//        var valid = dockerUtils.checkIfImageExists(this.listSearchId, $("#" + this.listInputId).val());
//        if (valid) {
//            validateUtils.removeInputErrorMessage(this.id);
//        } else {
//            validateUtils.addInputErrorMessage(this.id, messages.INVALID_DOCKER_IMAGE);
//        }
//        return valid;
//    };

    DockerType.prototype.renderDockerImages = function() {
        list.setSearchFieldListener(this);
        var rightPaneId = idUtils.getCardRightPaneId(this.inputVariable);
        $("#" + rightPaneId).closest(".parameters").attr("aria-label", messages.PARAMETERS_DOCKER_ARIA);
        $('#' + this.listSearchId + "_listSearchField").attr("placeholder", messages.SEARCH_IMAGES);
        $('#' + this.listSearchId + "_listSearchFieldLabel").text(messages.SEARCH_IMAGES);
        $("#" + this.listSearchId).attr("focusId", this.id);
        $('#' + this.listSearchId).removeClass("hidden");
        // hide the file browser if one exists
        list.hideRightPanelElements(this.inputVariable);

        dockerUtils.renderDockerImages(this.listSearchId, $("#" + this.listInputId).val());
    };

    var __create = function(inputVariable) {
        var dockerInput = new DockerType(inputVariable);
        return dockerInput;
    };

    return {
        /*
         * code usage example:
         *
         *   if (inputVariable.type === "docker") {
         *     typeObject = docker.create(inputVariable);
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
