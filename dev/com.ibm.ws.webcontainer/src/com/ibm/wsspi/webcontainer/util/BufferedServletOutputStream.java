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
package com.ibm.wsspi.webcontainer.util;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.servlet.response.IResponse;
import com.ibm.ws.webcontainer.srt.WriteBeyondContentLengthException;
import com.ibm.wsspi.bytebuffer.WsByteBuffer;
import com.ibm.wsspi.webcontainer.WCCustomProperties;
import com.ibm.wsspi.webcontainer.WebContainerRequestState;
import com.ibm.wsspi.webcontainer.logging.LoggerFactory;
/**
 * This class implements a buffered output stream for writing servlet
 * response data. It also keeps track of the number of bytes that have
 * been written, Additionally, an observer list is maintained which can
 * be used to notify observers the first time the stream is written to.
 *
 * @version	1.35, 03/06/98
 */
public class BufferedServletOutputStream extends WSServletOutputStream implements ByteBufferWriter
{
	/**
	 * The actual output stream.
	 */
	protected OutputStream out;
	/**
	 * The output buffer.
	 */
	protected byte[] buf = new byte[0];
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
	protected int limit;
	protected IResponse response;
	/**
	 * The content length for this stream.
	 */
	protected int length = -1;
	/**
	 * The observer that will be notified when the stream is first written.
	 */
	protected IOutputStreamObserver obs;
	/**
	 * Flag indicating that the first write has already occurred.
	 */
	protected boolean _hasWritten;
	/**
	 * Flag indicating that the first write has already occurred.
	 */
	protected boolean _hasFlushed;
	/**
	 * If set then an I/O exception is pending.
	 */
	protected IOException except;
	/**
	 * Indicated whether the buffer has been written to the stream
	 */
	protected boolean committed;
	private int bufferSize;
        protected static Logger logger = LoggerFactory.getInstance().getLogger("com.ibm.wsspi.webcontainer.util");
	private static final String CLASS_NAME="com.ibm.wsspi.webcontainer.util.BufferedServletOutputStream";
	private boolean outputstreamClosed = false; //PK89810
	/**
	 * Should we close the underlying stream on close ?
	 * This flag is used for handling servlet chains which uses piped streams
	 * to establish comunication from the filtered servlet to its servlet
	 * filter.
	 * By closing the filtered servlet output stream, we trigger the end 
	 * of the filter's input stream.
	 */
	private boolean closeOnClose = false;
	private List <IOutputStreamObserver> obsList;
	private static TraceNLS nls = TraceNLS.getTraceNLS(BufferedServletOutputStream.class, "com.ibm.ws.webcontainer.resources.Messages");
	/**
	 * Creates a new servlet output stream using the specified buffer size.
	 * @param size the output buffer size
	 */
	public BufferedServletOutputStream(int size)
	
