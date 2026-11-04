/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.zos.channel.wola.internal.otma;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.ibm.ejs.util.ByteArray;
import com.ibm.ws.zos.channel.wola.internal.natv.WOLANativeUtils;

/* @645942A*/
/* @645942A*/

public final class OLAIMSOTMAKeyMap /* @645942C */
{
    /**
     * Hash map to store anchors
     */
    private final HashMap<OLAIMSOTMAKeyMap.Key, ByteArray> _anchorMap = new HashMap<OLAIMSOTMAKeyMap.Key, ByteArray>(); /* @645942A */

    /**
     * Native methods
     */
    private final WOLANativeUtils _utils;

    /**
     * Constructor
     */
    public OLAIMSOTMAKeyMap(WOLANativeUtils utils) {
        _utils = utils;
    }

    /**
     * Get an OTMA anchor token for a target IMS system
     *
     * @throws OTMAException
     */
    public final synchronized byte[] getOTMAAnchorKey(OLAIMSOTMAKeyMap.Key keyData) throws OTMAException {
        byte[] keyDataBytes = null;

        if (keyData != null) {
            /*-------------------------------------------------------------------*/
            /* Check to see if the key was saved and return the value if so. */
            /*-------------------------------------------------------------------*/
            if (_anchorMap.containsKey(keyData)) /* @645942C */
            {
                ByteArray objectKey = _anchorMap.get(keyData); /* @645942C */
                keyDataBytes = objectKey.getBytes();
            } else {
                /*-----------------------------------------------------------*/
                /* Go get an anchor. */
                /*---------------------------------------------------@645942A*/
                byte[] anchor = _utils.openOTMAConenction(keyData.getXcfGroupName(), keyData.getXcfClientName(), keyData.getXcfServerName());
                _anchorMap.put(keyData, new ByteArray(anchor));
                keyDataBytes = anchor;
            }
        }

        return keyDataBytes;
    }

    /**
     * Clears the OTMA anchor for a target IMS system
     */
    public final synchronized void clearOTMAAnchorKey(OLAIMSOTMAKeyMap.Key keyData,
                                                      byte[] anchorBytes) {
        ByteArray badAnchor = new ByteArray(anchorBytes);

        ByteArray currentAnchor = _anchorMap.get(keyData);

        if ((currentAnchor != null) && (currentAnchor.equals(badAnchor))) {
            _anchorMap.remove(keyData);
            _utils.closeOtmaConnection(currentAnchor.getBytes());
        }
    }

    /**
     * Destroy the map by clearing all existing anchor entries.
     */
    public synchronized void destroy() {
        ArrayList<Map.Entry<Key, ByteArray>> list = new ArrayList<Map.Entry<Key, ByteArray>>(_anchorMap.entrySet());

        for (Map.Entry<Key, ByteArray> e : list) {
            clearOTMAAnchorKey(e.getKey(), e.getValue().getBytes());
        }
    }

    /**
     * Internal class representing the key for the hash map
     */
    static public class Key /* @645942A */
    {
        private String _xcfGroupName = null;
        private String _xcfServerName = null;
        private String _xcfClientName = null;

        public Key(String xcfGroupName, String xcfServerName, String xcfClientName) {
            _xcfGroupName = xcfGroupName.trim();
            _xcfServerName = xcfServerName.trim();
            _xcfClientName = xcfClientName.trim();
        }

        public String getXcfGroupName() {
            return _xcfGroupName;
        }

        public String getXcfServerName() {
            return _xcfServerName;
        }

        public String getXcfClientName() {
            return _xcfClientName;
        }

        @Override
        public int hashCode() {
            return _xcfGroupName.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            boolean equals = false;

            if ((o != null) && (o instanceof Key)) {
                Key k = (Key) o;
                equals = ((this._xcfGroupName.equals(k._xcfGroupName)) &&
                          (this._xcfServerName.equals(k._xcfServerName)) &&
                          (this._xcfClientName.equals(k._xcfClientName)));
            }

            return equals;
        }

        @Override
        public String toString() {
            StringBuffer sb = new StringBuffer();
            sb.append(_xcfGroupName);
            sb.append(":");
            sb.append(_xcfServerName);
            sb.append(":");
            sb.append(_xcfClientName);
            return sb.toString();
        }
    }
}