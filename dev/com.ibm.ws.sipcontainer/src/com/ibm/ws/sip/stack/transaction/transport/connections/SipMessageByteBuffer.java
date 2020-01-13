/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.stack.transaction.transport.connections;

import jain.protocol.ip.sip.SipException;
import jain.protocol.ip.sip.message.Message;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import com.ibm.ws.jain.protocol.ip.sip.message.MessageImpl;
import com.ibm.ws.jain.protocol.ip.sip.message.MessageImpl.HeaderForm;
import com.ibm.ws.sip.parser.util.CharsBuffer;
import com.ibm.ws.sip.parser.util.CharsBuffersPool;
import com.ibm.ws.sip.parser.util.ObjectPool;

/**
 * @author Amirk
 * buffer for the sip message bytes - better use nio ByteBuffer in next version , it
 * for the same purpose
 * 
 * (ran) todo - in addition to moving to ByteBuffer, need to derive a new class
 * from ByteBuffer that holds the peerHost/peerAddress and use that class
 * to queue outbound messages. inbound buffers don't really need the peer info.
 * a good option would be to change this class to inherit from ByteBuffer.
 */
public class SipMessageByteBuffer
{
	/**
	 * default buffer size
	 */
	public static final int BUF_SIZE = 4096;
	
    /**
     * buffer of bytes
     */
    private byte[] m_bytes;
	
	/**
	 * buffer content size
	 */
	private int m_markedBytesNumber;
	
	/**
	 * next character to return by calling get()
	 */
	private int m_position;
	
	/**
	 * pool of byte buffers
	 */
	private static ObjectPool s_bufferPool = new ObjectPool(SipMessageByteBuffer.class);
	
    /**
     * constructor with no arguments, for allocation by the object pool
     */
    public SipMessageByteBuffer() {
        m_bytes = new byte[BUF_SIZE];
        m_markedBytesNumber = 0;
        m_position = 0;
    }

	/**
	 * allocates a new byte buffer from the pool, for general use.
	 * the initial size depends on whatever there is in the pool,
	 * but anyway will grow as needed
	 * 
	 * @return a newly allocated byte buffer
	 */
	public static SipMessageByteBuffer fromPool() {
	    SipMessageByteBuffer buf = (SipMessageByteBuffer)s_bufferPool.get();
		return buf;
	}

	/**
	 * allocates a byte buffer for incoming network data
	 * 
	 * @param bytes incoming data
	 * @param length incoming data size
	 * @param peerHost source address
	 * @param peerPort source port number
	 * @return a newly allocated byte buffer
	 */
	public static SipMessageByteBuffer fromNetwork(
		byte[] bytes,
		int length,
		String peerHost,
		int peerPort)
	{
		SipMessageByteBuffer buf = fromPool();
		buf.put(bytes, 0, length);
		return buf;
	}
	
    /**
     * serializes a sip message into a newly allocated byte buffer.
     * if the buffer is too small, it is expanded to
     * accommodate at least as much as needed for the given message.
     * caller is responsible to call reset() for de-allocating the buffer.
     * 
     * @param msg message object to be serialized
     * @param headerForm compact or full form, as requested by the transport
     * @return the newly allocated buffer containing the serialized message
     * @throws SipException in case the given message is not from this
     *  JAIN SIP implementation
     */
    public static SipMessageByteBuffer fromMessage(Message msg, HeaderForm headerForm)
    	throws SipException
    {
		if (!(msg instanceof MessageImpl)) {
    		throw new SipException("attempt to serialize message from a different JAIN implementation");
		}
		MessageImpl msgImpl = (MessageImpl)msg;
		
    	// 1. allocate buffer
		SipMessageByteBuffer buf = fromPool();
    	
    	if (buf.m_markedBytesNumber != 0) {
    		throw new SipException("attempt to use pre-allocated byte buffer");
    	}
    	
		// 2. the body part is easy - it's already serialized
		byte[] bodyPart = msg.getBodyAsBytes();

		// 3. start line and headers
		CharsBuffer headerPartBuffer = CharsBuffersPool.getBuffer();
		msgImpl.writeHeadersToBuffer(headerPartBuffer, headerForm, true);
		byte[] headerPart = headerPartBuffer.getBytes();
		int headerPartLength = headerPartBuffer.getBytesSize();
		
		// 4. dump headers and body into buffer
		buf.put(headerPart, 0, headerPartLength);
		if (bodyPart != null) {
			buf.put(bodyPart, 0, bodyPart.length);
		}
		CharsBuffersPool.putBufferBack(headerPartBuffer);
		return buf;
    }
    
