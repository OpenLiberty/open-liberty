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
package com.ibm.ws.fat.wc.servlet31.upgradeHandler;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.WebConnection;
import javax.sql.DataSource;

/*
 * the handler sets a readListener for read requests, and this is  the implementation
 * of the read Listener relevant to the upgrade handler, called by the upgrade tests 
 *
 */
public class TestUpgradeReadListener implements ReadListener {

    private ServletInputStream input = null;
    private ServletOutputStream output = null;
    private WebConnection wc = null;
    private static String TestToCall = "";
    private final LinkedBlockingQueue<String> queue = new LinkedBlockingQueue<String>();
    private static final Logger LOG = Logger.getLogger(TestUpgradeReadListener.class.getName());
    long dataSize = 0;
    private final String TIMEOUT_OCCURRED = ", A Timeout has been triggered";
    private StringBuilder timeoutStringBuilder;

    TestUpgradeReadListener(ServletInputStream in, WebConnection c, ServletOutputStream out, String test)
        throws IOException {

        this.input = in;
        this.wc = c;
        output = out;
        TestToCall = test;
    }

    @Override
    public void onDataAvailable() throws IOException {
        if (TestToCall.equals("testUpgradeReadListenerSmallData"))
        {
            StringBuilder sb = new StringBuilder();
            try {

                LOG.info("\n [WebContainer | Servlet31_FAT | TestUpgradeReadListener| testUpgradeReadListenerSmallData| In onDataAvailable of TestUpgradeReadListener, reading small data chunks");

                int len = -1;
                byte b[] = new byte[1024];

                if (!input.isReady()) {
                    LOG.info("testUpgradeReadListenerSmallData in onDataAvailable() in TestUpgradeReadListener onDataAvailable, isReady=false ");
                }
                while (input.isReady() && (len = input.read(b)) != -1) {
                    LOG.info("testUpgradeReadListenerSmallData in onDataAvailable() in TestUpgradeReadListener onDataAvailable, isReady=true , reading data from input stream and appending to stringbuilder ");
                    String data = new String(b, 0, len);
                    sb.append(data);

                }
            } catch (Exception e) {
                LOG.info("EXCEPTION in  onDataAvailable in TestUpgradeReadListener for testUpgradeReadListenerSmallData, exception is : "
                         + e.toString() + " Printing stack trace ..");
                e.printStackTrace();
                this.onError(e);
            }

            try {

                if (output.isReady()) {
                    LOG.info("Writing out the string I read");
                    // get the input from the client and send the same back to the client, to ensure test passes.
                    output.println(sb.toString());
                    output.flush();

//                    LOG.info("closing web connection in onDataAvailable in TestUpgradeReadListener for testUpgradeReadListenerSmallData");
//                    this.wc.close();
//                    LOG.info("closed webConnection in onDataAvailable in TestUpgradeReadListener for testUpgradeReadListenerSmallData");
                }

            } catch (Exception e) {
                LOG.info("EXCEPTION in closing web connection in onDataAvailable in TestUpgradeReadListener for testUpgradeReadListenerSmallData, exception is : "
                         + e.toString() + " Printing stack trace ..");
                e.printStackTrace();
            }

        }

        else if (TestToCall.equals("testUpgradeReadListenerLargeData")) {

            boolean complete = false;
            try {

                LOG.info("\n [WebContainer | Servlet31_FAT | TestUpgradeReadListener| testUpgradeReadListenerLargeData| In onDataAvailable of TestUpgradeReadListener, reading large data chunks");

                int len = -1;
                byte b[] = new byte[1024];

                if (!input.isReady()) {
                    LOG.info("testUpgradeReadListenerLargeData onDataAvailable, isReady=false");
                }

                while (input.isReady() && (len = input.read(b)) != -1) {
                    LOG.info("testUpgradeReadListenerLargeData in onDataAvailable() in TestUpgradeReadListener onDataAvailable, isReady=true , reading data from input stream and measuring its size ");
                    dataSize += len;
                    String s = new String(b);
                    if (s.contains("Sending Data Complete")) {
                        LOG.info("testUpgradeReadListenerLargeData in onDataAvailable() in TestUpgradeReadListener, complete message found. Test is done");
                        dataSize -= ("Sending Data Complete").length();
                        complete = true;
                    }
                }

            } catch (Exception e) {
                LOG.info("EXCEPTION in  onDataAvailable in TestUpgradeReadListener for testUpgradeReadListenerLargeData, exception is : "
                         + e.toString() + " Printing stack trace ..");
                e.printStackTrace();
            }

            if (complete && output.isReady()) {
                LOG.info("testUpgradeReadListenerLargeData in onDataAvailable() in TestUpgradeReadListener, output is ready and we are complete");
                LOG.info("testUpgradeReadListenerLargeData in onDataAvailable() in TestUpgradeReadListener, writing out the amount of data we read excluding the amount for the complete message : "
                         + dataSize);
                output.println(Long.toString(dataSize)); //send data size of the data read back to the client
                output.flush();

//                try {
//                    LOG.info("closing web connection in onDataAvailable in testUpgradeReadListenerLargeData in TestUpgradeReadListener");
//
//                    this.wc.close();
//                    LOG.info("finished closing web connection in onDataAvailable in  testUpgradeReadListenerLargeData in TestUpgradeReadListener");
//                } catch (Exception e) {
//                    LOG.info("EXCEPTION in closing web connection in onDataAvailable in TestUpgradeReadListener for testUpgradeReadListenerLargeData, exception is : "
//                             + e.toString() + " Printing stack trace ..");
//                    e.printStackTrace();
//                }
            }

        }

        else if (TestToCall.equals("testUpgradeReadListenerSmallDataThrowException"))
        {
            try {

                LOG.info("\n [WebContainer | Servlet31_FAT | TestUpgradeReadListener| testUpgradeReadListenerSmallDataThrowException| In onDataAvailable of TestUpgradeReadListener, reading small data chunks and throw exception");

                StringBuilder sb = new StringBuilder();
                int len = -1;
                byte b[] = new byte[1024];

                if (!input.isReady()) {
                    LOG.info("testUpgradeReadListenerSmallDataThrowException in onDataAvailable() in TestUpgradeReadListener onDataAvailable, isReady=false ");
                }
                while (input.isReady() && (len = input.read(b)) != -1) {
                    LOG.info("testUpgradeReadListenerSmallDataThrowException in onDataAvailable() in TestUpgradeReadListener onDataAvailable, isReady=true , reading data from input stream and appending to stringbuilder ");
                    String data = new String(b, 0, len);
                    sb.append(data);

                }

                // get the input from the client and send the same back to the client, to ensure test passes.
                LOG.info("testUpgradeReadListenerSmallDataThrowException in onDataAvailable() in TestUpgradeReadListener , printing small data to the output stream, data printed is: "
                         + sb.toString());
                output.println(sb.toString());
                output.flush();
                LOG.info("testUpgradeReadListenerSmallDataThrowException in onDataAvailable() in TestUpgradeReadListener , finished printing small data to the output stream, data printed is: "
                         + sb.toString());
            } catch (Exception e) {
                LOG.info("EXCEPTION in  onDataAvailable in TestUpgradeReadListener for testUpgradeReadListenerSmallDataThrowException, exception is : "
                         + e.toString() + " Printing stack trace ..");
                e.printStackTrace();
            }

            LOG.info("Throwing an IOException for test testUpgradeReadListenerSmallDataThrowException, TestUpgradeReadListener[onDataAvailable()]");
            throw new IOException("Exception thrown from onDataAvailable");

        } else if (TestToCall.equals("testUpgradeReadListenerTimeout")) {
            LOG.info("\n [WebContainer | Servlet31_FAT | TestUpgradeReadListener| testUpgradeReadListenerTimeout| In onDataAvailable of TestUpgradeReadListener, reading all the data until we encounter a timeout");
            timeoutStringBuilder = new StringBuilder();
            int len = -1;
            byte b[] = new byte[1024];

            while (input.isReady() && (len = input.read(b)) != -1) {
                String data = new String(b, 0, len);
                timeoutStringBuilder.append(data);
            }

//            if (input.isReady()) {
//                try {
//                    LOG.info("Closing web connection in onDataAvailable in  testUpgradeReadListenerTimeout in TestUpgradeReadListener ");
//                    this.wc.close();
//                    LOG.info("Finished closing web connection in onDataAvailable in  testUpgradeReadListenerTimeout in TestUpgradeReadListener ");
//                } catch (Exception e) {
//                    LOG.info("EXCEPTION in closing web connection in onDataAvailable in TestUpgradeReadListener for testUpgradeReadListenerTimeout, exception is : "
//                             + e.toString() + " Printing stack trace ..");
//                    e.printStackTrace();
//                }
//            }
        }

        if (TestToCall.equals("testUpgrade_WriteListener_From_ReadListener"))
        {
            // no need to read anything but we will have to read otherwise it will calback onDataAv.
            // the test is to call WL from here and print and output from WL

            try {
                StringBuilder sb = new StringBuilder();
                int len = -1;
                byte b[] = new byte[1024];

                if (!input.isReady()) {
                    LOG.info("testUpgrade_WriteListener_From_ReadListener in onDataAvailable() in TestUpgradeReadListener onDataAvailable, isReady=false ");
                }
                while (input.isReady() && (len = input.read(b)) != -1) {
                    LOG.info("testUpgrade_WriteListener_From_ReadListener in onDataAvailable() in TestUpgradeReadListener onDataAvailable, isReady=true , reading data from input stream and appending to stringbuilder ");
                    String data = new String(b, 0, len);
                    sb.append(data);
                }
                //test_SmallData_UpgradeWL in TestUpgradeReadListener adding string to queue
                queue.add("numbers");
                //setting writeListener
                output = wc.getOutputStream();
                WriteListener writeListener = new TestUpgradeWriteListener(output, queue, wc, "testUpgrade_WL_From_RL");

                output.setWriteListener(writeListener);

            } catch (Exception e) {
                LOG.info("EXCEPTION in closing web connection in onDataAvailable in TestUpgradeReadListener for testUpgrade_WL_From_RL , exception is : "
                         + e.toString() + " Printing stack trace ..");
                e.printStackTrace();
                this.onError(e);
            }
        } else if (TestToCall.equals("testRead_ContextTransferProperly_WhenUpgradeRLSet")) {
            LOG.info("\n [WebContainer | Servlet31_FAT | TestUpgradeReadListener| testRead_ContextTransferProperly_WhenUpgradeRLSet | In onDataAvailable of TestUpgradeReadListener, doing a context lookup and passing the resulting exception text back");

            int len = -1;
            byte b[] = new byte[1024];
            String data = "";

            try {
                while (input.isReady() && (len = input.read(b)) != -1) {
                    data = new String(b, 0, len);
                }
                LOG.info("Read this string: " + data);

                LOG.info("Attempting a JNDI lookup");
                Context ctx = new InitialContext();
                DataSource ds = (DataSource) ctx.lookup("java:comp/UserTransaction");
                //We expect an exception here and don't expect to ever get to this point.
            } catch (Exception e) {
                LOG.info("Caught the JNDI exception. Passing the exception back to the client for comparison");
                LOG.info(e.getMessage());

                if (data.equals(e.getMessage())) {
                    LOG.info("Printing PASSED to the stream");
                    output.println("PASSED");
                } else {
                    LOG.info("Printing FAILED to the stream");
                    output.println("FAILED");
                }
                output.flush();
            }
            LOG.info("Finished test testRead_ContextTransferProperly_WhenUpgradeRLSet");
        }

    }

