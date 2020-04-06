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
import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.WebConnection;
import javax.sql.DataSource;

/*
 * the handler sets a writeListener for write requests, and this is  the implementation
 * of the write Listener relevant to the upgrade handler, called by the upgrade tests 
 * 
 */
public class TestUpgradeWriteListener implements WriteListener {

    private ServletOutputStream _out = null;
    private WebConnection _webcon = null;
    private LinkedBlockingQueue<String> _queue = new LinkedBlockingQueue<String>();
    private static final Logger LOG = Logger.getLogger(TestUpgradeWriteListener.class.getName());
    private String testName = "";

    public TestUpgradeWriteListener(ServletOutputStream output, LinkedBlockingQueue<String> q, WebConnection wc, String testType) {
        _out = output;
        _queue = q;
        _webcon = wc;
        testName = testType;
    }

    @Override
    public void onWritePossible() throws IOException {
        if ((testName.equals("test_SmallData_UpgradeWL"))
            || (testName.equals("test_Close_WebConnection_Container_UpgradeWL"))
            || (testName.equals("testUpgrade_WL_From_RL")))
        {
            LOG.info("\n [WebContainer | Servlet31_FAT | TestUpgradeWriteListener| " + testName + " | In onWritePossible of TestUpgradeWriteListener, writing small data chunks");
            while ((_queue.peek() != null) && _out.isReady()) {
                String data = _queue.poll();
                LOG.info("data in _queue is  " + data);
                if (data.equals("numbers"))
                {
                    try {
                        LOG.info("TestUpgradeWriteListener[onWritePossible] : printing _out the numbers " +
                                 "for | " + testName + " |...in writeListener thread ");
                        _out.println("0123456789");
                        LOG.info("TestUpgradeWriteListener[onWritePossible] : finished printing _out the numbers " +
                                 "for | " + testName + " |...in writeListener thread");

                    } catch (IOException e) {
                        LOG.info("EXCEPTION in printing _out data in TestUpgradeWriteListener for | " + testName + " |, exception is : "
                                 + e.toString() + " Printing stack trace ..");
                        e.printStackTrace();
                        throw e;
                    }
                }
            }

            if ((_queue.peek() == null) && (_out.isReady())) {
                try {
                    LOG.info("TestUpgradeWriteListener[onWritePossible] : closing web connection for | " + testName + " | in writeListener thread ");
                    this._webcon.close();
                    LOG.info(" TestUpgradeWriteListener[onWritePossible] : finished closing web connection for | " + testName + " | in writeListener thread");
                } catch (Exception e) {
                    LOG.info("EXCEPTION in closing web connection in TestUpgradeWriteListener for | " + testName + " |, exception is : "
                             + e.toString() + " Printing stack trace ..");
                    e.printStackTrace();
                    this.onError(e);
                }
            }

        }

        else if ((testName.equals("test_LargeDataInChunks_UpgradeWL"))
                 || (testName.equals("test_SingleWriteLargeData1000000__UpgradeWL"))
                 || (testName.equals("test_Timeout_UpgradeWL"))
                 || (testName.equals("TestWrite_DontCheckisRedy_fromUpgradeWL"))
                 || (testName.equals("TestWriteFromHandler_AftersetWL"))
                 || (testName.equals("TestUpgrade_ISE_setSecondWriteListener"))
                 || (testName.equals("TestUpgrade_NPE_setNullWriteListener")))
        {
            LOG.info("\n [WebContainer | Servlet31_FAT | TestUpgradeWriteListener| " + testName + " | In onWritePossible of TestUpgradeWriteListener, writing large data chunks");
            while ((_queue.peek() != null) && (_out.isReady()))
            {
                try {
                    LOG.info("In write Listener the length of the _queue is " + _queue.toString().length());
                    LOG.info("Printing _out the large _queue data in | " + testName + " |");

                    //We are looking for the end of the data here and adding on an end clause to the data
                    //The test cases will now look for the end cause and know we are done reading
                    String dataToWrite = _queue.poll();
                    if (_queue.peek() == null) {
                        _out.println(dataToWrite + "/END");
                    } else {
                        _out.println(dataToWrite);
                    }

                    // The test is following will not be part of response , nor the character 'a' or the "SRVE0918E" , this should only be in logs
                    if (testName.equalsIgnoreCase("TestWrite_DontCheckisRedy_fromUpgradeWL")) {
                        // The following will throw exception onError shud be called after the first print is finished if did not go async 
                        LOG.info(testName + " oWP: Printing output again without checking isReady");
                        _out.println('a');

                        // expected output:
                        // write SRVE0918E: The attempted blocking write is not allowed because the non-blocking I/O has already been started.
                        // TestAsyncWrit I   BasicWriteListenerImpl onError method is called ! 
                    }
                } catch (IOException e) {
                    LOG.info("EXCEPTION in printing _out data in TestUpgradeWriteListener for | " + testName + " | in onWritePossible, exception is : "
                             + e.toString() + " Printing stack trace ..");
                    e.printStackTrace();
                    throw e;
                }
            }

            if ((_queue.peek() == null) && (_out.isReady()))
            {
                try {

                    if (testName.equalsIgnoreCase("TestUpgrade_ISE_setSecondWriteListener")
                        || testName.equalsIgnoreCase("TestWriteFromHandler_AftersetWL")) {
                        try {
                            Thread.sleep(5000); // make sure servlet thread is done before calling webconnection close here
                        } catch (InterruptedException e) {
                            LOG.info(testName + " oWP: sleep interrupted");
                        }
                    }
                    LOG.info("closing web connection in onWritePossible in TestUpgradeWriteListener for | " + testName + " |");

                    this._webcon.close();

                    LOG.info("Webconnection closed | " + testName + " | test done ");
                } catch (Exception e) {
                    LOG.info("EXCEPTION in closing web connection in TestUpgradeWriteListener for| " + testName + " | in onWritePossible, exception is : "
                             + e.toString() + " Printing stack trace ..");
                    e.printStackTrace();
                    this.onError(e);
                }
            }
        }

        else if (testName.equals("test_ContextTransferProperly_UpgradeWL")) {
            LOG.info("\n [WebContainer | Servlet31_FAT | TestUpgradeWriteListener| [test_ContextTransferProperly_UpgradeWL] | Starting test in onWritePossible");
            try {

                LOG.info("Attempting a JNDI lookup");
                Context ctx = new InitialContext();
                DataSource ds = (DataSource) ctx.lookup("java:comp/UserTransaction");
                //We expect an exception here and don't expect to ever get to this point.
            } catch (Exception e) {
                LOG.info("Caught the JNDI exception. Passing the exception back to the client for comparison");
                LOG.info(e.getMessage());

                _out.println(e.getMessage());
                _out.flush();
            }

            try {
                LOG.info("Closing the WebConnection");
                _webcon.close();
            } catch (Exception e) {
                LOG.info("EXCEPTION in closing web connection in TestUpgradeWriteListener for [test_ContextTransferProperly_UpgradeWL] in onWritePossible, exception is : "
                         + e.toString() + " Printing stack trace ..");
                e.printStackTrace();
                this.onError(e);
            }

            LOG.info("\n [WebContainer | Servlet31_FAT | TestUpgradeWriteListener| [test_ContextTransferProperly_UpgradeWL] | Completing test in onWritePossible");
        }
    }

