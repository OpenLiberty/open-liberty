/*******************************************************************************
 * Copyright (c) 2003, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.jfapchannel.buffer;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * This interface completely matches the Channel Framework WsByteBuffer implementation. The JFap 
 * channel has a local copy of this as it is not guarenteed that we will be running in a JVM that
 * supports this (for example, the Portly, or 'thin' client). This class is therefore re-implemented
 * by the thin and rich JFap clients.
 * 
 * @author Gareth Matthews
 */
public interface WsByteBuffer extends Serializable
{
   /**
    * @return Returns the actual underlying buffer. This object can be cast to the correct
    *         implementation class.
    */
   public Object getUnderlyingBuffer();
   
   /**
    * @see java.nio.ByteBuffer#array()
    */
   public byte[] array();
    
   /**
    * @see java.nio.ByteBuffer#arrayOffset()
    */
   public int arrayOffset();
   
   /**
    * Compact this buffer
    * @return WsByteBuffer
    */
   public WsByteBuffer compact();
   
   /**
    * Compares this buffer to another object.
    * @return int
    */
   public int compareTo(Object ob);
   
   /**
    * @see java.nio.ByteBuffer#getChar()
    */
   public char getChar();
    
   /**
    * @see java.nio.ByteBuffer#getChar(int)
    */
   public char getChar(int index);
    
   /**
    * @see java.nio.ByteBuffer#putChar(char)
    */
   public WsByteBuffer putChar( char value);
    
   /**
    * @see java.nio.ByteBuffer#putChar(int, char)
    */
   public WsByteBuffer putChar(int index, char value);

   /**
    * @see java.nio.ByteBuffer#putChar(char[])
    */
   public WsByteBuffer putChar(char[] values);

   /**
    * @see java.nio.ByteBuffer#putChar(char[], int, int)
    */
   public WsByteBuffer putChar(char[] values, int off, int len);
   
   /**
    * @see java.nio.ByteBuffer#getDouble()
    */
   public double getDouble();

   /**
    * @see java.nio.ByteBuffer#getDouble(int)
    */
   public double getDouble(int index);

   /**
    * @see java.nio.ByteBuffer#putDouble(double)
    */
   public WsByteBuffer putDouble( double value);
    
   /**
    * @see java.nio.ByteBuffer#putDouble(int, double)
    */
   public WsByteBuffer putDouble(int index, double value);
    
   /**
    * @see java.nio.ByteBuffer#getFloat()
    */
   public float getFloat();
    
   /**
    * @see java.nio.ByteBuffer#getFloat(int)
    */
   public float getFloat(int index);
    
   /**
    * @see java.nio.ByteBuffer#putFloat(float)
    */
   public WsByteBuffer putFloat( float value);
    
   /**
    * @see java.nio.ByteBuffer#putFloat(int, float)
    */
   public WsByteBuffer putFloat(int index, float value);

   /**
    * @see java.nio.ByteBuffer#getInt()
    */
   public int getInt();
    
   /**
    * @see java.nio.ByteBuffer#getInt(int)
    */
   public int getInt(int index);

   /**
    * @see java.nio.ByteBuffer#putInt(int)
    */
   public WsByteBuffer putInt( int value);
    
   /**
    * @see java.nio.ByteBuffer#putInt(int, int)
    */
   public WsByteBuffer putInt(int index, int value);
    
   /**
    * @see java.nio.ByteBuffer#getLong()
    */
   public long getLong();
    
   /**
    * @see java.nio.ByteBuffer#getLong(int)
    */
   public long getLong(int index);
    
   /**
    * @see java.nio.ByteBuffer#putLong(long)
    */
   public WsByteBuffer putLong( long value);
    
   /**
    * @see java.nio.ByteBuffer#putLong(int, long)
    */
   public WsByteBuffer putLong(int index, long value);
    
   /**
    * @see java.nio.ByteBuffer#getShort()
    */
   public short getShort();
    
   /**
    * @see java.nio.ByteBuffer#getShort(int)
    */
   public short getShort(int index);

   /**
    * @see java.nio.ByteBuffer#putShort(short)
    */
   public WsByteBuffer putShort( short value);
    
   /**
    * @see java.nio.ByteBuffer#putShort(int, short)
    */
   public WsByteBuffer putShort(int index, short value);
    
   /**
    * @see java.nio.ByteBuffer#hasArray()
    */
   public boolean hasArray();
    
   /**
    * @see java.nio.ByteBuffer#order()
    */
    public ByteOrder order();
    
   /**
    * Modifies this buffer's byte order
    */
   public WsByteBuffer order(ByteOrder bo);

