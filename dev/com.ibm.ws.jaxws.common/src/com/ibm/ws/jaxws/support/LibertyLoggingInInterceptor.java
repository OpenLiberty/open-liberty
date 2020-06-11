/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxws.support;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.cxf.common.injection.NoJSR250Annotations;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.interceptor.*;
import org.apache.cxf.io.CachedOutputStream;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.Phase;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.jaxws.JaxWsConstants;

/**
 * A simple logging handler which outputs the bytes of the message to the
 * Logger.
 */
@NoJSR250Annotations
public class LibertyLoggingInInterceptor extends LoggingInInterceptor {

    public static final LibertyLoggingInInterceptor INSTANCE = new LibertyLoggingInInterceptor();

    private static final Logger LOG = LogUtils.getLogger(LoggingInInterceptor.class);

    private static final TraceComponent tc = Tr.register(
                                                         LibertyLoggingInInterceptor.class, null);

    // Allows the interceptor to know if the enabledLoggingInOutInterceptor property has been set
    // If the property is then the LibertyLoggingInInterceptor will print the contents of the SOAP Message
    // To the messages.log file.
    private static boolean loggingInterceptorConfigProp = false;

    public LibertyLoggingInInterceptor() {
        super(Phase.RECEIVE);
    }

    public LibertyLoggingInInterceptor(String phase) {
        super(phase);
    }

    public LibertyLoggingInInterceptor(String id, String phase) {
        super(id, phase);
    }

    public LibertyLoggingInInterceptor(int lim) {
        this();
        limit = lim;
    }

    public LibertyLoggingInInterceptor(String id, int lim) {
        this(id, Phase.RECEIVE);
        limit = lim;
    }

    public LibertyLoggingInInterceptor(PrintWriter w) {
        this();
        this.writer = w;
    }

    public LibertyLoggingInInterceptor(String id, PrintWriter w) {
        this(id, Phase.RECEIVE);
        this.writer = w;
    }

    /**
     * @param b
     */
    public LibertyLoggingInInterceptor(boolean loggingInterceptorConfigProp) {
        this();
        LibertyLoggingInInterceptor.loggingInterceptorConfigProp = loggingInterceptorConfigProp;
    }

    @Override
    public void handleMessage(Message message) throws Fault {

        Logger logger = getMessageLogger(message);
        if (tc.isDebugEnabled()) {
            if (logger.isLoggable(Level.INFO) || writer != null) {
                logging(logger, message);
            }
        } else if (loggingInterceptorConfigProp) {
            if (logger.isLoggable(Level.INFO) || writer != null) {
                logging(logger, message);
            }
        }
    }

    protected void logging(Logger logger, Message message) throws Fault {
        if (message.containsKey(LoggingMessage.ID_KEY)) {
            return;
        }
        String id = (String) message.getExchange().get(LoggingMessage.ID_KEY);
        if (id == null) {
            id = LoggingMessage.nextId();
            message.getExchange().put(LoggingMessage.ID_KEY, id);
        }
        message.put(LoggingMessage.ID_KEY, id);
        final LoggingMessage buffer = new LoggingMessage("Inbound Message\n----------------------------", id);

        Integer responseCode = (Integer) message.get(Message.RESPONSE_CODE);
        if (responseCode != null) {
            buffer.getResponseCode().append(responseCode);
        }

        String encoding = (String) message.get(Message.ENCODING);

        if (encoding != null) {
            buffer.getEncoding().append(encoding);
        }
        String httpMethod = (String) message.get(Message.HTTP_REQUEST_METHOD);
        if (httpMethod != null) {
            buffer.getHttpMethod().append(httpMethod);
        }
        String ct = (String) message.get(Message.CONTENT_TYPE);
        if (ct != null) {
            buffer.getContentType().append(ct);
        }
        Object headers = message.get(Message.PROTOCOL_HEADERS);

        if (headers != null) {
            buffer.getHeader().append(headers);
        }
        String uri = (String) message.get(Message.REQUEST_URL);
        if (uri != null) {
            buffer.getAddress().append(uri);
            String query = (String) message.get(Message.QUERY_STRING);
            if (query != null) {
                buffer.getAddress().append("?").append(query);
            }
        }

        if (!isShowBinaryContent() && isBinaryContent(ct)) {
            buffer.getMessage().append(BINARY_CONTENT_MESSAGE).append('\n');
            log(logger, buffer.toString());
            return;
        }

        InputStream is = message.getContent(InputStream.class);
        if (is != null) {
            CachedOutputStream bos = new CachedOutputStream();
            if (threshold > 0) {
                bos.setThreshold(threshold);
            }
            try {
                IOUtils.copy(is, bos);

                bos.flush();
                is.close();

                message.setContent(InputStream.class, bos.getInputStream());
                if (bos.getTempFile() != null) {
                    //large thing on disk...
                    buffer.getMessage().append("\nMessage (saved to tmp file):\n");
                    buffer.getMessage().append("Filename: " + bos.getTempFile().getAbsolutePath() + "\n");
                }
                if (bos.size() > limit) {
                    buffer.getMessage().append("(message truncated to " + limit + " bytes)\n");
                }
                writePayload(buffer.getPayload(), bos, encoding, ct);

                bos.close();
            } catch (Exception e) {
                throw new Fault(e);
            }
        } else {
            Reader reader = message.getContent(Reader.class);
            if (reader != null) {
                try {
                    BufferedReader r = new BufferedReader(reader, limit);
                    r.mark(limit);
                    char b[] = new char[limit];
                    int i = r.read(b);
                    buffer.getPayload().append(b, 0, i);
                    r.reset();
                    message.setContent(Reader.class, r);
                } catch (Exception e) {
                    throw new Fault(e);
                }

            }
        }
        log(logger, formatLoggingMessage(buffer));
    }

    protected String formatLoggingMessage(LoggingMessage loggingMessage) {

        return loggingMessage.toString();
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }

    public void setEnableLoggingProp(boolean loggingInterceptorConfigProp) {

        LibertyLoggingInInterceptor.loggingInterceptorConfigProp = loggingInterceptorConfigProp;
    }
}
