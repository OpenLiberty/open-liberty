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

import java.io.FilterWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.cxf.common.injection.NoJSR250Annotations;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.interceptor.*;
import org.apache.cxf.io.CacheAndWriteOutputStream;
import org.apache.cxf.io.CachedOutputStream;
import org.apache.cxf.io.CachedOutputStreamCallback;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.Phase;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;

/**
 *
 */
@NoJSR250Annotations
public class LibertyLoggingOutInterceptor extends LoggingOutInterceptor {
    private static final Logger LOG = LogUtils.getLogger(LoggingOutInterceptor.class);
    private static final String LOG_SETUP = LoggingOutInterceptor.class.getName() + ".log-setup";

    // Add the Liberty tracing component so that the SOAPMessage will print if CXF tracing is
    // dynamically enabled.
    private static final TraceComponent tc = Tr.register(
                                                         LibertyLoggingOutInterceptor.class, null);

    public static final LibertyLoggingOutInterceptor INSTANCE = new LibertyLoggingOutInterceptor();

    // Allows the interceptor to know if the enabledLoggingInOutInterceptor property has been set
    // If the property is then the LibertyLoggingOutInterceptor will print the contents of the SOAP Message
    // To the messages.log file.
    private static boolean loggingInterceptorConfigProp = false;

    public LibertyLoggingOutInterceptor(String phase) {
        super(phase);
        addBefore(StaxOutInterceptor.class.getName());
    }

    public LibertyLoggingOutInterceptor() {
        this(Phase.PRE_STREAM);
    }

    public LibertyLoggingOutInterceptor(int lim) {
        this();
        limit = lim;
    }

    public LibertyLoggingOutInterceptor(PrintWriter w) {
        this();
        this.writer = w;
    }

    /**
     * @param b
     */
    public LibertyLoggingOutInterceptor(boolean loggingInterceptorConfigProp) {
        this();
        LibertyLoggingOutInterceptor.loggingInterceptorConfigProp = loggingInterceptorConfigProp;
    }

    public void handleMessage(Message message) throws Fault {
        final OutputStream os = message.getContent(OutputStream.class);
        final Writer iowriter = message.getContent(Writer.class);
        if (os == null && iowriter == null) {
            return;
        }

        Logger logger = getMessageLogger(message);
        if (tc.isDebugEnabled()) {
            if (logger.isLoggable(Level.INFO) || writer != null) {
                // Write the output while caching it for the log message
                boolean hasLogged = message.containsKey(LOG_SETUP);
                if (!hasLogged) {
                    message.put(LOG_SETUP, Boolean.TRUE);
                    if (os != null) {
                        final CacheAndWriteOutputStream newOut = new CacheAndWriteOutputStream(os);
                        if (threshold > 0) {
                            newOut.setThreshold(threshold);
                        }
                        message.setContent(OutputStream.class, newOut);
                        newOut.registerCallback(new LoggingCallback(logger, message, os));
                    } else {
                        message.setContent(Writer.class, new LogWriter(logger, message, iowriter));
                    }
                }
            }
        } else if (loggingInterceptorConfigProp) {
            if (logger.isLoggable(Level.INFO) || writer != null) {
                // Write the output while caching it for the log message
                boolean hasLogged = message.containsKey(LOG_SETUP);
                if (!hasLogged) {
                    message.put(LOG_SETUP, Boolean.TRUE);
                    if (os != null) {
                        final CacheAndWriteOutputStream newOut = new CacheAndWriteOutputStream(os);
                        if (threshold > 0) {
                            newOut.setThreshold(threshold);
                        }
                        message.setContent(OutputStream.class, newOut);
                        newOut.registerCallback(new LoggingCallback(logger, message, os));
                    } else {
                        message.setContent(Writer.class, new LogWriter(logger, message, iowriter));
                    }
                }
            }
        }
    }

