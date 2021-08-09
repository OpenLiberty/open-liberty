/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.servlet_31_fat.multinbreadapp.war.test.listeners;

import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

import javax.servlet.AsyncContext;
import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * First Listener for MultiRead
 */
public class TestAsyncNBReadListener implements ReadListener {

    private ServletInputStream input = null;
    private HttpServletResponse response = null;
    private HttpServletRequest request = null;
    private AsyncContext ac = null;
    private final LinkedBlockingQueue<String> queue = new LinkedBlockingQueue<String>();
    private String responseString;
    String testToPerfom = null;
    boolean passCheck = false;

    long dataSize = 0;
    long dataSizeofLen = 0;
    int iterations = 1;
    String testToRun = null;
    int postDataLen = 0;
    boolean testFinished = false;
    boolean pass = false;

    String classname = "TestAsyncNBReadListener";

    private static final Logger LOG = Logger.getLogger(TestAsyncNBReadListener.class.getName());

    public TestAsyncNBReadListener(ServletInputStream in, HttpServletRequest req, HttpServletResponse r,
                                   AsyncContext c, String test) {
        input = in;
        response = r;
        ac = c;
        request = req;
        testToPerfom = test;
        dataSize = 0;
        dataSizeofLen = 0;
        iterations = 1;
        testToRun = null;
        postDataLen = 0;
        testFinished = false;
        responseString = null;
        pass = false;

    }

