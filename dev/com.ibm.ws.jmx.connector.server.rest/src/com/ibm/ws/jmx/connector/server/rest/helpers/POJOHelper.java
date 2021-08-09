/*******************************************************************************
 * Copyright (c) 2015, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jmx.connector.server.rest.helpers;

import java.io.IOException;

import com.ibm.json.java.JSONArray;
import com.ibm.json.java.JSONArtifact;
import com.ibm.json.java.OrderedJSONObject;
import com.ibm.ws.jmx.connector.server.rest.APIConstants;

/**
 * This helper class is used for returning complex POJO objects
 */
public class POJOHelper {

    private final OrderedJSONObject mainObj = new OrderedJSONObject();
    private final OrderedJSONObject mainLevelOne = new OrderedJSONObject();
    private final OrderedJSONObject mainLevelOneInner = new OrderedJSONObject();
    private final OrderedJSONObject valueStructureObj = new OrderedJSONObject();
    private final OrderedJSONObject typeStructureObj = new OrderedJSONObject();
    private final OrderedJSONObject openTypesStructureObj = new OrderedJSONObject();

    private final OrderedJSONObject valueStructureObjCase1 = new OrderedJSONObject();
    private final OrderedJSONObject valueStructureObjCase2 = new OrderedJSONObject();
    private final OrderedJSONObject valueStructureObjCase3 = new OrderedJSONObject();

    private final OrderedJSONObject valueStructureObjCase4 = new OrderedJSONObject();
    private final OrderedJSONObject valueStructureObjCase4InnerBlock1 = new OrderedJSONObject();

    private final OrderedJSONObject valueStructureObjCase5 = new OrderedJSONObject();
    private final OrderedJSONObject valueStructureObjCase5InnerBlock1 = new OrderedJSONObject();

    private final OrderedJSONObject valueStructureObjCase6 = new OrderedJSONObject();
    private final OrderedJSONObject valueStructureObjCase7 = new OrderedJSONObject();
    private final OrderedJSONObject valueStructureObjCase7InnerBlock1 = new OrderedJSONObject();

    private final OrderedJSONObject typeStructureObjCase1 = new OrderedJSONObject();
    private final OrderedJSONObject typeStructureObjCase2 = new OrderedJSONObject();

    private final OrderedJSONObject typeStructureObjCase3 = new OrderedJSONObject();
    private final OrderedJSONObject typeStructureObjCase3InnerBlock1 = new OrderedJSONObject();

    private final OrderedJSONObject typeStructureObjCase4 = new OrderedJSONObject();
    private final OrderedJSONObject typeStructureObjCase4InnerBlock1 = new OrderedJSONObject();
    private final OrderedJSONObject typeStructureObjCase4InnerBlock2 = new OrderedJSONObject();

    private final OrderedJSONObject typeStructureObjCase5 = new OrderedJSONObject();
    private final OrderedJSONObject typeStructureObjCase5InnerBlock1 = new OrderedJSONObject();
    private final OrderedJSONObject typeStructureObjCase5InnerBlock2 = new OrderedJSONObject();

    private final OrderedJSONObject typeStructureObjCase6 = new OrderedJSONObject();
    private final OrderedJSONObject typeStructureObjCase6InnerBlock1 = new OrderedJSONObject();

    private final OrderedJSONObject openTypesStructureObjCase1 = new OrderedJSONObject();
    private final OrderedJSONObject openTypesStructureObjCase2 = new OrderedJSONObject();
    private final OrderedJSONObject openTypesStructureObjCase3 = new OrderedJSONObject();
    private final OrderedJSONObject openTypesStructureObjCase3InnerBlock1 = new OrderedJSONObject();
    private final OrderedJSONObject openTypesStructureObjCase4 = new OrderedJSONObject();
    private final OrderedJSONObject openTypesStructureObjCase4InnerBlock1 = new OrderedJSONObject();
    private final OrderedJSONObject openTypesStructureObjCase5 = new OrderedJSONObject();
    private final OrderedJSONObject openTypesStructureObjCase5InnerBlock1 = new OrderedJSONObject();

    private final OrderedJSONObject examplePOJOObject1 = new OrderedJSONObject();
    private final OrderedJSONObject examplePOJOObject2 = new OrderedJSONObject();
    private final OrderedJSONObject examplePOJOObject1InnerBlock1 = new OrderedJSONObject();
    private final OrderedJSONObject examplePOJOObject1InnerBlock2 = new OrderedJSONObject();
    private final OrderedJSONObject examplePOJOObject2InnerBlock1 = new OrderedJSONObject();

