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
package com.ibm.ws.jaxws.globalhandler;

import javax.xml.ws.LogicalMessage;

import org.apache.cxf.jaxws.handler.logical.LogicalMessageContextImpl;
import org.apache.cxf.message.Message;

/**
 *
 */
public class GlobalHandlerLogicalMsgCtxt extends LogicalMessageContextImpl {

    /**
     * @param wrapped
     */
    public GlobalHandlerLogicalMsgCtxt(Message wrapped) {
        super(wrapped);
        // TODO Auto-generated constructor stub
    }

    @Override
    public LogicalMessage getMessage() {
        return new GlobalHandlerLogicalMessageImpl(this);
    }

}
