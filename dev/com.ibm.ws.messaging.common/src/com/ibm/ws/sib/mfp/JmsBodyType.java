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
package com.ibm.ws.sib.mfp;

import static com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled;
import static com.ibm.websphere.sib.SIApiConstants.JMS_FORMAT;
import static com.ibm.websphere.sib.SIApiConstants.JMS_FORMAT_BYTES;
import static com.ibm.websphere.sib.SIApiConstants.JMS_FORMAT_MAP;
import static com.ibm.websphere.sib.SIApiConstants.JMS_FORMAT_OBJECT;
import static com.ibm.websphere.sib.SIApiConstants.JMS_FORMAT_STREAM;
import static com.ibm.websphere.sib.SIApiConstants.JMS_FORMAT_TEXT;
import static com.ibm.ws.sib.mfp.MfpConstants.MSG_BUNDLE;
import static com.ibm.ws.sib.mfp.MfpConstants.MSG_GROUP;
import static com.ibm.ws.sib.utils.ras.SibTr.debug;
import static com.ibm.ws.sib.utils.ras.SibTr.entry;
import static com.ibm.ws.sib.utils.ras.SibTr.exit;
import static com.ibm.ws.sib.utils.ras.SibTr.register;
import static java.util.Collections.unmodifiableMap;

import java.util.HashMap;
import java.util.Map;

import com.ibm.websphere.ras.TraceComponent;

/**
 * JmsBodyType is a type-safe enumeration for JmsBody types.
 */
public enum JmsBodyType implements IntAble {
        /** Constant denoting a null-bodied JMS Message                   */
        NULL(0, JMS_FORMAT),
        /** Constant denoting JmsBody Type of Bytes                       */
        BYTES(1, JMS_FORMAT_BYTES),
        /** Constant denoting JmsBody Type of Map                         */
        MAP(2, JMS_FORMAT_MAP),
        /** Constant denoting JmsBody Type of Object                      */
        OBJECT(3, JMS_FORMAT_OBJECT),
        /** Constant denoting JmsBody Type of Stream                      */
        STREAM(4, JMS_FORMAT_STREAM),
        /** Constant denoting JmsBody Type of Text                        */
        TEXT(5, JMS_FORMAT_TEXT),
        ;

        private static final TraceComponent tc = register(JmsBodyType.class, MSG_GROUP, MSG_BUNDLE);

        // TODO remove constants once all dependencies addressed
        public static final int NULL_INT   = 0;
        public static final int BYTES_INT  = 1;
        public static final int MAP_INT    = 2;
        public static final int OBJECT_INT = 3;
        public static final int STREAM_INT = 4;
        public static final int TEXT_INT   = 5;

        private static final Map<Integer,JmsBodyType> types;
        private static final Map<String,JmsBodyType> typesByFormat;

        static {
                Map<Integer, JmsBodyType> map = new HashMap<>();
                Map<String, JmsBodyType> formatMap = new HashMap<>();
                for (JmsBodyType type: values()) {
                        map.put(type.toInt(), type);
                        formatMap.put(type.format, type);
                }
                types = unmodifiableMap(map);
                typesByFormat = unmodifiableMap(formatMap);
        }

        private final String format;
        private final Byte   value;
        private final int    intValue;

        private JmsBodyType(int aValue, String sdoFormat) {
                format = sdoFormat;
                value = Byte.valueOf((byte)aValue);
                intValue = aValue;
        }

        /**
         * Return the appropriate JMSBodyType for a specific format string
         *
         * @param format  A String corresponding to the SDO format of a JMS Message
         *
         * @return JmsBodyType The JmsBodyType singleton which maps to the given format string
         */
        public static JmsBodyType getBodyType(String format) {
                if (isAnyTracingEnabled() && tc.isEntryEnabled()) entry(tc, "getBodyType");

                JmsBodyType result = typesByFormat.get(format);

                if (isAnyTracingEnabled() && tc.isEntryEnabled()) exit(tc, "getBodyType", result);
                return result;
        }

        /**
         * Returns the corresponding JmsBodyType for a given Byte.
         * This method should NOT be called by any code outside the TEXT component.
         * It is only public so that it can be accessed by sub-packages.
         *
         * @param  aValue         The Byte for which an JmsBodyType is required.
         *
         * @return The corresponding JmsBodyType
         */
        public static final JmsBodyType getJmsBodyType(Byte aValue) {
                if (isAnyTracingEnabled() && tc.isDebugEnabled()) debug(tc,"Value = " + aValue);
                return types.get(aValue.intValue());
        }

        /**
         * Returns the corresponding JmsBodyType for a given integer.
         * This method should NOT be called by any code outside the TEXT component.
         * It is only public so that it can be accessed by sub-packages.
         *
         * @param  aValue         The integer for which an JmsBodyType is required.
         *
         * @return The corresponding JmsBodyType
         */
        public static final JmsBodyType getJmsBodyType(int aValue) {
            if (isAnyTracingEnabled() && tc.isDebugEnabled()) debug(tc,"Value = " + aValue);
            return types.get(aValue);
        }

        /**
         * Returns the Byte representation of the JmsBodyType.
         * This method should NOT be called by any code outside the MFP component.
         * It is only public so that it can be accessed by sub-packages.
         *
         * @return The Byte representation of the instance.
         */
        public final Byte toByte() {
                return value;
        }

        /**
         * Returns the integer representation of the JmsBodyType.
         * This method should NOT be called by any code outside the SIBus.
         * It is only public so that it can be accessed by SIBus components.
         *
         * @return The int representation of the instance.
         */
        public final int toInt() {
                return intValue;
        }
}
