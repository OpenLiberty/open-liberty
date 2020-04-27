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

var deployTool = (function() {
    "use strict";

    var renderStrings = function() {
        // Parameters section message to use a file already on the collective controller
        var previousFileLink = "<a href='#' style='text-decoration: none'>" + messages.FILE_UPLOAD_PREVIOUS + "</a>";
        $(".uploadPreviousFile").html(previousFileLink);

        hostUtils.changeHostSearchUrl();

        // Render the footer message by creating a link to another deploy in a new tab

        $("#footerButton").prop("title", messages.FOOTER_BUTTON_MESSAGE);
        $("#footerButton").on("click", function(e){
            e.stopPropagation();
            window.open(window.parent.location.href, '_blank');
        });
        $('#footerButton').keypress(function(e){
            if(e.which === 13){ //Enter key
                $('#footerButton').click();
            }
        });
    };

    var deploy = function() {
        utils.disableForm();

        // Update deploy button label
        $("#deployButtonContent").html(messages.DEPLOYING);

        // Obtain selected rule
        var selectedRule = ruleSelectUtils.getSelectedRule();

        // Create body for REST service call
        var body = {};
        var data = {};
        var needToUpload = false;
        var file = null;
        var fileName = null;

        // Add rule and host information
        data.rule = selectedRule.id;
        data.hosts = hostUtils.getSelectedHosts();

        var hostNames = "";
        data.hosts.forEach(function(host){
            hostNames += (host + ",");
        });
        hostNames = hostNames.substring(0, hostNames.length-1); // Remove trailing comma

        // Add input variable information
        data.variables = [];
        var selectedPackages = [];
        var inputImages = [];
        // ToDo: once card is cloned, need to change how to get inputs here
        var inputs = $(".inputVariablesContainer input:visible");
        var persistedParameters = [];
        var i, input, variable, typeObject, filePath = null;
        for(i = 0; i < inputs.length; i++) {
            input = inputs.get(i);
            typeObject = string.retrieveObjectForType(input);

            variable = {};
            variable.name = typeObject.inputVariable.name;
            variable.value = input.value;

            // Make a shallow copy of the variable so the persisted values don't get encoded
            var persistedVariable = {};
            var persistVariable = true;
            persistedVariable.name = variable.name;
            persistedVariable.value = variable.value;


            if(typeObject.inputVariable.type === "filePath"){
                variable.value = encodeURIComponent(input.value);
                if(typeObject.inputVariable.name === "targetDir" || typeObject.inputVariable.name === "applicationDir"){
                    filePath = input.value;
                }
            } else if (typeObject.inputVariable.type === "file") {
                // it is easier to get the final package name from the input
                // Check if the file is already on the controller or needs to be uploaded
                var deployPackage = fileUtils.getFile(typeObject.fileBrowserAndUploadId);
                if(deployPackage){
                    file = deployPackage;
                    fileName = file.name;
                    persistVariable = false;
                    needToUpload = true;
                }
                else{
                    file = input.value;
                    persistedVariable.local = true;
                }
                selectedPackages.push(utils.encodeData(input.value));
            } else if (typeObject.inputVariable.type === "dockerImage") {
                // always get images from the input field instead of the image list as there may not be an image list
                inputImages.push(utils.encodeData(input.value));
            }


            // check if input placeholder exists
            var placeholder = $("#" + input.id).attr("placeholder");
            if (!variable.value && placeholder !== undefined) {
                //console.log("value is empy, use placeholder value");
                variable.value = input.placeholder;
//                filePath = input.placeholder; // might not set filepath here
            }
            data.variables.push(variable);
            if(persistVariable){
              persistedParameters.push(persistedVariable);
            }
          }

        // Save the user's deployment parameters on the server side for pre-populating the form the next time they deploy
        // Using key: deploy rule's id, value: deploy variables and their respective values
        var deployRule = ruleSelectUtils.getSelectedRule();
        var persist = {};
        persist[deployRule.id] = persistedParameters;
        userConfig.save("persistedParameters", persist);

        fileUtils.setSelectedPackages(selectedPackages);
        dockerUtils.setInputImages(inputImages);

        var securityInputs = $("#securityInputContainer input");
        for(i = 0; i < securityInputs.length; i++){
            input = securityInputs.get(i);
            variable = {};
            typeObject = string.retrieveObjectForType(input);
            variable.name = typeObject.inputVariable.name;
            variable.value = input.value;
            data.variables.push(variable);
        }

        // If the file needs to be uplaoded to the controller then construct its path including the file name
        if(needToUpload){
          if(!filePath){
            filePath = "/placeholder/"; // The file transfer service requires a filePath param in the Deployment api call. It is unused by the deploy service.
          }
          else if(filePath.charAt(filePath.length-1) !== "/" && filePath.charAt(filePath.length-1) !== "\\"){
              filePath += "/";
          }

          // Do not send serverPackageDir for a remote Liberty deployment since the deploy service uses the file transfer service instead of needing to know where the file is
          if(ruleSelectUtils.getSelectedRule().name === "Liberty Server" || ruleSelectUtils.getSelectedRule().name === "Node.js Server"){
            for(i = 0; i < data.variables.length; i++){
              if(data.variables[i].name === "serverPackageDir" || data.variables[i].name === "applicationDir"){
                  data.variables.splice(i, 1);
                  break;
              }
            }
          }

          filePath = encodeURIComponent(filePath + fileName);
          body.file = file;
          body.data = data;

          fileUtils.displayFileUploadStatus(fileName);
          deployProgress.displayDeployModal();
          deployProgress.allowDeployment(body, needToUpload, filePath, hostNames);
        }
        else{
            body = data;

            if (fileUtils.getUploadInProgressCount() > 0) {
                // path when waiting for file upload to finish
                fileUtils.setDeploymentBody(body);
                //deployment.body = body;
                deployProgress.displayDeployModal();
                fileUtils.checkUploadProgress(null, null, null);
            } else {
                // path to perform deploy
                deployProgress.showDeploymentInProgressMessage();
                deployProgress.displayDeployModal();
                deployProgress.allowDeployment(body, needToUpload, null, hostNames);
            }
        }
    };

    $(document).ready(function() {

        $(".listSearchIcon").append(imgUtils.getSVGSmallObject('search'));
        $("#searchHostIcon").append(imgUtils.getSVGSmallObject('search'));
        $(".fileBrowseStatusIcon").append(imgUtils.getSVGSmallObject('upload'));

        $("#ruleSelector").on("change", function() {
            var description = "";
            var index = $("#ruleSelector").prop("selectedIndex");
            var rule = ruleSelectUtils.allRules[index];
            if(rule.description) {
                description = rule.description;
            }
            $("#ruleDescription").html(description);
            ruleSelectUtils.renderRule(index);
            validateUtils.validate();
        });

        //Review and Deploy Section
        $("#review").on("click", "#showMissingParameters", function(event) {
            event.preventDefault();

            // The new design is to show the missing/invalid input when missing input link is clicked.
            // Field input validation will only take care of enabling/disabling the review/deploy.
            validateUtils.validate(true, true);

            var inputErrorFound = false;
            // ToDo: again use card css to get all inputs once card is cloned
            var inputs = $(".inputVariablesContainer input:visible");
            var selectedHosts = hostUtils.getSelectedHosts();
            var i, input;
            for(i = 0; i < inputs.length; i++) {
                input = inputs.get(i);
                if(!input.value && !input.placeholder) {
                    utils.focusInvalidInput(input);
                    inputErrorFound = true;
                    break;
                }
                // check for invalid input
                else if ($("#" + input.id + "_inputDiv").next('p').length !== 0) {
                    utils.focusInvalidInput(input);
                    inputErrorFound = true;
                    break;
                }
            }
            if (!inputErrorFound && selectedHosts.length === 0) {
                // no missing/invalid inputs and host is not selected - put input focus on host search for now
                inputErrorFound = true;
                utils.focusInvalidInput($("#searchHost"));
            }

            var securityInputs = $("#securityInputContainer input, #securityInputConfirmContainer input");
            if(!inputErrorFound && securityInputs.length > 0){
                for(i = 0; i<securityInputs.length; i++){
                    input = securityInputs.get(i);
                    if(!input.value) {
                        utils.focusInvalidInput(input);
                        inputErrorFound = true;
                        break;
                    }
                }
            }
        });

        $("#deployButton").on("click", function(event) {
            event.preventDefault();
            deploy();
        });

        // Startup
        globalization.retrieveBidiPreference().always(function() {
            globalization.retrieveExternalizedStrings().done(function() {

                var retrieveRulesPromise = apiUtils.retrieveDeploymentRules();
                var retrieveHostListPromise = apiUtils.retrieveHostList();

                hostUtils.setHostListeners();
                hostUtils.searchName(); // Set the host search to search by name by default
                renderStrings(); // can be moved
                fileUtils.renderFileBrowse(); //can be moved
                deployProgress.initializeModalReferences();

                $.when(retrieveRulesPromise).done(function(rules) {
                    ruleSelectUtils.allRules=rules;

                    userConfig.init("com.ibm.websphere.appserver.adminCenter.tool.deploy");

                    ruleSelectUtils.renderRuleSelector(); //needs retrieveRulesPromise to get data

                    console.log('loading persistence');
                    userConfig.load(function(response) {
                        //get rule ids
                        var persist = response.lastDeploy;
                        console.log('Persistent data', persist);
                        if (persist) {
                            var id = persist.id;
                            var runtime = persist.runtime;
                            var persistedParameters = response && response.persistedParameters && response.persistedParameters[id];
                            if(persistedParameters){
                              ruleSelectUtils.setPersistedParameters(id, persistedParameters);
                            }

                            //returns null if ID doesn't exist in rules.
                            var data = ruleSelectUtils.getPersistentData(id, runtime);
                            if (data === null) { // can't find persisted ID in current list of rules
                                console.log('Cannot find persisted data ID in current list of rules');
                                ruleSelect.App.init();
                                return;
                            }

                            console.log('Rendering initial rule from persistence', id);
                               ruleSelectUtils.renderRule(id, persistedParameters);
                               validateUtils.validate(); //needs retrieveRulesPromise to finish and form to load

                            console.log('persistent data - rule select information', data);
                            ruleSelect.App.init(data);

                        } else {    // persistence data DNE
                            console.log('Empty persistence data');
                            ruleSelect.App.init();
                            return;
                        }
                    },function(err) {
                        console.log('No persistent data found.');
                        ruleSelect.App.init();
                    });

                    $("#contents").show();

                }).fail(function(e) {
                    $("#initializationErrorMessage").show();
                });

                $.when(retrieveHostListPromise).done(function(){
                         // Search the URL for host params coming from the Explore tool
                        hostUtils.parseUrlHosts();
                        hostUtils.renderHosts(); //needs retrieveHostListPromise to finish
                    }).fail(function(error){
                        //TODO: show something for failing to retrieve/render hosts
                        console.log("Failed to retrieve hosts: ", error);
                    });
            }).fail(function() {
                $("#translationsErrorMessage").show();
            }); //end of globalization.retrieveExternalizedStrings()
        }); //end of globalization.retrieveBidiPreference()
    }); //end of document.ready
})();
