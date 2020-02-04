/*******************************************************************************
 * Copyright (c) 2013,2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package web.mdb.bindings;

import java.util.concurrent.ConcurrentLinkedQueue;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

// This use to test the useJNDI property for wmqJms but that is no longer possible as the special processing
// for useJNDI with wmqJms now only occurs if wmqJms is the adapter. 
// This test now only covers if useJNDI happens to be a property on a resource adapter other than
// the WebSphere MQ resource adapter in which case the value should be passed as is and destination
// should be set to the configured value.
//
// Note that the ActiveMQ adapter has a similar property "useJndi", but that does not match useJNDI,
// so the special processing never occurred for that adapter.
@MessageDriven(activationConfig = {
                                   @ActivationConfigProperty(propertyName = "useJNDI", propertyValue = "true")
})
public class FVTMessageDrivenBeanBinding implements MessageListener {

    public static final ConcurrentLinkedQueue<String> messages = new ConcurrentLinkedQueue<String>();

    @Override
    public void onMessage(Message message) {
        try {
            messages.add(((TextMessage) message).getText());
        } catch (JMSException x) {
            throw new RuntimeException(x);
        }
    }
}
