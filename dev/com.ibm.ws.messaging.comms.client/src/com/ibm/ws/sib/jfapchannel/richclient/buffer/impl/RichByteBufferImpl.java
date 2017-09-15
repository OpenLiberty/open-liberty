/*******************************************************************************
 * Copyright (c) 2003, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.jfapchannel.richclient.buffer.impl;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import com.ibm.ws.sib.jfapchannel.buffer.WsByteBuffer;

/**
 * This is the Rich client implementation of the JFap WsByteBuffer interface. There is nothing
 * clever done in this class; all calls to the methods are simply duplicated onto the underlying
 * CFW WsByteBuffer.
 *
 * @author Gareth Matthews
 */
public class RichByteBufferImpl implements WsByteBuffer
{
   /** The serial UId */
   private static final long serialVersionUID = -7237169886420141657L;

   /** The underlying WsByteBuffer */
   private com.ibm.wsspi.bytebuffer.WsByteBuffer buffer = null;

   /** Reference to the owning pool */
   private RichByteBufferPool pool = null;

   /**
    * Resets the buffer with a new underlying buffer.
    *
    * @param buff
    * @param p
    */
   void reset(com.ibm.wsspi.bytebuffer.WsByteBuffer buff, RichByteBufferPool p)
   {
      this.buffer = buff;
      this.pool = p;
   }

   /**
    * @return Returns the underlying buffer.
    *
    * @see com.ibm.ws.sib.jfapchannel.buffer.WsByteBuffer#getUnderlyingBuffer()
    */
   public Object getUnderlyingBuffer()
   {
      return buffer;
   }

   /**
    * @see com.ibm.ws.sib.jfapchannel.buffer.WsByteBuffer#array()
    */
   public byte[] array() { return buffer.array(); }

   /**
    * @see com.ibm.ws.sib.jfapchannel.buffer.WsByteBuffer#arrayOffset()
    */
   public int arrayOffset() { return buffer.arrayOffset(); }

   /**
    * @see com.ibm.ws.sib.jfapchannel.buffer.WsByteBuffer#compact()
    */
   public WsByteBuffer compact() { buffer.compact(); return this; }

   /**
    * @see com.ibm.ws.sib.jfapchannel.buffer.WsByteBuffer#compareTo(java.lang.Object)
    */
   public int compareTo(Object arg0)
   {
      if (arg0 instanceof WsByteBuffer)
      {
         return buffer.compareTo(arg0);
      }
      return -1;
   }

   /**
    * @see com.ibm.ws.sib.jfapchannel.buffer.WsByteBuffer#getChar()
    */
   public char getChar() { return buffer.getChar(); }

   /**
    * @see com.ibm.ws.sib.jfapchannel.buffer.WsByteBuffer#getChar(int)
    */
   public char getChar(int arg0) { return buffer.getChar(arg0); }

   /**
    * @see com.ibm.ws.sib.jfapchannel.buffer.WsByteBuffer#putChar(char)
    */
   public WsByteBuffer putChar(char arg0) { buffer.putChar(arg0); return this; }

   /**
    * @see com.ibm.ws.sib.jfapchannel.buffer.WsByteBuffer#putChar(int, char)
    */
   public WsByteBuffer putChar(int arg0, char arg1) { buffer.putChar(arg0, arg1); return this; }

   /**
    * @see com.ibm.ws.sib.jfapchannel.buffer.WsByteBuffer#putChar(char[])
    */
   public WsByteBuffer putChar(char[] arg0) { buffer.putChar(arg0); return this; }

   /**
    * @see com.ibm.ws.sib.jfapchannel.buffer.WsByteBuffer#putChar(char[], int, int)
    */
   public WsByteBuffer putChar(char[] arg0, int arg1, int arg2) { buffer.putChar(arg0, arg1, arg2); return this; }

   /**
    * @see com.ibm.ws.sib.jfapchannel.buffer.WsByteBuffer#getDouble()
    */
   public double getDouble() { return buffer.getDouble(); }

