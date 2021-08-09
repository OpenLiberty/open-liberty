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

import java.util.HashMap;
import java.util.List;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.security.wim.Service;
import com.ibm.websphere.security.wim.ras.WIMMessageHelper;
import com.ibm.websphere.security.wim.ras.WIMMessageKey;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.registry.EntryNotFoundException;
import com.ibm.ws.security.registry.RegistryException;
import com.ibm.ws.security.wim.registry.dataobject.IDAndRealm;
import com.ibm.ws.security.wim.util.SchemaConstantsInternal;
import com.ibm.wsspi.security.wim.SchemaConstants;
import com.ibm.wsspi.security.wim.exception.EntityNotFoundException;
import com.ibm.wsspi.security.wim.exception.WIMException;
import com.ibm.wsspi.security.wim.model.Context;
import com.ibm.wsspi.security.wim.model.Control;
import com.ibm.wsspi.security.wim.model.Entity;
import com.ibm.wsspi.security.wim.model.Root;
import com.ibm.wsspi.security.wim.model.SearchControl;

/**
 * Bridge class for mapping user and group unique ID methods.
 *
 */
public class UniqueIdBridge {

    private static final TraceComponent tc = Tr.register(UniqueIdBridge.class);

    /**
     * Property mappings.
     */
    private TypeMappings propertyMap = null;

    /**
     * Mappings utility class.
     */
    private BridgeUtils mappingUtils = null;

    public UniqueIdBridge(BridgeUtils mappingUtil) {
        this.mappingUtils = mappingUtil;
        propertyMap = new TypeMappings(mappingUtils);
    }

    @FFDCIgnore(InvalidNameException.class)
    public static boolean isDN(String uniqueName) {
        if (uniqueName == null)
            return false;

        try {
            new LdapName(uniqueName);
            return true;
        } catch (InvalidNameException e) {
            return false;
        }
    }

