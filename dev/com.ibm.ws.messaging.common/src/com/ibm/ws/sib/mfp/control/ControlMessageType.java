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
import java.util.function.Function;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.mfp.IntAble;

/**
 * ControlMessageType is a type-safe enumeration which indicates the type of a
 * Control Message.
 */
public enum ControlMessageType implements IntAble {
        /**  Constant denoting an indeterminate DDD Message  */
        UNKNOWN(0, false),
        /**  Constant denoting an Ack Expected Messge */
        ACKEXPECTED(1, false),
        /**  Constant denoting a Silence Message  */
        SILENCE(2, false),
        /**  Constant denoting an Ack Message  */
        ACK(3, false),
        /**  Constant denoting a Nack Message  */
        NACK(4, false),
        /**  Constant denoting a Prevalue Message  */
        PREVALUE(5, false),
        /**  Constant denoting an Accept Message  */
        ACCEPT(6, false),
        /**  Constant denoting a Reject Message  */
        REJECT(7, false),
        /**  Constant denoting a Decision Message  */
        DECISION(8, false),
        /**  Constant denoting a Request Message  */
        REQUEST(9),
        /**  Constant denoting a RequestAck Message  */
        REQUESTACK(10),
        /**  Constant denoting a RequestHighestGeneratedTick Message  */
        REQUESTHIGHESTGENERATEDTICK(11),
        /**  Constant denoting a HighestGeneratedTick Message  */
        HIGHESTGENERATEDTICK(12),
        /**  Constant denoting a RequestRequestAck Message  */
        RESETREQUESTACK(13),
        /**  Constant denoting a RequestRequestAckAck Message  */
        RESETREQUESTACKACK(14),
        /**  Constant denoting a BrowseGet Message  */
        BROWSEGET(15),
        /**  Constant denoting a BrowseEnd Message  */
        BROWSEEND(16),
        /**  Constant denoting a BrowseStatus Message  */
        BROWSESTATUS(17),
        /**  Constant denoting a Completed Message  */
        COMPLETED(18),
        /**  Constant denoting a DecisionExpected Message  */
        DECISIONEXPECTED(19),
        /**  Constant denoting a CreateStream Message  */
        CREATESTREAM(20),
        /**  Constant denoting an AreYouFlushed Message  */
        AREYOUFLUSHED(21),
        /**  Constant denoting a Flushed Message  */
        FLUSHED(22),
        /**  Constant denoting a NotFlushed Message  */
        NOTFLUSHED(23),
        /**  Constant denoting a RequestFlushed Message  */
        REQUESTFLUSH(24),
        /**  Constant denoting a RequestCardinalityInfo Message  */
        REQUESTCARDINALITYINFO(25),
        /**  Constant denoting a CardinalityInfo Message  */
        CARDINALITYINFO(26),
        /** Constant denoting a CreateDurable Message */
        CREATEDURABLE(27),
        /**  Constant denoting a DeleteDurable Message */
        DELETEDURABLE(28),
        /**  Constant denoting a DurableConfirm Message */
        DURABLECONFIRM(29),
        ;

        private static final TraceComponent tc = register(ControlMessageType.class, MSG_GROUP, MSG_BUNDLE);

        private static String format(String name) { return String.format("%-27s", name); }

        private static final Map<Integer,ControlMessageType> types;

        static {
                Map<Integer,ControlMessageType> map = new HashMap<>();
                for (ControlMessageType type: values()) map.put(type.toInt(), type);
                types = unmodifiableMap(map);
        }

        private final Function<String,String> nameFormatter;
        private final Byte   value;
        private final int    intValue;

        private ControlMessageType(int aValue) {
                this(aValue, true);
        }

        private ControlMessageType(int aValue, boolean customFormatted) {
                nameFormatter = customFormatted ? ControlMessageType::format : s -> s;
                value = Byte.valueOf((byte)aValue);
                intValue = aValue;
        }


        /**
         * Returns the corresponding ControlMessageType for a given integer.
         * This method should NOT be called by any code outside the MFP component.
         * It is only public so that it can be accessed by sub-packages.
         *
         * @param  aValue         The integer for which an ControlMessageType is required.
         *
         * @return The corresponding ControlMessageType
         */
        public static final ControlMessageType getControlMessageType(Byte aValue) {
                if (isAnyTracingEnabled() && tc.isDebugEnabled()) debug(tc,"Value = " + aValue);
                return types.get(aValue.intValue());
        }

        public static final ControlMessageType getControlMessageType(int aValue) {
            return types.get(aValue);
        }

        /**
         * Returns the Byte representation of the ControlMessageType.
         * This method should NOT be called by any code outside the MFP component.
         * It is only public so that it can be accessed by sub-packages.
         *
         * @return The Byte representation of the instance.
         */
        public final Byte toByte() {
                return value;
        }

        /**
         * Returns the integer representation of the ControlMessageType.
         * This method should NOT be called by any code outside the MFP component.
         * It is only public so that it can be accessed by sub-packages.
         *
         * @return  The int representation of the instance.
         */
        public final int toInt() {
                return intValue;
        }

        /**
         * Returns the name of the ControlMessageType.
         *
         * @return  The name of the instance.
         */
        @Override
        public final String toString() {
                return nameFormatter.apply(name());
        }
}
