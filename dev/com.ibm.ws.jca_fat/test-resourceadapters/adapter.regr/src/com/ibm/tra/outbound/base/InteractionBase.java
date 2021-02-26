/*******************************************************************************
 * Copyright (c) 2003, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.tra.outbound.base;

import java.util.Properties;

import javax.resource.ResourceException;
import javax.resource.cci.Connection;
import javax.resource.cci.InteractionSpec;
import javax.resource.cci.Record;
import javax.resource.cci.ResourceWarning;

import com.ibm.ws.j2c.InteractionMetrics;

class InteractionBase implements javax.resource.cci.Interaction {
    ResourceWarning resWarn = null;
    ConnectionBase connection = null;
    InteractionMetrics listener = null;

    static String[] execute2args = new String[] { "fvt.cciadapter.CCIInteractionImpl", "execute(InteractionSpec, Record)" };
    static String[] execute3args = new String[] { "fvt.cciadapter.CCIInteractionImpl", "execute(InteractionSpec, Record, Record)" };

    @Override
    public void clearWarnings() throws ResourceException {
        resWarn = null;

    }

    @Override
    public void close() throws ResourceException {
        resWarn = null;
        connection = null;
    }

    @Override
    public Record execute(InteractionSpec ispec, Record input) throws ResourceException {

        InteractionSpecBase inSpec = null;

        boolean enabled = listener.isInteractionMetricsEnabled();
        System.out.println("InteractionImpl: execute(InteractionSpec, Record), InteractionMetrics enabled=" + enabled);
        Object ctx = null;
        @SuppressWarnings("unused")
        byte[] armCorBytes = null;
        if (enabled) {
            ctx = listener.preInteraction(execute2args);
            armCorBytes = listener.getCorrelator();
        }

        // Note: the armCorBytes should be passed to the downstream EIS with the request in real RA

        try {
            inSpec = (InteractionSpecBase) ispec;
        } catch (ClassCastException cce) {
            if (enabled) {
                postInteraction(ctx, InteractionMetrics.RM_ARM_FAILED, inSpec);
            }
            //InteractionMetrics.reqStop(null, InteractionMetrics.JDBC_COMPONENT_ID, InteractionMetrics.OUTBOUND, InteractionMetrics.RM_ARM_FAILED, null);
            throw new ResourceException(cce);
        }
        RecordBase resultRecord = null;
        if (inSpec.getInteractionVerb().equals(InteractionSpecBase.EXECUTE_GOOD)) {
            resultRecord = new RecordBase();
        } else if (inSpec.getInteractionVerb().equals(InteractionSpecBase.EXECUTE_BAD)) {
            if (enabled) {
                postInteraction(ctx, InteractionMetrics.RM_ARM_FAILED, inSpec);
            }
            throw new ResourceException("Interaction verb is set to EXECUTE_BAD");
        } else {
            if (enabled) {
                postInteraction(ctx, InteractionMetrics.RM_ARM_FAILED, inSpec);
            }
            throw new ResourceException("No Interaction verb is set");
        }

        if (enabled) {
            postInteraction(ctx, InteractionMetrics.RM_ARM_GOOD, inSpec);
        }
        //InteractionMetrics.reqStop(null, InteractionMetrics.JDBC_COMPONENT_ID, InteractionMetrics.OUTBOUND, InteractionMetrics.RM_ARM_GOOD, null);

        return resultRecord;

    }

    @Override
    public boolean execute(InteractionSpec ispec, Record input, Record output) throws ResourceException {

        InteractionSpecBase inSpec = null;

        boolean enabled = listener.isInteractionMetricsEnabled();
        System.out.println("InteractionImpl: execute(InteractionSpec, Record, Record), InteractionMetrics enabled=" + enabled);
        Object ctx = null;
        @SuppressWarnings("unused")
        byte[] armCorBytes = null;
        if (enabled) {
            ctx = listener.preInteraction(execute3args);
            armCorBytes = listener.getCorrelator();
        }
        // Note: the armCorBytes should be passed to the downstream EIS with the request in real RA

        try {
            inSpec = (InteractionSpecBase) ispec;
        } catch (ClassCastException cce) {
            System.err.println("InteractionSpec is not a CCIInteractionSpecImpl");
        }

        if (inSpec.getInteractionVerb().equals(InteractionSpecBase.EXECUTE_GOOD)) {
            //AWE set output fields here
            if (enabled) {
                postInteraction(ctx, InteractionMetrics.RM_ARM_GOOD, inSpec);
            }
            return true;
        } else if (inSpec.getInteractionVerb().equals(InteractionSpecBase.EXECUTE_BAD)) {
            if (enabled) {
                postInteraction(ctx, InteractionMetrics.RM_ARM_FAILED, inSpec);
            }
            return false;
        } else {
            if (enabled) {
                postInteraction(ctx, InteractionMetrics.RM_ARM_FAILED, inSpec);
            }
            throw new ResourceException("No Interaction verb is set");
        }

    }

    @Override
    public Connection getConnection() {
        return connection;
    }

    protected void setConnection(Connection con1) {
        try {
            connection = (ConnectionBase) con1;
            listener = connection.getInteractionListener();
            System.out.println("CCIInteractionImpl setConnection get InteractionMetrics listener=" + listener);
        } catch (ClassCastException cce) {
            connection = null;
        }
    }

    @Override
    public ResourceWarning getWarnings() throws ResourceException {
        return resWarn;
    }

    /*
     * Readable form of the javax.resource.cci.InteractionSpec: available at level 2
     * AdapterName
     * AdapterShortDescription
     * AdapterVendorName
     * AdapterVersion
     * InteractionSpecsSupported
     * SpecVersion
     *
     * Note: we use the static methods in CCIConnectionMetaDataImpl to get the tran detail data.
     * How to get data in RA is not part of the FVT for InteractionMetrics since our focus
     * is to test whether or not preInteraction and postInteraction work properly.
     */
    private void postInteraction(Object ctx, int status, InteractionSpecBase spec) {
        System.out.println("CCIInteraction postInteraction");

        int level = listener.getTranDetailLevel();

        System.out.println("CCIInteraction postInteraction level=" + level);

        Properties props = new Properties();

        if (level == 2) {
            props.put("InteractionSpec", spec);
        } else if (level == 3) {
            props.put("InteractionSpec", spec);
            props.put("AdapterName", ConnectionMetaDataBase.getAdapterName());
            props.put("AdapterShortDescription", ConnectionMetaDataBase.getAdapterShortDescription());
            props.put("AdapterVendorName", ConnectionMetaDataBase.getAdapterVendorName());
            props.put("AdapterVersion", ConnectionMetaDataBase.getAdapterVersion());
            props.put("InteractionSpecsSupported", ConnectionMetaDataBase.getInteractionSpecsSupported());
            props.put("SpecVersion", ConnectionMetaDataBase.getSpecVersion());
        }

        System.out.println("CCIInteraction postInteraction level=" + level + ", props=" + props.toString() + ", call postInteraction");
        listener.postInteraction(ctx, status, props);
    }

}
