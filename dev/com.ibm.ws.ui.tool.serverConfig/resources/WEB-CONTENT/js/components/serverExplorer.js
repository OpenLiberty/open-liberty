/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

var serverExplorer = (function() {
    "use strict";

    var cachedServerList = null;
    var cachedFilteredServerList = null;
    var filterApplied = false;
    var isFooterSticky = false;

    var renderServerExplorer = function() {
        var deferred = new $.Deferred();

        retrieveCollectiveServerList().done(function(serverList) {

            // Initialize filtered list with complete set of servers
            cachedFilteredServerList = cachedServerList;

            // Initially sort servers by name
            handleSort("name");

            renderServerList();

            deferred.resolve();
        }).fail(function() {
            deferred.reject();
        });

        // Clear filter
        var serverExplorerSearchInput = $("#serverExplorerSearchInput");
        serverExplorerSearchInput.val("");
        $("#serverExplorerClearButton").addClass("hidden");
        $("#serverExplorerSearchButton").attr("disabled", "disabled");

        // Handle bidi
        if(globalization.isBidiEnabled()) {
            var dirValue = globalization.getBidiTextDirection();
            if(dirValue !== "contextual") {
                serverExplorerSearchInput.attr("dir", dirValue);
            }
        }

        return deferred;
    };


    var retrieveCollectiveServerList = function() {
        var deferred = new $.Deferred();
        if(cachedServerList !== null && cachedServerList !== undefined) {
            deferred.resolve();
        } else {
            collectiveUtils.retrieveCollectiveServerList().done(function(serverList) {
                cachedServerList = serverList;
                deferred.resolve();
            }).fail(function() {
                deferred.reject();
            });
        }

        return deferred;
    };


    var renderServerList = function() {

        var serverExplorerTableBody = $("#serverExplorerTableBody");
        serverExplorerTableBody.empty();

        var serverCount = cachedFilteredServerList.length;
        var title = "";

        if(serverCount === 1) {
            title = editorMessages.ONE_SERVER;
        } else {
            title = stringUtils.formatString(editorMessages.SERVERS, [serverCount]);
        }

        if(serverCount > 500) {
            core.showControlById("serverExplorerTableTitleLimit");
        } else {
            core.hideControlById("serverExplorerTableTitleLimit");
        }

        $("#serverExplorerTableTitleText").text(title);

        var displayWarning = false;
        for(var i = 0; i < serverCount && i < 500; i++) {
            var server = cachedFilteredServerList[i];
            if(isSupportedServer(server)) {
                createSupportedServerEntry(server, serverExplorerTableBody, $("#serverExplorerSearchInput").val());
            } else {
                displayWarning = true;
                createUnsupportedServerEntry(server, serverExplorerTableBody, $("#serverExplorerSearchInput").val());
            }
        }
        if(displayWarning) {
            createWarningMessage(serverExplorerTableBody);
        }
    };


    var applySearchFilter = function() {

        // Obtain search string
        var searchString = $("#serverExplorerSearchInput").val();

        // Only apply search string when provided
        if(searchString.length > 0) {

            var searchStr = searchString.toLowerCase();  // case insensitive search

            // Update filter flag
            filterApplied = true;

            // Initialize list
            cachedFilteredServerList = [];

            // Apply filter string
            for(var i = 0; i < cachedServerList.length; i++) {
                var currentServer = cachedServerList[i];
                if(currentServer.name.toLowerCase().indexOf(searchStr) !== -1 || (currentServer.cluster && currentServer.cluster.toLowerCase().indexOf(searchStr) !== -1) ||
                    currentServer.host.toLowerCase().indexOf(searchStr) !== -1 || currentServer.wlpUserDir.toLowerCase().indexOf(searchStr) !== -1) {
                    cachedFilteredServerList.push(currentServer);
                }
            }
        } else {

            // Update filter flag
            filterApplied = false;

            // Unfiltered list if no search string is provided
            cachedFilteredServerList = cachedServerList;

        }
    };

    var createUnsupportedServerEntry = function(server, serverExplorerTableBody, searchFilter) {
        createServerEntry(server, serverExplorerTableBody, searchFilter, false);
    };
    var createSupportedServerEntry =  function(server, serverExplorerTableBody, searchFilter) {
        createServerEntry(server, serverExplorerTableBody, searchFilter, true);
    };
    var createServerEntry = function(server, serverExplorerTableBody, searchFilter, supportedServer) {
        var cluster = server.cluster;

        // Cluster might be null
        if(!cluster) {
            cluster = "";
        }

        // Handle bidi
        var bidiDirMarkupServerName = "";
        var bidiDirMarkupCluster = "";
        var bidiDirMarkupHost = "";
        var bidiDirMarkupUserDir = "";
        if(globalization.isBidiEnabled()) {
            var dirValue = globalization.getBidiTextDirection();
            if(dirValue === "contextual") {
                bidiDirMarkupServerName = "dir = \"" + globalization.obtainContextualDir(server.name) + "\"";
                bidiDirMarkupCluster = "dir = \"" + globalization.obtainContextualDir(cluster) + "\"";
                bidiDirMarkupHost = "dir = \"" + globalization.obtainContextualDir(server.host) + "\"";

                // TODO: special handling for user dir
                bidiDirMarkupUserDir = "dir = \"" + globalization.obtainContextualDir(server.wlpUserDir) + "\"";

            } else {
                bidiDirMarkupServerName = bidiDirMarkupCluster = bidiDirMarkupHost = bidiDirMarkupUserDir = "dir = \"" + dirValue + "\"";
            }
        }

        // Create server entry
        var spanInsert = "<span class=\"sr-only\">" + stringUtils.formatString(editorMessages.SERVER_TABLE_CELL_FOR_SCREEN_READER, [cluster, server.host, server.wlpUserDir]) + "</span>";
        var serverEntry = $("<tr tabindex=\"0\" class=\"serverExplorerServerEntry\" >" +
                            "    <td><span " + bidiDirMarkupServerName + ">" + (searchFilter.length > 0? markSearchFilter(server.name, searchFilter) : server.name) + "</span>" + spanInsert +"</td>" +
                            "    <td><span " + bidiDirMarkupCluster + ">" + (searchFilter.length > 0? markSearchFilter(cluster, searchFilter) : cluster) + "</span></td>" +
                            "    <td><span " + bidiDirMarkupHost + ">" + (searchFilter.length > 0? markSearchFilter(server.host, searchFilter) : server.host) + "</span></td>" +
                            "    <td><span " + bidiDirMarkupUserDir + ">" + (searchFilter.length > 0? markSearchFilter(server.wlpUserDir, searchFilter) : server.wlpUserDir) + "</span></td>" +
                            "</tr>");

        // Associate server ID with entry
        serverEntry.data("serverId", server.id);

        if(! supportedServer) {
            serverEntry.addClass("unsupportedServer");
        }

        // Add entry to table
        serverExplorerTableBody.append(serverEntry);
    };


    var markSearchFilter = function(value, searchFilter) {

        // Initialize value
        var markedValue = value;

        // Set for case insensitive search
        var valuelc = value.toLowerCase();
        var searchFilterlc = searchFilter.toLowerCase();

        // Enclose each occurence of searchFilter (case insensitive) in value
        var index = valuelc.indexOf(searchFilterlc);
        if (index !== -1 ) {
            var startIndex = 0;
            markedValue = '';  // re-initialize the return value to create string with markings.
            while(index !== -1) {
                markedValue += (value.substring(startIndex, index) + "<mark>" + value.substring(index, index + searchFilter.length) + "</mark>");
                var i = valuelc.indexOf(searchFilterlc, index + searchFilterlc.length);
                if (i === -1) {  // no more occurrences of search Filter in the string
                    markedValue += value.substring(index + searchFilterlc.length);
                } else {
                    startIndex = index + 1;
                }
                index = i;
            }
        }
        return markedValue;
    };


    var handleSort = function(key, descending) {
        cachedFilteredServerList.sort(function(a, b) {
            var valueA = a[key];
            var valueB = b[key];
            if(!valueA) {
                valueA = "";
            }
            if(!valueB) {
                valueB = "";
            }
            if(descending) {
                return (valueA < valueB)? 1 : (valueA > valueB)? -1 : 0;
            } else {
                return (valueA > valueB)? 1 : (valueA < valueB)? -1 : 0;
            }
        });
    };

    var createWarningMessage = function(serverExplorerTableBody) {
        // Append the warning message about server config support
        var message = editorMessages.SUPPORT_MESSAGE;
        var warning = $("#serverExplorerTableFooter").text(message);
    };

    var isSupportedServer = function(server) {
        // *************************
        // Is server on docker?
        var containerType = server.containerType;
        var isDockerContainer = false;
        if(containerType) {
            isDockerContainer = containerType.toUpperCase() === 'Docker'.toUpperCase();
        }

        // *************************
        // Is it a liberty server?
        var runtimeType = server.runtimeType; // never is null
        var isLiberty = runtimeType.toUpperCase() === 'Liberty'.toUpperCase();

        // *************************
        // Is the server a collective controller?
        var isCollectiveController = false;
        if(server.isCollectiveController) {
            isCollectiveController = server.isCollectiveController;
        }

        // *************************
        // Is the server a collective controller or collective member?
        var isServerStopped = server.state;

        // *************************
        // Is this a supported server?
        if(! isLiberty) {
            return false;
        } else if(isLiberty && ! isDockerContainer) {
            // All non-docker liberty servers are supported
            return true;
        } else if(isCollectiveController && isDockerContainer) {
            // Collective controllers on docker are supported
            return true;
        } else if(isDockerContainer) {
            // Collective members on docker are not supported.
            // If the server is in the list, that implies it is a collective member
            return false;
        }

        return true;
    };

    $(document).ready(function() {

        // Handle header outline in webkit (mouse)
        $("#serverExplorerTable").on("mousedown", function(event) {
            if("WebkitAppearance" in document.documentElement.style) {
                $("#serverExplorerTable").addClass("noOutline");
            }
        });


        // Handle header outline in webkit (keyboard)
        $("#serverExplorerTable").on("keydown", function(event) {
            if(event.keyCode !== 16 && event.keyCode !== 17 && event.keyCode !== 18 && "WebkitAppearance" in document.documentElement.style) {
                $("#serverExplorerTable").removeClass("noOutline");
            }
        });


        // Handle sort
        $("#serverExplorer").on("click", ".columnHeader", function(event) {
            event.preventDefault();
            var header = $(this);
            if(header.hasClass("ascending") || header.hasClass("descending")) {
                header.toggleClass("ascending descending");
            } else {
                header.addClass("ascending");
            }
            $("#serverExplorer .columnHeader").not(this).removeClass("ascending descending");

            handleSort($(this).data("key"), header.hasClass("descending"));
            renderServerList();
        });


        // Handle server selection
        $("#serverExplorerTableBody").on("click", "tr.serverExplorerServerEntry", function(event) {
            event.preventDefault();
            var serverId = $(event.currentTarget).data("serverId");
            core.setServer(serverId);
        });


        $("#serverExplorerTableBody").on("keydown", "tr", function(event) {
            if(event.keyCode === 13) {
                event.preventDefault();
                $(event.currentTarget).trigger("click");
            }
        });


        // Handle back to server list
        $("#navbarChangeServerButton").on("click", function(event) {
            event.preventDefault();
            core.clearServer();
        });


        // Handle enter key on search field
        $("#serverExplorerSearchInput").on("keydown", function(event) {
            if(event.keyCode === 13) {
                event.preventDefault();
                var serverExplorerSearchButton = $("#serverExplorerSearchButton");
                if(serverExplorerSearchButton.attr("disabled") !== "disabled") {
                    serverExplorerSearchButton.trigger("click");
                }
            }
        });


        // Handle enabling of search button depending on value in search field
        $("#serverExplorerSearchInput").on("input", function(event) {

            // Handle bidi
            if(globalization.isBidiEnabled() && globalization.getBidiTextDirection() === "contextual") {
                $(this).attr("dir", globalization.obtainContextualDir(event.currentTarget.value));
            }

            var serverExplorerSearchButton = $("#serverExplorerSearchButton");
            if($(this).val().length > 0) {
                serverExplorerSearchButton.removeAttr("disabled");
                serverExplorerSearchButton.removeAttr("tabIndex");
                core.showControlById("serverExplorerClearButton");
            } else if(!filterApplied) {
                serverExplorerSearchButton.attr("disabled", "disabled");
                serverExplorerSearchButton.attr("tabIndex", "-1");
                core.hideControlById("serverExplorerClearButton");
            }
        });


        // Handle search button
        $("#serverExplorerSearchButton").on("click", function(event) {
            event.preventDefault();
            applySearchFilter();
            renderServerList();

            // Update clear and search buttons
            if($("#serverExplorerSearchInput").val().length === 0) {
                $("#serverExplorerClearButton").addClass("hidden");
                $(this).attr("disabled", "disabled");
            }
        });


        // Handle clear search button
        $("#serverExplorerClearButton").on("click", function(event) {
            event.preventDefault();
            $("#serverExplorerSearchInput").val("");
            $("#serverExplorerSearchButton").trigger("click");
        });

    });

    $(window).resize(function() {
        // Make sure the footer is always in view,
        // no matter how big/small the size of the
        // browser window.
        makeFooterVisible();
    });

    function isElementInViewport (element) {
        // Determine if the element is outside of the window viewing area
        element = element[0]; // jQuery element
        var rect = element.getBoundingClientRect();
        var h = $(window).height();
        return (
            rect.top >= 0 &&
            rect.left >= 0 &&
            rect.bottom <= h
        );
    }

    function isScrollBarPresent() {
        // Determine if the scroll bar is present
        if ($("body").height() > $(window).height()) {
            return true;
        }
        return false;
    }

    function makeFooterVisible() {
        var footerSpace = $("#serverExplorerTableFooterSpace");
        var footer = $("#serverExplorerTableFooter");
        var isFooterInView = isElementInViewport(footer);
        var isScrollBarInView = isScrollBarPresent();
        if(! isFooterSticky && ! isFooterInView) {
            // The footer is off screen and is not stuck to the bottom
            // of the browser.  Let's make the footer always visible,
            // by making the footer stick to the bottom of the browser viewing
            // area
            footer.addClass("stickyFooter");
            isFooterSticky = true;

            // Add whitespace to avoid the banner from covering up the last element in the list.
            // This whitespace will push the last element above the message, when scrolled
            // to the very bottom of the server list.
            if(isScrollBarInView) {
                // The footerSpace is only used to avoid the footer
                // overlapping server items in the list when the view
                // is scrollable
                footerSpace.show();
            }
        } else if(isFooterSticky && ! isScrollBarInView) {
            // Make the footer look like the last item in the server list.
            // This means that it is possible the footer is NOT attached to
            // the bottom of the browser view.
            footerSpace.hide();
            footer.removeClass("stickyFooter");
            isFooterSticky = false;
        } else if(isFooterInView && ! isScrollBarInView) {
            // Since all the content is in view (no scrollbar), there
            // should not be any reason for the footer to overlap the
            // servers in the table.  Hide the space for the footer
            footerSpace.hide();
        }
    }

    return {
        renderServerExplorer: renderServerExplorer,
        makeFooterVisible : makeFooterVisible
    };

})();
