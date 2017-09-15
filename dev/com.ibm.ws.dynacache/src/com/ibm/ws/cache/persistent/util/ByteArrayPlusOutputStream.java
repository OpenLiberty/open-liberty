/*******************************************************************************
 * Copyright (c) 1997, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cache.persistent.util;

import java.io.*;

/******************************************************************************
*	Use an extended version of ByteArrayOutputStream in order to allow access
*	to protected values.
******************************************************************************/
public class ByteArrayPlusOutputStream extends ByteArrayOutputStream {

/******************************************************************************
*	Default constructor
******************************************************************************/
	public ByteArrayPlusOutputStream() {
		super();
	}

/******************************************************************************
*	Creates a new byte array output stream, with a buffer capacity of the 
*		specified size, in bytes.
*	@param len the expected length, or initial length, for the byte array
******************************************************************************/
	public ByteArrayPlusOutputStream(int len) {
		super(len);
	}

/******************************************************************************
*	Create a new ByteArrayOutputStream, where the output is directed to an
*	existing buffer.
*	@param inbuf the buffer to use to save output directed to this stream
******************************************************************************/
	public ByteArrayPlusOutputStream(byte[] inbuf) {
		super(0); // Have it create a zero len array, then overwrite with input
		buf = inbuf;
		count = inbuf.length;
	}

/******************************************************************************
*	Set the seek point so we can do random-access writes.
*	@param The offset within the buffer to seek to
******************************************************************************/    
	public void seek(int offset)
    {
        count = offset;
    }

/******************************************************************************
*	A dangerous function which returns the actual buffer used in this object.
*	Be aware that only "size()" of the buffer is currently full, and unless
*	some synchronization is done the data in the buffer could be changed at any time.
*	@return a reference to the buffer
******************************************************************************/
	public byte[] getTheBuffer() { return buf; }


// The following two functions were created, and then not used.  They are kept
// here in comments in case at a later date they become useful.
//	public int writeFrom(InputStream in, int len)
//		throws IOException
//	{
//		int newcount = count + len;
//		if (newcount > buf.length) {
//		    byte newbuf[] = new byte[Math.max(buf.length << 1, newcount)];
//		    System.arraycopy(buf, 0, newbuf, 0, count);
//		    buf = newbuf;
//		}
//		int rc = 0;
//		while (rc < len) {
//			rc += in.read(buf, count+rc, len-rc);
//		}
//		count = newcount;
//		return rc;
//	}
	
//	public ByteArrayInputStream getByteArrayInputStream() 
//	{
//		return new ByteArrayInputStream(buf, 0, count);
//	}
}
