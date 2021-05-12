/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

/*
 * Simple Admin object, allows for a queue of messages to be passed between client and RA
 */

package com.ibm.tra14.inbound.impl;

import java.util.LinkedList;

import javax.jms.Message;
import javax.naming.Reference;

import com.ibm.tra14.inbound.base.TRAAdminObject;
import com.ibm.tra14.trace.DebugTracer;

public class TRAAdminObject1 implements javax.jms.Queue, TRAAdminObject {

    private static final long serialVersionUID = 1878774682403041700L;
    //@SuppressWarnings("unchecked")
    protected LinkedList _queue;
    protected Reference _ref = null;

    protected String _queueName;

    @Override
    public Reference getReference() {
        if (_ref == null) {
            _ref = new Reference(this.getClass().getName());
        }
        return _ref;
    }

    public void setReference(Reference ref) {
        _ref = ref;
    }

    //@SuppressWarnings("unchecked")
    public TRAAdminObject1() {
        _queueName = "defaultTestQueueName";
        _queue = new LinkedList();
    }

    //@SuppressWarnings("unchecked")
    public TRAAdminObject1(String name) {
        _queueName = name;
        _queue = new LinkedList();
    }

    public void setQueueName(String name) {
        _queueName = name;
    }

    @Override
    public String getQueueName() {
        return _queueName;
    }

    //@SuppressWarnings("unchecked")
    @Override
    public void putMsg(Message msg) {
        _queue.add(msg);
        if (DebugTracer.isDebugMessages()) {
            DebugTracer.getPrintStream().println("Adding message to queue: " + msg.toString());
        }
    }

    @Override
    public Message getMsg() {
        Message ret = (Message) _queue.poll();
        if (DebugTracer.isDebugMessages()) {
            DebugTracer.getPrintStream().println("Removing message from queue: " + ret.toString());
        }
        return ret;
    }

    @Override
    public String toString() {
        String ret = "TRAAdminObject1 from the Test Resource Adapter";
        return ret;
    }

}
