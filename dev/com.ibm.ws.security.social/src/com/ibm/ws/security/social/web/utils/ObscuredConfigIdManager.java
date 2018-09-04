/*******************************************************************************
 * Copyright (c) 2017, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.social.web.utils;

import java.util.HashMap;
import java.util.Map;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.social.TraceConstants;

/**
 * Class used for managing mappings from configuration ID values to their obscured values. Obscured values are used in user-facing
 * situations where internal configuration data (such as configuration IDs) either must not or should not be divulged.
 */
public class ObscuredConfigIdManager {

    public static final TraceComponent tc = Tr.register(ObscuredConfigIdManager.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);

    private final Map<String, String> obscuredIdMap = new HashMap<String, String>();
    private final Map<String, String> invertedObscuredIdMap = new HashMap<String, String>();

    public void addId(String configId) {
        // TODO
        if (configId == null) {
            // TODO - what if an ID isn't specified in the config?
            return;
        }
        String obscuredId = getObscuredId(configId);
        synchronized (obscuredIdMap) {
            obscuredIdMap.put(configId, obscuredId);
        }
        synchronized (invertedObscuredIdMap) {
            invertedObscuredIdMap.put(obscuredId, configId);
        }
    }

    public void removeId(String configId) {
        // TODO
        if (configId == null) {
            // TODO - what if an ID isn't specified in the config?
            return;
        }
        String obscuredId = getObscuredId(configId);
        synchronized (obscuredIdMap) {
            obscuredIdMap.remove(configId);
        }
        synchronized (invertedObscuredIdMap) {
            invertedObscuredIdMap.remove(obscuredId);
        }
    }

    public String getObscuredIdFromConfigId(String configId) {
        synchronized (obscuredIdMap) {
            return obscuredIdMap.get(configId);
        }
    }

    public String getConfigIdFromObscuredId(String obscuredConfigId) {
        synchronized (invertedObscuredIdMap) {
            return invertedObscuredIdMap.get(obscuredConfigId);
        }
    }

    String getObscuredId(String configId) {
        // TODO
        return new Integer(configId.hashCode()).toString();
    }

}
