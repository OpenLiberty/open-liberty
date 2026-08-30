/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.zos.channel.wola.internal;

import com.ibm.ws.zos.channel.wola.WolaJcaRequestInfo;

/**
 * A simple implementation of WolaJcaRequestInfo for testing purposes.
 */
public class TestWolaJcaRequestInfoImpl implements WolaJcaRequestInfo {

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.zos.channel.wola.WolaJcaRequestInfo#getConnectionID()
     */
    @Override
    public int getConnectionID() {
        return 11;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.zos.channel.wola.WolaJcaRequestInfo#getConnectionWaitTimeout()
     */
    @Override
    public int getConnectionWaitTimeout() {
        return 10;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.zos.channel.wola.WolaJcaRequestInfo#getLinkTaskTranID()
     */
    @Override
    public String getLinkTaskTranID() {
        return "Tran";
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.zos.channel.wola.WolaJcaRequestInfo#getLinkTaskReqContID()
     */
    @Override
    public String getLinkTaskReqContID() {
        return "ReqContID";
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.zos.channel.wola.WolaJcaRequestInfo#getLinkTaskReqContType()
     */
    @Override
    public int getLinkTaskReqContType() {
        return 5;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.zos.channel.wola.WolaJcaRequestInfo#getLinkTaskRspContID()
     */
    @Override
    public String getLinkTaskRspContID() {
        return "RspContID";
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.zos.channel.wola.WolaJcaRequestInfo#getLinkTaskRspContType()
     */
    @Override
    public int getLinkTaskRspContType() {
        return 6;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.zos.channel.wola.WolaJcaRequestInfo#getLinkTaskChanID()
     */
    @Override
    public String getLinkTaskChanID() {
        return "ChanID";
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.zos.channel.wola.WolaJcaRequestInfo#getLinkTaskChanType()
     */
    @Override
    public int getLinkTaskChanType() {
        return 7;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.zos.channel.wola.WolaJcaRequestInfo#getMvsUserID()
     */
    @Override
    public String getMvsUserID() {
        return "MSTONE1";
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.zos.channel.wola.WolaJcaRequestInfo#getUseCICSContainer()
     */
    @Override
    public int getUseCICSContainer() {
        return 1;
    }

}