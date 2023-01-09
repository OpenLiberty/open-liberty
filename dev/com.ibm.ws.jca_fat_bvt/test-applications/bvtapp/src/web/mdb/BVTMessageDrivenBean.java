/*******************************************************************************
 * Copyright (c) 2012,2022 IBM Corporation and others.
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
package web.mdb;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import jakarta.annotation.Resource;
import jakarta.ejb.MessageDriven;
import jakarta.ejb.MessageDrivenContext;
import jakarta.resource.ResourceException;
import jakarta.resource.cci.MessageListener;
import jakarta.resource.cci.Record;

@MessageDriven
public class BVTMessageDrivenBean implements MessageListener {

    public static final ConcurrentLinkedQueue<String> messages = new ConcurrentLinkedQueue<String>();

    @Resource
    MessageDrivenContext ejbcontext;

    /*
     * (non-Javadoc)
     * 
     * @see jakarta.resource.cci.MessageListener#onMessage(jakarta.resource.cci.Record)
     */
    @Override
    public Record onMessage(Record arg0) throws ResourceException {
        System.out.println("onMessage invoked " + arg0);
        messages.add("onMessage: [" + ((List<String>) arg0).get(0) + "]");
        return arg0;
    }
}
