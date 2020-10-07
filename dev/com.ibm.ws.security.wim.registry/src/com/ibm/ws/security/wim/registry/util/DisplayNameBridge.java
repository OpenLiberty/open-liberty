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
import com.ibm.wsspi.security.wim.model.PersonAccount;
import com.ibm.wsspi.security.wim.model.Root;
import com.ibm.wsspi.security.wim.model.SearchControl;

/**
 * Bridge class for mapping user and group display name methods.
 *
 */
public class DisplayNameBridge {

    private static final TraceComponent tc = Tr.register(DisplayNameBridge.class);

    /**
     * Property mappings.
     */
    private TypeMappings propertyMap = null;

    /**
     * Mappings utility class.
     */
    private BridgeUtils mappingUtils = null;

    public DisplayNameBridge(BridgeUtils mappingUtil) {
        this.mappingUtils = mappingUtil;
        propertyMap = new TypeMappings(mappingUtil);
    }

    @FFDCIgnore(WIMException.class)
    public String getUserDisplayName(String inputUserSecurityName) throws EntryNotFoundException, RegistryException {
        // initialize the return value
        String returnValue = "";
        // bridge the APIs
        try {
            // validate the id
            this.mappingUtils.validateId(inputUserSecurityName);
            // separate the ID and the realm
            IDAndRealm idAndRealm = this.mappingUtils.separateIDAndRealm(inputUserSecurityName);
            // create an empty root DataObject
            Root root = this.mappingUtils.getWimService().createRootObject();
            // if realm is defined
            if (idAndRealm.isRealmDefined()) {
                // set "WIM.Realm" in the Context DataGraph to the realm
                this.mappingUtils.createRealmDataObject(root, idAndRealm.getRealm());
            }

            //PK63962
            String quote = "'";
            String id = idAndRealm.getId();
            if (id.indexOf("'") != -1) {
                quote = "\"";
            }

            // get input and output values
            String inputAttrName = this.propertyMap.getInputUserSecurityName(idAndRealm.getRealm());
            inputAttrName = this.mappingUtils.getRealInputAttrName(inputAttrName, id, true);
            String outputAttrName = this.propertyMap.getOutputUserDisplayName(idAndRealm.getRealm());
            String outputAttrNameMod = outputAttrName;

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

            // get the entity if the input parameter is an identifier type
            // if the input value is not a DN, then search on the principal name
            // this will allow the security name to be either a shortname or DN
            Root resultRoot = null;

            try {
                // New:: Change to Input/Output property
                if (outputAttrNameMod != null && outputAttrNameMod.equalsIgnoreCase(Service.PROP_PRINCIPAL_NAME))
                    outputAttrNameMod = SchemaConstantsInternal.PROP_DISPLAY_BRIDGE_PRINCIPAL_NAME;

                // get the entity if the input parameter is an identifier type
                resultRoot = this.mappingUtils.getEntityByIdentifier(root, inputAttrName,
                                                                     id, outputAttrNameMod, this.mappingUtils);
            } catch (WIMException e) {
                if (!allowDNAsPrincipalName)
                    throw e;
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

            root.getContexts().clear();

            if (resultRoot != null && !resultRoot.getEntities().isEmpty() && (isDN(id) || foundInURBridge)) {
                root = resultRoot;
            } else {
                // use the root DataGraph to create a SearchControl DataGraph

                List<Control> controls = root.getControls();
                SearchControl srchCtrl = new SearchControl();
                if (controls != null) {
                    controls.add(srchCtrl);
                }

                // if MAP(userDisplayName) is not an IdentifierType property
                // d112199
                if (!this.mappingUtils.isIdentifierTypeProperty(outputAttrName)) {
                    srchCtrl.getProperties().add(outputAttrName);
                }

                // d112199

                if (allowDNAsPrincipalName)
                    inputAttrName = "principalName";

                srchCtrl.setExpression("//" + Service.DO_ENTITIES + "[@xsi:type='"
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
            // return the value of MAP(userDisplayName) from the output DataGraph
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
                // This check might cause inconsistencies for customers whose
                // filters don't match what has been set in the entity objectclasses.
                // To fix this, they need to add the objectclass to the entity
                if (returnList.get(0) instanceof PersonAccount) {
                    PersonAccount personAccount = (PersonAccount) returnList.get(0);
                    // f113366
                    if (!this.mappingUtils.isIdentifierTypeProperty(outputAttrName)) {
                        //returnValue = loginAccount.getString(this.propertyMap.getOutputUserDisplayName(idAndRealm.getRealm()));
                        String mappedProp = outputAttrName;
                        if (mappedProp.equals("displayName")) {
                            if (personAccount.getDisplayName().size() == 0)
                                returnValue = "";
                            else
                                returnValue = personAccount.getDisplayName().get(0);
                        } else if (mappedProp.equals(SchemaConstants.PROP_PRINCIPAL_NAME) && foundInURBridge) {
                            String outputUserPrincipalAttr = this.propertyMap.getOutputUserPrincipal(idAndRealm.getRealm());
                            if (!this.mappingUtils.isIdentifierTypeProperty(outputUserPrincipalAttr)) {
                                Object value = personAccount.get(outputUserPrincipalAttr);
                                if (value instanceof List<?>) {
                                    returnValue = BridgeUtils.getStringValue(((List<?>) value).get(0));
                                } else {
                                    returnValue = BridgeUtils.getStringValue(value);
                                }
                            } else {
                                returnValue = (String) personAccount.getIdentifier().get(outputUserPrincipalAttr);
                            }
                        } else {
                            Object value = personAccount.get(mappedProp);
                            if (value instanceof List<?>) {
                                returnValue = BridgeUtils.getStringValue(((List<?>) value).get(0));
                            } else {
                                returnValue = BridgeUtils.getStringValue(value);
                            }
                        }
                    } else {
                        returnValue = BridgeUtils.getStringValue(personAccount.getIdentifier().get(outputAttrName));
                    }
                } else {
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "No groups will be returned for user because the entity was not of type \"PersonAccount\".", returnList.get(0));
                    }
                }
            }
        } catch (WIMException toCatch) {
            BridgeUtils.handleExceptions(toCatch);
        }
        return returnValue;
    }

