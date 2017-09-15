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

import java.util.List;
import java.util.Map;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.security.wim.ras.WIMMessageKey;
import com.ibm.ws.security.wim.adapter.urbridge.URBridge;
import com.ibm.ws.security.wim.util.SchemaConstantsInternal;
import com.ibm.wsspi.security.wim.SchemaConstants;
import com.ibm.wsspi.security.wim.exception.WIMApplicationException;
import com.ibm.wsspi.security.wim.exception.WIMException;
import com.ibm.wsspi.security.wim.model.Entity;

public abstract class URBridgeEntity {

    /**
     * Register the class to trace service.
     */
    private final static TraceComponent tc = Tr.register(URBridgeEntity.class);

    private final static String LOCAL_DOMAIN_REGISTRY_PROP = "com.ibm.websphere.registry.UseRegistry";
    private final static String SECURITY_NAME_RDN_PROP = "ldap.basedn";
    protected Map<String, String> attrMap;
    protected String baseEntryName;
    // protected UserRegistry reg;
    protected final URBridge urBridge;
    protected Entity entity;
    protected Map<String, String> entityConfigMap;

    protected String securityNameProp;
    protected String uniqueIdProp;
    protected String displayNameProp;
    protected String rdnProp;

    /**
     * Create an URBridgeEntity.
     *
     * @param inEntity The entity to be wrapped by the URBridgeEntity. This
     *            entity's type determines which type of URBridgeEntity
     *            is created.
     * @param urBridge The UserRegisty associated with the adapter.
     * @param inAttrMap An attribute map containing the configuration information
     *            on how underlying UR attributes map to WIM attributes.
     * @param baseEntryName The name of the baseEntryName.
     */
    public URBridgeEntity(Entity inEntity, URBridge urBridge, Map<String, String> inAttrMap, String inBaseEntryName,
                          Map<String, String> inEntityConfigMap) {
        super();
        entity = inEntity;
        this.urBridge = urBridge;
        attrMap = inAttrMap;
        entityConfigMap = inEntityConfigMap;
        baseEntryName = inBaseEntryName;
    }

    /**
     * Retrieve the groupSecurityName or userSecurityName from the registry.
     *
     * @param uniqueId the uniqueGroupId or uniqueUserId of the group
     *            or user in the underlying registry.
     */
    public abstract String getSecurityNameForEntity(String arg) throws Exception;

    /**
     * Retrieve the uniqueGroupId or uniqueUserId from the registry.
     *
     * @param securityName the groupSecurityName or userSecurityName of the user
     *            or group in the underlying registry.
     */
    public abstract String getUniqueIdForEntity(String arg) throws Exception;

    /**
     * Retrieve the groupDisplayName or userDisplayName from the registry.
     *
     * @param securityName the securityName of the user or group in the
     *            underlying registry.
     */
    public abstract String getDisplayNameForEntity(String arg) throws Exception;

    /**
     *
     * Adds a list of specified attributes to the entity.
     *
     * @param attrList a list of attributes to be added to the entity
     * @throws Exception an invalid attribute was requested or an exception
     *             was thrown from an underlying function.
     */
    public void populateEntity(List<String> attrList) throws WIMException {
        boolean uniqueIdPropSet = false;
        setIdentifierProperties();

        // Loop through the attributes to try to set each.
        for (int i = 0; i < attrList.size(); i++) {
            String attrName = attrList.get(i);
            try {
                if (attrName.equals(displayNameProp)) {
                    getDisplayName(true);
                } else if (attrName.equals(rdnProp)) {
                    entity.set(rdnProp, entity.getIdentifier().get(securityNameProp));
                } else if (attrName.equals(SchemaConstants.PROP_PRINCIPAL_NAME) || attrName.equals(SchemaConstantsInternal.PROP_DISPLAY_BRIDGE_PRINCIPAL_NAME)) {
                    if ((attrMap.containsKey(LOCAL_DOMAIN_REGISTRY_PROP) && (attrMap.get(LOCAL_DOMAIN_REGISTRY_PROP).toString().equalsIgnoreCase("local")
                                                                             || attrMap.get(LOCAL_DOMAIN_REGISTRY_PROP).toString().equalsIgnoreCase("domain")))
                        || attrMap.containsKey(SECURITY_NAME_RDN_PROP)) {
                        entity.set(SchemaConstants.PROP_PRINCIPAL_NAME, urBridge.getUserSecurityName(getUniqueId(true)));
                    } else {
                        entity.set(SchemaConstants.PROP_PRINCIPAL_NAME,
                                   entity.getIdentifier().get(securityNameProp));
                    }
                }
            } catch (Exception e) {
                throw new WIMApplicationException(e);
            }
        }
        if (entity.getIdentifier().isSet(uniqueIdProp)) {
            entity.getIdentifier().set(SchemaConstants.PROP_EXTERNAL_ID, entity.getIdentifier().get(uniqueIdProp));
        }
        if (!uniqueIdPropSet) {
            entity.getIdentifier().unset(uniqueIdProp);
        }
    }

    /**
     * Sets the securityNameProperty. This is useful when creating a new object
     * and wanting to populate its attributes. Mainly because you need 1 attribute
     * before you can retrieve others.
     *
     * @param securityName used to set the securityName property
     * @Exception an error occurred in the underlying repository.
     */
    public void setSecurityNameProp(String securityName) {
        if (securityName != null)
            entity.getIdentifier().set(securityNameProp, securityName);
    }

