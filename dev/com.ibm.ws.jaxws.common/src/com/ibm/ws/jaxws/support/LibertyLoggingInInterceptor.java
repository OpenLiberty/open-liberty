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
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.message.MessageUtils;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.jaxws.JaxWsConstants;
import javax.xml.stream.XMLStreamReader;
import java.io.ByteArrayInputStream;

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

	Boolean validate = MessageUtils.isTrue(message.getContextualProperty("cxf.enable.schema.validation"));
        if (tc.isDebugEnabled()) {
           Tr.debug(tc, "Property cxf.enable.schema.validation is set to: " + validate);
        }

        Logger logger = getMessageLogger(message);
        if (tc.isDebugEnabled()) {
            if (logger.isLoggable(Level.INFO) || writer != null) {
                newlogging(logger, message, validate);
            }
        } else if (loggingInterceptorConfigProp) {
            if (logger.isLoggable(Level.INFO) || writer != null) {
                newlogging(logger, message, validate);
            }
        }
	else if (validate) {
	    validatelogging(logger, message);
	}
    }

    protected void newlogging(Logger logger, Message message, Boolean validateXml) throws Fault {
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

	if (validateXml) {

	   // Validate format of SOAP message, checks for missing end-tag elements, etc.
	   ByteArrayInputStream paybais = null;
	   XMLStreamReader xmlReader1 = null;
	   try {
	      paybais = new ByteArrayInputStream(buffer.getPayload().toString().getBytes());
	      xmlReader1 = StaxUtils.createXMLStreamReader(paybais);
	      if (xmlReader1 != null) {
	         while (xmlReader1.hasNext()) {
	            xmlReader1.next();
	         }
              }
	   }
	   catch (Exception e1) {
              if (tc.isDebugEnabled()) {
                 Tr.debug(tc, "Exception occurred validating XML: " + e1);
              }
              throw new Fault(e1);
	   }
	   finally {
	      try {
	         if (xmlReader1 != null) {
		    xmlReader1.close();
	         }	
	         if (paybais != null) {
	            paybais.close();
	         }
	      }
	      catch (Exception e2) {
	        // Ignore
	      }
	   }
	}  // end if validateXml

        log(logger, formatLoggingMessage(buffer));

    }


    protected void logging(Logger logger, Message message) throws Fault {

	newlogging(logger, message, false);

    }


    protected void validatelogging(Logger logger, Message message) throws Fault {

        final LoggingMessage buffer = new LoggingMessage("0", "1");
        String ct = (String) message.get(Message.CONTENT_TYPE);
        String encoding = (String) message.get(Message.ENCODING);

        if (!isShowBinaryContent() && isBinaryContent(ct)) {
            // buffer.getMessage().append(BINARY_CONTENT_MESSAGE).append('\n');
            // log(logger, buffer.toString());
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

	// Validate format of SOAP message, checks for missing end-tag elements, etc.
	ByteArrayInputStream paybais = null;
	XMLStreamReader xmlReader1 = null;
	try {
	   paybais = new ByteArrayInputStream(buffer.getPayload().toString().getBytes());
	   xmlReader1 = StaxUtils.createXMLStreamReader(paybais);
	   if (xmlReader1 != null) {
	      while (xmlReader1.hasNext()) {
	         xmlReader1.next();
	      }
           }
	}
	catch (Exception e1) {
           throw new Fault(e1);
	}
	finally {
	   try {
	      if (xmlReader1 != null) {
	  xmlReader1.close();
	      }	
	      if (paybais != null) {
	         paybais.close();
	      }
	   }
	   catch (Exception e2) {
	     // Ignore
	   }
	}

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