    @FFDCIgnore({ WIMException.class, InvalidNameException.class })
    public HashMap<String, String> getUniqueUserId(String inputUserSecurityName) throws EntryNotFoundException, RegistryException {
        String methodName = "getUniqueUserId";
        // initialize the return value
        String returnValue = "";
        Root root = null;

        // bridge the APIs
        try {
            // validate the id
            this.mappingUtils.validateId(inputUserSecurityName);
            // separate the ID and the realm
            IDAndRealm idAndRealm = this.mappingUtils.separateIDAndRealm(inputUserSecurityName);
            // create an empty root DataGraph
            root = this.mappingUtils.getWimService().createRootObject();
            // if realm is defined
            if (idAndRealm.isRealmDefined()) {
                // set "WIM.Realm" in the Context DataGraph to the realm
                this.mappingUtils.createRealmDataObject(root, idAndRealm.getRealm());
                List<Context> contexts = root.getContexts();
                if (contexts != null) {
                    Context ctx = new Context();
                    ctx.setKey(Service.CONFIG_PROP_ALLOW_OPERATION_IF_REPOS_DOWN);
                    ctx.setValue(Boolean.valueOf(this.mappingUtils.getCoreConfiguration().isAllowOpIfRepoDown(idAndRealm.getRealm())));
                    contexts.add(ctx);
                }
            }

            //PK63962
            String quote = "'";
            String id = idAndRealm.getId();
            if (id.indexOf("'") != -1) {
                quote = "\"";
            }

            // get input and output values
            String inputAttrName = this.propertyMap.getInputUserSecurityName(idAndRealm.getRealm());

            // Dynamically handle both principalName and uniqueName
            inputAttrName = this.mappingUtils.getRealInputAttrName(inputAttrName, id, true);

            String outputAttrName = this.propertyMap.getOutputUniqueUserId(idAndRealm.getRealm());

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

            // Add context for URBridge
            Context context = new Context();
            context.setKey(SchemaConstantsInternal.IS_URBRIDGE_RESULT);
            context.setValue("false");
            root.getContexts().add(context);

            Root resultRoot = null;
            try {
                // get the entity if the input parameter is an identifier type
                // if the input value is not a DN, then search on the principal name
                // this will allow the security name to be either a shortname or DN
                resultRoot = this.mappingUtils.getEntityByIdentifier(root, inputAttrName,
                                                                     id, outputAttrName, this.mappingUtils);
            } catch (WIMException e) {
//                if (!allowDNAsPrincipalName)
//                    throw e;
                /*
                 * This is OK. Let's search for it below.
                 */
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Ignoring exception: " + e.getMessage(), e);
                }
            }

            // Did you find data in URBridge
            boolean foundInURBridge = false;
            if (resultRoot != null && !resultRoot.getEntities().isEmpty()) {
                // Determine if the return object to check if the context was set.
                List<Context> contexts = resultRoot.getContexts();
                for (Context ctx : contexts) {
                    String key = ctx.getKey();

                    if (key != null && SchemaConstantsInternal.IS_URBRIDGE_RESULT.equals(key)) {
                        if ("true".equalsIgnoreCase((String) ctx.getValue()))
                            foundInURBridge = true;
                    }
                }
            }

            if (resultRoot != null && !resultRoot.getEntities().isEmpty() && (isDN(id) || foundInURBridge)) {
                root = resultRoot;
            } else if (!this.mappingUtils.isIdentifierTypeProperty(inputAttrName) || allowDNAsPrincipalName) {
                if (allowDNAsPrincipalName)
                    inputAttrName = SchemaConstants.PROP_PRINCIPAL_NAME;

                // use the root DataGraph to create a SearchControl DataGraph
                List<Control> controls = root.getControls();
                SearchControl searchControl = new SearchControl();
                if (controls != null) {
                    controls.add(searchControl);
                }
                // if MAP(uniqueUserId) is not an IdentifierType property
                // d112199
                if (!this.mappingUtils.isIdentifierTypeProperty(outputAttrName)) {
                    searchControl.getProperties().add(outputAttrName);
                }

                // set the "expression" string to "type=LoginAccount and MAP(userSecurityName)="user""
                // d112199
                searchControl.setExpression("//" + Service.DO_ENTITIES + "[@xsi:type='"
                                            + Service.DO_LOGIN_ACCOUNT + "' and "
                                            + inputAttrName
                                            + "=" + quote + id + quote + "]");

                // Set context to use userFilter if applicable
                context = new Context();
                context.set("key", SchemaConstants.USE_USER_FILTER_FOR_SEARCH);
                context.set("value", id);
                root.getContexts().add(context);

                // invoke ProfileService.search with the input root DataGraph
                root = this.mappingUtils.getWimService().search(root);
            }

            // return the value of MAP(userUniqueId) from the output DataGraph
            List<Entity> returnList = root.getEntities();
            // the user was not found or more than one user was found
            // d125249
            if (returnList.isEmpty()) {
                // if (tc.isErrorEnabled()) {
                //     Tr.error(tc, WIMMessageKey.ENTITY_NOT_FOUND, WIMMessageHelper.generateMsgParms(inputUserSecurityName));
                // }
                throw new EntityNotFoundException(WIMMessageKey.ENTITY_NOT_FOUND, Tr.formatMessage(
                                                                                                   tc,
                                                                                                   WIMMessageKey.ENTITY_NOT_FOUND,
                                                                                                   WIMMessageHelper.generateMsgParms(inputUserSecurityName)));
            } else if (returnList.size() != 1) {
                // if (tc.isErrorEnabled()) {
                //     Tr.error(tc, WIMMessageKey.MULTIPLE_PRINCIPALS_FOUND, WIMMessageHelper.generateMsgParms(inputUserSecurityName));
                // }
                throw new EntityNotFoundException(WIMMessageKey.MULTIPLE_PRINCIPALS_FOUND, Tr.formatMessage(
                                                                                                            tc,
                                                                                                            WIMMessageKey.MULTIPLE_PRINCIPALS_FOUND,
                                                                                                            WIMMessageHelper.generateMsgParms(inputUserSecurityName)));
            }
            // the user was found
            else {
                Entity entity = returnList.get(0);
                if (entity != null) {
                    if (!this.mappingUtils.isIdentifierTypeProperty(outputAttrName)) {
                        Object value = entity.get(outputAttrName);
                        if (value instanceof List<?>) {
                            returnValue = BridgeUtils.getStringValue(((List<?>) value).get(0));
                        } else {
                            returnValue = BridgeUtils.getStringValue(value);
                        }
                    } else {
                        returnValue = BridgeUtils.getStringValue(entity.getIdentifier().get(outputAttrName));
                    }
                    // PM50390
                    if (mappingUtils.returnRealmInfoInUniqueUserId && idAndRealm.isRealmDefined()
                        || (idAndRealm.isRealmDefined() && (!this.mappingUtils.getDefaultRealmName().equals(idAndRealm.getRealm())))) {
                        returnValue += idAndRealm.getDelimiter() + idAndRealm.getRealm();
                    }
                }

                // if return attribute is uniqueName and returnValue is not in DN format, default to uniqueId (as this is potentially a customRegistry data)
                if (SchemaConstants.PROP_UNIQUE_NAME.equalsIgnoreCase(outputAttrName)) {
                    try {
                        new LdapName(returnValue);
                    } catch (InvalidNameException e) {
                        String uid = entity.getIdentifier().getUniqueId();
                        if (uid != null)
                            returnValue = uid;
                    }
                }
            }
        } catch (WIMException toCatch) {
            BridgeUtils.handleExceptions(toCatch);
        }

        // Determine if the returned object to check if the context was set.
        String isURBrigeResult = "false";
        if (root != null) {
            List<Context> contexts = root.getContexts();
            for (Context contextInput : contexts) {
                String key = contextInput.getKey();

                if (key != null && SchemaConstantsInternal.IS_URBRIDGE_RESULT.equals(key)) {
                    isURBrigeResult = String.valueOf(contextInput.getValue());
                }
            }
        }

