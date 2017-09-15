/*******************************************************************************
 * Copyright (c) 2006, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.jfapchannel;

import java.util.ArrayList;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.ws.sib.jfapchannel.buffer.WsByteBuffer;
import com.ibm.ws.sib.jfapchannel.buffer.WsByteBufferPool;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * This class represents one or more WsByteBuffers to the JFap channel. This class manages the
 * allocation of byte buffers automatically depending on the data placed in them.
 * <p>
 * The JFapByteBuffer operates in two modes - write only mode, or read only mode. The mode that the
 * buffer is in depends on how it was constructed or how it was reset (the operations are equivalent
 * but depend on whether the object is being pooled).
 * <p>
 * The buffer enters write-only mode using the empty constructor or the empty <code>reset()</code>
 * method.
 * <p>
 * In write mode, data should be written in a buffer using the putXXX() methods. Once putting is
 * complete the <code>prepareForTransmission()</code> method should be called which readies the
 * underlying byte buffers for transmission. Once this has been called the buffer can no longer be
 * written to. The underlying byte buffers that are to be transmitted can then be retrieved using
 * the <code>getBuffersForTransmission</code> method. A buffer that is transmitted across the JFap
 * channel does not need to be manually released - the JFap channel will perform this operation
 * once the buffer has been successfully sent.
 * <p>
 * The buffer enters read-only mode using the contructor or reset() methods that take a WsByteBuffer
 * or JFapChannel ReceivedData object.
 * <p>
 * In read mode, data can be retrieved using the getXXX() methods. Once the reading has completed,
 * <code>release()</code> must be called to free up the underlying byte buffers. If the byte buffers
 * contain data which should not be released the <code>releasePreservingBuffers()</code> method
 * must be called instead to ensure any other object storage is released. Once released, the buffer
 * cannot be read from.
 *
 * @author Gareth Matthews
 */
public abstract class JFapByteBuffer
{
   /** Register Class with Trace Component */
   private static final TraceComponent tc = SibTr.register(JFapByteBuffer.class,
                                                           JFapChannelConstants.MSG_GROUP,
                                                           JFapChannelConstants.MSG_BUNDLE);

   /** Constant to use when denoting dumping of the entire buffer */
   public static final int ENTIRE_BUFFER = -1;

   /** The default size of newly allocated WsByteBuffers */
   public static final int DEFAULT_BUFFER_SIZE = 200;

   /** The pool manager */
   private static WsByteBufferPool poolMan = WsByteBufferPool.getInstance();

   /** A flag to indicate whether this buffer can be written to */
   private boolean valid;

   /** A flag to indicate when theis buffer has been released */
   private boolean released = false;

   /** The list of WsByteBuffer objects that will be transmitted */
   private ArrayList<WsByteBuffer> dataList = new ArrayList<WsByteBuffer>();

   /** When this is being used for a received byte buffer, it is stored for convinience here */
   protected WsByteBuffer receivedBuffer = null;

   /** The received data */
   protected ReceivedData receivedData = null;


   // ******************************************************************************************* //
   // Construction


