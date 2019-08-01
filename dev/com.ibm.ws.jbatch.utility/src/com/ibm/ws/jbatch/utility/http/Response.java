/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jbatch.utility.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.util.List;

import com.ibm.ws.jbatch.utility.utils.IOUtils;
import com.ibm.ws.jbatch.utility.utils.ObjectUtils;

public class Response {

    /**
     * The connection from which to read the response.
     */
    private HttpURLConnection con;
    
    /**
     * CTOR.
     */
    public Response(HttpURLConnection con) {
        this.con = con;
    }

    /**
     * Use the provided EntityReader to read and parse the response.
     * 
     * @return the entity as read by the given entityReader.
     */
    public <T> T readEntity(EntityReader<T> entityReader) throws IOException {
        return entityReader.readEntity( getInputStream() );
    }
    
    /**
     * @return HttpURLConnection.getInputStream()
     */
    public InputStream getInputStream() throws IOException {
        try {
            return con.getInputStream();
        } catch (IOException e) {
            handleFailureResponse(e);
            throw e;
        }
    }
    
    /**
     * Read any error response from the connection and include it in the thrown exception.
     * 
     * @throws IOException
     */
    protected void handleFailureResponse(IOException e) throws IOException {
        List<String> errorResponse = new StringEntityReader().readEntity( con.getErrorStream() );
        if (errorResponse.isEmpty()) {
            throw e;    // No additional error info.
        } else {
            throw new IOException( e.getMessage() + ": " + errorResponse.toString(), e);
        }
    }

    /**
     * @return HttpURLConnection.getHeaderField()
     */
    public String getHeader(String name) {
        return con.getHeaderField(name);
    }
    
    /**
     * Write the response to the given OutputStream.
     * 
     * If the response is plain text, be mindful of char encoding.
     * If no char-encoding is specified, default to UTF-8.
     * 
     * @param outputStream write the response to this stream
     */
    public void copyToStream( OutputStream outputStream) throws IOException {
        
        // Note: must get the input stream *before* checking the content-type, 
        // in case the Response is an HTTP-302 redirect.  If it is, then getInputStream
        // will automatically follow the redirect.
        InputStream responseStream = getInputStream();
        
        String contentType = getHeader("Content-Type");
        
        if ( contentType != null && contentType.contains("text/plain") ) {
            // Plain text response.  Handle the char encoding.
            String charsetName = ObjectUtils.firstNonNull( HttpUtils.parseHeaderParameter(contentType, "charset"), "UTF-8");
            
            // The OutputStreamWriter deliberately uses the default platform encoding because,
            // well, what else should we use?
            IOUtils.copyReader( new InputStreamReader(responseStream, charsetName),
                                new OutputStreamWriter(outputStream) );
        } else {
            // Binary response.  Copy as-is.
            IOUtils.copyStream( responseStream, outputStream );
        }
    }
            
}
