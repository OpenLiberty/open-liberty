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

import java.util.List;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.security.wim.Service;
import com.ibm.websphere.security.wim.ras.WIMMessageHelper;
import com.ibm.websphere.security.wim.ras.WIMMessageKey;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.registry.EntryNotFoundException;
import com.ibm.ws.security.registry.RegistryException;
import com.ibm.ws.security.wim.registry.dataobject.IDAndRealm;
import com.ibm.wsspi.security.wim.SchemaConstants;
import com.ibm.wsspi.security.wim.exception.EntityNotFoundException;
import com.ibm.wsspi.security.wim.exception.WIMException;
import com.ibm.wsspi.security.wim.model.Context;
import com.ibm.wsspi.security.wim.model.Control;
import com.ibm.wsspi.security.wim.model.Entity;
import com.ibm.wsspi.security.wim.model.ExternalNameControl;
import com.ibm.wsspi.security.wim.model.Group;
import com.ibm.wsspi.security.wim.model.IdentifierType;
import com.ibm.wsspi.security.wim.model.LoginAccount;
import com.ibm.wsspi.security.wim.model.Root;
import com.ibm.wsspi.security.wim.model.SearchControl;

/**
 * Bridge class for mapping user and group security name methods.
 *
 */
public class SecurityNameBridge {

    private static final TraceComponent tc = Tr.register(SecurityNameBridge.class);

    /**
     * Property mappings.
     */
    private TypeMappings propertyMap = null;

    /**
     * Mappings utility class.
     */
    private BridgeUtils mappingUtils = null;

    /**
     * Default constructor.
     *
     * @param mappingUtil
     */
    public SecurityNameBridge(BridgeUtils mappingUtil) {
        this.mappingUtils = mappingUtil;
        propertyMap = new TypeMappings(mappingUtil);
        // initialize the method name
    }

