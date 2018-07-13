/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.wsoc;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.websocket.EncodeException;
import javax.websocket.Encoder;
import javax.websocket.EndpointConfig;
import javax.websocket.SendHandler;
import javax.websocket.SendResult;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.wsoc.MessageWriter.WRITE_TYPE;
import com.ibm.ws.wsoc.util.Utils;
import com.ibm.wsspi.bytebuffer.WsByteBuffer;
import com.ibm.wsspi.tcpchannel.TCPRequestContext;
import com.ibm.wsspi.tcpchannel.TCPWriteRequestContext;

/**
 *
 */
public class LinkWrite {

    private static final TraceComponent tc = Tr.register(LinkWrite.class);

    private HashMap<Class<?>, Encoder> binaryEncoders = null;
    private HashMap<Class<?>, Encoder> textEncoders = null;
    private HashMap<Class<?>, Encoder> binaryStreamEncoders = null;
    private HashMap<Class<?>, Encoder> textStreamEncoders = null;

    private TCPWriteRequestContext tcpWriteContext = null;
    private MessageWriter messageWriter = null;
    private boolean wsocSendOutstanding = false;
    private SendHandler wsocSendHandler = null;
    private final SendResult SendResultGood = new SendResult();
    private EndpointConfig endpointConfig = null;
    private boolean shouldMaskData = false;

    private WsocConnLink connLink = null;

    public void initialize(TCPWriteRequestContext _wrc, EndpointConfig _epc, WsocConnLink _link, boolean _shouldMaskData) {
        tcpWriteContext = _wrc;
        endpointConfig = _epc;
        connLink = _link;
        this.shouldMaskData = _shouldMaskData;

    }

    public void frameCleanup() {
        messageWriter.frameCleanup();
    }

