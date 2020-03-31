/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.cxf.interceptor;

import java.io.OutputStream;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

import java.util.logging.Level; 
import java.util.logging.Logger; 
import org.apache.cxf.common.logging.LogUtils;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.common.i18n.BundleUtils;
import org.apache.cxf.io.AbstractWrappedOutputStream;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.staxutils.StaxUtils;

/**
 * Creates an XMLStreamReader from the InputStream on the Message.
 */
public class StaxOutInterceptor extends AbstractPhaseInterceptor<Message> {

    private static final Logger LOG = LogUtils.getL7dLogger(StaxOutInterceptor.class); 
    public static final String OUTPUT_STREAM_HOLDER = StaxOutInterceptor.class.getName() + ".outputstream";
    public static final String WRITER_HOLDER = StaxOutInterceptor.class.getName() + ".writer";
    public static final String FORCE_START_DOCUMENT = "org.apache.cxf.stax.force-start-document";
    public static final StaxOutEndingInterceptor ENDING 
        = new StaxOutEndingInterceptor(OUTPUT_STREAM_HOLDER, WRITER_HOLDER);
    
    private static final ResourceBundle BUNDLE = BundleUtils.getBundle(StaxOutInterceptor.class);
    private static Map<Object, XMLOutputFactory> factories = new HashMap<Object, XMLOutputFactory>();

    
    public StaxOutInterceptor() {
        super(Phase.PRE_STREAM);
        addAfter(AttachmentOutInterceptor.class.getName());
    }

    public void handleMessage(Message message) {
        OutputStream os = message.getContent(OutputStream.class);
        XMLStreamWriter xwriter = message.getContent(XMLStreamWriter.class);
        Exchange ex = message.getExchange();
        Writer writer = null;

        if (os == null) {
            writer = message.getContent(Writer.class);
        }
        if ((os == null && writer == null) || xwriter != null) {
            return;
        }

        String encoding = getEncoding(message);
        
        try {
            XMLOutputFactory factory = getXMLOutputFactory(message);
            if (factory == null) {
                if (writer == null) {
                    os = setupOutputStream(message, os);
                    xwriter = StaxUtils.createXMLStreamWriter(os, encoding);
                } else {
                    xwriter = StaxUtils.createXMLStreamWriter(writer);
                }
            } else {
                synchronized (factory) {
                    if (writer == null) {
                        os = setupOutputStream(message, os);
                        xwriter = factory.createXMLStreamWriter(os, encoding);
                    } else {
                        xwriter = factory.createXMLStreamWriter(writer);
                    }
                }
            }

	    Message exInMsg = ex.getInMessage();
	    Boolean addXmlHdr = false;
            if (exInMsg != null) {
	       addXmlHdr = MessageUtils.isTrue(exInMsg.getContextualProperty(FORCE_START_DOCUMENT)); 
	       LOG.log(Level.FINE, "XML Header property in Input message: " + addXmlHdr);
            }

            if (MessageUtils.getContextualBoolean(message, FORCE_START_DOCUMENT, false) || addXmlHdr) {
	        LOG.log(Level.FINE, "Adding XML declaration header to message...");
                xwriter.writeStartDocument(encoding, "1.0");
                message.removeContent(OutputStream.class);
                message.put(OUTPUT_STREAM_HOLDER, os);
                message.removeContent(Writer.class);
                message.put(WRITER_HOLDER, writer);
            }
        } catch (XMLStreamException e) {
	    System.out.println("Caught XMLStreamException: " + e);
            throw new Fault(new org.apache.cxf.common.i18n.Message("STREAM_CREATE_EXC", BUNDLE), e);
        }
        message.setContent(XMLStreamWriter.class, xwriter);

        // Add a final interceptor to write end elements
        message.getInterceptorChain().add(ENDING);
    }
    private OutputStream setupOutputStream(Message message, OutputStream os) {
        if (!(os instanceof AbstractWrappedOutputStream)) {
            os = new AbstractWrappedOutputStream(os) { };
        }
        ((AbstractWrappedOutputStream)os).allowFlush(false);
        return os;
    }

    @Override
    public void handleFault(Message message) {
        super.handleFault(message);
        OutputStream os = (OutputStream)message.get(OUTPUT_STREAM_HOLDER);
        if (os != null) {
            message.setContent(OutputStream.class, os);
        }
        Writer writer = (Writer)message.get(WRITER_HOLDER);
        if (writer != null) {
            message.setContent(Writer.class, writer);
        }
    }

    private String getEncoding(Message message) {
        Exchange ex = message.getExchange();
        String encoding = (String)message.get(Message.ENCODING);
        if (encoding == null && ex.getInMessage() != null) {
            encoding = (String) ex.getInMessage().get(Message.ENCODING);
            message.put(Message.ENCODING, encoding);
        }
        
        if (encoding == null) {
            encoding = "UTF-8";
            message.put(Message.ENCODING, encoding);
        }
        return encoding;
    }

    public static XMLOutputFactory getXMLOutputFactory(Message m) throws Fault {
        Object o = m.getContextualProperty(XMLOutputFactory.class.getName());
        if (o instanceof XMLOutputFactory) {
            m.put(AbstractOutDatabindingInterceptor.DISABLE_OUTPUTSTREAM_OPTIMIZATION,
                        Boolean.TRUE);
            m.put(FORCE_START_DOCUMENT, Boolean.TRUE);
            return (XMLOutputFactory)o;
        } else if (o != null) {
            XMLOutputFactory xif = factories.get(o);
            if (xif == null) {
                Class<?> cls;
                if (o instanceof Class) {
                    cls = (Class<?>)o;
                } else if (o instanceof String) {
                    try {
                        cls = ClassLoaderUtils.loadClass((String)o, StaxInInterceptor.class);
                    } catch (ClassNotFoundException e) {
                        throw new Fault(e);
                    }
                } else {
                    throw new Fault(new org.apache.cxf.common.i18n.Message("INVALID_INPUT_FACTORY", 
                                                                           BUNDLE, o));
                }

                try {
                    xif = (XMLOutputFactory)(cls.newInstance());
                    factories.put(o, xif);
                } catch (InstantiationException e) {
                    throw new Fault(e);
                } catch (IllegalAccessException e) {
                    throw new Fault(e);
                }
            }
            m.put(AbstractOutDatabindingInterceptor.DISABLE_OUTPUTSTREAM_OPTIMIZATION,
                  Boolean.TRUE);
            m.put(FORCE_START_DOCUMENT, Boolean.TRUE);
            return xif;
        }
        return null;
    }
    
}
