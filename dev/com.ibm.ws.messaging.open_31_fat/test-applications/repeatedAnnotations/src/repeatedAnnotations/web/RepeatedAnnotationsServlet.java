/*******************************************************************************
 * Copyright (c) 2017, 2022 IBM Corporation and others.
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
package repeatedAnnotations.web;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.junit.Test;

import componenttest.app.FATServlet;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import jakarta.ejb.EJB;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.inject.Inject;
import jakarta.jms.JMSConnectionFactory;
import jakarta.jms.JMSConsumer;
import jakarta.jms.JMSContext;
import jakarta.jms.JMSException;
import jakarta.jms.Queue;
import jakarta.jms.TextMessage;
import jakarta.jms.Topic;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import repeatedAnnotations.ejb.RepeatedAnnotationsBean;

@SuppressWarnings("serial")
@WebServlet("/RepeatedAnnotatonsServlet")
public class RepeatedAnnotationsServlet extends FATServlet {

    /** @return the methodName of the caller. */
    private final static String methodName() {
        return new Exception().getStackTrace()[1].getMethodName();
    }

    private final class TestException extends Exception {
        TestException(String message) {
            super(timeStamp() + " " + message);
        }
    }

    // The current time, formatted with millisecond resolution.
    private static final SimpleDateFormat timeStampFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS Z");

    private static final String timeStamp() {
        return timeStampFormat.format(new Date());
    }

    @Inject
    @JMSConnectionFactory("localQueueConnectionFactory")
    private JMSContext jmsContextQueue;

    @Inject
    @JMSConnectionFactory("localTopicConnectionFactory")
    private JMSContext jmsContextTopic;

    @EJB
    RepeatedAnnotationsBean repeatedAnnotationsBean;

    @Test
    @Mode(TestMode.FULL)
    //@SkipForRepeat({ NO_MODIFICATION, EE8_FEATURES, EE9_FEATURES })
    @TransactionAttribute(value = TransactionAttributeType.REQUIRED)
    public void testRepeatedAnnotations(HttpServletRequest request, HttpServletResponse response) throws JMSException, IOException, TestException, NamingException {

        response.getWriter().println("Running test method " + methodName());
        if (repeatedAnnotationsBean == null) {
            throw new TestException("Failed injection of repeatedAnnotationsBean EJB");
        }

        String sentQueueMessageText = methodName() + " QUEUE at " + timeStamp();
        repeatedAnnotationsBean.sendQueueMessage(sentQueueMessageText);

        Queue jmsQueue = (Queue) new InitialContext().lookup("Queue1");
        try (JMSConsumer jmsConsumer = jmsContextQueue.createConsumer(jmsQueue)) {
            TextMessage receivedMessage = (TextMessage) jmsConsumer.receive(500);
            if (receivedMessage == null)
                throw new TestException("No message received, sentQueueMessageText:" + sentQueueMessageText);
            if (!receivedMessage.getText().equals(sentQueueMessageText))
                throw new TestException("Wrong message received, receivedMessage:" + receivedMessage + " sentQueueMessageText:" + sentQueueMessageText);
        }

        String sentTopicMessageText = methodName() + " TOPIC at " + timeStamp();

        Topic jmsTopic = (Topic) new InitialContext().lookup("Topic1");
        try (JMSConsumer jmsConsumer = jmsContextTopic.createConsumer(jmsTopic)) {
            // Now that the subscription exists, publish a message.
            repeatedAnnotationsBean.sendTopicMessage(sentTopicMessageText);
            TextMessage receivedMessage = (TextMessage) jmsConsumer.receive(500);
            if (receivedMessage == null)
                throw new TestException("No message received, sentTopicMessageText:" + sentTopicMessageText);
            if (!receivedMessage.getText().equals(sentTopicMessageText))
                throw new TestException("Wrong message received, receivedMessage:" + receivedMessage + " sentTopicMessageText:" + sentTopicMessageText);
        }
    }

}
