/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.srt;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.http.channel.inputstream.HttpInputStreamConnectWeb;
import com.ibm.ws.http.channel.inputstream.HttpInputStreamObserver;
import com.ibm.wsspi.webcontainer.logging.LoggerFactory;
import com.ibm.wsspi.webcontainer.util.WSServletInputStream;

//LIBERTY - Switched from HttpInputStream to SRTInputStream to avoid IP issues

public class SRTInputStream extends WSServletInputStream
{
  protected InputStream in;
  protected HttpInputStreamConnectWeb inStream = null;
  
  protected long contentLength;
  //private static TraceNLS nls = TraceNLS.getTraceNLS(SRTInputStream.class, "com.ibm.ws.webcontainer.resources.Messages");
  private static final String CLASS_NAME="com.ibm.ws.webcontainer.srt.SRTInputStream";
  protected static final Logger logger = LoggerFactory.getInstance().getLogger(CLASS_NAME);


  @Override
  public void finish() throws IOException
  {
    this.in.close();
  }

  @Override
  public void init(InputStream in) throws IOException
  {
    this.in = in;
    if (in != null && in instanceof HttpInputStreamConnectWeb) {
        this.inStream = (HttpInputStreamConnectWeb) in;
        if (TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME,"init", "set up for "+this + " ,in"+ this.inStream);
        }
    }
    else if(in == null){
        this.inStream = null;
    }
  }

  @Override
  public void setContentLength(long contentLength)
  {
    this.contentLength = contentLength;
  }

  @Override
  public int read() throws IOException
  {
    return this.in.read();
  }

  @Override
  public int read(byte[] output) throws IOException {
      
      return this.in.read(output, 0, output.length);
  }

  @Override
  public int read(byte[] output, int offset, int length) throws IOException {
  
      return this.in.read(output, offset, length);
  }
 
  //Following needed to support MultiRead
  
  @Override
  public void close() throws IOException
  {
      if(this.in != null && this.inStream != null){
          if (TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {
              logger.logp(Level.FINE, CLASS_NAME,"close", "close called->"+this);
          }
          this.in.close();
      }
      else{
          super.close();
      }
  }
  
  @Override
  public long skip(long n) throws IOException {
      
      if(this.in != null && this.inStream != null ){          
          if (TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {
              logger.logp(Level.FINE, CLASS_NAME,"skip", "skip called->"+this);
          }
          return this.in.skip(n);
      }
      else {
          return super.skip(n);
      }
  }
  
  @Override
  public int available() throws IOException {
      if(this.in != null && this.inStream != null ){
          if (TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {
              logger.logp(Level.FINE, CLASS_NAME,"available", "available called->"+this);
          }
          return this.in.available();
      }
      else {
          return super.available();
      }
  }
  
  /**
   * @return the inStream
   */
  public InputStream getInStream() {
      return inStream;
  }

  public void restart() {
      if(this.inStream != null){
          if (TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {
              logger.logp(Level.FINE, CLASS_NAME,"restart", "SRTInputStream: Start re-read of data"+this);
          }
          this.inStream.restart();
      }
  }

  public void setupforMultiRead(boolean set){
      if(this.inStream != null){
          if (TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {
              logger.logp(Level.FINE, CLASS_NAME,"setISObserver", "set up for Multiread"+this);
          }
          this.inStream.setupforMultiRead(set);
      }
  }

  public void cleanupforMultiRead(){
      if(this.inStream != null) {
          if (TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {
              logger.logp(Level.FINE, CLASS_NAME,"cleanupforMultiRead", "clean up for Multiread"+this);
          }
          this.inStream.cleanupforMultiRead();
      }
  }

  public void setISObserver(HttpInputStreamObserver obs){
      if(this.inStream != null){          
          this.inStream.setISObserver(obs);
      }
  }
  


  
}
