/* =============================================================================
 * Copyright (c) 2012,2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 * =============================================================================
 */

package com.ibm.ws.sib.mfp.trm;

import static com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled;
import static com.ibm.ws.sib.mfp.MfpConstants.MSG_BUNDLE;
import static com.ibm.ws.sib.mfp.MfpConstants.MSG_GROUP;
import static com.ibm.ws.sib.utils.ras.SibTr.debug;
import static com.ibm.ws.sib.utils.ras.SibTr.register;
import static java.util.Collections.unmodifiableMap;

import java.util.HashMap;
import java.util.Map;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.mfp.IntAble;

/**
 * TrmFirstContactMessageType is a type-safe enumeration which indicates the
 * type of a TRM First Contact message.
 */
public enum TrmFirstContactMessageType implements IntAble {
        /**  Constant denoting a Client Bootstrap Request  */
        CLIENT_BOOTSTRAP_REQUEST(0),
        /**  Constant denoting a Client Bootstrap Reply  */
        CLIENT_BOOTSTRAP_REPLY(1),
        /**  Constant denoting a Client Attach Request  */
        CLIENT_ATTACH_REQUEST(2),
        /**  Constant denoting a Client Attach Request 2 */
        CLIENT_ATTACH_REQUEST2(3),
        /**  Constant denoting a Client Attach Reply  */
        CLIENT_ATTACH_REPLY(4),
        /**  Constant denoting a ME Bootstrap Request  */
        ME_CONNECT_REQUEST(5),
        /**  Constant denoting a ME Bootstrap Reply  */
        ME_CONNECT_REPLY(6),
        /**  Constant denoting a ME Link Request  */
        ME_LINK_REQUEST(7),
        /**  Constant denoting a ME Link Reply  */
        ME_LINK_REPLY(8),
        /**  Constant denoting a ME Bridge Request  */
        ME_BRIDGE_REQUEST(9),
        /**  Constant denoting a ME Bridge Reply  */
        ME_BRIDGE_REPLY(10),
        /**  Constant denoting a ME Bridge Bootstrap Request */
        ME_BRIDGE_BOOTSTRAP_REQUEST(11),
        /**  Constant denoting a ME Bridge Bootstrap Reply */
        ME_BRIDGE_BOOTSTRAP_REPLY(12),
        ;

        private static final TraceComponent tc = register(TrmFirstContactMessageType.class, MSG_GROUP, MSG_BUNDLE);

        private static final Map<Integer,TrmFirstContactMessageType> types;

        static {
                Map<Integer,TrmFirstContactMessageType> map = new HashMap<>();
                for (TrmFirstContactMessageType type: values()) map.put(type.toInt(), type);
                types = unmodifiableMap(map);
        }

        private final int value;

        private TrmFirstContactMessageType(int aValue) {
                value = aValue;
        }

        /**
         * Returns the corresponding TrmFirstContactMessageType for a given integer.
         * This method should NOT be called by any code outside the MFP component.
         * It is only public so that it can be accessed by sub-packages.
         *
         * @param  aValue         The integer for which an TrmFirstContactMessageType is required.
         *
         * @return The corresponding TrmFirstContactMessageType
         */
        public static final TrmFirstContactMessageType getTrmFirstContactMessageType(int aValue) {
                if (isAnyTracingEnabled() && tc.isDebugEnabled()) debug(tc,"Value = " + aValue);
                return types.get(aValue);
        }

        /**
         * Returns the integer representation of the TrmFirstContactMessageType.
         * This method should NOT be called by any code outside the MFP component.
         * It is only public so that it can be accessed by sub-packages.
         *
         * @return  The int representation of the instance.
         */
        public final int toInt() {
                return value;
        }
}
