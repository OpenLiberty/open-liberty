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
package com.ibm.wsspi.webcontainer.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.servlet.response.IResponse;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.webcontainer.osgi.osgi.WebContainerConstants;
import com.ibm.ws.webcontainer.srt.WriteBeyondContentLengthException;
import com.ibm.wsspi.bytebuffer.WsByteBuffer;
import com.ibm.wsspi.channelfw.ChannelFrameworkFactory;

public class ByteBufferOutputStream extends WSServletOutputStream
{

  private static final TraceComponent tc = Tr.register(ByteBufferOutputStream.class, WebContainerConstants.TR_GROUP, WebContainerConstants.NLS_PROPS);

  private List<WsByteBuffer> bbList = new ArrayList<WsByteBuffer>();
  private WsByteBuffer current = null;
  private boolean _hasWritten = false;
  private int bufferSize = 8192;
  /**
   * The current number of bytes in the buffer.
   */
  protected int count;
  /**
   * The total number of bytes written so far.
   */
  protected int total;
  /**
   * The maximum number of bytes that can be written. This is initially
   * set to -1 in order to indicate that observers must be notified.
   */
  protected int limit = -1;
  private IOutputStreamObserver obs;
  private IOException except;

  // private static final String
  // CLASS_NAME="com.ibm.wsspi.webcontainer.util.ByteBufferOutputStream";
  private boolean committed = false;
  private boolean byteBuffersRetrieved;
  private IResponse response;
  private static TraceNLS nls = TraceNLS.getTraceNLS(ByteBufferOutputStream.class, "com.ibm.ws.webcontainer.resources.Messages");

  public ByteBufferOutputStream()
  {

    super();
    // TODO Auto-generated constructor stub
  }

  public List<WsByteBuffer> getByteBufferList()
  {
    byteBuffersRetrieved = true;
    return bbList;
  }

  // clean up...
  public void reset()
  {
    current = null;
    _hasWritten = false;
    byteBuffersRetrieved = false;
    limit = -1;
    total = 0;
    if (!byteBuffersRetrieved)
    {
      ListIterator<WsByteBuffer> it = bbList.listIterator();
      while (it.hasNext())
      {
        WsByteBuffer next = it.next();
        next.release();
        it.remove();
      }
    }
  }

  /**
   * Writes a byte.
   */
  public void write(int ch) throws IOException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
    { // 306998.15
      Tr.debug(tc, "write --> " + ch + ", limit->" + limit);
    }
    if (limit > -1)
    {
      if (total >= limit)
      {
        throw new WriteBeyondContentLengthException();
      }
    }
    if (!_hasWritten && obs != null)
    {
      _hasWritten = true;
      obs.alertFirstWrite();
    }

    checkList();

