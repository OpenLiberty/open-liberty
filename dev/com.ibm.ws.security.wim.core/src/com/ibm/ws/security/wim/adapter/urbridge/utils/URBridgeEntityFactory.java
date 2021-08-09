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

import java.util.Map;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.security.wim.Service;
import com.ibm.websphere.security.wim.ras.WIMMessageHelper;
import com.ibm.websphere.security.wim.ras.WIMMessageKey;
import com.ibm.ws.security.wim.adapter.urbridge.URBridge;
import com.ibm.wsspi.security.wim.exception.WIMApplicationException;
import com.ibm.wsspi.security.wim.exception.WIMException;
import com.ibm.wsspi.security.wim.model.Entity;

public class URBridgeEntityFactory {

    /**
     * Register the class to trace service.
     */
    private final static TraceComponent tc = Tr.register(URBridgeEntityFactory.class);

    /**
     * Create a factory for making URBridgeEntityObjects.
     *
     */
    public URBridgeEntityFactory() {
        super();
    }

    /**
     * Create a URBridgeEntity from input parameters. With inheritance this
     * allows Users and Groups to be treated identically.
     *
     * @param entity The entity to be wrapped by the URBridgeEntity. This
     *            entity's type determines which type of OSEntityObject
     *            is created.
     * @param urBridge The UserRegisty associated with the adapter.
     * @param attrMap An attribute map containing the configuration information
     *            on how underlying UR attributes map to WIM attributes.
     * @param baseEntryName The name of the baseEntry
     * @return a URBridgeEntity that can be used to get and set properties
     *         of the entity.
     * @throws WIMException The entity type passed in of not of type 'Person'
     *             or of type 'Group'.
     */
    public URBridgeEntity createObject(Entity entity, URBridge urBridge,
                                       Map<String, String> attrMap, String baseEntryName, Map<String, String> entityConfigMap) throws WIMException {
        String entityType = entity.getTypeName();
        URBridgeEntity obj = null;

        if (Service.DO_GROUP.equals(entityType)
            || Entity.getSubEntityTypes(Service.DO_GROUP).contains(entityType)) {
            obj = new URBridgeGroup(entity, urBridge, attrMap, baseEntryName, entityConfigMap);
        } else if (Service.DO_LOGIN_ACCOUNT.equals(entityType)
                   || Entity.getSubEntityTypes(Service.DO_LOGIN_ACCOUNT).contains(entityType)) {
            obj = new URBridgePerson(entity, urBridge, attrMap, baseEntryName, entityConfigMap);
        } else {
            throw new WIMApplicationException(WIMMessageKey.ENTITY_TYPE_NOT_SUPPORTED, Tr.formatMessage(tc, WIMMessageKey.ENTITY_TYPE_NOT_SUPPORTED,
                                                                                                        WIMMessageHelper.generateMsgParms(entityType)));
        }
        return obj;
    }
}
