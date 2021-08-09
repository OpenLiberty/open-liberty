/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package context.war;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;
import javax.jms.TopicPublisher;
import javax.jms.TopicSession;
import javax.jms.TopicSubscriber;
import javax.naming.InitialContext;
import javax.websocket.CloseReason;
import javax.websocket.EndpointConfig;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import io.openliberty.wsoc.common.Constants;

/**
 *
 */
// @ServerEndpoint(value = "/contextEndpoint", configurator = ContextServerEndpoint.ContextConfigurator.class)
@ServerEndpoint(value = "/contextEndpoint", configurator = ContextConfigurator.class)
public class ContextServerEP {

    ClassLoader tccl = null;
    TopicConnection topicConnection = null;
    TopicSession topicSession = null;

    int testNumber = -1;
    static String session0onCloseErrorMsg = "Error. onCLose not called as we thought it would be";

    static final String GOOD_ERROR_MESSAGE = "Inside onMessage";

    static CountDownLatch timingLatch1 = new CountDownLatch(2);
    static CountDownLatch timingLatch2 = new CountDownLatch(1);

    // give about 30 seconds to respond for each send.  if system delays, then this timing mechanism will dealy also.  If test
    // works as it should, then very little time will need to be taken, looping will rarely be more the 1, let alone 30.
    int LOOP_MAX = 30;

    String messageToSend = "initial value";

    static int openCount = 0;

//    public class Configurator extends ServerEndpointConfig.Configurator {
//        @Override
//        public void modifyHandshake(ServerEndpointConfig sec,
//                                    HandshakeRequest request,
//                                    HandshakeResponse response) {
//            tccl = Thread.currentThread().getContextClassLoader();
//
//            messageToSend = messageToSend + "...modifyHandShake see classloader of: " + tccl;
//        }
//    }

    @OnOpen
    public void onOpen(Session session, EndpointConfig ec) {

        tccl = ContextConfigurator.tccl;
        messageToSend = ContextConfigurator.message;

        ClassLoader cl = Thread.currentThread().getContextClassLoader();

        if (cl != tccl) {
            messageToSend = messageToSend + "..." + Constants.FAILED + " onOpen has different classloader: " + cl + " vs " + tccl;
        } else {
            messageToSend = messageToSend + "... onOpen classloader matches ok.";
        }

        // client staggers opens on a lock, so don't need to synchronize
        if (openCount == 0) {
            openCount++;
            try {
                this.jmsCreateTopicConnection();
                messageToSend = messageToSend + "... jms jndi checked out ok, componentMetaData ok ..." + Constants.SUCCESS;
            } catch (Exception x) {
                messageToSend = messageToSend + "... jms jndi usage got an execption: " + x + "..." + Constants.FAILED;
            }
        }

    }

