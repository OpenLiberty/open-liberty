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

var hostUtils = (function(){
    "use strict";

    var searchMode = ''; //set Search to Name by default
    var fullHostList = [];
    var allHosts = [];
    var selectedHosts = [];

    // Host Section: Link to Explore Tool search view to filter for more specific hosts
    // Note: location.origin is not supported in all browsers.
    var baseExploreSearchUrl = location.protocol + "//" + location.host + "/adminCenter/#explore/search/?type=host";

    /**
     * Set the hosts in the list
     * apiUtils.js calls this once the GET call is successful in retrieving the hosts
     */
    var __setHosts = function(hosts) {
        var hostNames = [];
        for (var host in hosts) {
            if(hosts.hasOwnProperty(host)){
                hostNames.push(hosts[host].name);
            }
        }
        allHosts = hostNames;
    };

    // Handler for parsing the url for hosts
    var __parseUrlHosts = function() {
        var url = location.href;
        var len = url.length;
        if(url.charAt(len-1) === "#"){
          url = url.substring(0,len-1);
        }
        var hostListIndex = url.indexOf('?hostlist=');
        if(hostListIndex > -1){
            var hostString = url.substring(hostListIndex + 10);
            // Remove other lexicon parts of the url
            if(hostString.indexOf('?') > -1){
                hostString = hostString.substring(0, hostString.indexOf('?'));
            }
            var hosts = hostString.split(',');
            hosts.forEach(function(host){
                if(allHosts.indexOf(host) > -1){
                    allHosts.splice(allHosts.indexOf(host), 1);
                    selectedHosts.push(host);
                }
            });
        }
    };

    var __setHostListeners = function() {
        //Host Selection
        $("#searchHost").on("input", function() {
            __renderHosts();
            __changeHostSearchUrl();
        });

        $("#searchNameFilter").on("click", function(){
            __searchName();
            __changeHostSearchUrl();
            __renderHosts();
        });

        $("#searchTagFilter").on("click", function(){
            __searchTag();
            __changeHostSearchUrl();
            __renderHosts();
        });

        $("#hostSelection").on("mouseenter focusin", ".hostListItem", function(event) {
            var list = event.currentTarget.parentNode.id;
            var item = event.currentTarget;
            var hostName = event.currentTarget.getAttribute("data-hostName");
//            console.log("------ setting title", hostName);
//            event.currentTarget.setAttribute("title", globalization.createBidiDisplayString(hostName));

            var svg;
            if(list === "selectedHostsList") {
                var id = $(item.children[1]).attr("id");
                $(item.children[1]).remove();
                svg = imgUtils.getSVGSmallObject('blue-selected');
                svg.setAttribute("id", id);
                svg.setAttribute("pointer-events", "none");
                $(item).append(svg);
            }
            else if(list === "allHostsList"){
                // Only show the SVG when the user hovers over the host
                svg = $(item.children[0]);
                svg.attr("visibility", "visible");
            }
        });

        $("#hostSelection").on("mouseleave focusout", ".hostListItem", function(event) {
            var list = event.currentTarget.parentNode.id;
            var item = event.currentTarget;
            var hostName = event.currentTarget.getAttribute("data-hostName");

            var id, svg;
            if(list === "allHostsList"){
                id = $(item.children[0]).attr("id");
                $(item.children[0]).remove();
                svg = imgUtils.getSVGSmallObject('add-hover');
                svg.setAttribute("id", id);
                svg.setAttribute("pointer-events", "none");
                svg.setAttribute("visibility", "hidden");
                $(item).prepend(svg);
            }
            else if(list === "selectedHostsList"){
                id = $(item.children[1]).attr("id");
                $(item.children[1]).remove();
                svg = imgUtils.getSVGSmallObject('clear-selected');
                svg.setAttribute("id", id);
                svg.setAttribute("pointer-events", "none");
                $(item).append(svg);
            }
        });

        $("#hostSelection").on("click", ".hostListItem", function(event) {
            event.preventDefault();
            var list = event.currentTarget.parentNode.id;

            var hostName = event.currentTarget.getAttribute("data-hostName");
            var index;
            if(list === "allHostsList") {
                // Remove from allHosts
                index = allHosts.indexOf(hostName);
                allHosts.splice(index, 1);

                // Add to selectedHosts
                selectedHosts.push(hostName);
            } else {
                // Remove from selectedHosts
                index = selectedHosts.indexOf(hostName);
                selectedHosts.splice(index, 1);

                // Add to allHosts
                allHosts.push(hostName);
            }
            __renderHosts();

            // Checks if the form is complete and shows the deploy button if so
            validateUtils.validate();
        });

        $("#hostSelection").on("mousedown", ".hostListItem", function(event) {
            event.preventDefault();
            var item = event.currentTarget;
            var list = event.currentTarget.parentNode.id;

            var id, svg;
            if(list === "selectedHostsList") {
                id = $(item.children[1]).attr("id");
                $(item.children[1]).remove();
                svg = imgUtils.getSVGSmallObject('blue-selected');
                svg.setAttribute("id", id);
                svg.setAttribute("pointer-events", "none");
                $(item).append(svg);
            }
            else if(list === "allHostsList"){
                id = $(item.children[0]).attr("id");
                $(item.children[0]).remove();
                svg = imgUtils.getSVGSmallObject('add-click');
                svg.setAttribute("id", id);
                svg.setAttribute("pointer-events", "none");
                $(item).prepend(svg);
            }
        });

        // Important: Don't disable event default for all cases so the enter key will work
        $("#hostSelection").on("keydown", ".hostListItem", function(e) {
            var list = e.currentTarget.parentNode.id;

            // Shift+tab key
            if(e.shiftKey && e.keyCode === 9){
                e.preventDefault();
                if(list === "allHostsList"){
                    // Send focus to the filter that is not selected if the search filters are expanded
                    if($("#searchTagFilter:visible").length){
                        $("#searchTagFilter").focus();
                    }
                    // Send focus to the search icon so that the tags may be expanded
                    else{
                        $("#searchHost").focus();
                    }
                }
                else if(list === "selectedHostsList"){
                    $("#hostSelection").focus();
                }
            }
            // Tab key
            else if(e.which === 9){
                e.preventDefault();
                if(list === "allHostsList"){
                    // Focus Explore link if the Explore Tool feature is available (won't return in selector if not available)
                    if($("#hostSearchFooter a").length){
                        $("#hostSearchFooter a").focus();
                    }
                    else if($("hostSelection").nextAll(".card:visible")[0]){
                        $("hostSelection").nextAll(".card:visible")[0].focus();
                    } else{
                        $("#review").focus();
                    }
                }
                else if(list === "selectedHostsList"){
                    $("#searchHost").focus();
                }
            }
            // Down key or right key
            else if(e.which === 39 || e.which === 40){
                e.preventDefault();
                // Focus next host if not the last host in the list
                if($(this).nextAll(".hostListItem")[0]){
                    $(this).nextAll(".hostListItem")[0].focus();
                }
            }
            // Up key or left key
            else if(e.which === 37 || e.which === 38){
                e.preventDefault();
                // Focus previous host if not the first host in the list
                if($(this).prevAll(".hostListItem")[0]){
                    $(this).prevAll(".hostListItem")[0].focus();
                }
            }
        });

        $("#searchNameFilter").on("mouseenter", function(event) {
            var $this = $(this);
            // for hover to display the full button text if truncated
            if (this.offsetWidth < this.scrollWidth && !$this.attr('title')){
                $this.attr('title', $this.text());
            }
        });

        $("#searchTagFilter").on("mouseenter", function(event) {
            var $this = $(this);
            // for hover to display the full button text if truncated
            if (this.offsetWidth < this.scrollWidth && !$this.attr('title')){
                $this.attr('title', $this.text());
            }
        });
    };

    var __resetHostList = function() {
        $("#searchHost").val("");
        __searchName();

        // Transfer selected hosts back to allHosts
        for(var i=0; i<selectedHosts.length; i++){
            allHosts.push(selectedHosts[i]);
        }

        selectedHosts = [];

        __parseUrlHosts(); // Set hosts according to the parameter in the url
        __renderHosts();
    };

    var __getAllHosts = function() {
        return allHosts;
    };

    var __getSelectedHosts = function() {
        return selectedHosts;
    };

    var __searchName = function() {
        searchMode = 'name';
        $("#searchNameFilter").attr("aria-pressed", "true");
        $("#searchTagFilter").attr("aria-pressed", "false");
        $("#searchNameFilter").addClass("searchFilterSelected");
        $("#searchTagFilter").removeClass("searchFilterSelected");
    };

    var __searchTag = function() {
        searchMode = 'tag';
        $("#searchNameFilter").attr("aria-pressed", "false");
        $("#searchTagFilter").attr("aria-pressed", "true");
        $("#searchNameFilter").removeClass("searchFilterSelected");
        $("#searchTagFilter").addClass("searchFilterSelected");
    };

    var __changeHostSearchUrl = function() {
        // Do not display the Explore Tool link unless it is available in the Admin Center
        utils.checkIfExploreToolExists().then(function(response){
            if(response === false){
                return;
            }

            var exploreSearchUrl;
            // Change the link to the Explore tool's search view to filter by name or tag depending on which they are filtering by
            var filter = $("#searchHost").val();
            if(searchMode === 'name'){
                // Host Section: Change the link to Explore Tool search view using type host and also the name filter
                exploreSearchUrl = baseExploreSearchUrl + ((filter !== "") ? ("&name=" + filter) : "");
            }
            else if(searchMode === 'tag'){
                // Host Section: Change the link to Explore Tool search view using type host and also the tag filter
                exploreSearchUrl = baseExploreSearchUrl + ((filter !== "") ? ("&tag=~eq~" + filter) : "");
            }

            var hostExploreString = utils.formatString( messages.SELECT_HOSTS_FOOTER, ["<a href='" + exploreSearchUrl + "' target='_blank' rel='noreferrer' class='link'>" + messages.EXPLORE_TOOL_INSERT  + "</a>"]);
            $("#hostSearchFooter").html(hostExploreString);
            $("#hostSearchFooter a").attr("aria-label", messages.EXPLORE_TOOL_ARIA);
        });
    };

    var __renderHosts = function() {
        // Handle bidi
        utils.setBidiTextDirection($("#searchHost"));

        // Apply filter
        var filteredHosts = [];
        var filter = $("#searchHost").val();
        var totalNumHosts = allHosts.length;
        var i;

        // Search by name
        if (searchMode === "name"){
            for(i = 0; i < allHosts.length; i++) {
                if(allHosts[i].indexOf(filter) !== -1) {
                    filteredHosts.push(allHosts[i]);
                }
            }
        }
        // Search by tag
        else if (searchMode === "tag"){
            for(i = 0; i < fullHostList.length; i++) {
                if (selectedHosts.indexOf(fullHostList[i].name) === -1) {
                    //do not show if this host is selected already
                    if (filter === '') { //search is empty
                        filteredHosts.push(fullHostList[i].name);
                    }
                    else if(fullHostList[i].tags) { //search has something
                        for(var tag = 0; tag < fullHostList[i].tags.length; tag++) {
                            if(fullHostList[i].tags[tag].indexOf(filter) !== -1){
                                filteredHosts.push(fullHostList[i].name);
                                break;
                            }
                        }
                    }
                }
            }
        }

        // Sort selected hosts
        selectedHosts.sort(function(a, b) {
            return (a > b)? 1 : (a < b)? -1 : 0;
        });

        // Sort filtered hosts
        filteredHosts.sort(function(a, b) {
            return (a > b)? 1 : (a < b)? -1 : 0;
        });

        var row, icon, hostName;

        // Populate list of selected hosts
        var selectedHostsList = $("#selectedHostsList");
        selectedHostsList.empty();
        for(i=0; i < selectedHosts.length; i++) {
            row =  document.createElement('button');
            row.setAttribute('class', 'hostListItem');
            row.setAttribute('data-hostName', selectedHosts[i]);
            row.setAttribute('id', 'hostButton_' + selectedHosts[i]);
            icon = imgUtils.getSVGSmallObject('clear-selected');
            icon.setAttribute("pointer-events", "none");
            icon.setAttribute("id", "removeSearchHostIcon_" + selectedHosts[i]);
            hostName = document.createElement('a');
            // handle bidi
            utils.setBidiTextDirection(hostName, true);
            // set hover over
            hostName.title = selectedHosts[i];
            hostName.innerHTML = selectedHosts[i];
            row.appendChild(hostName);
            row.appendChild(icon);

            selectedHostsList.append(row);
        }

        // Populate list of all hosts
        var allHostsList = $("#allHostsList");
        allHostsList.empty();
        for(i=0; i < filteredHosts.length; i++) {
            row =  document.createElement('button');
            row.setAttribute('class', 'hostListItem');
            row.setAttribute('data-hostName', filteredHosts[i]);
            row.setAttribute('id', 'hostButton_' + filteredHosts[i]);
            icon = imgUtils.getSVGSmallObject('add-hover');
            icon.setAttribute("pointer-events", "none");
            icon.setAttribute("id", "addSearchHostIcon_" + filteredHosts[i]);
            icon.setAttribute("visibility", "hidden");
            hostName = document.createElement('a');
            // handle bidi
            utils.setBidiTextDirection(hostName, true);
            // set hover over
            hostName.title = filteredHosts[i];
            hostName.innerHTML = filteredHosts[i];
            row.appendChild(icon);
            row.appendChild(hostName);

            allHostsList.append(row);
        }

        // Update banner
        var selectedHostsBannerText;
        if(selectedHosts.length === 1){
            selectedHostsBannerText = utils.formatString(messages.ONE_SELECTED_HOST, [selectedHosts.length]);
        }
        else{
            selectedHostsBannerText = utils.formatString(messages.N_SELECTED_HOSTS, [selectedHosts.length]);
        }
        $("#hostSelection").attr("aria-label" , selectedHostsBannerText);
        $("#hostSelectionBanner").html(selectedHostsBannerText);

        var hostCountText;
        if(filteredHosts.length === 1){
            hostCountText = utils.formatString(messages.ONE_HOST, [filteredHosts.length  + '/' + totalNumHosts]);
        }
        else{
            hostCountText = utils.formatString(messages.N_HOSTS, [filteredHosts.length  + '/' + totalNumHosts]);
        }

        $("#hostListCount").html(hostCountText);

        // Display the message that the user needs to pick at least one host
        if (selectedHosts.length === 0) {
            $("#selectedHostsList").empty();
            $("#selectedHostsList").append(messages.MISSING_HOST);
        }
    };



    return {
        setHosts: __setHosts,
        parseUrlHosts: __parseUrlHosts,
        resetHostList: __resetHostList,
        getAllHosts: __getAllHosts,
        getSelectedHosts: __getSelectedHosts,
        setHostListeners: __setHostListeners,
        searchName: __searchName,
        searchTag: __searchTag,
        changeHostSearchUrl: __changeHostSearchUrl,
        renderHosts: __renderHosts
    };
})();
