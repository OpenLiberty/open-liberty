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
package org.apache.cxf.jaxws.interceptors;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import javax.xml.bind.JAXBContext;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import org.apache.cxf.attachment.AttachmentImpl;
import org.apache.cxf.attachment.ByteDataSource;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.interceptor.AbstractSoapInterceptor;
import org.apache.cxf.binding.soap.model.SoapBodyInfo;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.databinding.DataBinding;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.helpers.LoadingByteArrayOutputStream;
import org.apache.cxf.interceptor.AttachmentOutInterceptor;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.jaxb.JAXBDataBinding;
import org.apache.cxf.message.Attachment;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.model.BindingMessageInfo;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.MessagePartInfo;
import org.apache.cxf.staxutils.StaxUtils;

import com.ibm.websphere.ras.annotation.Sensitive;

public class SwAOutInterceptor extends AbstractSoapInterceptor {
 // Liberty Change issue #26529
    private static final Logger LOG = LogUtils.getL7dLogger(SwAOutInterceptor.class);   // Liberty Change issue #26529
    private static final Map<String, Method> SWA_REF_METHOD
        = new ConcurrentHashMap<>(4, 0.75f, 2);
    private static final Set<String> SWA_REF_NO_METHOD
        = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>(4, 0.75f, 2));

    //  Liberty Change Start -  Migration of behaviour from CXF 2.6.2
    private static boolean skipHasSwaRef;
    
    static {

        String skipSwaRef = System.getProperty("cxf.multipart.attachment");
        if(LOG.isLoggable(Level.FINE))  {   // Liberty Change issue #26529
            LOG.fine("cxf.multipart.attachment property value: " + skipSwaRef);
        }
        if (skipSwaRef != null 
            && skipSwaRef.trim().length() > 0
            && skipSwaRef.trim().equalsIgnoreCase("false")) {
            skipHasSwaRef = true;
        } else {
            skipHasSwaRef = false;
        }

    } // Liberty Change End
    
    AttachmentOutInterceptor attachOut = new AttachmentOutInterceptor();

    public SwAOutInterceptor() {
        super(Phase.PRE_LOGICAL);
        addAfter(HolderOutInterceptor.class.getName());
        addBefore(WrapperClassOutInterceptor.class.getName());
        if(LOG.isLoggable(Level.FINE))  {   // Liberty Change issue #26529
            LOG.fine("WrapperClassOutInterceptor is added before and HolderOutInterceptor is added after SwAOutInterceptor.");
        }
    }

    private boolean callSWARefMethod(final JAXBContext ctx) {
        
        boolean isFinestEnabled = LOG.isLoggable(Level.FINEST);   // Liberty Change issue #26529
        String cname = ctx.getClass().getName();
        Method m = SWA_REF_METHOD.get(cname);
        if(isFinestEnabled)  {
            LOG.finest("Method that is obtained from SWA_REF_METHOD with JAXBContext class name: " + cname + "  is : " + m);   // Liberty Change issue #26529
        } 
        if (m == null && !SWA_REF_NO_METHOD.contains(cname)) {
            try {
                m = AccessController.doPrivileged(new PrivilegedExceptionAction<Method>() {

                    public Method run() throws Exception {
                        Method hasSwaRefMethod = ctx.getClass().getMethod("hasSwaRef", new Class[0]);
                        if (!hasSwaRefMethod.isAccessible()) {
                            hasSwaRefMethod.setAccessible(true);
                        }
                        return hasSwaRefMethod;
                    }
                });
                if(isFinestEnabled)  {
                    LOG.finest("Method that is obtained from SWA_REF_METHOD with JAXBContext class name: " + cname + " using privileged access: " + m);   // Liberty Change issue #26529
                } 
                if (m == null) {
                    SWA_REF_NO_METHOD.add(cname);
                    if(isFinestEnabled)  {
                        LOG.finest("JAXBContext class name: " + cname + " is added to SWA_REF_NO_METHOD.");   // Liberty Change issue #26529
                    } 
                } else {
                    SWA_REF_METHOD.put(cname, m);
                    if(isFinestEnabled)  {
                        LOG.finest("Method : " + m + " is added to SWA_REF_METHOD with JAXBContext class name: " + cname);   // Liberty Change issue #26529
                    } 
                }
            } catch (Exception e) {
                //ignore
                if(isFinestEnabled)  {
                    LOG.finest("Exception stacktrace while calling SWARefMethod: " + e.getStackTrace());   // Liberty Change issue #26529
                } 
            }
        }
        try {
            if (m != null) {
                return (Boolean)m.invoke(ctx);
            }
        } catch (Exception e) {
            //ignore
            if(isFinestEnabled)  {
                LOG.finest("Exception invoking method with JAXBContext: " + e.getStackTrace() );   // Liberty Change issue #26529
            } 
        }
        return false;
    }

    public void handleMessage(@Sensitive SoapMessage message) throws Fault { // Liberty Change
        boolean isFinestEnabled = LOG.isLoggable(Level.FINEST);   // Liberty Change issue #26529
        boolean isFineEnabled = LOG.isLoggable(Level.FINE);   // Liberty Change issue #26529
        Exchange ex = message.getExchange();
        BindingOperationInfo bop = ex.getBindingOperationInfo();
        if (bop == null) {
            if(isFinestEnabled)  {
                LOG.finest("BindingOperationInfo is null. Returning.");   // Liberty Change issue #26529
            } 
            return;
        }

        if (bop.isUnwrapped()) {
            bop = bop.getWrappedOperation();
            if(isFinestEnabled)  {
                LOG.finest("BindingOperationInfo is switched to wrapped version.");   // Liberty Change issue #26529
            }
        }

        boolean client = isRequestor(message);
        BindingMessageInfo bmi = client ? bop.getInput() : bop.getOutput();

        if (bmi == null) {
            if(isFinestEnabled)  {
                LOG.finest("BindingMessageInfo is null. Returning.");   // Liberty Change issue #26529
            }
            return;
        }
		// Liberty Change Start - Migration of behaviour from CXF 2.6.2
        Boolean newAttachment = false;
        Message exOutMsg = ex.getOutMessage();
        if (exOutMsg != null) {
            newAttachment = MessageUtils.isTrue(exOutMsg.getContextualProperty("cxf.add.attachments"));
            if(isFineEnabled)  {
                LOG.fine("Request context attachment property: cxf.add.attachments is set to: " + newAttachment);
            }
        } // Liberty change end
        
        SoapBodyInfo sbi = bmi.getExtensor(SoapBodyInfo.class);

        if (sbi == null || sbi.getAttachments() == null || sbi.getAttachments().isEmpty()) {
            Service s = ex.getService();
            DataBinding db = s.getDataBinding();
            if (db instanceof JAXBDataBinding
                && hasSwaRef((JAXBDataBinding) db)) {
            	// Liberty Change Start - Migration of behaviour from CXF 2.6.2
                Boolean includeAttachs = false;
                Message exInpMsg = ex.getInMessage();
                if(isFineEnabled)  {    // Liberty Change issue #26529
                    LOG.log(Level.FINE, "Exchange Input message: " + exInpMsg);
                }
                if (exInpMsg != null) {
                    includeAttachs = MessageUtils.isTrue(exInpMsg.getContextualProperty("cxf.add.attachments"));
                }   
                if(isFineEnabled)  {    // Liberty Change issue #26529
                    LOG.log(Level.FINE, "Add attachments message property: cxf.add.attachments value is " + includeAttachs);
                }
                if (!skipHasSwaRef || includeAttachs || newAttachment) {
                    setupAttachmentOutput(message);
                } else {
                    skipAttachmentOutput(message);
                } // Liberty change end
            }
            return;
        }
        processAttachments(message, sbi);
    }
    protected void processAttachments(@Sensitive SoapMessage message, SoapBodyInfo sbi) { // Liberty Change
        boolean isFinestEnabled = LOG.isLoggable(Level.FINEST);   // Liberty Change issue #26529
        Collection<Attachment> atts = setupAttachmentOutput(message);
        List<Object> outObjects = CastUtils.cast(message.getContent(List.class));

        for (MessagePartInfo mpi : sbi.getAttachments()) {
            String partName = mpi.getConcreteName().getLocalPart();
            String ct = (String) mpi.getProperty(Message.CONTENT_TYPE);

            String id = new StringBuilder().append(partName)
                .append('=')
                .append(UUID.randomUUID())
                .append("@apache.org").toString();
            if(isFinestEnabled)  {
                LOG.finest("Id created for MessagePartInfo: " + id);   // Liberty Change issue #26529
            } 
            // this assumes things are in order...
            int idx = mpi.getIndex();
            Object o = outObjects.get(idx);

            if (o == null) {
                if(isFinestEnabled)  {
                    LOG.finest("MessagePartInfo with index: " + idx + " doesn't exist in message content.This MessagePartInfo will be skipped.");   // Liberty Change issue #26529
                } 
                continue;
            }
            outObjects.set(idx, null);
            DataHandler dh;

            if(isFinestEnabled)  {
                LOG.finest("Object that is obtained from message content with index: (" + idx + ") is : " + o);   // Liberty Change issue #26529
            } 
            // This code could probably be refactored out somewhere...
            if (o instanceof Source) {
                dh = new DataHandler(createDataSource((Source)o, ct));
                if(isFinestEnabled)  {
                    LOG.finest("DataHandler constructed by DataSource creation with Source parameter: " + dh);   // Liberty Change issue #26529
                } 
            } else if (o instanceof Image) {
                final Image img = (Image)o;
                final String contentType = ct;
                dh = new DataHandler(o, ct) {
                    @Override
                    public InputStream getInputStream() throws IOException {
                        LoadingByteArrayOutputStream bout = new LoadingByteArrayOutputStream();
                        writeTo(bout);
                        return bout.createInputStream();
                    }
                    @Override
                    public void writeTo(OutputStream out) throws IOException {
                        ImageWriter writer = null;
                        Iterator<ImageWriter> writers = ImageIO.getImageWritersByMIMEType(contentType);
                        if (writers.hasNext()) {
                            writer = writers.next();
                        }
                        if (writer != null) {
                            BufferedImage bimg = convertToBufferedImage(img);
                            ImageOutputStream iout = ImageIO.createImageOutputStream(out);
                            writer.setOutput(iout);
                            writer.write(bimg);
                            writer.dispose();
                            iout.flush();
                            out.flush();
                        }
                    }
                };
                if(isFinestEnabled)  {
                    LOG.finest("DataHandler constructed by Stream creation: " + dh);   // Liberty Change issue #26529
                } 
            } else if (o instanceof DataHandler) {
                dh = (DataHandler) o;
                ct = dh.getContentType();

                try {
                    if ("text/xml".equals(ct)
                        && dh.getContent() instanceof Source) {
                        dh = new DataHandler(createDataSource((Source)dh.getContent(), ct));
                    }
                } catch (IOException e) {
                    //ignore, use same dh
                }
                if(isFinestEnabled)  {
                    LOG.finest("DataHandler constructed by DataSource creation with DataHandler content as Source parameter: " + dh);   // Liberty Change issue #26529
                } 
            } else if (o instanceof byte[]) {
                if (ct == null) {
                    ct = "application/octet-stream";
                }
                dh = new DataHandler(new ByteDataSource((byte[])o, ct));
                if(isFinestEnabled)  {
                    LOG.finest("DataHandler constructed from ByteDataSource by byte[] object: " + dh);   // Liberty Change issue #26529
                } 
            } else if (o instanceof String) {
                if (ct == null) {
                    ct = "text/plain; charset=\'UTF-8\'";
                }
                dh = new DataHandler(new ByteDataSource(((String)o).getBytes(StandardCharsets.UTF_8), ct));
                if(isFinestEnabled)  {
                    LOG.finest("DataHandler constructed with ByteDataSource by String object: " + dh);   // Liberty Change issue #26529
                } 
            } else {
                throw new Fault(new org.apache.cxf.common.i18n.Message("ATTACHMENT_NOT_SUPPORTED",
                                                                       LOG, o.getClass()));
            }

            AttachmentImpl att = new AttachmentImpl(id);
            att.setDataHandler(dh);
            att.setHeader("Content-Type", ct);
            att.setHeader("Content-ID", "<" + id + ">");
            atts.add(att);
            if(isFinestEnabled)  {
                LOG.finest("AttachmentImpl is added to attachment collection " + att + ", with DataHandler: " + dh + ", Content-Type: " + ct + ", Content-ID:" + id);   // Liberty Change issue #26529
            } 
        }
    }

    protected boolean hasSwaRef(JAXBDataBinding db) {
        JAXBContext context = db.getContext();
        return callSWARefMethod(context);
    }

    private DataSource createDataSource(Source o, String ct) {
        DataSource ds = null;

        if (o instanceof StreamSource) {
            StreamSource src = (StreamSource)o;
            try {
                if (src.getInputStream() != null) {
                    try (ByteArrayOutputStream bos = new ByteArrayOutputStream(2048)) {
                        IOUtils.copy(src.getInputStream(), bos, 1024);
                        ds = new ByteDataSource(bos.toByteArray(), ct);
                    }
                } else {
                    ds = new ByteDataSource(IOUtils.toString(src.getReader()).getBytes(StandardCharsets.UTF_8),
                                                 ct);
                }
            } catch (IOException e) {
                throw new Fault(e);
            }
        } else {
            ByteArrayOutputStream bwriter = new ByteArrayOutputStream();
            XMLStreamWriter writer = null;
            try {
                writer = StaxUtils.createXMLStreamWriter(bwriter);
                StaxUtils.copy(o, writer);
                writer.flush();
                ds = new ByteDataSource(bwriter.toByteArray(), ct);
            } catch (XMLStreamException e1) {
                throw new Fault(e1);
            } finally {
                StaxUtils.close(writer);
            }
        }
        return ds;
    }

    private BufferedImage convertToBufferedImage(Image image) throws IOException {
        if (image instanceof BufferedImage) {
            if(LOG.isLoggable(Level.FINEST))  {
                LOG.finest("Image is already a BufferedImage. Returning the cached image.");   // Liberty Change issue #26529
            } 
            return (BufferedImage)image;
        }

        // Wait until the image is completely loaded
        MediaTracker tracker = new MediaTracker(new Component() {
            private static final long serialVersionUID = 6412221228374321325L;
        });
        tracker.addImage(image, 0);
        try {
            tracker.waitForAll();
        } catch (InterruptedException e) {
            throw new Fault(e);
        }

        // Create a BufferedImage so we can write it out later
        BufferedImage bufImage = new BufferedImage(
                image.getWidth(null),
                image.getHeight(null),
                BufferedImage.TYPE_INT_ARGB);

        Graphics g = bufImage.createGraphics();
        g.drawImage(image, 0, 0, null);
        return bufImage;
    }

    private Collection<Attachment> setupAttachmentOutput(SoapMessage message) {
        // We have attachments, so add the interceptor
        message.getInterceptorChain().add(attachOut);
        // We should probably come up with another property for this
        message.put(AttachmentOutInterceptor.WRITE_ATTACHMENTS, Boolean.TRUE);


        Collection<Attachment> atts = message.getAttachments();
        if(LOG.isLoggable(Level.FINEST))  {      // Liberty Change issue #26529
            LOG.log(Level.FINE, "setupAttachmentOutput: getAttachments returned  " + atts);     // Liberty Change issue #26529
        }
        
        
        if (atts == null) {
            atts = new ArrayList<>();
            message.setAttachments(atts);
            if(LOG.isLoggable(Level.FINEST))  { 
                LOG.finest("There is no attachment in the message. An empty list is created and set in the message.");   // Liberty Change issue #26529
            } 
        }
        return atts;
    }
    
	// Liberty Change Start - Migration of behaviour from CXF 2.6.2
    private Collection<Attachment> skipAttachmentOutput(SoapMessage message) {

        boolean isFinestEnabled = LOG.isLoggable(Level.FINEST);   // Liberty Change issue #26529
        
        Collection<Attachment> atts = message.getAttachments();
        
        // Please do not modify log message below, it's been used in PropertySettingTest
        if(LOG.isLoggable(Level.FINE))  {       // Liberty Change issue #26529
            LOG.fine("skipAttachmentOutput: getAttachments returned  " + atts);
        }
        
        if (atts != null) {
            // We have attachments, so add the interceptor
            message.getInterceptorChain().add(attachOut);
            // We should probably come up with another property for this
            message.put(AttachmentOutInterceptor.WRITE_ATTACHMENTS, Boolean.TRUE);
            if(isFinestEnabled)  {
                LOG.finest("AttachmentOutInterceptor is set into interceptor chain. write.attachments is set to true in message.");   // Liberty Change issue #26529
            } 
        } else {    
            atts = new ArrayList<Attachment>();
            message.setAttachments(atts);
            if(isFinestEnabled)  {
                LOG.finest("A newly created empty list is set as attachements.");   // Liberty Change issue #26529
            } 
        }               
        
        return atts;
    } // Liberty change end
}
