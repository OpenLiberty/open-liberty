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
package com.ibm.ws.zos.channel.wola;

/**
 * A bunch of additional meta data passed along on a WOLA JCA request.
 */
public interface WolaJcaRequestInfo {

    /**
     * @return the connection ID (TODO: which conn id?)
     */
    public int getConnectionID();

    /**
     * @return the connection timeout time (in seconds).
     */
    public int getConnectionWaitTimeout();

    /**
     * @return the link task tran ID.
     */
    public String getLinkTaskTranID();

    /**
     * @return the link task request container ID.
     */
    public String getLinkTaskReqContID();

    /**
     * @return the link task request container type.
     */
    public int getLinkTaskReqContType();

    /**
     * @return the link task response container ID.
     */
    public String getLinkTaskRspContID();

    /**
     * @return the link task response container type.
     */
    public int getLinkTaskRspContType();

    /**
     * @return the link task channel ID.
     */
    public String getLinkTaskChanID();

    /**
     * @return the link task channel type.
     */
    public int getLinkTaskChanType();

    /**
     * @return the MVS User ID (TODO: which user id?)
     */
    public String getMvsUserID();

    /**
     * @return the Use Containers flag.
     */
    public int getUseCICSContainer();
}
