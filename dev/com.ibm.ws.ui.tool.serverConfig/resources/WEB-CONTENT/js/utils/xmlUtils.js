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

var xmlUtils = (function() {
    "use strict";
    
    var getElementPath = function(element) {
        var path = [];
        while(element.parentNode !== null && element.parentNode !== undefined) {
            path.unshift(element.nodeName);
            element = element.parentNode;
        } 
        return path;
    };
    
    
    var elementHasChildren = function(element) {
        for(var child = element.firstChild; child !== null && child !== undefined; child = child.nextSibling) {
            if(child.nodeType === 1) {
                return true;
            }
        }
        return false;
    };
    
    
    var getElementChildrenCount = function(element) {
        var count = 0;
        for(var child = element.firstChild; child !== null && child !== undefined; child = child.nextSibling) {
            if(child.nodeType === 1) {
                count++;
            }
        }
        return count;
    };
    
    
    var getChildElementAtIndex = function(parent, index) {
        var currentIndex = 0;
        for(var child = parent.firstChild; child !== null && child !== undefined; child = child.nextSibling) {
            if(child.nodeType === 1) {
                if(currentIndex === index) {
                    return child;
                } else {
                    currentIndex++;
                }
            }
        }
        return null;
    };
    
    
    var getElementIndex = function(element) {
        var index = 0;
        for(var child = element.parentNode.firstChild; child !== null && child !== undefined; child = child.nextSibling) {
            if(child.nodeType === 1) {
                if(child === element) {
                    return index;
                } else {
                    index++;
                }
            }
        }
        return null;
    };
    
    
    var canMoveUp = function(element) {
        for(var child = element.parentNode.firstChild; child !== null && child !== undefined; child = child.nextSibling) {
            if(child.nodeType === 1) {
                return child !== element;
            }
        }
        return true;
    };
    
    
    var canMoveDown = function(element) {
        for(var child = element.parentNode.lastChild; child !== null && child !== undefined; child = child.previousSibling) {
            if(child.nodeType === 1) {
                return child !== element;
            }
        }
        return true;
    };
    
    
    var getElementNestLevel = function(element) {
        var level = 0;
        while(element.parentNode !== null && element.parentNode !== undefined) {
            element = element.parentNode;
            level++;
        }
        return level;
    };
    
    
    var getDocumentDeclaration = function(documentText) {
        var documentDeclaration = null;
        if(documentText.length > 4 && documentText.substring(0, 2) === "<?") {
            return documentText.substring(0, documentText.indexOf("?>") + 2);
        }
        return documentDeclaration;
    };
    
    
    return {
        getElementPath: getElementPath,
        elementHasChildren: elementHasChildren,
        getElementChildrenCount: getElementChildrenCount,
        getChildElementAtIndex: getChildElementAtIndex,
        getElementIndex: getElementIndex,
        canMoveUp: canMoveUp,
        canMoveDown: canMoveDown,
        getElementNestLevel: getElementNestLevel,
        getDocumentDeclaration: getDocumentDeclaration
    };
    
})();
