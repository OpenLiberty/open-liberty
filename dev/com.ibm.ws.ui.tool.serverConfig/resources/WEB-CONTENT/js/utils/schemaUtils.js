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

var schemaUtils = (function() {
    "use strict";
    
    var getRootElements = function(schema) {
        var schemaRoot = getSchemaRoot(schema);
        var rootElements = [];
        for(var child = schemaRoot.firstChild; child !== null && child !== undefined; child = child.nextSibling) {
            if(child.nodeType === 1 && child.nodeName === "xsd:element") {
                rootElements.push(child);
            }
        }
        return rootElements;
    };
    
    
    var getRootElementByName = function(schema, rootElementName) {
      var rootElements = getRootElements(schema);
      for(var i = 0; i < rootElements.length; i++) {
        if(rootElements[i].getAttribute("name") === rootElementName) {
          return rootElements[i];
        }
      }
      return null;
    };
    
    
    var getElementFromPath = function (element, path) {
        if(element.getAttribute("name") === path[0]) {
            if(path.length === 1) {
                return element;
            } else {
                var elementDeclaration = resolveElementDeclaration(element);
                var children = getChildElements(elementDeclaration);
                for(var i = 0; i < children.length; i++) {
                    if(children[i].getAttribute("name") === path[1]) {
                        return getElementFromPath(children[i], path.slice(1));
                    }
                }
            }
        }
        return null;
    };
    
    
    var getAttributes = function(elementDeclaration) {
        var attributes = [];
        var attributeNodes = elementDeclaration.getElementsByTagName("xsd:attribute");
        if(attributeNodes.length === 0) {
            attributeNodes = elementDeclaration.getElementsByTagName("attribute");
        }
        for(var i = 0; i < attributeNodes.length; i++) {
            attributes.push(attributeNodes.item(i));
        }
        for(var child = elementDeclaration.firstChild; child !== null && child !== undefined; child = child.nextSibling) {
            if(child.nodeType === 1 && child.nodeName === "xsd:complexContent") {
                for(var grandChild = child.firstChild; grandChild !== null && grandChild !== undefined; grandChild = grandChild.nextSibling) {
                    if(grandChild.nodeType === 1 && grandChild.nodeName === "xsd:extension") {
                        var base = grandChild.getAttribute("base");
                        var schema = elementDeclaration.ownerDocument;
                        var typeDeclaration = getTypeDeclaration(schema, base);
                        attributes = attributes.concat(getAttributes(typeDeclaration));
                    }
                }
            }
        }
        return attributes;
    };
    
    
    var getAttribute = function(elementDeclaration, attributeName) {
        var attributeNodes = elementDeclaration.getElementsByTagName("xsd:attribute");
        if(attributeNodes.length === 0) {
            attributeNodes = elementDeclaration.getElementsByTagName("attribute");
        }
        for(var i = 0; i < attributeNodes.length; i++) {
            if(attributeNodes.item(i).getAttribute("name") === attributeName) {
                return attributeNodes.item(i);
            }
        }
        for(var child = elementDeclaration.firstChild; child !== null && child !== undefined; child = child.nextSibling) {
            if(child.nodeType === 1 && child.nodeName === "xsd:complexContent") {
                for(var grandChild = child.firstChild; grandChild !== null && grandChild !== undefined; grandChild = grandChild.nextSibling) {
                    if(grandChild.nodeType === 1 && grandChild.nodeName === "xsd:extension") {
                        var base = grandChild.getAttribute("base");
                        var schema = elementDeclaration.ownerDocument;
                        var typeDeclaration = getTypeDeclaration(schema, base);
                        return getAttribute(typeDeclaration, attributeName);
                    }
                }
            }
        }
        return null;
    };
    
    
    var hasChildElements = function(elementDeclaration) {
        if(elementDeclaration.getElementsByTagName("xsd:element").length > 0) {
            return true;
        }
        if(elementDeclaration.getElementsByTagName("element").length > 0) {
            return true;
        }
        for(var child = elementDeclaration.firstChild; child !== null && child !== undefined; child = child.nextSibling) {
            if(child.nodeType === 1 && child.nodeName === "xsd:complexContent") {
                for(var grandChild = child.firstChild; grandChild !== null && grandChild !== undefined; grandChild = grandChild.nextSibling) {
                    if(grandChild.nodeType === 1 && grandChild.nodeName === "xsd:extension") {
                        var base = grandChild.getAttribute("base");
                        var schema = elementDeclaration.ownerDocument;
                        var typeDeclaration = getTypeDeclaration(schema, base);
                        return hasChildElements(typeDeclaration);
                    }
                }
            }
        }
    };
    
    
    var getChildElements = function(elementDeclaration) {
        var childElements = [];
        var sequenceNodes = elementDeclaration.getElementsByTagName("xsd:sequence");
        var i, child, grandChild;
        if(sequenceNodes.length === 0) {
            sequenceNodes = elementDeclaration.getElementsByTagName("sequence");
        }
        for(i = 0; i < sequenceNodes.length; i++) {
            for(child = sequenceNodes.item(i).firstChild; child !== null && child !== undefined; child = child.nextSibling) {
                if(child.nodeType === 1 && child.nodeName === "xsd:element") {
                    childElements.push(child);
                }
            }
        }
        var choiceNodes = elementDeclaration.getElementsByTagName("xsd:choice");
        if(choiceNodes.length === 0) {
            choiceNodes = elementDeclaration.getElementsByTagName("choice");
        }
        for(i = 0; i < choiceNodes.length; i++) {
            for(child = choiceNodes.item(i).firstChild; child !== null && child !== undefined; child = child.nextSibling) {
                if(child.nodeType === 1 && child.nodeName === "xsd:element") {
                    childElements.push(child);
                }
            }
        }
        for(child = elementDeclaration.firstChild; child !== null && child !== undefined; child = child.nextSibling) {
            if(child.nodeType === 1 && child.nodeName === "xsd:complexContent") {
                for(grandChild = child.firstChild; grandChild !== null && grandChild !== undefined; grandChild = grandChild.nextSibling) {
                    if(grandChild.nodeType === 1 && grandChild.nodeName === "xsd:extension") {
                        var base = grandChild.getAttribute("base");
                        var schema = elementDeclaration.ownerDocument;
                        var typeDeclaration = getTypeDeclaration(schema, base);
                        childElements = childElements.concat(getChildElements(typeDeclaration));
                    }
                }
            }
        }
        return childElements;
    };
    
    
    var getLabel = function(node) {
        var child, grandChild, greatGrandChild;
        for(child = node.firstChild; child !== null && child !== undefined && child !== undefined; child = child.nextSibling) {
            if(child.nodeType === 1 && child.nodeName === "xsd:annotation") {
                for(grandChild = child.firstChild; grandChild !== null && grandChild !== undefined; grandChild = grandChild.nextSibling) {
                    if(grandChild.nodeType === 1 && grandChild.nodeName === "xsd:appinfo") {
                        for(greatGrandChild = grandChild.firstChild; greatGrandChild !== null && greatGrandChild !== undefined; greatGrandChild = greatGrandChild.nextSibling) {
                            if(greatGrandChild.nodeType === 1 && greatGrandChild.nodeName === "ext:label") {
                                return greatGrandChild.firstChild.nodeValue;
                            }
                        }
                    }
                }
            }
        }
        for(child = node.firstChild; child !== null && child !== undefined && child !== undefined; child = child.nextSibling) {
            if(child.nodeType === 1 && child.nodeName === "xsd:complexContent") {
                for(grandChild = child.firstChild; grandChild !== null && grandChild !== undefined; grandChild = grandChild.nextSibling) {
                    if(grandChild.nodeType === 1 && grandChild.nodeName === "xsd:extension") {
                        var base = grandChild.getAttribute("base");
                        var schema = node.ownerDocument;
                        var typeDeclaration = getTypeDeclaration(schema, base);
                        return getLabel(typeDeclaration);
                    }
                }
            }
        }
        return null;
    };
    
    var getVariable = function(node) {
        var child, grandChild, greatGrandChild;
        for(child = node.firstChild; child !== null && child !== undefined && child !== undefined; child = child.nextSibling) {
            if(child.nodeType === 1 && child.nodeName === "xsd:annotation") {
                for(grandChild = child.firstChild; grandChild !== null && grandChild !== undefined; grandChild = grandChild.nextSibling) {
                    if(grandChild.nodeType === 1 && grandChild.nodeName === "xsd:appinfo") {
                        for(greatGrandChild = grandChild.firstChild; greatGrandChild !== null && greatGrandChild !== undefined; greatGrandChild = greatGrandChild.nextSibling) {
                            if(greatGrandChild.nodeType === 1 && greatGrandChild.nodeName === "ext:variable") {
                                return greatGrandChild.firstChild.nodeValue;
                            }
                        }
                    }
                }
            }
        }
        for(child = node.firstChild; child !== null && child !== undefined && child !== undefined; child = child.nextSibling) {
            if(child.nodeType === 1 && child.nodeName === "xsd:complexContent") {
                for(grandChild = child.firstChild; grandChild !== null && grandChild !== undefined; grandChild = grandChild.nextSibling) {
                    if(grandChild.nodeType === 1 && grandChild.nodeName === "xsd:extension") {
                        var base = grandChild.getAttribute("base");
                        var schema = node.ownerDocument;
                        var typeDeclaration = getTypeDeclaration(schema, base);
                        return getVariable(typeDeclaration);
                    }
                }
            }
        }
        return null;
    };
    
    var getGroupDeclarations = function(node) {
        var groupDeclarations = [];
        var child, grandChild, greatGrandChild;
        for(child = node.firstChild; child !== null && child !== undefined && child !== undefined; child = child.nextSibling) {
            if(child.nodeType === 1 && child.nodeName === "xsd:annotation") {
                for(grandChild = child.firstChild; grandChild !== null && grandChild !== undefined && grandChild !== undefined; grandChild = grandChild.nextSibling) {
                    if(grandChild.nodeType === 1 && grandChild.nodeName === "xsd:appinfo") {
                        for(greatGrandChild = grandChild.firstChild; greatGrandChild !== null && greatGrandChild !== undefined; greatGrandChild = greatGrandChild.nextSibling) {
                            if(greatGrandChild.nodeType === 1 && greatGrandChild.nodeName === "ext:groupDecl") {
                                groupDeclarations.push({
                                    "id": greatGrandChild.getAttribute("id"),
                                    "label": greatGrandChild.getAttribute("label"), 
                                    "description": greatGrandChild.firstChild.nodeValue
                                });
                            }
                        }
                    }
                }
            }
        }
        for(child = node.firstChild; child !== null && child !== undefined; child = child.nextSibling) {
            if(child.nodeType === 1 && child.nodeName === "xsd:complexContent") {
                for(grandChild = child.firstChild; grandChild !== null && grandChild !== undefined && grandChild !== undefined; grandChild = grandChild.nextSibling) {
                    if(grandChild.nodeType === 1 && grandChild.nodeName === "xsd:extension") {
                        var base = grandChild.getAttribute("base");
                        var schema = node.ownerDocument;
                        var typeDeclaration = getTypeDeclaration(schema, base);
                        return getGroupDeclarations(typeDeclaration);
                    }
                }
            }
        }
        return groupDeclarations;
    };
    
    var getGroup = function(node) {
        var child, grandChild, greatGrandChild;
        for(child = node.firstChild; child !== null && child !== undefined && child !== undefined; child = child.nextSibling) {
            if(child.nodeType === 1 && child.nodeName === "xsd:annotation") {
                for(grandChild = child.firstChild; grandChild !== null && grandChild !== undefined && grandChild !== undefined; grandChild = grandChild.nextSibling) {
                    if(grandChild.nodeType === 1 && grandChild.nodeName === "xsd:appinfo") {
                        for(greatGrandChild = grandChild.firstChild; greatGrandChild !== null && greatGrandChild !== undefined; greatGrandChild = greatGrandChild.nextSibling) {
                            if(greatGrandChild.nodeType === 1 && greatGrandChild.nodeName === "ext:group") {
                                return greatGrandChild.getAttribute("id");
                            }
                        }
                    }
                }
            }
        }
        for(child = node.firstChild; child !== null && child !== undefined && child !== undefined; child = child.nextSibling) {
            if(child.nodeType === 1 && child.nodeName === "xsd:complexContent") {
                for(grandChild = child.firstChild; grandChild !== null && grandChild !== undefined && grandChild !== undefined; grandChild = grandChild.nextSibling) {
                    if(grandChild.nodeType === 1 && grandChild.nodeName === "xsd:extension") {
                        var base = grandChild.getAttribute("base");
                        var schema = node.ownerDocument;
                        var typeDeclaration = getTypeDeclaration(schema, base);
                        return getGroup(typeDeclaration);
                    }
                }
            }
        }
        return null;
    };
    
    
    var getDocumentation = function (node) {
        var child, grandChild;
        for(child = node.firstChild; child !== null && child !== undefined && child !== undefined; child = child.nextSibling) {
            if(child.nodeType === 1 && child.nodeName === "xsd:annotation") {
                for(grandChild = child.firstChild; grandChild !== null && grandChild !== undefined && grandChild !== undefined; grandChild = grandChild.nextSibling) {
                    if(grandChild.nodeType === 1 && grandChild.nodeName === "xsd:documentation") {
                        return grandChild.firstChild.nodeValue;
                    }
                }
            }
        }
        for(child = node.firstChild; child !== null && child !== undefined && child !== undefined; child = child.nextSibling) {
            if(child.nodeType === 1 && child.nodeName === "xsd:complexContent") {
                for(grandChild = child.firstChild; grandChild !== null && grandChild !== undefined && grandChild !== undefined; grandChild = grandChild.nextSibling) {
                    if(grandChild.nodeType === 1 && grandChild.nodeName === "xsd:extension") {
                        var base = grandChild.getAttribute("base");
                        var schema = node.ownerDocument;
                        var typeDeclaration = getTypeDeclaration(schema, base);
                        return getDocumentation(typeDeclaration);
                    }
                }
            }
        }
        return null;
    };
    
    
    var getDefaultValue = function(node) {
        return node.getAttribute("default");
    };
    
    
    var elementContainsText = function(elementDeclaration) {
        if(isPrimitiveDataType(elementDeclaration.getAttribute("type"))) {
            return true;
        }
        return elementDeclaration.nodeName === "xsd:simpleType";
    };
    
    
    var isRequired = function(node) {
        return node.getAttribute("use") === "required";
    };
    
    
    var hasPossibleValues = function(node) {
        if(node.getAttribute("type") === "booleanType" || node.getAttribute("type") === "xsd:boolean") {
            return true;
        }
        var enumerations = node.getElementsByTagName("xsd:enumeration");
        if(enumerations.length === 0) {
            enumerations = node.getElementsByTagName("enumeration");
        }
        return enumerations.length > 0;
    };
    
    
    var getPossibleValues = function(node) {
        var possibleValues = [];
        var enumerations = node.getElementsByTagName("xsd:enumeration");
        if(enumerations.length === 0) {
            enumerations = node.getElementsByTagName("enumeration");
        }
        for(var i = 0; i < enumerations.length; i++) {
            var value = enumerations.item(i);
            possibleValues[i] = [value.getAttribute("value"), getDocumentation(value)];
        }
        if(node.getAttribute("type") === "booleanType" || node.getAttribute("type") === "xsd:boolean") {
            possibleValues.push(["true", "true"],["false", "false"]);
        }
        return possibleValues;
    };
    
    
    var resolveElementDeclaration = function(element) {
        var type = element.getAttribute("type");
        if(type !== null && type !== undefined && !isPrimitiveDataType(type)) {
            var schema = element.ownerDocument;
            return getTypeDeclaration(schema, type);
        }
        return element;
    };
    
    
    var getDataType = function(node) {
        var dataType = node.getAttribute("type");
        if(dataType === null || dataType === undefined) {
            dataType = node.getAttribute("xsd:type");
        }
        return dataType;
    };
    
    
    function getTypeDeclaration(schema, name) {
        var schemaRoot = getSchemaRoot(schema);
        for(var child = schemaRoot.firstChild; child !== null && child !== undefined; child = child.nextSibling) {
            if(child.nodeType === 1 && (child.nodeName === "xsd:simpleType" || child.nodeName === "xsd:complexType") && child.getAttribute("name") === name) {
                return child;
            }
        }
        
    }
    

    function isPrimitiveDataType(name) {
        
        var primitiveDataTypes =   ["string",
                                "boolean",
                                "decimal",
                                "float",
                                "double",
                                "duration",
                                "dateTime",
                                "time",
                                "date",
                                "gYearMonth",
                                "gYear",
                                "gMonthDay",
                                "gDay",
                                "gMonth",
                                "hexBinary",
                                "base64Binary",
                                "anyURI",
                                "QName",
                                "NOTATION"];
        
        for(var i = 0; i < primitiveDataTypes.length; i++) {
            if("xsd:" + primitiveDataTypes[i] === name) {
                return true;
            }
        }
        return false;
    }

    
    function getSchemaRoot(schema) {
        var child = schema.firstChild;
        while(child !== null && child !== undefined) {
            if(child.nodeType === 1 && child.nodeName === "xsd:schema") {
                return child;
            }
            child = child.nextSibling;
        }
        return null;
    }
    
    
    return {
        getRootElements: getRootElements,
        getRootElementByName: getRootElementByName,
        getElementFromPath: getElementFromPath,
        getAttributes: getAttributes,
        getAttribute: getAttribute,
        hasChildElements: hasChildElements,
        getChildElements: getChildElements,
        getLabel: getLabel,
        getDocumentation: getDocumentation,
        getDefaultValue: getDefaultValue,
        elementContainsText: elementContainsText,
        isRequired: isRequired,
        getPossibleValues: getPossibleValues,
        resolveElementDeclaration: resolveElementDeclaration,
        hasPossibleValues: hasPossibleValues,
        getDataType: getDataType,
        getGroupDeclarations: getGroupDeclarations,
        getGroup: getGroup,
        getVariable: getVariable
    };
    
})();
