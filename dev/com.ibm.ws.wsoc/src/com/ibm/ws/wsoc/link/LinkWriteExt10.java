package com.ibm.ws.wsoc.link;

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
import com.ibm.ws.wsoc.MessageWriter;
import com.ibm.ws.wsoc.OpcodeType;
import com.ibm.ws.wsoc.WsocConnLink;
import com.ibm.ws.wsoc.WsocWriteCallback;
import com.ibm.ws.wsoc.MessageWriter.WRITE_TYPE;
import com.ibm.ws.wsoc.util.Utils;
import com.ibm.wsspi.bytebuffer.WsByteBuffer;
import com.ibm.wsspi.tcpchannel.TCPRequestContext;
import com.ibm.wsspi.tcpchannel.TCPWriteRequestContext;

public class LinkWriteExt10 extends LinkWrite {

        private static final TraceComponent tc = Tr.register(LinkWriteExt10.class);
    
        public void processWrite(TCPWriteRequestContext wsc) {

        // write completed successfully - call sendHandler if this was the result of websocket async write.
        // if a Send with a Future is being used, then we are using our future send handler here.

        if (wsocSendOutstanding == true) {

            wsocSendOutstanding = false;
            if (wsocSendHandler != null) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "calling onResult on SendHandler: " + wsocSendHandler);
                }
                System.out.println("SESSION TO USE " + connLink.getWsocSession());
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
}
