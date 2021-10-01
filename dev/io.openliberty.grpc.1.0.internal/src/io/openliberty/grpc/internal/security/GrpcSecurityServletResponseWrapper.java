/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.grpc.internal.security;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
/**
 * To override the security committed response.
 *
 */
public class GrpcSecurityServletResponseWrapper extends HttpServletResponseWrapper{
    
    private GrpcSecurityOutputWriter  writer      = null;
    private ServletOutputStream stream = null;

    public GrpcSecurityServletResponseWrapper(HttpServletResponse response) throws IOException {
        super(response);		
    }
    
      @Override
      public PrintWriter getWriter() throws IOException {
         if (this.writer == null && this.stream != null) {
           throw new IllegalStateException(
             "SecurityServletResponseWrapper: OutputStream obtained already - cannot get PrintWriter");
         }
         if(this.writer == null) {
             writer = new GrpcSecurityOutputWriter(getResponse().getOutputStream());
         }
         return this.writer;
      }

      @Override
      public ServletOutputStream getOutputStream() throws IOException {
        if (this.writer != null) {
          throw new IllegalStateException(
            "SecurityServletResponseWrapper: PrintWriter obtained already - cannot get OutputStream");
        }
        if (this.stream == null) {
          this.stream = 
            getResponse().getOutputStream();
        }
        return this.stream;
      }
    

      
      @Override
      public void flushBuffer() throws IOException {

        if(this.writer != null) {
          this.writer.flush();
        }

        IOException excep = null;
        try{
          if(this.stream != null) {
            this.stream.flush();
          }
        } catch(IOException e) {
            excep = e;
        }

        IOException excep1 = null;
        try {
          super.flushBuffer();
        } catch(IOException e){
          excep1 = e;
        }

        if(excep != null) throw excep;
        if(excep1 != null) throw excep1;
      }

      
}

