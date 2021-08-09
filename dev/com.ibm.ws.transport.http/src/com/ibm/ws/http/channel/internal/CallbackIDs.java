/*******************************************************************************
 * Copyright (c) 2004, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.http.channel.internal;

import com.ibm.wsspi.genericbnf.GenericKeys;

/**
 * The HTTP channel has a number of callbacks used throughout depending on
 * the context of the read or write. This class provides a list of Callback
 * IDs used when storing ServiceContext references in the VirtualConnection's
 * state map (i.e ID=mySC)
 * 
 */
public class CallbackIDs extends GenericKeys {

    /** Counter for the number of ids defined, used as an ordinal for each ID */
    private static int NUM_IDS = 0;

    /** ID for an HttpOutboundServiceContext read/write */
    public static final CallbackIDs CALLBACK_HTTPOSC = new CallbackIDs("HttpOSC");
    /** ID for an HTTP inbound connection link read/write */
    public static final CallbackIDs CALLBACK_HTTPICL = new CallbackIDs("HttpICL");
    /** ID for an HttpInboundServiceContext read/write */
    public static final CallbackIDs CALLBACK_HTTPISC = new CallbackIDs("HttpISC");

    /**
     * Constructor for a callback id object
     * 
     * @param s
     */
    public CallbackIDs(String s) {
        super(s, NUM_IDS++);
    }
}