   /**
    * @see com.ibm.ws.sib.jfapchannel.buffer.WsByteBuffer#getDouble(int)
    */
   public double getDouble(int arg0) { return buffer.getDouble(arg0); }

   /**
    * @see com.ibm.ws.sib.jfapchannel.buffer.WsByteBuffer#putDouble(double)
    */
   public WsByteBuffer putDouble(double arg0) { buffer.putDouble(arg0); return this; }

   /**
    * @see com.ibm.ws.sib.jfapchannel.buffer.WsByteBuffer#putDouble(int, double)
    */
   public WsByteBuffer putDouble(int arg0, double arg1) { buffer.putDouble(arg0, arg1); return this; }

   /**
    * @see com.ibm.ws.sib.jfapchannel.buffer.WsByteBuffer#getFloat()
    */
   public float getFloat() { return buffer.getFloat(); }

   /**
    * @see com.ibm.ws.sib.jfapchannel.buffer.WsByteBuffer#getFloat(int)
    */
   public float getFloat(int arg0) { return buffer.getFloat(arg0); }

   /**
    * @see com.ibm.ws.sib.jfapchannel.buffer.WsByteBuffer#putFloat(float)
    */
   public WsByteBuffer putFloat(float arg0) { buffer.putFloat(arg0); return this; }

   /**
    * @see com.ibm.ws.sib.jfapchannel.buffer.WsByteBuffer#putFloat(int, float)
    */
   public WsByteBuffer putFloat(int arg0, float arg1) { buffer.putFloat(arg0, arg1); return this; }

   /**
    * @see com.ibm.ws.sib.jfapchannel.buffer.WsByteBuffer#getInt()
    */
   public int getInt() { return  buffer.getInt(); }

   /**
    * @see com.ibm.ws.sib.jfapchannel.buffer.WsByteBuffer#getInt(int)
    */
   public int getInt(int arg0) { return buffer.getInt(arg0);}

   /**
    * @see com.ibm.ws.sib.jfapchannel.buffer.WsByteBuffer#putInt(int)
    */
   public WsByteBuffer putInt(int arg0) { buffer.putInt(arg0); return this; }

   /**
    * @see com.ibm.ws.sib.jfapchannel.buffer.WsByteBuffer#putInt(int, int)
    */
   public WsByteBuffer putInt(int arg0, int arg1) { buffer.putInt(arg0, arg1); return this; }

   /**
    * @see com.ibm.ws.sib.jfapchannel.buffer.WsByteBuffer#getLong()
    */
   public long getLong() { return buffer.getLong(); }

   /**
    * @see com.ibm.ws.sib.jfapchannel.buffer.WsByteBuffer#getLong(int)
    */
   public long getLong(int arg0) { return buffer.getLong(arg0); }

   /**
    * @see com.ibm.ws.sib.jfapchannel.buffer.WsByteBuffer#putLong(long)
    */
   public WsByteBuffer putLong(long arg0) { buffer.putLong(arg0); return this; }

   /**
    * @see com.ibm.ws.sib.jfapchannel.buffer.WsByteBuffer#putLong(int, long)
    */
   public WsByteBuffer putLong(int arg0, long arg1) { buffer.putLong(arg0, arg1); return this; }

   /**
    * @see com.ibm.ws.sib.jfapchannel.buffer.WsByteBuffer#getShort()
    */
   public short getShort() { return buffer.getShort(); }

   /**
    * @see com.ibm.ws.sib.jfapchannel.buffer.WsByteBuffer#getShort(int)
    */
   public short getShort(int arg0) { return buffer.getShort(arg0); }

   /**
    * @see com.ibm.ws.sib.jfapchannel.buffer.WsByteBuffer#putShort(short)
    */
   public WsByteBuffer putShort(short arg0) { buffer.putShort(arg0); return this; }

   /**
    * @see com.ibm.ws.sib.jfapchannel.buffer.WsByteBuffer#putShort(int, short)
    */
   public WsByteBuffer putShort(int arg0, short arg1) { buffer.putShort(arg0, arg1); return this; }