	{
		if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
			logger.logp(Level.FINE, CLASS_NAME,"BufferedServletOutputStream", "Constructor --> "+size);
        }
		buf = new byte[size];
		bufferSize = size;
		_hasWritten = false;
		_hasFlushed = false;
	}
	/**
	 * Creates a new, uninitialized servlet output stream with a default
	 * buffer size.
	 */
	public BufferedServletOutputStream()
	{
		this(1 * 1024);
	}
	/**
	 * Initializes the output stream with the specified raw output stream.
	 * @param out the raw output stream
	 */
	public void init(OutputStream out, int bufSize)
	{
		if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
			logger.logp(Level.FINE, CLASS_NAME,"init", "init", out);
        }
		// make sure that we don't have anything hanging around between
		// init()s -- this is the fix for the broken pipe error being
		// returned to the browser
		initNewBuffer(out, bufSize);
	}
	/**
	 * Initializes the output stream with the specified raw output stream.
	 * @param out the raw output stream
	 */
	void initNewBuffer(OutputStream out, int bufSize)
	{
		if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
			logger.logp(Level.FINE, CLASS_NAME,"init", "initNewBuffer --> "+bufSize);
        }
		this.out = out;
		if (bufSize != buf.length)
		{
			bufferSize = bufSize;
			buf = new byte[bufferSize];
		}
	}
	/**
	 * Finishes the current response.
	 */
	private void finish() throws IOException
	{
		if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
			logger.logp(Level.FINE, CLASS_NAME,"finish", "finish");
        }
		if (length == -1 && total != 0)
			length = total;
		//PK89810 Start
		if (WCCustomProperties.FINISH_RESPONSE_ON_CLOSE) {			
			WebContainerRequestState reqState = WebContainerRequestState.getInstance(false);		
			if (reqState==null || reqState.getAttribute("com.ibm.ws.webcontainer.appIsArdEnabled")==null){
				if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
        			logger.logp(Level.FINE, CLASS_NAME,"finish", "finishresponseonclose and appIsNotArdEnabled, setLastBuffer to true");
				response.setLastBuffer(true);	
			}       	
		}//PK89810 End
		flush();
	}
	/**
	 * Resets the output stream for a new connection.
	 */
	public void reset()
	{
		if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
			logger.logp(Level.FINE, CLASS_NAME,"reset", "reset");
        }
		out = null;
		// obs = null;
		count = 0;
		total = 0;
		limit = -1;
		length = -1;
		committed = false;
		_hasWritten = false;
		_hasFlushed = false;
		except = null;
		response = null;
		if(WCCustomProperties.FINISH_RESPONSE_ON_CLOSE) //PK89810
					outputstreamClosed = false;
	}
	/**
	 * Returns the total number of bytes written so far.
	 */
	public int getTotal()
	{
		return total;
	}
	/**
	 * Sets an observer for this output stream. The observer will be
	 * notified when the stream is first written to.
	 */
	public void setObserver(IOutputStreamObserver obs)
	{
		this.obs = obs;
		limit = -1;
	}
	
	/**
	 * Sets an observer for this output stream. The observer will be
	 * notified when the stream is first written to.
	 */
	public void addObserver(IOutputStreamObserver obs)
	{
		if (obs==null){
			this.obs = obs;
		}
		else {
			if (obsList==null){
				this.obsList = new ArrayList <IOutputStreamObserver> ();
			}
			this.obsList.add(obs);
		}
	}
	
	/**
	 * Returns whether the output has been committed or not.
	 */
	public boolean isCommitted()
	{
		if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
			logger.logp(Level.FINE, CLASS_NAME,"isCommitted", "isCommitted: " + committed);
        }
		return committed;
	}
	/**
	 * Checks the output stream for a pending IOException that needs to be
	 * thrown, or a content length that has been exceeded.
	 * @param len the number of bytes about to be written
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
	 * Writes a byte. This method will block until the byte is actually
	 * written.
	 * @param b the byte
	 * @exception IOException if an I/O error has occurred
	 */
	public void write(int b) throws IOException
	{
		//PK89810, If WCCustomProperties.FINISH_RESPONSE_ON_CLOSE is set and stream is already obtained and closed. 
		// User will not be allowed to write more data if the above case is met, outputstreamClosed will be true and default is false.
		if (!(WCCustomProperties.FINISH_RESPONSE_ON_CLOSE) || !outputstreamClosed ) //PK89810
		{
		if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
			logger.logp(Level.FINE, CLASS_NAME,"write", "write --> "+b);
        }
		if (!_hasWritten && obs != null)
		{
			_hasWritten = true;
			if (obsList!=null){
				for (int i=0;i<obsList.size();i++){
					obsList.get(i).alertFirstWrite();
				}
			}
			else {
			obs.alertFirstWrite();
		}
		}
		if (limit > -1)
		{
			if (total >= limit)
			{
				throw new WriteBeyondContentLengthException();
			}
		}
		if (count == buf.length)
		{
			response.setFlushMode(false);
			flushBytes();
			response.setFlushMode(true);
		}
		buf[count++] = (byte) b;
		total++;
	}
		else
		{//PK89810 start
			if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
				logger.logp(Level.FINE, CLASS_NAME,"write", " write not allowed, outputstreamClosed value -->" + outputstreamClosed);
			
		}//PK89810 End
	}
	/**
	 * Writes an array of bytes. This method will block until all the bytes
	 * are actually written.
	 * @param b the data to be written
	 * @param off the start offset of the data
	 * @param len the number of bytes to write
	 * @exception IOException if an I/O error has occurred
	 */
	public void write(byte[] b, int off, int len) throws IOException
	{
		//PK89810, If WCCustomProperties.FINISH_RESPONSE_ON_CLOSE is set and stream is already obtained and closed. 
		// User will not be allowed to write more data if the above case is met, outputstreamClosed will be true and default is false.
		if (!(WCCustomProperties.FINISH_RESPONSE_ON_CLOSE) || !outputstreamClosed ) //PK89810
		{
		if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
			logger.logp(Level.FINE, CLASS_NAME,"write", "write len --> "+len);
        }
		if (len < 0)
		{
			logger.logp(Level.SEVERE, CLASS_NAME,"write", "Illegal.Argument.Trying.to.write.chars");
			throw new IllegalArgumentException();
		}
		if (!_hasWritten && obs != null)
		{
			_hasWritten = true;
			if (obsList!=null){
				for (int i=0;i<obsList.size();i++){
					obsList.get(i).alertFirstWrite();
				}
			}
			else {
			obs.alertFirstWrite();
		}
		}
		if (limit > -1)
		{
			if (total + len > limit)
			{
				len = limit - total;
				except = new WriteBeyondContentLengthException();
			}
		}
		if (len >= buf.length)
		{
			response.setFlushMode(false);
			flushBytes();
			total += len;
			writeOut(b, off, len);
			response.setFlushMode(true);
			check();
			return;
		}
		int avail = buf.length - count;
		if (len > avail)
		{
			response.setFlushMode(false);
			flushBytes();
			response.setFlushMode(true);
		}
		System.arraycopy(b, off, buf, count, len);
		count += len;
		total += len;
		check();
	}
		else
		{//PK89810 start
			if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
				logger.logp(Level.FINE, CLASS_NAME,"write", "write bytes not allowed, outputstreamClosed value --> " + outputstreamClosed);			
		}//PK89810 End
	}
	/**
	 * Flushes the output stream.
	 */
	public void flush() throws IOException
	{
		if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
			logger.logp(Level.FINE, CLASS_NAME,"flush", "flush");
        }
		flushBytes();
	}
	/**
	 * Flushes the output stream bytes.
	 */
	private void flushBytes() throws IOException
	{
		if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
			logger.logp(Level.FINE, CLASS_NAME,"flushBytes", "flushBytes");
        }
		if (!committed)
		{
			if (!_hasFlushed && obs != null)
			{
				_hasFlushed = true;
				if (obsList!=null){
					for (int i=0;i<obsList.size();i++){
						obsList.get(i).alertFirstFlush();
					}
				}
				else {
				obs.alertFirstFlush();
			}
		}
		}
		committed = true;
		if (count > 0)
		{
			writeOut(buf, 0, count);
// PK34562			out.flush();
			count = 0;
		}
		// PK34562 Start
