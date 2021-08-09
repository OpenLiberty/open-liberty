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

package com.ibm.tra.ann;

import java.math.BigInteger;
import java.util.LinkedList;

import javax.jms.Message;
import javax.naming.NamingException;
import javax.naming.Reference;
import javax.resource.spi.AdministeredObject;
import javax.resource.spi.ConfigProperty;

import com.ibm.tra.inbound.base.TRAAdminObject;
import com.ibm.tra.trace.DebugTracer;

@SuppressWarnings("serial")
@AdministeredObject(
                    adminObjectInterfaces = { TRAAdminObject.class })
public class ConfigPropertyValidationAdminObjectAnn7 implements TRAAdminObject {

    protected Reference _ref = null;
    @SuppressWarnings("unchecked")
    protected LinkedList _queue;
    protected String _className = this.getClass().getName();
    protected String _factoryName = "com.ibm.tra.inbound.impl.TRAObjectFactory";

    BigInteger height;

    @SuppressWarnings("unchecked")
    public ConfigPropertyValidationAdminObjectAnn7() {
        _queue = new LinkedList();
    }

    @Override
    public Reference getReference() throws NamingException {
        if (_ref == null) {
            _ref = new Reference(this.getClass().getName(), _factoryName, null);
        }
        return _ref;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void putMsg(Message msg) {
        synchronized (_queue) {
            if (DebugTracer.isDebugMessages()) {
                DebugTracer.getPrintStream().println("Adding message to queue: " + msg.toString());
            }
            _queue.addLast(msg);
        }
    }

    @Override
    public Message getMsg() {
        Message ret = null;
        synchronized (_queue) {
            ret = (Message) _queue.poll();
        }
        if (DebugTracer.isDebugMessages()) {
            DebugTracer.getPrintStream().println("Removing message from queue: " + ret.toString());
        }
        return ret;
    }

    @Override
    public String toString() {
        return _className;
    }

    public void deleteMsgs() {
        synchronized (_queue) {
            _queue.clear();
        }
    }

    public BigInteger getHeight() {
        return height;
    }

    @ConfigProperty(type = BigInteger.class)
    public void setHeight(BigInteger height) {
        this.height = height;
    }
}
