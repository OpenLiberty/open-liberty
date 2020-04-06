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
package com.ibm.ws.fat.wc.servlet31.writeListener;

import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.servlet.AsyncContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

public class TestAsyncWriteListener implements WriteListener {

    private ServletOutputStream output = null;
    private AsyncContext ac = null;
    private HttpServletRequest req = null;
    private HttpServletResponse response = null;
    private String TestCall = "";
    private String postDataSize = "";
    private String type = "";

    private static final Logger LOG = Logger.getLogger(TestAsyncWriteListener.class.getName());
    private LinkedBlockingQueue<String> queue = new LinkedBlockingQueue<String>();

    TestAsyncWriteListener(ServletOutputStream s, LinkedBlockingQueue<String> q, AsyncContext c, HttpServletRequest request, HttpServletResponse res, String st) {
        output = s;
        queue = q;
        ac = c;
        req = request;
        response = res;
        TestCall = st;
    }

    public TestAsyncWriteListener(ServletOutputStream out, LinkedBlockingQueue<String> q, AsyncContext ac2, HttpServletRequest req2, HttpServletResponse res, String st2,
                                  String pds, String t) {
        output = out;
        queue = q;
        ac = ac2;
        req = req2;
        response = res;
        TestCall = st2;
        postDataSize = pds;
        type = t;
        LOG.info("TestAsyncWriteListener: run the test with following parameters -->" + TestCall + " , " + postDataSize + " , " + type);
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.WriteListener#onWritePossible()
     */
    @Override
    public void onWritePossible() throws IOException {

        // write while there is data and is ready to write
        if (TestCall.equalsIgnoreCase("TestWrite_DontCheckisReady_fromWL")
            || TestCall.equalsIgnoreCase("TestWriteFromServlet_AftersetWL")
            || TestCall.equalsIgnoreCase("Test_ISE_setSecondWriteListener"))
        {

            if ((queue.peek() != null) && (output.isReady()))
            {
                LOG.info(TestCall + " oWP: queue length is " + queue.toString().length());
                //LOG.info("The contents of the queue are " + queue.toString());

                try {
                    output.print(queue.take());

                    // The test is following will not be part of response , nor the character 'a' or the "SRVE0918E" , this should only be in logs
                    if (TestCall.equalsIgnoreCase("TestWrite_DontCheckisReady_fromWL")) {
                        // The following will throw exception onError shud be called after the first print is finished if did not go async 
                        LOG.info(TestCall + " oWP: Printing output again without checking isReady");
                        output.print('a');

                        // expected output:
                        // write SRVE0918E: The attempted blocking write is not allowed because the non-blocking I/O has already been started.
                        // TestAsyncWrit I   BasicWriteListenerImpl onError method is called ! 
                    }
                } catch (InterruptedException e) {

                    e.printStackTrace();
                }
                LOG.info(TestCall + " oWP: Done printing output");
            }
            if ((queue.peek() == null) && (output.isReady()))
            {
                output.flush();
                LOG.info(TestCall + " oWP: Done flushing outputStream.");

                if (TestCall.equalsIgnoreCase("Test_ISE_setSecondWriteListener")) {
                    try {
                        Thread.sleep(3000); // make sure servlet thread is done before calling ac.complete here
                    } catch (InterruptedException e) {
                        LOG.info(TestCall + " oWP: sleep interrupted");
                    }
                }
                if (output.isReady())
                {
                    ac.complete();
                    LOG.info(TestCall + " oWP: Finished call to ac.complete");
                }
                else {
                    LOG.info(TestCall + " oWP:  out may not be ready , cannot complete");
                }

            }
        }
        else if (TestCall.equals("TestWL_Println_Large")
                 || TestCall.equals("TestWL_Write_Large")
                 || TestCall.equals("TestWL_Write_Medium"))
        {
            if ((queue.peek() != null) && (output.isReady()))
            {
                LOG.info(TestCall + " oWP: queue length is " + queue.toString().length());
                //LOG.info("The contents of the queue are " + queue.toString());
                try {
                    if ("print".equalsIgnoreCase(type))
                        output.print(queue.take());
                    else if ("println".equalsIgnoreCase(type)) {
                        LOG.info(TestCall + " oWP: println output bytes");
                        output.println(queue.take());
                    }
                    else {
                        LOG.info(TestCall + " oWP: write output bytes");
                        output.write(queue.take().getBytes());
                    }

                } catch (InterruptedException e) {

                    e.printStackTrace();
                    this.onError(e);
                }
                LOG.info(TestCall + " oWP: Done with output");
            }
            if (queue.peek() == null) {
                if (output.isReady())
                {
                    ac.complete();
                    LOG.info(TestCall + " oWP: Finished call to ac.complete");
                }
                else {
                    LOG.info(TestCall + " oWP:  out may not be ready , cannot complete");
                }
            }
        }
        else if (TestCall.equals("TestWL_Write_MediumChunks")
                 || TestCall.equals("TestWL_Println_MediumChunks")
                 || TestCall.equals("TestWL_Write_LargeChunks")
                 || TestCall.equals("TestWL_Println_LargeChunks"))
        {

            LOG.info(TestCall + " oWP:  queue length is " + queue.toString().length());
            //LOG.info(TestCall+ " oWP:  The contents of the queue are " + queue.toString());

            while ((queue.peek() != null) && (output.isReady()))
            {
                try {
                    if ("print".equalsIgnoreCase(type)) {
                        LOG.info(TestCall + " oWP: print output bytes");
                        output.print(queue.take());
                        LOG.info(TestCall + " oWP: Done printing output , queue size left -->" + queue.size());
                    }
                    else if ("println".equalsIgnoreCase(type)) {
                        LOG.info(TestCall + " oWP: println output bytes");
                        output.println(queue.take());
                        LOG.info(TestCall + " oWP: Done printingln output , queue size left -->" + queue.size());
                    }
                    else if ("writeAndsleep".equalsIgnoreCase(type)) {
                        LOG.info(TestCall + " oWP: write output bytes");
                        output.write(queue.take().getBytes());
                        LOG.info(TestCall + " oWP: sleep");
                        Thread.sleep(1000);
                        LOG.info(TestCall + " oWP: Done writing output , queue size left -->" + queue.size());
                    }
                    else {
                        LOG.info(TestCall + " oWP: write output bytes");
                        output.write(queue.take().getBytes());
                        LOG.info(TestCall + " oWP: Done writing output , queue size left -->" + queue.size());
                    }

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }
            if ((queue.peek() == null))
            {
                if (output.isReady())
                {
                    ac.complete();
                    LOG.info(TestCall + " oWP: Finished call to ac.complete");
                }
                else {
                    LOG.info(TestCall + " oWP:  out may not be ready , cannot complete");
                }

            }
        }
        else if (TestCall.equalsIgnoreCase("Test_Return_onWritePossible"))
        {
            LOG.info("Test being done : " + TestCall);
            return;

        }
        else if (TestCall.equalsIgnoreCase("TestWL_IOE_AfterWrite"))
        {
            StringBuilder sb = new StringBuilder();
            String postDataSize = req.getHeader("ContentSizeSent");

            if (postDataSize != null)
            {
                while (sb.toString().length() < Long.parseLong(postDataSize)) {
                    sb.append('1');

                }
            }
            if (output.isReady())
            {
                output.print(sb.toString());
            }
            throw new IOException("ThrowExceptionAfterWrite");
        }

        else if (TestCall.equalsIgnoreCase("TestWL_onError"))
        {
            String postDataSize = req.getHeader("ContentSizeSent");

            StringBuilder sb = new StringBuilder();

            if (postDataSize != null) {
                while (sb.toString().length() < Long.parseLong(postDataSize)) {
                    sb.append('1');

                }
            }
            throw new IOException("TestonError");
        }
        else if (TestCall.equalsIgnoreCase("TestWL_ContextTransferProperly"))
        {
            String testcase = "TestWL_ContextTransferProperly";
            LOG.info("Test being done : " + testcase);
            try {
                //This is expected to fail. We will write back the exception back to the client
                Context ctx = new InitialContext();
                @SuppressWarnings("unused")
                DataSource ds = (DataSource) ctx.lookup("java:comp/UserTransaction");
                LOG.info(testcase + " InitialContext lookup successful ! ");
            } catch (Exception e) {
                LOG.info(testcase + " Expected exception occurred while doing the initialContext lookup : " + e);
                if (output.isReady()) {
                    output.print(e.toString());
                }
                if (output.isReady()) {
                    output.flush();
                }
            } finally {
                ac.complete();
            }
        }
        else if (TestCall.equalsIgnoreCase("Test_NPE_setNullWriteListener")) {
            LOG.info(TestCall + " oWP: We should not in Listener! ");
        }

        else if (TestCall.equalsIgnoreCase("printBoolean")
                 || TestCall.equals("printChar")
                 || TestCall.equals("printInt")
                 || TestCall.equals("printDouble")
                 || TestCall.equals("printFloat")
                 || TestCall.equals("printLong"))
        {
            if ((queue.peek() != null) && (output.isReady()))
            {
                LOG.info("queue length is " + queue.toString().length());
                //LOG.info("The contents of the queue are " + queue.toString());
                try {
                    if (TestCall.equals("printBoolean")) {
                        Boolean b = new Boolean(queue.take());
                        output.print(b);
                    }
                    else if (TestCall.equals("printChar")) {
                        char c = queue.take().charAt(0);
                        output.print(c);
                    }
                    else if (TestCall.equals("printDouble")) {
                        double d = Double.parseDouble(queue.take());
                        output.print(d);
                    }
                    else if (TestCall.equals("printFloat")) {
                        float f = Float.parseFloat(queue.take());
                        output.print(f);
                    } else if (TestCall.equals("printInt")) {
                        int i = Integer.parseInt(queue.take());
                        output.print(i);
                    } else if (TestCall.equals("printLong")) {
                        long l = Long.parseLong(queue.take());
                        output.print(l);
                    }
                } catch (Exception e) {

                    e.printStackTrace();
                    this.onError(e);
                    return;
                }
            }
            if ((queue.peek() == null) && (output.isReady()))
            {
                ac.complete();
                LOG.info(TestCall + " oWP: Finished call to ac.complete");
            }
            LOG.info(TestCall + " oWP:Done with output");

        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.WriteListener#onError(java.lang.Throwable)
     */
    @Override
    public void onError(final Throwable t) {
        if (TestCall.equalsIgnoreCase("TestWrite_DontCheckisReady_fromWL")
            || TestCall.equals("TestWL_Println_Large")
            || TestCall.equals("TestWL_Write_Large")
            || TestCall.equals("TestWL_Write_Medium")
            || TestCall.equals("Test_ISE_setSecondWriteListener"))
        {
            try {
                LOG.info(TestCall + " onError is called ! ");
                t.printStackTrace();
                String outError = t.getMessage();
                LOG.info(TestCall + " onError --> " + outError);

            } finally {
                ac.complete();
            }
        }
        else if (TestCall.equalsIgnoreCase("TestWL_onError"))
        {
            LOG.info(TestCall + " oError method is called ! ");
            ServletOutputStream out;
            try {
                out = response.getOutputStream();
                String outError = t.getMessage();
                if (out.isReady())
                    out.print("TestonError onError method is called ! " + outError);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                ac.complete();
            }
        }
        else if (TestCall.equalsIgnoreCase("TestWL_IOE_AfterWrite"))
        {
            LOG.info(TestCall + " oError method is called ! ");
            ServletOutputStream out;
            try {
                out = response.getOutputStream();
                String outError = t.getMessage();
                if (out.isReady())
                    out.print("ThrowExceptionAfterWrite onError method is called ! " + outError);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                ac.complete();
            }
        }
        else if (TestCall.equalsIgnoreCase("printBoolean") || TestCall.equals("printChar")
                 || TestCall.equals("printInt") || TestCall.equals("printDouble")
                 || TestCall.equals("printFloat") || TestCall.equals("printLong"))
        {
            LOG.info(TestCall + " oError method is called ! ");
            ServletOutputStream out;
            try {
                out = response.getOutputStream();
                String outError = t.getMessage();
                if (out.isReady())
                    out.print("print onError method is called ! " + outError);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                ac.complete();
            }
        }

    }
}
