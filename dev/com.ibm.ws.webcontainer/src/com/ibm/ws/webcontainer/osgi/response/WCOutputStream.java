/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.osgi.response;

import java.io.IOException;

import javax.servlet.ServletOutputStream;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.http.channel.outstream.HttpOutputStreamConnectWeb;
import com.ibm.ws.webcontainer.osgi.osgi.WebContainerConstants;

/**
 * Servlet stream wrapper around a regular IO output stream instance.
 */
public class WCOutputStream extends ServletOutputStream
{

  /** Hardcoded CRLF for println usage */
  private static final byte[] CRLF = new byte[] { 0x0D, 0x0A };

  protected HttpOutputStreamConnectWeb output = null;
  private byte[] singleByte = new byte[1];
  boolean closed = false;
  
  protected static final TraceNLS nls = TraceNLS.getTraceNLS(WCOutputStream.class, "com.ibm.ws.webcontainer.resources.Messages");
  
  private static final TraceComponent tc = Tr.register(WCOutputStream.class, WebContainerConstants.TR_GROUP, WebContainerConstants.NLS_PROPS);

  /**
   * Constructor.
   * 
   * @param httpOutputStreamConnectWeb
   */
  public WCOutputStream(HttpOutputStreamConnectWeb httpOutputStreamConnectWeb)
  {
      this.setOutput(httpOutputStreamConnectWeb);
  }

  /*
   * @see java.lang.Object#toString()
   */
  public String toString()
  {
    StringBuilder sb = new StringBuilder(getClass().getSimpleName());
    sb.append('@').append(Integer.toHexString(hashCode()));
    sb.append(" output=").append(this.output);
    return sb.toString();
  }

  /*
   * @see javax.servlet.ServletOutputStream#print(boolean)
   */
  public void print(boolean b) throws IOException
  {
    String value = Boolean.toString(b);
    this.output.write(value.getBytes(), 0, value.length());    
  }

  /*
   * @see javax.servlet.ServletOutputStream#print(char)
   */
  public void print(char c) throws IOException
  {
    this.singleByte[0] = (byte) c;
    this.output.write(this.singleByte, 0, 1);
  }

  /*
   * @see javax.servlet.ServletOutputStream#print(double)
   */
  public void print(double d) throws IOException
  {
    String value = Double.toString(d);
    this.output.write(value.getBytes(), 0, value.length());
  }

  /*
   * @see javax.servlet.ServletOutputStream#print(float)
   */
  public void print(float f) throws IOException
  {
    String value = Float.toString(f);
    this.output.write(value.getBytes(), 0, value.length());
  }

  /*
   * @see javax.servlet.ServletOutputStream#print(int)
   */
  public void print(int i) throws IOException
  {
    String value = Integer.toString(i);
    this.output.write(value.getBytes(), 0, value.length());
  }

  /*
   * @see javax.servlet.ServletOutputStream#print(long)
   */
  public void print(long l) throws IOException
  {
    String value = Long.toString(l);
    this.output.write(value.getBytes(), 0, value.length());
  }

  /*
   * @see javax.servlet.ServletOutputStream#print(java.lang.String)
   */
  public void print(String value) throws IOException
  {
    if(value!=null) {
        this.output.write(value.getBytes(), 0, value.length());        
    }
  }

  /*
   * @see javax.servlet.ServletOutputStream#println()
   */
  public void println() throws IOException
  {
    this.output.write(CRLF, 0, 2);
  }

  /*
   * @see javax.servlet.ServletOutputStream#println(boolean)
   */
  public void println(boolean b) throws IOException
  {
    String value = Boolean.toString(b);
    this.output.write(value.getBytes(), 0, value.length());
    this.output.write(CRLF, 0, 2);
  }

  /*
   * @see javax.servlet.ServletOutputStream#println(char)
   */
  public void println(char c) throws IOException
  {
    this.singleByte[0] = (byte) c;
    this.output.write(this.singleByte, 0, 1);
    this.output.write(CRLF, 0, 2);
  }

  /*
   * @see javax.servlet.ServletOutputStream#println(double)
   */
  public void println(double d) throws IOException
  {
    String value = Double.toString(d);
    this.output.write(value.getBytes(), 0, value.length());
    this.output.write(CRLF, 0, 2);
  }

  /*
   * @see javax.servlet.ServletOutputStream#println(float)
   */
  public void println(float f) throws IOException
  {
    String value = Float.toString(f);
    this.output.write(value.getBytes(), 0, value.length());
    this.output.write(CRLF, 0, 2);
  }

  /*
   * @see javax.servlet.ServletOutputStream#println(int)
   */
  public void println(int i) throws IOException
  {
    String value = Integer.toString(i);
    this.output.write(value.getBytes(), 0, value.length());
    this.output.write(CRLF, 0, 2);
  }

  /*
   * @see javax.servlet.ServletOutputStream#println(long)
   */
  public void println(long l) throws IOException
  {
    String value = Long.toString(l);
    this.output.write(value.getBytes(), 0, value.length());
    this.output.write(CRLF, 0, 2);
  }

  /*
   * @see javax.servlet.ServletOutputStream#println(java.lang.String)
   */
  public void println(String s) throws IOException
  {
    if(s!=null) {
        this.output.write(s.getBytes(), 0, s.length());
    }
    this.output.write(CRLF, 0, 2);
  }

  /*
   * @see java.io.OutputStream#close()
   */
  public void close() throws IOException
  {
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
      {
        Tr.debug(tc, "close");
      }
      closed = true;
      this.output.close();
  }
  
  public void setIsClosing(boolean isClosing) {
      output.setIsClosing(isClosing);
  }
  
  public boolean isClosed() {
      return output.isClosed();
  }
  /*
   * @see java.io.OutputStream#flush()
   */
  public void flush() throws IOException
  {
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
      {
        Tr.debug(tc, "flush");
      }
      this.output.flush();
  }

  public void flush(boolean ignoreFlag) throws IOException
  {
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
      {
        Tr.debug(tc, "flush, ignoreFlag="+ignoreFlag);
      }
      this.output.flush(ignoreFlag);
  }

  /*
   * @see java.io.OutputStream#write(byte[], int, int)
   */
  public void write(byte[] value, int start, int len) throws IOException
  {
    this.output.write(value, start, len);
  }

  /*
   * @see java.io.OutputStream#write(byte[])
   */
  public void write(byte[] value) throws IOException
  {
    this.output.write(value, 0, value.length);
  }

  /*
   * @see java.io.OutputStream#write(int)
   */
  public void write(int value) throws IOException
  {
    byte[] buf = new byte[1];
    buf[0] = (byte) value;
    this.output.write(buf, 0, 1);
  }
  
  /**
   * @return the output
   */
  public HttpOutputStreamConnectWeb getOutput() {
      return this.output;
  }

  /**
   * @param output the output to set
   */
  public void setOutput(HttpOutputStreamConnectWeb houtput) {
      this.output = houtput;
  }

}
