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
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

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
@ServerEndpoint(value = "/session5Endpoint")
public class Session5ServerEP {

    Session onOpenSession = null;
    String onOpenId = "nothing yet";
    int testNumber = -1;
    String openErrorMsg = null;

    // give about 30 seconds to respond for each send.  if system delays, then this timing mechanism will dealy also.  If test
    // works as it should, then very little time will need to be taken, looping will rarely be more the 1, let alone 30.
    int LOOP_MAX = 30;

    static int onOpenNumber = -1;
    static CountDownLatch timingLatch1 = new CountDownLatch(2);
    static CountDownLatch timingLatch2 = new CountDownLatch(2);

    public static synchronized int incAndReturnOnOpenCount() {
        onOpenNumber++;
        return onOpenNumber;
    }

    @OnOpen
    public void onOpen(Session session, EndpointConfig ec) {
        onOpenSession = session;

        if (session != null) {
            if (session.getId() != null) {
                onOpenId = session.getId();
            } else {
                onOpenId = "null returned by session.getId()";
            }
            Map<String, Object> userMap = onOpenSession.getUserProperties();
            if (userMap != null) {
                userMap.put(Constants.ON_OPEN_ID, onOpenId);
            }
        }

        int sessionNumber = incAndReturnOnOpenCount();

        // session 3 should see sessions 1, 2 and 3 only.
        if (sessionNumber == 3) {
            int sessionsActiveNow = session.getOpenSessions().size();

            if (sessionsActiveNow != 3) {
                openErrorMsg = "SessionNumber 3 getOpenSession size was wrong.  size: " + sessionsActiveNow;
            }
        }
    }

    @OnMessage
    public void receiveMessage(String message, Session messageSession) {

        String messageToSend = null;
        String messageId = null;
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
                else if (message.indexOf(Constants.CLIENT_NUMBER + "2") != -1) {
                    testNumber = 2;
                }
                else if (message.indexOf(Constants.CLIENT_NUMBER + "3") != -1) {
                    testNumber = 3;
                }
                else if (message.indexOf(Constants.CLIENT_NUMBER + "4") != -1) {
                    testNumber = 4;
                }
            }
            messageToSend = " testNumber: " + testNumber + " timestamp of: " + System.currentTimeMillis() + " ";

            if (openErrorMsg != null) {
                messageSession.getBasicRemote().sendText("marker 1 " + openErrorMsg + Constants.FAILED);
                return;
            }

            messageId = messageSession.getId();

            // check that the passed in session id, is the same as the session id in the onOpen session
            if (messageId.compareTo(onOpenId) == 0) {
                messageToSend = messageToSend + "OnOpen and OnMessage Map ID Equal: " + onOpenId;
            } else {
                messageSession.getBasicRemote().sendText("marker 2 " + messageToSend + "OnOpen and OnMessage Not Equal. OnOpen: " + onOpenId + " messageID: " + messageId + " "
                                                         + Constants.FAILED);
                return;
            }

            // put that number into our user properties
            Map<String, Object> userMap = onOpenSession.getUserProperties();
            if (userMap != null) {
                userMap.put(Constants.USER_PROP_TEST_NUMBER, new Integer(testNumber));
            }