    @FFDCIgnore(WIMException.class)
    public String getUserSecurityName(String inputUniqueUserId) throws EntryNotFoundException, RegistryException {
        // initialize the method name
        String methodName = "getUserSecurityName";
        // initialize the return value
        String returnValue = "";
        // bridge the APIs
        try {
            // validate the id
            this.mappingUtils.validateId(inputUniqueUserId);
            // separate the ID and the realm
            IDAndRealm idAndRealm = this.mappingUtils.separateIDAndRealm(inputUniqueUserId);
            // create an empty root DataObject
            Root root = this.mappingUtils.getWimService().createRootObject();

            // if realm is defined
            if (idAndRealm.isRealmDefined()) {
                // create the realm DataObject
                this.mappingUtils.createRealmDataObject(root, idAndRealm.getRealm());
                List<Context> contexts = root.getContexts();
                if (contexts != null) {
                    Context ctx = new Context();
                    ctx.setKey(Service.CONFIG_PROP_ALLOW_OPERATION_IF_REPOS_DOWN);
                    ctx.setValue(Boolean.valueOf(this.mappingUtils.getCoreConfiguration().isAllowOpIfRepoDown(idAndRealm.getRealm())));
                    contexts.add(ctx);
                }

            }

            //PM55588 Read the custom property from BridgeUtils
            boolean allowDNAsPrincipalName = this.mappingUtils.allowDNAsPrincipalName;
            if (allowDNAsPrincipalName) {
                List<Context> contexts = root.getContexts();
                if (contexts != null) {
                    Context ctx = new Context();
                    ctx.setKey(SchemaConstants.ALLOW_DN_PRINCIPALNAME_AS_LITERAL);
                    ctx.setValue(allowDNAsPrincipalName);
                    contexts.add(ctx);
                }
            }

            // if MAP(uniqueUserId) is an IdentifierType property
            // d112199

            //PM55588 Change the order, first call search API
            String inputAttrName = this.propertyMap.getInputUniqueUserId(idAndRealm.getRealm());
            String outputAttrName = this.propertyMap.getOutputUserSecurityName(idAndRealm.getRealm());
            if (!this.mappingUtils.isIdentifierTypeProperty(inputAttrName) || allowDNAsPrincipalName) {
                List<Control> controls = root.getControls();
                SearchControl searchControl = new SearchControl();
                if (controls != null) {
                    controls.add(searchControl);
                }
                // add MAP(userSecurityName) to the return list of properties
                // d115256
                if (!this.mappingUtils.isIdentifierTypeProperty(outputAttrName)) {
                    searchControl.getProperties().add(outputAttrName);
                }
                // set the "expression" string to "type=LoginAccount and MAP(uniqueUserId)="user""
                String quote = "'";
                String id = idAndRealm.getId();
                if (id.indexOf("'") != -1) {
                    quote = "\"";
                }

                // d112199

                String inputName = null;
                if (allowDNAsPrincipalName) {
                    inputName = SchemaConstants.PROP_PRINCIPAL_NAME;
                } else {
                    inputName = inputAttrName;
                }
                searchControl.setExpression("//" + Service.DO_ENTITIES + "[@xsi:type='"
                                            + Service.DO_LOGIN_ACCOUNT + "' and "
                                            + inputName
                                            + "=" + quote + id + quote + "]");

                // Set context to use userFilter if applicable
                Context context = new Context();
                context.set("key", SchemaConstants.USE_USER_FILTER_FOR_SEARCH);
                context.set("value", id);
                root.getContexts().add(context);

                // invoke ProfileService.search with the input root DataGraph
                root = this.mappingUtils.getWimService().search(root);
            }
            List<Entity> returnList = root.getEntities();

            if (this.mappingUtils.isIdentifierTypeProperty(inputAttrName) && (returnList == null || returnList.size() == 0)) {
                // add MAP(userSecurityName) to the return list of properties
                // d115256
                if (!this.mappingUtils.isIdentifierTypeProperty(outputAttrName)) {
                    this.mappingUtils.createPropertyControlDataObject(root, outputAttrName);
                }
                // use the root DataGraph to create a LoginAccount DataObject
                List<Entity> entities = root.getEntities();
                LoginAccount loginAccount = new LoginAccount();
                if (entities != null) {
                    entities.add(loginAccount);
                }
                // set the MAP(uniqueUserId) to user
                // d112199
                IdentifierType idfType = new IdentifierType();
                idfType.set(inputAttrName, idAndRealm.getId());
                loginAccount.setIdentifier(idfType);

                // Create an external name control if the external identifier is being used
                if (inputAttrName.equals(Service.PROP_EXTERNAL_NAME)) {
                    List<Control> extCtrls = root.getControls();
                    if (extCtrls != null) {
                        extCtrls.add(new ExternalNameControl());
                    }
                }
                // invoke ProfileService.get with the input root DataGraph
                root = this.mappingUtils.getWimService().get(root);
            }

            // return the value of MAP(userSecurtyName) from the output DataGraph
            returnList = root.getEntities();
            // the user was not found or more than one user was found
            // d125249
            if (returnList.isEmpty()) {
                String msg = Tr.formatMessage(tc, WIMMessageKey.ENTITY_NOT_FOUND, WIMMessageHelper.generateMsgParms(inputUniqueUserId));
                throw new EntityNotFoundException(WIMMessageKey.ENTITY_NOT_FOUND, msg);
            } else if (returnList.size() != 1) {
                String msg = Tr.formatMessage(tc, WIMMessageKey.MULTIPLE_PRINCIPALS_FOUND, WIMMessageHelper.generateMsgParms(inputUniqueUserId));
                throw new EntityNotFoundException(WIMMessageKey.MULTIPLE_PRINCIPALS_FOUND, msg);
            }
            // the user was found
            else {
                Entity entity = returnList.get(0);
                if (entity != null) {
                    // d115256
                    if (!this.mappingUtils.isIdentifierTypeProperty(outputAttrName)) {
                        Object value = entity.get(outputAttrName);
                        if (value instanceof List<?>) {
                            returnValue = BridgeUtils.getStringValue(((List<?>) value).get(0));
                        } else {
                            returnValue = BridgeUtils.getStringValue(value);
                        }
                    } else {
                        returnValue = (String) entity.getIdentifier().get(outputAttrName);
                    }
                }
            }
        } catch (WIMException toCatch) {
            BridgeUtils.handleExceptions(toCatch);
        }
        return returnValue;
    }

