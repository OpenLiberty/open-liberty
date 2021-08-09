/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.channelfw.testsuite.channels.broken;

import java.util.Map;

import com.ibm.websphere.channelfw.ChannelData;
import com.ibm.ws.channelfw.testsuite.channels.protocol.ProtocolDummyChannel;
import com.ibm.wsspi.channelfw.exception.ChannelException;

/**
 * The purpose of this class is to allow the channel framework test code to force
 * error conditions into chain configuration. By default, each of the life cycle
 * methods will throw exceptions when called. The test program should catch them
 * and ensure the surround logic that lead to them being called supports the exception
 * in the documented behavior.
 */
public class BrokenChannel extends ProtocolDummyChannel {
    /** key on handling init stage */
    public static final String HANDLE_INIT = "HANDLE_INIT";
    /** key on handling start stage */
    public static final String HANDLE_START = "HANDLE_START";
    /** key on handling quiesce stage */
    public static final String HANDLE_QUIESCE = "HANDLE_QUIESCE";
    /** key on handling stop stage */
    public static final String HANDLE_STOP = "HANDLE_STOP";
    /** key on handling destroy stage */
    public static final String HANDLE_DESTROY = "HANDLE_DESTROY";
    /** setting for explicitly failing a particular stage */
    public static final String FAIL = "FAIL";
    /** setting for explicitly completing a particular stage */
    public static final String SUCCEED = "SUCCEED";

    // Default the behavior of all life cycle methods to throw exceptions.
    private boolean failInit = true;
    private boolean failStart = true;
    private boolean failQuiesce = true;
    private boolean failStop = true;
    private boolean failDestroy = true;

    /**
     * Constructor.
     * 
     * @param cc
     * @param factory
     *            s
     */
    public BrokenChannel(ChannelData cc, BrokenChannelFactory factory) {
        super(cc, factory);
        Map<Object, Object> props = cc.getPropertyBag();
        String failFlag = null;

        // Check if an entry exists in the property back to cause init to succeed.
        failFlag = (String) props.get(HANDLE_INIT);
        if (failFlag != null && failFlag.equals(SUCCEED)) {
            failInit = false;
        }
        // Check if an entry exists in the property back to cause start to succeed.
        failFlag = (String) props.get(HANDLE_START);
        if (failFlag != null && failFlag.equals(SUCCEED)) {
            failStart = false;
        }
        // Check if an entry exists in the property back to cause stop to succeed.
        failFlag = (String) props.get(HANDLE_STOP);
        if (failFlag != null && failFlag.equals(SUCCEED)) {
            failStop = false;
        }
        // Check if an entry exists in the property back to cause destroy to succeed.
        failFlag = (String) props.get(HANDLE_DESTROY);
        if (failFlag != null && failFlag.equals(SUCCEED)) {
            failDestroy = false;
        }
    }

    public void init() throws ChannelException {
        if (failInit) {
            throw new ChannelException("Coded to throw exception");
        }
    }

    public void start() throws ChannelException {
        if (failStart) {
            throw new ChannelException("Coded to throw exception");
        }
    }

    public void stop(long millisec) throws ChannelException {
        if (millisec == 0) {
            if (failStop) {
                throw new ChannelException("Coded to throw exception");
            }
        } else {
            if (failQuiesce) {
                throw new ChannelException("Coded to throw exception");
            }
        }
    }

    public void destroy() throws ChannelException {
        if (failDestroy) {
            throw new ChannelException("Coded to throw exception");
        }
    }
}