//  		else if ( count == 0 && out != null) {
//			out.flush();
//		}
		else if (count == 0)
		{
	        if(response != null && response.getFlushMode())
	        {
	        	if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //PK34562
	    			logger.logp(Level.FINE, CLASS_NAME,"flushBytes", "Count 0 still flush mode is true , forceful flush");
	            }
	            response.flushBufferedContent();
	        } else if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //PK34562
				logger.logp(Level.FINE, CLASS_NAME,"flushBytes", "flush mode is false");
	        }	 
		}
		// PK34562 End
	}
	/**
	 * Prints a string.
	 * @exception IOException if an I/O error has occurred
	 */
	public void print(String s) throws IOException
	{
		//PK89810, If WCCustomProperties.FINISH_RESPONSE_ON_CLOSE is set and stream is already obtained and closed. 
		// We will not allow user to print more data if the above case is met, outputstreamClosed will be true and default is false.
		if (!(WCCustomProperties.FINISH_RESPONSE_ON_CLOSE) || !outputstreamClosed ) //PK89810
		{
		if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
			logger.logp(Level.FINE, CLASS_NAME,"print", "print --> "+s);
                }
                if(s != null) {  // PQ88872
                    if (!_hasWritten && obs != null)
                    {
                            _hasWritten = true;
            			if (obsList!=null){
            				for (int i=0;i<obsList.size();i++){
            					obsList.get(i).alertFirstWrite();
            				}
            			}
            			else {
                            obs.alertFirstWrite();
                    }
            		}
			int len = s.length();
			if (limit > -1)
			{
				if (total + len > limit)
				{
					len = limit - total;
					except = new WriteBeyondContentLengthException();
				}
			}
			if(this.getBufferSize() != 0){		 // PM44112 
					int off = 0;
					while (len > 0)
					{
						int n = buf.length - count;
						if (n == 0)
						{
							response.setFlushMode(false);
							flushBytes();
							response.setFlushMode(true);
							n = buf.length - count;
						}
						if (n > len)
						{
							n = len;
						}
						//
						// NOTE:  this getBytes call is deprecated ... it doesn't work
						// correctly for multibyte characters, and doesn't take account
						// of the character encoding that the client expects.
						//
						s.getBytes(off, off + n, buf, count);
						
						count += n;
						total += n;
						off += n;
						len -= n;
					}
				} // PM44112 Start
				else if(getBufferSize() == 0) {	
					
					byte[] origBuf=buf; // store the original buff , which is 0
					buf = s.getBytes();	// update buf with the String to print.
					count = buf.length;
					total+=count; // keep the count and the total written
					
					if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
						logger.logp(Level.FINE, CLASS_NAME,"print",  "response Buffersize is set to zero , to print --> "+ count +" total -->"+  total);
					
					
					response.setFlushMode(true);
					flushBytes();				
					buf=origBuf; // set it back to original buff, i.e. 0 value
					
					if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
						logger.logp(Level.FINE, CLASS_NAME,"print",  "after flushbytes, buf --> "+ buf.length +" count --> "+  count+" total -->"+  total);
					
				}// PM44112 End
			check();
		} // PQ88872
	}
		else{//PK89810 start
			if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
				logger.logp(Level.FINE, CLASS_NAME,"print", "print not allowed, outputstreamClosed value --> " + outputstreamClosed);			
		}//PK89810 End
	}
	/**
	 * Closes the servlet output stream.
	 */
	public void close() throws IOException
	{
		if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
			logger.logp(Level.FINE, CLASS_NAME,"close", "close");
        }
		// Were we requested to close the underlying stream ?
		finish();
		try
		{
			out.close();
		}
		catch (Exception ex)
		{
		  com.ibm.ws.ffdc.FFDCFilter.processException(ex, "com.ibm.ws.webcontainer.srt.BufferedServletOutputStream.close", "415", this);
		}
		//PK89810	Start	
		if (WCCustomProperties.FINISH_RESPONSE_ON_CLOSE){
			// If WCCustomProperties.FINISH_RESPONSE_ON_CLOSE is set and stream is already obtained and being closed. 
			// User will not be allowed to print more data ,outputstreamClosed will be set to true and default is false.			 
			outputstreamClosed = true;
			if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) 
				logger.logp(Level.FINE, CLASS_NAME,"close", "outputstreamClosed value --> " + outputstreamClosed);			
		}//PK89810 End
    }
	public void setLimit(int lim)
	{
		limit = lim;
	}
	
	public void setResponse(IResponse resp) {
		response = resp;
	}
	
	/*
	 * Writes to the underlying stream
	 */
	protected void writeOut(byte[] buf, int offset, int len) throws IOException
	{
		if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
			logger.logp(Level.FINE, CLASS_NAME,"writeOut", "writeOut len --> "+len);
        }
		out.write(buf, offset, len);
	}
	public int getBufferSize()
	{
		return bufferSize;
	}
	public void setBufferSize(int size)
	{
		if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
			logger.logp(Level.FINE, CLASS_NAME,"setBufferSize", "setBufferSize --> "+size);
        }
		if (total > 0)
		{
			if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
				logger.logp(Level.FINE, CLASS_NAME,"setBufferSize", "setBufferSize(): illegal state--> already wrote " + total + " bytes");
            }
			throw new IllegalStateException(nls.getString("Cannot.set.buffer.size.after.data", "Can't set buffer size after data has been written to stream"));
		}
		initNewBuffer(out, size);
	}
	public void clearBuffer()
	{
		if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
			logger.logp(Level.FINE, CLASS_NAME,"clearBuffer", "clearBuffer");
        }
		if (isCommitted())
		{
			if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {
				logger.logp(Level.FINE, CLASS_NAME,"clearBuffer", "clearBuffer(): illegal state--> stream is committed ");
            }
			throw new IllegalStateException("clearBuffer(): illegal state--> stream is committed ");
		}
		total = 0;
		count = 0;
		_hasWritten=false;
	}
	public void flushBuffer() throws IOException
	{
		if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
			logger.logp(Level.FINE, CLASS_NAME,"flushBuffer", "flushBuffer");
        }
		flush();
	}
//BEGIN ZHJ	
	public void writeByteBuffer(WsByteBuffer[] buf) {
		if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
			logger.entering(CLASS_NAME, "writeByteBuffer");
        }
		if (!committed)
		{
			if (!_hasFlushed && obs != null)
			{
				_hasFlushed = true;
				obs.alertFirstFlush();
			}
		}
		committed = true;
		((ByteBufferWriter)out).writeByteBuffer(buf);
		if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
			logger.exiting(CLASS_NAME, "writeByteBuffer");
        }
	}
//END ZHJ

}
