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
package com.ibm.tra.inbound.impl;

import java.io.PrintStream;

import javax.jms.Message;
import javax.jms.MessageListener;

import com.ibm.tra.trace.DebugTracer;

public class TRAMessageListener1 implements MessageListener {

    public void onMessage(Message arg0) {
        if (DebugTracer.isDebugMessages()) {
            PrintStream out = DebugTracer.getPrintStream();
            out.println("TRAMessageListener1 Recieved a message: " + arg0.toString());
        }

    }

}
