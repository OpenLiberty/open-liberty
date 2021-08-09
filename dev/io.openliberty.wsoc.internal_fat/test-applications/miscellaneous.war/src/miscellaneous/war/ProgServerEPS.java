/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package miscellaneous.war;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.ByteBuffer;

import javax.websocket.CloseReason;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.SendHandler;
import javax.websocket.SendResult;
import javax.websocket.Session;

/**
 *
 */
public abstract class ProgServerEPS extends Endpoint {

    public static class BasicEndpoint extends ProgServerEPS {
        @Override
        public void onOpen(final Session session, EndpointConfig ec) {
            session.setMaxBinaryMessageBufferSize(100000000);
            session.setMaxTextMessageBufferSize(100000000);
            session.addMessageHandler(new MessageHandler.Whole<String>() {

                @Override
                public void onMessage(String text) {

                    try {
                        session.getBasicRemote().sendText(text);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }

            });

            session.addMessageHandler(new MessageHandler.Whole<ByteBuffer>() {

                @Override
                public void onMessage(ByteBuffer buffer) {

                    try {
                        session.getBasicRemote().sendBinary(buffer);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            });

        }
    }

    public static class AsyncTextEndpoint extends ProgServerEPS {
        volatile int echoHitCount = 0;
        volatile int resultHitCount = 0;

        @Override
        public void onOpen(final Session session, EndpointConfig ec) {
            echoHitCount = 0;
            resultHitCount = 0;

            session.setMaxBinaryMessageBufferSize(100000000);
            session.setMaxTextMessageBufferSize(100000000);
            session.addMessageHandler(new MessageHandler.Whole<String>() {

                @Override
                public void onMessage(String text) {

                    echoHitCount++;
                    int iterationCounter = 0;
                    int iterationCountLimit = 20; // five seconds

                    // if we enter here before we see the result for the last entry, we need to wait.  Can't do two async writes in a row.
                    // wait for 5 seconds for condition to be met, then just go on anyways.
                    while (echoHitCount - resultHitCount > 1) {
                        try {
                            iterationCounter++;
                            Thread.sleep(250);
                        } catch (InterruptedException e) {
                        }

                        if (iterationCounter == iterationCountLimit) {
                            return; // test will fail since we are not sending back data
                        }
                    }

                    session.getAsyncRemote().sendText(text, new SendHandler() {
                        @Override
                        public void onResult(SendResult result) {
                            resultHitCount++;
                            boolean ok = result.isOK();
                            if (!ok) {
                                // TODO - fail the test somehow?
                            }

                        }

                    });

                    session.addMessageHandler(new MessageHandler.Whole<ByteBuffer>() {

                        @Override
                        public void onMessage(ByteBuffer buffer) {

                            try {
                                session.getBasicRemote().sendBinary(buffer);
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                        }
                    });
                }

            });

        }
    }

    public static class Basic2Endpoint extends ProgServerEPS {
        @Override
        public void onOpen(final Session session, EndpointConfig ec) {
            session.setMaxBinaryMessageBufferSize(100000000);
            session.setMaxTextMessageBufferSize(100000000);
            session.addMessageHandler(new MessageHandler.Whole<Reader>() {

                @Override
                public void onMessage(Reader reader) {

                    int x;

                    try {
                        StringBuffer retValue = new StringBuffer();
                        try {
                            while ((x = reader.read()) >= 0) {
                                retValue.append((char) x);
                            }
                        } catch (IOException ie) {
                            ie.printStackTrace();
                        }

                        session.getBasicRemote().sendText(retValue.toString());
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            });

            session.addMessageHandler(new MessageHandler.Whole<InputStream>() {

                @Override
                public void onMessage(InputStream stream) {
                    try {
                        if (stream instanceof ByteArrayInputStream) {
                            ByteArrayInputStream ba = (ByteArrayInputStream) stream;
                            byte[] retData = new byte[ba.available()];
                            ba.read(retData);
                            session.getBasicRemote().sendBinary(ByteBuffer.wrap(retData));

                        }
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }

                }
            });
        }
    }

    public static class PartialEndpoint extends ProgServerEPS {
        @Override
        public void onOpen(final Session session, EndpointConfig ec) {
            session.setMaxBinaryMessageBufferSize(100000000);
            session.setMaxTextMessageBufferSize(100000000);
            session.addMessageHandler(new MessageHandler.Partial<String>() {

                @Override
                public void onMessage(String text, boolean isLast) {
                    try {
                        session.getBasicRemote().sendText(text, isLast);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }

            });
            session.addMessageHandler(new MessageHandler.Partial<byte[]>() {

                @Override
                public void onMessage(byte[] data, boolean isLast) {
                    try {
                        session.getBasicRemote().sendBinary(ByteBuffer.wrap(data), isLast);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }

            });
        }
    }

    @Override
    public void onClose(Session session, CloseReason reason) {

    }

    @Override
    public void onError(Session session, Throwable thr) {

    }

}