    @Override
    public void onDataAvailable() throws IOException {

        System.out.println("onDataAvailable : TestAsyncNBReadListener Start" + testToPerfom);

        if (testToPerfom.equalsIgnoreCase("getParamNotAllowed")) {
            this.getParamNotAllowed();
            LOG.info("--------------------------------------------------------------");
        } else if (testToPerfom.equalsIgnoreCase("getParamNotAllowedReadStream")) {
            // async RL getParameter (not allowed ) , async getInputStream read (allowed ) close
            this.getParamNotAllowedReadStream();
            LOG.info("--------------------------------------------------------------");
        } else if ((testToPerfom.equalsIgnoreCase("NBReadStreamNBReadStreamServlet"))) {

            // Read the first stream
            // Read 5 bytes
            int Len = 5;

            //int Len = request.getContentLength();

            StringBuffer inBuffer = new StringBuffer();
            try {
                byte[] inBytes = new byte[(int) (Len - dataSizeofLen)];

                ServletInputStream input = request.getInputStream();

                if (input.isReady()) {
                    Len = input.read(inBytes);
                    if (Len > 0) {
                        inBuffer.append(new String(inBytes, 0, Len));
                        dataSizeofLen += Len;
                    }
                }
            } catch (IOException exc) {
                responseString += "Exception reading post data " + exc + " :";
                System.out.println(responseString);
            }

            if (dataSizeofLen == Len) {

                dataSizeofLen = 0;
                input.close();

                // now read async again

                ServletInputStream input = request.getInputStream(); // required to get the RL
                LOG.info(classname + " input ->" + input);
                ReadListener readListener = new TestAsyncNBReadListener2(input, request, response, ac, "NBReadStreamNBReadStreamServlet", inBuffer);
                input.setReadListener(readListener);

            }
            LOG.info("--------------------------------------------------------------");

        } else if ((testToPerfom.equalsIgnoreCase("ReadParameterNBReadFilter_NoWorkServlet"))) {

            int cl = request.getContentLength();

            readInputStream(input, cl);

            if (testFinished & pass) {
                ServletOutputStream out = response.getOutputStream();
                out.println("PASS : " + responseString);
                return;
            }
            LOG.info("--------------------------------------------------------------");

        }

        else if ((testToPerfom.equalsIgnoreCase("NBReadPostDataFromInputStreamServlet")) ||
                 testToPerfom.equalsIgnoreCase("ReadPostDataFromNBInputStreamFilter_NoWorkServlet") ||
                 testToPerfom.equalsIgnoreCase("ReadParameterFilter_ReadPostDataFromInputStreamServlet")) {
            // now read the stream

            int cl = request.getContentLength();

            readInputStream(input, cl);

            if (testFinished) {

                if (pass) {
                    LOG.info("onDataAvailable expected output for " + testToPerfom);
                    //out.println("PASS : " + responseString);
                    LOG.info("onDataAvailable read again ");
                } else {
                    ServletOutputStream out = response.getOutputStream();
                    out.println("FAIL : " + responseString);
                    return;
                }

                // now read again
                // the first one is closed so listener must be set to null
                //reset the var for reread same
                testFinished = false;
                dataSize = 0;
                input.close(); // first close

                // now any more read will be blocking but since we read all the data it is only reading from store
                ServletInputStream input2 = request.getInputStream();
                readInputStream(input2, cl);

                if (testFinished) {
                    ServletOutputStream out = response.getOutputStream();
                    if (pass) {
                        LOG.info("onDataAvailable expected output for " + testToPerfom);
                        out.println("PASS : " + responseString);
                    } else {
                        out.println("FAIL : " + responseString);
                        return;
                    }
                }

                // Since listener was set to null on previous close , we will not be able to call onAllDataRead
                if (queue.peek() == null) {
                    ac.complete();
                }
            }
            LOG.info("--------------------------------------------------------------");

        } else if (testToPerfom.equalsIgnoreCase("NBReadLargePostDataInputStreamServlet")) {
            this.doWork(request, response);
            LOG.info("--------------------------------------------------------------");
        } else if (testToPerfom.equalsIgnoreCase("NBInputStream_Reader")) {

            // Read the first stream
            // Read 5 bytes
            int Len = 5;

            //int Len = request.getContentLength();

            StringBuffer inBuffer = new StringBuffer();
            try {
                byte[] inBytes = new byte[(int) (Len - dataSizeofLen)];

                ServletInputStream input = request.getInputStream();

                if (input.isReady()) {
                    Len = input.read(inBytes);
                    if (Len > 0) {
                        inBuffer.append(new String(inBytes, 0, Len));
                        dataSizeofLen += Len;
                    }
                }
            } catch (IOException exc) {
                responseString += "Exception reading post data " + exc + " :";
                System.out.println(responseString);
            }

            if (dataSizeofLen == Len) {

                dataSizeofLen = 0;
                input.close();

                // get the reader

                java.io.BufferedReader rdr = request.getReader();
                try {
                    StringBuffer inBuffer3 = new StringBuffer();
                    char[] inChars = new char[1024];
                    for (int n3; (n3 = rdr.read(inChars, 0, 1024)) != -1;) {
                        inBuffer3.append(new String(inChars, 0, n3));
                    }
                    if (inBuffer3.indexOf("NBInputStream_Reader") != -1) {
                        responseString += "Pass from Reader : ";
                        pass = true;
                        System.out.println("responseString NBInputStream_Reader = " + responseString);
                    } else {
                        responseString += "Fail from NBInputStream_Reader - post data = " + inBuffer3 + " : ";
                        pass = false;
                    }

                } catch (IOException exc) {
                    responseString += "Fail from NBInputStream_Reader - " + exc.toString();
                    pass = false;
                }

                if (pass) {

                    rdr.close();

                    String param2 = request.getParameter("F003449Test");

                    if (param2 == null) {
                        responseString += "Fail from NBInputStream_Reader - parameter not available :";
                        pass = false;
                    } else {
                        responseString += "Pass from parameter read : ";
                    }

                }

                ServletOutputStream out = response.getOutputStream();
                if (pass) {
                    LOG.info("onDataAvailable expected output for " + testToPerfom);
                    out.println("PASS : " + responseString);
                } else {
                    out.println("FAIL : " + responseString);
                    return;
                }

                // Since listener was set to null on previous close , we will not be able to call onAllDataRead
                if (queue.peek() == null) {
                    ac.complete();
                }

            }

        }

        System.out.println("onDataAvailable : TestAsyncNBReadListener done " + testToPerfom);

    }

