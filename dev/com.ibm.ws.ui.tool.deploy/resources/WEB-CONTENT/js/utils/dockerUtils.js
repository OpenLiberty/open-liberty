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

var dockerUtils = (function() {
    "use strict";

    var dockerImages = {}; // Maps each dockerImagesRootId to its docker images and selectedImage
    var inputImages = [];

    var __retrieveDockerImages = function (rootDockerImagesSection, inputId, dockerObject) {
        var deferred = new $.Deferred();
        // Add a loading spinner until done loading and set the status to keep track of what to display
        if (listUtils.isInputInFocus(rootDockerImagesSection, inputId)) {
            loadingUtils.displayLoader(rootDockerImagesSection);
        }
        dockerObject.setLoadingStatus(dockerObject.waitStatus.loading);

        apiUtils.retrieveDockerImages().done(function(response) {
            // No images retrieved but is a valid repository, display no images found message to user
            if(response.length === 0) {
                __setupDockerError(rootDockerImagesSection, inputId, dockerObject);
            } else {
                dockerImages[rootDockerImagesSection] = {};
                dockerImages[rootDockerImagesSection].listItems = [];
                    response.forEach(function(image){
                        dockerImages[rootDockerImagesSection].listItems.push(image);
                });
            }

            // display the images only it is refresh and user hasn't clicked to go to a different list
            if (listUtils.isInputInFocus(rootDockerImagesSection, inputId)) {
                // Since the placeholder may have changed, make sure to update it with the correct value.
                $('#' + dockerObject.listSearchId + "_listSearchField").attr("placeholder", messages.SEARCH_IMAGES);
                $('#' + dockerObject.listSearchId + "_listSearchFieldLabel").text(messages.SEARCH_IMAGES);
                __renderDockerImages(rootDockerImagesSection);
                loadingUtils.removeLoader(rootDockerImagesSection);
            }

            // set status to done
            dockerObject.setLoadingStatus(dockerObject.waitStatus.done);
            deferred.resolve();
        }).fail(function(response){
            console.log("Docker image API call failed: ", response);

            // allow user to type into the image input field all the times
            if (response.status === 422) {
                // with the new design, when no repository configuration is in server.xml, allow for local image and
                // do not display the docker image list on the right hand side.
                dockerImages[rootDockerImagesSection] = {};
                dockerImages[rootDockerImagesSection].localRepository = "true";

                if (listUtils.isInputInFocus(rootDockerImagesSection, inputId)) {
                    loadingUtils.removeLoader(rootDockerImagesSection);
                    // take out the focus so that cluster could render its list if the cluster list
                    // REST API finishes later than the docker image REST API
                    listUtils.removeInputInFocus(rootDockerImagesSection);
                }

                // add local image name as placeholder
                var input = $("#" + inputId)[0];
                var placeHolderValue = messages.LOCAL_IMAGE;
                if (globalization.isBidiEnabled()) {
                    placeHolderValue = globalization.createBidiDisplayString(placeHolderValue, "FILE_PATH");
                }
                input.placeholder = placeHolderValue;
                loadingUtils.displayFirstInputForRightHandPanel(rootDockerImagesSection);

                dockerObject.setLoadingStatus(dockerObject.waitStatus.done);
                deferred.reject(response.status);
            } else {
                __setupDockerError(rootDockerImagesSection, inputId, dockerObject, response.responseText);

                if (listUtils.isInputInFocus(rootDockerImagesSection, inputId)) {
                    loadingUtils.removeLoader(rootDockerImagesSection);
                    // Since the placeholder may have changed, make sure to update it with the correct value.
                    $('#' + dockerObject.listSearchId + "_listSearchField").attr("placeholder", messages.SEARCH_IMAGES);
                    $('#' + dockerObject.listSearchId + "_listSearchFieldLabel").text(messages.SEARCH_IMAGES);
                    __renderDockerImages(rootDockerImagesSection);
                }

                dockerObject.setLoadingStatus(dockerObject.waitStatus.done);
                deferred.reject(response.responseText);
            }
        });

        return deferred;
    };

    function __setupDockerError(rootDockerImagesSection, inputId, dockerObject, errorResponse){
//        $('#' + rootDockerImagesSection + "_listViewList").hide();
//        $('#' + rootDockerImagesSection + "_listViewError").hide();
        $("#" + rootDockerImagesSection + "_listViewErrorIcon").empty().append(imgUtils.getSVGSmallObject('grayX'));

        var errorMsg;

        // Display a link to the SM API to show the error message
        if(errorResponse){
            errorMsg = messages.DOCKER_GENERIC_ERROR;
            var errorLink = utils.formatString("<a target='_blank' rel='noreferrer' title='" + messages.ERROR_VIEW_DETAILS + "' href='" + location.protocol + "//" + location.host + "/ibm/api/collective/v1/images/docker/" + "'>{0}</a>", [messages.ERROR_VIEW_DETAILS]);
            $("#" + rootDockerImagesSection + "_listViewErrorLink").html(errorLink);
            $("#" + rootDockerImagesSection + "_listViewErrorLink").prop('title', messages.ERROR_VIEW_DETAILS);
        }
        // Display no images found error
        else{
            errorMsg = messages.DOCKER_EMPTY_IMAGE_ERROR;
        }

        // Reset docker images button
        var resetButton = $("<input type='button' role='button' class='listViewResetButton' aria-label='" + messages.REFRESH + "'  value='" + messages.REFRESH + "'/>");
        resetButton.attr('id', rootDockerImagesSection + "_listViewrRefreshButton");
        resetButton.attr('aria-label', messages.REFRESH_ARIA);

        $(resetButton).on("click", function(){
            __retrieveDockerImages(rootDockerImagesSection, inputId, dockerObject).done(function(response) {
                // validate the input (if there is) against the refresh list
                var input = $("#" + inputId)[0];
                if (input.value) {
                    validateUtils.validateInputField(input, true);
                }
            });
        });

        $("#" + rootDockerImagesSection + "_listViewErrorFooter").html(resetButton);
        $("#" + rootDockerImagesSection + "_listViewErrorMessage").dotdotdot({ watch: "window"});
        $("#" + rootDockerImagesSection + "_listViewErrorMessage").html(errorMsg);
        $("#" + rootDockerImagesSection + "_listViewErrorMessage").prop('title', errorMsg);
    }

    /*
     * Filter the Docker images when the user types in the search field or in the image field on the left pane
     */
    var __renderDockerImages = function(rootDockerImagesId, filterQuery) {
        return listUtils.renderList(rootDockerImagesId, dockerImages, filterQuery);
    };

    var __checkIfImageExists = function(rootDockerImagesId, imageName){
        return listUtils.checkIfListItemExists(rootDockerImagesId, imageName, dockerImages);
    };

    var __clearDockerImages = function(){
        this.dockerImages = {};
    };

    // Used in deployProgress.js to get all images to deploy
    var __getAllSelectedImages = function(){
        // always return the input field as there may not be a selected image
        return inputImages;
    };

    var __setInputImages = function(images) {
        inputImages = images;
    };

    var __getSelectedImage = function(rootDockerImagesId){
        // This function is not being used.
        if (dockerImages[rootDockerImagesId] && dockerImages[rootDockerImagesId].localRepository !== "true") {
            return dockerImages[rootDockerImagesId].selectedListItem;
        } else {
            return "";
        }
    };

    var __setSelectedImage = function(_selectedImage, rootDockerImagesId){
        if (dockerImages[rootDockerImagesId] && dockerImages[rootDockerImagesId].localRepository !== "true") {
            listUtils.setSelectedListItem(_selectedImage, dockerImages[rootDockerImagesId]);
        }
    };

    var __isLocal = function(rootDockerImagesId) {
        var isLocal = dockerImages[rootDockerImagesId] && dockerImages[rootDockerImagesId].localRepository;
        return isLocal;
    };

    return {
        retrieveDockerImages: __retrieveDockerImages,
        renderDockerImages: __renderDockerImages,
        checkIfImageExists: __checkIfImageExists,
        clearDockerImages: __clearDockerImages,
        getAllSelectedImages: __getAllSelectedImages,
        getSelectedImage: __getSelectedImage,
        setSelectedImage: __setSelectedImage,
        setInputImages: __setInputImages,
        isLocal: __isLocal
    };
})();
