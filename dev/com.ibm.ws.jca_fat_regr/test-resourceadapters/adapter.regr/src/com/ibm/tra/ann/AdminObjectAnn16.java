/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.tra.ann;

import java.util.LinkedList;

import javax.jms.Message;
import javax.resource.spi.AdministeredObject;

import com.ibm.tra.inbound.base.TRAAdminObject1;
import com.ibm.tra.trace.DebugTracer;

@SuppressWarnings("serial")
@AdministeredObject(
                    adminObjectInterfaces = {})
public class AdminObjectAnn16 extends AdminObjectAnnSuperClass3 implements TRAAdminObject1 {

    protected String _className = this.getClass().getName();

    @SuppressWarnings("unchecked")
    public AdminObjectAnn16() {
        _queue = new LinkedList();
    }

    @SuppressWarnings("unchecked")
    public void putMsg(Message msg) {
        synchronized (_queue) {
            if (DebugTracer.isDebugMessages()) {
                DebugTracer.getPrintStream().println("Adding message to queue: " + msg.toString());
            }
            _queue.addLast(msg);
        }
    }

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

    public int countMessages() {
        int size = 0;
        synchronized (_queue) {
            size = _queue.size();
        }
        return size;
    }

    public String toString() {
        return _className;
    }

}