            // test 0 - 2 need to wait for all three session to appear in the open session list.
            if ((testNumber >= 0) && (testNumber <= 2)) {
                loopCount = 0;
                while (loopCount < LOOP_MAX) {
                    if (messageSession.getOpenSessions().size() == 3) {
                        if (onOpenSession.getOpenSessions().size() == 3) {
                            break;
                        }
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

                if (loopCount >= LOOP_MAX) {
                    // test failed on at least this session
                    messageSession.getBasicRemote().sendText("marker 3 " + messageToSend + " " + Constants.LATCH_DOWN + "  " + Constants.FAILED);
                    return;
                }

                // client 0 will close the connection, once connections 0-2 send back messages.
                // so server side connection 0 is complete and successful at this point
                if (testNumber == 0) {
                    messageSession.getBasicRemote().sendText("marker 4 " + messageToSend + "  " + Constants.LATCH_DOWN + "  " + Constants.SUCCESS);
                    return;
                } else {
                    // signal to the client that 3 sessions have been seen, by writing a message to the client without status yet.
                    messageSession.getBasicRemote().sendText("marker 5 " + messageToSend + " " + Constants.LATCH_DOWN + "  ");
                }

                // connections 1 and 2 should wait to see the session 0 leave the list.  to test the list shrinks correctly

                loopCount = 0;
                while (loopCount < LOOP_MAX) {

                    Set<Session> set = messageSession.getOpenSessions();

                    boolean foundMe = false;
                    boolean found0 = false;
                    // until set size equals 2, we could hit a race condition with session 0 moving out of the set
                    size = set.size();
                    if (size == 2) {
                        for (Session session : set) {
                            Map<String, Object> map = session.getUserProperties();
                            Integer sessionNumber = (Integer) map.get(Constants.USER_PROP_TEST_NUMBER);
                            // want to find ourselves, but not find session 0.  make no assumptions about the other session to avoid
                            // any false negative test failures.
                            if ((sessionNumber != null) && (sessionNumber.intValue() == 0)) {
                                found0 = true;
                            } else if ((sessionNumber != null) && (sessionNumber.intValue() == testNumber)) {
                                foundMe = true;
                            }
                        }
                    }

                    // want me, but not 0
                    if ((foundMe == true) && (found0 == false)) {
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

                // wait for both session 1 and 2 to leave above loop to avoid test code race conditions
                timingLatch1.countDown();
                try {
                    timingLatch1.await(LOOP_MAX * 1000 + 5000, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                }

                if (loopCount >= LOOP_MAX) {
                    messageSession.getBasicRemote().sendText("marker 6 " + messageToSend + "size: " + size + " " + Constants.FAILED + " " + Constants.LATCH_DOWN);
                    return;
                }

                messageSession.getBasicRemote().sendText("marker 7 " + messageToSend + "size: " + size + " " + Constants.LATCH_DOWN);

                // session 1 and 2 will now close when they see that session 3 has been added
                loopCount = 0;
                boolean done = false;
                while (loopCount < LOOP_MAX) {
                    HashSet<Session> set = (HashSet<Session>) messageSession.getOpenSessions();
                    size = set.size();
                    if (size == 3) {
                        for (Session session : set) {
                            Map<String, Object> map = session.getUserProperties();
                            Integer sessionNumber = (Integer) map.get(Constants.USER_PROP_TEST_NUMBER);
                            // session number 2 is the third session.
                            if ((sessionNumber != null) && (sessionNumber.intValue() == 2)) {
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
                    messageSession.getBasicRemote().sendText("marker 8 " + messageToSend + "size: " + size + " " +
                                                             Constants.FAILED + " " + Constants.LATCH_DOWN);
                    return;
                }

                // wait for both session 1 and 2 to leave above loop to avoid race condition
                timingLatch2.countDown();
                try {
                    timingLatch2.await(LOOP_MAX * 1000 + 5000, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                }

                // session 1 and 2 can send back success if they made it this far
                messageSession.getBasicRemote().sendText("marker 9 " + messageToSend + " " + Constants.SUCCESS + " " + Constants.LATCH_DOWN);

                return;
            }

            // when session 3 or 4 gets a message it should wait till there is only 1 session, verify it is itself, 
            // send back a success message and close
            if ((testNumber == 3) || (testNumber == 4)) {
                int numberNow = testNumber;
                loopCount = 0;

                if (testNumber == 3) {
                    // write a message so client can close 1 and 2
                    messageSession.getBasicRemote().sendText("marker 10 " + messageToSend + Constants.LATCH_DOWN);
                }

                while (loopCount < LOOP_MAX) {
                    // want to find ourselves, but no other sessions
                    HashSet<Session> set = (HashSet<Session>) messageSession.getOpenSessions();
                    size = set.size();
                    if (size == 1) {
                        Map<String, Object> map = messageSession.getUserProperties();
                        Integer sessionNumber = (Integer) map.get(Constants.USER_PROP_TEST_NUMBER);
                        if ((sessionNumber != null) && (sessionNumber.intValue() == numberNow)) {
                            break;
                        }
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
                    messageSession.getBasicRemote().sendText("marker 11 " + messageToSend + "size: " + size + " " + Constants.FAILED + " " + Constants.LATCH_DOWN);
                    return;
                }

                // session can send back success if they made it this far
                messageSession.getBasicRemote().sendText("marker 12 " + messageToSend + "   " + Constants.SUCCESS + " " + Constants.LATCH_DOWN);

            }

        } catch (IOException e) {
        }

    }

    @OnClose
    public void onClose(Session session, CloseReason reason) {

    }

    @OnError
    public void onError(Session session, Throwable t) {

    }

}