    current.put((byte) ch);
    total++;
  }

  /**
   * Writes a byte array
   */
  public void write(byte[] buf, int offset, int len) throws IOException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
    { // 306998.15
      Tr.debug(tc, "write len --> " + len + ", limit->" + limit);
    }
    if (len < 0)
    {
      if (tc.isErrorEnabled())
        Tr.error(tc, "Illegal.Argument.Trying.to.write.chars");
      throw new IllegalArgumentException();
    }
    if (!_hasWritten && obs != null)
    {
      _hasWritten = true;
      obs.alertFirstWrite();
    }

    if (limit > -1)
    {
      if (total + len > limit)
      {
        len = limit - total;
        except = new WriteBeyondContentLengthException();
      }
    }

    int toWrite = 0, amountWritten = 0, remaining = 0;

    while (amountWritten != len)
    {

      checkList();

      toWrite = len - amountWritten;
      remaining = current.remaining();

      if (toWrite <= remaining)
      {

        /*
         * We can write it all in the current buffer
         */
        current.put(buf, offset + amountWritten, toWrite);
        amountWritten += toWrite;
      }
      else
      {

        /*
         * Write what we can to the current position
         */
        current.put(buf, offset + amountWritten, remaining);
        amountWritten += remaining;
      }
    }
    count += len;
    total += len;
    check();
  }

  /**
   * Writes a byte array.
   */
  public void write(byte[] buf) throws IOException
  {
    write(buf, 0, buf.length);
  }

  private void checkList()
  {
    if (current == null)
    {

      /*
       * Get the first buffer
       */
      current = getNewByteBuffer();
      bbList.add(current);
    }
    else if (current.hasRemaining())
    {

      /*
       * We have room in the current position
       */
      return;
    }
    else
    {
      current.flip();
      current = getNewByteBuffer();
      bbList.add(current);
    }
  }

  private WsByteBuffer getNewByteBuffer()
  {
    return (ChannelFrameworkFactory.getBufferManager().allocateDirect(bufferSize));
  }

  @SuppressWarnings("unchecked")
  public void writeTo(OutputStream os)
  {
    try
    {

      Iterator it = bbList.iterator();
      WsByteBuffer bb;
      while (it.hasNext())
      {
        bb = (WsByteBuffer) it.next();
        byte[] b = new byte[bb.limit()];
        bb.get(b);
        os.write(b);
      }
    }
    catch (IOException ioe)
    {
      FFDCFilter.processException(ioe, this.getClass().getName() + ".writeTo", "247");
    }
  }

  public byte[] toByteArray()
  {
    ByteArrayOutputStream arrayOS = new ByteArrayOutputStream();
    writeTo(arrayOS);
    return arrayOS.toByteArray();
  }

  public void clearBuffer()
  {
    if (isCommitted())
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
      {
        Tr.debug(tc, "clearBuffer(): illegal state--> stream is committed ");
      }
      throw new IllegalStateException("clearBuffer(): illegal state--> stream is committed ");
    }

    ListIterator<WsByteBuffer> it = bbList.listIterator();
    while (it.hasNext())
    {
      WsByteBuffer next = it.next();
      next.release();
      it.remove();
    }
    total = 0;
    count = 0;
    _hasWritten = false;
  }

  @Override
  public void flushBuffer() throws IOException
  {
    if (current != null && current.position() != 0)
      current.flip();
    commit();
  }

  @Override
  public int getBufferSize()
  {
    // TODO Auto-generated method stub
    return this.bufferSize;
  }

  @Override
  public void init(OutputStream out, int bufferSize)
  {
    this.bufferSize = bufferSize;
    // ard never uses the underlying stream
  }

  @Override
  public boolean isCommitted()
  {
    if (!committed)
    {
      if (total >= bufferSize)
        committed = true;
      else
        committed = false;
    }
    return committed;
  }

  @Override
  public void setBufferSize(int size)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
    { // 306998.15
      Tr.debug(tc, "setBufferSize --> " + size);
    }
    if (total > 0)
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
      { // 306998.15
        Tr.debug(tc, "setBufferSize(): illegal state--> already wrote " + total + " bytes");
      }
      throw new IllegalStateException(nls.getString("Cannot.set.buffer.size.after.data", "Can't set buffer size after data has been written to stream"));
    }
    clearBuffer();
  }

  public void setLimit(int contentLength)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
    { // 306998.15
      Tr.debug(tc, "setLimit(): contentLength->" + contentLength);
    }
    this.limit = contentLength;
  }

  @Override
  public void setObserver(IOutputStreamObserver obs)
  {
    this.obs = obs;
  }

  @Override
  public void setResponse(IResponse response)
  {
    this.response = response;
  }

  public void flush() throws IOException
  {
    commit();
  }

  private void commit()
  {
    committed = true;
  }

  /**
   * Checks the output stream for a pending IOException that needs to be
   * thrown, or a content length that has been exceeded.
   * 
   * @param len
   *          the number of bytes about to be written
   */
  protected void check() throws IOException
  {
    // check for pending IOException
    if (except != null)
    {
      flush();
      throw except;
    }
  }

  /**
   * Returns the total number of bytes written so far.
   */
  public int getTotal()
  {
    return total;
  }

  @Override
  public void addObserver(IOutputStreamObserver obs)
  {
    // TODO we don't support this on ByteBufferOutputStream yet
  }


}