   /**
    * @see com.ibm.ws.sib.jfapchannel.buffer.WsByteBuffer#hasArray()
    */
   public boolean hasArray() { return buffer.hasArray(); }

   /**
    * @see com.ibm.ws.sib.jfapchannel.buffer.WsByteBuffer#order()
    */
   public ByteOrder order() { return buffer.order(); }

   /**
    * @see com.ibm.ws.sib.jfapchannel.buffer.WsByteBuffer#order(java.nio.ByteOrder)
    */
   public WsByteBuffer order(ByteOrder arg0) { buffer.order(arg0); return this; }

   /**
    * @see com.ibm.ws.sib.jfapchannel.buffer.WsByteBuffer#clear()
    */
   public WsByteBuffer clear() { buffer.clear(); return this; }

   /**
    * @see com.ibm.ws.sib.jfapchannel.buffer.WsByteBuffer#capacity()
    */
   public int capacity() { return buffer.capacity(); }

   /**
    * @see com.ibm.ws.sib.jfapchannel.buffer.WsByteBuffer#flip()
    */
   public WsByteBuffer flip() { buffer.flip(); return this; }

   /**
    * @see com.ibm.ws.sib.jfapchannel.buffer.WsByteBuffer#get()
    */
   public byte get() { return buffer.get(); }

   /**
    * @see com.ibm.ws.sib.jfapchannel.buffer.WsByteBuffer#position()
    */
   public int position() { return buffer.position(); }

   /**
    * @see com.ibm.ws.sib.jfapchannel.buffer.WsByteBuffer#position(int)
    */
   public WsByteBuffer position(int arg0) { buffer.position(arg0); return this; }

   /**
    * @see com.ibm.ws.sib.jfapchannel.buffer.WsByteBuffer#limit(int)
    */
   public WsByteBuffer limit(int arg0) { buffer.limit(arg0); return this; }

   /**
    * @see com.ibm.ws.sib.jfapchannel.buffer.WsByteBuffer#limit()
    */
   public int limit() { return buffer.limit(); }

   /**
    * @see com.ibm.ws.sib.jfapchannel.buffer.WsByteBuffer#remaining()
    */
   public int remaining() { return buffer.remaining(); }

   /**
    * @see com.ibm.ws.sib.jfapchannel.buffer.WsByteBuffer#mark()
    */
   public WsByteBuffer mark() { buffer.mark(); return this; }

   /**
    * @see com.ibm.ws.sib.jfapchannel.buffer.WsByteBuffer#reset()
    */
   public WsByteBuffer reset() { buffer.reset(); return this; }

   /**
    * @see com.ibm.ws.sib.jfapchannel.buffer.WsByteBuffer#rewind()
    */
   public WsByteBuffer rewind() { buffer.rewind(); return this; }

   /**
    * @see com.ibm.ws.sib.jfapchannel.buffer.WsByteBuffer#isReadOnly()
    */
   public boolean isReadOnly() { return buffer.isReadOnly(); }

   /**
    * @see com.ibm.ws.sib.jfapchannel.buffer.WsByteBuffer#hasRemaining()
    */
   public boolean hasRemaining() { return buffer.hasRemaining(); }

   /**
    * @see com.ibm.ws.sib.jfapchannel.buffer.WsByteBuffer#duplicate()
    */
   public WsByteBuffer duplicate()
   {
      RichByteBufferImpl duplicate = pool.getFromPool();
      duplicate.reset(buffer.duplicate(), pool);
      return duplicate;
   }

   /**
    * @see com.ibm.ws.sib.jfapchannel.buffer.WsByteBuffer#slice()
    */
   public WsByteBuffer slice()
   {
      RichByteBufferImpl slice = pool.getFromPool();
      slice.reset(buffer.slice(), pool);
      return slice;
   }

   /**
    * @see com.ibm.ws.sib.jfapchannel.buffer.WsByteBuffer#get(byte[])
    */
   public WsByteBuffer get(byte[] arg0) { buffer.get(arg0); return this; }

