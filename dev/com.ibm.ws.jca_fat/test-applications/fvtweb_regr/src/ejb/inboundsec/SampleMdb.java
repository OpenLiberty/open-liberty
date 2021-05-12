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

package ejb.inboundsec;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.MessageDriven;
import javax.ejb.MessageDrivenContext;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

@MessageDriven
public class SampleMdb implements MessageListener {

    @Resource
    MessageDrivenContext messageContext = null;

    public String message;

    @EJB
    SecureSessionLocal local;

    @Override
    public void onMessage(Message msg) {
        TextMessage txtMessage = (TextMessage) msg;
        try {
            message = txtMessage.getText();
            System.out.println("On Message called with message:" + message);
            System.out.println(messageContext.getCallerPrincipal());
        } catch (JMSException e) {
            e.printStackTrace(System.out);
        }

        if (message.contains("testEJBInvocation")) {
            local.execute();
            try {
                local.executeSpecial();
            } catch (Exception e) {
                e.printStackTrace(System.out);
            }
        }
    }
}
