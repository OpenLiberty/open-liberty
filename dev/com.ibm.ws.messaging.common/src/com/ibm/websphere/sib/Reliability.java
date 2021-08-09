/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.sib;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.utils.TraceGroups;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * Reliability is a type-safe enumeration for Reliability quality of service values.
 * 
 */
public final class Reliability implements java.lang.Comparable {

    private static TraceComponent tc = SibTr.register(Reliability.class, TraceGroups.TRGRP_MFPAPI, "com.ibm.websphere.sib.CWSIEMessages");

    private final static String NONE_STRING = "None";

    /****************************************************************************/
    /* Internal use only Public constants */
    /****************************************************************************/

    /* String representations of Reliability values - for Property matching */
    /**
     * Constant naming DeliveryMode Type - Best Effort Non-persistent.
     * This constant should NOT be accessed by any code outside the SIBus.
     * It is only public so that it can be accessed by other SIBus components.
     */
    public final static String BEST_EFFORT_NONPERSISTENT_STRING = "BestEffortNonPersistent";
    /**
     * Constant naming DeliveryMode Type - Express Non-persistent.
     * This constant should NOT be accessed by any code outside the SIBus.
     * It is only public so that it can be accessed by other SIBus components.
     */
    public final static String EXPRESS_NONPERSISTENT_STRING = "ExpressNonPersistent";
    /**
     * Constant naming DeliveryMode Type - Reliable Non-persistent.
     * This constant should NOT be accessed by any code outside the SIBus.
     * It is only public so that it can be accessed by other SIBus components.
     */
    public final static String RELIABLE_NONPERSISTENT_STRING = "ReliableNonPersistent";
    /**
     * Constant naming DeliveryMode Type - Reliable Persistent.
     * This constant should NOT be accessed by any code outside the SIBus.
     * It is only public so that it can be accessed by other SIBus components.
     */
    public final static String RELIABLE_PERSISTENT_STRING = "ReliablePersistent";
    /**
     * Constant naming DeliveryMode Type - Assured Persistent.
     * This constant should NOT be accessed by any code outside the SIBus.
     * It is only public so that it can be accessed by other SIBus components.
     */
    public final static String ASSURED_PERSISTENT_STRING = "AssuredPersistent";

    /****************************************************************************/
    /* Public constants */
    /****************************************************************************/

    /**
     * Constant denoting a currently unknown or no persistence type
     * 
     * @ibm-was-base
     * @ibm-api
     */
    public final static Reliability NONE = new Reliability(NONE_STRING, (byte) 0, -1, (byte) 0);

    /**
     * Constant denoting DeliveryMode Type - Best Effort Non-persistent
     * 
     * @ibm-was-base
     * @ibm-api
     */
    public final static Reliability BEST_EFFORT_NONPERSISTENT = new Reliability(BEST_EFFORT_NONPERSISTENT_STRING, (byte) 5, 0, (byte) 1);

    /**
     * Constant denoting DeliveryMode Type - Express Non-persistent
     * 
     * @ibm-was-base
     * @ibm-api
     */
    public final static Reliability EXPRESS_NONPERSISTENT = new Reliability(EXPRESS_NONPERSISTENT_STRING, (byte) 15, 1, (byte) 1);

    /**
     * Constant denoting DeliveryMode Type - Reliable Non-persistent
     * 
     * @ibm-was-base
     * @ibm-api
     */
    public final static Reliability RELIABLE_NONPERSISTENT = new Reliability(RELIABLE_NONPERSISTENT_STRING, (byte) 25, 2, (byte) 1);

    /**
     * Constant denoting DeliveryMode Type - Reliable Persistent
     * 
     * @ibm-was-base
     * @ibm-api
     */
    public final static Reliability RELIABLE_PERSISTENT = new Reliability(RELIABLE_PERSISTENT_STRING, (byte) 35, 3, (byte) 2);

    /**
     * Constant denoting DeliveryMode Type - Assured Persistent
     * 
     * @ibm-was-base
     * @ibm-api
     */
    public final static Reliability ASSURED_PERSISTENT = new Reliability(ASSURED_PERSISTENT_STRING, (byte) 45, 4, (byte) 2);

