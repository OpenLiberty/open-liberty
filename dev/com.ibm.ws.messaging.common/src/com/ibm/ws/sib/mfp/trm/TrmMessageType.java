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
 * TrmMessageType is a type-safe enumeration which indicates the type of a TRM
 * message.
 */
public enum TrmMessageType implements IntAble {
        /**  Constant denoting an indeterminate TRM Message  */
        UNKNOWN(0),
        /**  Constant denoting a Route data Message */
        ROUTE_DATA(1),
        ;

        private static final TraceComponent tc = register(TrmMessageType.class, MSG_GROUP, MSG_BUNDLE);

        private static final Map<Integer,TrmMessageType> types;

        static {
                Map<Integer,TrmMessageType> map = new HashMap<>();
                for (TrmMessageType type: values()) map.put(type.toInt(), type);
                types = unmodifiableMap(map);
        }

        private final int value;

        private TrmMessageType(int aValue) {
                value = aValue;
        }

        /**
         * Returns the corresponding TrmMessageType for a given integer.
         * This method should NOT be called by any code outside the MFP component.
         * It is only public so that it can be accessed by sub-packages.
         *
         * @param  aValue         The integer for which an TrmMessageType is required.
         *
         * @return The corresponding TrmMessageType
         */
        public static final TrmMessageType getTrmMessageType(int aValue) {
                if (isAnyTracingEnabled() && tc.isDebugEnabled()) debug(tc,"Value = " + aValue);
                return types.get(aValue);
        }

        /**
         * Returns the integer representation of the TrmMessageType.
         * This method should NOT be called by any code outside the MFP component.
         * It is only public so that it can be accessed by sub-packages.
         *
         * @return  The int representation of the instance.
         */
        public final int toInt() {
                return value;
        }
}
