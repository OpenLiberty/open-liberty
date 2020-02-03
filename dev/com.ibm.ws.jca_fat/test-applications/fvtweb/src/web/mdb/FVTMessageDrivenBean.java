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
package web.mdb;

import java.util.concurrent.ConcurrentLinkedQueue;

import javax.annotation.Resource;
import javax.ejb.MessageDriven;
import javax.ejb.MessageDrivenContext;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

@MessageDriven
public class FVTMessageDrivenBean implements MessageListener {

    public static final ConcurrentLinkedQueue<String> messages = new ConcurrentLinkedQueue<String>();

    @Resource
    MessageDrivenContext ejbcontext;

    @Override
    public void onMessage(Message message) {
        try {
            messages.add(((TextMessage) message).getText());
        } catch (JMSException x) {
            throw new RuntimeException(x);
        }
    }
}
