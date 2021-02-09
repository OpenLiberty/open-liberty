/*******************************************************************************
 * Copyright (c) 2012, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.wim.registry.util;

import java.util.ArrayList;
import java.util.List;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.security.wim.Service;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.registry.RegistryException;
import com.ibm.ws.security.registry.SearchResult;
import com.ibm.ws.security.wim.registry.dataobject.IDAndRealm;
import com.ibm.ws.security.wim.util.UniqueNameHelper;
import com.ibm.wsspi.security.wim.SchemaConstants;
import com.ibm.wsspi.security.wim.exception.EntityNotFoundException;
import com.ibm.wsspi.security.wim.exception.EntityNotInRealmScopeException;
import com.ibm.wsspi.security.wim.exception.InvalidUniqueNameException;
import com.ibm.wsspi.security.wim.exception.WIMException;
import com.ibm.wsspi.security.wim.model.Context;
import com.ibm.wsspi.security.wim.model.Control;
import com.ibm.wsspi.security.wim.model.Entity;
import com.ibm.wsspi.security.wim.model.Group;
import com.ibm.wsspi.security.wim.model.IdentifierType;
import com.ibm.wsspi.security.wim.model.Root;
import com.ibm.wsspi.security.wim.model.SearchControl;

/**
 * Bridge class for mapping user and group search methods.
 *
 */
public class SearchBridge {

    private static final TraceComponent tc = Tr.register(SearchBridge.class);

    /**
     * Property mappings.
     */
    private TypeMappings propertyMap = null;

    /**
     * Mappings utility class.
     */
    private BridgeUtils mappingUtils = null;

    /**
     * RDN property for a group.
     */
    private String groupRDN = "cn";

    /**
     * Default constructor.
     *
     * @param mappingUtil
     */
    @FFDCIgnore(Exception.class)
    public SearchBridge(BridgeUtils mappingUtil) {
        String methodName = "SearchBridge";
        this.mappingUtils = mappingUtil;
        propertyMap = new TypeMappings(mappingUtil);
        try {
            // Get the group RDN property
            String[] groupRDNList = this.mappingUtils.getCoreConfiguration().getRDNProperties(Service.DO_GROUP);

            if (groupRDNList != null && groupRDNList.length > 0)
                groupRDN = groupRDNList[0];
        } catch (Exception excp) {
            if (tc.isEventEnabled()) {
                Tr.event(tc, methodName + " " + excp.getMessage());
            }
        }
    }

