/*******************************************************************************
 * Copyright (c) 1997, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.pmi.server;

import com.ibm.websphere.ras.*;
import com.ibm.websphere.pmi.stat.StatConstants;

/**
 * #author Srini Rangaswamy
 * 
 */
public class PMIServiceState {
    private static final long serialVersionUID = 3216072052838075672L;
    private boolean bSynchronizedUpdate = false;
    private String sStatisticSet = "";
    private String sInstrumentationSpec = "";

    private static TraceComponent tc = Tr.register(PMIServiceState.class);

    // Yes, public integer statistic ID. This integer is modified in this CLASS only
    // and USED in component modules to return from the method if level is none    
    public static int iStatisticSet = 0;

    // internal constants. not exposed to public
    public static final int STATISTIC_SETID_INVALID = -1;
    public static final int STATISTIC_SETID_NONE = 0;
    public static final int STATISTIC_SETID_BASIC = 1;
    public static final int STATISTIC_SETID_EXTENDED = 2;
    public static final int STATISTIC_SETID_ALL = 3;
    public static final int STATISTIC_SETID_CUSTOM = 4;

    public PMIServiceState() {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "<init>");

        bSynchronizedUpdate = false;
        sStatisticSet = "";
        sInstrumentationSpec = "";

        if (tc.isEntryEnabled())
            Tr.exit(tc, "<init>");

    }

    public void update(PMIServiceState s) {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "Updating state with new state object.");

        this.bSynchronizedUpdate = s.bSynchronizedUpdate;
        this.sStatisticSet = s.sStatisticSet;
        this.sInstrumentationSpec = s.sInstrumentationSpec;
    }

    public boolean getSynchronizedUpdate() {
        return bSynchronizedUpdate;
    }

    public String getStatisticSet() {
        return sStatisticSet;
    }

    public String getInstrumentationSpec() {
        return sInstrumentationSpec;
    }

    public void setSynchronizedUpdate(boolean b) {
        bSynchronizedUpdate = b;
    }

    public void setStatisticSet(String set) {
        sStatisticSet = set;

        // Set the integer statistic ID. This integer is modified ONLY
        // in this method and USED in component modules to return from the method
        // if level is none
        if (sStatisticSet.equals(StatConstants.STATISTIC_SET_NONE))
            iStatisticSet = STATISTIC_SETID_NONE;
        else if (sStatisticSet.equals(StatConstants.STATISTIC_SET_BASIC))
            iStatisticSet = STATISTIC_SETID_BASIC;
        else if (sStatisticSet.equals(StatConstants.STATISTIC_SET_EXTENDED))
            iStatisticSet = STATISTIC_SETID_EXTENDED;
        else if (sStatisticSet.equals(StatConstants.STATISTIC_SET_ALL))
            iStatisticSet = STATISTIC_SETID_ALL;
        else if (sStatisticSet.equals(StatConstants.STATISTIC_SET_CUSTOM))
            iStatisticSet = STATISTIC_SETID_CUSTOM;
        else
            iStatisticSet = STATISTIC_SETID_INVALID;
    }

    public void setInstrumentationSpec(String spec) {
        sInstrumentationSpec = spec;
    }

    public synchronized String getStateObjectInfo() {
        StringBuffer sb = new StringBuffer(Integer.toString(this.hashCode()));

        sb.append(" = statisticSet ==|").append(sStatisticSet).append("|==");
        sb.append(" = synchronizedUpdate ==|").append(bSynchronizedUpdate).append("|==");
        sb.append(" = instrumentationSpec ==|").append(sInstrumentationSpec).append("|==");

        return sb.toString();
    }
}
