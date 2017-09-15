/*******************************************************************************
 * Copyright (c) 2012, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.api.jms.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UTFDataFormatException;
import java.io.UnsupportedEncodingException;
import java.util.Vector;

import javax.jms.BytesMessage;
import javax.jms.IllegalStateException;
import javax.jms.JMSException;
import javax.jms.MessageEOFException;
import javax.jms.MessageFormatException;
import javax.jms.MessageNotReadableException;
import javax.jms.MessageNotWriteableException;
import javax.jms.ResourceAllocationException;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.api.jms.ApiJmsConstants;
import com.ibm.ws.sib.mfp.JsJmsBytesMessage;
import com.ibm.ws.sib.mfp.JsJmsMessage;
import com.ibm.ws.sib.mfp.MessageCreateFailedException;
import com.ibm.ws.sib.utils.HexString;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * Implementation of BytesMessage for Jetstream.
 * 
 * @author kingdon (based on existing MQJMS code)
 * 
 */
public class JmsBytesMessageImpl extends JmsMessageImpl implements javax.jms.BytesMessage
{

    /**
     * This svUID assigned at version 1.27 of this class.
     */
    private static final long serialVersionUID = 1691954622519522063L;

    // variables

    /** Reference to the Jetstream message object which underpins this instance. */
    private JsJmsBytesMessage jsBytesMsg;

    /**
     * boolean to indicate that the js message already has the body contents set in it.
     * This is used to avoid multiple calls to the (expensive) _exportBody call when a
     * message is repeatedly sent.
     */
    private boolean bodySetInJsMsg = false;

    /**
     * Indicates wither 'writeBytes' has been called or not.
     * Only relevant when producer has promised not to modify the payload after setting it
     */
    private boolean writeByteArrayCalled = false;

    /**
     * This records the encoding that was used in the last call to exportBody.
     * If it is different then we have to re-export, regardless of the value of
     * bodySetInJsMsg.
     */
    private int lastEncoding;

    /**
     * Flag to indicate that this message has been constructed using the (JsJmsBytesMessage) constructor,
     * but has not yet had the streams set up for reading. This permits a lazy init optimisation for
     * performance tests.
     */
    private boolean requiresInit = false;

    // When the message is being constructed (readOnly = false), the
    // data that has been written so far is held in a
    // ByteArrayOutputStream. The writeStream DataOutputStream is
    // overlaid on the ByteArrayOutputStream and provides most of the
    // formatting of basic datatypes for the writeXXX methods. Note that
    // numeric datatypes are always held in standard Java encoding in
    // the ByteArrayOutputStream, and are only 'byte-swapped' into
    // another format - if at all - after the message has been
    // completely constructed.
    transient private ByteArrayOutputStream _writeBytes;
    transient private DataOutputStream writeStream;

    // As just mentioned, the byte array constructed by the write
    // methods will initially hold numeric dataypes encoded in the
    // standard Java manner. This may not be what is wanted, so the
    // following two arrays are used to track the positions and types of
    // the data in the buffer, so that they may be byte swapped at
    // send() time if necessary.  We are interested here in the various
    // 'integer' types - e.g. long, integer, char
    private static final int ARRAY_SIZE = 20; // allow for 20 items initially
    private int integer_count; // number of numeric items recorded in the current arrays
    private int[] integer_offsets; // offset of start of each numeric item
    private int[] integer_sizes; // length in byte of the item in question
    private Vector integers; // vector to put the arrays in if they overflow

    // Separate vectors are used to hold floating point values. These
    // are likely to be used infrequently, so it's reasonable to hold
    // all the values in a single vector.
    private Vector float_offsets;
    private Vector float_values;

    // When the message is being read (readOnly = true), the bytes that
    // comprise the message body are held in the dataBuffer byte
    // array. The numeric encoding used in this byte array is given by
    // the following two data members. The readStream input stream is
    // overlaid on the byte array and keeps track of how far the readXXX
    // methods have progressed through it.
    private byte[] dataBuffer; // the raw data in the message
    private int dataStart; // offset of first genuine byte in databuffer
    private int integerEncoding = ApiJmsConstants.ENC_INTEGER_NORMAL; // defaults to standard Java encoding
    private int floatEncoding = ApiJmsConstants.ENC_FLOAT_IEEE_NORMAL; // defaults to standard Java encoding

    // stream used for read methods
    transient private ByteArrayInputStream readStream;

    /**
     * Offset into readStream if message is in readOnly mode.
     * Used during message serialisation.
     */
    private int streamOffset;

    // The markInUse indicates that a position in the input stream has
    // been marked, to allow the read methods to reset the current
    // position should they end with an exception.
    private boolean markInUse = false;

    /**
     * This variable holds a cache of the message toString at the Message level.
     * A separate cache holds the subclass information. The cache is invalidated
     * by changing any property of the message.
     */
    private transient String cachedBytesToString = null;

    // *************************** TRACE INITIALIZATION **************************
    private static TraceComponent tc = SibTr.register(JmsBytesMessageImpl.class, ApiJmsConstants.MSG_GROUP_EXT, ApiJmsConstants.MSG_BUNDLE_EXT);

    // ************************ CONSTRUCTORS *************************

    public JmsBytesMessageImpl() throws JMSException {
        // Calling the superclass no-args constructor in turn leads to the
        // instantiateMessage method being called, which we override to return
        // a bytes message.
        super();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "JmsBytesMessageImpl");

        // calling clear body will ensure that the output streams are created
        // ready to accept data.
        clearBody();