    @Override
    public void onAllDataRead() throws IOException {
        System.out.println("onAllDataRead : TestAsyncNBReadListener");
        ServletOutputStream out = response.getOutputStream();
        while (queue.peek() != null) {
            String data = queue.poll();
            LOG.info("onAllDataRead queueContains = " + data);
        }

        out.flush();

        if (request.getRequestURI().contains("AnotherRLRequestServlet")) {
            return;
        }

        if (queue.peek() == null) {
            ac.complete();
        }
    }

    @Override
    public void onError(final Throwable t) {
        try {
            ServletOutputStream out = response.getOutputStream();
            out.print("OnError method successfully called !!!");
        } catch (IOException e) {
            e.printStackTrace();
        }
        ac.complete();
    }

    private void doWork(HttpServletRequest request, HttpServletResponse response) throws IOException {

        responseString = "F00349 Test Results with InputStream";
        String testToRun = null;

        postDataLen = request.getContentLength();
        if (postDataLen < 18) {
            responseString += " : Fail insufficent post data - at least 18 butes expected but received " + postDataLen;
            pass = false;
            ServletOutputStream out = response.getOutputStream();
            out.println("FAIL : " + responseString);
            //LOG.info("onDataAvailable doWork finish for " + testToRun);
        } else {
            if (testToRun == null) {
                testToRun = this.readLen(request, 64, true);
                LOG.info("onDataAvailable doWork first read done of 64 and start work for " + testToRun);
            }

            if (testToRun != null) {
                if (!testToRun.startsWith("F003449Test=")) {
                    responseString += " : Fail post data :" + testToRun + ": does not start F003449Test=";
                    pass = false;
                } else {
                    testToRun = testToRun.substring(12);

                    int iterIndex = testToRun.indexOf("Num=") + 4;
                    if (iterations <= 1) {
                        // int iterations = 1;
                        if (iterIndex > 3) {
                            iterations = Integer.parseInt(testToRun.substring(iterIndex, iterIndex + 1));
                        }
                    }

                    int lenIndex = testToRun.indexOf("Len=") + 4;
                    int len = 0;
                    if (lenIndex > 3) {
                        len = Integer.parseInt(testToRun.substring(lenIndex, lenIndex + 2));
                    }

                    if (testToRun.startsWith("Large")) {
                        if (responseString == null) {
                            responseString = " : Large Test postDataLen = " + postDataLen + ", interations = " + iterations;
                        } else
                            responseString += " : Large Test postDataLen = " + postDataLen + ", interations = " + iterations;
                        System.out.println("Large responseString ->" + responseString);

                        for (int i = 0; (i < iterations) && pass; i++) {
                            ServletInputStream input = request.getInputStream();
                            readLargeInputStream(input, true);
                            if (dataSize == postDataLen) {
                                responseString += " : Successful read of post data on read " + (i + 1);
                                pass = true;
                                input.close();
                            }
                        }

                        ServletOutputStream out = response.getOutputStream();
                        if (pass)
                            out.println("PASS : " + responseString);
                        else
                            out.println("FAIL : " + responseString);

                        ac.complete(); // cannot call onAllDataRead
                        LOG.info("onDataAvailable doWork finish for " + testToRun);

                    } else if (testToRun.startsWith("Skip")) {
                        if (responseString == null) {
                            responseString = " : Skip Test postDataLen = " + postDataLen + ", interations = " + iterations + ", skipLen = " + len;
                        } else
                            responseString += " : Skip Test postDataLen = " + postDataLen + ", interations = " + iterations + ", skipLen = " + len;

                        System.out.println("Skip responseString ->" + responseString);

                        for (int i = 0; (i < iterations) && pass; i++) {
                            String data = this.skipAndReadAll(request, len, true);
                            if (!checkData(data, postDataLen - len)) {
                                pass = false;
                            }
                        }

                        ServletOutputStream out = response.getOutputStream();
                        if (pass)
                            out.println("PASS : " + responseString);
                        else
                            out.println("FAIL : " + responseString);
                        ac.complete(); // cannot call onAllDataRead
                        LOG.info("onDataAvailable doWork finish for " + testToRun);
                    } else if (testToRun.startsWith("ReadAsBytes")) {
                        responseString += " : ReadAsBytes Test postDataLen = " + postDataLen + ", interations = " + iterations;
                        System.out.println(responseString);
                        for (int i = 0; (i < iterations) && pass; i++) {
                            String data = this.readAllAsBytes(request, true);
                            if (!checkData(data, postDataLen)) {
                                pass = false;
                            }
                        }
                        ServletOutputStream out = response.getOutputStream();
                        if (pass)
                            out.println("PASS : " + responseString);
                        else
                            out.println("FAIL : " + responseString);
                        ac.complete(); // cannot call onAllDataRead
                        LOG.info("onDataAvailable doWork finish for " + testToRun);
                    } else if (testToRun.startsWith("ReadAsByteArrays")) {
                        responseString += " : ReadAsByteArrays Test postDataLen = " + postDataLen + ", interations = " + iterations;
                        System.out.println(responseString);
                        for (int i = 0; (i < iterations) && pass; i++) {
                            String data = this.readAllAsByteArrays(request, postDataLen, true);
                            if (!checkData(data, postDataLen)) {
                                pass = false;
                            }
                        }
                        ServletOutputStream out = response.getOutputStream();
                        if (pass)
                            out.println("PASS : " + responseString);
                        else
                            out.println("FAIL : " + responseString);
                        ac.complete();
                        LOG.info("onDataAvailable doWork finish for " + testToRun);
                    } else if (testToRun.startsWith("Mixit")) {
                        responseString += " : Mixit Test postDataLen = " + postDataLen + ", interations = " + iterations;
                        System.out.println(responseString);

                        for (int i = 0; (i < iterations) && pass; i++) {

                            String data = null, totalData = null;
                            if (i == 0) {
                                responseString += " : ReadDLen past end";
                                System.out.println(responseString);
                                dataSizeofLen = 0;
                                data = this.readLen(request, 16, false); // getStream, not closed here
                                if (data != null) {
                                    while (data.length() != 0) {
                                        System.out.println("len : " + data.length());
                                        dataSizeofLen = 0;
                                        totalData += data;
                                        data = this.readLen(request, 16, false); //getStream, not closed here
                                    }
                                }
                                dataSizeofLen = 0;
                                totalData += this.readLen(request, 16, true); //getStream, closed here
                            } else if (i == 1) {
                                responseString += " : Skip and read twice";
                                System.out.println(responseString);
                                totalData = this.skipAndReadAll(request, len, false);
                                totalData += this.skipAndReadAll(request, len, true);
                                pass = checkData(totalData, postDataLen - len);
                            } else if (i == 2) {
                                responseString += " : Read as bytes, skip and read";
                                System.out.println(responseString);
                                totalData = this.readAllAsBytes(request, false);
                                totalData += this.skipAndReadAll(request, len, true);
                                pass = checkData(totalData, postDataLen);
                            } else if (i > 2) {
                                responseString += " : Read len then as byteArreay then as len";
                                System.out.println(responseString);
                                dataSizeofLen = 0;
                                totalData = this.readLen(request, 16, false);
                                dataSizeofLen = 0;
                                totalData += this.readAllAsByteArrays(request, len, false);
                                totalData += this.readLen(request, 16, true);
                                dataSizeofLen = 0;
                                pass = checkData(totalData, postDataLen);
                            }
                        }
                        ServletOutputStream out = response.getOutputStream();
                        if (pass)
                            out.println("PASS : " + responseString);
                        else
                            out.println("FAIL : " + responseString);
                        ac.complete();
                        LOG.info("onDataAvailable doWork finish for " + testToRun);
                    } else if (testToRun.startsWith("Available")) {

                        responseString += " : Avaiable Test postDataLen = " + postDataLen + ", interations = " + iterations;
                        System.out.println(responseString);

                        for (int i = 0; (i < iterations) && pass; i++) {
                            System.out.println("Call readAvailable");
                            int availNum = readAvailable(request, false);

                            if (availNum != postDataLen) {
                                responseString += " : Fail available " + availNum + " is not the postDatalen";
                                System.out.println(responseString);
                                pass = false;
                            } else {

                                dataSizeofLen = 0; // since we already called readLen before , need to reset this
                                this.readLen(request, 24, false);
                                System.out.println("Call readAvailable");
                                availNum = readAvailable(request, true);
                                if (availNum != (postDataLen - 24)) {
                                    responseString += " : Fail available " + availNum + " is not the postDatalen - 24";
                                    System.out.println(responseString);
                                    pass = false;
                                }
                            }
                        }
                        ServletOutputStream out = response.getOutputStream();
                        if (pass)
                            out.println("PASS : " + responseString);
                        else
                            out.println("FAIL : " + responseString);

                        ac.complete();
                        LOG.info("onDataAvailable doWork finish for " + testToRun);

                    }
                }
            }

        }

    }