    public String getPOJOObject() {
        mainObj.put("pojoExample", mainLevelOne);
        mainLevelOne.put("value", "valueStructure");
        mainLevelOne.put("type", "typeStructure");
        mainLevelOne.put("serialized", "base64");
        //array
        mainLevelOneInner.put("name", "openTypesStructure");
        JSONArray openTypesArray = new JSONArray();
        openTypesArray.add(mainLevelOneInner);
        mainLevelOne.put("openTypes", openTypesArray);

        setCommonJSONElements(valueStructureObjCase1, null, "For null value, or a value that's not expressible in "
                                                            + "JSON (unsupported class, and the actual value is Java serialized and "
                                                            + "stored in the serialized part)");
        valueStructureObj.put("case1", valueStructureObjCase1);

        setCommonJSONElements(valueStructureObjCase2, "string", "For a leaf (primitive, BigInteger/Decimal, Date, etc.)");
        valueStructureObj.put("case2", valueStructureObjCase2);

        JSONArray arrayValue = new JSONArray();
        arrayValue.add("Value");

        setCommonJSONElements(valueStructureObjCase3, arrayValue, "For array or set, list, collection");
        valueStructureObj.put("case3", valueStructureObjCase3);

        valueStructureObjCase4InnerBlock1.put("String", "Value");
        setCommonJSONElements(valueStructureObjCase4, valueStructureObjCase4InnerBlock1, "For map with simple key");
        valueStructureObj.put("case4", valueStructureObjCase4);

        valueStructureObjCase5InnerBlock1.put("key", "Value");
        valueStructureObjCase5InnerBlock1.put("value", "Value");

        setCommonJSONElements(valueStructureObjCase5, valueStructureObjCase5InnerBlock1, "For map with complex keys");
        valueStructureObj.put("case5", valueStructureObjCase5);

        setCommonJSONElements(valueStructureObjCase6, valueStructureObjCase4InnerBlock1, "For CompositeData (Key cannot be null)");
        valueStructureObj.put("case6", valueStructureObjCase6);

        valueStructureObjCase7InnerBlock1.put("keys", arrayValue);
        valueStructureObjCase7InnerBlock1.put("value", "Value");

        setCommonJSONElements(valueStructureObjCase7, valueStructureObjCase7InnerBlock1, "TabularData");
        valueStructureObj.put("case7", valueStructureObjCase7);

        mainObj.put("valueStructure", valueStructureObj);

        setCommonJSONElements(typeStructureObjCase1, "string", "For a leaf (primitive, array of primitive, BigInteger/Decimal, Date, null etc.)");
        typeStructureObj.put("case1", typeStructureObjCase1);

        JSONArray arrayType = new JSONArray();
        arrayType.add("Type");

        setCommonJSONElements(typeStructureObjCase2, arrayType, "For an array item that is itself an array");
        valueStructureObj.put("case2", typeStructureObjCase2);

        typeStructureObjCase3InnerBlock1.put("className", "String");
        typeStructureObjCase3InnerBlock1.put("items", arrayType);
        setCommonJSONElements(typeStructureObjCase3, typeStructureObjCase3InnerBlock1, "For array or set, list, collection classes");
        typeStructureObj.put("case3", typeStructureObjCase3);

        typeStructureObjCase4InnerBlock2.put("key", "String");
        typeStructureObjCase4InnerBlock2.put("keyType", "Type");
        typeStructureObjCase4InnerBlock2.put("value", "Type");

        JSONArray entriesArray = new JSONArray();
        entriesArray.add(typeStructureObjCase4InnerBlock2);

        typeStructureObjCase4InnerBlock1.put("className", "String");
        typeStructureObjCase4InnerBlock1.put("simpleKey", false);
        typeStructureObjCase4InnerBlock1.put("entries", entriesArray);

        setCommonJSONElements(typeStructureObjCase4, typeStructureObjCase4InnerBlock1, "For map with simple keys");
        typeStructureObj.put("case4", typeStructureObjCase4);

        typeStructureObjCase5InnerBlock2.put("key", "String");
        typeStructureObjCase5InnerBlock2.put("keyType", "Type");

        JSONArray arrayEntries1 = new JSONArray();
        arrayEntries1.add(typeStructureObjCase5InnerBlock2);

        typeStructureObjCase5.put("className", "String");
        typeStructureObjCase5.put("simpleKey", "false");
        typeStructureObjCase5.put("entries", arrayEntries1);

        setCommonJSONElements(typeStructureObjCase5InnerBlock1, typeStructureObjCase5, "For map with complex keys");
        typeStructureObjCase5.put("entries", typeStructureObjCase4InnerBlock1);
        typeStructureObj.put("case5", typeStructureObjCase5);

        typeStructureObjCase6InnerBlock1.put("className", "String");
        setCommonJSONElements(typeStructureObjCase6, typeStructureObjCase6InnerBlock1, "For CompositeData and TabularData");
        typeStructureObj.put("case6", typeStructureObjCase6);
        mainObj.put("typeStructure", typeStructureObj);

        setCommonJSONElements(openTypesStructureObj, "string", "For SimpleType instances");
        openTypesStructureObj.put("case1", openTypesStructureObjCase1);

        setCommonOpenTypesStructureObject(openTypesStructureObjCase1);
        setCommonJSONElements(openTypesStructureObjCase2, openTypesStructureObjCase1, "ArrayType");
        openTypesStructureObj.put("case2", openTypesStructureObjCase2);

        setCommonOpenTypesStructureObject(openTypesStructureObjCase3InnerBlock1);
        JSONArray arrayItems = new JSONArray();
        arrayItems.add("null");
        openTypesStructureObjCase3InnerBlock1.put("items", arrayItems);
        setCommonJSONElements(openTypesStructureObjCase3, openTypesStructureObjCase3InnerBlock1, "CompositeType");
        openTypesStructureObj.put("case3", openTypesStructureObjCase3);

        setCommonOpenTypesStructureObject(openTypesStructureObjCase4InnerBlock1);
        JSONArray arrayIndexNames = new JSONArray();
        arrayIndexNames.add("string");
        openTypesStructureObjCase4InnerBlock1.put("items", arrayIndexNames);
        setCommonJSONElements(openTypesStructureObjCase4, openTypesStructureObjCase4InnerBlock1, "TabularType");
        openTypesStructureObj.put("case4", openTypesStructureObjCase4);

        setCommonOpenTypesStructureObject(openTypesStructureObjCase5InnerBlock1);
        openTypesStructureObjCase5InnerBlock1.put("serialized", "string");
        setCommonJSONElements(openTypesStructureObjCase5, openTypesStructureObjCase5InnerBlock1, "Unknown OpenTypes");
        openTypesStructureObj.put("case5", openTypesStructureObjCase5);
        mainObj.put("openTypesStructure", openTypesStructureObj);

        examplePOJOObject1InnerBlock1.put("myKey2", "myValue2");
        examplePOJOObject1InnerBlock1.put("myKey1", "myValue1");
        examplePOJOObject1.put("value", examplePOJOObject1InnerBlock1);

        examplePOJOObject1InnerBlock2.put("className", "java.util.HashMap");
        examplePOJOObject1InnerBlock2.put("simpleKey", true);
        examplePOJOObject1.put("type", examplePOJOObject1InnerBlock2);

        JSONArray arrayEntries = new JSONArray();
        arrayEntries.add(setCommonKeyTypeObject(examplePOJOObject1InnerBlock2));
        examplePOJOObject1.put("entries", arrayEntries);
        mainObj.put("HashMapExample", examplePOJOObject1);

        JSONArray arrayStr = new JSONArray();
        arrayStr.add("java.lang.String");
        examplePOJOObject2InnerBlock1.put("className", "string");
        examplePOJOObject2InnerBlock1.put("items", arrayStr);

        examplePOJOObject2.put("value", arrayValue);
        examplePOJOObject2.put("type", examplePOJOObject2InnerBlock1);
        mainObj.put("ArrayListExample", examplePOJOObject2);

        return serializeJSON(mainObj);

    }

