/*******************************************************************************
 * Copyright (c) 2012, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.wim.adapter.urbridge.utils;

import java.io.StringReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.security.wim.Service;
import com.ibm.websphere.security.wim.ras.WIMMessageHelper;
import com.ibm.websphere.security.wim.ras.WIMMessageKey;
import com.ibm.ws.security.wim.xpath.TokenMgrError;
import com.ibm.ws.security.wim.xpath.WIMXPathInterpreter;
import com.ibm.ws.security.wim.xpath.mapping.datatype.PropertyNode;
import com.ibm.ws.security.wim.xpath.mapping.datatype.XPathNode;
import com.ibm.wsspi.security.wim.exception.PropertyNotDefinedException;
import com.ibm.wsspi.security.wim.exception.SearchControlException;
import com.ibm.wsspi.security.wim.exception.WIMApplicationException;
import com.ibm.wsspi.security.wim.exception.WIMException;
import com.ibm.wsspi.security.wim.model.Entity;

public class URBridgeXPathHelper {

    /**
     * Register the class to trace service.
     */
    private final static TraceComponent tc = Tr.register(URBridgeXPathHelper.class);

    /**
     * The XPathNode
     */
    private XPathNode node = null;

    /**
     * List of entity types
     */
    private List<String> entityTypes = null;

    public URBridgeXPathHelper(String searchExpr) throws WIMException {
        parseSearchExpression(searchExpr);
    }

    /**
     * // parse the XPATH search expression and build a tree
     * // searchExpr = "//entities[@xsi:type='PersonAccount' and principalName='*']";//Returns Users with pattern *.
     * // searchExpr = "//entities[@xsi:type='PersonAccount' and cn='*']";//Returns Users with pattern *.
     * // searchExpr = "//entities[@xsi:type='LoginAccount' and principalName='*']";//Returns Users with pattern *.
     * // searchExpr = "//entities[(@xsi:type='Group' or @xsi:type='LoginAccount') and cn='*']"; //Returns Group And Users matching the pattern.
     * // searchExpr = "//entities[@xsi:type='LoginAccount' and uid='*']";//Returns Users with pattern *.
     * // searchExpr = "//entities[@xsi:type='Entity' and cn='*']";////Returns Group And Users matching the pattern.
     *
     * @param searchExpr
     * @throws WIMException
     */
    public void parseSearchExpression(String searchExpr) throws WIMException {
        try {
            if (searchExpr == null || searchExpr.trim().length() == 0) {
                return;
            }

            WIMXPathInterpreter parser = new WIMXPathInterpreter(new StringReader(searchExpr));
            node = parser.parse(null);

            entityTypes = parser.getEntityTypes();

            // remove the namespace from property names and also validate
            // that valid wildcard is specified
            HashMap propNodes = new HashMap();
            if (node != null) {
                Iterator<PropertyNode> propNodesItr = node.getPropertyNodes(propNodes);
                // skip if more than one node present. if (uid="a*" or cn="b*") then only one of the node uid=a* or cn=b* will be taken
                // since urbridge supports only search by prinicpal Name.
                // we can implement/override  PropertyNode to return ListIterator so that it always uses first node uid=a*
                while (propNodesItr.hasNext()) {
                    PropertyNode propNode = propNodesItr.next();
                    node = propNode; //PM43353 assign node to first propNode
                    propNode.setName(removeNamespace(propNode.getName()));
                    // validate property
                    boolean foundProperty = false;
                    String propName = propNode.getName();
                    for (Iterator<String> iter = entityTypes.iterator(); iter.hasNext() && (!foundProperty);) {
                        String entityType = iter.next();
                        if (entityType.equalsIgnoreCase(Service.DO_LOGIN_ACCOUNT)) //This is to avoid propertynotsupported for LoginAc if search returns rdn property.
                            entityType = Service.DO_PERSON_ACCOUNT;

                        foundProperty = Entity.getPropertyNames(entityType).contains(propName);
                    }
                    if (!foundProperty) {
                        throw new PropertyNotDefinedException(WIMMessageKey.PROPERTY_NOT_DEFINED_FOR_ENTITY, Tr.formatMessage(tc, WIMMessageKey.PROPERTY_NOT_DEFINED_FOR_ENTITY,
                                                                                                                              WIMMessageHelper.generateMsgParms(propName,
                                                                                                                                                                entityTypes)));
                    }
                    break;
                    //validate end
                }
            }
        } catch (Exception e) {
            throw new WIMApplicationException(WIMMessageKey.MALFORMED_SEARCH_EXPRESSION, Tr.formatMessage(tc, WIMMessageKey.MALFORMED_SEARCH_EXPRESSION,
                                                                                                          WIMMessageHelper.generateMsgParms(searchExpr)), e);
        } catch (TokenMgrError e) {
            throw new SearchControlException(WIMMessageKey.INVALID_SEARCH_EXPRESSION, Tr.formatMessage(tc, WIMMessageKey.INVALID_SEARCH_EXPRESSION,
                                                                                                       WIMMessageHelper.generateMsgParms(searchExpr)), e);
        }
    }

    //  transforms 'namespace:entityType' to entityType(for example, 'wim:Person' to Person)
    public String removeNamespace(String entityType) {
        String str = entityType.replace('\'', ' ').trim();
        int index = str.indexOf(":");
        if (index > 0)
            str = str.substring(index + 1);
        return str;
    }

    public String getExpression() throws Exception {
        if (node != null) {
            switch (node.getNodeType()) {
                case XPathNode.NODE_PROPERTY:
                    return getExpression((PropertyNode) node);
            }
        }
        return "";
    }

    private String getExpression(PropertyNode propNode) throws Exception {
        String pattern = (String) propNode.getValue();
        pattern = pattern.replace("\"\"", "\""); // Unescape escaped XPath quotations
        pattern = pattern.replace("''", "'"); // Unescape escaped XPath apostrophes
        return pattern;
    }

    public List<String> getEntityTypes() {
        return entityTypes;
    }
}