    /* Array of defined Reliabilitys - needed by getReliability */
    /* We provide plenty of room for expansion by mapping each Reliability to a */
    /* range of values. Hence if someone later adds RELIABLE_RATHER_PERSISTENT, */
    /* say, they can give it value 30 to have an older ME treat it as */
    /* RELIABLE_NONPERSISTENT, or 31 to have it treated as RELIABLE_PERSISTENT. */
    private final static Reliability[] set = { NONE
                                              , BEST_EFFORT_NONPERSISTENT
                                              , BEST_EFFORT_NONPERSISTENT
                                              , BEST_EFFORT_NONPERSISTENT
                                              , BEST_EFFORT_NONPERSISTENT
                                              , BEST_EFFORT_NONPERSISTENT
                                              , BEST_EFFORT_NONPERSISTENT
                                              , BEST_EFFORT_NONPERSISTENT
                                              , BEST_EFFORT_NONPERSISTENT
                                              , BEST_EFFORT_NONPERSISTENT
                                              , BEST_EFFORT_NONPERSISTENT
                                              , EXPRESS_NONPERSISTENT
                                              , EXPRESS_NONPERSISTENT
                                              , EXPRESS_NONPERSISTENT
                                              , EXPRESS_NONPERSISTENT
                                              , EXPRESS_NONPERSISTENT
                                              , EXPRESS_NONPERSISTENT
                                              , EXPRESS_NONPERSISTENT
                                              , EXPRESS_NONPERSISTENT
                                              , EXPRESS_NONPERSISTENT
                                              , EXPRESS_NONPERSISTENT
                                              , RELIABLE_NONPERSISTENT
                                              , RELIABLE_NONPERSISTENT
                                              , RELIABLE_NONPERSISTENT
                                              , RELIABLE_NONPERSISTENT
                                              , RELIABLE_NONPERSISTENT
                                              , RELIABLE_NONPERSISTENT
                                              , RELIABLE_NONPERSISTENT
                                              , RELIABLE_NONPERSISTENT
                                              , RELIABLE_NONPERSISTENT
                                              , RELIABLE_NONPERSISTENT
                                              , RELIABLE_PERSISTENT
                                              , RELIABLE_PERSISTENT
                                              , RELIABLE_PERSISTENT
                                              , RELIABLE_PERSISTENT
                                              , RELIABLE_PERSISTENT
                                              , RELIABLE_PERSISTENT
                                              , RELIABLE_PERSISTENT
                                              , RELIABLE_PERSISTENT
                                              , RELIABLE_PERSISTENT
                                              , RELIABLE_PERSISTENT
                                              , ASSURED_PERSISTENT
                                              , ASSURED_PERSISTENT
                                              , ASSURED_PERSISTENT
                                              , ASSURED_PERSISTENT
                                              , ASSURED_PERSISTENT
                                              , ASSURED_PERSISTENT
                                              , ASSURED_PERSISTENT
                                              , ASSURED_PERSISTENT
                                              , ASSURED_PERSISTENT
                                              , ASSURED_PERSISTENT
                                           };

    /* Compact array of only the existing Reliability values - the index value */
    /* is used only be MP and does not get flowed between MEs. Hence there is */
    /* no need to provide space for upward compatibility. */
    private final static Reliability[] indexSet = { NONE
                                                   , BEST_EFFORT_NONPERSISTENT
                                                   , EXPRESS_NONPERSISTENT
                                                   , RELIABLE_NONPERSISTENT
                                                   , RELIABLE_PERSISTENT
                                                   , ASSURED_PERSISTENT
                                                };

    /* Compact array of Names for only the existing Reliability values - it is */
    /* used only internally and the index does not get flowed between MEs. */
    /* Hence there is no need to provide space for upward compatibility. */
    /* However, the index to this array MUST match that of indexSet. */
    private final static String[] nameSet = { NONE_STRING
                                             , BEST_EFFORT_NONPERSISTENT_STRING
                                             , EXPRESS_NONPERSISTENT_STRING
                                             , RELIABLE_NONPERSISTENT_STRING
                                             , RELIABLE_PERSISTENT_STRING
                                             , ASSURED_PERSISTENT_STRING
                                          };

    /* Maximum index - required by MP */
    /**
     * Constant denoting the maximum index.
     * This constant should NOT be accessed by any code outside the SIBus.
     * It is only public so that it can be accessed by other SIBus components.
     */
    public final static int MAX_INDEX = ASSURED_PERSISTENT.getIndex();

    private final String name;
    private final Byte value;
    private final int intValue;
    private final int index;
    private final Byte persistence;

    /* Private constructor - ensures the 'constants' defined here are the total set. */
    private Reliability(String aName, byte aValue, int aIndex, byte aPersistence) {
        name = aName;
        value = Byte.valueOf(aValue);
        intValue = aValue;
        index = aIndex;
        persistence = Byte.valueOf(aPersistence);
    }

