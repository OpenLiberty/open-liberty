/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.transport.http.inactivity.timeout.servlet;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet that writes a large response body in a single flush to trigger a write timeout
 * when the client is not reading.
 *
 * A single large os.write() + os.flush() is used intentionally so that the entire payload
 * is handed to the transport layer as one operation. On the Netty path this translates to a
 * single writeAndFlush whose promise stays pending until the OS send buffer drains — giving
 * the WriteTimeoutHandler a sustained stall to fire against. On the ChannelFW path the single
 * synchWrite() call blocks until the OS accepts the data, where the write inactivity selector
 * fires the SocketTimeoutException.
 *
 * The response is 40 MB so that even with macOS TCP receive-buffer auto-tuning
 * (autorcvbufmax=4 MB) the OS cannot absorb the full payload when the client stops reading.
 */
@WebServlet("/SlowWriteServlet")
public class SlowWriteServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    /** 40 MB — exceeds macOS autorcvbufmax (4 MB) so the write will stall when client stops reading. */
    private static final int RESPONSE_SIZE_BYTES = 40 * 1024 * 1024;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/octet-stream");
        response.setContentLength(RESPONSE_SIZE_BYTES);

        OutputStream os = response.getOutputStream();
        byte[] body = new byte[RESPONSE_SIZE_BYTES];
        Arrays.fill(body, (byte) 'X');

        try {
            System.out.println("[FAT_DEBUG] SlowWriteServlet: writing " + RESPONSE_SIZE_BYTES + " bytes in one shot");
            os.write(body);
            os.flush();
            System.out.println("[FAT_DEBUG] SlowWriteServlet: finished writing all " + RESPONSE_SIZE_BYTES + " bytes");
        } catch (IOException e) {
            System.out.println("[FAT_DEBUG] SlowWriteServlet: write failed - " + e);
        }
    }
}
