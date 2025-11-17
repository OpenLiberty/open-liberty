/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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
package com.ibm.ws.messaging.security.fat.tests.roleordering;

import java.io.PrintStream;
import java.util.Date;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import componenttest.annotation.AllowedFFDC;
import componenttest.app.FATServlet;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
import javax.jms.JMSProducer;
import javax.jms.JMSRuntimeException;
import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.TextMessage;
import junit.framework.Assert;

@WebServlet("/")
public class MessagingServlet extends FATServlet {

    private static final PrintStream out = System.out;

    private enum creds {

        USER1("user1", "user1pwd"), //All
        USER2("user2", "user2pwd"), //Send Only
        USER3("user3", "user3pwd"); //Receive only

        private final String user;
        private final String password;

        creds(String user, String password) {
            this.user = user;
            this.password = password;
        }

        public String getuserid() {
            return user;
        }

        public String getpassword() {
            return password;
        }
    }

    private final static String connectionFactoryLookupName = "jms/TestConnectionFactory";
    private final static String queueLookupName = "jms/TestQueue";

    @Test
    @AllowedFFDC(value = { "com.ibm.ws.sib.jfapchannel.JFapConnectionBrokenException" }) //TODO fix this harmless error
    public void testRoleOrdering() throws Exception {

        Date date = new Date();

        out.println("Running the messaging servlet: " + date.toString());

        ConnectionFactory cf = this.getConnectionFactory();
        Queue queue = this.getQueue();

        out.println("====== Testing user1 ==========");
        try {
            sendMessage(cf, queue, creds.USER1.getuserid(), creds.USER1.getpassword());
            receiveMessage(cf, queue, creds.USER1.getuserid(), creds.USER1.getpassword());
        } catch (JMSRuntimeException e) {
            out.println("====== Unexpected failure when testing user1 ==========");
            out.print(e.toString());
            Assert.fail("Unexpected failure when testing user1");
        }

        out.println("====== Testing user2 ==========");
        try {
            sendMessage(cf, queue, creds.USER2.getuserid(), creds.USER2.getpassword());
        } catch (JMSRuntimeException e) {
            out.println("====== Unexpected failure when testing user2 ==========");
            out.print(e.toString());
            Assert.fail("Unexpected failure when testing user2");
        }
        try {
            receiveMessage(cf, queue, creds.USER2.getuserid(), creds.USER2.getpassword());
            out.println("====== Unexpected lack of failure when testing user2 ==========");
            Assert.fail("Unexpected lack of failure when testing user2");
        } catch (JMSRuntimeException e) {
            //expected
        }

        out.println("====== Testing user3 ==========");
        try {
            sendMessage(cf, queue, creds.USER3.getuserid(), creds.USER3.getpassword());
            out.println("====== Unexpected lack of failure when testing user3 ==========");
            Assert.fail("Unexpected lack of failure when testing user3");
        } catch (JMSRuntimeException e) {
            //expected
        }
        try {
            receiveMessage(cf, queue, creds.USER3.getuserid(), creds.USER3.getpassword());

        } catch (JMSRuntimeException e) {
            out.println("====== Unexpected failure when testing user3 ==========");
            out.print(e.toString());
            Assert.fail("Unexpected failure when testing user3");
        }

        out.println("\n\nDone");
        return;
    }

    InitialContext context = null;

    private ConnectionFactory getConnectionFactory() throws NamingException {

        ConnectionFactory cf = null;

        if (context == null) {
            out.println("Getting InitialContext");
            context = new InitialContext();
        }

        out.println("Looking up ConnectionFactory");
        Object lookup = context.lookup(connectionFactoryLookupName);

        out.println("Got: " + lookup);

        out.println("Casting to ConnectionFactory");
        cf = (ConnectionFactory) lookup;

        return cf;

    }

    private Queue getQueue() throws NamingException {

        Queue queue = null;

        if (context == null) {
            out.println("Getting InitialContext");
            context = new InitialContext();
        }

        out.println("Looking up Queue");
        Object lookup = context.lookup(queueLookupName);

        out.println("Got: " + lookup);

        out.println("Casting to Queue");
        queue = (Queue) lookup;

        return queue;

    }

    private void sendMessage(ConnectionFactory cf, Destination destination, String userid, String password) {

        out.println("Entering sendMessage");
        out.println("Creating JMSContext");
        JMSContext context = cf.createContext(userid, password);

        out.println("Creating JMSProducer");
        JMSProducer producer = context.createProducer();

        try {
            out.println("Sending Message");
            TextMessage message = context.createTextMessage("Test Message");
            producer.send(destination, message);
        } catch (Throwable t) {
            out.println("Exception thrown sending message: " + t.getMessage());
            throw t;
        }
    }

    private void receiveMessage(ConnectionFactory cf, Destination destination, String userid, String password) {

        out.println("Entering receiveMessage");
        out.println("Creating JMSContext");
        JMSContext context = cf.createContext(userid, password);

        try {
            out.println("Creating JMSConsumer");
            JMSConsumer consumer = context.createConsumer(destination);

            out.println("Receiving Message");
            Message receivedMessage = consumer.receive(100);

            out.println("Received:\n" + receivedMessage);
        } catch (Throwable t) {
            out.println("Exception thrown receiving message: " + t.getMessage());
            throw t;
        }

        out.println("Closing JMSContext");
        context.close();
    }

}
