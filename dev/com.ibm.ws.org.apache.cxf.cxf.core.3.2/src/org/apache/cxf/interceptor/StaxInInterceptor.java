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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.PropertyUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.helpers.HttpHeaderHelper;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.staxutils.StaxUtils;

import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.websphere.ras.annotation.Trivial;

/**
 * Creates an XMLStreamReader from the InputStream on the Message.
 */
public class StaxInInterceptor extends AbstractPhaseInterceptor<Message> {

    private static final Logger LOG = LogUtils.getL7dLogger(StaxInInterceptor.class);

    private static Map<Object, XMLInputFactory> factories = new HashMap<>();

    @Trivial  // Liberty change: line is added
    public StaxInInterceptor() {
        super(Phase.POST_STREAM);
    }
    @Trivial  // Liberty change: line is added
    public StaxInInterceptor(String phase) {
        super(phase);
    }

    public void handleMessage(@Sensitive Message message) { // Liberty change: @Sensitive is added to parameter
        if (isGET(message) || message.getContent(XMLStreamReader.class) != null) {
            LOG.fine("StaxInInterceptor skipped.");
            return;
        }
        InputStream is = message.getContent(InputStream.class);
        Reader reader = null;
        if (is == null) {
            reader = message.getContent(Reader.class);
            if (reader == null) {
                return;
            }
        }
        String contentType = (String)message.get(Message.CONTENT_TYPE);

        if (contentType != null
            && contentType.contains("text/html")
            && MessageUtils.isRequestor(message)) {
            StringBuilder htmlMessage = new StringBuilder(1024);
            try {
                if (reader == null) {
                    reader = new InputStreamReader(is, (String)message.get(Message.ENCODING));
                }
                char[] s = new char[1024];
                int i = reader.read(s);
                while (htmlMessage.length() < 64536 && i > 0) {
                    htmlMessage.append(s, 0, i);
                    i = reader.read(s);
                }
            } catch (IOException e) {
                throw new Fault(new org.apache.cxf.common.i18n.Message("INVALID_HTML_RESPONSETYPE",
                        LOG, "(none)"));
            }
            throw new Fault(new org.apache.cxf.common.i18n.Message("INVALID_HTML_RESPONSETYPE",
                    LOG, (htmlMessage.length() == 0) ? "(none)" : htmlMessage));
        }
        if (contentType == null) {
            //if contentType is null, this is likely a an empty post/put/delete/similar, lets see if it's
            //detectable at all
            Map<String, List<String>> m = CastUtils.cast((Map<?, ?>)message.get(Message.PROTOCOL_HEADERS));
            if (m != null) {
                List<String> contentLen = HttpHeaderHelper
                    .getHeader(m, HttpHeaderHelper.CONTENT_LENGTH);
                List<String> contentTE = HttpHeaderHelper
                    .getHeader(m, HttpHeaderHelper.CONTENT_TRANSFER_ENCODING);
                List<String> transferEncoding = HttpHeaderHelper
                    .getHeader(m, HttpHeaderHelper.TRANSFER_ENCODING);
                if ((StringUtils.isEmpty(contentLen) || "0".equals(contentLen.get(0)))
                    && StringUtils.isEmpty(contentTE)
                    && (StringUtils.isEmpty(transferEncoding)
                    || !"chunked".equalsIgnoreCase(transferEncoding.get(0)))) {
                    return;
                }
            }
        }

        String encoding = (String)message.get(Message.ENCODING);

        XMLStreamReader xreader;
        try {
            XMLInputFactory factory = getXMLInputFactory(message);
            if (factory == null) {
                if (reader != null) {
                    xreader = StaxUtils.createXMLStreamReader(reader);
                } else {
                    xreader = StaxUtils.createXMLStreamReader(is, encoding);
                }
            } else {
                if (PropertyUtils.isTrue(message.getContextualProperty(Message.THREAD_SAFE_STAX_FACTORIES))) {
                    if (reader != null) {
                        xreader = factory.createXMLStreamReader(reader);
                    } else {
                        xreader = factory.createXMLStreamReader(is, encoding);
                    }
                } else {
                    synchronized (factory) {
                        if (reader != null) {
                            xreader = factory.createXMLStreamReader(reader);
                        } else {
                            xreader = factory.createXMLStreamReader(is, encoding);
                        }
                    }
                }
            }
            // xreader = StaxUtils.configureReader(xreader, message);   Liberty change: line is removed
        } catch (XMLStreamException e) {
            throw new Fault(new org.apache.cxf.common.i18n.Message("STREAM_CREATE_EXC",
                                                                   LOG,
                                                                   encoding), e);
        }
        message.setContent(XMLStreamReader.class, xreader);
        message.getInterceptorChain().add(StaxInEndingInterceptor.INSTANCE);
    }


    /**
     * @throws Fault
     */
    @Trivial  // Liberty change: line is added
    public static XMLInputFactory getXMLInputFactory(Message m) {
        Object o = m.getContextualProperty(XMLInputFactory.class.getName());
        if (o instanceof XMLInputFactory) {
            return (XMLInputFactory)o;
        } else if (o != null) {
            XMLInputFactory xif = factories.get(o);
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
                    throw new Fault(
                                    new org.apache.cxf.common.i18n.Message("INVALID_INPUT_FACTORY",
                                                                           LOG, o));
                }

                try {
                    xif = (XMLInputFactory)(cls.newInstance());
                    factories.put(o, xif);
                } catch (InstantiationException | IllegalAccessException e) {
                    throw new Fault(e);
                }
            }
            return xif;
        }
        return null;
    }
}