    /**
     * Returns the name of the Reliability.
     * 
     * @return The name of the Reliability instance.
     * 
     * @ibm-was-base
     * @ibm-api
     */
    @Override
    public final String toString() {
        return name;
    }

    /**
     * Compare this Reliability with another.
     * <p>
     * The method implements java.util.Comaparable.compareTo and therefore
     * has the same semantics.
     * 
     * @param other The Reliability this is to be compared with.
     * 
     * @return An int indicating the relative values as follows:
     *         <br> >0: this > other (i.e. more Reliable).
     *         <br> 0: this == other
     *         <br> <0: this < other (i.e. less Reliable).
     * 
     * @ibm-was-base
     * @ibm-api
     */
    public final int compareTo(Object other) {

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.info(tc, "this: " + this + ", other: " + other.toString() + ", result: " + (this.intValue - ((Reliability) other).intValue));

        return (this.intValue - ((Reliability) other).intValue);

    }

    /****************************************************************************/
    /* Internal use only Public methods */
    /****************************************************************************/

    /**
     * Returns the corresponding Reliability for a given integer.
     * This method should NOT be called by any code outside the SIBus.
     * It is only public so that it can be accessed by other SIBus components.
     * 
     * @param aValue The integer for which an Reliability is required.
     * 
     * @return The corresponding Reliability
     */
    public final static Reliability getReliability(int aValue) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.info(tc, "Value = " + aValue);
        return set[aValue];
    }

    /**
     * Returns the integer representation of the Reliability.
     * This method should NOT be called by any code outside the MFP component.
     * It is only public so that it can be accessed by sub-packages.
     * 
     * @return The int representation of the Reliability instance.
     */
    public final int toInt() {
        return intValue;
    }

    /**
     * Returns the corresponding Reliability for a given name.
     * This method should NOT be called by any code outside the SIBus.
     * It is only public so that it can be accessed by other SIBus components.
     * 
     * @param name The toString value of a Reliability constant.
     * 
     * @return The corresponding Reliability
     */
    public final static Reliability getReliabilityByName(String name)
                         throws NullPointerException, IllegalArgumentException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.info(tc, "Name = " + name);

        if (name == null) {
            throw new NullPointerException();
        }

        /* Look for the name in the nameSet, and return the corresponding */
        /* Reliability from the indexSet. */
        for (int i = 0; i <= MAX_INDEX + 1; i++) {
            if (name.equals(nameSet[i])) {
                return indexSet[i];
            }
        }

        /* If the name didn't match, throw IllegalArgumentException */
        throw new IllegalArgumentException();

    }

    /**
     * Returns the corresponding Reliability for a given index.
     * This method should NOT be called by any code outside the SIBus.
     * It is only public so that it can be accessed by other SIBus components.
     * 
     * @param mpIndex The MP index for which an Reliability is required. MP use
     *            -1 for NONE and start at 0 for real values.
     * 
     * @return The corresponding Reliability
     */
    public final static Reliability getReliabilityByIndex(int mpIndex) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.info(tc, "Index = " + mpIndex);
        return indexSet[mpIndex + 1];
    }

    /**
     * Returns the index of the Reliability.
     * This method should NOT be called by any code outside the SIBus.
     * It is only public so that it can be accessed by other SIBus components.
     * 
     * @return The index of the Reliability instance.
     */
    public final int getIndex() {
        return index;
    }

    /**
     * Returns the corresponding Reliability for a given Byte.
     * This method should NOT be called by any code outside the MFP component.
     * It is only public so that it can be accessed by sub-packages.
     * 
     * @param aValue The Byte for which an Reliability is required.
     * 
     * @return The corresponding Reliability
     */
    public final static Reliability getReliability(Byte aValue) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.info(tc, "Value = " + aValue);
        return set[aValue.intValue()];
    }

    /**
     * Returns the Byte representation of the Reliability.
     * This method should NOT be called by any code outside the MFP component.
     * It is only public so that it can be accessed by sub-packages.
     * 
     * @return The Byte representation of the Reliability instance.
     */
    public final Byte toByte() {
        return value;
    }

    /**
     * Returns the JMS Persistence value for the Reliability.
     * This method should NOT be called by any code outside the MFP component.
     * It is only public so that it can be accessed by sub-packages.
     * 
     * @return The persistence Byte from the Reliability instance.
     */
    public final Byte getPersistence() {
        return persistence;
    }

}
