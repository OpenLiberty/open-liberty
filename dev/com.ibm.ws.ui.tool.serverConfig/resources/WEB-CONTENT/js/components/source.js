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

var source = (function() {
    "use strict";
    
    var skipDirtyChange = true;
    
    var initializeSource = function(initialContent, readOnly) {
        var deferred = new $.Deferred();
        
        if(source.orionEditor !== null && source.orionEditor !== undefined) {

            // Set content
            source.orionEditor.updateText(initialContent);
            
            // Set read only mode
            source.orionEditor.editor.getTextView().setOptions({"readonly": readOnly});
            
            deferred.resolve();
            
        } else {
            var orionEditor = new orion.codeEdit();
            
            orionEditor.create({
                parent: "orionEditor",
                contents: initialContent,
                contentType: "application/xml"
                }).then(function(orionEditor) {
                    
                    // Set read only mode
                    orionEditor.readonly = readOnly;
                    
                    var contentAssistDocumentation = null;
                    
                    //orionEditor.setContents(initialContent, "application/xml");
                    orionEditor.inputManager.setAutoSaveTimeout(-1);
                    orionEditor.editor.setAnnotationRulerVisible(false);
                    orionEditor.editor.setFoldingRulerVisible(false);
                    orionEditor.editor.setOverviewRulerVisible(false);
                    
                    // Handle accessibility
                    $("#orionEditor [role='application']").attr("aria-label", editorMessages.SOURCE_EDITOR);
                    $("#orionEditor [role='textbox']").attr("aria-label", editorMessages.SOURCE_EDITOR_CONTENT);
                    $("#orionEditorContainer [role='menu']").attr("aria-label", editorMessages.SOURCE_EDITOR_MENU);
        
    
                    // Utility for programmatically updating editor
                    orionEditor.updateText = function(content) {
                        skipDirtyChange = true;
                        source.orionEditor.editor.setText(content);
                        source.orionEditor.editor.getTextView().setCaretOffset(content.length);
                        source.orionEditor.editor.getTextView().setCaretOffset(0);
                        source.orionEditor.editor.getTextView().setSelection(0,0);
                        source.orionEditor.editor.resize();
                        skipDirtyChange = true;
                        orionEditor.editor.setDirty(false);
                    };            
    
                    // Add listener for changes
                    orionEditor.editor.addEventListener("DirtyChanged", function() {
                        if(!skipDirtyChange && orionEditor.editor.isDirty()) {
                            editor.markDocumentAsDirty();
                        }
                        skipDirtyChange = false;
                    });
    
                    // Content assist provider
                    var contentAssistProvider = {
    
                            computeProposals: function(buffer, offset, context) {
    
                                // Skip proposals of overlapping with other content
                                var endSelectionChar = buffer.charAt(context.selection.end);
                                if(endSelectionChar.trim().length!== 0 && !(endSelectionChar === ">"|| endSelectionChar === "\"" || 
                                    endSelectionChar === "/" || endSelectionChar === "<")) {
                                    return [];
                                }
    
                                // Obtain element stack
                                var elementStack = getElementStack(buffer, offset);
    
                                // Calculate location context        
                                var reverseCounter = offset;
                                var insideStartTag = false;
                                var insideEndTag = false;
                                var quoted = false;
                                var assignment = false;
                                var attributeName = "";
                                var skip = false;
    
                                while(reverseCounter-- > 0) {
                                    var currentChar = buffer.charAt(reverseCounter);
                                    if(assignment) {
                                        if(currentChar.trim().length > 0) {
                                            attributeName = currentChar + attributeName;
                                        } else if(attributeName.length > 0) {
                                            break;
                                        }
                                        
                                    }
                                    if(currentChar === "\"") {
                                        if(!quoted) {
                                            quoted = true;
                                        } else {
                                            quoted = false;
                                            skip = true;
                                        }
                                    } else if(currentChar === "=" && !skip) {
                                        assignment = true;
                                    } else if(currentChar === "<" && !quoted) {
                                        insideStartTag = true;
                                        break;
                                    } else if(currentChar === "/" && !quoted) {
                                        insideEndTag = true;
                                        break;
                                    } else if(currentChar === ">" && !quoted) {
                                        break;
                                    }
                                }
                                
                                // Retrieve proposals depending on context
                                if(assignment && attributeName.length > 0) {
                                    var endQuote = buffer.charAt(context.selection.end) === "\"";
                                    return getAttributeValueProposals(attributeName, quoted, endQuote, context.prefix, elementStack);
                                } else if(insideStartTag) {
                                    // Determine if cursor is within the tag name
                                    var precedingCharacterIndex = offset - context.prefix.length - 1;
                                    if(precedingCharacterIndex > 0 && buffer.charAt(precedingCharacterIndex) === "<") {
                                        return getElementProposals(true, offset, context.prefix, elementStack);
                                    } else {
                                        if(offset > 0) {
                                            return getAttributeProposals(buffer, offset, context.prefix, elementStack);
                                        }
                                    }
                                } else if(!insideEndTag) {
                                    // Outside tag
                                    if(elementStack.length > 0) {
                                        return getElementProposals(false, offset, context.prefix, elementStack);
                                    } else {
                                        // Check if root element is present
                                        var schemaDocumentRootElement = editor.getSchemaDocumentRootElement();
                                        if(schemaDocumentRootElement !== null && schemaDocumentRootElement !== undefined) {
                                            var rootName = schemaDocumentRootElement.getAttribute("name");
                                            if(buffer.indexOf("<" + rootName) === -1) {
                                                return [{proposal: "<" + rootName + "></" + rootName + ">", escapePosition: offset + rootName.length + 2}];
                                            }                            
                                        }
                                    }
                                }
                                return [];
                            }
                        };
                    
                    // Set content assist provider
                    orionEditor.serviceRegistry.registerService("orion.edit.contentassist",
                            contentAssistProvider,
                            {    name: "xmlContentAssist",
                                contentType: ["application/xml"]
                            });
                    
                    // Calculates element location based on text
                    function getElementStack(buffer, offset) {
                        var elementStack = [];
                        var currentOffset = 0;
                        var tagIndex = buffer.indexOf("<");
                        while(tagIndex !== -1 && tagIndex < offset) {
                            var tagNameEndIndex = buffer.substring(tagIndex + 1).search("\\s|>");
                            if(tagNameEndIndex !== -1) {                            
                                var tagName = buffer.substring(tagIndex + 1, tagIndex + tagNameEndIndex + 1);
                                if(tagName.length > 0) {
                                    var firstChar = tagName.charCodeAt(0);
                                    if(firstChar === 47) {
                                        elementStack.pop();
                                    } else if((firstChar >= 65 && firstChar <= 90) || (firstChar >= 97 && firstChar <= 122)) {
                                        var closingTagIndex = buffer.indexOf(">", tagIndex + 1);
                                        if(!(closingTagIndex !== -1 && closingTagIndex < offset && buffer.charAt(closingTagIndex - 1) === "/")) {
                                            elementStack.push(tagName);
                                        }
                                    }
                                }
                            }
                            currentOffset = tagIndex + 1;    
                            tagIndex = buffer.indexOf("<", currentOffset);
                        }
                        return elementStack;
                    }
                    
                    
                    // Calculates proposals for attributes
                    function getAttributeProposals(buffer, offset, prefix, elementPath) {
                        var proposals = [];
                        var schemaElement = schemaUtils.getElementFromPath(editor.getSchemaDocumentRootElement(), elementPath);
                        if(schemaElement !== null && schemaElement !== undefined) {
                            
                            var existingAttributes = getExistingAttributes(buffer, offset, elementPath[elementPath.length - 1]);
                            var elementDeclaration = schemaUtils.resolveElementDeclaration(schemaElement);
                            
                            if(elementDeclaration !== null && elementDeclaration !== undefined) {
                                var attributes = schemaUtils.getAttributes(elementDeclaration);
                                
                                // Sort attributes by name
                                attributes.sort(function(a, b) {
                                  var valueA = a.getAttribute("name").toLowerCase();
                                  var valueB = b.getAttribute("name").toLowerCase();
                                  return (valueA > valueB)? 1 : (valueA < valueB)? -1 : 0;
                                });
                                
                                contentAssistDocumentation = [];
                                
                                for(var i = 0; i < attributes.length; i++) {
                                    
                                    var attributeName = attributes[i].getAttribute("name");
                                    
                                    if(existingAttributes.indexOf(attributeName) === -1) {
                                    
                                        var proposal = attributeName + "=\"\"";
                                        
                                        var description = " ";
                                        if(schemaUtils.isRequired(attributes[i])) {
                                            description = description + editorMessages.REQUIRED_SUFFIX;
                                        }
                                        
                                        if (attributeName.indexOf(prefix) === 0) {
                                            proposals.push({
                                                proposal: proposal.substring(prefix.length),
                                                name: proposal,
                                                description: description,
                                                escapePosition: offset + proposal.length - prefix.length - 1
                                            });
                                            
                                            var documentationLabel = schemaUtils.getLabel(attributes[i]);
                                            var documentationDescription = schemaUtils.getDocumentation(attributes[i]);
                                            
                                            
                                            var defaultValue = schemaUtils.getDefaultValue(attributes[i]);
                                            
                                            contentAssistDocumentation[proposal] = {label: documentationLabel, description: documentationDescription, defaultValue: defaultValue};
                                        }
                                    }
                                }
                            }
                        }
                        return proposals;
                    }
                    
                    
                    function getExistingAttributes(buffer, offset, elementName) {
                        var attributes = [];
                        var startIndex = buffer.substring(0, offset).lastIndexOf("<" + elementName);
                        var endIndex = buffer.substring(offset - 1).indexOf(">");
                        if(endIndex === -1) {
                            endIndex = buffer.length;
                        }
                        var fragment = buffer.substring(startIndex + elementName.length + 2, offset + endIndex - 1);
                        var index = fragment.indexOf("=");
                        while(index !== -1) {
                            var reverseCounter = index;
                            var variableName = "";
                            while(reverseCounter-- > 0) {
                                
                                var currentChar = fragment.charAt(reverseCounter);
                                
                                if(currentChar.trim().length > 0) {
                                    variableName = currentChar + variableName;
                                } else if(variableName.length > 0) {
                                    break;
                                }
                                
                            }
                            attributes.push(variableName);
                            index = fragment.indexOf("=", index + 1);
                        }
                        return attributes;
                    }
    
                    
                    // Calculates proposals for enumerations and booleans
                    function getAttributeValueProposals(attributeName, startQuote, endQuote, prefix, elementPath) {
                        var proposals = [];
                        var schemaElement = schemaUtils.getElementFromPath(editor.getSchemaDocumentRootElement(), elementPath);
                        if(schemaElement !== null && schemaElement !== undefined) {
                            var elementDeclaration = schemaUtils.resolveElementDeclaration(schemaElement);
                            var attribute = schemaUtils.getAttribute(elementDeclaration, attributeName);
                            if(attribute !== null && attribute !== undefined) {
                                var possibleValues = schemaUtils.getPossibleValues(attribute);
                                var defaultValue = schemaUtils.getDefaultValue(attribute);
                                if(possibleValues.length > 0) {
                                    contentAssistDocumentation = [];
                                    
                                    // Don't load descriptions for booleans
                                    if(possibleValues.length === 2 && 
                                            possibleValues[0][0] === possibleValues[0][1] && 
                                            possibleValues[0][0] === "true" && 
                                            possibleValues[1][0] === possibleValues[1][1] && 
                                            possibleValues[1][0] === "false") {
                                                contentAssistDocumentation = null;
                                    }
                                    
                                    for(var i = 0; i < possibleValues.length; i++) {
                                        var attributeValue = possibleValues[i][0];
                                        var proposal = (startQuote? "" : "\"") + attributeValue + (endQuote? "" : "\"");
                                        var name =  "\"" + attributeValue + "\"";
                                        var description = " ";
                                        if(defaultValue === attributeValue) {
                                            description = description + editorMessages.DEFAULT_SUFFIX;
                                        }
                                        if (attributeValue.indexOf(prefix) === 0) {
                                            proposals.push({
                                                proposal: proposal.substring(prefix.length),
                                                name: name,
                                                description: description
                                            });
                                            
                                            var documentationDescription = possibleValues[i][1];
                                            if(contentAssistDocumentation !== null && contentAssistDocumentation !== undefined) {
                                                contentAssistDocumentation[proposal] = {label: attributeValue, description: documentationDescription};
                                            }
                                        }
                                    }
                                }
                            }    
                        }
                        return proposals;
                    }
                    
                    
                    function getElementProposals(inTag, offset, prefix, elementPath) {
                        var proposals = [];
                        contentAssistDocumentation = [];
                        
                        // Map element path to schema (last element name in path might be incomplete)
                        var schemaElement = null;
                        while(schemaElement === null || schemaElement === undefined) {
                            schemaElement = schemaUtils.getElementFromPath(editor.getSchemaDocumentRootElement(), elementPath);
                            if(schemaElement === null || schemaElement === undefined) {
                                if(elementPath.length > 0) {
                                    elementPath.pop();
                                } else {
                                    return [];
                                }
                            }
                        }
                        
                        // Obtain element declaration
                        var elementDeclaration = schemaUtils.resolveElementDeclaration(schemaElement);
                        
                        // Obtain child elements
                        var childElements = schemaUtils.getChildElements(elementDeclaration);
                        
                        // Sort elements by tag name
                        childElements.sort( function(a, b) {
                            var valueA = a.getAttribute("name").toLowerCase();
                            var valueB = b.getAttribute("name").toLowerCase();
                            return (valueA > valueB)? 1 : (valueA < valueB)? -1 : 0;
                        });
    
                        for(var i = 0; i < childElements.length; i++) {
                            var requiredAttributesString = "";
                            var elementName = childElements[i].getAttribute("name");
                            
                            if (elementName.indexOf(prefix) === 0) {
                                var childElementDeclaration = schemaUtils.resolveElementDeclaration(childElements[i]);
                                if(childElementDeclaration !== null && childElementDeclaration !== undefined) {
                                    var childElementAttributes = schemaUtils.getAttributes(childElementDeclaration);
                                    for(var j = 0; j < childElementAttributes.length; j++) {
                                        if(schemaUtils.isRequired(childElementAttributes[j])) {
                                            requiredAttributesString = requiredAttributesString + " " + childElementAttributes[j].getAttribute("name") + "=\"\"";
                                        }
                                    }
                                    
                                    var emptyElement = !schemaUtils.hasChildElements(childElementDeclaration) && !schemaUtils.elementContainsText(childElementDeclaration);
                                    var proposal = (inTag? "" : "<") + elementName + requiredAttributesString + (emptyElement? " />" : "></" + elementName + ">");
                                    
                                    var escapePosition = proposal.indexOf("\"") + 1;
                                    if(escapePosition === 0) {
                                        escapePosition = proposal.indexOf(">") + 1;
                                    }
                                    
                                    proposals.push({
                                        proposal: proposal,
                                        description: " ",
                                        name: elementName,
                                        escapePosition: offset + escapePosition - prefix.length,
                                        overwrite: true
                                    });
        
                                    var documentationLabel = schemaUtils.getLabel(childElementDeclaration);
                                    var documentationDescription = schemaUtils.getDocumentation(childElementDeclaration);
                                    contentAssistDocumentation[proposal] = {label: documentationLabel, description: documentationDescription};

                                } else {
                                    proposals.push({
                                        proposal: "<" + elementName + "/>",
                                        description: " ",
                                        name: elementName,
                                        escapePosition: offset + elementName.length + 3,
                                        overwrite: true
                                    });
                                    contentAssistDocumentation["<" + elementName + "/>"] = {label: elementName, description: ""};
                                }
                            }
                        }
                        return proposals;
                    }
    
                    
                    var hoverProvider = {
                            
                            computeHoverInfo: function (editorContext, context) {
    
                                if(context.proposal) {
                                    
                                    return {
                                        
                                        title: contentAssistDocumentation[context.proposal.proposal].label,
                                        content: contentAssistDocumentation[context.proposal.proposal].description,
                                        type: "markdown"
                                    };
                                    
                                } else {
    
                                    if(context.offset) {
                                        var text = source.orionEditor.editor.getText();
                                        var elementStack = getElementStack(text, context.offset);
                                        if(elementStack.length > 0) {
                                            
                                            var schemaElement = schemaUtils.getElementFromPath(editor.getSchemaDocumentRootElement(), elementStack);
                                            if(schemaElement !== null && schemaElement !== undefined) {
                                                
                                                var elementDeclaration = schemaUtils.resolveElementDeclaration(schemaElement);
                                                if(elementDeclaration !== null && elementDeclaration !== undefined) {
                                                    var attributes = schemaUtils.getAttributes(elementDeclaration);
                                                    
                                                    
                                                    var elementName = elementStack[elementStack.length - 1];
                                                    var elementContentStartOffset = text.lastIndexOf("<" + elementName) + elementName.length + 2;
                                                    
                                                    
                                                    var reverseCounter = context.offset;
                                                    var buffer = "";
                                                    var quoteFound = false;
                                                    var currentChar;
                                                    while(reverseCounter-- > elementContentStartOffset) {
                                                        currentChar = text.charAt(reverseCounter);
                                                        if(currentChar === "\"") {
                                                            if(quoteFound) {
                                                                if(buffer.indexOf("=") === -1) {
                                                                    buffer = buffer.substring(buffer.indexOf("\"") + 1);
                                                                } 
                                                                break;
                                                            } else {
                                                                quoteFound = true;                                                
                                                            }
                                                        }
                                                        buffer = currentChar + buffer;
                                                    }
                                                    
                
                                                    var forwardCounter = context.offset - 1;
                                                    while(forwardCounter++ < text.length) {
                                                        currentChar = text.charAt(forwardCounter);
                                                        if(currentChar === "\"" || currentChar === ">" || currentChar === "=") {
                                                            break;
                                                        } else {
                                                            buffer = buffer + currentChar;
                                                        }
                                                    }
                                                    
                                                    
                                                    var equalIindex = buffer.indexOf("="); 
                                                    if(equalIindex !== -1) {
                                                        buffer = buffer.substring(0, equalIindex);
                                                    }
                                                    var quoteIndex = buffer.indexOf("\"");
                                                    if(quoteIndex !== -1) {
                                                        buffer = buffer.substring(quoteIndex + 1);
                                                    }
        
                                                    
                                                    buffer = buffer.trim();
                                                    
                                                    var attributeFound = false;
                                                    var documentationLabel, documentationDescription;
                                                    for(var i = 0; i < attributes.length; i++) {
        
                                                        if(attributes[i].getAttribute("name") === buffer) {
                                                            documentationLabel = schemaUtils.getLabel(attributes[i]);
                                                            documentationDescription = schemaUtils.getDocumentation(attributes[i]);
                                                            var defaultValue = schemaUtils.getDefaultValue(attributes[i]);
                                                            if(defaultValue !== null && defaultValue !== undefined) {
                                                                var defaultValueString = stringUtils.formatString(editorMessages.DOCUMENTATION_DEFAULT, [defaultValue]);
                                                                documentationDescription += "  \n \n" + defaultValueString;
                                                            }
                                                            
                                                            if(documentationLabel !== null && documentationLabel !== undefined && documentationDescription !== null && documentationDescription !== undefined) {
                                                                return {
                                                                    title: documentationLabel,
                                                                    content: documentationDescription,
                                                                    type: "markdown"
                                                                };
                                                            }
                                                            attributeFound = true;
                                                        }
                                                        
                                                    }
                                                    
                                                    if(!attributeFound) {
                                                        documentationLabel = schemaUtils.getLabel(elementDeclaration);
                                                        documentationDescription = schemaUtils.getDocumentation(elementDeclaration);
                                                        
                                                        if(documentationLabel !== null && documentationLabel !== undefined && documentationDescription !== null && documentationDescription !== undefined) {
                                                            return {
                                                                title: documentationLabel,
                                                                content: documentationDescription,
                                                                type: "markdown"
                                                            };
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                return null;
                            }
                    };
    
                    orionEditor.serviceRegistry.registerService("orion.edit.hover",
                            hoverProvider,
                            {    name: "xmlContentHover",
                                contentType: ["application/xml"]
                            });
    
                    source.orionEditor = orionEditor;
                    deferred.resolve();
            });
        }
        return deferred;
    };
    
    return {
        initializeSource: initializeSource,
        orionEditor: null
    };
    
})();
