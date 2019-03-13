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
package com.ibm.ws.jaxrs20.injection.metadata;

import org.apache.cxf.jaxrs.model.OperationResourceInfo;
import org.apache.cxf.message.Message;

/**
 *
 */
public class ParamInjectionMetadata {

    OperationResourceInfo operationResourceInfo;
    Message inMessage;

    public ParamInjectionMetadata(OperationResourceInfo operationResourceInfo, Message inMessage) {
        this.operationResourceInfo = operationResourceInfo;
        this.inMessage = inMessage;
    }

    /**
     * @return the operationResourceInfo
     */
    public OperationResourceInfo getOperationResourceInfo() {
        return operationResourceInfo;
    }

    /**
     * @param operationResourceInfo the operationResourceInfo to set
     */
    public void setOperationResourceInfo(OperationResourceInfo operationResourceInfo) {
        this.operationResourceInfo = operationResourceInfo;
    }

    /**
     * @return the inMessage
     */
    public Message getInMessage() {
        return inMessage;
    }

    /**
     * @param inMessage the inMessage to set
     */
    public void setInMessage(Message inMessage) {
        this.inMessage = inMessage;
    }

}