   /**
    * @see com.ibm.ws.sib.jfapchannel.buffer.WsByteBuffer#get(byte[], int, int)
    */
   public WsByteBuffer get(byte[] arg0, int arg1, int arg2) { buffer.get(arg0, arg1, arg2); return this; }

   /**
    * @see com.ibm.ws.sib.jfapchannel.buffer.WsByteBuffer#get(int)
    */
   public byte get(int arg0) { return buffer.get(arg0); }

   /**
    * @see com.ibm.ws.sib.jfapchannel.buffer.WsByteBuffer#isDirect()
    */
   public boolean isDirect() { return buffer.isDirect(); }

   /**
    * @see com.ibm.ws.sib.jfapchannel.buffer.WsByteBuffer#put(byte)
    */
   public WsByteBuffer put(byte arg0) { buffer.put(arg0); return this; }

   /**
    * @see com.ibm.ws.sib.jfapchannel.buffer.WsByteBuffer#put(byte[])
    */
   public WsByteBuffer put(byte[] arg0) { buffer.put(arg0); return this; }

   /**
    * @see com.ibm.ws.sib.jfapchannel.buffer.WsByteBuffer#put(byte[], int, int)
    */
   public WsByteBuffer put(byte[] arg0, int arg1, int arg2) { buffer.put(arg0, arg1, arg2); return this; }

   /**
    * @see com.ibm.ws.sib.jfapchannel.buffer.WsByteBuffer#put(int, byte)
    */
   public WsByteBuffer put(int arg0, byte arg1) { buffer.put(arg0, arg1); return this; }

   /**
    * @see com.ibm.ws.sib.jfapchannel.buffer.WsByteBuffer#put(java.nio.ByteBuffer)
    */
   public WsByteBuffer put(ByteBuffer arg0) { buffer.put(arg0); return this; }

   /**
    * @see com.ibm.ws.sib.jfapchannel.buffer.WsByteBuffer#put(com.ibm.ws.sib.jfapchannel.buffer.WsByteBuffer)
    */
   public WsByteBuffer put(WsByteBuffer arg0)
   {
      buffer.put((com.ibm.wsspi.bytebuffer.WsByteBuffer) arg0.getUnderlyingBuffer());
      return this;
   }

   /**
    * @see com.ibm.ws.sib.jfapchannel.buffer.WsByteBuffer#put(com.ibm.ws.sib.jfapchannel.buffer.WsByteBuffer[])
    */
   public WsByteBuffer put(WsByteBuffer[] arg0)
   {
      final int length = arg0.length;
      for (int i=0; i < length; ++i)
         buffer.put((com.ibm.wsspi.bytebuffer.WsByteBuffer) arg0[i].getUnderlyingBuffer());
      return this;
   }

   /**
    * @see com.ibm.ws.sib.jfapchannel.buffer.WsByteBuffer#setReadOnly(boolean)
    */
   public void setReadOnly(boolean arg0) { buffer.setReadOnly(arg0); }

   /**
    * @see com.ibm.ws.sib.jfapchannel.buffer.WsByteBuffer#getReadOnly()
    */
   public boolean getReadOnly() { return buffer.getReadOnly(); }

   /**
    * @see com.ibm.ws.sib.jfapchannel.buffer.WsByteBuffer#removeFromLeakDetection()
    */
   public void removeFromLeakDetection() { buffer.removeFromLeakDetection(); }

   /**
    * @see com.ibm.ws.sib.jfapchannel.buffer.WsByteBuffer#release()
    */
   public void release()
   {
      buffer.release();
      buffer = null;
      pool.release(this);
   }

   /**
    * @see java.lang.Object#toString()
    */
   public String toString()
   {
      return "RichByteBufferImpl@" + Integer.toHexString(System.identityHashCode(this)) +
             ": " + (buffer!=null ? buffer.toString() : "buffer=null");                                         //469387
   }
}