    public void processWrite(TCPWriteRequestContext wsc) {

        // write completed successfully - call sendHandler if this was the result of websocket async write.
        // if a Send with a Future is being used, then we are using our future send handler here.

        if (wsocSendOutstanding == true) {

            wsocSendOutstanding = false;
            if (wsocSendHandler != null) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "calling onResult on SendHandler: " + wsocSendHandler);
                }
                wsocSendHandler.onResult(SendResultGood);
            }
        }

    }

    public void processError(TCPWriteRequestContext wsc, Throwable ioe) {
        // write completed with an error - call sendHandler if this was the result of websocket async write
        // if a Send with a Future is being used, then we are using our future send handler here.

        // cleanup up before calling onResult, since onResult, or an async user thread, may want to oddly write data right away
        // no cleanup if exception occurred before trying to write on the wire
        if (wsc != null) {
            messageWriter.frameCleanup();
        }

        if (wsocSendOutstanding == true) {

            wsocSendOutstanding = false;
            if (wsocSendHandler != null) {

                SendResult result = new SendResult(ioe);
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "calling onResult on SendHandler: " + wsocSendHandler);
                }
                wsocSendHandler.onResult(result);
            }
        }

    }

    public void destroy(Exception e) {

        if (binaryEncoders != null) {
            destroyEncoders(binaryEncoders);
        }
        if (textEncoders != null) {
            destroyEncoders(textEncoders);
        }
        if (binaryStreamEncoders != null) {
            destroyEncoders(binaryStreamEncoders);
        }
        if (textStreamEncoders != null) {
            destroyEncoders(textStreamEncoders);
        }
    }

    private void destroyEncoders(Map<?, Encoder> _encoders) {
        Iterator<?> it = _encoders.entrySet().iterator();
        while (it.hasNext()) {
            @SuppressWarnings("unchecked")
            Map.Entry<?, Encoder> entry = (Entry<?, Encoder>) it.next();
            Encoder en = entry.getValue();
            en.destroy();
            it.remove();
        }
    }

    @FFDCIgnore(IOException.class)
    public boolean cancelWriteBufferAsync() {
        try {
            messageWriter.cancelMessageAsync();
            return true;
        } catch (IOException e) {
            // do NOT allow instrumented FFDC to be used here
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "caught IOException: " + e);
            }
            return false;
        }

    }

    @FFDCIgnore(IOException.class)
    public void writeBuffer(@Sensitive WsByteBuffer buffer, OpcodeType ot, WRITE_TYPE writeType, SendHandler handler, int timeout) throws IOException {
        if (messageWriter == null) {
            WsocWriteCallback writeCallback = connLink.getWriteCallback();
            messageWriter = new MessageWriter();
            messageWriter.initialize(tcpWriteContext, writeCallback, shouldMaskData);
        }

        try {
            if (writeType == WRITE_TYPE.ASYNC) {
                wsocSendHandler = handler;
                wsocSendOutstanding = true;
            }

            messageWriter.WriteMessage(buffer, ot, timeout, writeType);

        } catch (IOException x) {
            // do NOT allow instrumented FFDC to be used here
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Caught IOException: " + x.getMessage());
            }
            if (writeType == WRITE_TYPE.ASYNC) {
                // TCPChannel with not throw an IOException if tell it to do an Async Write with force queue set to true,
                // which is what we will do later on in this code path.
                // So, IOException can not be thrown by this Async path, so this clause is really dead code.
                // but also not right in theory to throw back an IOException.  So add debug, convert the
                // exception to a runtime exception, and throw that back.
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Unexpectedly Caught IOException on Async Write code path: " + x);
                }

                wsocSendOutstanding = false;
                RuntimeException re = new RuntimeException(x);
                throw re;

            } else { //sync case. Throw exception to the caller to caller, which eventually invokes onError().
                throw x;
            }
        }

    }

    public void writeObject(@Sensitive Object objectToWrite, WRITE_TYPE writeType, SendHandler handler, boolean fromOnMessage) throws EncodeException, IOException {

        Class<?> clazzToWrite = objectToWrite.getClass();

        if (messageWriter == null) {
            WsocWriteCallback writeCallback = connLink.getWriteCallback();
            messageWriter = new MessageWriter();
            messageWriter.initialize(this.tcpWriteContext, writeCallback, shouldMaskData);
        }

        if (writeType == WRITE_TYPE.ASYNC) {
            this.wsocSendHandler = handler;
            this.wsocSendOutstanding = true;
        }

        List<Class<? extends Encoder>> encoders = endpointConfig.getEncoders();
        if (encoders == null) {
            encoders = Collections.emptyList();
        }

        boolean encoderUsed = false;
        for (Class<? extends Encoder> encoder : encoders) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "looking at encoder: " + encoder);
            }
            if (encoderUsed) {
                break;
            }

            // try to find a encoder with the same generic type as that of the type we are looking for.
            ArrayList<Type> interfaces = new ArrayList<Type>();
            Utils.getAllInterfaces(encoder, interfaces);
            Object ta[] = interfaces.toArray();
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "ta[]: " + Arrays.toString(ta));
            }
            for (Object t : ta) {

                Class<?> clazz = Utils.getCodingClass((Type) t);
                if ((clazz != null) && (clazzToWrite != null)) {
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "encoding looking at clazz: " + clazz);
                        Tr.debug(tc, "with clazzToWrite: " + clazzToWrite);
                    }

                    if (clazz.isAssignableFrom(clazzToWrite)) {

                        encoderUsed = writeUsingEncoderFromCache(objectToWrite, clazz, writeType);

                        if (!encoderUsed) {
                            Encoder en = null;
                            // encoder wasn't found, so make a new one, cache it for the next time, and then use it
                            try {
                                en = encoder.newInstance();
                            } catch (Exception e) {
                                throw new EncodeException(objectToWrite, e.getMessage(), e.getCause());
                            }

                            if (en instanceof Encoder.Binary) {
                                encoderUsed = writeUsingEncoderBinary(objectToWrite, clazz, writeType, (Encoder.Binary) en, true);

                            } else if (en instanceof Encoder.Text) {
                                encoderUsed = writeUsingEncoderText(objectToWrite, clazz, writeType, (Encoder.Text) en, true);

                            } else if (en instanceof Encoder.BinaryStream) {
                                encoderUsed = writeUsingEncoderBinaryStream(objectToWrite, clazz, writeType, (Encoder.BinaryStream) en, true);

                            } else if (en instanceof Encoder.TextStream) {
                                encoderUsed = writeUsingEncoderTextStream(objectToWrite, clazz, writeType, (Encoder.TextStream) en, true);

                            }
                        }
                    }
                }
            }

        }

        if (!encoderUsed) {
            // no user encoder was found for this object (or primitive), see if this is a primitive and use a default encoding
            if (objectToWrite instanceof String) {
                String data = (String) objectToWrite;
                byte[] ba = data.toString().getBytes(Utils.UTF8_CHARSET);
                writeTextMessage(ba, writeType);
            } else if (objectToWrite instanceof Character) {
                Character data = (Character) objectToWrite;
                byte[] ba = data.toString().getBytes(Utils.UTF8_CHARSET);
                writeTextMessage(ba, writeType);
            } else if (objectToWrite instanceof Boolean) {
                Boolean data = (Boolean) objectToWrite;
                byte[] ba = data.toString().getBytes(Utils.UTF8_CHARSET);
                writeTextMessage(ba, writeType);
            } else if (objectToWrite instanceof Short) {
                Short data = (Short) objectToWrite;
                byte[] ba = data.toString().getBytes(Utils.UTF8_CHARSET);
                writeTextMessage(ba, writeType);
            } else if (objectToWrite instanceof Integer) {
                Integer data = (Integer) objectToWrite;
                byte[] ba = data.toString().getBytes(Utils.UTF8_CHARSET);
                writeTextMessage(ba, writeType);
            } else if (objectToWrite instanceof Long) {
                Long data = (Long) objectToWrite;
                byte[] ba = data.toString().getBytes(Utils.UTF8_CHARSET);
                writeTextMessage(ba, writeType);
            } else if (objectToWrite instanceof Float) {
                Float data = (Float) objectToWrite;
                byte[] ba = data.toString().getBytes(Utils.UTF8_CHARSET);
                writeTextMessage(ba, writeType);
            } else if (objectToWrite instanceof Double) {
                Double data = (Double) objectToWrite;
                byte[] ba = data.toString().getBytes(Utils.UTF8_CHARSET);
                writeTextMessage(ba, writeType);
            } else if (objectToWrite instanceof Byte) {
                byte[] ba = ((Byte) objectToWrite).toString().getBytes(Utils.UTF8_CHARSET);
                writeTextMessage(ba, writeType);
            } else if (objectToWrite instanceof byte[]) {
                WsByteBuffer wsBuffer = connLink.getBufferManager().wrap((byte[]) objectToWrite);
                messageWriter.WriteMessage(wsBuffer, OpcodeType.BINARY_WHOLE, TCPWriteRequestContext.NO_TIMEOUT, writeType);
            } else if (objectToWrite instanceof ByteBuffer) {
                WsByteBuffer wsBuffer = connLink.getBufferManager().wrap((ByteBuffer) objectToWrite);
                messageWriter.WriteMessage(wsBuffer, OpcodeType.BINARY_WHOLE, TCPWriteRequestContext.NO_TIMEOUT, writeType);
            } else {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Can not send data, no encoder found: ");
                }
                //can't send data since there is no encoder. Report to the user calling onError()
                EncodeException encodeException = new EncodeException(objectToWrite, "Can not send data, no encoder found");
                connLink.callOnError(encodeException);
            }

        }

    }

    private boolean writeUsingEncoderText(@Sensitive Object objectToWrite, Class<?> encoderClass, WRITE_TYPE writeType, Encoder.Text _en,
                                          boolean addToCache) throws IOException, EncodeException {
        int timeout = TCPRequestContext.NO_TIMEOUT;
        Encoder.Text en = _en;
        if (en == null) {
            if (textEncoders != null) {
                en = (Encoder.Text) textEncoders.get(encoderClass);
            }
        }

        if (en != null) {
            String s = en.encode(objectToWrite);
            byte[] ba = s.getBytes(Utils.UTF8_CHARSET);
            WsByteBuffer buffer = connLink.getBufferManager().wrap(ba);

            messageWriter.WriteMessage(buffer, OpcodeType.TEXT_WHOLE, timeout, writeType);

            if (addToCache) {
                if (textEncoders == null) {
                    textEncoders = new HashMap<Class<?>, Encoder>();
                }
                en.init(endpointConfig);
                textEncoders.put(encoderClass, en);

            }
            return true;
        }
        return false;
    }

    @FFDCIgnore(IOException.class)
    private boolean writeUsingEncoderTextStream(@Sensitive Object objectToWrite, Class encoderClass, WRITE_TYPE writeType, Encoder.TextStream _en,
                                                boolean addToCache) throws IOException, EncodeException {
        int timeout = TCPRequestContext.NO_TIMEOUT;
        Encoder.TextStream en = _en;
        if (en == null) {
            if (textStreamEncoders != null) {
                en = (Encoder.TextStream) textStreamEncoders.get(encoderClass);
            }
        }

        if (en != null) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            OutputStreamWriter osw = new OutputStreamWriter(bos);

            en.encode(objectToWrite, osw);
            try {
                osw.flush();
            } catch (IOException x) {
                // do nothing, user encoder may have close the stream, which gives a stream closed ioexception on the flush
            }

            byte[] ba = bos.toByteArray();
            WsByteBuffer buffer = connLink.getBufferManager().wrap(ba);

            messageWriter.WriteMessage(buffer, OpcodeType.TEXT_WHOLE, timeout, writeType);

            if (addToCache) {
                if (textStreamEncoders == null) {
                    textStreamEncoders = new HashMap<Class<?>, Encoder>();
                }
                en.init(endpointConfig);
                textStreamEncoders.put(encoderClass, en);
            }
            return true;
        }
        return false;
    }

    private boolean writeUsingEncoderBinary(@Sensitive Object objectToWrite, Class encoderClass, WRITE_TYPE writeType, Encoder.Binary _en,
                                            boolean addToCache) throws IOException, EncodeException {
        int timeout = TCPRequestContext.NO_TIMEOUT;
        Encoder.Binary en = _en;
        if (en == null) {
            if (binaryEncoders != null) {
                en = (Encoder.Binary) binaryEncoders.get(encoderClass);
            }
        }

        if (en != null) {
            ByteBuffer b = en.encode(objectToWrite);
            WsByteBuffer buffer = connLink.getBufferManager().wrap(b);

            messageWriter.WriteMessage(buffer, OpcodeType.BINARY_WHOLE, timeout, writeType);

            if (addToCache) {
                if (binaryEncoders == null) {
                    binaryEncoders = new HashMap<Class<?>, Encoder>();
                }
                en.init(endpointConfig);
                binaryEncoders.put(encoderClass, en);
            }

            return true;
        }
        return false;
    }

    private boolean writeUsingEncoderBinaryStream(@Sensitive Object objectToWrite, Class encoderClass, WRITE_TYPE writeType, Encoder.BinaryStream _en,
                                                  boolean addToCache) throws IOException, EncodeException {
        int timeout = TCPRequestContext.NO_TIMEOUT;
        Encoder.BinaryStream en = _en;
        if (en == null) {
            if (binaryStreamEncoders != null) {
                en = (Encoder.BinaryStream) binaryStreamEncoders.get(encoderClass);
            }
        }

        if (en != null) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            en.encode(objectToWrite, bos);
            byte[] ba = bos.toByteArray();
            WsByteBuffer buffer = connLink.getBufferManager().wrap(ba);

            messageWriter.WriteMessage(buffer, OpcodeType.BINARY_WHOLE, timeout, writeType);

            if (addToCache) {
                if (binaryStreamEncoders == null) {
                    binaryStreamEncoders = new HashMap<Class<?>, Encoder>();
                }
                en.init(endpointConfig);
                binaryStreamEncoders.put(encoderClass, en);
            }
            return true;
        }
        return false;
    }

    private boolean writeUsingEncoderFromCache(@Sensitive Object objectToWrite, Class encoderClass, WRITE_TYPE writeType) throws IOException, EncodeException {

        if (textEncoders != null) {
            Encoder.Text en = (Encoder.Text) textEncoders.get(encoderClass);
            if (en != null) {
                return writeUsingEncoderText(objectToWrite, encoderClass, writeType, en, false);
            }
        }

        if (textStreamEncoders != null) {
            Encoder.TextStream en = (Encoder.TextStream) textStreamEncoders.get(encoderClass);
            if (en != null) {
                return writeUsingEncoderTextStream(objectToWrite, encoderClass, writeType, en, false);
            }
        }

        if (binaryEncoders != null) {
            Encoder.Binary en = (Encoder.Binary) binaryEncoders.get(encoderClass);
            if (en != null) {
                return writeUsingEncoderBinary(objectToWrite, encoderClass, writeType, en, false);
            }
        }

        if (binaryStreamEncoders != null) {
            Encoder.BinaryStream en = (Encoder.BinaryStream) binaryStreamEncoders.get(encoderClass);
            if (en != null) {
                return writeUsingEncoderBinaryStream(objectToWrite, encoderClass, writeType, en, false);
            }
        }

        // didn't find an encoder that was in one of the encoder caches
        return false;
    }

    private void writeTextMessage(byte[] byteArray, WRITE_TYPE type) throws IOException {
        ByteBuffer buffer = ByteBuffer.wrap(byteArray);
        WsByteBuffer wsBuffer = connLink.getBufferManager().wrap(buffer);
        messageWriter.WriteMessage(wsBuffer, OpcodeType.TEXT_WHOLE, TCPWriteRequestContext.NO_TIMEOUT, type);
    }
}
