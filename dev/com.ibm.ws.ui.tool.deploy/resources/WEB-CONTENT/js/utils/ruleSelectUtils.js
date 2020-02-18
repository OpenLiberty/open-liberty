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

var ruleSelectUtils = (function() {
    "use strict";

    var allRules = [];

    var selectedIndex;
    var selectedRule;

    var runtimeSet = {};
    var containerSet = {};
    var runtimeContainers = {};
    var typeMap = {};

    var defaultRuntimeList = $("#default-runtime-list-items");
    var customRuntimeList = $("#custom-runtime-list-items");
    var defaultPackageList = $("#default-container-list-items");
    var customPackageList = $("#custom-container-list-items");

    var renderRuleSelector = function() {
        var stockRuntimes = [];
        // Create set of runtimeTypes to remove duplicate runtime types
        var runtime;
        for (var index in this.allRules) {
            if(this.allRules.hasOwnProperty(index)){
                var rule = this.allRules[index];

                if (rule.runtimeTypes && rule.packageType) {
                    for (var i in rule.runtimeTypes) {
                        if(rule.runtimeTypes.hasOwnProperty(i)){
                            runtime = rule.runtimeTypes[i].displayName;
                            runtimeSet[runtime] = runtime;

                            // create set of stock runtimes
                            if (rule['default'] && $.inArray(runtime, stockRuntimes) === -1) {
                                stockRuntimes.push(runtime);
                            }
                        }
                    }

                } else if (rule.type) {
                    if (rule.type === 'docker') {
                        runtimeSet.Liberty = 'Liberty';
                    } else {
                        //also includes Liberty and Node.js types
                        runtimeSet[rule.type] = rule.type;
                    }
                }
            }
        }

        // Create left side items - runtime types
        for (runtime in runtimeSet) {
            if ($.inArray(runtime, stockRuntimes) !== -1) { //is stock runtime
                defaultRuntimeList.append(__createListItem(runtime, 'server', true));
            } else {
                customRuntimeList.append(__createListItem(runtime, 'server', false));
            }
        }
        defaultRuntimeList.sort(function(a, b) {
            return (a > b)? 1 : (a < b)? -1 : 0;
        });
        customRuntimeList.sort(function(a, b) {
            return (a > b)? 1 : (a < b)? -1 : 0;
        });
        ruleSelect.App.bindServerEvents();
    };

    var __getPersistedParameters = function(){
       return this.persistedParameters;
     };

     var __setPersistedParameters = function(ruleId, persistedParameters){
       if(!this.persistedParameters){
         this.persistedParameters = {};
       }
       this.persistedParameters[ruleId] = persistedParameters;
     };

    // Create right side items based on runtime selection - package types
    var generateContainerList = function(runtime, isNavBarClick) {
        // If the user clicks the packageType from the navBar and the lists haven't changed then don't generate them again
        if(isNavBarClick && (defaultPackageList.children() || customPackageList.children())){
          return;
        }
        defaultPackageList.empty();
        customPackageList.empty();
        var defaultPackageSet = {};
        var hasCustomRule = false;

        var index, rule;
        //Remove duplicate rules, add custom rules to list
        for (index in this.allRules) {
            if(this.allRules.hasOwnProperty(index)){
                rule = this.allRules[index];
                var container = __getPackageFromRule(rule);
                if (__ruleContainsRuntime(runtime, rule)) {
                    if (rule['default']) { //DEFAULT RULE
                        defaultPackageSet[container] = index;
                    } else { //CUSTOM RULE
                        hasCustomRule = true;
                        customPackageList.append(__createListItem(rule.id, 'deploy', rule['default'], rule.description, index));
                    }
                }
            }
        }

        //Add default rules to list
        for (var pack in defaultPackageSet) {
            if(defaultPackageSet.hasOwnProperty(pack)){
                index = defaultPackageSet[pack];
                defaultPackageList.append(__createListItem(pack, 'deploy', rule['default'], '', index));
            }
        }


        defaultPackageList.sort(function(a, b) {
            return (a > b)? 1 : (a < b)? -1 : 0;
        });

        customPackageList.sort(function(a, b) {
            return (a > b)? 1 : (a < b)? -1 : 0;
        });

        //Add message if no custom containers
        if (!hasCustomRule) {
            customPackageList.append(__createListItem('', '', '', messages.RULESELECT_CUSTOM_INFO));
        }

        // Add multi-line ellipses to custom rule descriptions
        $(customPackageList.children()).each(function(customPackage) {
            var description = this.firstChild.lastChild;
            if(description instanceof HTMLElement) {
                $(description).dotdotdot({
                    watch: "window"
                });
            }
        });

        ruleSelect.App.shouldScrollShowOnDeployLists();
        ruleSelect.App.bindDeployEvents();
    };

    var __createListItem = function(name, type, def, desc, index) {
        var listItem = document.createElement('li');
        var a = __createAnchor(name, type);

        if (type === 'server') {
            a = __createAnchor(name, type, def, name); //name is really runtimeType. desc can be used as runtimeType if it needs to be different
            a.innerHTML = name;
        } else if (type === 'deploy') {
            a = __createAnchor(name, type, def);
            a.setAttribute('data-index', index);

            if (desc) { // custom rule will pass in desc
                var titleSpan = document.createElement('span');
                titleSpan.innerHTML = name;
                var descSpan = document.createElement('span');
                descSpan.innerHTML = desc;

                a.appendChild(titleSpan);
                a.appendChild(descSpan);
            } else {
                a.innerHTML = name;
            }
        } else { // type === '', this is for message on custom rules
            a = document.createElement('span');

            var text = document.createElement('span');
            text.innerHTML = desc;

            // Learn More link disabled for now
//            var link = document.createElement('a'); // link for Learn More
//            link.setAttribute('href', 'https://www.ibm.com/support/knowledgecenter/en/was_beta_liberty/com.ibm.websphere.wlp.nd.multiplatform.doc/ae/rwlp_config_deployRule.html');
//            link.setAttribute('id', 'customLearnMore');
//            link.innerHTML = messages['RULESELECT_CUSTOM_INFO_LINK'];

            a.appendChild(text);
//            a.appendChild(link);
        }

        listItem.appendChild(a);
        return listItem;
    };

    var __createAnchor = function(name, type, def, runtimeType) {
        var a = document.createElement('a');
        a.setAttribute('href', '#');
        a.setAttribute('id', type + "-" + utils.replaceSpaces(name));
        a.setAttribute('title', name);
        a.setAttribute('data-type', type);
        a.setAttribute('role', 'button');

        a.setAttribute('data-breadcrumb-color', __getDataBreadcrumbColor(name));
        a.setAttribute('data-breadcrumb-hover-color', __getDataBreadcrumbHoverColor(name));
        a.setAttribute('data-bg-color', __getDataBgColor(name));
        a.setAttribute('data-text-color', __getDataTextColor(name));
        a.setAttribute('data-li-color', __getDataLiColor(name));
        a.setAttribute('data-next-color', __getDataNextColor(name));

        if (def) {
            a.setAttribute('data-rule', 'default');
        } else {
            a.setAttribute('data-rule', 'custom');
        }

        if (type === 'server' && runtimeType) {
            a.setAttribute('data-runtime', runtimeType);  //runtimeType is same as name
        }

        if (type === 'deploy') {

            //create a short name for data-title
            // should limit to X characters (max length to show in smaller font on selection panel | also for breadcrumb display)
                    //text size for selection panel will be determined based on length

            a.setAttribute('data-title', name); //TODO: function for shortening name? but what about auto-scaling text size...
        }
        return a;
    };

    var __getDataBreadcrumbColor = function(title) {
        switch (title) {
        case 'Liberty':
            return 'D7AAFF';
        case 'Node.js':
            return 'B4E051';
        case messages.APPLICATION_PACKAGE:
        case messages.SERVER_PACKAGE:
        case 'Application Package':
        case 'Server Package':
            return 'FF9EEE';
        case messages.DOCKER_CONTAINER:
        case 'Docker Container':
            return '7CC7FF';
        default:
            return 'FDD600';
        }
    };

    var __getDataBreadcrumbHoverColor = function(title) {
        switch (title) {
        case 'Liberty':
            return 'EED2FF';
        case 'Node.js':
            return 'C8F08F';
        case messages.APPLICATION_PACKAGE:
        case messages.SERVER_PACKAGE:
        case 'Application Package':
        case 'Server Package':
            return 'FFD2FF';
        case messages.DOCKER_CONTAINER:
        case 'Docker Container':
            return 'C0E6FF';
        default:
            return 'FDE876';
        }
    };

    var __getDataBgColor = function(title) {
        switch (title) {
        case 'Liberty':
            return '562E71';
        case 'Node.js':
            return '4B853D';
        case messages.APPLICATION_PACKAGE:
        case messages.SERVER_PACKAGE:
        case 'Application Package':
        case 'Server Package':
            return 'A6266E';
        case messages.DOCKER_CONTAINER:
        case 'Docker Container':
            return '325C80';
        default:
            return 'EFC100';
        }
    };

    var __getDataTextColor = function(title) {
        switch (title) {
        case 'Liberty':
            return 'BA8FF7';
        case 'Node.js':
            return 'C8F08F';
        case messages.APPLICATION_PACKAGE:
        case messages.SERVER_PACKAGE:
        case 'Application Package':
        case 'Server Package':
            return 'FF9EEE';
        case messages.DOCKER_CONTAINER:
        case 'Docker Container':
            return '7CC7FF';
        default:
            return '323232';
        }
    };

    var __getDataLiColor = function(title) {
        switch (title) {
        case 'Liberty':
            return 'AF6EE8';
        case 'Node.js':
            return '8CD211';
        case messages.APPLICATION_PACKAGE:
        case messages.SERVER_PACKAGE:
        case 'Application Package':
        case 'Server Package':
            return 'FF71D4';
        case messages.DOCKER_CONTAINER:
        case 'Docker Container':
            return '5AAAFA';
        default:
            return 'FDD500';
        }
    };

    var __getDataNextColor = function(title) {
        switch (title) {
        case 'Liberty':
            return 'BA8FF7';
        case 'Node.js':
            return 'C8F08F';
        case messages.APPLICATION_PACKAGE:
        case messages.SERVER_PACKAGE:
        case 'Application Package':
        case 'Server Package':
            return 'FF9EEE';
        case messages.DOCKER_CONTAINER:
        case 'Docker Container':
            return '7CC7FF';
        default:
            return 'FDE876';
        }
    };

    var __ruleContainsRuntime = function (runtime, rule) {
        if (rule.runtimeTypes.length > 0 && rule.packageType) {
            for (var index in rule.runtimeTypes) {
                if(rule.runtimeTypes.hasOwnProperty(index)){
                    var rt = rule.runtimeTypes[index].displayName;
                    if (runtime === rt) {
                        return true;
                    }
                }
            }
        } else if (rule.type) {
            if (rule.type === 'docker') {
                return (runtime === 'Liberty') ? true : false;
            } else {
                // also includes Liberty and Node.js matching
                return (runtime === rule.type) ? true : false;
            }
        }

        return false; //if cannot find
    };

    var __getPackageFromRule = function(rule) {
        //used when retrieving persistence for correct colors and array indexing.
        // may not need to be translated
        if (rule['default'] === false) { //custom rule
            return rule.id;
        }

        if (rule.runtimeTypes && rule.packageType) {
            return rule.packageType;
        } else if (rule.type) {
            if (rule.type === 'docker') {
                return messages.DOCKER_CONTAINER;
            } else if (rule.type === 'Node.js') {
                return messages.APPLICATION_PACKAGE;
            } else if (rule.type === 'Liberty') {
                return messages.SERVER_PACKAGE;
            }
        }
    };

    /**
     * retrieves extra information on persisted data for the rule selector to render
     *  returns null if cannot find ID or runtime does not match up
     */
    var getPersistentData = function(id, runtime) { //TODO: rename - this method gets information based on persisted id
        var data = {};
        var index = __getIndexFromId(id, this.allRules);
        if (index !== -1) { // if found ID in allRules
            var rule = this.allRules[index];
            if (!__ruleContainsRuntime(runtime, rule)) { // if rule does not contain runtime (if runtime changes or so)
                return null;
            }

            var runtimeColor = __getDataBreadcrumbColor(runtime);

            data.server = { 'dataTitle': runtime, 'dataHoverColor': runtimeColor };

            var container = __getPackageFromRule(rule);
            var containerColor = __getDataBreadcrumbColor(container);

            data.deploy = {'dataTitle': container, 'dataHoverColor': containerColor };
            return data;
        } else {
            return null;
        }
    };

    var __getIndexFromId = function(id, rules) {
        for (var index in rules) {
            if(rules.hasOwnProperty(index)){
                var rule = rules[index];
                if (rule.id === id) { //found matching id in allRules
                    return index;
                }
            }
        }

        return -1; // if no matches, return -1
    };
    var __getIdFromIndex = function(index) {
        return this.allRules[index].id;
    };

    var __getSelectedRule = function() {
        return this.selectedRule;
    };

    var __isCustomRule = function() {
      return !(this.selectedRule.id === "Liberty Server Rule" || this.selectedRule.id === "Node.js Server Rule" || this.selectedRule.id === "Liberty Docker Rule");
    };

    var __getSelectedPackageType = function() {
      return this.selectedRule.packageType;
    };

    /*
     * Render all of the cards from the rule
     */
    var renderRule = function(index, persistedParameters) {

        //If index is the rule id, find the actual index
        if (isNaN(index)) { //index is rule id
            var ruleName = index;
            index = __getIndexFromId(ruleName, this.allRules);
            if (isNaN(index)) {
                console.error("Could not find rule", index);
            } else {
                console.log("Found rule", ruleName, "at index", index);
            }
        }

        card.clearAndHideOldCards();

        var inputVariablesContainer = $(".inputVariablesContainer");
        var securityInputContainer = $("#securityInputContainer");
        var securityInputConfirmContainer = $("#securityInputConfirmContainer");

        // Obtain selected rule
        this.selectedRule = this.allRules[index];
        console.log("Selected rule", this.selectedRule);

        // Build parameters object from the persisted values
        var params = {};
        if(persistedParameters){
          for(var i = 0; i < persistedParameters.length; i++){
            var obj = persistedParameters[i];

            var persistedObject = {};
            persistedObject.value = obj.value;
            persistedObject.local = obj.local;
            params[obj.name] = persistedObject;
          }
        }

         // Render input variables
        /* jshint shadow:true */
          var inputVariables = this.selectedRule.inputVariables;
          if(inputVariables) {
              for(var i = 0; i < inputVariables.length; i++) {
                  var inputVariable = inputVariables[i];
                  var name = inputVariable.name;

                  // Create unique id
                  var id = idUtils.getInputId(inputVariable);

                  // If the data used from the last deployment was persisted, check for this input's value
                  var value, local;
                  if(params[name]){
                    value = params[name].value;
                    if(params[name].local){
                      local = params[name].local;
                    }
                  }

                  if(inputVariable.type !== "password"){
                      var deplParamsCard = card.create(inputVariable);
                      if (deplParamsCard.hasClass("hidden")) {
                          deplParamsCard.removeClass("hidden");
                      }
                  }

                  if (inputVariable.group) {
                      var groupId = idUtils.getGroupId(inputVariable.group);
                      $("#" + groupId + "_parameterBanner").html(
                              utils.formatString(messages.GROUP_DEPLOYMENT_PARAMETERS, [groupId]));
                  }

                  var cardInputContainerId = idUtils.getCardInputContainerId(inputVariable);
                  console.log("cardInputContainerId ", cardInputContainerId);

                  // query for variablesInputContainer
                  inputVariablesContainer = $("#" + cardInputContainerId);
                  console.log("inputVariablesContainer ", inputVariablesContainer);

                  switch(inputVariable.type){
                      case "password":
                          card.addInputToContainer(securityInputContainer, inputVariable, "", securityInputConfirmContainer);
                          $("#security").show();
                          break;
                      case "filePath":
                          card.addInputToContainer(inputVariablesContainer, inputVariable, value);
                          break;
                      case "file":
                          card.addInputToContainer(inputVariablesContainer, inputVariable, value, null, local);
                          $("#" + idUtils.getCardId(inputVariable)).addClass("parametersGrayBackground");
                          $("#" + idUtils.getBrowseAndUploadId(inputVariable)).removeClass("hidden");
                          break;
                      case "dockerImage":
                      case "cluster":
                      case "String":
                          card.addInputToContainer(inputVariablesContainer, inputVariable, value);
                          break;
                      default:
                          card.addInputToContainer(inputVariablesContainer, inputVariable, value);
                          break;
                  }
              }
          }

          // Adjust card heights to accomodate the hidden inputs when toggling between local/remote deployments
          var parameterCards = $('.parameters:visible');
          parameterCards.each(function(i, card){
              var $card = $(card);
              var height = $card.outerHeight();
              $card.css('min-height', height);

              // This should usually not need to add any height to the card because everything is visible when checking the height. The toggle is switched to
              // local or remote after a deferred comes back with the user's persisted choice so this calculation is done just in case it comes back
              // before this calculation is done and things are hidden before this is called.
              var heightToAdd = 0;
              var hiddenInputs = $card.find('.inputDiv:hidden, .parametersInputLabel:hidden, .parametersInputDescription:hidden');
              hiddenInputs.each(function(){
                heightToAdd += $(this).outerHeight();
              });
              if(heightToAdd > 0){
                height += heightToAdd;
                $card.class('min-height', height);
              }
          });

          // Add dependency listeners based on the input's defaultValue. This calls the setDependencyListener() in string.js
          for (var i=0; i<inputVariables.length; i++) {
              var inputVariable = inputVariables[i];
              var id = idUtils.getInputId(inputVariable);
              var input = $('#' + id).get(0);
              var typeObject = string.retrieveObjectForType(input);
              console.log(typeObject);
              typeObject.setDependencyListener();
          }
          card.addLabelListener();
    };

    return {
        getSelectedRule: __getSelectedRule,
        isCustomRule: __isCustomRule,
        getSelectedPackageType: __getSelectedPackageType,
        allRules: allRules,
        renderRuleSelector: renderRuleSelector,
        renderRule: renderRule,
        getPersistedParameters: __getPersistedParameters,
        setPersistedParameters: __setPersistedParameters,
        generateContainerList: generateContainerList,
        getPersistentData: getPersistentData,
        getIdFromIndex: __getIdFromIndex
    };

})();