	/**
	 * copies a byte array from object input stream into a new buffer
	 * @param in object input stream with source data
	 * @return the newly created buffer. the caller is responsible to
	 *  return this buffer to the pool
	 * @throws IOException on de-serialization error
	 */
	public static SipMessageByteBuffer fromStream(ObjectInput in) throws IOException {
		SipMessageByteBuffer buf = fromPool();
		int length = in.readInt();
		buf.ensureCapacity(length);
		byte[] dest = buf.getBytes();
		in.readFully(dest, 0, length);
		buf.m_markedBytesNumber = length;
		return buf;
	}
    
	/**
	 * writes buffer contents into the given stream
	 * @param out stream to write to
	 * @throws IOException on serialization error
	 */
	public void toStream(ObjectOutput out) throws IOException {
		out.writeInt(m_markedBytesNumber);
		out.write(m_bytes, 0, m_markedBytesNumber);
		out.flush();
		m_position = m_markedBytesNumber;
	}

    /**
     * @return the bytes the buffer holds
     */
    public byte[] getBytes() {
        return m_bytes;
    } 
    
    /**
     * @return the number of bytes read into the buffer
     */
    public int getMarkedBytesNumber() {
        return m_markedBytesNumber; 
    }
    
    /**
     * recycles this buffer
     */
    public void reset() {
        init();
        s_bufferPool.putBack(this);
    }
   
    /**
     * Init the buffer - cleans its contents. 
     * Changes the buffer's intrenal state without get/return the pool from pool.  
     */
    public void init() {
        m_markedBytesNumber = 0;
        m_position = 0;
    }

    /**
     * rewinds the read position to the beginning of the buffer
     */
    public void rewind() {
        rewind(0);
    }
    
    /**
     * rewinds the read position to a specific position
     * @param position index of the next char to return by calling get
     */
    public void rewind(int position) {
        m_position = position;
    }
    
    /**
     * @return index of the next char that will be read by calling get()
     */
    public int getReadPos() {
    	return m_position;
    }
    
    /**
     * @return number of bytes remaining to read
     */
    public int getRemaining() {
    	return m_markedBytesNumber - m_position;
    }
    
    /**
     * peeks buffer
     * @param distance number of bytes to look ahead
     * @return the byte at requested distance
     */
    public byte lookahead(int distance) {
    	int pos = m_position + distance - 1;
    	return m_bytes[pos];
    }
    
    /**
     * @return true if next get() call may succeed
     */
    public boolean hasMore() {
    	return m_position < m_markedBytesNumber;
    }
    
    /**
     * finds out if there are enough bytes left in the buffer for reading
     * @param count number of bytes required
     * @return true if the next <var>count</var> calls to get() may succeed
     */
    public boolean hasMore(int count) {
    	return m_position + count <= m_markedBytesNumber;
    }
    
    /**
     * gets the next byte in the buffer
     * @return the next byte in the buffer
     * @throws IndexOutOfBoundsException if no more available bytes to read
     */
    public byte get() {
    	if (m_position >= m_markedBytesNumber) {
    		throw new IndexOutOfBoundsException();
    	}
    	return m_bytes[m_position++];
    }
    
    /**
     * copies bytes from this buffer into a destination byte array
     * @param dest the destination byte array
     * @param offset position in dest byte array to start copying to
     * @param length number of bytes to copy
     */
    public void copyTo(byte[] dest, int offset, int length) {
    	if (m_position + length > m_markedBytesNumber) {
    		throw new IndexOutOfBoundsException();
    	}
    	System.arraycopy(m_bytes, m_position, dest, offset, length);
    	m_position += length;
    }
    
    /**
     * appends one byte to the end of the buffer
     * @param b byte to append
     */
    public void put(byte b) {
    	ensureCapacity(1);
        m_bytes[m_markedBytesNumber++] = b;
    }
    
    /**
     * copies bytes into the buffer
     * @param src - byte source to read from
     * @param offset - offset to read from 
     * @param length - number of bytes to copy
     */
    public void put(byte[] src, int offset, int length) {
    	ensureCapacity(length);
        System.arraycopy(src, offset , m_bytes, m_markedBytesNumber , length );
        m_markedBytesNumber += length;
    }
    
    /**
     * ensure buffer is large enough to accomodate given number of bytes
     * in addition to the bytes already stored.
     * @param length number of bytes to be appended to buffer
     */
    public void ensureCapacity(int length) {
        if (m_markedBytesNumber+length > m_bytes.length) {
            // re-allocate buffer. new size is the required size,
            // rounded up to the next binary exponent.
            int bits;
            int newSize = m_markedBytesNumber + length;
            for (bits = 0; newSize > 0; bits++)
                newSize /= 2;

            newSize = 1 << bits;

            byte[] newBuf = new byte[newSize];
            System.arraycopy(m_bytes, 0, newBuf, 0, m_markedBytesNumber);
            m_bytes = newBuf;
        }
    }
    
    /**
     * sets the content size of this buffer
     * @param size the new content size
     */
    public void setContentSize(int size) {
    	m_markedBytesNumber = size;
    }
}