    private boolean checkData(String data, int expectedLen) {
        boolean result = true;

        if (data.length() != expectedLen) {
            responseString += " : Fail insufficent post data on read, expected " + expectedLen + ", got " + data.length();
            System.out.println(responseString);
            result = false;
        } else {
            String checkData = data.substring(data.indexOf("Data=") + 5);
            int checkStringLen = new String("TestingTesting123").length();
            for (int i = 0; (i < (checkData.length() - 1)) && result; i += checkStringLen) {
                if (!checkData.substring(i).startsWith("TestingTesting123")) {
                    int dataRemaining = Math.min(checkData.length() - i, checkStringLen);
                    responseString += " : Fail post data content wrong at data index " + i + ", data was : " + checkData.substring(i, i + dataRemaining);
                    System.out.println(responseString);
                    result = false;
                }
            }
        }
        if (result) {
            responseString += " : Succesful read of data";
            System.out.println(responseString);
        }
        return result;
    }

    private String readAllAsByteArrays(HttpServletRequest request, int expectedLen, boolean close) {
        int n = -1;
        StringBuffer inBuffer = new StringBuffer();

        try {
            ServletInputStream in = request.getInputStream();

            byte[] inBytes = new byte[32];

            while (in.isReady() && (n = in.read(inBytes, 0, 32)) != -1) {
                inBuffer.append(new String(inBytes, 0, n));
            }
//            for (int n; (n = in.read(inBytes, 0, 32)) != -1;) {
//                inBuffer.append(new String(inBytes, 0, n));
//            }
            if (close)
                in.close();
        } catch (IOException exc) {
            responseString += "Fail Exception reading post data " + exc + " :";
            System.out.println(responseString);
        }

        return inBuffer.toString();

    }