    @OnMessage
    public void receiveMessage(String message, Session messageSession) throws Exception {

        int loopCount = 0;
        int size = 0;

        try {
            if (testNumber == -1) {
                // match our endpoint number to the client number
                if (message.indexOf(Constants.CLIENT_NUMBER + "0") != -1) {
                    testNumber = 0;
                }
                else if (message.indexOf(Constants.CLIENT_NUMBER + "1") != -1) {
                    testNumber = 1;
                }
            }

            messageToSend = messageToSend + "...testNumber: " + testNumber + " timestamp of: " + System.currentTimeMillis() + " ";

            // put that number into our user properties
            Map<String, Object> userMap = messageSession.getUserProperties();
            if (userMap != null) {
                userMap.put(Constants.USER_PROP_TEST_NUMBER, new Integer(testNumber));
            }

            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            if (cl != tccl) {
                messageToSend = messageToSend + "..." + Constants.FAILED + " onMessage has different classloader: " + cl + " vs " + tccl;
            } else {
                messageToSend = messageToSend + "... onMessage classloader matches ok.";
            }

            // connections 0 and 1 need to wait for both sessions to appear in the open session list.
            if ((testNumber == 0) || (testNumber == 1)) {
                loopCount = 0;
                while (loopCount < LOOP_MAX) {
                    if (messageSession.getOpenSessions().size() == 2) {
                        break;
                    }

                    // sleep one second and try again
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException x) {
                        // if we can't even sleep, let's just leave, but not mark the test as bad yet.
                        break;
                    }

                    loopCount++;
                }

                // both sessions can send back a message, then wait to make sure that both are ready to move on to the next step
                if (loopCount >= LOOP_MAX) {
                    messageSession.getBasicRemote().sendText("marker 1 " + messageToSend + "size: " + size + " " + Constants.FAILED + " " + Constants.LATCH_DOWN);
                } else {
                    if (testNumber == 0) {
                        try {
                            this.jmsStartTopicConnection();
                            messageToSend = messageToSend + "... jms jndi checked out ok, componentMetaData ok ..." + Constants.SUCCESS;
                        } catch (Exception x) {
                            messageToSend = messageToSend + "... jms jndi usage got an execption: " + x + "..." + Constants.FAILED;
                        }

                        // the results from testNumber 0 onClose test will be reported by testNumber 1.  So testNumber 0 is a success at this point
                        messageSession.getBasicRemote().sendText("marker 2 " + messageToSend + "..." + Constants.LATCH_DOWN);
                    } else {
                        messageSession.getBasicRemote().sendText("marker 3 " + messageToSend + "size: " + size + " " + Constants.LATCH_DOWN);
                    }
                }

                // wait for both session 1 and 2 to get here to avoid test code race conditions
                timingLatch1.countDown();
                try {
                    timingLatch1.await(LOOP_MAX * 1000 + 5000, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                }

                if (loopCount >= LOOP_MAX) {
                    return;
                }

                if (testNumber == 1) {

                    try {
                        // make sure session 0 has run through onClose
                        timingLatch2.await(LOOP_MAX * 1000 + 5000, TimeUnit.MILLISECONDS);
                    } catch (InterruptedException e) {
                    }

                    // session 1 should become the only session once session 0 exits.
                    loopCount = 0;
                    boolean done = false;
                    while (loopCount < LOOP_MAX) {
                        HashSet<Session> set = (HashSet<Session>) messageSession.getOpenSessions();
                        size = set.size();
                        if (size == 1) {
                            for (Session session : set) {
                                Map<String, Object> map = session.getUserProperties();
                                Integer sessionNumber = (Integer) map.get(Constants.USER_PROP_TEST_NUMBER);
                                if ((sessionNumber != null) && (sessionNumber.intValue() == 1)) {
                                    done = true;
                                    break;
                                }
                            }
                        }
                        if (done == true) {
                            break;
                        }
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException x) {
                            // if we can't even sleep, let's just leave, but not mark the test as bad yet.
                            break;
                        }
                        loopCount++;
                    }

                    if (loopCount >= LOOP_MAX) {
                        messageSession.getBasicRemote().sendText("marker 4 " + messageToSend + "size: " + size + " " +
                                                                 Constants.FAILED + " " + Constants.LATCH_DOWN);
                        return;
                    }

                    // Check to see if session 0 was successful with testing the onClose session stuff
                    if (session0onCloseErrorMsg == null) {

                        messageToSend = messageToSend + "...session 1 saw session 0 onClose classloader matches ok., so good so far, testing onError next";
                        // throwing an error from on onMessage routine, should cause onError to be called by the WebSocket API code
                        Exception x = new Exception(messageToSend + "..." + GOOD_ERROR_MESSAGE);
                        throw x;

                    } else {
                        messageSession.getBasicRemote().sendText("marker 6 " + messageToSend + " session 0 onClose error message: "
                                                                 + session0onCloseErrorMsg + " " +
                                                                 Constants.FAILED + " " + Constants.LATCH_DOWN);
                    }
                }
            }
        } catch (IOException e) {
        }

    }

    @OnClose
    public void onClose(Session closeSession, CloseReason reason) {

        if (testNumber == 0) {
            session0onCloseErrorMsg = null;

            if (closeSession == null) {
                session0onCloseErrorMsg = "Error.  session parameter on close was null";
            } else {
                ClassLoader cl = Thread.currentThread().getContextClassLoader();
                if (cl != tccl) {
                    session0onCloseErrorMsg = "..." + Constants.FAILED + " Sesssion 0 onClose has different classloader: " + cl;
                } else {
                    try {
                        this.jmsCreateTopic();
                    } catch (Exception x) {
                        session0onCloseErrorMsg = session0onCloseErrorMsg + "... jms jndi usage got an execption: " + x + "..." + Constants.FAILED;
                    }

                }
            }

            timingLatch2.countDown();
        }

        return;
    }

    @OnError
    public void onError(Session errorSession, java.lang.Throwable throwable) {

        String ourMsg = null;

        if (throwable.getCause() != null) {
            ourMsg = throwable.getCause().getMessage();
        } else {
            ourMsg = throwable.getMessage();
        }

        if (testNumber == 1) {
            try {

                if ((ourMsg != null) && (ourMsg.indexOf(GOOD_ERROR_MESSAGE) != -1)) {

                    ClassLoader cl = Thread.currentThread().getContextClassLoader();
                    if (cl != tccl) {
                        ourMsg = ourMsg + " onError has different classloader: " + cl;
                    } else {
                        // Success.  Just use the errorSession session now to send back a success message
                        ourMsg = ourMsg + "... onError classloader matches ok.";
                        errorSession.getBasicRemote().sendText("marker 7 " + ourMsg + "..." + Constants.SUCCESS + " " + Constants.LATCH_DOWN);
                        return;
                    }
                } else {
                    if (ourMsg == null) {
                        ourMsg = "...No message passed in throwable.";
                    } else {
                        ourMsg = ourMsg + "...didn't find the right error message in onError";
                    }
                }

                errorSession.getBasicRemote().sendText("marker 8 " + ourMsg + "..." + Constants.FAILED + " " + Constants.LATCH_DOWN);

            } catch (IOException e) {
            }
        }

    }

    private void jmsCreateTopicConnection() throws Exception {
        // jms calls will test that the component metadata got pushed onto the current thread correctly
        // create a topic connection factory
        TopicConnectionFactory cf1 = (TopicConnectionFactory) new InitialContext().lookup("java:comp/env/jmsTCF");
        // create topic connection
        topicConnection = cf1.createTopicConnection();
    }

    private void jmsStartTopicConnection() throws Exception {
        topicConnection.start();
        topicSession = topicConnection.createTopicSession(false, javax.jms.Session.AUTO_ACKNOWLEDGE);
    }

    private void jmsCreateTopic() throws Exception {

        // Lookup topic from JNDI
        Topic topic = (Topic) new InitialContext().lookup("java:comp/env/jmsTopic");

        // create a NON-Durable subscriber
        TopicSubscriber sub = topicSession.createSubscriber(topic);

        // create a topic publisher
        TopicPublisher publisher = topicSession.createPublisher(topic);
    }

}
