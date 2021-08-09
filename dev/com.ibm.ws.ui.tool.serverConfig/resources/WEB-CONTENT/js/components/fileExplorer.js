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

var fileExplorer = (function() {
    "use strict";

    var renderFileExplorer = function() {
        var deferred = new $.Deferred();

        if (!window.globalIsAdmin) {
            // User is not Administration role and therefore output should be read-only
            renderReadOnlyUserWarningMessage();
        } else if (fileUtils.isServerReadOnly()) {
            // Show read only warning when applicable
            renderReadOnlyWarningMessage();
        }

        // Obtain file explorer
        var fileExplorerContent = $("#fileExplorerContent");

        // Clear file explorer
        fileExplorerContent.empty();

        // Check if config dropins folders for defaults and overrides exist
        var configDropinsDefaultsExistsDeferred = fileUtils.fileExists(constants.CONFIG_DROPINS_DEFAULTS_DIRECTORY);
        var configDropinsOverridesExistsDeferred = fileUtils.fileExists(constants.CONFIG_DROPINS_OVERRIDES_DIRECTORY);

        $.when(configDropinsDefaultsExistsDeferred, configDropinsOverridesExistsDeferred).done(function(configDropinsDefaultsExists, configDropinsOverridesExists) {

            var defaultsDeferred = null;
            var overridesDeferred = null;

            // If defaults folder exists, retrieve file list
            if(configDropinsDefaultsExists) {
                defaultsDeferred = fileUtils.retrieveFileList(constants.CONFIG_DROPINS_DEFAULTS_DIRECTORY, false, ".xml");
            } else {
                defaultsDeferred = new $.Deferred().resolve(null);
            }

            // If overrides folder exists, retrieve file list
            if(configDropinsOverridesExists) {
                overridesDeferred = fileUtils.retrieveFileList(constants.CONFIG_DROPINS_OVERRIDES_DIRECTORY, false, ".xml");
            } else {
                overridesDeferred = new $.Deferred().resolve(null);
            }

            $.when(defaultsDeferred, overridesDeferred).done(function(defaults, overrides) {
                // Counters for defaults and overrides
                var defaultsCounter = 0;
                var overridesCounter = 0;

                // List of files to retrieve and render
                var deferredArray = [];

                // Add all default files to list
                if(defaults) {
                    defaultsCounter = defaults.length;
                    for(var i = 0; i < defaultsCounter; i++) {
                        deferredArray.push(retrieveTreeRoot(constants.CONFIG_DROPINS_DEFAULTS_DIRECTORY + "/" + defaults[i]));
                    }
                }

                // Add server.xml to list
                deferredArray.push(retrieveTreeRoot("${server.config.dir}/server.xml"));

                // Add override files to list
                if(overrides) {
                    overridesCounter = overrides.length;
                    /* jshint shadow:true */
                    for(var i = 0; i < overridesCounter; i++) {
                        deferredArray.push(retrieveTreeRoot(constants.CONFIG_DROPINS_OVERRIDES_DIRECTORY + "/" + overrides[i]));
                    }
                    /* jshint shadow:false */
                }

                // Once all files have been retrieved, render them
                $.when.apply($, deferredArray).done(function() {

                    // Add sections when applicable
                    if(defaultsCounter > 0) {
                        renderSeparator(editorMessages.DEFAULTS);
                    } else if(overridesCounter > 0) {
                        renderSeparator(editorMessages.PRIMARY);
                    }


                    for(var i = 0; i < arguments.length; i++) {

                        // Add sections when applicable
                        if(defaultsCounter > 0 && i === defaultsCounter) {
                            renderSeparator(editorMessages.PRIMARY);
                        } else if(overridesCounter > 0 && i === defaultsCounter + 1) {
                            renderSeparator(editorMessages.OVERRIDES);
                        }

                        // Render file
                        renderFile(arguments[i]);
                    }
                    deferred.resolve();
                }).fail(function() {
                    deferred.reject();
                });
            }).fail(function() {
                deferred.reject();
            });
        });

        return deferred;
    };


    var renderSeparator = function(title) {
        var fileExplorerContent = $("#fileExplorerContent");
        var separator = $("<div class=\"separator\">" + title + "</div>");
        fileExplorerContent.append(separator);
    };


    var retrieveTreeRoot = function(filePath) {
        var deferred = new $.Deferred();
        fileUtils.retrieveFile(filePath).done(function(file) {
            var treeRoot = {
                    location: filePath,
                    resolvedLocationWithVariable: filePath,
                    resolvedLocation: file.resolvedPath,
                    editable: !file.isReadOnly && window.globalIsAdmin,
                    includeLocations: parseIncludeLocations(file.content)
            };
            deferred.resolve(treeRoot);
        }).fail(function() {
            deferred.reject();
        });
        return deferred;
    };


    var renderFile = function(treeNode, parent) {

        // Obtain file explorer content
        var fileExplorerContent = $("#fileExplorerContent");

        // Placeholders
        var expandButton = "";
        var fileAnnotation = "";
        var openButtonState = "";
        var fileLocation = "";

        // Obtain file name
        var fileName = fileUtils.getFileFromFilePath(treeNode.location);


        // Obtain file location
        if(treeNode.resolvedLocationWithVariable !== null && treeNode.resolvedLocationWithVariable !== undefined) {
            fileLocation = fileUtils.getPathFromFilePath(treeNode.resolvedLocationWithVariable);
        } else {
            fileLocation = fileUtils.getPathFromFilePath(treeNode.location);
        }

        if(treeNode.resolvedLocation) {
            if(!treeNode.editable) {
                // Set decoration to read only of file is not editable
                fileAnnotation =
                    "<div class=\"fileExplorerAnnotation\">" +
                    "    <img src=\"img/readonly-DT.png\" alt=\"" + editorMessages.READ_ONLY + "\">" +
                    "    <span class=\"titleDescription fileExplorerAnnotationText\">" + editorMessages.READ_ONLY + "</span>" +
                    "</div>";
            }
            if(treeNode.includeLocations !== null && treeNode.includeLocations !== undefined && treeNode.includeLocations.length > 0) {
                // Replace expand button place holder with actual button
                expandButton = "<button href=\"#\" class=\"fileExplorerToggleExpansionButton\" aria-label='" + editorMessages.EXPAND + "' title=\"" + editorMessages.EXPAND + "\" alt=\"" + editorMessages.EXPAND + "\"></button>";
            }
        } else {
            // If file location could not be resolved, disable open button and add unaccessible decorator
            openButtonState = "disabled=\"disabled\"";
            var unaccessibleMessage = treeNode.fileMissing? editorMessages.FILE_NOT_FOUND : editorMessages.RESTRICTED_OR_UNAVAILABLE;
            fileAnnotation =
                "<div class=\"fileExplorerAnnotation\">" +
                "    <img src=\"img/warning-D.png\" alt=\"" + editorMessages.RESTRICTED_OR_UNAVAILABLE + "\">" +
                "    <span class=\"titleDescription fileExplorerAnnotationText\">" + unaccessibleMessage + "</span>" +
                "</div>";
        }

        // Obtain file hyperlink
        var hyperlink = core.getFileURL(treeNode.resolvedLocationWithVariable);

        var listItem =
            $("<div class=\"list-group-item fileExplorerFileListItem collapsed\">" +
            "    <div class=\"fileExplorerFileListItemContainer\">" +
            "        <div class=\"fileExplorerExpandCollapseButton\">" +
            "            " + expandButton +
            "        </div>" +
            "        <div class=\"fileExplorerLeftColumn\">" +
            "            <div class=\"fileExplorerLeftColumnLabel\"><a href=\"" + hyperlink + "\" " + openButtonState + " class=\"fileExplorerFileName\" role=\"button\" alt=\"" + fileName + "\">" + fileName + "</a></div>" +
            "            <div class=\"fileExplorerLeftColumnLabel\">" +
            "                <img src=\"img/folder-DT.png\" alt=\"" + editorMessages.LOCATION + "\">" +
            "                <span class=\"typography_titleDescription fileExplorerFolder\">" + fileLocation + "</span>" +
            "            </div>" +
            "        </div>" +
            "        <div class=\"pull-right fileExplorerRightColumn\">" +
            "            " + fileAnnotation +
            "        </div>" +
            "    </div>" +
            "</div>");

        // Calculate indentation based tree depth
        var currentParent = treeNode.parent;
        var level = 0;
        while(currentParent !== null && currentParent !== undefined) {
            level++;
            currentParent = currentParent.parent;
        }

        // Apply indentation to list item
        $(".fileExplorerFileListItemContainer",listItem).css("padding-left", 20 + 50 * level + "px");

        // Associate tree node with list item
        listItem.data("treeNode", treeNode);

        // Append list item to file explorer container in the corresponding location
        if(parent === null || parent === undefined) {
            fileExplorerContent.append(listItem);
        } else {
            parent.after(listItem);
        }

    };


    var toggleTreeNode = function(parentFileContainer) {
        var toggleButton = $(".fileExplorerToggleExpansionButton", parentFileContainer);
        var span = $("span", toggleButton);

        // Disable button
        toggleButton.addClass("disabled");
        toggleButton.attr("title", editorMessages.LOADING);

        var treeNode = parentFileContainer.data("treeNode");
        if(parentFileContainer.hasClass("collapsed")) {

            // Expand
            if(parentFileContainer.hasClass("loaded")) {
                // cached
                expandTreeNodes(treeNode);
                toggleButton.attr("title", editorMessages.COLLAPSE);
                toggleButton.attr("alt", editorMessages.COLLAPSE);
                span.text(editorMessages.COLLAPSE);
                parentFileContainer.removeClass("collapsed");
                toggleButton.removeClass("disabled");

            } else {

                // retrieve
                resolveIncludes(treeNode).done(function() {
                    for(var i = treeNode.includes.length - 1; i >= 0; i--) {
                        renderFile(treeNode.includes[i], parentFileContainer);
                    }
                    parentFileContainer.removeClass("collapsed");
                    parentFileContainer.addClass("loaded");
                    toggleButton.attr("title", editorMessages.COLLAPSE);
                    toggleButton.attr("alt", editorMessages.COLLAPSE);
                    span.text(editorMessages.COLLAPSE);
                    toggleButton.removeClass("disabled");

                }).fail(function() {
                    toggleButton.removeClass("disabled");
                    core.renderMessage(editorMessages.ERROR_ACCESSING_INCLUDE_FILES, "danger", true);
                });
            }

        } else {
            // Collapse
            $("#fileExplorerContent div.fileExplorerFileListItem").filter(function(){
                return isDescendant($(this).data("treeNode"), treeNode);
            }).addClass("hidden");

            toggleButton.attr("title", editorMessages.EXPAND);
            toggleButton.attr("alt", editorMessages.EXPAND);
            span.text(editorMessages.EXPAND);
            parentFileContainer.addClass("collapsed");
            toggleButton.removeClass("disabled");
        }
    };


    var parseIncludeLocations = function(fileContent) {
        var elementStack = [];
        var includes = [];
        var currentOffset = 0;
        var length = fileContent.length;
        var tagIndex = fileContent.indexOf("<");
        while(tagIndex !== -1 && tagIndex < length) {
            var tagNameEndIndex = fileContent.substring(tagIndex + 1).search("\\s|>");
            if(tagNameEndIndex !== -1) {
                var tagName = fileContent.substring(tagIndex + 1, tagIndex + tagNameEndIndex + 1);
                if(tagName.length > 0) {
                    var firstChar = tagName.charCodeAt(0);
                    if(firstChar === 47) {
                        elementStack.pop();
                    } else if((firstChar >= 65 && firstChar <= 90) || (firstChar >= 97 && firstChar <= 122)) {
                        var closingTagIndex = fileContent.indexOf(">", tagIndex + 1);
                        if(!(closingTagIndex !== -1 && closingTagIndex < length && fileContent.charAt(closingTagIndex - 1) === "/")) {
                            elementStack.push(tagName);
                        }
                        if(tagName === "include" && elementStack.length === 1 && elementStack[0] === "server" && !isCommented(fileContent, tagIndex)) {
                            var locationAttribute = fileContent.indexOf("location", tagIndex + 1);
                            if(locationAttribute !== -1) {
                                var locationAttributeStart = fileContent.indexOf("\"", locationAttribute + 8);
                                if(locationAttributeStart !== -1) {
                                    var locationAttributeEnd = fileContent.indexOf("\"", locationAttributeStart + 1);
                                    if(locationAttributeEnd !== -1) {
                                        includes.push(fileContent.substring(locationAttributeStart + 1, locationAttributeEnd));
                                    }
                                }
                            }
                        }
                    }
                }
            }
            currentOffset = tagIndex + 1;
            tagIndex = fileContent.indexOf("<", currentOffset);
        }
        return includes;
    };


    var isCommented = function(fileContent, offset) {
        var openCommentIndex = fileContent.substring(0, offset).lastIndexOf("<!--");
        if(openCommentIndex !== -1) {
            var closeCommentIndex = fileContent.substring(0, offset).lastIndexOf("-->");
            if(closeCommentIndex < openCommentIndex) {
                return true;
            }
        }
        return false;
    };


    var resolveIncludes = function(treeNode) {
        var deferred = new $.Deferred();
        var contextLocation = fileUtils.getPathFromFilePath(treeNode.resolvedLocation);
        fileUtils.resolveIncludeLocations(contextLocation, treeNode.includeLocations).done(function(resolvedIncludes) {
            var deferredCounter = resolvedIncludes.length;
            resolvedIncludes.forEach(function(resolvedInclude) {
                resolvedInclude.parent = treeNode;
                if(resolvedInclude.resolvedLocation !== null && resolvedInclude.resolvedLocation !== undefined) {
                    resolvedInclude.resolvedLocationWithVariable = fileUtils.applyVariablesToFilePath(resolvedInclude.resolvedLocation);
                    fileUtils.retrieveFileContent(resolvedInclude.resolvedLocation).done(function(content) {
                        resolvedInclude.content = content;
                        resolvedInclude.includeLocations = parseIncludeLocations(content);
                    }).always(function() {
                        deferredCounter--;
                        if(deferredCounter === 0) {
                            treeNode.includes = resolvedIncludes;
                            deferred.resolve();
                        }
                    });
                } else {
                    // Attempt to resolve include file directory
                    var locationPath = fileUtils.getPathFromFilePath(resolvedInclude.location);
                    fileUtils.resolveIncludeLocation(contextLocation, locationPath).done(function(result) {
                        resolvedInclude.fileMissing = true;
                    }).fail(function() {
                        resolvedInclude.fileMissing = false;
                    }).always(function() {
                        deferredCounter--;
                        if(deferredCounter === 0) {
                          treeNode.includes = resolvedIncludes;
                            deferred.resolve();
                        }
                    });
                }
            });
        }).fail(function() {
            deferred.reject();
        });
        return deferred;
    };


    var expandTreeNodes = function(treeNode) {
        $("#fileExplorerContent div.fileExplorerFileListItem").filter(function() {
            return $(this).data("treeNode").parent === treeNode;
        }).each(function(index) {
            var current = $(this);
            current.removeClass("hidden");
            if(!current.hasClass("collapsed")) {
                expandTreeNodes(current.data("treeNode"));
            }
        });
    };


    var isDescendant = function(treeNode, ancestorTreeNode) {
        while(treeNode !== null && treeNode !== undefined) {
            if(treeNode.parent === ancestorTreeNode) {
                return true;
            } else {
                treeNode = treeNode.parent;
            }
        }
        return false;
    };


    var renderReadOnlyWarningMessage = function() {
        var message = editorMessages.READ_ONLY_WARNING_MESSAGE +
        "<br><div class=\"codeFragment\">" +
        "&lt;remoteFileAccess&gt;<br>" +
        "&nbsp;&nbsp;&nbsp;&lt;writeDir&gt;<span class=\"literal\">${server.config.dir}</span>&lt;/writeDir&gt;</br>" +
        "&lt;/remoteFileAccess&gt;" +
        "<div>";
        core.renderMessage(message, "warning", true);
    };

    var renderReadOnlyUserWarningMessage = function() {
        core.renderMessage(editorMessages.NO_ROLE_MESSAGE, "warning", true);
    };

    $(document).ready(function() {

        // Handle include file expand/collapse
        $("#fileExplorerContent").on("click", "button.fileExplorerToggleExpansionButton", function(event) {
            event.preventDefault();
            var link = $(event.currentTarget);
            if(!link.hasClass("disabled")) {
                console.log("link", link);
                console.log("links parents", link.parents());
                var fileContainer = link.parents().eq(2); // Select the third generation parent
                toggleTreeNode(fileContainer);
            }
        });


        // Handle opening files
        $("#fileExplorerContent").on("click", "a.fileExplorerFileName", function(event) {
            if((event.which === 0 || event.which === 1) && !event.ctrlKey && !event.shiftKey) {
                event.preventDefault();
                var link = $(event.currentTarget);
                console.log(link.parents());
                var fileContainer = link.parents().eq(3); // Select the file Container div which is the fourth generation parent
                var treeNode = fileContainer.data("treeNode");
                core.setFile(treeNode.resolvedLocationWithVariable);
            }
        });


        // Handle tooltips for truncated labels
        $("#fileExplorerContent").on("mouseenter", ".fileExplorerLeftColumnLabel", function(event) {
            var label = event.currentTarget;
            var folder = $(".fileExplorerFolder", label);
            if(label.offsetWidth < label.scrollWidth) {
                if(folder.length) {
                    folder.attr("title", folder.text());
                } else {
                    label.title = label.innerHTML;
                }
            } else {
                if(folder.length) {
                    folder.removeAttr("title");
                } else {
                    $(label).removeAttr("title");
                }
            }
        });

    });

    return {
        renderFileExplorer: renderFileExplorer
    };

})();
