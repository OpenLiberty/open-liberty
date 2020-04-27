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

var cluster = (function(){
    "use strict";
    
    var ClusterType = function(inputVariable) {
        list.listType.call(this, inputVariable);
            
        // ToDo: add translation
        //$("#" + rightPaneId).closest(".parameters").attr("aria-label", messages.PARAMETERS_DOCKER_ARIA);
        //$("#listSearchField").attr("data-externalizedPlaceholder", "SEARCH_CLUSTERS");
        $('#' + this.listSearchId + "_listSearchField").attr("placeholder", messages.SEARCH_CLUSTERS);
        $('#' + this.listSearchId + "_listSearchFieldLabel").text(messages.SEARCH_CLUSTERS);
        var rule = ruleSelectUtils.getSelectedRule();
        var ruleType = "";
        if (rule.type === "Node.js") {
            ruleType = clusterUtils.ruleType.nodejs;
        } else if (rule.type === "docker") {
            ruleType = clusterUtils.ruleType.docker;
        } else if (rule.type === "Liberty") {
            ruleType = clusterUtils.ruleType.liberty;
        }

        // Call the cluster API and only show the cluster section and enable input if the images API comes back successfully
        $("#" + this.listSearchId).attr("focusId", this.id);
        
        // mock up testing
        //clusterUtils.retrieveClusters(this.listSearchId, clusterUtils.ruleType.liberty, this.id, this);
        clusterUtils.retrieveClusters(this.listSearchId, ruleType, this.id, this);
    };
    
    ClusterType.prototype = $.extend ({},
            list.listType.prototype);    
  
    ClusterType.prototype.addInputListener = function() {
        list.listType.prototype.addInputListener.call(this);
        var me = this;
        $("#" + this.id).on("focus", function(event) {
             $("#" + me.listSearchId).attr("focusId", me.id);
             switch(me.loadingStatus) {
             case me.waitStatus.loading:
                 $('#' + this.listSearchId + "_listSearchField").attr("placeholder", messages.SEARCH_CLUSTERS);
                 $('#' + this.listSearchId + "_listSearchFieldLabel").text(messages.SEARCH_CLUSTERS);
                 me.displayLoading();
                 break;
             case me.waitStatus.done: 
                    me.renderClusters();
                
                 // Remove spinner if its still there and clusters are loaded
//                 if($('#' + me.listSearchId + "_listViewList").children().length > 0){
//                     $('#' + me.listSearchId).find('.loader').removeClass("loader");
//                 }
                    // Remove spinner as it is possible to have no list item or no matching list item based on the input
                 loadingUtils.removeLoader(me.listSearchId);
                 break;
             default:
                 console.log("not a recognized status");
                 break;    
             }
        });
        
        $("#" + this.id).on("input", function(event) {
            // Clear the cluster filter so that the search doesn't override that filter
            $("#" + me.listSearchId + "_listSearchField").val("");
            
            clusterUtils.renderClusters(me.listSearchId, $(this).val());

            // No error handling is needed as it could be a new or an existing cluster
            var valid = clusterUtils.checkIfClusterExists(me.listSearchId, $("#" + me.listInputId).val());
            if(valid){
                clusterUtils.setSelectedCluster($(this).val(), me.listSearchId);
            }
            
            // Checks if the form is complete and shows the deploy button if so
            validateUtils.validate(valid);
        });
        
    };
    
    ClusterType.prototype.renderClusters = function() {
        list.setSearchFieldListener(this);
        var rightPaneId = idUtils.getCardRightPaneId(this.inputVariable); 
        $("#" + rightPaneId).closest(".parameters").attr("aria-label", messages.PARAMETERS_DOCKER_ARIA);
        $('#' + this.listSearchId + "_listSearchField").attr("placeholder", messages.SEARCH_CLUSTERS);
        $('#' + this.listSearchId + "_listSearchFieldLabel").text(messages.SEARCH_CLUSTERS);
        $("#" + this.listSearchId).attr("focusId", this.id);
        $('#' + this.listSearchId).removeClass("hidden");
        // hide the file browser if one exists
        list.hideRightPanelElements(this.inputVariable);

        clusterUtils.renderClusters(this.listSearchId, $("#" + this.listInputId).val());
    };
        
    var __create = function(inputVariable) {
        var clusterInput = new ClusterType(inputVariable);
        return clusterInput;
    };
    
    return {
        /*
         * code usage example:
         *   
         *   if (inputVariable.type === "cluster") {
         *     typeObject = cluster.create(inputVariable);
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






