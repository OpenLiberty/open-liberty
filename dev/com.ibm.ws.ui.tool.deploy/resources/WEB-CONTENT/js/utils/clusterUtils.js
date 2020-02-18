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

var clusterUtils = (function() {
    "use strict";

    var clusters = {}; // Maps each clustersRootId to its docker images and selectedCluster
    var ruleType = {
            docker: 1,
            nodejs: 2,
            liberty: 3
   };

    var __retrieveClusters = function (rootClustersSection, ruleType, inputId, clusterObject) {
        // Add a loading spinner until done loading and set the status to keep track of what to display
        if (listUtils.isInputInFocus(rootClustersSection, inputId)) {
            loadingUtils.displayLoader(rootClustersSection);
        }
        clusterObject.setLoadingStatus(clusterObject.waitStatus.loading);

        apiUtils.retrieveClusters(ruleType).done(function(clusterList) {
            clusters[rootClustersSection] = {};
            clusters[rootClustersSection].listItems = [];
            if (clusterList.length > 0) {
                clusterList.forEach(function(cluster){
                    // Do not allow duplicates
                    if(clusters[rootClustersSection].listItems.indexOf(cluster) === -1){
                      clusters[rootClustersSection].listItems.push(cluster);
                    }
                });
            }

            // set status to done
            clusterObject.setLoadingStatus(clusterObject.waitStatus.done);

            var inputInFocus = listUtils.getInputInFocus(rootClustersSection);
            if (inputInFocus === inputId) {
                // Normal case when focus is on the cluster.
                loadingUtils.removeLoader(rootClustersSection);
                __renderClusters(rootClustersSection);
            } else if (!inputInFocus) {
                // This is the case to render the cluster list when
                // 1. there is no docker image list to render for no docker configuration scenario and
                // 2. the docker image REST API comes back first and the cluster API comes back last
                // 3. the focus was initially in the image input and then unset
                clusterObject.renderClusters();
            }
        }).fail(function(error) {
            // render an empty list
            clusters[rootClustersSection] = {};
            clusters[rootClustersSection].listItems = [];

            // set status to done
            clusterObject.setLoadingStatus(clusterObject.waitStatus.done);

            if (listUtils.isInputInFocus(rootClustersSection, inputId)) {
                loadingUtils.removeLoader(rootClustersSection);
                __renderClusters(rootClustersSection);
            }
        });
    };

    /*
     * Filter the Docker images when the user types in the search field or in the image field on the left pane
     */
    var __renderClusters = function(rootClustersId, filterQuery) {
        listUtils.renderList(rootClustersId, clusters, filterQuery);
    };

    var __checkIfClusterExists = function(rootClustersId, clusterName){
        return listUtils.checkIfListItemExists(rootClustersId, clusterName, clusters);
    };

    var __clearClusters = function(){
        this.clusters = {};
    };

    var __setSelectedCluster = function(selectedCluster, rootClustersId){
        listUtils.setSelectedListItem(selectedCluster, clusters[rootClustersId]);
    };

    return {
        retrieveClusters: __retrieveClusters,
        //highlightSelectedCluster: __highlightSelectedCluster,
        //scrollToCluster: __scrollToCluster,
        renderClusters: __renderClusters,
        checkIfClusterExists: __checkIfClusterExists,
        clearClusters: __clearClusters,
        setSelectedCluster: __setSelectedCluster,
        ruleType: ruleType
    };
})();