    private LoggingMessage setupBuffer(Message message) {
        String id = (String) message.getExchange().get(LoggingMessage.ID_KEY);
        if (id == null) {
            id = LoggingMessage.nextId();
            message.getExchange().put(LoggingMessage.ID_KEY, id);
        }
        final LoggingMessage buffer = new LoggingMessage("Outbound Message\n---------------------------", id);

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
        String address = (String) message.get(Message.ENDPOINT_ADDRESS);
        if (address != null) {
            buffer.getAddress().append(address);
        }
        String ct = (String) message.get(Message.CONTENT_TYPE);
        if (ct != null) {
            buffer.getContentType().append(ct);
        }
        Object headers = message.get(Message.PROTOCOL_HEADERS);
        if (headers != null) {
            buffer.getHeader().append(headers);
        }
        return buffer;
    }

    private class LogWriter extends FilterWriter {
        StringWriter out2;
        int count;
        Logger logger; //NOPMD
        Message message;

        public LogWriter(Logger logger, Message message, Writer writer) {
            super(writer);
            this.logger = logger;
            this.message = message;
            if (!(writer instanceof StringWriter)) {
                out2 = new StringWriter();
            }
        }

        public void write(int c) throws IOException {
            super.write(c);
            if (out2 != null && count < limit) {
                out2.write(c);
            }
            count++;
        }

        public void write(char[] cbuf, int off, int len) throws IOException {
            super.write(cbuf, off, len);
            if (out2 != null && count < limit) {
                out2.write(cbuf, off, len);
            }
            count += len;
        }

        public void write(String str, int off, int len) throws IOException {
            super.write(str, off, len);
            if (out2 != null && count < limit) {
                out2.write(str, off, len);
            }
            count += len;
        }

        public void close() throws IOException {
            LoggingMessage buffer = setupBuffer(message);
            if (count >= limit) {
                buffer.getMessage().append("(message truncated to " + limit + " bytes)\n");
            }
            StringWriter w2 = out2;
            if (w2 == null) {
                w2 = (StringWriter) out;
            }
            String ct = (String) message.get(Message.CONTENT_TYPE);
            try {
                writePayload(buffer.getPayload(), w2, ct);
            } catch (Exception ex) {
                //ignore
            }
            log(logger, buffer.toString());
            message.setContent(Writer.class, out);
            super.close();
        }
    }

    protected String formatLoggingMessage(LoggingMessage buffer) {
        return buffer.toString();
    }

    class LoggingCallback implements CachedOutputStreamCallback {

        private final Message message;
        private final OutputStream origStream;
        private final Logger logger; //NOPMD

        public LoggingCallback(final Logger logger, final Message msg, final OutputStream os) {
            this.logger = logger;
            this.message = msg;
            this.origStream = os;
        }

        @Trivial
        public void onFlush(CachedOutputStream cos) {

        }

        public void onClose(CachedOutputStream cos) {
            LoggingMessage buffer = setupBuffer(message);

            String ct = (String) message.get(Message.CONTENT_TYPE);
            if (!isShowBinaryContent() && isBinaryContent(ct)) {
                buffer.getMessage().append(BINARY_CONTENT_MESSAGE).append('\n');
                log(logger, formatLoggingMessage(buffer));
                return;
            }

            if (cos.getTempFile() == null) {
                //buffer.append("Outbound Message:\n");
                if (cos.size() > limit) {
                    buffer.getMessage().append("(message truncated to " + limit + " bytes)\n");
                }
            } else {
                buffer.getMessage().append("Outbound Message (saved to tmp file):\n");
                buffer.getMessage().append("Filename: " + cos.getTempFile().getAbsolutePath() + "\n");
                if (cos.size() > limit) {
                    buffer.getMessage().append("(message truncated to " + limit + " bytes)\n");
                }
            }
            try {
                String encoding = (String) message.get(Message.ENCODING);
                writePayload(buffer.getPayload(), cos, encoding, ct);
            } catch (Exception ex) {
                //ignore
            }

            log(logger, formatLoggingMessage(buffer));
            try {
                //empty out the cache
                cos.lockOutputStream();
                cos.resetOut(null, false);
            } catch (Exception ex) {
                //ignore
            }
            message.setContent(OutputStream.class,
                               origStream);
        }
    }

    @Override
    protected Logger getLogger() {
        return LOG;

    }

    public void setEnableLoggingProp(boolean loggingInterceptorConfigProp) {
        this.loggingInterceptorConfigProp = loggingInterceptorConfigProp;
    }
}
