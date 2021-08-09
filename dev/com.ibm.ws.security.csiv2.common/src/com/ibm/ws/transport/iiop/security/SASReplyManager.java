/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
/*
 * Some of the code was derived from code supplied by the Apache Software Foundation licensed under the Apache License, Version 2.0.
 */
package com.ibm.ws.transport.iiop.security;

import java.util.Hashtable;
import java.util.Map;

import org.omg.CSI.SASContextBody;

/**
 * Stores requests' SASContextBody because get/setSlot does not seem to work in
 * OpenORB.
 * <p/>
 * TODO: There may be an error where the interceptor does not remove the
 * registered subjects. We should have a daemon that cleans up old requests.
 * 
 * @version $Revision: 451417 $ $Date: 2006-09-29 13:13:22 -0700 (Fri, 29 Sep 2006) $
 */
public final class SASReplyManager {
    private final static Map requestSASMsgs = new Hashtable();

    public static SASContextBody getSASReply(int requestId) {
        return (SASContextBody) requestSASMsgs.get(requestId);
    }

    public static void setSASReply(int requestId, SASContextBody sasMsg) {
        requestSASMsgs.put(requestId, sasMsg);
    }

    public static SASContextBody clearSASReply(int requestId) {
        return (SASContextBody) requestSASMsgs.remove(requestId);
    }
}
