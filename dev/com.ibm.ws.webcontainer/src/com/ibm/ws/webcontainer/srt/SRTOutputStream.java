/*******************************************************************************
 * Copyright (c) 1997, 2006 IBM Corporation and others.
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
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.ws.webcontainer.osgi.response.WCOutputStream;
import com.ibm.wsspi.bytebuffer.WsByteBuffer;
import com.ibm.wsspi.http.HttpOutputStream;
import com.ibm.wsspi.webcontainer.logging.LoggerFactory;
import com.ibm.wsspi.webcontainer.util.ByteBufferWriter;
import com.ibm.wsspi.webcontainer.util.IOutputStreamObserver;
/**
 * Implements a ServletOutputStream for a ISRPConnection.
 */
public class SRTOutputStream extends javax.servlet.ServletOutputStream implements ByteBufferWriter
{
  private OutputStream _conn;
  private IOutputStreamObserver _observer;
  protected static Logger logger = LoggerFactory.getInstance().getLogger("com.ibm.ws.webcontainer.srt");
  private static final String CLASS_NAME="com.ibm.ws.webcontainer.srt.SRTOutputStream";
  private static TraceNLS nls = TraceNLS.getTraceNLS(SRTOutputStream.class, "com.ibm.ws.webcontainer.resources.Messages");

  private int fastCheck = 0;

  /**
   * This method was created in VisualAge.
   */
  public SRTOutputStream()
  {
  }

  public void init(OutputStream str)
  {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
            logger.logp(Level.FINE, CLASS_NAME,"init", "Initializing");
        }
    _conn = str;
    fastCheck = 0;
  }

  
  public void flush() throws IOException
  {
    if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)){  //306998.15
        logger.logp(Level.FINE, CLASS_NAME,"flush", "Flushing");
    }
                
    if (fastCheck == 0) {
        if (_conn instanceof WCOutputStream) {
            fastCheck = 1;
        } else {
            fastCheck = 2;
        }
    }
    
    if (fastCheck == 1) {            
        // make sure the flush is not ignored
        ((WCOutputStream) _conn).flush(false);

    } else {

        _conn.flush();

    }
  }

  public void reset()
  {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)){  //306998.15
            logger.logp(Level.FINE, CLASS_NAME,"reset", "Reseting");
        }
    _conn = null;
    fastCheck = 0;
  }

  /**
   * This method was created in VisualAge.
   */
  public void close() throws java.io.IOException
  {
                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
                        logger.logp(Level.FINE, CLASS_NAME,"close", "Closing");
        }

                if (_observer != null) {
      _observer.alertClose();
    }
    super.close();
  }

  /**
   * This method was created in VisualAge.
         * @param obs com.ibm.servlet.engine.srp.IOutputStreamObserver
   */
  protected void setObserver(IOutputStreamObserver obs)
  {
    _observer = obs;
  }

  /**
   * This method was created in VisualAge.
         * @param b byte[]
         * @param off int
         * @param len int
   */
  public void write(byte[] b, int off, int len) throws IOException
  {
                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
                        logger.logp(Level.FINE, CLASS_NAME,"write", "Writing");
        }

    if (_observer != null)
      _observer.alertFirstWrite();
    _conn.write(b, off, len);
  }

  @Override
  public void write(byte[] b) throws IOException {
      write(b, 0, b.length);
  }

  
  /**
   * This method was created in VisualAge.
         * @param b int
   */
  public void write(int b) throws java.io.IOException
  {
    byte[] buf = new byte[1];
    buf[0] = (byte) b;
    write(buf, 0, 1);
  }

  // BEIGN ZHJ
        public void writeByteBuffer(WsByteBuffer[] buf) {

    ((ByteBufferWriter) _conn).writeByteBuffer(buf);
  }

  // END ZHJ

  // LIBERTY WI #3179 BEGIN
  public void write(FileChannel fileChannel) throws java.io.IOException
  {
    ((HttpOutputStream) _conn).writeFile(fileChannel);
  }
  // LIBERTY WI #3179 END

}
