/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package response.servlets;

import java.io.IOException;

import javax.servlet.annotation.WebServlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/*
 * Test response Content-Length VS Chunked when using Writer and OutputStream.
 * DBCS, setBufferSize, data Size (over/under buffersize) are also tested.
 * Content is wrapped in prefix "BEGIN_" and postfix "_END"; it is easier to assert on these.
 * 
 *          data_size=2000              1000 (default)
 *          multibyte=true              test DBCS multiplebyte string instead of ASCII (default). 
 *          multisend=true              sending multiple data set : first data_set > explicit flush > 2nd data_set. 
 *          write_type=out              Writing use getOutputStream instead of getWriter (default)
 *          force_flush=true            response.flushBuffer() after writing
 *          buffer_size=8192            4096 (default)
 *          set_contentlength=true      set CL to the "encoded" length.
 *  Example: 
 *      /contextRoot/responechunkedencoding?
 *      /contextRoot/responechunkedencoding?data_size=3000
 *      /contextRoot/responechunkedencoding?data_size=3000&multibyte=true
 *      /contextRoot/responechunkedencoding?data_size=3000&write_type=out
 *      /contextRoot/responechunkedencoding?data_size=3000&write_type=out&buffer_size=8192&multi_send=true&multibyte=true
 *      /contextRoot/responechunkedencoding?data_size=3000&write_type=out&force_flush=true&buffer_size=8192&multi_send=true&multibyte=true
 */

@WebServlet(urlPatterns = { "/responechunkedencoding/*" })
public class ResponseContentLengthChunkServlet extends HttpServlet {
    private static final String CLASS_NAME = ResponseContentLengthChunkServlet.class.getName();
    ServletConfig servletConfig;
    String seed1K; // default seed 1 Kbytes of "0123456789" x 100 

    //3 bytes character in UTF-8 "Both sushi and tempura are delicious."
    private static final String DBCS_DATA = "すしもてんぷらもおいしい。";

    public ResponseContentLengthChunkServlet() {
        super();
    }

    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        servletConfig = config;
        String seed = "0123456789"; // 10bytes
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < 100; i++)
            sb.append(seed);

        seed1K = sb.toString();
    }

    public void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        doGet(req, res);
    }

    public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        _LOG("doGET ENTRY");
        _LOG(">>>>>>>>>> Testing: [" + req.getQueryString() + "] <<<<<<<<<<");
        String temp;
        StringBuilder content = new StringBuilder();
        String secondSetData = "_SECOND_SET_DATA_END";

        int contentByteLength;

        //Parse and setup all test params
        int bufferSizeParm = ((temp = req.getParameter("buffer_size")) != null) ? Integer.parseInt(temp) : -1;
        int size = ((temp = req.getParameter("data_size")) != null) ? Integer.parseInt(temp) : 1000;
        boolean forceFlush = req.getParameter("force_flush") != null;
        boolean multipleBytes = req.getParameter("multibyte") != null;
        boolean multisend = req.getParameter("multi_send") != null;
        boolean outputStream = req.getParameter("write_type") != null;
        boolean setContentLengthParam = req.getParameter("set_contentlength") != null;

        // Set character encoding
        String encoding = "UTF-8";
        res.setCharacterEncoding(encoding);
        res.setContentType("text/plain; charset=" + encoding);

        //Setup response buffer size
        if (bufferSizeParm > 0) {
            res.setBufferSize(bufferSizeParm);
        }
        _LOG("Buffer Size : " + res.getBufferSize());

        //Setup data: either DBCS or repeating asci "0123456789"
        _LOG("Prepare data size [" + size + "]. Append prefix BEGIN_");
        content.append("BEGIN_"); // 6 bytes prefix

        int dataLength = multipleBytes ? DBCS_DATA.length() : seed1K.length();
        int fullRepeats = size / dataLength;
        int remainder = size % dataLength;

        if (multipleBytes) {
            _LOG("DBCS (3 bytes character) data length [" + DBCS_DATA.length() + "]");
            for (int i = 0; i < fullRepeats; i++) {
                content.append(DBCS_DATA);
            }
            if (remainder > 0) {
                content.append(DBCS_DATA.substring(0, remainder));
            }
        } else {
            _LOG("ASCII data with seed length [" + seed1K.length() + "]");
            for (int i = 0; i < fullRepeats; i++) {
                content.append(seed1K);
            }
            if (remainder > 0) {
                content.append(seed1K.substring(0, remainder));
            }
        }
        content.append("_END"); // 4 bytes postfix . assert this string in the response data. Size has 10 bytes extra for BEGIN_ and _END
        _LOG("Append postfix _END . Total String length [" +  content.length() + "] . Actual byte[] length in UTF-8 [" +  (contentByteLength = content.toString().getBytes(encoding).length) + "]");
        
        if (setContentLengthParam){
            _LOG("Set Content-Length to byte length " + contentByteLength);
            res.setContentLength(contentByteLength);
        }

        //Obtain a writing type and write out data. 
        //Intentionally call getWriter/getOutputStream multiple time to see them in the trace
        if (outputStream){
            _LOG("Writing Type: OutputStream");
            res.getOutputStream().write(content.toString().getBytes("UTF-8")); 
            if (multisend){
                _LOG("Writing: OutputStream flush then write 2nd data set");
                res.flushBuffer();
                res.getOutputStream().write(secondSetData.getBytes("UTF-8")); 
            }
        }
        else {
            _LOG("Writing Type: Writer");
            res.getWriter().write(content.toString());
            if (multisend){
                _LOG("Writing: Writer flush then write 2nd data set");
                res.flushBuffer();
                res.getWriter().write(secondSetData);
            }
        }

        if (forceFlush) {
            _LOG("Force flushBuffer() ...");
            res.flushBuffer();
        }
        _LOG("doGET RETURN");
    }

    private void _LOG(String s) {
        System.out.println(CLASS_NAME + " === " + s);
    }
}