        messageClass = CLASS_BYTES;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "JmsBytesMessageImpl");
    }

    /**
     * Construct a jetstream jms message from a (possibly non-jetstream)
     * vanilla jms message.
     */
    JmsBytesMessageImpl(BytesMessage bytesMessage) throws JMSException {

        // copy message headers and properties.
        super(bytesMessage);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "JmsBytesMessageImpl", bytesMessage);

        // set-up this class's state (i.e. do what this() does).
        clearBody();

        // go to start of stream (unfortunately also makes body read-only).
        bytesMessage.reset();

        // allocate buffer used to copy bytes between messages.
        int bufferSize = 32768; // MAX BUFFER SIZE
        long bodyLength = bytesMessage.getBodyLength();
        if (bodyLength < bufferSize) {
            bufferSize = (int) bodyLength;
        }
        byte[] buffer = new byte[bufferSize];

        // copy bytes.
        int nRead = 0;
        while ((nRead = bytesMessage.readBytes(buffer)) > 0)
            writeBytes(buffer, 0, nRead);

        messageClass = CLASS_BYTES;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "JmsBytesMessageImpl");
    }

    /**
     * This constructor is used by the JmsMessage.inboundJmsInstance method (static)
     * in order to provide the inbound message path from MFP component to JMS component.
     */
    JmsBytesMessageImpl(JsJmsBytesMessage newMsg, JmsSessionImpl newSess) {
        // Pass this object to the parent class so that it can keep a reference.
        super(newMsg, newSess);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "JmsBytesMessageImpl", new Object[] { newMsg, newSess });

        // Store the reference we are given
        jsBytesMsg = newMsg;

        // set the flag to indicate that the message is not yet ready to be read
        requiresInit = true;

        // message arrives writeable, so we need to set readonly here
        setBodyReadOnly();
        messageClass = CLASS_BYTES;

        // Note that we do NOT initialize the defaults for inbound messages.
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "JmsBytesMessageImpl");
    }

    //////////////////////////////////////////////////////////////////////////////
    // Interface methods
    //////////////////////////////////////////////////////////////////////////////

    /**
     * Clear out the message body. All other parts of the message are left
     * untouched.
     * 
     * @exception JMSException if JMS fails to due to some internal JMS error.
     */
    @Override
    public void clearBody() throws javax.jms.JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "clearBody");

        // Set the message into write-only mode, handle generic actions
        super.clearBody();

        // Clear the locally stored reference to body of the message, and ensure the
        // 'read' stream is set to null
        dataBuffer = null;
        dataStart = 0; // @P2A
        readStream = null;

        // remove the encoding and character set properties as these pertain to the
        // body which is being cleared.
        // SIB0121: also clear the underlying MFP payload
        if (jsBytesMsg != null) {
            jsBytesMsg.setBytes(null);
            jsBytesMsg.setObjectProperty(ApiJmsConstants.ENCODING_PROPERTY, null);
            jsBytesMsg.setObjectProperty(ApiJmsConstants.CHARSET_PROPERTY, null);
        }

        integerEncoding = ApiJmsConstants.ENC_INTEGER_NORMAL; // reset to standard Java encoding
        floatEncoding = ApiJmsConstants.ENC_FLOAT_IEEE_NORMAL; // reset to standard Java encoding

        // Invalidate the cached toString object.
        cachedBytesToString = null;

        // This class has different behaviour depending on the whether the
        // producer might modify the payload after it's been set or not
        if (!producerWontModifyPayloadAfterSet) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "Producer might modify the payload after set - encoding is required, so reinitialise encoding relating variables");

            // Prepare a new stream for the client to write to
            _writeBytes = new ByteArrayOutputStream();
            writeStream = new DataOutputStream(_writeBytes);

            // Initialise the arrays used to record the position of numeric items
            integer_count = 0;
            // number of integer items recorded in the current arrays
            integer_offsets = new int[ARRAY_SIZE];
            // offset of start of each numeric item
            integer_sizes = new int[ARRAY_SIZE];
            // length in byte of the item in question
            if (integers != null)
                integers.removeAllElements();
            // arrays which we filled up are stored in this vector - remove them all

            if (float_offsets != null)
                float_offsets.removeAllElements();
            if (float_values != null)
                float_values.removeAllElements();

            // Ensure that the new data gets exported when the time comes.
            bodySetInJsMsg = false;
        }

        else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "Producer has promised not to modify payload - ensure write streams used for encoding are null");

            // Ensure the write stream variables are null - they are never used when the
            // producer has promised not to modify the payload after it's been set
            _writeBytes = null;
            writeStream = null;

            // Ensure the producer promise flag is checked correctly in the writeBytes method
            writeByteArrayCalled = false;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "clearBody");
    }

    /**
     * Read a <code>boolean</code> from the stream message.
     * 
     * @return the <code>boolean</code> value read.
     * 
     * @exception MessageNotReadableException if message in write-only mode.
     * @exception JMSException if JMS fails to read message due to
     *                some internal JMS error.
     * @exception MessageEOFException if end of message stream
     */
    @Override
    public boolean readBoolean() throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "readBoolean");

        // Check that we are in read mode
        checkBodyReadable("readBoolean");
        if (requiresInit)
            lazyInitForReading();

        // Read the boolean from the input stream
        int byteRead = readStream.read(); // read the byte

        if (byteRead < 0) { // this is how read() signals EOF
            throw (JMSException) JmsErrorUtils.newThrowable(
                                                            MessageEOFException.class,
                                                            "END_BYTESMESSAGE_CWSIA0183",
                                                            null,
                                                            tc);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "readBoolean", (byteRead != 0));
        return (byteRead != 0);
    }

    /**
     * Read a signed 8-bit value from the stream message.
     * 
     * @return the next byte from the stream message as a signed 8-bit
     *         <code>byte</code>.
     * 
     * @exception MessageNotReadableException if message in write-only mode.
     * @exception MessageEOFException if end of message stream
     * @exception JMSException if JMS fails to read message due to
     *                some internal JMS error.
     */
    @Override
    public byte readByte() throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "readByte");

        // Check that we are in read mode
        checkBodyReadable("readByte");
        if (requiresInit)
            lazyInitForReading();

        // Read the byte from the input stream
        int byteRead = readStream.read(); // read the byte

        if (byteRead < 0) // this is how read() signals EOF
            throw (JMSException) JmsErrorUtils.newThrowable(
                                                            MessageEOFException.class,
                                                            "END_BYTESMESSAGE_CWSIA0183",
                                                            null,
                                                            tc);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "readByte", byteRead);
        return (byte) byteRead;
    }

    /**
     * Read a byte array from the bytes message stream.
     * 
     * <P>If the length of array <code>value</code> is less than
     * the bytes remaining to be read from the stream, the array should
     * be filled. A subsequent call reads the next increment, etc.
     * 
     * <P>If the bytes remaining in the stream is less than the length of
     * array <code>value</code>, the bytes should be read into the array.
     * The return value of the total number of bytes read will be less than
     * the length of the array, indicating that there are no more bytes left
     * to be read from the stream. The next read of the stream returns -1.
     * 
     * @param value the buffer into which the data is read.
     * 
     * @return the total number of bytes read into the buffer, or -1 if
     *         there is no more data because the end of the stream has been reached.
     * 
     * @exception MessageNotReadableException if message in write-only mode.
     * @exception JMSException if JMS fails to read message due to
     *                some internal JMS error.
     */

    @Override
    public int readBytes(byte[] value) throws JMSException {
        return readBytes(value, value.length);
    }

    /**
     * Read a portion of the bytes message stream.
     * 
     * <P>If the length of array <code>value</code> is less than
     * the bytes remaining to be read from the stream, the array should
     * be filled. A subsequent call reads the next increment, etc.
     * 
     * <P>If the bytes remaining in the stream is less than the length of
     * array <code>value</code>, the bytes should be read into the array.
     * The return value of the total number of bytes read will be less than
     * the length of the array, indicating that there are no more bytes left
     * to be read from the stream. The next read of the stream returns -1.
     * 
     * <p> If <code>length</code> is negative, or
     * <code>length</code> is greater than the length of the array
     * <code>value</code>, then an <code>IndexOutOfBoundsException</code> is
     * thrown. No bytes will be read from the stream for this exception case.
     * 
     * @param value the buffer into which the data is read.
     * @param length the number of bytes to read. Must be less than or equal to value.length.
     * 
     * @return the total number of bytes read into the buffer, or -1 if
     *         there is no more data because the end of the stream has been reached.
     * 
     * @exception MessageNotReadableException if message in write-only mode.
     * @exception JMSException if JMS fails to read message due to
     *                some internal JMS error
     */
    @Override
    public int readBytes(byte[] value, int length) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "readBytes", new Object[] { value, length });

        // Check that we are in read mode
        checkBodyReadable("readBytes");
        if (requiresInit)
            lazyInitForReading();

        // Check that there's enough room in the byte array supplied by the application for the number of
        // bytes requested
        if (value.length < length || length < 0)
            throw new IndexOutOfBoundsException();

        // Attempt to read into the application's byte array. If there are insufficient bytes in the message readStream
        // then this method returns the number actually read. If we've reached the end of data, it returns -1
        int result = readStream.read(value, 0, length);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "readBytes", result);
        return result;
    }

    /**
     * Read a Unicode character value from the stream message.
     * 
     * @return the next two bytes from the stream message as a Unicode
     *         character.
     * 
     * @exception MessageNotReadableException if message in write-only mode.
     * @exception MessageEOFException if end of message stream
     * @exception JMSException if JMS fails to read message due to
     *                some internal JMS error.
     */
    @Override
    public char readChar() throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "readChar");

        // Check that we are in read mode
        checkBodyReadable("readChar");
        if (requiresInit)
            lazyInitForReading();

        // Mark the current position, so we can return to it if there's an error
        // (if our caller hasn't set a mark already)
        if (markInUse == false)
            readStream.mark(2);

        // Read the character value from the input stream
        int byte1 = readStream.read(); // read the first byte
        int byte2 = readStream.read(); // read the next
        if (byte2 < 0) { // this is how read() signals EOF
            readStream.reset(); // return to the marked position
            throw (JMSException) JmsErrorUtils.newThrowable(
                                                            MessageEOFException.class,
                                                            "END_BYTESMESSAGE_CWSIA0183",
                                                            null,
                                                            tc);
        }

        char result;
        // Byte swap the character if required
        if (integerEncoding == ApiJmsConstants.ENC_INTEGER_REVERSED) {
            result = (char) ((byte2 << 8) + byte1);
        }
        else {
            result = (char) ((byte1 << 8) + byte2);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "readChar", result);
        return result;
    }

    /**
     * Read a <code>double</code> from the stream message.
     * 
     * @return the next eight bytes from the stream message, interpreted as
     *         a <code>double</code>.
     * 
     * @exception MessageNotReadableException if message in write-only mode.
     * @exception MessageEOFException if end of message stream
     * @exception JMSException if JMS fails to read message due to
     *                some internal JMS error.
     */
    @Override
    public double readDouble() throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "readDouble");

        int savedEncoding = integerEncoding; // save this as we might corrupt it

        // don't need to call checkBodyReadable because it will be done in readLong()

        double result;
        try {
            switch (floatEncoding) {
                case ApiJmsConstants.ENC_FLOAT_IEEE_NORMAL:
                    integerEncoding = ApiJmsConstants.ENC_INTEGER_NORMAL;
                    result = Double.longBitsToDouble(readLong());
                    break;
                case ApiJmsConstants.ENC_FLOAT_IEEE_REVERSED:
                    integerEncoding = ApiJmsConstants.ENC_INTEGER_REVERSED;
                    result = Double.longBitsToDouble(readLong());
                    break;
                case ApiJmsConstants.ENC_FLOAT_S390:
                    integerEncoding = ApiJmsConstants.ENC_INTEGER_NORMAL;
                    result = JMS390FloatSupport.longS390BitsToDouble(readLong());
                    break;
                default:
                    throw (JMSException) JmsErrorUtils.newThrowable(
                                                                    JMSException.class,
                                                                    "BAD_ENCODING_CWSIA0181",
                                                                    new Object[] { Integer.toHexString(floatEncoding) },
                                                                    tc);
            }
        }
        // An error in the S390 floating point code may throw an IOException...
        catch (IOException ex) {
            // No FFDC code needed

            // d222942 review. Hopefully a rare and unusual case, default message ok.
            // d238447 review. Generate an FFDC for this case.
            throw (JMSException) JmsErrorUtils.newThrowable(
                                                            JMSException.class,
                                                            "EXCEPTION_RECEIVED_CWSIA0190",
                                                            new Object[] { ex, "JmsBytesMessageImpl.readDouble" },
                                                            ex,
                                                            "JmsBytesMessageImpl.readDouble#1",
                                                            this,
                                                            tc
                            );
        }
        // Need to reinstate integerEncoding, in case it was affected
        finally {
            integerEncoding = savedEncoding;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "readDouble", result);
        return result;
    }

    /**
     * Read a <code>float</code> from the stream message.
     * 
     * @return the next four bytes from the stream message, interpreted as
     *         a <code>float</code>.
     * 
     * @exception MessageNotReadableException if message in write-only mode.
     * @exception MessageEOFException if end of message stream
     * @exception JMSException if JMS fails to read message due to
     *                some internal JMS error.
     */
    @Override
    public float readFloat() throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "readFloat");

        // don't need to call checkBodyReadable because it will be done in readInt()
        int savedEncoding = integerEncoding; // save this as we might corrupt it

        float result;
        try {
            switch (floatEncoding) {
                case ApiJmsConstants.ENC_FLOAT_IEEE_NORMAL:
                    integerEncoding = ApiJmsConstants.ENC_INTEGER_NORMAL;
                    result = Float.intBitsToFloat(readInt());
                    break;
                case ApiJmsConstants.ENC_FLOAT_IEEE_REVERSED:
                    integerEncoding = ApiJmsConstants.ENC_INTEGER_REVERSED;
                    result = Float.intBitsToFloat(readInt());
                    break;
                case ApiJmsConstants.ENC_FLOAT_S390:
                    integerEncoding = ApiJmsConstants.ENC_INTEGER_NORMAL;
                    result = JMS390FloatSupport.intS390BitsToFloat(readInt());
                    break;
                default:
                    throw (JMSException) JmsErrorUtils.newThrowable(
                                                                    JMSException.class,
                                                                    "BAD_ENCODING_CWSIA0181",
                                                                    new Object[] { Integer.toHexString(floatEncoding) },
                                                                    tc);
            }
        }
        // An error in the S390 floating point code may throw an IOException...
        catch (IOException ex) {
            // No FFDC code needed
            // d222942 review. Hopefully a rare and unusual case, default message ok.
            // d238447 review. Generate FFDC for this case.
            throw (JMSException) JmsErrorUtils.newThrowable(
                                                            JMSException.class,
                                                            "EXCEPTION_RECEIVED_CWSIA0190",
                                                            new Object[] { ex, "JmsBytesMessageImpl.readFloat" },
                                                            ex,
                                                            "JmsBytesMessageImpl.readFloat#1",
                                                            this,
                                                            tc
                            );
        }
        // Need to reinstate integerEncoding, in case it was affected
        finally {
            integerEncoding = savedEncoding;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "readFloat", result);
        return result;
    }

    /**
     * Read a signed 32-bit integer from the stream message.
     * 
     * @return the next four bytes from the stream message, interpreted as
     *         an <code>int</code>.
     * 
     * @exception MessageNotReadableException if message in write-only mode.
     * @exception MessageEOFException if end of message stream
     * @exception JMSException if JMS fails to read message due to
     *                some internal JMS error.
     */
    @Override
    public int readInt() throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "readInt");

        // Check that we are in read mode
        checkBodyReadable("readInt");
        if (requiresInit)
            lazyInitForReading();

        // Mark the current position, so we can return to it if there's an error
        // but avoid re-marking if we're called from readLong
        if (markInUse == false)
            readStream.mark(4);

        // Read the integer value from the input stream
        int byte1 = readStream.read(); // read the first byte
        int byte2 = readStream.read(); // read the next
        int byte3 = readStream.read(); // read the next
        int byte4 = readStream.read(); // read the next

        if (byte4 < 0) { // this is how read() signals EOF
            readStream.reset(); // return to the marked position
            throw (JMSException) JmsErrorUtils.newThrowable(
                                                            MessageEOFException.class,
                                                            "END_BYTESMESSAGE_CWSIA0183",
                                                            null,
                                                            tc);
        }

        // Byte swap the integer if required
        int result;
        if (integerEncoding == ApiJmsConstants.ENC_INTEGER_REVERSED) {
            result = (byte4 << 24) + (byte3 << 16) + (byte2 << 8) + byte1;
        }
        else {
            result = (byte1 << 24) + (byte2 << 16) + (byte3 << 8) + byte4;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "readInt", result);
        return result;
    }

    /**
     * Read a signed 64-bit integer from the stream message.
     * 
     * @return the next eight bytes from the stream message, interpreted as
     *         a <code>long</code>.
     * 
     * @exception MessageNotReadableException if message in write-only mode.
     * @exception MessageEOFException if end of message stream
     * @exception JMSException if JMS fails to read message due to
     *                some internal JMS error.
     */
    @Override
    public long readLong() throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "readLong");
        long result;

        // Check that we are in read mode
        checkBodyReadable("readLong");
        if (requiresInit)
            lazyInitForReading();

        try {
            // Mark the current position, so we can return to it if there's an error
            readStream.mark(8); // the argument appears to be ignored
            markInUse = true; // stop readInt from remarking

            // We handle the long as if it were two consecutive ints.
            long int1 = readInt() & 0xFFFFFFFFL;
            long int2 = readInt() & 0xFFFFFFFFL;

            // Byte swap the long if required
            if (integerEncoding == ApiJmsConstants.ENC_INTEGER_REVERSED) {
                result = (int2 << 32) + int1;
            }
            else {
                result = (int1 << 32) + int2;
            }
        } finally {
            markInUse = false; // release the mark
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "readLong", result);
        return result;
    }

    /**
     * Read a signed 16-bit number from the stream message.
     * 
     * @return the next two bytes from the stream message, interpreted as a
     *         signed 16-bit number.
     * 
     * @exception MessageNotReadableException if message in write-only mode.
     * @exception MessageEOFException if end of message stream
     * @exception JMSException if JMS fails to read message due to
     *                some internal JMS error.
     */
    @Override
    public short readShort() throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "readShort");

        // Check that we are in read mode
        checkBodyReadable("readShort");
        if (requiresInit)
            lazyInitForReading();

        // Mark the current position, so we can return to it if there's an error
        // if our caller hasn't set a mark
        if (markInUse == false)
            readStream.mark(2);

        // Read the short value from the input stream
        int byte1 = readStream.read(); // read the first byte
        int byte2 = readStream.read(); // read the next 	
        if (byte2 < 0) {
            readStream.reset(); // return to the marked position
            throw (JMSException) JmsErrorUtils.newThrowable(
                                                            MessageEOFException.class,
                                                            "END_BYTESMESSAGE_CWSIA0183",
                                                            null,
                                                            tc);
        }

        // Byte swap the short if required
        short result;
        if (integerEncoding == ApiJmsConstants.ENC_INTEGER_REVERSED) {
            result = (short) ((byte2 << 8) + byte1);
        }
        else {
            result = (short) ((byte1 << 8) + byte2);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "readShort", result);
        return result;
    }

    /**
     * Read an unsigned 8-bit number from the stream message.
     * 
     * @return the next byte from the stream message, interpreted as an
     *         unsigned 8-bit number.
     * 
     * @exception MessageNotReadableException if message in write-only mode.
     * @exception MessageEOFException if end of message stream
     * @exception JMSException if JMS fails to read message due to
     *                some internal JMS error.
     */
    @Override
    public int readUnsignedByte() throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "readUnsignedByte");

        // Check that we are in read mode
        checkBodyReadable("readUnsignedByte");
        if (requiresInit)
            lazyInitForReading();

        // Read the byte from the input stream
        int byteRead = readStream.read(); // read the byte
        if (byteRead < 0) {
            throw (JMSException) JmsErrorUtils.newThrowable(
                                                            MessageEOFException.class,
                                                            "END_BYTESMESSAGE_CWSIA0183",
                                                            null,
                                                            tc);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "readUnsignedByte", byteRead);
        return byteRead;
    }

    /**
     * Read an unsigned 16-bit number from the stream message.
     * 
     * @return the next two bytes from the stream message, interpreted as an
     *         unsigned 16-bit integer.
     * 
     * @exception MessageNotReadableException if message in write-only mode.
     * @exception MessageEOFException if end of message stream
     * @exception JMSException if JMS fails to read message due to
     *                some internal JMS error.
     */
    @Override
    public int readUnsignedShort() throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "readUnsignedShort");

        // Check that we are in read mode
        checkBodyReadable("readUnsignedShort");
        if (requiresInit)
            lazyInitForReading();

        // Mark the current position, so we can return to it if there's an error
        // if our caller hasn't set a mark already
        if (markInUse == false)
            readStream.mark(2);

        // Read the short value from the input stream
        int byte1 = readStream.read(); // read the first byte
        int byte2 = readStream.read(); // read the next
        if (byte2 < 0) {
            readStream.reset(); // return to the marked position
            throw (JMSException) JmsErrorUtils.newThrowable(
                                                            MessageEOFException.class,
                                                            "END_BYTESMESSAGE_CWSIA0183",
                                                            null,
                                                            tc);
        }

        // Byte swap the short if required
        int result;
        if (integerEncoding == ApiJmsConstants.ENC_INTEGER_REVERSED) {
            result = (byte2 << 8) + byte1;
        }
        else {
            result = (byte1 << 8) + byte2;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "readUnsignedShort", result);
        return result;
    }

    /**
     * Read in a string that has been encoded using a modified UTF-8
     * format from the stream message.
     * 
     * <P>For more information on the UTF-8 format, see "File System Safe
     * UCS Transformation Format (FSS_UFT)", X/Open Preliminary Specification,
     * X/Open Company Ltd., Document Number: P316. This information also
     * appears in ISO/IEC 10646, Annex P.
     * 
     * @return a Unicode string from the stream message.
     * 
     * @exception MessageNotReadableException if message in write-only mode.
     * @exception MessageEOFException if end of message stream
     * @exception JMSException if JMS fails to read message due to
     *                some internal JMS error.
     */
    @Override
    public String readUTF() throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "readUTF");
        String result;

        int savedEncoding = integerEncoding; // save this as we might corrupt it

        try {
            // Check that we are in read mode
            checkBodyReadable("readUTF");
            if (requiresInit)
                lazyInitForReading();

            // Mark the current position, so we can return to it if there's an error
            readStream.mark(8); // the argument appears to be ignored
            markInUse = true; // stop readUnsignedShort from remarking

            // Read the 2-byte length prefix
            integerEncoding = ApiJmsConstants.ENC_INTEGER_NORMAL; // it's always hi-byte, lo-byte
            int length = readUnsignedShort();

            // Read in the bytes that make up the body of the UTF8 string
            byte[] utfBytes = new byte[length];

            if (readBytes(utfBytes, length) != length) {
                // If we didn't read the expected number of bytes for some reason
                readStream.reset(); // return to the marked position
                throw (JMSException) JmsErrorUtils.newThrowable(
                                                                MessageEOFException.class,
                                                                "END_BYTESMESSAGE_CWSIA0183",
                                                                null,
                                                                tc);
            }

            // Return the bytes as a Java String
            result = new String(utfBytes, 0, length, "UTF8");
        }

        // We don't expect any exceptions from the String constructor, but we must catch them anyway
        catch (UnsupportedEncodingException ex) {
            // No FFDC code needed
            // d238447 review. Generate FFDC for this case.
            throw (JMSException) JmsErrorUtils.newThrowable(
                                                            MessageFormatException.class,
                                                            "UTF8_CONV_CWSIA0184",
                                                            null,
                                                            ex,
                                                            "JmsBytesMessage.readUTF#1",
                                                            this,
                                                            tc);

        }

        finally {
            integerEncoding = savedEncoding; // make sure it's put back correctly
            markInUse = false; // release the mark
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "readUTF", result);
        return result;
    }

    /**
     * Put the message in read-only mode, and reposition the stream of
     * bytes to the beginning.
     * 
     * @exception JMSException if JMS fails to reset the message due to
     *                some internal JMS error.
     * @exception MessageFormatException if message has an invalid
     *                format.
     */
    @Override
    public void reset() throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "reset");

        // After reset has been called, the message body is immutable (the only way of changing it is to
        // call clearBody and start again). If we're in Write Mode, we need to convert the writeStream
        // to a byte array and switch into read mode. If we're in Read Mode, we must have already done this,
        // so we don't want to do it again
        if (!isBodyReadOnly()) {

            // If the producer won't modify the payload after set,skip the
            // encoding of the write stream because encoding is not needed
            if (!producerWontModifyPayloadAfterSet) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(this, tc, "Body is writeable & producer might modify the payload, so encode the write stream");

                // Convert the stream in which the message was being assembled to a byte array
                dataBuffer = _writeBytes.toByteArray();

                // store the content in the JS msg
                lastEncoding = integerEncoding | floatEncoding;
                jsBytesMsg.setBytes(_exportBody(lastEncoding));
                bodySetInJsMsg = true;

                // Throw away the write stream
                writeStream = null;
                _writeBytes = null;
            }

            dataStart = 0; // start from the first byte in the buffer

            // The message is now read only
            setBodyReadOnly();
        }

        // For some odd reason, after receiving an 'empty' message the dataBuffer
        // is null rather than pointing to an empty array. This will compensate
        // hopefully without any side effects - SXA #58832
        if (dataBuffer == null)
            dataBuffer = new byte[0];

        // Create a Byte array input stream for the readxxx methods to operate on. We have to create a new
        // stream each time (rather than call the stream's read method), as the stream may have a position
        // marked in it that isn't at the start.
        readStream = new ByteArrayInputStream(dataBuffer);

        // Jump over any header that might be present in the dataBuffer
        // JBK - I don't think we will ever have dataStart!=0 in JS, but I'm
        // leaving the capability here until I understand better.
        readStream.skip(dataStart);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "reset");
    }

    /**
     * Write a <code>boolean</code> to the stream message as a 1-byte value.
     * The value <code>true</code> is written out as the value
     * <code>(byte)1</code>; the value <code>false</code> is written out as
     * the value <code>(byte)0</code>.
     * 
     * @param value the <code>boolean</code> value to be written.
     * 
     * @exception MessageNotWriteableException if message in read-only mode.
     * @exception JMSException if JMS fails to write message due to
     *                some internal JMS error.
     */
    @Override
    public void writeBoolean(boolean value) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "writeBoolean", value);

        // Check if the producer has promised not to modify the payload after it's been set
        checkProducerPromise("writeBoolean(boolean)", "JmsBytesMessageImpl.writeBoolean#1");

        // Check that we are in write mode. Need to do this here (as well as in
        // writeByte) otherwise we get a misleading error message.
        checkBodyWriteable("writeBoolean");

        // Convert the boolean to a 1-byte value as described above.
        writeByte((byte) (value ? 1 : 0));

        // Invalidate the cached toString object.
        cachedBytesToString = null;

        // Ensure that the new data gets exported when the time comes.
        bodySetInJsMsg = false;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "writeBoolean");
    }

    /**
     * Write out a <code>byte</code> to the stream message as a 1-byte value.
     * 
     * @param value the <code>byte</code> value to be written.
     * 
     * @exception MessageNotWriteableException if message in read-only mode.
     * @exception JMSException if JMS fails to write message due to
     *                some internal JMS error.
     */
    @Override
    public void writeByte(byte value) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "writeByte", value);

        try {
            // Check if the producer has promised not to modify the payload after it's been set
            checkProducerPromise("writeByte(byte)", "JmsBytesMessageImpl.writeByte#2");

            // Check that we are in write mode
            checkBodyWriteable("writeByte");

            // Write the byte to the output stream
            writeStream.writeByte(value);

            // Invalidate the cached toString object.
            cachedBytesToString = null;

            // Ensure that the new data gets exported when the time comes.
            bodySetInJsMsg = false;
        }

        // We don't expect the writeByte to fail, but we need to catch the exception anyway
        catch (IOException ex) {
            // No FFDC code needed
            // d238447 review. Generate FFDC for this case.
            throw (JMSException) JmsErrorUtils.newThrowable(
                                                            ResourceAllocationException.class,
                                                            "WRITE_PROBLEM_CWSIA0186",
                                                            null,
                                                            ex,
                                                            "JmsBytesMessageImpl.writeByte#1",
                                                            this,
                                                            tc);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "writeByte");
    }

    /**
     * Write a byte array to the stream message.
     * 
     * @param value of the byte array to be written.
     * 
     * @exception MessageNotWriteableException if message in read-only mode.
     * @exception JMSException if JMS fails to write message due to
     *                some internal JMS error.
     */
    @Override
    public void writeBytes(byte[] value) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "writeBytes", value);

        try {
            // Check that we are in write mode
            checkBodyWriteable("writeBytes");

            // This method has different behaviours for storing the byte array, based on the whether the producer
            // has promised not to mess with data after it's been written...
            if (producerWontModifyPayloadAfterSet) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(this, tc, "Producer has promised not to modify the payload after setting it in the message - check if they've violated that promise");

                // The producer has promised not to modify the payload after it's been set, so check
                // the flag to see whether this is the first, or a subsequent call to writeBytes.
                if (!writeByteArrayCalled) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        SibTr.debug(this, tc, "This is the first call to writeBytes(byte[] value) - storing the byte array reference directly in the underlying MFP object");

                    writeByteArrayCalled = true;

                    // Set the data buffer & MFP data references to the value param & reset
                    // the dataStart attribute
                    jsBytesMsg.setBytes(value);
                    dataBuffer = value;
                    dataStart = 0;
                }

                else {
                    // The producer has promised not to modify the payload after it's been set, but the producer has
                    // been naughty by calling this method more than once! Throw exception to admonish the producer.
                    throw (JMSException) JmsErrorUtils.newThrowable(
                                                                    IllegalStateException.class, // JMS illegal state exception
                                                                    "PROMISE_BROKEN_EXCEPTION_CWSIA0511", // promise broken
                                                                    null, // No inserts
                                                                    null, // no cause - original exception
                                                                    "JmsBytesMessageImpl.writeBytes#3", // Probe ID
                                                                    this, // Caller (?)
                                                                    tc); // Trace component
                }
            }

            else {
                // Producer makes no promises relating to the accessing the message payload, so
                // make a copy of the byte array at this point to ensure the message is transmitted safely.
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(this, tc, "Producer 'payload modification' promise is not in place - make a copy of the byte array");

                // Write the byte array to the output stream
                // We don't check for value==null as JDK1.1.6 DataOutputStream doesn't
                writeStream.write(value, 0, value.length);

                // Ensure that the new data gets exported when the time comes.
                bodySetInJsMsg = false;
            }

            // Invalidate the cached toString object, because the message payload has changed.
            cachedBytesToString = null;
        }

        catch (IOException ex) {
            // No FFDC code needed
            //(exception repro'ed from 3-param writeBytes method)
            throw (JMSException) JmsErrorUtils.newThrowable(
                                                            ResourceAllocationException.class,
                                                            "WRITE_PROBLEM_CWSIA0186",
                                                            null,
                                                            ex,
                                                            "JmsBytesMessageImpl.writeBytes#4",
                                                            this,
                                                            tc);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "writeBytes");
    }

    /**
     * Write a portion of a byte array to the stream message.
     * 
     * @param value the byte array value to be written.
     * @param offset the initial offset within the byte array.
     * @param length the number of bytes to use.
     * 
     * @exception MessageNotWriteableException if message in read-only mode.
     * @exception JMSException if JMS fails to write message due to
     *                some internal JMS error.
     */
    @Override
    public void writeBytes(byte[] value, int offset, int length) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "writeBytes", new Object[] { value, offset, length });

        try {
            // Check if the producer has promised not to modify the payload after it's been set
            checkProducerPromise("writeBytes(byte[], int, int)", "JmsBytesMessageImpl.writeBytes#2");

            // Check that we are in write mode
            checkBodyWriteable("writeBytes");

            // Write the byte array to the output stream
            // We don't check for value==null as JDK1.1.6 DataOutputStream doesn't
            writeStream.write(value, offset, length);

            // Invalidate the cached toString object.
            cachedBytesToString = null;

            // Ensure that the new data gets exported when the time comes.
            bodySetInJsMsg = false;
        }

        // We don't expect the writeBytes to fail, but we need to catch the exception anyway
        catch (IOException ex) {
            // No FFDC code needed
            // d238447 review. Generate FFDC for this case.
            throw (JMSException) JmsErrorUtils.newThrowable(
                                                            ResourceAllocationException.class,
                                                            "WRITE_PROBLEM_CWSIA0186",
                                                            null,
                                                            ex,
                                                            "JmsBytesMessageImpl.writeBytes#1",
                                                            this,
                                                            tc);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "writeBytes");
    }

    /**
     * Write a <code>char</code> to the stream message as a 2-byte value,
     * high byte first.
     * 
     * @param value the <code>char</code> value to be written.
     * 
     * @exception MessageNotWriteableException if message in read-only mode.
     * @exception JMSException if JMS fails to write message due to
     *                some internal JMS error.
     */
    @Override
    public void writeChar(char value) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "writeChar", value);

        try {
            // Check if the producer has promised not to modify the payload after it's been set
            checkProducerPromise("writeChar(char)", "JmsBytesMessageImpl.writeChar#2");

            // Check that we are in write mode
            checkBodyWriteable("writeChar");

            // Write the character to the output stream using the JDK
            writeStream.writeChar(value);

            // Record the position of the character so that it can be byte swapped later if required
            recordInteger(writeStream.size() - 2, 2);
            // length of character is 2 bytes

            // Invalidate the cached toString object.
            cachedBytesToString = null;

            // Ensure that the new data gets exported when the time comes.
            bodySetInJsMsg = false;
        }

        // We don't expect the writeChar to fail, but we need to catch the exception anyway
        catch (IOException ex) {
            // No FFDC code needed
            // d238447 review. Generate FFDC for this case.
            throw (JMSException) JmsErrorUtils.newThrowable(
                                                            ResourceAllocationException.class,
                                                            "WRITE_PROBLEM_CWSIA0186",
                                                            null,
                                                            ex,
                                                            "JmsBytesMessageImpl.writeChar#1",
                                                            this,
                                                            tc);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "writeChar");
    }

    /**
     * Convert the double argument to a <code>long</code> using the
     * <code>doubleToLongBits</code> method in class <code>Double</code>,
     * and then writes that <code>long</code> value to the stream
     * message as an 8-byte quantity, high byte first.
     * 
     * @param value the <code>double</code> value to be written.
     * 
     * @exception MessageNotWriteableException if message in read-only mode.
     * @exception JMSException if JMS fails to write message due to
     *                some internal JMS error.
     */
    @Override
    public void writeDouble(double value) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "writeDouble", value);

        try {
            // Check if the producer has promised not to modify the payload after it's been set
            checkProducerPromise("writeDouble(double)", "JmsBytesMessageImpl.writeDouble#2");

            // Check that we are in write mode
            checkBodyWriteable("writeDouble");

            // Write the long to the output stream as described above
            writeStream.writeLong(Double.doubleToLongBits(value));

            // Record the position and value of the double in the float vectors, so that it can be
            // encoded in a different
            // representation if desired
            if (float_values == null)
                float_values = new Vector();
            float_values.addElement(new Double(value)); // value
            if (float_offsets == null)
                float_offsets = new Vector();
            float_offsets.addElement(Integer.valueOf(writeStream.size() - 8));
            // offset

            // Invalidate the cached toString object.
            cachedBytesToString = null;

            // Ensure that the new data gets exported when the time comes.
            bodySetInJsMsg = false;
        }

        // We don't expect the writeLong to fail, but we need to catch the exception anyway
        catch (IOException ex) {
            // No FFDC code needed
            // d238447 review. Generate FFDC for this case.
            throw (JMSException) JmsErrorUtils.newThrowable(
                                                            ResourceAllocationException.class,
                                                            "WRITE_PROBLEM_CWSIA0186",
                                                            null,
                                                            ex,
                                                            "JmsBytesMessageImpl.writeDouble#1",
                                                            this,
                                                            tc);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "writeDouble");
    }

    /**
     * Convert the float argument to an <code>int</code> using the
     * <code>floatToIntBits</code> method in class <code>Float</code>,
     * and then writes that <code>int</code> value to the stream
     * message as a 4-byte quantity, high byte first.
     * 
     * @param value the <code>float</code> value to be written.
     * 
     * @exception MessageNotWriteableException if message in read-only mode.
     * @exception JMSException if JMS fails to write message due to
     *                some internal JMS error.
     */
    @Override
    public void writeFloat(float value) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "writeFloat", value);

        try {
            // Check if the producer has promised not to modify the payload after it's been set
            checkProducerPromise("writeFloat(float)", "JmsBytesMessageImpl.writeFloat#2");

            // Check that we are in write mode
            checkBodyWriteable("writeFloat");

            // Convert the float to an int as described above
            writeStream.writeInt(Float.floatToIntBits(value));

            // Record the position and value of the float in the float vectors, so that it can be encoded in a different
            // representation if desired
            if (float_values == null)
                float_values = new Vector();
            float_values.addElement(new Float(value)); // value
            if (float_offsets == null)
                float_offsets = new Vector();
            float_offsets.addElement(Integer.valueOf(writeStream.size() - 4)); // offset

            // Invalidate the cached toString object.
            cachedBytesToString = null;

            // Ensure that the new data gets exported when the time comes.
            bodySetInJsMsg = false;
        }

        // We don't expect the writeInt to fail, but we need to catch the exception anyway
        catch (IOException ex) {
            // No FFDC code needed
            // d238447 review. Generate FFDC for this case.
            throw (JMSException) JmsErrorUtils.newThrowable(
                                                            ResourceAllocationException.class,
                                                            "WRITE_PROBLEM_CWSIA0186",
                                                            null,
                                                            ex,
                                                            "JmsBytesMessageImpl.writeFloat#1",
                                                            this,
                                                            tc);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "writeFloat");
    }

    /**
     * Write an <code>int</code> to the stream message as four bytes,
     * high byte first.
     * 
     * @param value the <code>int</code> to be written.
     * 
     * @exception MessageNotWriteableException if message in read-only mode.
     * @exception JMSException if JMS fails to write message due to
     *                some internal JMS error.
     */
    @Override
    public void writeInt(int value) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "writeInt", value);

        try {
            // Check if the producer has promised not to modify the payload after it's been set
            checkProducerPromise("writeInt(int)", "JmsBytesMessageImpl.writeInt#2");

            // Check that we are in write mode
            checkBodyWriteable("writeInt");

            // Write the int to the output stream
            writeStream.writeInt(value);

            // Record the position of the integer so that it can be byte swapped later if required
            recordInteger(writeStream.size() - 4, 4);
            // length of integer is 4 bytes

            // Invalidate the cached toString object.
            cachedBytesToString = null;

            // Ensure that the new data gets exported when the time comes.
            bodySetInJsMsg = false;
        }

        // We don't expect the writeInt to fail, but we need to catch the exception anyway
        catch (IOException ex) {
            // No FFDC code needed
            // d238447 review. Generate FFDC for this case.
            throw (JMSException) JmsErrorUtils.newThrowable(
                                                            ResourceAllocationException.class,
                                                            "WRITE_PROBLEM_CWSIA0186",
                                                            null,
                                                            ex,
                                                            "JmsBytesMessageImpl.writeInt#1",
                                                            this,
                                                            tc);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "writeInt");
    }

    /**
     * Write a <code>long</code> to the stream message as eight bytes,
     * high byte first.
     * 
     * @param value the <code>long</code> to be written.
     * 
     * @exception MessageNotWriteableException if message in read-only mode.
     * @exception JMSException if JMS fails to write message due to
     *                some internal JMS error.
     */
    @Override
    public void writeLong(long value) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "writeLong", value);

        try {
            // Check if the producer has promised not to modify the payload after it's been set
            checkProducerPromise("writeLong(long)", "JmsBytesMessageImpl.writeLong#2");

            // Check that we are in write mode
            checkBodyWriteable("writeLong");

            // Write the long to the output stream
            writeStream.writeLong(value);

            // Record the position of the long so that it can be byte swapped later if required
            recordInteger(writeStream.size() - 8, 8);
            // length of long is 8 bytes

            // Invalidate the cached toString object.
            cachedBytesToString = null;

            // Ensure that the new data gets exported when the time comes.
            bodySetInJsMsg = false;
        }

        // We don't expect the writeLong to fail, but we need to catch the exception anyway
        catch (IOException ex) {
            // No FFDC code needed
            // d238447 review. Generate FFDC for this case.
            throw (JMSException) JmsErrorUtils.newThrowable(
                                                            ResourceAllocationException.class,
                                                            "WRITE_PROBLEM_CWSIA0186",
                                                            null,
                                                            ex,
                                                            "JmsBytesMessageImpl.writeLong#1",
                                                            this,
                                                            tc);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "writeLong");
    }

    /**
     * Write a Java object to the stream message.
     * 
     * <P>Note that this method only works for the objectified primitive
     * object types (Integer, Double, Long ...), String's and byte arrays.
     * 
     * @param value the Java object to be written.
     * 
     * @exception MessageNotWriteableException if message in read-only mode.
     * @exception MessageFormatException if object is invalid type.
     * @exception JMSException if JMS fails to write message due to
     *                some internal JMS error.
     */
    @Override
    public void writeObject(Object value) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "writeObject");

        // Check if the producer has promised not to modify the payload after it's been set
        checkProducerPromise("writeObject(Object)", "JmsBytesMessageImpl.writeObject#1");

        // Check that we are in write mode (doing this test here ensures
        // any error message refers to the correct method)
        checkBodyWriteable("writeObject");

        if (value instanceof byte[])
            writeBytes((byte[]) value);
        else if (value instanceof String)
            writeUTF((String) value);
        else if (value instanceof Integer)
            writeInt(((Integer) value).intValue());
        else if (value instanceof Byte)
            writeByte(((Byte) value).byteValue());
        else if (value instanceof Short)
            writeShort(((Short) value).shortValue());
        else if (value instanceof Long)
            writeLong(((Long) value).longValue());
        else if (value instanceof Float)
            writeFloat(((Float) value).floatValue());
        else if (value instanceof Double)
            writeDouble(((Double) value).doubleValue());
        else if (value instanceof Character)
            writeChar(((Character) value).charValue());
        else if (value instanceof Boolean)
            writeBoolean(((Boolean) value).booleanValue());
        else if (value == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "given null, throwing NPE");
            throw new NullPointerException();
        }
        else {
            // d238447 FFDC review. Passing in an object of the wrong type is an app error,
            // so no FFDC needed here.
            throw (JMSException) JmsErrorUtils.newThrowable(
                                                            MessageFormatException.class,
                                                            "BAD_OBJECT_CWSIA0185",
                                                            new Object[] { value.getClass().getName() },
                                                            tc);
        }

        // Invalidate the cached toString object.
        cachedBytesToString = null;

        // Ensure that the new data gets exported when the time comes.
        bodySetInJsMsg = false;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "writeObject");
    }

    /**
     * Write a <code>short</code> to the stream message as two bytes, high
     * byte first.
     * 
     * @param value the <code>short</code> to be written.
     * 
     * @exception MessageNotWriteableException if message in read-only mode.
     * @exception JMSException if JMS fails to write message due to
     *                some internal JMS error.
     */
    @Override
    public void writeShort(short value) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "writeShort", value);

        try {
            // Check if the producer has promised not to modify the payload after it's been set
            checkProducerPromise("writeShort(short)", "JmsBytesMessageImpl.writeShort#2");

            // Check that we are in write mode
            checkBodyWriteable("writeShort");

            // Write the short to the output stream
            writeStream.writeShort(value);

            // Record the position of the short so that it can be byte swapped later if required
            recordInteger(writeStream.size() - 2, 2);
            // length of short is 2 bytes

            // Invalidate the cached toString object.
            cachedBytesToString = null;

            // Ensure that the new data gets exported when the time comes.
            bodySetInJsMsg = false;
        }

        // We don't expect the writeShort to fail, but we need to catch the exception anyway
        catch (IOException ex) {
            // No FFDC code needed
            // d238447 review. Generate FFDC for this case.
            throw (JMSException) JmsErrorUtils.newThrowable(
                                                            ResourceAllocationException.class,
                                                            "WRITE_PROBLEM_CWSIA0186",
                                                            null,
                                                            ex,
                                                            "JmsBytesMessageImpl.writeShort#1",
                                                            this,
                                                            tc);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "writeShort");
    }

    /**
     * Write a string to the stream message using UTF-8 encoding in a
     * machine-independent manner.
     * 
     * <P>For more information on the UTF-8 format, see "File System Safe
     * UCS Transformation Format (FSS_UFT)", X/Open Preliminary Specification,
     * X/Open Company Ltd., Document Number: P316. This information also
     * appears in ISO/IEC 10646, Annex P.
     * 
     * @param value the <code>String</code> value to be written.
     * 
     * @exception MessageNotWriteableException if message in read-only mode.
     * @exception JMSException if JMS fails to write message due to
     *                some internal JMS error.
     */
    @Override
    public void writeUTF(String value) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "writeUTF", value);

        try {
            // Check if the producer has promised not to modify the payload after it's been set
            checkProducerPromise("writeUTF(String)", "JmsBytesMessageImpl.writeUTF#1");

            // Check that we are in write mode
            checkBodyWriteable("writeUTF");

            // Write the string to the output stream
            writeStream.writeUTF(value);

            // Invalidate the cached toString object.
            cachedBytesToString = null;

            // Ensure that the new data gets exported when the time comes.
            bodySetInJsMsg = false;
        }

        catch (UTFDataFormatException ex) {
            // No FFDC code needed
            // d238447 FFDC review. This is thrown if the String is too long to use UTF8 (2 byte length counter)
            // This would be an application error, so no FFDC required.
            throw (JMSException) JmsErrorUtils.newThrowable(
                                                            MessageFormatException.class,
                                                            "UTF8_CONV_CWSIA0184",
                                                            null,
                                                            ex,
                                                            null, // null probeId = no FFDC
                                                            this,
                                                            tc);
        } catch (IOException ex) {
            // No FFDC code needed
            // We don't expect any other exceptions from writeUTF, but we must catch the exception anyway
            // d238447 review. Generate FFDC for this case.
            throw (JMSException) JmsErrorUtils.newThrowable(
                                                            ResourceAllocationException.class,
                                                            "WRITE_PROBLEM_CWSIA0186",
                                                            null,
                                                            ex,
                                                            "JmsBytesMessageImpl.writeUTF#2",
                                                            this,
                                                            tc);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "writeUTF");
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.jms.BytesMessage#getBodyLength()
     */
    @Override
    public long getBodyLength() throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getBodyLength");
        long bLen;

        // D175063 CTS requires a MessageNotReadableException. This is entirely
        // pointless, so I'll leave the rest of the code intact, and just add a
        // call to checkBodyReadable. Perhaps the restriction can be lifted in
        // future.
        // Check that we are in read mode
        checkBodyReadable("getBodyLength");
        if (requiresInit)
            lazyInitForReading();

        if (dataBuffer == null) {
            bLen = 0;
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "buffer null");
        }
        else {
            // the length is the dataBuffer length - dataStart.
            bLen = dataBuffer.length - dataStart;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getBodyLength", bLen);
        return bLen;
    }

    /**
     * This method returns a String containing a formatted version
     * of the Message.
     * 
     * @return java.lang.String
     */
    @Override
    public String toString() {

        if (cachedBytesToString == null) {

            // Constants to control formatting of the binary data
            int MAX_LINES = 10;
            int LINE_LENGTH = 40;

            // We don't trace anywhere in this method, as trace itself may well call toString()
            StringBuffer retval = new StringBuffer();

            // can't do this because the build complains about the translated text not having a prefix
            //    retval.append(nls.getFormattedMessage("ENCS_TOSTR_CWSIA0187",
            //                                          new Object[] {new Integer(integerEncoding),new Integer(floatEncoding)},
            //                                          null));
            //    retval.append("\n");

            // Convert the message contents to a byte array, if that hasn't already happened	

            if (!isBodyReadOnly()) {
                // Somewhat horrible, but getMsgReference will side-effect the dataBuffer and do
                // the correct conversions for encoding.
                try {
                    // SIB0121: if the producer has promised not to modify the payload after setting it,
                    // getMsgReference() will actually do nothing
                    getMsgReference();
                } catch (JMSException e) {
                    // No FFDC code needed
                    // Debug but otherwise ignore
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        SibTr.debug(this, tc, "getMsgReference failed with " + e);
                }
            }

            if (requiresInit)
                lazyInitForReading();

            if (dataBuffer != null) {
                // Write the message body out in hex. As the message could be very large, we limit the amount actually
                // output

                int curpos = dataStart; // @ P2C
                int lines_written = 0;

                // Limit the amount of lines output	
                while (lines_written < MAX_LINES) {
                    if (dataBuffer.length <= curpos + LINE_LENGTH) {
                        // if not enough for a complete line
                        HexString.binToHex(dataBuffer, curpos, dataBuffer.length - curpos, retval);
                        curpos = dataBuffer.length;
                        retval.append("\n");
                        break;
                    }
                    else {
                        // at least enough for one more line
                        HexString.binToHex(dataBuffer, curpos, LINE_LENGTH, retval);
                        retval.append("\n");
                        curpos += LINE_LENGTH;
                    }
                    lines_written++;
                }

                // Put out extra message, if the data was truncated
                if (curpos != dataBuffer.length)
                    retval.append("...\n");
            }

            // Store the output in the cache.
            cachedBytesToString = retval.toString();
        }

        return super.toString() + "\n" + cachedBytesToString;
    }

    //////////////////////////////////////////////////////////////////////////////
    // Support for object serialisation.
    //////////////////////////////////////////////////////////////////////////////

    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "writeObject", out);

        try {
            if (requiresInit)
                lazyInitForReading();

            // If message is in write mode, get data into byte array so that we
            // can restore the output stream during readObject.
            // If message is in read mode, store the offset so that we can skip
            // the input stream during readObject.
            if (isBodyReadOnly()) {
                // read mode
                streamOffset = dataBuffer.length - readStream.available();
            }
            else {
                // write mode
                if (_writeBytes != null) {
                    // SIB0121: If the producer has promised not to modify the payload after set,
                    // _writeBytes will always be null
                    dataBuffer = _writeBytes.toByteArray();
                }
            }
            // Use the default mechanism to write the fields.
            out.defaultWriteObject();
        } catch (IOException e) {
            // No FFDC code needed
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "Error during writeObject", e);
            // pass on
            throw e;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "writeObject");
    }

    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "readObject", in);

        try {
            // read the fields using the default mechanism.
            in.defaultReadObject();

            if (isBodyReadOnly()) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(this, tc, "Body is read only - recreating read stream from local reference to payload");
                // If message is in read mode, recreate input stream and skip to offset
                readStream = new ByteArrayInputStream(dataBuffer);
                readStream.skip(streamOffset);
            }
            else {
                if (!producerWontModifyPayloadAfterSet) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        SibTr.debug(this, tc,
                                    "Body is writeable & producer hasn't promised not to modify the payload after it's been set - recreating write streams from local reference to payload");
                    // If message is in write mode, & the producer hasn't promised not to modify the payload
                    // after it's been set (SIB0121), recreate output stream and fill from buffer
                    _writeBytes = new ByteArrayOutputStream();
                    writeStream = new DataOutputStream(_writeBytes);
                    writeStream.write(dataBuffer);
                }
                else {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        SibTr.debug(this, tc, "Body is writeable but producer has promised not to modify the payload after it's been set, ignore write streams");
                }
            }

        } catch (IOException e) {
            // No FFDC code needed
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "Error during readObject", e);
            // pass on
            throw e;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "readObject");
    }

    //////////////////////////////////////////////////////////////////////////////
    // Package & private methods
    //////////////////////////////////////////////////////////////////////////////

    /**
     * @see com.ibm.ws.sib.api.jms.impl.JmsMessageImpl#instantiateMessage()
     */
    @Override
    protected JsJmsMessage instantiateMessage() throws Exception {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "instantiateMessage");

        // Create a new message object.
        JsJmsBytesMessage newMsg = null;

        try {
            newMsg = jmfact.createJmsBytesMessage();
        } catch (MessageCreateFailedException e) {
            // No FFDC code needed
            // 238447 Don't call process throwable here to generate FFDC as it will be done in JmsMessageImpl.
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "Error occurred creating message", e);
            throw e;
        }

        // Do any other reference storing here (for subclasses)
        jsBytesMsg = newMsg;

        // Return the new object.
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "instantiateMessage", newMsg);
        return newMsg;
    }

    /**
     * Perform some of the initialisation steps that used to be done in the (JsJmsBytesMessage)
     * constructor. Postponing these steps until first read allows a performance improvement
     * for cases where the message is never read. SIB0121: renamed to 'lazyInitForReading'
     */
    private void lazyInitForReading() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "lazyInitForReading");

        // reset the flag to indicate that the message is not yet ready to be read
        requiresInit = false;

        // Get the contents of the message and initialise the data streams
        dataBuffer = jsBytesMsg.getBytes();

        // If the bytes message was sent by a non-JSJMS client that did not populate the
        // body bytes then it is possible that the dataBuffer is now null, which would
        // lead to NPE when we create the ByteArrayInputStream (228074).
        if (dataBuffer == null)
            dataBuffer = new byte[0];

        readStream = new ByteArrayInputStream(dataBuffer);
        dataStart = 0;

        // set the encoding fields
        try {
            if (propertyExists(ApiJmsConstants.ENCODING_PROPERTY)) {
                int encoding = getIntProperty(ApiJmsConstants.ENCODING_PROPERTY);
                integerEncoding = encoding & ApiJmsConstants.ENC_INTEGER_MASK;
                floatEncoding = encoding & ApiJmsConstants.ENC_FLOAT_MASK;
            }
        } catch (JMSException je) {
            // No FFDC code needed
            // can safely ignore and use defaults instead
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "ignoring exception whilst getting encoding " + je);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "lazyInitForReading");
    }

    /**
     * override the getMsgReference method of the parent class.
     * We need to ensure that the message body is written into the JS message
     * before returning the reference to it.
     */
    @Override
    protected JsJmsMessage getMsgReference() throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getMsgReference");

        // If the body is read only, the contents should already be in the
        // JS message, so only do the copy over if we are in write mode.
        // SIB0121: ... and if the producer might modify the payload after it's been set
        if (!isBodyReadOnly() && !producerWontModifyPayloadAfterSet) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "Body is writeable & producer hasn't promised not to modify the payload - attempt encoding");

            try {
                int encoding = integerEncoding | floatEncoding; // NB bitwise or (|), not logical or (||)
                // check if JMS_IBM_Encoding has been set
                if (propertyExists(ApiJmsConstants.ENCODING_PROPERTY)) {
                    encoding = getIntProperty(ApiJmsConstants.ENCODING_PROPERTY);
                }

                if (bodySetInJsMsg && (encoding == lastEncoding)) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        SibTr.debug(this, tc, "Body cached - encoding not necessary");
                }
                else {
                    // flatten and encode the data streams
                    byte[] encodedBody = _exportBody(encoding);
                    // and set it into the Jetstream message
                    jsBytesMsg.setBytes(encodedBody);
                    lastEncoding = encoding;
                    bodySetInJsMsg = true;
                }
            }

            catch (JMSException e) {
                // No FFDC code needed

                // d222942 review JMSException could be thrown by _exportBody if passed an unknown
                // value for encoding, or if there was an IO error handling the streams. These already
                // have good nls messages, so we should simply pass on, not wrap with a generic message.
                // Similarly, getIntProperty can throw a JMSException which can also be passed on.
                throw e;
            }

        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getMsgReference", jsBytesMsg);
        return jsBytesMsg;
    }

    /**
     * Internal use method that returns the body of the JMS BytesMessage as a byte array suitable
     * for transmission
     * 
     * @param encoding int The numeric encoding (required)
     *            The numeric encoding parameter selects the manner in which numeric values are encoded in the byte array
     *            It applies only to messages that are populated by the client application itself. If a message is read in and
     *            resent by the client, the resent method has the same encoding as the incoming message.
     * @param characterSet String The character set encoding (ignored)
     * @exception javax.jms.JMSException if the encoding is not recognized, or if an IOException occurs.
     * 
     *                Used from getMsgReference to flatten to byte[] and put in JS message. JBK
     * 
     */
    private byte[] _exportBody(int encoding) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "_exportBody", encoding);

        int requestedIntegerEncoding;
        int requestedFloatEncoding;

        try {
            // get data from output stream.
            dataBuffer = _writeBytes.toByteArray();
            dataStart = 0; // start from the first byte in the buffer

            // The following code (up to returning dataBuffer) used to be wrapped in a check to see if the
            // message was populated by the client, however it was realised that the alternative case would
            // never be executed in SIB, so was removed. The variable that was checked was: 'populated_by_client'

            // split the encoding parameter into its integer and float components
            requestedIntegerEncoding = encoding & ApiJmsConstants.ENC_INTEGER_MASK;
            requestedFloatEncoding = encoding & ApiJmsConstants.ENC_FLOAT_MASK;

            // if the caller has asked for an integer encoding that differs from the one currently in the
            // buffer, then we have to go through our list of numeric items byteswapping them all

            if (((integerEncoding <= 1) && (requestedIntegerEncoding == 2))
                || ((integerEncoding == 2) && (requestedIntegerEncoding <= 1))) {

                // For economy we split the actual list into a set of arrays. The last segment of the list is held in the
                // variables numeric_count, numeric_offsets[] and numeric_sizes[]. Previous segments are saved away in the
                // numerics Vector. If there are any saved-away values in the vectors, we process them first
                if (integers != null) {
                    for (int i = 0; i < integers.size(); i += 2) {
                        // Retrieve the offset and size arrays from their storing place in
                        // the Vector. NB the Vector is built
                        // by the recordNumeric method, so this code has to be kept in
                        // step with that.

                        int[] offsets = (int[]) integers.elementAt(i);
                        int[] sizes = (int[]) integers.elementAt(i + 1);

                        for (int j = 0; j < ARRAY_SIZE; j++) {
                            reverse(dataBuffer, offsets[j], sizes[j]);
                        }
                    }
                }

                // Now run through the final segment of the list - this is still in our variables and never made it into
                // the Vector

                for (int j = 0; j < integer_count; j++) {
                    reverse(dataBuffer, integer_offsets[j], integer_sizes[j]);
                }
                // d252032 can't reset this.*encoding since we get the unswapped data from
                // the stream each time.
                // Remember that the encoding has now changed
                //this.integerEncoding = requestedIntegerEncoding;
            }

            // if the caller has asked for a float encoding that differs from the one currently in the
            // buffer, then we have to go through the float vector and regenerate the float and double elements
            if (floatEncoding != requestedFloatEncoding) {

                // For economy we split the actual list into a set of arrays. The last segment of the list is held in the
                // variables numeric_count, numeric_offsets[] and numeric_sizes[]. Previous segments are saved away in the
                // numerics Vector. If there are any saved-away values in the vectors, we process them first
                byte[] floatbytes;
                ByteArrayOutputStream floatbaos = new ByteArrayOutputStream(8);
                DataOutputStream floatstream = new DataOutputStream(floatbaos);

                if (float_offsets != null && float_values != null) { // These ought to get set at the same time, but check both for safety.
                    for (int i = 0; i < float_offsets.size(); i++) {
                        floatbaos.reset();
                        Object value = float_values.elementAt(i);
                        int offset = ((Integer) (float_offsets.elementAt(i))).intValue();

                        if (value instanceof Float) {
                            float floatValue = ((Float) value).floatValue();

                            switch (requestedFloatEncoding) {
                                case ApiJmsConstants.ENC_FLOAT_IEEE_NORMAL:
                                case ApiJmsConstants.ENC_FLOAT_IEEE_REVERSED:
                                    floatstream.writeInt(Float.floatToIntBits(floatValue));
                                    break;
                                case ApiJmsConstants.ENC_FLOAT_S390:
                                    floatstream.writeInt(JMS390FloatSupport.floatToS390IntBits(floatValue));
                                    break;
                                default:
                                    throw (JMSException) JmsErrorUtils.newThrowable(JMSException.class,
                                                                                    "BAD_ENCODING_CWSIA0181", new Object[] { Integer
                                                                                                    .toHexString(requestedFloatEncoding) }, tc);
                            }

                            floatbytes = floatbaos.toByteArray();
                            if (requestedFloatEncoding == ApiJmsConstants.ENC_FLOAT_IEEE_REVERSED) {
                                reverse(floatbytes, 0, 4); // reverse all 4 bytes
                            }
                            System.arraycopy(floatbytes, 0, dataBuffer, offset, 4);
                        }

                        else if (value instanceof Double) {
                            double doubleValue = ((Double) value).doubleValue();

                            switch (requestedFloatEncoding) {
                                case ApiJmsConstants.ENC_FLOAT_IEEE_NORMAL:
                                case ApiJmsConstants.ENC_FLOAT_IEEE_REVERSED:
                                    floatstream.writeLong(Double.doubleToLongBits(doubleValue));
                                    break;
                                case ApiJmsConstants.ENC_FLOAT_S390:
                                    floatstream.writeLong(JMS390FloatSupport.doubleToS390LongBits(doubleValue));
                                    break;
                                default:
                                    throw (JMSException) JmsErrorUtils.newThrowable(JMSException.class,
                                                                                    "BAD_ENCODING_CWSIA0181", new Object[] { Integer
                                                                                                    .toHexString(requestedFloatEncoding) }, tc);
                            }

                            floatbytes = floatbaos.toByteArray();
                            if (requestedFloatEncoding == ApiJmsConstants.ENC_FLOAT_IEEE_REVERSED) {
                                reverse(floatbytes, 0, 8); // reverse all 8 bytes
                            }
                            System.arraycopy(floatbytes, 0, dataBuffer, offset, 8);
                        }
                    }
                }

                // d252032 can't reset this.*encoding since we get the unswapped data from
                // the stream each time.
                // Remember that the encoding has now changed
                //this.floatEncoding = requestedFloatEncoding;

            }

        } catch (IOException e) {
            // No FFDC code needed
            // d238447 FFDC review. This seems an appropriate case to generate an FFDC.
            throw (JMSException) JmsErrorUtils.newThrowable(
                                                            JMSException.class,
                                                            "DATA_STREAM_PROBLEM_CWSIA0182",
                                                            null,
                                                            e,
                                                            "JmsBytesMessageImpl._exportBody#1",
                                                            this,
                                                            tc
                            );
        }

        // Return the body
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "_exportBody", dataBuffer);
        return dataBuffer;
    }

    /**
     * Records the presence of an integer-style item in the bytes message stream. This method is called by the writeXXX methods
     * to keep a track of where the integer items are, so they can be byteswapped if necessary at send time.
     * 
     * NB. The arrays and vector maintained by this method are read directly by the _exportBody() method, so these two method
     * implementations need to be kept in step
     * 
     * @param offset int offset of start of numeric item
     * @param length int length of the item (2,4,8 bytes)
     */
    private void recordInteger(int offset, int length) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "recordInteger", new Object[] { offset, length });

        // If the current arrays are full, save them in the vector and allocate some new ones
        if (integer_count == ARRAY_SIZE) {
            if (integers == null)
                integers = new Vector();
            integers.addElement(integer_offsets);
            integers.addElement(integer_sizes);
            integer_offsets = new int[ARRAY_SIZE];
            integer_sizes = new int[ARRAY_SIZE];
            integer_count = 0;
        }

        // Add the offset and size of the current numeric item to the arrays
        integer_offsets[integer_count] = offset;
        integer_sizes[integer_count++] = length;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "recordInteger");
    }

    /**
     * This method reverses a sequence of bytes within a byte array. It is used for byte swapping numeric values
     * 
     * @param buffer byte[]
     * @param offset int offset of start of sequence
     * @param length int length of sequence in bytes
     */
    private void reverse(byte[] buffer, int offset, int length) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "reverse", new Object[] { buffer, offset, length });

        byte temp;
        for (int i = 0; i < length / 2; i++) {
            temp = buffer[offset + i];
            buffer[offset + i] = buffer[offset + (length - 1) - i];
            buffer[offset + (length - 1) - i] = temp;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "reverse");
    }

    /**
     * Checks to see if the producer has promised not to modify the payload after it's been set.
     * If they have, then throw a JMS exception based on the parameters
     * 
     * @throws JMSException
     */
    private void checkProducerPromise(String jmsMethod, String ffdcProbeID) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "checkProducerPromise", new Object[] { jmsMethod, ffdcProbeID });

        // Only proceed if the producer hasn't promised not to modify the payload
        // after setting it.
        if (producerWontModifyPayloadAfterSet) {
            throw (JMSException) JmsErrorUtils.newThrowable(
                                                            IllegalStateException.class, // JMS illegal state exception
                                                            "PROMISE_BROKEN_EXCEPTION_CWSIA0510", // promise broken
                                                            new Object[] { jmsMethod }, // insert = jms method name
                                                            null, // no cause, original exception
                                                            ffdcProbeID, // Probe ID
                                                            this, // caller (?)
                                                            tc); // Trace component
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "checkProducerPromise");
    }

    /**
     * @param paramClass
     * @return
     * @throws JMSException
     * @throws MessageFormatException
     */
    @Override
    public <T> T getBody(Class<T> paramClass) throws JMSException, MessageFormatException {
        T returnObj = null;
        reset();

        try {
            returnObj = super.getBody(paramClass);
        } finally {
            reset();
        }

        return returnObj;
    }

}