   /**
    * Clear this buffer
    * @return WsByteBuffer object after clear
    */
   public WsByteBuffer clear();
    
   /**
    * @see java.nio.Buffer#capacity()
    */
   public int capacity();
 
   /**
    * Flip this buffer.
    * @return WsByteBuffer after flipped
    */
   public WsByteBuffer flip();
    
   /**
    * relative get method
    * @return abstract byte
    */
   public byte get();
   
   /**
    * @see java.nio.Buffer#position()
    */
   public int position();
    
   /**
    * Sets the position mark in the ByteBuffer.
    * @param p the integer mark to set
    * @return the WsByteBuffer object
    */  
   public WsByteBuffer position(int p);
    
   /**
    * Sets the limit mark in the ByteBuffer
    * @param l the integer to set the limit to
    * @return the WsByteBuffer object
    */
   public WsByteBuffer limit(int l);
    
   /**
    * @see java.nio.Buffer#limit()
    */
   public int limit();
    
   /**
    * tells whether there are bytes remaining or not
    * @return boolean
    */
   public int remaining();

   /**
    * Sets the mark to the current position
    * @return WsByteBuffer object
    */
   public WsByteBuffer mark();
    
   /**
    * resets the position to the mark
    * @return WsByteBuffer object
    */
   public WsByteBuffer reset();

   /**
    * rewinds this ByteBuffer
    * @return WsByteBuffer object
    */
   public WsByteBuffer rewind();

   /**
    * returns whether this buffer is read only or not
    * @return abstract boolean
    */
   public boolean isReadOnly();

   /**
    * tells whether there are bytes remaining or not
    * @return boolean
    */
   public boolean hasRemaining();
    
   /**
    * duplicates this WsByteBuffer
    * @return WsByteBuffer
    */
   public WsByteBuffer duplicate();
   
   /**
    * slice the buffer (sets up a view into the buffer removing the head)
    * @return WsByteBuffer
    */
   public WsByteBuffer slice();

   /**
    * relative bulk get
    * @param dst destination byte array
    * @return WsByteBuffer
    */
    public WsByteBuffer get(byte[] dst);

   /**
    * absolute bulk get
    * @param dst destination byte array
    * @param offset
    * @param length
    * @return WsByteBuffer
    */
   public WsByteBuffer get(byte[] dst, int offset, int length);

   /**
    * absolute get
    * @param index integer of bytew to get
    * @return abstract byte
    */
   public byte get(int index);
   
   /**
    * tells if the buffer is direct or not
    * @return boolean
    */
   public boolean isDirect();

   /**
    * relative put method
    * @param b source byte
    * @return WsByteBuffer
    */
   public WsByteBuffer put(byte b); //throws WriteOnReadOnlyBufferException;

   /**
    * relative bulk put
    * @param src byte array
    * @return WsByteBuffer
    */
   public WsByteBuffer put(byte[] src); // throws WriteOnReadOnlyBufferException;

   /**
    * absolute bulk put
    * @param src byte array
    * @param offset
    * @param length
    * @return WsByteBuffer
    */
   public WsByteBuffer put(byte[] src, int offset, int length); // throws WriteOnReadOnlyBufferException;

   /**
    * absolute put
    * @param index integer of bytew to put
    * @param b source byte
    * @return WsByteBuffer
    */
   public WsByteBuffer put(int index, byte b); // throws WriteOnReadOnlyBufferException;

   /**
    * relative bulk put method
    * @param src ByteBuffer
    * @return WsByteBuffer
    */
   public WsByteBuffer put(ByteBuffer src); // throws WriteOnReadOnlyBufferException;

   /**
    * relative bulk put method
    * @param src WsByteBuffer
    * @return WsByteBuffer
    */
   public WsByteBuffer put(WsByteBuffer src); // throws WriteOnReadOnlyBufferException;
   public WsByteBuffer put(WsByteBuffer[] src);

   /**
    * mark this buffer as read only
    * @param value true - buffer is to be read only, false - read and write opertion are
    * allowed on the buffer
    */
   public void setReadOnly(boolean value);

   /**
    * query the read only setting for this buffer
    * @return true - if the buffer is read only, otherwise return false
    */
   public boolean getReadOnly();
   
   /**
    * If this buffer is known to be held for a long time, then it can
    * be removed from the leak detection logic, so as not to create
    * a false-positive leak detection hit against this buffer.
    */
   public void removeFromLeakDetection();
   
   /**
    * release the buffer by telling the pool manager that we are done with it.
    * safeguard that this release will not be called twice per instance of this object
    */
   public void release();
}
