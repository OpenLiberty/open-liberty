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

import com.ibm.ws.j2c.InteractionMetrics;

class LocalTransactionBase implements javax.resource.cci.LocalTransaction, javax.resource.spi.LocalTransaction {

    private static final int TRANS_ACTIVE = 1;
    private static final int TRANS_INACTIVE = 2;
    private static final int TRANS_ROLLBACK = 3;

    private int status = TRANS_INACTIVE;

    private ConnectionMetaDataBase metaData = null;
    private InteractionMetrics listener = null;

    private static final String[] BEGIN_METHOD = new String[] { "fvt.cciadapter.CCILocalTransactionImpl", "begin" };
    private static final String[] COMMIT_METHOD = new String[] { "fvt.cciadapter.CCILocalTransactionImpl", "commit" };
    private static final String[] ROLLBACK_METHOD = new String[] { "fvt.cciadapter.CCILocalTransactionImpl", "rollback" };

    public LocalTransactionBase(ConnectionMetaDataBase metaData, InteractionMetrics listener) {
        this.metaData = metaData;
        this.listener = listener;

        if (this.listener == null)
            System.out.println("ERROR: CCILocalTransactionImpl constructor, null InteractionMetrics listener");
        else
            System.out.println("CCILocalTransactionImpl constructor, InteractionMetrics listener=" + listener);
    }

    @Override
    public void begin() throws ResourceException {
        // test InteractionMetrics
        boolean enabled = listener.isInteractionMetricsEnabled();
        System.out.println("CCILocalTransactionImpl begin, InteractionMetrics enabled=" + enabled);
        Object ctx = null;
        @SuppressWarnings("unused")
        byte[] armCorBytes = null;
        if (enabled) {
            ctx = listener.preInteraction(BEGIN_METHOD);
            armCorBytes = listener.getCorrelator();
        }

        if (status == TRANS_INACTIVE) {
            status = TRANS_ACTIVE;
        } else if (status == TRANS_ACTIVE) {
            // call postInteraction
            if (enabled) {
                postInteraction(ctx, InteractionMetrics.RM_ARM_FAILED, metaData);
            }
            throw new ResourceException("begin transaction failed:  Transaction is already started");
        } else if (status == TRANS_ROLLBACK) {
            // call postInteraction
            if (enabled) {
                postInteraction(ctx, InteractionMetrics.RM_ARM_FAILED, metaData);
            }
            throw new ResourceException("begin transaction failed:  Transaction is in rollback state");
        } else {
            // call postInteraction
            if (enabled) {
                postInteraction(ctx, InteractionMetrics.RM_ARM_FAILED, metaData);
            }
            throw new ResourceException("begin transaction failed:  Transaction state not recognized");
        }

        // call postInteraction
        if (enabled) {
            postInteraction(ctx, InteractionMetrics.RM_ARM_GOOD, metaData);
        }
    }

    @Override
    public void commit() throws ResourceException {
        boolean enabled = listener.isInteractionMetricsEnabled();
        System.out.println("CCILocalTransactionImpl commit, InteractionMetrics enabled=" + enabled);
        Object ctx = null;
        @SuppressWarnings("unused")
        byte[] armCorBytes = null;
        if (enabled) {
            ctx = listener.preInteraction(COMMIT_METHOD);
            armCorBytes = listener.getCorrelator();
        }

        if (status == TRANS_INACTIVE) {
            // call postInteraction
            if (enabled) {
                postInteraction(ctx, InteractionMetrics.RM_ARM_FAILED, metaData);
            }
            throw new ResourceException("commit transaction failed:  Transaction is not started");
        } else if (status == TRANS_ACTIVE) {
            status = TRANS_INACTIVE;
        } else if (status == TRANS_ROLLBACK) {
            status = TRANS_INACTIVE;
            // call postInteraction
            if (enabled) {
                postInteraction(ctx, InteractionMetrics.RM_ARM_FAILED, metaData);
            }
            throw new ResourceException("commit transaction failed:  Transaction was rolled back");
        } else {
            // call postInteraction
            if (enabled) {
                postInteraction(ctx, InteractionMetrics.RM_ARM_FAILED, metaData);
            }
            throw new ResourceException("commit transaction failed:  Transaction state not recognized");
        }

        // call postInteraction
        if (enabled) {
            postInteraction(ctx, InteractionMetrics.RM_ARM_GOOD, metaData);
        }
    }

    @Override
    public void rollback() throws ResourceException {
        boolean enabled = listener.isInteractionMetricsEnabled();
        System.out.println("CCILocalTransactionImpl rollback, InteractionMetrics enabled=" + enabled);
        Object ctx = null;
        @SuppressWarnings("unused")
        byte[] armCorBytes = null;
        if (enabled) {
            ctx = listener.preInteraction(ROLLBACK_METHOD);
            armCorBytes = listener.getCorrelator();
        }

        if (status == TRANS_INACTIVE) {
            // call postInteraction
            if (enabled) {
                postInteraction(ctx, InteractionMetrics.RM_ARM_FAILED, metaData);
            }
            throw new ResourceException("rollback transaction failed:  Transaction is not started");
        } else {
            status = TRANS_INACTIVE;
        }

        // call postInteraction
        if (enabled) {
            postInteraction(ctx, InteractionMetrics.RM_ARM_GOOD, metaData);
        }
    }

    /*
     * AdapterName
     * AdapterShortDescription
     * AdapterVendorName
     * AdapterVersion
     * InteractionSpecsSupported
     * SpecVersion
     */
    private void postInteraction(Object ctx, int status, ConnectionMetaDataBase metaData) {

        System.out.println("CCILocalTransactionImpl postInteraction");

        Properties props = null;
        int level = listener.getTranDetailLevel();

        if (level == 3) {
            props = new Properties();
            props.put("AdapterName", ConnectionMetaDataBase.getAdapterName());
            props.put("AdapterShortDescription", ConnectionMetaDataBase.getAdapterShortDescription());
            props.put("AdapterVendorName", ConnectionMetaDataBase.getAdapterVendorName());
            props.put("AdapterVersion", ConnectionMetaDataBase.getAdapterVersion());
            props.put("InteractionSpecsSupported", ConnectionMetaDataBase.getInteractionSpecsSupported());
            props.put("SpecVersion", ConnectionMetaDataBase.getSpecVersion());
        }

        listener.postInteraction(ctx, status, props);
    }
}
