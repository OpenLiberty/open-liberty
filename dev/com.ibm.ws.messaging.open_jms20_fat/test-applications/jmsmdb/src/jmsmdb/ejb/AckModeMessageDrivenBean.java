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
package jmsmdb.ejb;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.EJBContext;
import javax.ejb.MessageDriven;
import javax.ejb.MessageDrivenContext;
import javax.jms.Message;
import javax.jms.MessageListener;

@MessageDriven
public class AckModeMessageDrivenBean implements MessageListener {
    static int i = 0;

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

    /*
     * Simple Basic onMessage Method for consuming Message.
     * Whenever Message will reach on defined Queue this method will be called.
     */
    @Override
    public void onMessage(Message message) {
        try {
            i++;
            System.out.println("The Message No Received=" + i);
            System.out.println((new StringBuilder()).append("Message received on mdb").append(message).toString());

        } catch (Exception x) {
            x.printStackTrace();
        }
    }
}
