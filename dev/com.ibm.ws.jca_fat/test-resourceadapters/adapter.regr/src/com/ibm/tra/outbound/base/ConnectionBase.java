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

import javax.resource.ResourceException;
import javax.resource.cci.ConnectionMetaData;
import javax.resource.cci.Interaction;
import javax.resource.cci.LocalTransaction;
import javax.resource.cci.ResultSetInfo;

import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;
import com.ibm.tra.SimpleRAImpl;
import com.ibm.ws.j2c.InteractionMetrics;

/**
 *         To change the template for this generated type comment go to
 *         Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class ConnectionBase implements javax.resource.cci.Connection {

    public static final String EIS_DOWN = "down";
    public static final String EIS_OK = "up";
    public static final String EIS_HANG = "wait";

    @SuppressWarnings("unused")
    private ConnectionSpecBase conSpec;

    private String eisStatus = EIS_OK;
    @SuppressWarnings("unused")
    private boolean bInitialized = false;
    @SuppressWarnings("unused")
    private boolean bSignalError = false;

    private ManagedConnectionBase mc = null;

    private static final TraceComponent tc = Tr.register(ConnectionBase.class, SimpleRAImpl.RAS_GROUP, null);

    /**
     *
     */
    protected ConnectionBase() {
        eisStatus = EIS_OK;
    }

    public ConnectionBase(ManagedConnectionBase mc, ConnectionRequestInfoBase cxReq) {
        this.mc = mc;
        System.out.println("CCIConnectionImpl constructor mc=" + mc);
    }

    public void initialize(ConnectionSpecBase initConSpec) {
        final String methodName = "initialize";
        Tr.entry(tc, methodName);

        bInitialized = true;

        eisStatus = initConSpec.getEISStatus();
        conSpec = initConSpec;

        Tr.exit(tc, methodName);

    }

    /**
     * @see com.ibm.tra.outbound.base.Connection#close()
     */
    @Override
    public void close() throws ResourceException {
        final String methodName = "close";
        Tr.entry(tc, methodName);

        bInitialized = false;

        Tr.exit(tc, methodName);
    }

    /**
     * @see com.ibm.tra.outbound.base.Connection#createInteraction()
     *      Creates a fake interaction with a EIS resource.
     */
    @Override
    public Interaction createInteraction() throws ResourceException {
        final String methodName = "createInteraction";
        Tr.entry(tc, methodName);

        InteractionBase inter = null;

        if (eisStatus.equals(EIS_OK)) {
            inter = new InteractionBase();
            inter.setConnection(this);
        } else if (eisStatus.equals(EIS_DOWN)) {
            throw new ResourceException("EIS Service is not available");
        } else if (eisStatus.equals(EIS_HANG)) {
            try {
                Thread.sleep(600000);
            } catch (Exception e) {

            }
        } else {
            throw new ResourceException("EIS Status is unknown.");
        }

        Tr.exit(tc, methodName);

        return inter;

    }

    /**
     * @see com.ibm.tra.outbound.base.Connection#getLocalTransaction()
     *      Creating a local transaction and returning it for the user to use. Note that at this point this
     *      is a fake local transaction and really isn't going to be used to demarcate anything.
     */
    @Override
    public LocalTransaction getLocalTransaction() {
        final String methodName = "getLocalTransaction";
        Tr.entry(tc, methodName);

        LocalTransactionBase localTran = new LocalTransactionBase(new ConnectionMetaDataBase(), getInteractionListener());

        Tr.exit(tc, methodName);

        return localTran;

    }

    /**
     * @see com.ibm.tra.outbound.base.Connection#getMetaData()
     *      Gets the meta data object CCIConnectionMetaData
     */
    @Override
    public ConnectionMetaData getMetaData() {
        final String methodName = "getMetaData";
        Tr.entry(tc, methodName);

        ConnectionMetaDataBase metaData = new ConnectionMetaDataBase();

        Tr.exit(tc, methodName);

        return metaData;
    }

    /**
     * @see com.ibm.tra.outbound.base.Connection#getResultSetInfo()
     *      Gets the details of various result set supported features. These are all definied in our CCIResultSetInfo object
     */
    @Override
    public ResultSetInfo getResultSetInfo() {
        final String methodName = "getResultSetInfo";
        Tr.entry(tc, methodName);

        ResultSetInfoBase resSetInfo = new ResultSetInfoBase();

        Tr.exit(tc, methodName);

        return resSetInfo;
    }

    protected InteractionMetrics getInteractionListener() {
        if (mc == null)
            return null;
        else
            return mc.getInteractionListener();
    }

}
