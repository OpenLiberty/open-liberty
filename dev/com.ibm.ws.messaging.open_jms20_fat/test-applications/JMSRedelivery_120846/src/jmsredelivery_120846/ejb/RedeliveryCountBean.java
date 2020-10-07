/*
 * Copyright (c) 2012,2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package jmsredelivery_120846.ejb;

import javax.jms.Message;
import javax.jms.MessageListener;

public class RedeliveryCountBean implements MessageListener {
    private static final long serialVersionUID = 1L;

    //

    public int redeliveryCount = 0;

    public int getRedeliveryCount() {
        return redeliveryCount;
    }

    public void setRedeliveryCount(int redeliveryCount) {
        this.redeliveryCount = redeliveryCount;
    }

    //

    @Override
    public void onMessage(Message message) {
        System.out.println( getClass().getSimpleName() + ".onMessage" + " [ " + message + " ]" );
        try {
            System.out.println("Redelivered [ " + message.getBooleanProperty("redelivered") + " ]");
        } catch ( Exception ex ) {
            System.out.println("Redelivered [ ** FAILED ** ]");
            ex.printStackTrace(System.out);
        }

        try {
            System.out.println("Delivery count [ " + message.getIntProperty("deliveryCount") + " ]");
        } catch ( Exception ex ) {
            System.out.println("Delivery count [ ** FAILED ** ]");
            ex.printStackTrace(System.out);
        }

        throw new RuntimeException();
    }
}