    private String readAllAsBytes(HttpServletRequest request, boolean close) {
        int n = -1;
        StringBuffer inBuffer = new StringBuffer();

        try {
            ServletInputStream in = request.getInputStream();
            while (in.isReady() && (n = in.read()) != -1) {
                inBuffer.append((char) n);
            }
            if (close)
                in.close();
        } catch (IOException exc) {
            responseString += "Exception reading post data " + exc + " :";
            System.out.println(responseString);
        }

        return inBuffer.toString();
    }

    private String readLen(HttpServletRequest request, int Len, boolean close) throws IOException {
        int LenToRead = Len;
        StringBuffer inBuffer = new StringBuffer();
        try {
            byte[] inBytes = new byte[(int) (Len - dataSizeofLen)];

            ServletInputStream input = request.getInputStream();

            if (input.isReady()) {
                Len = input.read(inBytes);
                if (Len > 0) {
                    inBuffer.append(new String(inBytes, 0, Len));
                    dataSizeofLen += Len;
                }
            }
        } catch (IOException exc) {
            responseString += "Exception reading post data " + exc + " :";
            System.out.println(responseString);
        }

        if (dataSizeofLen <= LenToRead) {
            System.out.println("dataSizeof 16 completed , now close " + close);
            if (close)
                input.close();
            pass = true;
            return inBuffer.toString();
        }
        return null;
    }

