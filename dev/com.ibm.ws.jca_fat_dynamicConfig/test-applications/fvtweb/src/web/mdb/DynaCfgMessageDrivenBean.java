/*******************************************************************************
 * Copyright (c) 2013, 2020 IBM Corporation and others.
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

import java.util.concurrent.ConcurrentLinkedQueue;

import javax.annotation.Resource;
import javax.ejb.MessageDriven;
import javax.ejb.MessageDrivenContext;
import javax.resource.ResourceException;
import javax.resource.cci.MessageListener;
import javax.resource.cci.Record;

@MessageDriven
public class DynaCfgMessageDrivenBean implements MessageListener {

    public static final ConcurrentLinkedQueue<String> messages = new ConcurrentLinkedQueue<String>();

    @Resource
    MessageDrivenContext ejbcontext;

    /**
     * @see javax.resource.cci.MessageListener#onMessage(javax.resource.cci.Record)
     */
    @Override
    public Record onMessage(Record record) throws ResourceException {
        System.out.println("onMessage invoked " + record + " name " + record.getRecordName());
        messages.add(record.getRecordName());
        return record;
    }
}