    @FFDCIgnore(WIMException.class)
    public String getGroupDisplayName(String inputGroupSecurityName) throws EntryNotFoundException, RegistryException {
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
                // set "WIM.Realm" in the Context DataGraph to the realm
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
            String outputAttrName = this.propertyMap.getOutputGroupDisplayName(idAndRealm.getRealm());
            String outputAttrNameMod = outputAttrName;

            // New:: Change to Input/Output property
            if (outputAttrNameMod != null && outputAttrNameMod.equalsIgnoreCase("cn"))
                outputAttrNameMod = SchemaConstantsInternal.PROP_DISPLAY_BRIDGE_CN;

            // get the entity if the input parameter is an identifier type
            Root resultRoot = this.mappingUtils.getEntityByIdentifier(root, inputAttrName,
                                                                      id, outputAttrNameMod, this.mappingUtils);
            if (resultRoot != null) {
                root = resultRoot;
            } else {
                // use the root DataGraph to create a SearchControl DataGraph
                List<Control> controls = root.getControls();
                SearchControl searchControl = new SearchControl();
                if (controls != null) {
                    controls.add(searchControl);
                }
                // if MAP(groupDisplayName) is not an IdentifierType property
                // d112199
                if (!this.mappingUtils.isIdentifierTypeProperty(outputAttrName)) {
                    // add MAP(groupDisplayName) to the return list of properties
                    searchControl.getProperties().add(outputAttrName);
                }

                // d115907
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

            // return the value of MAP(groupDisplayName) from the output DataGraph
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
                // if (tc.isErrorEnabled()) {
                //     Tr.error(tc, WIMMessageKey.MULTIPLE_PRINCIPALS_FOUND, WIMMessageHelper.generateMsgParms(inputGroupSecurityName));
                // }
                throw new EntityNotFoundException(WIMMessageKey.MULTIPLE_PRINCIPALS_FOUND, Tr.formatMessage(
                                                                                                            tc,
                                                                                                            WIMMessageKey.MULTIPLE_PRINCIPALS_FOUND,
                                                                                                            WIMMessageHelper.generateMsgParms(inputGroupSecurityName)));
            }
            // the group was found
            else {
                Entity group = returnList.get(0);
                // f113366
                if (!this.mappingUtils.isIdentifierTypeProperty(outputAttrName)) {
                    // get the property to return
                    Object value = group.get(outputAttrName);
                    if (value instanceof List<?>) {
                        returnValue = BridgeUtils.getStringValue(((List<?>) value).get(0));
                    } else {
                        returnValue = BridgeUtils.getStringValue(value);
                    }
                } else {
                    // get the identifier to return
                    returnValue = BridgeUtils.getStringValue(group.getIdentifier().get(outputAttrName));
                }
            }
        } catch (WIMException toCatch) {
            BridgeUtils.handleExceptions(toCatch);
        }
        return returnValue;
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
}