    @FFDCIgnore(WIMException.class)
    public SearchResult getUsers(String inputPattern, int inputLimit) throws RegistryException {
        String methodName = "getUsers";
        // initialize the return value
        SearchResult returnValue = new SearchResult();
        // bridge the APIs
        try {
            // validate the id
            this.mappingUtils.validateId(inputPattern);
            // separate the ID and the realm
            IDAndRealm idAndRealm = this.mappingUtils.separateIDAndRealm(inputPattern);
            // create an empty root DataObject
            Root root = this.mappingUtils.getWimService().createRootObject();
            // if realm is defined
            if (idAndRealm.isRealmDefined()) {
                // create the realm DataObject
                this.mappingUtils.createRealmDataObject(root, idAndRealm.getRealm());
            }

            // search on the principalName if the input attribute is an identifier type
            String inputAttrName = this.propertyMap.getInputUserSecurityName(idAndRealm.getRealm());
            boolean isInputAttrIdentifier = this.mappingUtils.isIdentifierTypeProperty(inputAttrName);
            if (isInputAttrIdentifier)
                inputAttrName = SchemaConstants.PROP_PRINCIPAL_NAME;
            String outputAttrName = this.propertyMap.getOutputUserSecurityName(idAndRealm.getRealm());

            // use the root DataGraph to create a SearchControl DataGraph
            List<Control> controls = root.getControls();
            SearchControl searchControl = new SearchControl();
            if (controls != null) {
                controls.add(searchControl);
            }
            // d115256
            if (!this.mappingUtils.isIdentifierTypeProperty(outputAttrName)) {
                searchControl.getProperties().add(outputAttrName);
            }
            // set the "expression" string to "type=LoginAccount and MAP(userSecurityName)="userPattern""
            String quote = "'";
            String id = idAndRealm.getId();
            if (id.indexOf("'") != -1) {
                quote = "\"";
            }

            // d112199
            searchControl.setExpression("//" + Service.DO_ENTITIES + "[@xsi:type='"
                                        + Service.DO_LOGIN_ACCOUNT + "' and " + inputAttrName + "=" + quote + id + quote + "]");
            // d122142
            // if limit > 0, set the search limit to limit + 1
            if (inputLimit > 0) {
                searchControl.setCountLimit((inputLimit + 1));
            } else {
                searchControl.setCountLimit((inputLimit));
            }

            // Set context to use userFilter if applicable
            Context context = new Context();
            context.set("key", SchemaConstants.USE_USER_FILTER_FOR_SEARCH);
            context.set("value", id);
            root.getContexts().add(context);

            // invoke ProfileService.search with the input root DataGraph
            root = this.mappingUtils.getWimService().search(root);
            List<Entity> returnedList = root.getEntities();
            if (!returnedList.isEmpty()) {
                // add the MAP(userSecurityName)s to the Result list while count < limit
                ArrayList<String> people = new ArrayList<String>();
                for (int count = 0; count < returnedList.size(); count++) {
                    // d122142
                    if ((inputLimit > 0) && (count == inputLimit)) {
                        // set the Result boolean to true
                        //returnValue.setHasMore();
                        break;
                    }
                    Entity loginAccount = returnedList.get(count);
                    // d115256
                    if (!this.mappingUtils.isIdentifierTypeProperty(outputAttrName)) {
                        Object value = loginAccount.get(outputAttrName);
                        if (value instanceof String) {
                            people.add((String) value);
                        } else {
                            people.add(BridgeUtils.getStringValue(((List<?>) value).get(0)));
                        }
                    } else {
                        people.add(BridgeUtils.getStringValue(loginAccount.getIdentifier().get(outputAttrName)));
                    }
                }
                returnValue = new SearchResult(people, true);
            } else {
                returnValue = new SearchResult(new ArrayList<String>(), false);
            }
        }
        // other cases
        catch (WIMException toCatch) {
            // f113366
            if (toCatch instanceof EntityNotFoundException
                || toCatch instanceof InvalidUniqueNameException
                || toCatch instanceof EntityNotInRealmScopeException) {
                returnValue = new SearchResult(new ArrayList<String>(), false);
            }
            // log the Exception
            else {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, methodName + " " + toCatch.getMessage(), toCatch);
                }
                if (tc.isErrorEnabled()) {
                    Tr.error(tc, toCatch.getMessage(), toCatch);
                }
                throw new RegistryException(toCatch.getMessage(), toCatch);
            }
        }
        return returnValue;
    }

    @FFDCIgnore({ WIMException.class, InvalidNameException.class })
    public SearchResult getGroups(String inputPattern, int inputLimit) throws RegistryException {
        // initialize the method name
        String methodName = "getGroups";
        // initialize the return value
        SearchResult returnValue = new SearchResult();
        // bridge the APIs
        try {
            // validate the id
            this.mappingUtils.validateId(inputPattern);
            // separate the ID and the realm
            IDAndRealm idAndRealm = this.mappingUtils.separateIDAndRealm(inputPattern);
            // create an empty root DataObject
            Root root = this.mappingUtils.getWimService().createRootObject();
            // if realm is defined
            if (idAndRealm.isRealmDefined()) {
                // set "WIM.Realm" in the Context DataGraph to the realm
                this.mappingUtils.createRealmDataObject(root, idAndRealm.getRealm());
            }

            // search on the group RDN if the input attribute is an identifier type
            String inputAttrName = this.propertyMap.getInputGroupSecurityName(idAndRealm.getRealm());
            String groupSecNameAttr = inputAttrName;
            boolean isInputAttrIdentifier = this.mappingUtils.isIdentifierTypeProperty(inputAttrName);
            if (isInputAttrIdentifier)
                inputAttrName = groupRDN;
            String outputAttrName = this.propertyMap.getOutputGroupSecurityName(idAndRealm.getRealm());

            String quote = "'";
            String id = idAndRealm.getId();
            if (id.indexOf("'") != -1) {
                quote = "\"";
            }

            // PM37404 call isDN method to find out if the input to the method is DN
            boolean callGetAPI = false;

            if (UniqueNameHelper.isDN(id) != null && groupSecNameAttr.equals(Service.PROP_UNIQUE_NAME)) {
                if (tc.isEventEnabled()) {
                    Tr.event(tc, methodName + " " + "Group Security name mapped to uniqueName. Invoking get instead of search", methodName);
                }
                // call get API
                callGetAPI = true;
                // Change 3 ... create a SDO for entity type GROUP
                //DataObject entity = SDOHelper.createEntityDataObject(root, null, Service.DO_GROUP);
                List<Entity> entities = root.getEntities();
                Group entity = new Group();
                if (entities != null) {
                    entities.add(entity);
                }
                IdentifierType idfType = new IdentifierType();
                idfType.setUniqueName(id);
                entity.setIdentifier(idfType);
                root = this.mappingUtils.getWimService().get(root);

            } else {
                // use the root DataGraph to create a SearchControl DataGraph
                List<Control> controls = root.getControls();
                SearchControl searchControl = new SearchControl();
                if (controls != null) {
                    controls.add(searchControl);
                }
                // add MAP(groupSecurityName) to the return list of properties
                // d115913
                if (!this.mappingUtils.isIdentifierTypeProperty(outputAttrName)) {
                    searchControl.getProperties().add(outputAttrName);
                }

                // d112199
                LdapName dnName = null;
                try {
                    dnName = new LdapName(inputPattern);
                } catch (InvalidNameException e) {
                }
                if (dnName != null) {
                    int index = inputPattern.indexOf("=");
                    int endIndex = inputPattern.indexOf(",", index);
                    String attrName = inputAttrName;
                    String value = null;
                    if (index > 0) {
                        attrName = inputPattern.substring(0, index);
                        value = inputPattern.substring(index + 1, endIndex);

                        // OLGH8840: We're comparing the property to WIM properties and not accounting for case or Ldap specific properties.
                        // If we don't swap the case, we can get error, "CWIML0514W: The user registry operation could not be completed. The CN property is not defined."
                        // from LdapXPathTranslateHelper. If the user changes their groupSecurity mappings, we can force this down the get() instead of search() path,
                        // but that can result in unintended changes for the customer (changing their security authorization mappings).
                        // So we're double checking if it looks like we have a valid property here and sending in the matching case from Group.
                        List<String> propList = Group.getPropertyNames(null);
                        for (String prop : propList) {
                            if (prop.equalsIgnoreCase(attrName)) {
                                if (tc.isEventEnabled()) {
                                    Tr.event(tc, methodName + " equalsIgnoreCase match on " + prop + ", swapping out " + attrName);
                                }
                                attrName = prop;
                            }
                        }
                    }

                    String searchBase = null;
                    if (endIndex + 1 < inputPattern.length()) {
                        searchBase = inputPattern.substring(endIndex + 1);
                        searchControl.getSearchBases().add(searchBase);
                    }
                    searchControl.setExpression("//" + Service.DO_ENTITIES + "[@xsi:type='"
                                                + Service.DO_GROUP + "' and " + attrName + "=" + quote + value + quote + "]");
                } else
                    searchControl.setExpression("//" + Service.DO_ENTITIES + "[@xsi:type='"
                                                + Service.DO_GROUP + "' and " + inputAttrName + "=" + quote + id + quote + "]");
                // d122142
                // if limit > 0, set the search limit to limit + 1
                if (inputLimit > 0) {
                    searchControl.setCountLimit(inputLimit + 1);
                } else {
                    searchControl.setCountLimit(inputLimit);
                }

                // Set context to use groupFilter if applicable
                Context context = new Context();
                context.set("key", SchemaConstants.USE_GROUP_FILTER_FOR_SEARCH);
                context.set("value", id);
                root.getContexts().add(context);

                // invoke ProfileService.search with the input root DataGraph
                root = this.mappingUtils.getWimService().search(root);
            }
            List<Entity> returnedList = root.getEntities();
            if (!returnedList.isEmpty()) {
                // add the MAP(groupSecurityName)s to the Result list while count < limit
                ArrayList<String> groups = new ArrayList<String>();
                for (int count = 0; count < returnedList.size(); count++) {
                    // d122142
                    if ((inputLimit > 0) && (count == inputLimit)) {
                        // set the Result boolean to true
                        //returnValue.setHasMore();
                        break;
                    }
                    Entity group = returnedList.get(count);
                    boolean isEntityTypeGrp = false;
                    if (callGetAPI) {
                        //isEntityTypeGrp = SchemaManager.singleton().isSuperType(Service.DO_GROUP,
                        //        SchemaManager.singleton().getQualifiedTypeName(group.getType()));
                        isEntityTypeGrp = group.isSubType(Service.DO_GROUP);
                    } else {
                        isEntityTypeGrp = true;
                    }
                    if (tc.isEventEnabled()) {
                        Tr.event(tc, methodName + " " + "Value of isEntityTypGrp :" + isEntityTypeGrp, methodName);
                    }
                    // d113801
                    if (isEntityTypeGrp) {
                        if (!this.mappingUtils.isIdentifierTypeProperty(outputAttrName)) {
                            Object value = group.get(outputAttrName);
                            if (value instanceof String) {
                                groups.add((String) value);
                            } else {
                                groups.add(BridgeUtils.getStringValue(((List<?>) value).get(0)));
                            }
                        } else {
                            groups.add(BridgeUtils.getStringValue(group.getIdentifier().get(outputAttrName)));
                        }
                    } else {
                        if (tc.isEventEnabled()) {
                            Tr.event(tc, methodName + " " + "The Entity type was not compatible with Group. The entityType is : "
                                         + group.getTypeName());
                        }
                    }
                }
                returnValue = new SearchResult(groups, true);

            } else {
                returnValue = new SearchResult(new ArrayList<String>(), false);
            }
        }
        // other cases
        catch (WIMException toCatch) {
            // f113366
            // PM37404 Catch the invalid uniqueName exception for get() API
            if (toCatch instanceof EntityNotFoundException
                || toCatch instanceof InvalidUniqueNameException
                || toCatch instanceof EntityNotInRealmScopeException) {
                returnValue = new SearchResult(new ArrayList<String>(), false);
            } else {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, methodName + " " + toCatch.getMessage(), toCatch);
                }
                if (tc.isErrorEnabled()) {
                    Tr.error(tc, toCatch.getMessage(), toCatch);
                }
                throw new RegistryException(toCatch.getMessage(), toCatch);
            }
        }
        return returnValue;
    }
}