    @Override
    public void onError(final Throwable t) {

        if ((testName.equals("test_Timeout_UpgradeWL")) && (t instanceof SocketTimeoutException)) {
            LOG.info("\n [WebContainer | Servlet31_FAT | test_Timeout_UpgradeWL | In onError of TestUpgradeWriteListener with a timeout");

            try {
                LOG.info("test_Timeout_UpgradeWL : Timeout occurred during the test");
                LOG.info("Closing the webConnection in onError");
                if (_out.isReady()) {
                    _webcon.close();
                    LOG.info("Finishing sending the string and closing the webConnection in onError in TestUpgradeWriteListener");
                }
            } catch (Exception e) {
                LOG.info("EXCEPTION in closing web connection or sending the string in onError  TestUpgradeWriteListener , exception is : "
                         + e.toString() + " Printing stack trace ..");
                e.printStackTrace();
            }
        }

        else if (testName.equalsIgnoreCase("TestWrite_DontCheckisRedy_fromWL"))
        {
            try {
                LOG.info(testName + " onError is called ! ");
                t.printStackTrace();
                String outError = t.getMessage();
                LOG.info(testName + " onError --> " + outError);

            } finally {
                if (_out.isReady())
                {
                    try {
                        this._webcon.close();
                    } catch (Exception e) {
                        LOG.info("EXCEPTION in closing web connection in onError  TestUpgradeWriteListener , exception is : "
                                 + e.toString() + " Printing stack trace ..");
                        e.printStackTrace();
                    }
                }
            }
        }
        else {
            try {
                LOG.info("\n [WebContainer | Servlet31_FAT | TestUpgradeWriteListener| | In onError of TestUpgradeWriteListener");

                LOG.info("closing web connection in onError in TestUpgradeWriteListener ");
                if (_out.isReady())
                {
                    this._webcon.close();
                }
                LOG.info("finished closing web connection in onError in TestUpgradeWriteListener ");
            } catch (Exception e) {
                LOG.info("EXCEPTION in closing web connection in onError  TestUpgradeWriteListener , exception is : "
                         + e.toString() + " Printing stack trace ..");
                e.printStackTrace();
            }
        }
    }

}
