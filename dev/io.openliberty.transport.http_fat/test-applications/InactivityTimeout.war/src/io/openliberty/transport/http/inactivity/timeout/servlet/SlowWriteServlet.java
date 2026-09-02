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
 * Servlet that writes a large response body to trigger a write timeout when
 * the client is not reading. The response is 4 MB, which is large enough to
 * fill typical TCP socket receive buffers and stall the server's write.
 */
@WebServlet("/SlowWriteServlet")
public class SlowWriteServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    /** 4 MB — large enough to fill typical socket receive buffers (~256 KB) and stall the write. */
    private static final int RESPONSE_SIZE_BYTES = 4 * 1024 * 1024;

    /** Chunk size for each individual write call. */
    private static final int CHUNK_SIZE = 32 * 1024;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/octet-stream");
        response.setContentLength(RESPONSE_SIZE_BYTES);

        OutputStream os = response.getOutputStream();
        byte[] chunk = new byte[CHUNK_SIZE];
        Arrays.fill(chunk, (byte) 'X');

        int remaining = RESPONSE_SIZE_BYTES;
        int chunkNum = 0;
        try {
            while (remaining > 0) {
                int toWrite = Math.min(CHUNK_SIZE, remaining);
                os.write(chunk, 0, toWrite);
                os.flush();
                remaining -= toWrite;
                chunkNum++;
                System.out.println("[FAT_DEBUG] SlowWriteServlet: wrote chunk " + chunkNum + ", remaining=" + remaining);
            }
            System.out.println("[FAT_DEBUG] SlowWriteServlet: finished writing all " + RESPONSE_SIZE_BYTES + " bytes");
        } catch (IOException e) {
            System.out.println("[FAT_DEBUG] SlowWriteServlet: write failed after " + chunkNum + " chunks - " + e);
        }
    }
}