    /**
     * @param request
     * @param skipLen
     * @param close
     * @return
     */
    private String skipAndReadAll(HttpServletRequest request, long skipLen, boolean close) {

        int n = -1;
        StringBuffer inBufferForSkip = new StringBuffer();
        try {
            ServletInputStream in = request.getInputStream();

            //only want to skip first time , if async we may be here second time
            if (in.isReady() && inBufferForSkip.length() == 0) {
                in.skip(skipLen);
            }
            while (in.isReady() && (n = in.read()) != -1) {
                inBufferForSkip.append((char) n);
            }
            if (close)
                in.close();
        } catch (IOException exc) {
            responseString += "Exception reading post data " + exc + " :";
            System.out.println(responseString);
        }
        return inBufferForSkip.toString();
    }

    private void readLargeInputStream(ServletInputStream input, boolean close) throws IOException {

        int len = -1;
        try {
            //ServletInputStream input = request.getInputStream();
            byte[] inBytes = new byte[2048];

            while (input.isReady() && (len = input.read(inBytes)) != -1) {
                dataSize += len;
                System.out.println("dataSize -->" + dataSize);
            }
        } catch (IOException exc) {
            responseString += "Exception reading post data " + exc + " :";
            System.out.println(responseString);
        }
    }

    private int readAvailable(HttpServletRequest request, boolean close) {

        int availLen = 0;

        try {
            ServletInputStream in = request.getInputStream();
            if (in.isReady()) {
                System.out.println("Call available");
                availLen = in.available();
                System.out.println("available -> " + availLen);
            }
            if (close)
                in.close();
        } catch (IOException exc) {
            responseString += "Exception reading post data " + exc + " :";
            System.out.println(responseString);
        }
        return availLen;
    }

    private void getParamNotAllowedReadStream() {

        passCheck = getParamWork(); // expect false as parameters must be null
        try {
            if (!passCheck) {
                LOG.info("onDataAvailable expected output for getParameter, now read the stream for " + testToPerfom);
                // now read the stream
                int cl = request.getContentLength();
                readInputStream(input, cl);

            } else {
                responseString = "Fail";
                pass = false;
            }

            if (testFinished) {
                ServletOutputStream out = response.getOutputStream();
                if (pass) {
                    LOG.info("onDataAvailable expected output for " + testToPerfom);
                    out.println("PASS : " + responseString);
                } else {
                    out.println("FAIL : " + responseString);
                }
            }
        } catch (IOException exc) {
            System.out.println("execption -->" + exc);
            responseString += "Fail from NBReadParameterServletReadStream";
        }
    }

    private void getParamNotAllowed() throws IOException {

        // async RL getParameter (not allowed ) close , sync getInputStream read (allowed ) close
        passCheck = getParamWork(); // expect false as parameters must be null

        ServletOutputStream out = response.getOutputStream();
        if (!passCheck) {
            LOG.info("onDataAvailable expected output for " + testToPerfom);
            out.println("PASS : " + responseString);
        } else {
            out.println("FAIL : " + responseString);
        }
        LOG.info("onDataAvailable doWork finish for " + testToPerfom);

        //since we didnt read anything we will have to complete the ac context since onAlldataRead wil not be called , confirm with Chris
        ac.complete();
    }

