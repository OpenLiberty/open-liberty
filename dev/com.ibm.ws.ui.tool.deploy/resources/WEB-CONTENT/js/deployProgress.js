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

var deployProgress = (function() {
    "use strict";

    var deploymentButtonHasOnClick = false;

    var deployment = {
            id: null,
            checkInProgress: null
    };

    // Initialize references to the modal elements to reduce lookup times.
    var __initializeModalReferences = function() {
        // modal
        deployProgress.modalContainer = $("#deployModalContainer");
        deployProgress.modal = $("#deployModal");

        // Top section of the modal`
        deployProgress.deploymentMessage = $("#deploymentMessage");
        deployProgress.deployMessageTitle = $("#deploymentMessageTitle");
        deployProgress.deployMessageContentWrapper = $("#deploymentMessageContentWrapper");
        deployProgress.deployMessageContent = $("#deploymentMessageContent");
        deployProgress.deployMessageButtonHeader = $("#deploymentMessageButtonHeader");
        deployProgress.deployMessageButton = $("#deployMessageButton");
        deployProgress.deployMessageButtonContent = $("#deployMessageButtonContent");
        deployProgress.progressSpinnerContainer = $("#deploymentProgressCircleContainer");
        deployProgress.progressSpinnerText = $("#deployProgressCircleText");

        // Bottom section
        deployProgress.deployErrors = $("#deployErrors");
        deployProgress.deployErrorsTitle = $("#deployErrorsTitle");
        deployProgress.backgroundTasksButton = $("#backgroundTasksButton");
    };

    // Links the JQuery element to the Explore tool background tasks
    var __linkToExploreToolBgTasks = function($element) {
      $element.off("click").on("click", function(e){
          e.stopPropagation();

          var loc = window.parent.location;
          var devVersion = (loc.href.indexOf('devDeploy') > -1 || loc.href.indexOf('devAdminCenter') > -1);
          var url = loc.protocol + '//' + loc.host + '/';
          url += (devVersion) ? 'devAdminCenter/#backgroundTasks' : 'adminCenter/#backgroundTasks';

          window.open(url, '_blank');
      });

      $element.off("keydown").on("keydown", function(e){
          if(e.which === 13){ //Enter key
              $element.click();
          }
      });
      $element.show();
    };

    // Links the JQuery element to the Explore tool servers view
    var __linkToExploreToolServers = function($element) {
      var location = window.parent.location;
      var devVersion = (location.href.indexOf('devDeploy') > -1 || location.href.indexOf('devAdminCenter') > -1);
      var url = location.protocol + '//' + location.host + '/';

      // Check if the Toolbox is available
      utils.checkIfToolboxExists().then(function(response){
          if (response === true) {
              url += (devVersion) ? 'devAdminCenter/#explore/servers'    : 'adminCenter/#explore/servers';
          }
          else{
              // Dev version does not have a standalone Explore tool
              url += (devVersion) ? 'devAdminCenter/#explore/servers' : 'ibm/adminCenter/explore-1.0/#explore/servers';
          }

          $element.off("click").on("click", function(e){
              e.stopPropagation();
              window.open(url, '_blank');
          });

          $element.off("keydown").on("keydown", function(e){
              if(e.which === 13){ //Enter key
                  $element.click();
              }
          });
      });
    };

    // Show deployment status message in the deploy modal
    var __displayDeploymentMessage = function() {
        deployProgress.deployErrors.hide();
        deployProgress.deployMessageButton.hide();

        deployProgress.progressSpinnerText.html(messages.DEPLOYING);
        deployProgress.progressSpinnerText.attr("title", messages.DEPLOYING);

        deployProgress.deployMessageTitle.html(messages.DEPLOY_IN_PROGRESS);
        deployProgress.deployMessageTitle.attr("title", messages.DEPLOY_IN_PROGRESS);
        deployProgress.deployMessageTitle.attr("tabindex", "0");
        deployProgress.deployMessageTitle.attr("aria-label", messages.DEPLOY_IN_PROGRESS);

        deployProgress.deployMessageContent.html(messages.DEPLOY_WATCH_FOR_UPDATES + " " + messages.DEPLOY_CHECK_STATUS);
        deployProgress.deployMessageContent.attr("title", messages.DEPLOY_WATCH_FOR_UPDATES + " " + messages.DEPLOY_CHECK_STATUS);
        deployProgress.deployMessageContent.attr("aria-label", messages.DEPLOY_WATCH_FOR_UPDATES + " " + messages.DEPLOY_CHECK_STATUS);

        // Show link to background tasks
        // Check for Explore Tool before showing link to deployed servers
        utils.checkIfExploreToolExists().then(function(response){
            if(response === true){
                if(!deploymentButtonHasOnClick){
                  deployProgress.deployMessageButtonHeader.show();
                  deployProgress.deployMessageButtonHeader.html(messages.EXPLORE_TOOL);
                  deployProgress.deployMessageButtonHeader.attr("title", messages.EXPLORE_TOOL);
                  deployProgress.deployMessageButtonHeader.attr("aria-label", "");

                  deployProgress.deployMessageButtonContent.html(messages.DEPLOY_VIEW_BG_TASKS);

                  deployProgress.deployMessageButton.prop("tabindex", "0");
                  deployProgress.deployMessageButton.prop("title", messages.DEPLOY_VIEW_BG_TASKS);
                  deployProgress.deployMessageButton.prop("aria-label", messages.DEPLOY_VIEW_BG_TASKS);

                  deploymentButtonHasOnClick = true;
                  __linkToExploreToolBgTasks(deployProgress.deployMessageButton);
                }
            }
            else{
                deployProgress.deployMessageButton.hide();
            }
        });

        deployProgress.deploymentMessage.focus();
    };


    /**  Deploy completed. The Deploy service finished trying to deploy. There may or not be errors with the deployment.
    *    @param hasErrors: Flag if the deployment had an error on at least one host
    */
    var __displayDeploymentComplete = function(hasErrors) {

        // Disable success SVG in the top pane
        $("#deployProgressCircle, #deployProgressCircleText").addClass("hidden");

        // Check if any previous modal SVG is present and remove it before adding
        $("#modalSVG").remove();

        var statusSVG;
        if(hasErrors){
          statusSVG = imgUtils.getSVGSmallObject('modal-fail-x');
          deployProgress.deployMessageContent.addClass("failureMessageVerticalBar");
        }
        else {
          statusSVG = imgUtils.getSVGSmallObject('modal-success-check');
        }
        statusSVG.setAttribute("id", "modalSVG"); // Used in the modal tear-down to be able to remove it for the next deployment
        deployProgress.progressSpinnerContainer.append(statusSVG);

        var deployCompleteTitle = hasErrors ? messages.DEPLOYMENT_COMPLETE_WITH_ERRORS_TITLE : messages.DEPLOYMENT_COMPLETE_TITLE;
        deployProgress.deployMessageTitle.html(deployCompleteTitle);
        deployProgress.deployMessageTitle.attr("tabindex", "0");
        deployProgress.deployMessageTitle.attr("aria-label", deployCompleteTitle);
        deployProgress.deployMessageTitle.attr("title", deployCompleteTitle);

        deployProgress.deployMessageContent.html(messages.DEPLOYMENT_COMPLETE_MESSAGE);
        deployProgress.deployMessageContent.attr("aria-label", messages.DEPLOYMENT_COMPLETE_MESSAGE);
        deployProgress.deployMessageContent.attr("title", messages.DEPLOYMENT_COMPLETE_MESSAGE);

        // Check for Explore Tool before showing link to deployed servers
        utils.checkIfExploreToolExists().then(function(response){
            if(response === true){
                deployProgress.deployMessageButtonHeader.show();
                deployProgress.deployMessageButtonHeader.html(messages.EXPLORE_TOOL);
                deployProgress.deployMessageButtonHeader.attr("title", messages.EXPLORE_TOOL);
                deployProgress.deployMessageButtonHeader.attr("aria-label", "");
                deployProgress.deployMessageButtonContent.html(messages.VIEW_DEPLOYED_SERVERS);

                deployProgress.deployMessageButton.prop("tabindex", "0");
                deployProgress.deployMessageButton.prop("title", messages.VIEW_DEPLOYED_SERVERS);
                deployProgress.deployMessageButton.prop("aria-label", messages.VIEW_DEPLOYED_SERVERS);

                deploymentButtonHasOnClick = true;
                __linkToExploreToolServers(deployProgress.deployMessageButton);
                deployProgress.deployMessageButton.show();
            }
        });

        // Finally send focus to the modal for accessibility users and read the title
        deployProgress.deploymentMessage.focus();
    };

    // The Deploy Service failed to start and no deployment token was returned.
    var __displayDeploymentFailed = function(errorMsg) {
        console.log(errorMsg);
        deployProgress.deployErrors.hide();

        // Enable failure SVG in the top pane
        $("#deployProgressCircle, #deployProgressCircleText").addClass("hidden");

        // Check if any previous modal SVG is present and remove it before adding
        $("#modalSVG").remove();

        var statusSVG = imgUtils.getSVGSmallObject('modal-fail-x');
        statusSVG.setAttribute("id", "modalSVG"); // Used in the modal tear-down to be able to remove it for the next deployment
        deployProgress.progressSpinnerContainer.append(statusSVG);

        deployProgress.deployMessageContent.addClass("failureMessageVerticalBar");

        deployProgress.deployMessageTitle.html(messages.DEPLOYMENT_FAILED);
        deployProgress.deployMessageTitle.attr("tabindex", "0");
        deployProgress.deployMessageTitle.attr("aria-label", messages.DEPLOYMENT_FAILED);
        deployProgress.deployMessageTitle.attr("title", messages.DEPLOYMENT_FAILED);

        // Special case for error messages returned by the Docker API itself
        var cleanedMsg = "";
        if(errorMsg.indexOf('!DOCTYPE') > -1){
          // Check for the code tag
          var codeIndex = errorMsg.indexOf('<div id=\"code\">');
          if(codeIndex){
            var codeEndIndex = errorMsg.substring(codeIndex + 15).indexOf("<br>");
            cleanedMsg += errorMsg.substring(codeIndex + 15, codeIndex + 15 + codeEndIndex);
          }
          if(cleanedMsg !== ""){
            errorMsg = cleanedMsg;
          }
        }

        deployProgress.deployMessageContent.text(errorMsg);
        deployProgress.deployMessageContent.attr("aria-label", errorMsg);
        deployProgress.deployMessageContent.attr("title", errorMsg);

        deployProgress.deployMessageButtonHeader.show();
        deployProgress.deployMessageButtonHeader.html(messages.REUTRN_DEPLOY_HEADER);
        deployProgress.deployMessageButtonHeader.attr("title", messages.RETURN_DEPLOY);
        deployProgress.deployMessageButtonHeader.attr("aria-label", "");

        deployProgress.deployMessageButtonContent.html(messages.RETURN_DEPLOY);

        deployProgress.deployMessageButton.attr("aria-label", messages.RETURN_DEPLOY);
        deployProgress.deployMessageButton.attr("title", messages.RETURN_DEPLOY);
        deployProgress.deployMessageButton.prop("tabindex", "0");

        deploymentButtonHasOnClick = true;
        deployProgress.deployMessageButton.off("click").on("click", function(e){
            e.stopPropagation();
            __hideModalWindow();
        });
        deployProgress.deployMessageButton.off("keydown").on("keydown", function(e){
            if(e.which === 13){
                deployProgress.deployMessageButton.click();
            }
        });
        deployProgress.deployMessageButton.show();

        // Finally send focus to the modal for accessibility users and read the title
        deployProgress.deploymentMessage.focus();
    };

    var __displayDeploymentError = function(errorHosts) {
        var errorsOnHosts = messages.ONE_ERROR_ONE_HOST;
        var hosts = [];
        var errorMsg = null;
        var firstErrorResult = errorHosts[0];
        hosts.push(firstErrorResult.host);

        if (errorHosts.length > 1) {
            errorsOnHosts = messages.ONE_ERROR_MULTIPLE_HOST;
            var errorResult = errorHosts[0];

            // ToDo: validate the codes when SM has implemented the new error message
            var msgToCompare = errorResult.message;
            for (var i = 1; i < errorHosts.length; i++) {
                var result = errorHosts[i];
                if (result.message !== msgToCompare) {
                    errorsOnHosts = messages.MULTIPLE_ERROR_MULTIPLE_HOST;
                }
                // display 3 hosts only and the rest is displayed as a count
                if (hosts.length < 3) {
                    hosts.push(result.host);
                }
            }
        }

        deployProgress.deployErrorsTitle.html(errorsOnHosts);
        deployProgress.deployErrorsTitle.attr("title", errorsOnHosts);
//        deployProgress.deployErrorsTitle.attr("aria-label", errorsOnHosts);

        var hostString = hosts.toString().replace(/,/g, ", "); // global substitute
        if (errorHosts.length > 3) {
           hostString = utils.formatString(messages.ERROR_HOSTS, [hostString, errorHosts.length - 3]);
        }
        $("#deployErrorsHosts").html(hostString);
        $("#deployErrorsHosts").attr("title", hostString);
//        $("#deployErrorsHosts").attr("aria-label", hostString);

        deployProgress.backgroundTasksButton.prop("tabindex", "0");
        deployProgress.backgroundTasksButton.prop("title", messages.ERROR_VIEW_DETAILS);
        deployProgress.backgroundTasksButton.prop("aria-label", messages.ERROR_VIEW_DETAILS);

        utils.checkIfToolboxExists().then(function(response){
            if(response === true){
                __linkToExploreToolBgTasks(deployProgress.backgroundTasksButton);
            }
            else{
                deployProgress.backgroundTasksButton.hide();
            }
        });

        $("#deployErrorsFooter").addClass("deployErrorsFooterWhite");
        deployProgress.deployErrors.attr("aria-label", errorsOnHosts + " " + hostString);
        deployProgress.deployErrors.attr("tabindex", "0");
        deployProgress.deployErrors.show();
    };

    var __displayProgressCompleted = function(percentCompleted) {
        // Percent of deployment complete
        $("#deployProgressPercentComplete").html(utils.formatString(messages.DEPLOY_PERCENTAGE, [percentCompleted]));
        // Change the deploy progress bar's loaded color
        $("#deployProgressBarLoaded").width(percentCompleted + '%');
    };

    // Display status/errors when deployment fails
    var __displayDeployModal = function() {
        // display only if it is not visible
//        if (deployProgress.modal.is(":visible") === false) {
            deployProgress.modal.show();

            utils.disableTemplate();

            deployProgress.modalContainer.addClass("modalFooterContainer"); // Necessary, this is so the modal and footer will move and stay together *after* the user deploys

            var viewDeployedServers = "<a href='#' class='link'><u>" + messages.VIEW_DEPLOYED_SERVERS + "</u></a>";
            $("#deploymentMessageSubContent").html(viewDeployedServers);
            $("#deploymentMessageSubContent").attr("aria-label", viewDeployedServers);

            // Name of deployment and the number of hosts deploying to
            var selected = dockerUtils.getAllSelectedImages().join(", "); // Returns an array of the selected images. Join concatenates them as one string
            var selectedHosts = hostUtils.getSelectedHosts();
            if (!selected) {
                selected = fileUtils.getSelectedPackages();
            }
            var progressCount = utils.formatString(messages.DEPLOYING_IMAGE, [selected, selectedHosts.length]);
            $("#deployProgressCount").html(progressCount);
            utils.setBidiTextDirection($("#deployProgressCount"));
            $("#deployProgressCount").attr("title", progressCount);

            __displayProgressCompleted(0);

            deployProgress.deployErrors.hide();

            // When the error modal pops up, disable clicking on anything in #contents
            // To restore it, set 'pointerEvents' to 'auto'
            //$("#contents").css({pointerEvents: "none"});
            $("#contents, #review").addClass("underlay");
            $('html, body').animate({
                scrollTop: deployProgress.modal.offset().top
            }, 500);

            deployProgress.deployMessageTitle.focus();
    };

    var _showDeploymentInProgressMessage = function(){
        $("#deployProgressCircle, #deployProgressCircleText").removeClass("hidden");
        $("#deployProgressCircle").addClass('deploySpinner');
        $("deployProgressCircleText").html(messages.DEPLOYING);
        __displayDeploymentMessage();
    };

    var __allowDeployment = function(body, needToUpload, filePath, hostNames) {
        // Submit deployment
        apiUtils.postDeployment(body, needToUpload, filePath, hostNames).done(function(id) {
            deployment.id = id;
            if(needToUpload){
              _showDeploymentInProgressMessage();
            }
        }).fail(function(errorResponse) {
            var error;
            if (errorResponse) {
                if (errorResponse.responseJSON && errorResponse.responseJSON.stackTrace) {
                    var stack = errorResponse.responseJSON.stackTrace;
                    error = apiMsgUtils.findErrorMsg(stack);
                    if (error) {
                        __displayDeploymentFailed(error);
                        return;
                    } else {
                        error = apiMsgUtils.firstLineOfStackTrace(stack);
                        if (error) {
                            __displayDeploymentFailed(error);
                            return;
                        }
                    }
                }
                if (errorResponse.responseText) {
                    var response = errorResponse.responseText;
                    error = apiMsgUtils.findErrorMsg(response);
                    if (error) {
                        __displayDeploymentFailed(error);
                    } else {
                        error = apiMsgUtils.firstLineOfStackTrace(response);
                        if (error) {
                            __displayDeploymentFailed(error);
                        } else {
                            __displayDeploymentFailed(response);
                        }
                    }
                    return;
                }
                if (errorResponse.statusText) {
                    __displayDeploymentFailed(errorResponse.statusText);
                    return;
                }
            }
            __displayDeploymentFailed("");
        });
    };

    // Hides the modal and clears the modal content for the next deployment, re-enables the main form
    var __hideModalWindow = function() {
        // Enable form inputs and links
        utils.enableForm();
        utils.enableTemplate();

        // Todo remove once the template is delivered
        $("#ruleSelector").prop("disabled", false);

        // Update deploy button label
        $("#deployButtonContent").html(messages.DEPLOY);

        deployProgress.deployMessageButtonHeader.html("").hide();

        // Reset the deploy message vertical bar color
        deployProgress.deployMessageContent.removeClass("failureMessageVerticalBar");

        // Disable tabbing to modal buttons
        $("#deploymentMessageTitle, #deploymentMessageContentWrapper, #deploymentMessageContent, #deployMessageButton, #deployErrors, #backgroundTasksButton").prop("tabindex", "-1");

        // Remove the modal status SVG
        $("#modalSVG").remove();

        // Reset the button listeners
        deployProgress.deployMessageButton.attr('onclick','').unbind('click');
        deployProgress.deployMessageButton.attr('keydown','').unbind('keydown');
        deploymentButtonHasOnClick = false;

        deployProgress.backgroundTasksButton.attr('onclick','').unbind('click');
        deployProgress.backgroundTasksButton.attr('keydown','').unbind('keydown');

        deployProgress.modalContainer.removeClass("modalFooterContainer");
        deployProgress.modal.hide();

        // Validate the form to reset the deploy button and showMissingParameters message correctly
        validateUtils.validate();

        $('html, body').animate({
            scrollTop: 0
        }, 500);
    };

    // Check current deployments and update the progress
    window.setInterval(function() {
        if (deployment.id && !deployment.checkInProgress) {
            // don't want to issue another results call if we're still in the middle of handling the earlier call
            deployment.checkInProgress = true;
            apiUtils.retrieveDeploymentResults(deployment).done(function(response) {
                // Render results
                var resultList = response.results;
                var errorHost = [];
                var completedHost = [];
                for(var i = 0; i < resultList.length; i++) {

                    //var table = $("<table></table>");
                    var currentResult = resultList[i];
                    for(var key in currentResult) {
                        if (currentResult.hasOwnProperty(key)) {
                            if (key.toLowerCase() === "status") {

                                if (currentResult[key].toLowerCase() === "error") {
                                    errorHost.push(currentResult);
                                } else if (currentResult[key].toLowerCase() === "finished") {
                                    completedHost.push(currentResult.host);
                                }
                            }
                        }
                    }
                }
                var percentCompleted = (completedHost.length + errorHost.length) / resultList.length * 100;
                if (percentCompleted > 100) { // just in case
                    percentCompleted = 100;
                }
                __displayProgressCompleted(percentCompleted);

                var hasErrors;
                if (errorHost.length > 0) {
                    // console.error(errorHost);
                    hasErrors = true;
                    __displayDeploymentError(errorHost);
                } else {
                    deployProgress.deployErrors.hide();
                }
                if (percentCompleted === 100) {
                    // we're done
                    __displayDeploymentComplete(hasErrors);
                    deployment.id = null;
                }
                deployment.checkInProgress = null;
            }).fail(function(error) {
                deployment.checkinProgress = null;
                // ToDo: if the results API call fails, is displayDeploymentFailed the right call to display error?
                __displayDeploymentFailed(error);
            });
        }
    }, 5000);

    return {
        initializeModalReferences: __initializeModalReferences,
        allowDeployment: __allowDeployment,
        displayDeployModal: __displayDeployModal,
        showDeploymentInProgressMessage: _showDeploymentInProgressMessage,
        hideModalWindow: __hideModalWindow,
        displayDeploymentError: __displayDeploymentError,
        displayDeploymentFailed: __displayDeploymentFailed,
        displayDeploymentMessage: __displayDeploymentMessage,
        displayDeploymentComplete: __displayDeploymentComplete,
        displayProgressCompleted: __displayProgressCompleted
    };

})();
