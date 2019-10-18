/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cxf.transport.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.net.HttpURLConnection;

import org.apache.cxf.helpers.HttpHeaderHelper;
// Added this file changing it's access modifier to public because it was used from a different package 
// for issue #8827 Create com.ibm.ws.jaxws.2.3.clientcontainer. 
final public class ChunkedUtil {
    private ChunkedUtil() {
    }

    /**
     * Get an input stream containing the partial response if one is present.
     *
     * @param connection the connection in question
     * @param responseCode the response code
     * @return an input stream if a partial response is pending on the connection
     */
    public static InputStream getPartialResponse(
        HttpURLConnection connection,
        int responseCode
    ) throws IOException {
        InputStream in = null;
        if (responseCode == HttpURLConnection.HTTP_ACCEPTED
            || responseCode == HttpURLConnection.HTTP_OK) {
            if (connection.getContentLength() > 0) {
                in = connection.getInputStream();
            } else if (hasChunkedResponse(connection)
                       || hasEofTerminatedResponse(connection)) {
                // ensure chunked or EOF-terminated response is non-empty
                in = getNonEmptyContent(connection);
            }
        }
        return in;
    }

    /**
     * @param connection the given HttpURLConnection
     * @return true iff the connection has a chunked response pending
     */
    private static boolean hasChunkedResponse(HttpURLConnection connection) {
        return HttpHeaderHelper.CHUNKED.equalsIgnoreCase(
                   connection.getHeaderField(HttpHeaderHelper.TRANSFER_ENCODING));
    }

    /**
     * @param connection the given HttpURLConnection
     * @return true iff the connection has a chunked response pending
     */
    private static boolean hasEofTerminatedResponse(
        HttpURLConnection connection
    ) {
        return HttpHeaderHelper.CLOSE.equalsIgnoreCase(
                   connection.getHeaderField(HttpHeaderHelper.CONNECTION));
    }

    /**
     * @param connection the given HttpURLConnection
     * @return an input stream containing the response content if non-empty
     */
    private static InputStream getNonEmptyContent(
        HttpURLConnection connection
    ) {
        InputStream in = null;
        try {
            PushbackInputStream pin =
                new PushbackInputStream(connection.getInputStream());
            int c = pin.read();
            if (c != -1) {
                pin.unread((byte)c);
                in = pin;
            }
        } catch (IOException ioe) {
            // ignore
        }
        return in;
    }
}
