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
package com.ibm.ws.webcontainer.servlet_31_fat.libertyreadwritelistenertest.war.upgradeHandler;

import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpUpgradeHandler;
import javax.servlet.http.WebConnection;

/*The Upgrade servlet calls the handler, which in turn sets Read or writeListeners and calls them,
 * or executes actions without the read or write listeners, depending on the test called.
 */

public class TestHttpUpgradeHandler implements HttpUpgradeHandler {

    HttpServletRequest request = null;
    HttpServletResponse response = null;
    private static final Logger LOG = Logger.getLogger(TestHttpUpgradeHandler.class.getName());
    private static String TestSet = "";
    private static String ContentSize = "";
    private final LinkedBlockingQueue<String> q = new LinkedBlockingQueue<String>();

    /*
     * upgrade handler, initiates a webconnection which can be
     * passed to read or write Listeners(non-Javadoc)
     *
     * @see javax.servlet.http.HttpUpgradeHandler#init(javax.servlet.http.WebConnection)
     */
    @Override
    public void init(WebConnection wc) {
        // call appropriate code based on test call.
        if (getTestSet().contains("Read")) {
            try {
                setReadListenerTests(wc);
            } catch (Exception e) {

                e.printStackTrace();
            }
        } else {
            try {
                setWriteListenerTests(wc);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * @param wc
     */
    private void setWriteListenerTests(WebConnection wc) throws Exception {
        LOG.info("setWriteListenerTests ENTER, WebConnection -> [" + wc + "]");

        ServletOutputStream out = null;
        try {
            out = wc.getOutputStream();
        } catch (IOException e) {
            LOG.info("exception in TestHttpUpgradeHandler while getting OutputStream . exception is " + e.toString() + " printing stack trace ... ");
            e.printStackTrace();
            return;
        }

        if (getTestSet().equals("test_SmallData_UpgradeWL")) {
            LOG.info("\n [RUNNING TEST|test_SmallData_UpgradeWL| In init method of TestHttpUpgradeHandler");
            try {
                //test_SmallData_UpgradeWL in TestUpgradeReadListener adding string to queue
                q.add("numbers");
                //setting writeListener
            } catch (Exception e) {
                LOG.info("exception in testHttpUpgradeHandler in test:test_SmallData_UpgradeWL . exception is " + e.toString() + " printing stack trace ... ");
                e.printStackTrace();
                throw e;
            }
            WriteListener writeListener = new TestUpgradeWriteListener(out, q, wc, "test_SmallData_UpgradeWL");
            out.setWriteListener(writeListener);
        }
        else if (getTestSet().equals("test_SmallDataInHandler_NoWriteListener")) {
            LOG.info("\n RUNNING TEST|test_SmallDataInHandler_NoWriteListener| In init method of TestHttpUpgradeHandler");
            try {
                //printing out the numbers in test_SmallDataInHandler_NoWriteListener
                out.println("0123456789");
            } catch (IOException e) {
                LOG.info("EXCEPTION in printing out data in TestHttpUpgradeHandler for test_SmallDataInHandler_NoWriteListener in init method, exception is : "
                         + e.toString() + " Printing stack trace ..");
                e.printStackTrace();
                throw e;
            }
            try {
                LOG.info("closing web connection in test_SmallDataInHandler_NoWriteListener ");
                if (out.isReady()) {
                    wc.close();
                }
                LOG.info(" finished closing web connection in test_SmallDataInHandler_NoWriteListener ");
            } catch (Exception e) {
                LOG.info("EXCEPTION in closing web connection in TestHttpUpgradeHandler for test_SmallDataInHandler_NoWriteListener in init method, exception is : "
                         + e.toString() + " Printing stack trace ..");
                e.printStackTrace();
                throw e;
            }

        }
        else if (getTestSet().equals("test_Close_WebConnection_Container_UpgradeWL")) {
            try {

                LOG.info("\n [RUNNING TEST|test_Close_WebConnection_Container_UpgradeWL| In init method of TestHttpUpgradeHandler");
                //In TestHttpUpgradeHandler [test : test_Close_WebConnection_Container_UpgradeWL] adding string to queue"
                q.add("numbers");
            } catch (Exception e) {
                LOG.info("exception in testHttpUpgradeHandler while adding data to Queue:test_Close_WebConnection_Container_UpgradeWL . exception is " + e.toString()
                         + " printing stack trace ... ");
                e.printStackTrace();
                throw e;
            }
            //setting writeListener
            WriteListener writeListener = new TestUpgradeWriteListener(out, q, wc, "test_Close_WebConnection_Container_UpgradeWL");
            out.setWriteListener(writeListener);
            LOG.info("In TestHttpUpgradeHandler [test : test_Close_WebConnection_Container_UpgradeWL] Handler thread is now going to sleep for 60 milliseconds");
            Thread.sleep(60);
            LOG.info("In TestHttpUpgradeHandler [test : test_Close_WebConnection_Container_UpgradeWL] Handler thread has woken up after sleep ");
        }
        else if (getTestSet().equals("test_SingleWriteLargeData1000000__UpgradeWL")) {
            try {

                LOG.info("\n RUNNING TEST|test_SingleWriteLargeData1000000__UpgradeWL| In init method of TestHttpUpgradeHandler");
                String postDataSize = ContentSize;
                StringBuilder sb = new StringBuilder();
                LOG.info("In TestHttpUpgradeHandler UpgradeHandler postDataSize is for test_SingleWriteLargeData1000000__UpgradeWL " + postDataSize);
                if (postDataSize != null) {
                    byte[] b = new byte[10000];
                    for (int i = 0; i < b.length; i++) {
                        b[i] = (byte) 0x61; // building a byte buffer which will be used to build a large data set for the queue, that will be passed on to the writeListener
                    }
                    // build a data set of specified size and put it in a string builder.
                    int postDataSizeInt = Integer.parseInt(postDataSize);
                    int total = 0;
                    while (total < postDataSizeInt) {
                        if ((postDataSizeInt - total) < b.length) {
                            sb.append(new String(b, 0, postDataSizeInt - total));
                            total = postDataSizeInt;
                        } else {
                            sb.append(new String(b));
                            total += b.length;
                        }
                    }

                }
                sb.trimToSize();

                try {
                    q.put(sb.toString()); // put large data chunk in queue.
                } catch (InterruptedException e) {
                    LOG.info("EXCEPTION when adding large data chunk to queue in TestHttpUpgradeHandler for test_SingleWriteLargeData1000000__UpgradeWL in init method, exception is : "
                             + e.toString() + " Printing stack trace ..");
                    e.printStackTrace();
                    throw e;
                }
                LOG.info("queue size, the number of elements, is now " + q.size());
                LOG.info("queue length is " + q.toString().length());

            } catch (Exception e) {
                LOG.info("exception in testHttpUpgradeHandler in test:test_SingleWriteLargeData1000000__UpgradeWL . exception is " + e.toString() + " printing stack trace ... ");
                e.printStackTrace();
                throw e;
            }

            //setting writeListener.
            WriteListener writeListener = new TestUpgradeWriteListener(out, q, wc, "test_SingleWriteLargeData1000000__UpgradeWL");
            out.setWriteListener(writeListener);

        } 
        else if (getTestSet().equals("TestWrite_DontCheckisRedy_fromUpgradeWL")
                   || getTestSet().equals("TestWriteFromHandler_AftersetWL")
                   || getTestSet().equals("TestUpgrade_ISE_setSecondWriteListener")
                   || getTestSet().equals("TestUpgrade_NPE_setNullWriteListener")) {

            String testToCall = getTestSet();
            try {

                LOG.info("\n RUNNING TEST|" + testToCall + "| In init method of TestHttpUpgradeHandler");
                String postDataSize = ContentSize;
                StringBuilder sb = new StringBuilder();
                LOG.info(testToCall + " , postDataSize" + postDataSize);
                if (postDataSize != null) {
                    byte[] b = new byte[10000];
                    for (int i = 0; i < b.length; i++) {
                        b[i] = (byte) 0x61; // building a byte buffer which will be used to build a large data set for the queue, that will be passed on to the writeListener
                    }
                    // build a data set of specified size and put it in a string builder.
                    int postDataSizeInt = Integer.parseInt(postDataSize);
                    int total = 0;
                    while (total < postDataSizeInt) {
                        if ((postDataSizeInt - total) < b.length) {
                            sb.append(new String(b, 0, postDataSizeInt - total));
                            total = postDataSizeInt;
                        } else {
                            sb.append(new String(b));
                            total += b.length;
                        }
                    }

                }
                sb.trimToSize();

                try {
                    q.put(sb.toString()); // put large data chunk in queue.
                } catch (InterruptedException e) {
                    LOG.info("EXCEPTION when adding large data chunk to queue in TestHttpUpgradeHandler for" + testToCall + "in init method, exception is : "
                             + e.toString() + " Printing stack trace ..");
                    e.printStackTrace();
                    throw e;
                }
                LOG.info("queue size, the number of elements, is now " + q.size());
                LOG.info("queue length is " + q.toString().length());

            } catch (Exception e) {
                LOG.info("exception in testHttpUpgradeHandler in test:" + testToCall + ". exception is " + e.toString() + " printing stack trace ... ");
                e.printStackTrace();
                throw e;
            }

            if (getTestSet().equals("TestUpgrade_NPE_setNullWriteListener")) {
                LOG.info(testToCall + " set WriteListener to null ");
                out.setWriteListener(null);
            }
            //setting writeListener.
            WriteListener writeListener = new TestUpgradeWriteListener(out, q, wc, testToCall);
            out.setWriteListener(writeListener);

            if (getTestSet().equals("TestUpgrade_ISE_setSecondWriteListener")) {
                //setting writeListener.
                WriteListener writeListener2 = new TestUpgradeWriteListener(out, q, wc, testToCall);
                out.setWriteListener(writeListener2);
            }

            if (getTestSet().equals("TestWriteFromHandler_AftersetWL")) {
//                if (out.isReady()) {
//                    out.print(false);
//                }
//                else {
//                    LOG.info(testToCall + " TAWS: isReady always false from the thread which sets the WL");
//                }
                // The following shud log Error in logs
                //Expected output :  SRVE0918E: The attempted blocking write is not allowed because the non-blocking I/O has already been started.
                LOG.info(testToCall + " TAWS: isReady not checked Printing output again from this thread , it shud not be allowed");
                out.print(false);

            }

        } 
        else if (getTestSet().equals("test_LargeDataInChunks_UpgradeWL")){ //test for largeDataSizes in multiple chunks in queue, with writeListener going async
            try {
                LOG.info("\n RUNNING TEST|test_LargeDataInChunks_UpgradeWL| In init method of TestHttpUpgradeHandler");
                String postDataSize = ContentSize;
                StringBuilder sb = new StringBuilder();
                LOG.info("In TestHttpUpgradeHandler UpgradeHandler postDataSize is for test_LargeDataInChunks_UpgradeWL " + postDataSize);
                if (postDataSize != null) {
                    byte[] b = new byte[10000];
                    for (int i = 0; i < b.length; i++) {
                        b[i] = (byte) 0x61; // building a byte buffer which will be used to build a large data set for the queue, that will be passed on to the writeListener
                    }
                    int postDataSizeInt = Integer.parseInt(postDataSize);
                    LOG.info("TestHttpUpgradeHandler for test_LargeDataInChunks_UpgradeWL postDataSizeInt is " + postDataSizeInt);
                    int total = 0;
                    while (total < postDataSizeInt) {
                        if ((postDataSizeInt - total) < b.length) {
                            LOG.info("postDataSizeInt -total is " + (postDataSizeInt - total) + " b.length is " + b.length);
                            sb.append(new String(b, 0, postDataSizeInt - total));
                            try {
                                q.put(sb.toString()); // put small chunk, one of many, into queue to be sent to the writeListener

                            } catch (Exception e) {
                                LOG.info("Error in putting elements in queue in test_LargeDataInChunks_UpgradeWL in TestHttpUpgradeHandler  ");
                                e.printStackTrace();
                            }
                            sb.delete(0, postDataSizeInt - total); //clear temporary buffer, used to append to the queue
                            total = postDataSizeInt;
                        } else {
                            sb.append(new String(b));
                            try {
                                q.put(sb.toString()); // put small chunk, one of many, into queue to be sent to the writeListener

                            } catch (Exception e) {
                                LOG.info("Error in putting elements in queue in test_LargeDataInChunks_UpgradeWL in TestHttpUpgradeHandler  ");
                                e.printStackTrace();
                            }
                            sb.delete(0, b.length); //clear temporary buffer, used to append to the queue
                            total += b.length;

                        }
                    }

                }
                LOG.info("queue size, the number of elements, is now " + q.size());
                LOG.info("queue length is " + q.toString().length());
            } catch (Exception e) {
                LOG.info("exception in testHttpUpgradeHandler in test:test_LargeDataInChunks_UpgradeWL . exception is " + e.toString() + " printing stack trace ... ");
                e.printStackTrace();
            }
            //setting writeListener.
            WriteListener writeListener = new TestUpgradeWriteListener(out, q, wc, "test_LargeDataInChunks_UpgradeWL");
            out.setWriteListener(writeListener);
        }
        else if (getTestSet().equals("test_Timeout_UpgradeWL")) {
            try {
                LOG.info("\n RUNNING TEST| test_Timeout_UpgradeWL | In init method of TestHttpUpgradeHandler");
                String postDataSize = "1000000";
                StringBuilder sb = new StringBuilder();
                LOG.info("In TestHttpUpgradeHandler UpgradeHandler postDataSize is for test_LargeDataInChunks_UpgradeWL " + postDataSize);
                if (postDataSize != null) {
                    byte[] b = new byte[10000];
                    for (int i = 0; i < b.length; i++) {
                        b[i] = (byte) 0x61; // building a byte buffer which will be used to build a large data set for the queue, that will be passed on to the writeListener
                    }
                    int postDataSizeInt = Integer.parseInt(postDataSize);
                    LOG.info("TestHttpUpgradeHandler for test_Timeout_UpgradeWL postDataSizeInt is " + postDataSizeInt);
                    int total = 0;
                    while (total < postDataSizeInt) {
                        if ((postDataSizeInt - total) < b.length) {
                            LOG.info("postDataSizeInt -total is " + (postDataSizeInt - total) + " b.length is " + b.length);
                            sb.append(new String(b, 0, postDataSizeInt - total));
                            try {
                                q.put(sb.toString()); // put small chunk, one of many, into queue to be sent to the writeListener

                            } catch (Exception e) {
                                LOG.info("Error in putting elements in queue in test_Timeout_UpgradeWL in TestHttpUpgradeHandler  ");
                                e.printStackTrace();
                            }
                            sb.delete(0, postDataSizeInt - total); //clear temporary buffer, used to append to the queue
                            total = postDataSizeInt;
                        } else {
                            sb.append(new String(b));
                            try {
                                q.put(sb.toString()); // put small chunk, one of many, into queue to be sent to the writeListener

                            } catch (Exception e) {
                                LOG.info("Error in putting elements in queue in test_Timeout_UpgradeWL in TestHttpUpgradeHandler  ");
                                e.printStackTrace();
                            }
                            sb.delete(0, b.length); //clear temporary buffer, used to append to the queue
                            total += b.length;

                        }
                    }

                }
                LOG.info("queue size, the number of elements, is now " + q.size());
                LOG.info("queue length is " + q.toString().length());
            } catch (Exception e) {
                LOG.info("exception in testHttpUpgradeHandler in test_Timeout_UpgradeWL . exception is " + e.toString() + " printing stack trace ... ");
                e.printStackTrace();
            }
            //setting writeListener.
            WriteListener writeListener = new TestUpgradeWriteListener(out, q, wc, "test_Timeout_UpgradeWL");
            out.setWriteListener(writeListener);
        }
        else if (getTestSet().equals("test_ContextTransferProperly_UpgradeWL")) {
            try {
                LOG.info("\n [RUNNING TEST | test_ContextTransferProperly_UpgradeWL | In init method of TestHttpUpgradeHandler");
                //setting writeListener
                WriteListener writeListener = new TestUpgradeWriteListener(out, q, wc, "test_ContextTransferProperly_UpgradeWL");
                out.setWriteListener(writeListener);
            } catch (Exception e) {
                LOG.info("exception in testHttpUpgradeHandler while adding data to test_ContextTransferProperly_UpgradeWL . exception is " + e.toString()
                         + " printing stack trace ... ");
                e.printStackTrace();
                throw e;
            }

            LOG.info("In TestHttpUpgradeHandler [test : test_ContextTransferProperly_UpgradeWL] completed setting the WriteListener");
        }

        LOG.info("setWriteListenerTests EXIT");
    }

    /**
     * @param wc
     * @throws Exception
     */
    private void setReadListenerTests(WebConnection wc) throws Exception {
        LOG.info("setReadListenerTests ENTER, WebConnection -> [" + wc + "]");

        if (getTestSet().equals("testUpgradeReadListenerSmallData")) {
            ServletInputStream input = null;
            ServletOutputStream output = null;
            try {

                LOG.info("\n [RUNNING TEST | testUpgradeReadListenerSmallData| In init method of TestHttpUpgradeHandler");
                input = wc.getInputStream();
                output = wc.getOutputStream();
                //Inside  TestHttpUpgradeHandler, calling ReadListener in testUpgradeReadListenerSmallData

                //In TestHttpUpgradeHandler, finished calling ReadListener for testUpgradeReadListenerSmallData
            } catch (Exception e) {
                LOG.info("exception in testHttpUpgradeHandler in test:testUpgradeReadListenerSmallData . exception is " + e.toString() + " printing stack trace ... ");
                e.printStackTrace();
                throw e;
            }

            //setting readListener
            ReadListener readListener = new TestUpgradeReadListener(input, wc, output, "testUpgradeReadListenerSmallData");
            input.setReadListener(readListener);

        }
        else if (getTestSet().equals("testUpgradeReadSmallDataInHandler")) {
            try {

                LOG.info("\n [RUNNING TEST |testUpgradeReadSmallDataInHandler| In init method of TestHttpUpgradeHandler");
                ServletInputStream input = wc.getInputStream();
                byte b[] = new byte[1024];
                String stringToSend = "";
                int len = input.read(b);
                String s = new String(b);
                String s1 = s.substring(0, 35);
                LOG.info("In TestHttpUpgradeHandler, test: testUpgradeReadSmallDataInHandler, I read : " + len + " bytes, this is what it said : \"" + s1 + "\"");
                if (s1.equals("123456789abcdefghijklmnopqrstuvwxyz")) {
                    stringToSend = "OK";
                } else {
                    stringToSend = "NOT OK";
                }
                ServletOutputStream out = wc.getOutputStream();
                LOG.info("TestHttpUpgradeHandler,test:  testUpgradeReadSmallDataInHandler, Now writing out : \"" + stringToSend + "\"");
                out.println(stringToSend);
                out.flush();
                try {
                    LOG.info("closing web connection in testUpgradeReadSmallDataInHandler in TestUpgradeReadListener ");
                    wc.close();
                    LOG.info("closed webConnection in testUpgradeReadSmallDataInHandler in TestUpgradeReadListener ");
                } catch (Exception e) {
                    LOG.info("EXCEPTION in closing web connection in TestHttpUpgradeHandler for testUpgradeReadSmallDataInHandler in init method, exception is : "
                             + e.toString() + " Printing stack trace ..");
                    e.printStackTrace();
                    throw e;
                }
            } catch (Exception e) {
                LOG.info("exception in testHttpUpgradeHandler in test:testUpgradeReadSmallDataInHandler . exception is " + e.toString() + " printing stack trace ... ");
                e.printStackTrace();
                throw e;
            }

        }
        else if (getTestSet().equals("testUpgradeReadListenerLargeData")) {
            ServletInputStream input = null;
            ServletOutputStream output = null;
            try {
                LOG.info("\n RUNNING TEST |testUpgradeReadListenerLargeData| In init method of TestHttpUpgradeHandler");
                input = wc.getInputStream();
                output = wc.getOutputStream();
                //Inside  TestHttpUpgradeHandler calling ReadListener in testUpgradeReadListenerLargeData
                //setting readListener.

            } catch (Exception e) {
                LOG.info("exception in testHttpUpgradeHandler in test:testUpgradeReadListenerLargeData . exception is " + e.toString() + " printing stack trace ... ");
                e.printStackTrace();
                throw e;
            }
            ReadListener readListener = new TestUpgradeReadListener(input, wc, output, "testUpgradeReadListenerLargeData");
            input.setReadListener(readListener);

        } 
        else if (getTestSet().equals("testUpgradeReadListenerTimeout")) {
            ServletInputStream input = null;
            ServletOutputStream output = null;

            try {
                LOG.info("\n RUNNING TEST | testUpgradeReadListenerTimeout | In init method of TestHttpUpgradeHandler");
                input = wc.getInputStream();
                output = wc.getOutputStream();
            } catch (Exception e) {
                LOG.info("exception in testHttpUpgradeHandler in test:testUpgradeReadListenerLargeData . exception is " + e.toString() + " printing stack trace ... ");
                e.printStackTrace();
                throw e;
            }

            ReadListener readListener = new TestUpgradeReadListener(input, wc, output, "testUpgradeReadListenerTimeout");
            input.setReadListener(readListener);
            LOG.info("Finished with the testUpgradeReadListenerTimeout TestHttpUpgradeHandler init");

        } 
        else if (getTestSet().equals("testRead_ContextTransferProperly_WhenUpgradeRLSet")) {
            ServletInputStream input = null;
            ServletOutputStream output = null;

            try {
                LOG.info("\n RUNNING TEST | testRead_ContextTransferProperly_WhenUpgradeRLSet | In init method of TestHttpUpgradeHandler");
                input = wc.getInputStream();
                output = wc.getOutputStream();
                ReadListener readListener = new TestUpgradeReadListener(input, wc, output, "testRead_ContextTransferProperly_WhenUpgradeRLSet");
                input.setReadListener(readListener);
            } catch (Exception e) {
                LOG.info("exception in testHttpUpgradeHandler in test:testRead_ContextTransferProperly_WhenUpgradeRLSet . exception is " + e.toString()
                         + " printing stack trace ... ");
                e.printStackTrace();
                throw e;
            }
            LOG.info("Finished with the testRead_ContextTransferProperly_WhenUpgradeRLSet TestHttpUpgradeHandler init");
        }
        else if (getTestSet().equals("testUpgrade_WriteListener_From_ReadListener")) {
            ServletInputStream input = null;
            ServletOutputStream output = null;
            try {

                LOG.info("\n [RUNNING TEST | testUpgrade_WriteListener_From_ReadListener | In init method of TestHttpUpgradeHandler");
                input = wc.getInputStream();
            } catch (Exception e) {
                LOG.info("exception in testHttpUpgradeHandler in testUpgrade_WriteListener_From_ReadListener . exception is " + e.toString() + " printing stack trace ... ");
                e.printStackTrace();
                throw e;
            }

            //setting readListener
            ReadListener readListener = new TestUpgradeReadListener(input, wc, output, "testUpgrade_WriteListener_From_ReadListener");
            input.setReadListener(readListener);

        }

        LOG.info("setReadListenerTests EXIT");
    }

    @Override
    public void destroy() {
    }

    //set content size for writeListenerUpgrade for large data based on header received from client.
    public void setContentSize(String size) {
        ContentSize = size;
    }

    /**
     * @return the testSet
     */
    public static String getTestSet() {
        return TestSet;
    }

    /**
     * @param testSet
     */
    public static void setTestSet(String testSet) {
        TestSet = testSet;
    }
}