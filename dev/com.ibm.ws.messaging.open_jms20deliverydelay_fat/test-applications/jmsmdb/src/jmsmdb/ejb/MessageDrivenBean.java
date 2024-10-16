/*******************************************************************************
 * Copyright (c) 2012, 2020 IBM Corporation and others.
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
package jmsmdb.ejb;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.EJBContext;
import javax.ejb.MessageDriven;
import javax.ejb.MessageDrivenContext;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;
import javax.jms.StreamMessage;

@MessageDriven
public class MessageDrivenBean implements MessageListener {

    @Resource
    MessageDrivenContext ejbcontext;

    @SuppressWarnings("unused")
    @Resource
    private void setMessageDrivenContext(EJBContext ejbcontext) {
        System.out.println("TODO: remove this if we don't need it: setMessageDrivenContext invoked");
    }
    
    @PostConstruct
    public void postConstruct() {
        System.out.println("TODO: remove this if we don't need it: postConstruct invoked");
    }

    private static final SimpleDateFormat DATE_FORMAT =
        new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

    @Override
    public void onMessage(Message message) {
        try {
            if ( message instanceof StreamMessage ) {
                Calendar now = Calendar.getInstance();

                StreamMessage msg = (StreamMessage) message;

                String text = msg.readString();

                Calendar min = Calendar.getInstance();
                min.setTimeInMillis( msg.readLong() );

                if ( min.before(now) ) {
                    System.out.println("Message received on mdb : " + text + " at " + now);

                } else {
                    System.out.println(
                        "Message \"" + text + "\" received at " + (min.getTimeInMillis() - now.getTimeInMillis()) +
                        " ms before expected delivery delay (at " + DATE_FORMAT.format( min.getTime() ) + ")." );
                }

            } else {
                System.out.println(message);

                TextMessage msg = (TextMessage) message;
                System.out.println("Message received on mdb : " + msg.getText());
            }

        } catch ( Exception e ) {
            throw new RuntimeException(e);
        }
    }
}