    @FFDCIgnore(WIMException.class)
    public String getGroupSecurityName(String inputUniqueGroupId) throws EntryNotFoundException, RegistryException {
        // initialize the method name
        String methodName = "getGroupSecurityName";
        // initialize the return value
        String returnValue = "";
        // bridge the APIs
        try {
            // validate the id
            this.mappingUtils.validateId(inputUniqueGroupId);
            // separate the ID and the realm
            IDAndRealm idAndRealm = this.mappingUtils.separateIDAndRealm(inputUniqueGroupId);
            // create an empty root DataObject
            Root root = this.mappingUtils.getWimService().createRootObject();
            // if realm is defined
            if (idAndRealm.isRealmDefined()) {
                // set "WIM.Realm" in the Context DataGraph to the realm
                this.mappingUtils.createRealmDataObject(root, idAndRealm.getRealm());
            }
            // if MAP(uniqueGroupId) is an IdentifierType property
            // d112199
            String inputAttrName = this.propertyMap.getInputUniqueGroupId(idAndRealm.getRealm());
            String outputAttrName = this.propertyMap.getOutputGroupSecurityName(idAndRealm.getRealm());
            if (this.mappingUtils.isIdentifierTypeProperty(inputAttrName)) {
                // use the root DataGraph to create a PropertyControl DataGraph
                // d115913
                if (!this.mappingUtils.isIdentifierTypeProperty(outputAttrName)) {
                    this.mappingUtils.createPropertyControlDataObject(root, outputAttrName);
                }
                // use the root DataGraph to create a Group DataObject
                List<Entity> entities = root.getEntities();
                Group group = null;
                if (entities != null) {
                    group = new Group();
                    entities.add(group);
                }
                // set MAP(uniqueGroupId) to group
                // d112199
                IdentifierType grpIdfType = new IdentifierType();
                grpIdfType.set(inputAttrName, idAndRealm.getId());
                if (group != null) {
                    group.setIdentifier(grpIdfType);
                }
                // Create an external name control if the external identifier is being used
                if (inputAttrName.equals(Service.PROP_EXTERNAL_NAME)) {
                    List<Control> extCtrls = root.getControls();
                    if (extCtrls != null) {
                        extCtrls.add(new ExternalNameControl());
                    }
                }
                // invoke ProfileService.get with the input root DataGraph
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
                // set the "expression" string to "type=Group and MAP(uniqueGroupId)="group""
                String quote = "'";
                String id = idAndRealm.getId();
                if (id.indexOf("'") != -1) {
                    quote = "\"";
                }

                // d112199
                searchControl.setExpression("//" + Service.DO_ENTITIES + "[@xsi:type='"
                                            + Service.DO_GROUP + "' and " + inputAttrName
                                            + "=" + quote + id + quote + "]");

                // Set context to use groupFilter if applicable
                Context context = new Context();
                context.set("key", SchemaConstants.USE_GROUP_FILTER_FOR_SEARCH);
                context.set("value", id);
                root.getContexts().add(context);

                // invoke ProfileService.search with the input root DataGraph
                root = this.mappingUtils.getWimService().search(root);
            }
            // return the value of MAP(groupSecurityName) from the output DataGraph
            List<Entity> returnList = root.getEntities();
            // the group was not found or more than one group was found
            // d125249
            if (returnList.isEmpty()) {
                String msg = Tr.formatMessage(tc, WIMMessageKey.ENTITY_NOT_FOUND, WIMMessageHelper.generateMsgParms(inputUniqueGroupId));
                throw new EntityNotFoundException(WIMMessageKey.ENTITY_NOT_FOUND, msg);
            } else if (returnList.size() != 1) {
                String msg = Tr.formatMessage(tc, WIMMessageKey.MULTIPLE_PRINCIPALS_FOUND, WIMMessageHelper.generateMsgParms(inputUniqueGroupId));
                throw new EntityNotFoundException(WIMMessageKey.MULTIPLE_PRINCIPALS_FOUND, msg);
            }
            // the group was found
            else {
                Entity group = returnList.get(0);
                // d115913
                if (!this.mappingUtils.isIdentifierTypeProperty(outputAttrName)) {
                    Object value = group.get(outputAttrName);

                    if (value instanceof List<?>) {
                        returnValue = BridgeUtils.getStringValue(((List<?>) value).get(0));
                    } else {
                        returnValue = BridgeUtils.getStringValue(value);
                    }

                } else {
                    returnValue = (String) group.getIdentifier().get(outputAttrName);
                }
            }
        } catch (WIMException toCatch) {
            BridgeUtils.handleExceptions(toCatch);
        }
        return returnValue;
    }
}