   /**
    * This constructor should be called from code that wants to create a buffer for transmission.
    * Once this has been constructed, items should be placed into the buffer using the putXXX()
    * methods before finally calling the getBufferForTransmission() method.
    */
   public JFapByteBuffer()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "<init>");

      reset();

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "<init>");
   }

   /**
    * This constructor should be used by code that has received some data. Once this has been
    * contructed the buffer should be read by calling the getXXX() methods.
    *
    * @param _receivedData The data received from the JFap channel.
    */
   public JFapByteBuffer(ReceivedData _receivedData)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "<init>", receivedData);

      reset(_receivedData);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "<init>");
   }

   /**
    * This constructor should be used by code that wishes to wrap a WsByteBuffer directly. This is
    * usually in the case where data has been received. Using this constructor the buffer is
    * automatically flipped so that data can immediately be read.
    * <p>
    * A JFapByteBuffer constructed in this way is automatically marked as read-only.
    *
    * @param _buffer The data received from the JFap channel.
    */
   public JFapByteBuffer(WsByteBuffer _buffer)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "<init>", receivedData);

      reset(_buffer);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "<init>");
   }



   // ******************************************************************************************* //
   // Reset methods

   /**
    * This method should be called from code that wants to reset a buffer for transmission.
    * Once this has been called, items should be placed into the buffer using the putXXX()
    * methods before finally calling the getBufferForTransmission() method.
    */
   public synchronized void reset()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "reset");

      valid = true;
      released = false;

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "reset");
   }

   /**
    * This method should be used by code that has received some data. Once this has been
    * called the buffer should be read by calling the getXXX() methods.
    * <p>
    * The JFapByteBuffer is automatically marked as read-only when calling this method.
    *
    * @param _receivedData The data received from the JFap channel.
    */
   public synchronized void reset(ReceivedData _receivedData)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "reset", _receivedData);

      this.receivedBuffer = _receivedData.getBuffer();
      this.receivedData = _receivedData;
      this.receivedBuffer.flip();

      // Not valid to write data to this buffer
      valid = false;
      released = false;

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "reset");
   }

   /**
    * This method should be used by code that wishes to wrapper a WsByteBuffer directly. This is
    * usually in the case where data has been received. Using this method the buffer is
    * automatically flipped so that data can immediately be read.
    * <p>
    * The JFapByteBuffer is automatically marked as read-only when calling this method.
    *
    * @param _buffer The data received from the JFap channel.
    */
   public synchronized void reset(WsByteBuffer buffer)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "reset", buffer);

      this.receivedBuffer = buffer;
      this.receivedBuffer.flip();

      // Not valid to write data to this buffer
      valid = false;
      released = false;

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "reset");
   }


   // ******************************************************************************************* //
   // Methods used when putting into the buffer


   /**
    * Puts a single byte into the byte buffer.
    *
    * @param item
    */
   public synchronized void put(byte item)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "put", Byte.valueOf(item));

      checkValid();
      getCurrentByteBuffer(1).put(item);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "put");
   }

   /**
    * Puts a short into the byte buffer.
    *
    * @param item
    */
   public synchronized void putShort(short item)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "putShort", Short.valueOf(item));

      checkValid();
      getCurrentByteBuffer(2).putShort(item);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "putShort");
   }

   /**
    * Puts an int into the byte buffer.
    *
    * @param item
    */
   public synchronized void putInt(int item)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "putInt", Integer.valueOf(item));

      checkValid();
      getCurrentByteBuffer(4).putInt(item);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "putInt");
   }

   /**
    * Puts a long into the byte buffer.
    *
    * @param item
    */
   public synchronized void putLong(long item)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "putLong", Long.valueOf(item));

      checkValid();
      getCurrentByteBuffer(4).putLong(item);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "putLong");
   }

   /**
    * Puts a byte[] into the byte buffer.
    *
    * @param item
    */
   public synchronized void put(byte[] item)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "put", item);

      checkValid();
      getCurrentByteBuffer(item.length).put(item);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "put");
   }

   /**
    * This method is used to add the data in the argument into the buffer. However, this method will
    * not make a copy of the data. Instead the byte[] passed in will back the newly created buffer.
    * By implication, any modifications done to the byte[] passed in will affect the byte buffer
    * that is created from it.
    * <p>
    * Normally, a call to <code>wrap()</code> will cause a newly created buffer to be returned.
    * This method however, simply causes the new buffer to be added onto the end of whatever data
    * is already in the buffer.
    *
    * @param item
    */
   public synchronized void wrap(byte[] item)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "wrap", item);

      wrap(item, 0, item.length);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "wrap");
   }

   /**
    * This method is used to add the data in the argument into the buffer. However, this method will
    * not make a copy of the data. Instead the byte[] passed in will back the newly created buffer.
    * By implication, any modifications done to the byte[] passed in will affect the byte buffer
    * that is created from it.
    * <p>
    * Normally, a call to <code>wrap()</code> will cause a newly created buffer to be returned.
    * This method however, simply causes the new buffer to be added onto the end of whatever data
    * is already in the buffer.
    *
    * @param item
    * @param offset
    * @param length
    */
   public synchronized void wrap(byte[] item, int offset, int length)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "wrap",
                                           new Object[]{item, offset, length});

      // First see if there is anything in the data list. If there is, complete that buffer so that
      // we can start a new one with our item backing it
      if (dataList.size() > 0)
      {
         WsByteBuffer lastBuffer = dataList.get(dataList.size() - 1);
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Last buffer:", lastBuffer);

         lastBuffer.flip();
      }

      // Now create a new buffer from the backing byte[]
      WsByteBuffer newBuffer = poolMan.wrap(item, offset, length);
      // If the offset is non-zero then the resulting buffer returned has the position set to the
      // offset. This is a bit crap, as clearly if we specified an offset to start at, we didn't
      // care about the data before it. We can get around this however by slicing the buffer which
      // causes the current position to becoming position 0 which means good things happen when
      // we later flip the buffer.
      newBuffer = newBuffer.slice();

      // We must ensure that the position is at the end of the buffer so that we can either continue
      // adding data to this JFapByteBuffer (i.e. we'll create a new buffer for the subsequent data)
      // or the right thing will happen when prepareForTransmission() is called.
      newBuffer.position(newBuffer.limit());

      // And add it to the data list
      dataList.add(newBuffer);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "wrap");
   }

   /**
    * This method sets all the byte buffers that we have in the list as read only.
    */
   public synchronized void setReadOnly()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "setReadOnly");

      // And actually mark the real buffers as read-only too
      for (int x = 0; x < dataList.size(); x++)
      {
         WsByteBuffer buff = dataList.get(x);
         buff.setReadOnly(true);
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "setReadOnly");
   }

   /**
    * This method prepares the byte buffers wrapped by this class for transmission. In practise
    * this means that all the buffers are flipped and the number of bytes that will be transmitted
    * is returned.
    *
    * @return Returns the amount of data that is ready to be sent.
    */
   public synchronized long prepareForTransmission()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "prepareForTransmission");

      checkValid();
      valid = false;

      // Get the last buffer and flip it. Then we can simply return the list of buffers
      if (dataList.size() > 0)
      {
         WsByteBuffer buff = dataList.get(dataList.size() - 1);
         buff.flip();
      }

      long sendAmount = 0;
      for (int x = 0; x < dataList.size(); x++)
      {
         WsByteBuffer buff = dataList.get(x);
         sendAmount += buff.remaining();
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "prepareForTransmission", Long.valueOf(sendAmount));
      return sendAmount;
   }

   /**
    * This method is called just before this buffer is due to be transmitted by the JFap channel.
    * When calling this method the underlying byte buffer is prepared by setting the correct limits
    * and the buffer is added to a List that is returned. Once this method is called the buffer
    * may not be used again and any attempt to modify the data will cause a RuntimeException to be
    * thrown. This method can only be called once.
    *
    * @return Returns a List containing one or more WsByteBuffer's with the data in it.
    */
   public synchronized WsByteBuffer[] getBuffersForTransmission()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getBufferForTransmission");

      // Ensure the buffer has been prepared
      checkNotValid();

      WsByteBuffer[] bufferArray = new WsByteBuffer[dataList.size()];
      for (int x = 0; x < dataList.size(); x++)
      {
         bufferArray[x] = dataList.get(x);
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getBufferForTransmission", dataList);
      return bufferArray;
   }


   // ******************************************************************************************* //
   // Methods used when getting from the buffer


   /**
    * Reads a single byte from the byte buffer and returns the result as a byte.
    *
    * @return Returns a byte
    */
   public synchronized byte get()
   {
      checkReleased();
      return receivedBuffer.get();
   }

   /**
    * Reads the specified number of bytes from the byte buffer and returns the result as a byte[].
    *
    * @param numberOfBytes
    *
    * @return Returns a byte[]
    */
   public synchronized byte[] get(int numberOfBytes)
   {
      checkReleased();
      byte[] bytes = new byte[numberOfBytes];
      receivedBuffer.get(bytes);
      return bytes;
   }


   /**
    * Reads 2 bytes from the byte buffer and returns the result as a short.
    *
    * @return Returns a short
    */
   public synchronized short getShort()
   {
      checkReleased();
      return receivedBuffer.getShort();
   }

   /**
    * Reads 4 bytes from the byte buffer and returns the result as a int.
    *
    * @return Returns a int
    */
   public synchronized int getInt()
   {
      checkReleased();
      return receivedBuffer.getInt();
   }

   /**
    * Reads 8 bytes from the byte buffer and returns the result as a long.
    *
    * @return Returns a long
    */
   public synchronized long getLong()
   {
      checkReleased();
      return receivedBuffer.getLong();
   }

   /**
    * @return Returns true if there is more data remaining to be read.
    */
   public synchronized boolean hasRemaining()
   {
      checkReleased();
      return receivedBuffer.hasRemaining();
   }

   /**
    * This method will release any storage held by any of the byte buffers. It will also mark
    * itself as released so that any subsequent attempts to get from it will cause an
    * SIErrorException to be thrown.
    */
   public synchronized void release()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "release");

      released = true;
      valid = false;

      if (receivedBuffer != null)
      {
         receivedBuffer.release();
         receivedBuffer = null;
      }
      if (receivedData != null)
      {
         receivedData.release();
         receivedData = null;
      }

      if (dataList != null)
      {
         for (int x = 0; x < dataList.size(); x++)
         {
            dataList.get(x).release();
         }

         dataList.clear();
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "release");
   }

   /**
    * This method releases the ReceivedData instance but does not release any of the WsByteBuffers
    * associated with this JFapByteBuffer. This can be used instead of the normal
    * <code>release()</code> call where the actual buffer has been used as the underlying backing
    * storage for another object (such as a message) and we do not want that to be released.
    */
   public synchronized void releasePreservingBuffers()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "releasePreservingBuffers");

      released = true;
      valid = false;

      if (receivedData != null)
      {
         receivedData.release();
         receivedData = null;
      }

      // Simply null out the received buffer
      receivedBuffer = null;
      dataList.clear();

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "releasePreservingBuffers");
   }


   // ******************************************************************************************* //
   // Private helper methods


   /**
    * @throws SIErrorException if this buffer is not valid at this time.
    */
   protected void checkValid() throws SIErrorException
   {
      if (!valid) throw new SIErrorException(
         TraceNLS.getFormattedMessage(JFapChannelConstants.MSG_BUNDLE, "BUFFER_NOT_VALID_SICJ0074", null, null)
      );
   }

   /**
    * @throws SIErrorException if this buffer is valid at this time.
    */
   protected void checkNotValid() throws SIErrorException
   {
      if (valid) throw new SIErrorException(
         TraceNLS.getFormattedMessage(JFapChannelConstants.MSG_BUNDLE, "BUFFER_NOT_PREPARED_SICJ0075", null, null)
      );
   }

   /**
    * @throws SIErrorException if this buffer has already been released.
    */
   protected void checkReleased() throws SIErrorException
   {
      if (released) throw new SIErrorException(
         TraceNLS.getFormattedMessage(JFapChannelConstants.MSG_BUNDLE, "BUFFER_ALREADY_RELEASED_SICJ0076", null, null)
      );
   }

   /**
    * This method will return a byte buffer that can be written to for the amount of data that
    * needs to be written. If there is no room left in the current byte buffer, a new one will be
    * created and added to the list.
    *
    * @param sizeNeeded The amount of data that needs to be written
    *
    * @return Returns a WsByteBuffer that can be used.
    */
   protected WsByteBuffer getCurrentByteBuffer(int sizeNeeded)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getCurrentByteBuffer", Integer.valueOf(sizeNeeded));

      WsByteBuffer byteBuffer = null;

      // First have a look in the dataList for a buffer.
      if (dataList.size() == 0)
      {
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Creating first buffer");

         // This is the first piece of data to be written into the buffer. As such, we simply
         // allocate a new buffer and return that
         dataList = new ArrayList<WsByteBuffer>();

         byteBuffer = createNewWsByteBuffer(sizeNeeded);

         dataList.add(byteBuffer);
      }
      else
      {
         // Otherwise get the last byte buffer in the list
         byteBuffer = dataList.get(dataList.size() - 1);
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Last buffer: ", byteBuffer);

         // Now check and see if there is enough room in there for data that needs to be written.
         // If there is enough room, great - we return that buffer. Otherwise we need to complete
         // the current buffer and start a new one.
         if (byteBuffer.remaining() < sizeNeeded)
         {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "No room in buffer, creating a new one");

            // First flip the finished one
            byteBuffer.flip();

            // Now create a new one and add it onto the list
            byteBuffer = createNewWsByteBuffer(sizeNeeded);
            dataList.add(byteBuffer);
         }
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getCurrentByteBuffer", byteBuffer);
      return byteBuffer;
   }

   /**
    * This method will create a new WsByteBuffer. By default it will be created to hold 200 bytes
    * but if the size needed is larger than this then the buffer will simply be allocated to hold
    * the exact number of bytes requested.
    *
    * @param sizeNeeded The amount of data waiting to be put into a buffer.
    *
    * @return Returns a new WsByteBuffer
    */
   private WsByteBuffer createNewWsByteBuffer(int sizeNeeded)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "createNewWsByteBuffer", Integer.valueOf(sizeNeeded));

      if (sizeNeeded < DEFAULT_BUFFER_SIZE)
      {
         sizeNeeded = DEFAULT_BUFFER_SIZE;
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Allocating a buffer of size: " + sizeNeeded);

      WsByteBuffer buffer = poolMan.allocate(sizeNeeded);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "createNewWsByteBuffer", buffer);
      return buffer;
   }

   /**
    * This method should be used in the unit test environment where we want to mock up a buffer
    * to appear as if it was received across the wire. When this method is called the following
    * occurs:
    * <ol>
    *   <li>The buffers that are currently waiting in the data list are prepared (flipped) and
    *       copied into a single byte buffer.</li>
    *   <li>The single byte buffer is assigned into the receivedBuffer variable (which now enables
    *       the getXXX() methods</li>
    *   <li>The data list is cleared meaning that the putXXX() methods will now fail.</li>
    * </ol>
    * <p>
    * You would be ill-advised to use this method in the main-line code.
    */
   public synchronized void prepareForReception()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "prepareForReception");

      long length = prepareForTransmission();

      // Allocate another WsByteBuffer to copy it all into
      WsByteBuffer newBuffer = createNewWsByteBuffer((int) length);
      // Now copy in the buffers to the new one
      for (int x = 0; x < dataList.size(); x++)
      {
         WsByteBuffer buff = dataList.get(x);
         newBuffer.put(buff);
      }
      // Now initialise ourselves with the new buffer
      reset(newBuffer);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "prepareForReception");
   }

   /**
    * @see java.lang.Object#toString()
    */
   public String toString()
   {
      return "JFapByteBuffer@" + Integer.toHexString(System.identityHashCode(this)) + ": " +
             "{ valid=" + valid + ", released=" + released + ", dataList=" + dataList +
             ", receivedData=" + receivedData + ", receivedBuffer=" + receivedBuffer + " }";
   }


   // ******************************************************************************************* //
   // Dump helper methods


   /**
    * This method will dump the number of bytes from this instance of the JFapByteBuffer. To dump
    * out the entire contents of all buffers, a value of JFapByteBuffer.ENTIRE_BUFFER can be passed
    * in.
    * <p>Note this method should be guarded with an if (isxxxEnabled()) to ensure that it is not
    * executed when tracing is turned off.
    *
    * @param bytesToDump
    */
   public synchronized void dump(Object _this, TraceComponent _tc, int bytesToDump)
   {
      int bytesRemainingToDump = bytesToDump;

      if (receivedBuffer != null)
      {
         SibTr.debug(this, tc, "Buffer contains a single underlying WsByteBuffer:");
         dump(_this, _tc, receivedBuffer, bytesRemainingToDump, false);
      }
      else
      {
         SibTr.debug(this, tc, "Buffer contains " + dataList.size() + " underlying WsByteBuffer(s):");

         for (int x = 0; x < dataList.size(); x++)
         {
            WsByteBuffer buff = dataList.get(x);

            dump(_this, _tc, buff, bytesRemainingToDump, true);

            // If we are not dumping the entire buffer
            if (bytesToDump != ENTIRE_BUFFER)
            {
               // Was there more in the buffer than we could dump anyway?
               if (buff.remaining() >= bytesRemainingToDump)
               {
                  // If so - we are done
                  bytesRemainingToDump = 0;
               }
               else
               {
                  // Otherwise, decrement the amount left to dump
                  bytesRemainingToDump -= buff.remaining();
               }

               // Break out if nothing more to do
               if (bytesRemainingToDump <= 0) break;
            }
         }
      }
   }

   /**
    * Returns the String that represents a dump of the bytes received in this comms byte buffer.
    * This method is not intended to be used for buffers that are being contructed for outbound use.
    *
    * @param bytesToDump
    *
    * @return
    */
   public synchronized String getDumpReceivedBytes(int bytesToDump)
   {
      String dump = null;

      if (receivedBuffer != null)
      {
         dump = getDumpBytes(receivedBuffer, bytesToDump, false);
      }

      return dump;
   }

   /**
    * This static method can be used to dump out the contents of the specified byte buffer to
    * SibTr.debug. The number of bytes dumped can be specified in the second parameter. To dump
    * out the entire contents of the buffer, a value of JFapByteBuffer.ENTIRE_BUFFER can be passed
    * in.
    * <p>Note this method should be guarded with an if (isxxxEnabled()) to ensure that it is not
    * executed when tracing is turned off.
    *
    * @param buffer The buffer to dump
    * @param bytesToDump The number of bytes to dump out
    */
   public static void dump(Object _this, TraceComponent _tc, WsByteBuffer buffer, int bytesToDump, boolean rewind)
   {
      StringBuffer sb = new StringBuffer();
      sb.append("\nBuffer hashcode:  ");
      sb.append(Integer.toHexString(System.identityHashCode(buffer)));
      sb.append("\nBuffer position:  ");
      sb.append(buffer.position());
      sb.append("\nBuffer remaining: ");
      sb.append(buffer.remaining());
      sb.append("\n");

      SibTr.debug(_this, _tc, sb.toString());
      SibTr.debug(_this, _tc, getDumpBytes(buffer, bytesToDump, rewind));
   }

   /**
    * Returns a dump of the specified number of bytes of the specified buffer.
    *
    * @param buffer
    * @param bytesToDump
    *
    * @return Returns a String containing a dump of the buffer.
    */
   private static String getDumpBytes(WsByteBuffer buffer, int bytesToDump, boolean rewind)
   {
      // Save the current position
      int pos = buffer.position();
      if (rewind)
      {
         buffer.rewind();
      }

      byte[] data = null;
      int start;
      int count = bytesToDump;
      if (count > buffer.remaining() || count == ENTIRE_BUFFER) count = buffer.remaining();

      if (buffer.hasArray())
      {
         data = buffer.array();
         start = buffer.arrayOffset() + buffer.position();
      }
      else
      {
         data = new byte[count];
         buffer.get(data);
         start = 0;
      }

      String strData = "Dumping "+count+" bytes of buffer data:\r\n";
      if (count > 0) strData += SibTr.formatBytes(data, start, count);

      // Return the position to where it should be
      if (rewind) buffer.position(pos);

      return strData;
   }
}
