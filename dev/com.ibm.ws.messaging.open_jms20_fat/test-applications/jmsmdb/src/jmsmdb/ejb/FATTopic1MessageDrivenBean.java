/*
 * Copyright (c) 2012,2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 * --------------- -------- -------- ------------------------------------------
 * 93171           07022013 chetbhat Increasing the message receive wait period
 */
package jmsmdb.ejb;

import javax.ejb.MessageDriven;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

@MessageDriven
public class FATTopic1MessageDrivenBean implements MessageListener {

    @Override
    public void onMessage(Message message) {
        try {
            System.out.println("Message Received in MDB1: "
                               + ((TextMessage) message).getText());

        } catch (Exception x) {
            throw new RuntimeException(x);
        }
    }

}
