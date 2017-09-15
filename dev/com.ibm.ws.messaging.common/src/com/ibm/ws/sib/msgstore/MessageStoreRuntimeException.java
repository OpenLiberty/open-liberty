package com.ibm.ws.sib.msgstore;
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

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.ws.exception.WsRuntimeException;

public class MessageStoreRuntimeException extends WsRuntimeException
{
    private static final long serialVersionUID = -3790027338845641878L;

    private static TraceNLS nls = TraceNLS.getTraceNLS(MessageStoreConstants.MSG_BUNDLE);

    public MessageStoreRuntimeException()
    {
        super();
    }

    /**
     * @param arg0
     */
    public MessageStoreRuntimeException(String arg0)
    {
        super(nls.getString(arg0));
    }

    /**
     * @param arg0
     */
    public MessageStoreRuntimeException(Throwable arg0)
    {
        super(arg0);
    }

    /**
     * @param arg0
     * @param arg1
     */
    public MessageStoreRuntimeException(String arg0, Throwable arg1)
    {
        super(nls.getString(arg0), arg1);
    }

    /**
     * Provide a key and use formatted string
     * @param arg0
     * @param args
     */
    public MessageStoreRuntimeException(String arg0, Object[] args)
    {
        super(nls.getFormattedMessage(arg0, args, null));
    }

    /**
     * Provide a key and use formatted string
     * @param arg0
     * @param args
     */
    public MessageStoreRuntimeException(String arg0, Object[] args, Throwable exp)
    {
        super(nls.getFormattedMessage(arg0, args, null), exp);
    }
}

