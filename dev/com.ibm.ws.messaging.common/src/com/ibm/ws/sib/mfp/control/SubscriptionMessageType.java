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
package com.ibm.ws.sib.mfp.control;

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
 * SubscriptionMessageType is a type-safe enumeration which indicates the type of a
 * Subscription Propagation message.
 */
public enum SubscriptionMessageType implements IntAble {
        /** Constant denoting an indeterminate Subscription Propagation Message  */
        UNKNOWN(0),
        /** Constant denoting a Reset Message  */
        RESET(1),
        /** Constant denoting a Create Message  */
        CREATE(2),
        /** Constant denoting a Delete Message  */
        DELETE(3),
        /** Constant denoting a Request Message  */
        REQUEST(4),
        /** Constant denoting a Reply Message  */
        REPLY(5),
        ;

        private static final TraceComponent tc = register(SubscriptionMessageType.class, MSG_GROUP, MSG_BUNDLE);

        private static final Map<Integer,SubscriptionMessageType> types;

        static {
                Map<Integer,SubscriptionMessageType> map = new HashMap<>();
                for (SubscriptionMessageType type: values()) map.put(type.toInt(), type);
                types = unmodifiableMap(map);
        }

        private final int value;

        private SubscriptionMessageType(int aValue) {
                value = aValue;
        }

        /**
         * Returns the corresponding SubscriptionMessageType for a given integer.
         * This method should NOT be called by any code outside the MFP component.
         * It is only public so that it can be accessed by sub-packages.
         *
         * @param  aValue         The integer for which an SubscriptionMessageType is required.
         *
         * @return The corresponding SubscriptionMessageType
         */
        public static final SubscriptionMessageType getSubscriptionMessageType(int aValue) {
                if (isAnyTracingEnabled() && tc.isDebugEnabled()) debug(tc,"Value = " + aValue);
                return types.get(aValue);
        }

        /**
         * Returns the integer representation of the SubscriptionMessageType.
         * This method should NOT be called by any code outside the MFP component.
         * It is only public so that it can be accessed by sub-packages.
         *
         * @return  The int representation of the instance.
         */
        public final int toInt() {
                return value;
        }
}