    @Override
    public void onAllDataRead() throws IOException {

        if (!TestToCall.equals("testUpgrade_WriteListener_From_ReadListener")) {
            try {
                LOG.info("\n [WebContainer | Servlet31_FAT | TestUpgradeReadListener| | In onAllDataRead of TestUpgradeReadListener");

                output.flush();

                LOG.info("closing web connection in onAllDataRead in TestUpgradeReadListener ");
                //Don't need to check is ready since all the data has been read by this point
                this.wc.close();
                LOG.info("closed webConnection in onAllDataRead in TestUpgradeReadListener");
            } catch (Exception e) {
                LOG.info("EXCEPTION in onAllDataRead in TestUpgradeReadListener , exception is : "
                         + e.toString() + " Printing stack trace ..");
                e.printStackTrace();
            }
        } else {
            LOG.info("\n [WebContainer | Servlet31_FAT | TestUpgradeReadListener| | In onAllDataRead, nothing to do for test testUpgrade_WriteListener_From_ReadListener");
        }

    }

    @Override
    public void onError(final Throwable t) {
        LOG.info("\n [WebContainer | Servlet31_FAT | TestUpgradeReadListener| | In onError of TestUpgradeReadListener");

        if (t.getMessage() != null && t.getMessage().equals("Exception thrown from onDataAvailable") && TestToCall.equals("testUpgradeReadListenerSmallDataThrowException")) {
            LOG.info("onError for test : testUpgradeReadListenerSmallDataThrowException");

            try {
                output.println(t.getMessage());
                output.flush();

                this.wc.close();
            } catch (Exception e) {
                LOG.info("An error occurred while writing the data or closing the WebConnection");
                e.printStackTrace();
            }
        } else if ((t instanceof SocketTimeoutException) && (TestToCall.equals("testUpgradeReadListenerTimeout"))) {
            LOG.info("onError for test : testUpgradeReadListenerTimeout");
            timeoutStringBuilder.append(TIMEOUT_OCCURRED);
            try {
                output.println(timeoutStringBuilder.toString());
                output.flush();

                this.wc.close();
            } catch (Exception e) {
                LOG.info("An error occurred while writing the data or closing the WebConnection");
                e.printStackTrace();
            }
        } else {
            t.printStackTrace();
            try {

                LOG.info("Closing web connection in onError in TestUpgradeReadListener ");
                //Don't need to check isReady here since it's an error case
                this.wc.close();
                LOG.info("Closed webConnection in onError  in TestUpgradeReadListener");

            } catch (Exception e) {
                LOG.info("EXCEPTION in closing web connection in onError in TestUpgradeReadListener , exception is : "
                         + e.toString() + " Printing stack trace ..");
                e.printStackTrace();
            }
        }

    }

}