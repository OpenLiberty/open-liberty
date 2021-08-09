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
import com.ibm.websphere.security.wim.ras.WIMMessageHelper;
import com.ibm.websphere.security.wim.ras.WIMMessageKey;
import com.ibm.ws.security.wim.adapter.urbridge.URBridge;
import com.ibm.wsspi.security.wim.exception.EntityNotFoundException;
import com.ibm.wsspi.security.wim.exception.WIMApplicationException;
import com.ibm.wsspi.security.wim.exception.WIMException;
import com.ibm.wsspi.security.wim.model.Entity;
import com.ibm.wsspi.security.wim.model.Group;
import com.ibm.wsspi.security.wim.model.IdentifierType;
import com.ibm.wsspi.security.wim.model.Root;

public class URBridgePerson extends URBridgeEntity {

    /**
     * Register the class to trace service.
     */
    private final static TraceComponent tc = Tr.register(URBridgePerson.class);

    /**
     * Create an URBridgePerson. Extracts and set Property mapping
     * from the inAttrMap.
     *
     * @param inEntity The entity to be wrapped by the URBridgePerson. This
     *            entity's type determines which type of URBridgeEntity
     *            is created.
     * @param urBridge The UserRegisty associated with the adapter.
     * @param inAttrMap An attribute map containing the configuration information
     *            on how underlying UR attributes map to WIM attributes.
     * @param inBaseEntry The name of the baseEntry.
     */
    public URBridgePerson(Entity inEntity, URBridge urBridge, Map<String, String> inAttrMap, String inBaseEntry,
                          Map<String, String> inEntityConfigMap) {
        super(inEntity, urBridge, inAttrMap, inBaseEntry, inEntityConfigMap);

        // Get the attrMap. From it get the values.
        securityNameProp = inAttrMap.get(URBridgeConstants.USER_SECURITY_NAME_PROP);
        uniqueIdProp = inAttrMap.get(URBridgeConstants.UNIQUE_USER_ID_PROP);
        displayNameProp = inAttrMap.get(URBridgeConstants.USER_DISPLAY_NAME_PROP);
        rdnProp = inEntityConfigMap.get((entity.getTypeName()));
    }

    /**
     * Request to retrieve the uniqueUserId from the registry.
     *
     * @param securityName the userSecurityName of the user in the
     *            underlying registry.
     */
    @Override
    public String getUniqueIdForEntity(String securityName) throws Exception {
        return urBridge.getUniqueUserId(securityName);
    }

    /**
     * Request to retrieve the userSecurityName from the registry.
     *
     * @param uniqueId the uniqueUserId of the user in the
     *            underlying registry.
     */
    @Override
    public String getSecurityNameForEntity(String uniqueId) throws Exception {
        return urBridge.getUserSecurityName(uniqueId);
    }

    /**
     * Request to retrieve the userDisplayName from the registry.
     *
     * @param securityName the userSecurityName of the user in the
     *            underlying registry.
     */
    @Override
    public String getDisplayNameForEntity(String securityName) throws Exception {
        return urBridge.getUserDisplayName(securityName);
    }

    /**
     * Get the members for the group and add the specified attributes
     * to each of the members.
     *
     * @param grpMbrAttrs the attributes to be added to the members.
     * @WIMException an error occurs in the underlying registry
     */
    @Override
    public void getGroupsForUser(List<String> grpMbrshipAttrs, int countLimit) throws WIMException {
        String securityName = null;
        try {
            securityName = getSecurityName(false);
            List<String> returnNames = urBridge.getGroupsForUser(securityName);

            // if countLimit is zero then we should return complete result-set
            countLimit = (countLimit == 0) ? returnNames.size() : countLimit;

            // if countLImit is negative we should not return any result.
            countLimit = (countLimit < 0) ? 0 : countLimit;

            // if countLimt greater than result size then return the complete result.
            countLimit = (returnNames.size() > countLimit) ? countLimit : returnNames.size();

            for (int j = 0; j < countLimit; j++) {
                Root fakeRoot = new Root();
                Group memberDO = new Group();
                fakeRoot.getEntities().add(memberDO);

                IdentifierType identifier = new IdentifierType();
                memberDO.setIdentifier(identifier);

                URBridgeEntityFactory osFactory = new URBridgeEntityFactory();
                URBridgeEntity osEntity = osFactory.createObject(memberDO, urBridge, attrMap, baseEntryName, entityConfigMap);
                osEntity.setSecurityNameProp(returnNames.get(j));
                osEntity.populateEntity(grpMbrshipAttrs);
                osEntity.setRDNPropValue(returnNames.get(j));
                entity.getGroups().add(memberDO);
            }
        } catch (com.ibm.websphere.security.EntryNotFoundException enfe) {
            throw new EntityNotFoundException(WIMMessageKey.ENTITY_NOT_FOUND, Tr.formatMessage(tc, WIMMessageKey.ENTITY_NOT_FOUND,
                                                                                               WIMMessageHelper.generateMsgParms(securityName)));
        } catch (com.ibm.websphere.security.NotImplementedException nie) {
            throw new EntityNotFoundException(WIMMessageKey.ENTITY_NOT_FOUND, Tr.formatMessage(tc, WIMMessageKey.ENTITY_NOT_FOUND,
                                                                                               WIMMessageHelper.generateMsgParms(securityName)));
        } catch (Exception e) {
            throw new WIMApplicationException(WIMMessageKey.ENTITY_GET_FAILED, Tr.formatMessage(tc, WIMMessageKey.ENTITY_GET_FAILED,
                                                                                                WIMMessageHelper.generateMsgParms(securityName, e.toString())));
        }
    }
}