        HashMap<String, String> result = new HashMap<String, String>();
        result.put(SchemaConstantsInternal.IS_URBRIDGE_RESULT, isURBrigeResult);
        result.put("RESULT", returnValue);

        return result;
    }

    @FFDCIgnore({ WIMException.class, InvalidNameException.class })
    public String getUniqueGroupId(String inputGroupSecurityName) throws EntryNotFoundException, RegistryException {

        String methodName = "getUniqueGroupId";
        // initialize the return value
        String returnValue = "";
        // bridge the APIs
        try {
            // validate the id
            this.mappingUtils.validateId(inputGroupSecurityName);
            // separate the ID and the realm
            IDAndRealm idAndRealm = this.mappingUtils.separateIDAndRealm(inputGroupSecurityName);
            // create an empty root DataObject
            Root root = this.mappingUtils.getWimService().createRootObject();
            // if realm is defined
            if (idAndRealm.isRealmDefined()) {
                // create the realm DataObject
                this.mappingUtils.createRealmDataObject(root, idAndRealm.getRealm());
            }
            //PK63962
            String quote = "'";
            String id = idAndRealm.getId();
            if (id.indexOf("'") != -1) {
                quote = "\"";
            }

            // get input and output values
            String inputAttrName = this.propertyMap.getInputGroupSecurityName(idAndRealm.getRealm());
            inputAttrName = this.mappingUtils.getRealInputAttrName(inputAttrName, id, false);
            String outputAttrName = this.propertyMap.getOutputUniqueGroupId(idAndRealm.getRealm());

            // get the entity if the input parameter is an identifier type
            // if the input value is not a DN, then search on the group RDN
            // this will allow the security name to be either a shortname or DN
            Root resultRoot = null;
            if (isDN(id))
                resultRoot = this.mappingUtils.getEntityByIdentifier(root, inputAttrName,
                                                                     id, outputAttrName, this.mappingUtils);

            if (resultRoot != null && !resultRoot.getEntities().isEmpty()) {
                root = resultRoot;
            } else {
                // use the root DataGraph to create a SearchControl DataGraph
                List<Control> controls = root.getControls();
                SearchControl searchControl = new SearchControl();
                if (controls != null) {
                    controls.add(searchControl);
                }
                // if MAP(uniqueGroupId) is not an IdentifierType property
                // d112199
                if (!this.mappingUtils.isIdentifierTypeProperty(outputAttrName)) {
                    searchControl.getProperties().add(outputAttrName);
                }

                // set the "expression" string to "type=Group and MAP(groupSecurityName)="group""
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

            // return the value of MAP(uniqueGroupId) from the output DataGraph
            List<Entity> returnList = root.getEntities();

            // the group was not found or more than one group was found
            // d125249
            if (returnList.isEmpty()) {
                // if (tc.isErrorEnabled()) {
                //     Tr.error(tc, WIMMessageKey.ENTITY_NOT_FOUND, WIMMessageHelper.generateMsgParms(inputGroupSecurityName));
                // }
                throw new EntityNotFoundException(WIMMessageKey.ENTITY_NOT_FOUND, Tr.formatMessage(
                                                                                                   tc,
                                                                                                   WIMMessageKey.ENTITY_NOT_FOUND,
                                                                                                   WIMMessageHelper.generateMsgParms(inputGroupSecurityName)));
            } else if (returnList.size() != 1) {
                if (tc.isErrorEnabled()) {
                    Tr.error(tc, WIMMessageKey.MULTIPLE_PRINCIPALS_FOUND, WIMMessageHelper.generateMsgParms(inputGroupSecurityName));
                }
                throw new EntityNotFoundException(WIMMessageKey.MULTIPLE_PRINCIPALS_FOUND, Tr.formatMessage(
                                                                                                            tc,
                                                                                                            WIMMessageKey.MULTIPLE_PRINCIPALS_FOUND,
                                                                                                            WIMMessageHelper.generateMsgParms(inputGroupSecurityName)));
            }
            // the group was found
            else {
                Entity group = returnList.get(0);
                // d113801
                if (!this.mappingUtils.isIdentifierTypeProperty(outputAttrName)) {
                    Object value = group.get(outputAttrName);
                    if (value instanceof List<?>) {
                        returnValue = BridgeUtils.getStringValue(((List<?>) value).get(0));
                    } else {
                        returnValue = BridgeUtils.getStringValue(value);
                    }
                } else {
                    returnValue = BridgeUtils.getStringValue(group.getIdentifier().get(outputAttrName));
                }

                // if return attribute is uniqueName and returnValue is not in DN format, default to uniqueId (as this is potentially a customRegistry data)
                if (SchemaConstants.PROP_UNIQUE_NAME.equalsIgnoreCase(outputAttrName)) {
                    try {
                        new LdapName(returnValue);
                    } catch (InvalidNameException e) {
                        String uid = group.getIdentifier().getUniqueId();
                        if (uid != null)
                            returnValue = uid;
                    }
                }
            }
        } catch (WIMException toCatch) {
            BridgeUtils.handleExceptions(toCatch);
        }
        return returnValue;
    }
}