    // This will read the stream
    private void readInputStream(ServletInputStream input, int cl) {

        try {
            int len = -1;
            StringBuffer inBuffer = new StringBuffer();
            byte[] inBytes = new byte[1024];
            System.out.println("Start redaing using in -->" + input + " nned cl -->" + cl);

            while (input.isReady() && (len = input.read(inBytes)) != -1) {
                inBuffer.append(new String(inBytes, 0, len));
                dataSize += len;
                System.out.println("dataSize -->" + dataSize);
            }

            if (dataSize == cl) {
                LOG.info("onDataAvailable expected output when read the stream for " + testToPerfom);

                if (testToPerfom.equalsIgnoreCase("ReadPostDataFromNBInputStreamFilter_NoWorkServlet")) {
                    if (inBuffer.indexOf("ReadPostDataFromNBInputStreamFilter_NoWorkServlet") != -1) {
                        responseString += "Pass from ReadPostDataFromNBInputStreamFilter_NoWorkServlet :";
                        pass = true;
                    } else {
                        responseString += "Fail from ReadPostDataFromNBInputStreamFilter_NoWorkServlet - post data = " + inBuffer + " :";
                        pass = false;
                    }
                } else if (testToPerfom.equalsIgnoreCase("ReadParameterFilter_ReadPostDataFromInputStreamServlet")) {
                    if (inBuffer.indexOf("ReadParameterFilter_ReadPostDataFromInputStreamServlet") != -1) {
                        responseString += "Pass from ReadParameterFilter_ReadPostDataFromInputStreamServlet :";
                        pass = true;
                    } else {
                        responseString += "Fail from ReadParameterFilter_ReadPostDataFromInputStreamServlet - post data = " + inBuffer + " :";
                        pass = false;
                    }
                } else if (testToPerfom.equalsIgnoreCase("getParamNotAllowedReadStream")) {
                    if (inBuffer.indexOf("NBReadParameterServletReadStream") != -1) {
                        responseString += "Pass from NBReadParameterServletReadStream :";
                        pass = true;
                    } else {
                        responseString += "Fail from NBReadParameterServletReadStream - post data = " + inBuffer + " :";
                        pass = false;
                    }
                } else if (testToPerfom.equalsIgnoreCase("ReadParameterNBReadFilter_NoWorkServlet")) {
                    if (inBuffer.indexOf("ReadParameterNBReadFilter_NoWorkServlet") != -1) {
                        responseString += "Pass from ReadParameterNBReadFilter_NoWorkServlet :";
                        pass = true;
                    } else {
                        responseString += "Fail from ReadParameterNBReadFilter_NoWorkServlet - post data = " + inBuffer + " :";
                        pass = false;
                    }
                } else {
                    if (inBuffer.indexOf("NBReadPostDataFromInputStreamServlet") != -1) {
                        responseString += "Pass from NBReadPostDataFromInputStreamServlet :";
                        pass = true;
                    } else {
                        responseString += "Fail from NBReadPostDataFromInputStreamServlet - post data = " + inBuffer + " :";
                        pass = false;
                    }
                }
                testFinished = true;

                System.out.println("responseString -->" + responseString + " ,for " + testToPerfom);

                //input.close(); //cannot close here if we want onAllDataRead be called
            } else {
                System.out.println("Hoping to read more when available");
            }

        } catch (IOException exc) {
            System.out.println("execption -->" + exc);
            responseString += " Fail ,for " + testToPerfom;
        }
    }

    private boolean getParamWork() {

        boolean passed = false;

        String attr = (String) request.getAttribute("F003449Filter");
        String uri = request.getRequestURI();

        responseString += "URI : " + uri + " : ";

        if (attr == null) {
            if (uri.indexOf("Nofilter") != -1) {
                responseString += " Pass - No Filter : ";
            } else {
                responseString += " Fail - No Filter but filter expected : ";
                passed = false;
            }
        } else {
            if (attr.indexOf("Fail") != -1) {
                responseString += "Fail from Filter - " + attr + " : ";
                passed = false;
            } else {
                responseString += "Pass from Filter - " + attr + " : ";
            }
        }

        String param = request.getParameter("F003449Test");

        if (param == null) {
            responseString += "Fail from ReadParameterServlet - parameter not available :";
            passed = false;
        } else {
            if (param.indexOf("ReadParameterServlet") != -1) {
                responseString += "Pass from ReadParameterServlet :";
            } else {
                responseString += "Fail from ReadParameterServlet - parameter = " + param + " :";
                passed = false;
            }
        }

        return passed;
    }
}