    /**
     * Sets common structure to JSON objects
     * {
     * "Representation": "string",
     * "Description": "For a leaf (primitive, BigInteger/Decimal, Date, etc.)"
     * }
     * 
     * @param OrderedJSONObject objects to be added here
     * @param representation Value of representation element
     * @param description Value of description
     * @return OrderedJSONObject
     */
    private static OrderedJSONObject setCommonJSONElements(OrderedJSONObject obj, Object representation, String description) {
        obj.put("Representation", representation);
        obj.put("Description", description);

        return obj;
    }

    /**
     * Sets common structure to JSON objects for openTypeStructure element
     * {
     * "openTypeClass": "string",
     * "className": "string",
     * "typeName": "string",
     * "description": "string"
     * }
     * 
     * @param obj objects to be added here
     * @return OrderedJSONObject
     */
    private static OrderedJSONObject setCommonOpenTypesStructureObject(OrderedJSONObject obj) {
        obj.put("openTypeClass", "string");
        obj.put("className", "string");
        obj.put("typeName", "string");
        obj.put("description", "string");

        return obj;
    }

    /**
     * Sets common structure to JSON objects for specific elements
     * {
     * "key": "myKey2",
     * "keyType": "java.lang.String",
     * "value": "java.lang.String"
     * }
     * 
     * @param obj objects to be added here
     * @return OrderedJSONObject
     */
    private static OrderedJSONObject setCommonKeyTypeObject(OrderedJSONObject obj) {
        obj.put("key", "myKey2");
        obj.put("keyType", "java.lang.String");
        obj.put("value", "java.lang.String");

        return obj;
    }

    private String serializeJSON(JSONArtifact artifact) {
        try {
            //Our JSON library escapes forward slashes, but there's no need to escape them in this case (they are just URLs)
            return artifact.serialize().replace("\\/", "/");
        } catch (IOException ioe) {
            throw ErrorHelper.createRESTHandlerJsonException(ioe, null, APIConstants.STATUS_INTERNAL_SERVER_ERROR);
        }
    }

}
