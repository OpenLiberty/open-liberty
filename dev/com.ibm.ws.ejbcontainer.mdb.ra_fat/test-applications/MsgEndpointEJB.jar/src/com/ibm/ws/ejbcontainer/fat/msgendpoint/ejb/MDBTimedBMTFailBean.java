/*******************************************************************************
 * Copyright (c) 2014, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.fat.msgendpoint.ejb;

import static org.junit.Assert.fail;

import javax.ejb.MessageDrivenContext;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.naming.InitialContext;

public class MDBTimedBMTFailBean implements MessageListener {
    /**
     * Test illegal access from Constructor on a CMT Message
     * Driven bean that implements the TimedObject interface. <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li> Constructor.getTimerService() fails with IllegalStateException
     * </ol>
     */
    public MDBTimedBMTFailBean() {
        try {
            System.out.println("EJB Constructor: Calling getTimerService()");
            InitialContext ic = new InitialContext();
            MessageDrivenContext myMessageDrivenCtx = (MessageDrivenContext) ic.lookup("java:comp/EJBContext");

            if (myMessageDrivenCtx != null) {
                myMessageDrivenCtx.getTimerService();
            } else {
                System.out.println("myMessageDrivenCtx is null.");
            }

            fail("1 ---> getTimerService should have failed!");
        } catch (IllegalStateException ise) {
            System.out.println("MDBTimedBMTFailBean caught expected IllegalStateException");
        } catch (Throwable th) {
            System.out.println("Unexpected exception from getTimerService(): " + th);
        }
    }

    @Override
    public void onMessage(Message msg) {
    }

    @Timeout
    public void timeout(Timer timer) {
    }
}
