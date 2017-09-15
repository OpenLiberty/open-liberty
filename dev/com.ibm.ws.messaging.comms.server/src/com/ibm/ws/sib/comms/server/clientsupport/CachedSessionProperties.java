/*******************************************************************************
 * Copyright (c) 2004, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.comms.server.clientsupport;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.Reliability;
import com.ibm.websphere.sib.SIDestinationAddress;
import com.ibm.ws.sib.comms.CommsConstants;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.SelectionCriteria;

/**
 * This class is used when we cache the properties of a consumer session.
 * This happens when a client does a SICoreConnection.receive(wait / noWait).
 * At this point we will create a consumer and do the recieve but we will not
 * destroy the consumer in case they do it again.
 * <p>
 * The important method here is the equals method as we need to work out whether
 * we can re-use the cached consumer session, or we need to close it and create
 * another one.
 * 
 * @author Gareth Matthews
 */
public class CachedSessionProperties {
    /** Trace */
    private static TraceComponent tc = SibTr.register(CachedSessionProperties.class,
                                                      CommsConstants.MSG_GROUP,
                                                      CommsConstants.MSG_BUNDLE);
    /** Log class info on static load */
    static {
        if (tc.isDebugEnabled())
            SibTr.debug(tc, "@(#)SIB/ws/code/sib.comms.server.impl/src/com/ibm/ws/sib/comms/server/clientsupport/CachedSessionProperties.java, SIB.comms, WASX.SIB, aa1225.01 1.10");
    }

    // Session properties
    private SIDestinationAddress destinationAddress = null; // f192759.2
    private SelectionCriteria criteria = null; // F207007.2
    private Reliability unrecovReliability = null;

    /**
     * Constructor
     * 
     * @param destAddr
     * @param sc
     * @param rel
     */
    public CachedSessionProperties(SIDestinationAddress destAddr, // f192759.2
                                   SelectionCriteria sc, // F207007.2
                                   Reliability rel) {
        if (tc.isEntryEnabled())
            SibTr.entry(tc, "<init>");

        destinationAddress = destAddr; // f192759.2
        criteria = sc; // F207007.2
        unrecovReliability = rel;

        if (tc.isEntryEnabled())
            SibTr.exit(tc, "<init>");
    }

    /**
     * Determines whether this object has the exact same properties as
     * another CachedSessionProperties object.
     * 
     * @param other
     * @return Returns true if the cached session properties are equal
     */
    public boolean equals(CachedSessionProperties other) {
        if (tc.isEntryEnabled())
            SibTr.entry(tc, "equals");
        if (tc.isDebugEnabled())
            SibTr.debug(tc, "Params: other", other);

        boolean result = (areTheyTheSame(destinationAddress, other.getDestinationAddress())) && // f192759.2
                         (areTheyTheSame(criteria, other.getSelectionCriteria())) && // F207007.2
                         (areTheyTheSame(unrecovReliability, other.getReliability()));

        if (tc.isDebugEnabled())
            SibTr.debug(tc, "rc=", "" + result);
        if (tc.isEntryEnabled())
            SibTr.exit(tc, "equals");
        return result;
    }

    /**
     * Method which compares 2 objects and determines if they are the same.
     * Objects are the same if they are both null, or if they have equal
     * values.
     * <p>
     * Note this method should only be used for comparing String objects,
     * Reliability objects and SIDestinationAddress objects (and shouldn't
     * be used for comparing a String to a Reliability either - smart guy).
     * 
     * @param obj1
     * @param obj2
     * 
     * @return Returns true if the objects are equal.
     */
    private boolean areTheyTheSame(Object obj1, Object obj2) {
        // If they are both null, then they are equal
        if (obj1 == null && obj2 == null)
            return true;
        // If only one is null then they are not equal
        if (obj1 == null || obj2 == null)
            return false;

        // At this point neither are null - so check them

        // String
        if (obj1 instanceof String) {
            return ((String) obj1).equals(obj2);
        }

        // Reliability
        if (obj1 instanceof Reliability) {
            return ((Reliability) obj1).compareTo(obj2) == 0;
        }

        // Start f192759.2
        // SIDestinationAddress
        if (obj1 instanceof SIDestinationAddress) {
            return obj1.toString().equals(obj2.toString());
        }
        // End f192759.2

        // Otherwise, I have no idea - so return false
        return false;
    }

    /**
     * Returns us a nice String about what we are caching.
     * 
     * @return String
     */
    @Override
    public String toString() {
        return "Destination Addr: '" + destinationAddress + // f192759.2
               "', SelectionCriteria: '" + criteria + // F207007.2 
               "' , Reliability: " + unrecovReliability;
    }

    // Getters

    public SIDestinationAddress getDestinationAddress() {
        return destinationAddress;
    } // f192759.2

    public SelectionCriteria getSelectionCriteria() {
        return criteria;
    } // F207007.2

    public Reliability getReliability() {
        return unrecovReliability;
    }
}