    public void setPrincipalName(String securityName) {
        if (securityName != null)
            entity.set(SchemaConstants.PROP_PRINCIPAL_NAME, securityName);
    }

    /**
     * Returns the securityName of a user or a group.
     *
     * @param setAttr a boolean indicating whether to actually set the attribute
     *            in the entity or just perform a lookup.
     * @return the user or group's securityName.
     * @throws Exception identifier values are missing or the underlying
     *             registry threw an error.
     */
    public String getSecurityName(boolean setAttr) throws Exception {
        String securityName = null;
        if (entity.getIdentifier().isSet(securityNameProp)) {
            securityName = (String) entity.getIdentifier().get(securityNameProp);
        }

        // Neither identifier is set.
        if (securityName == null && !entity.getIdentifier().isSet(uniqueIdProp)) {
            throw new WIMApplicationException(WIMMessageKey.REQUIRED_IDENTIFIERS_MISSING, Tr.formatMessage(tc, WIMMessageKey.REQUIRED_IDENTIFIERS_MISSING));
        } else if (securityName == null) {
            String uniqueId = (String) entity.getIdentifier().get(uniqueIdProp);

            // Get the securityName.
            securityName = getSecurityNameForEntity(uniqueId);
        }

        // Set the attribute in the WIM entity if requested. If it needs a RDN style name, make it.
        if (setAttr) {
            // Construct RDN style from securityName only if its not already a DN
            // securityName = (UniqueNameHelper.isDN(securityName) == null ||
            //                !StringUtil.endsWithIgnoreCase(securityName, "," + baseEntryName)) ? securityName : securityName;
            entity.getIdentifier().set(securityNameProp, securityName);
        }

        return securityName;
    }

    public void setIdentifierProperties() throws WIMException {
        try {
            getUniqueId(true);
            getSecurityName(true);
        } catch (Exception e) {
            throw new WIMApplicationException(e);
        }
    }

    /**
     * Returns the uniqueId of a user or a group.
     *
     * @param setAttr a boolean indicating whether to actually set the attribute
     *            in the entity or just perform a lookup.
     * @return the user or group's uniqueId.
     * @throws Exception identifier values are missing or the underlying
     *             registry threw an error.
     */
    public String getUniqueId(boolean setAttr) throws Exception {
        String uniqueName = null;
        String uniqueId = null;
        if (entity.getIdentifier().isSet(uniqueIdProp)) {
            uniqueId = (String) entity.getIdentifier().get(uniqueIdProp);
            return uniqueId;
        }
        uniqueName = (String) entity.getIdentifier().get(securityNameProp);

        if ((uniqueId == null) && (uniqueName == null)) {
            throw new WIMApplicationException(WIMMessageKey.REQUIRED_IDENTIFIERS_MISSING, Tr.formatMessage(tc, WIMMessageKey.REQUIRED_IDENTIFIERS_MISSING));
        }

        // Get the attribute value. If it's part of the DN we must strip it out.
        // ZZZZ uniqueName = stripRDN(uniqueName);
        uniqueId = getUniqueIdForEntity(uniqueName);

        if (setAttr) {
            // Set the attribute in the WIM entity.
            entity.getIdentifier().set(uniqueIdProp, uniqueId);
        }

        return uniqueId;
    }

    /**
     * Returns the displayName of a user or a group.
     *
     * @param setAttr a boolean indicating whether to actually set the attribute
     *            in the entity or just perform a lookup.
     * @return the user or group's displayName.
     * @throws Exception identifier values are missing or the underlying
     *             registry threw an error.
     */
    @SuppressWarnings("unchecked")
    public String getDisplayName(boolean setAttr) throws Exception {
        String displayName = null;
        String securityName = getSecurityName(false);
        displayName = getDisplayNameForEntity(securityName);

        if (!((displayName == null) || (displayName.trim().length() == 0)) && setAttr)
            ((List<String>) entity.get(URBridgeConstants.DISPLAY_NAME)).add(displayName);

        return displayName;
    }

    public void setRDNPropValue(String value) {
        if (value != null)
            entity.set(entityConfigMap.get(entity.getTypeName()), value);
    }

    /**
     * Ensure that an exception will be thrown if this function is called
     * but not implemented by the child class.
     *
     * @param grpMbrshipAttrs
     * @throws Exception on all calls.
     */
    public void getGroupsForUser(List<String> grpMbrshipAttrs, int countLimit) throws Exception {
        throw new WIMApplicationException(WIMMessageKey.METHOD_NOT_IMPLEMENTED, Tr.formatMessage(tc, WIMMessageKey.METHOD_NOT_IMPLEMENTED));
    }

    /**
     * Ensure that an exception will be thrown if this function is called
     * but not implemented by the child class.
     *
     * @param grpMbrshipAttrs
     * @throws Exception on all calls.
     */
    public void getUsersForGroup(List<String> grpMbrAttrs, int countLimit) throws Exception {
        throw new WIMApplicationException(WIMMessageKey.METHOD_NOT_IMPLEMENTED, Tr.formatMessage(tc, WIMMessageKey.METHOD_NOT_IMPLEMENTED));
    }
}
