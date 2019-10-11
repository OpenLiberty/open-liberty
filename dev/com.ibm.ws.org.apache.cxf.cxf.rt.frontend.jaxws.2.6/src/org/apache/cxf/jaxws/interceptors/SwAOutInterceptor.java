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
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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

public class SwAOutInterceptor extends AbstractSoapInterceptor {
    private static final Logger LOG = LogUtils.getL7dLogger(SwAOutInterceptor.class);
    
    private static final Map<String, Method> SWA_REF_METHOD 
        = new ConcurrentHashMap<String, Method>(4, 0.75f, 2);
    
    private static boolean skipHasSwaRef;
    
    static {

        String skipSwaRef = System.getProperty("cxf.multipart.attachment");
        LOG.log(Level.FINE, "cxf.multipart.attachment property is set to " + skipSwaRef);

        if (skipSwaRef != null 
            && skipSwaRef.trim().length() > 0
            && skipSwaRef.trim().equalsIgnoreCase("false")) {
            skipHasSwaRef = true;
        } else {
            skipHasSwaRef = false;
        }

    }
    
    AttachmentOutInterceptor attachOut = new AttachmentOutInterceptor();
    
    public SwAOutInterceptor() {
        super(Phase.PRE_LOGICAL);
        addAfter(HolderOutInterceptor.class.getName());
        addBefore(WrapperClassOutInterceptor.class.getName());
    }
    
    private boolean callSWARefMethod(final JAXBContext ctx) {
        Method m = SWA_REF_METHOD.get(ctx.getClass().getName());
        if (m == null && !SWA_REF_METHOD.containsKey(ctx.getClass().getName())) {
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
                SWA_REF_METHOD.put(ctx.getClass().getName(), m);
            } catch (Exception e) {
                //ignore
            }
        }
        try {
            return (Boolean)m.invoke(ctx);
        } catch (Exception e) {
            return false;
        }
    }

    public void handleMessage(SoapMessage message) throws Fault {
        Exchange ex = message.getExchange();
        BindingOperationInfo bop = ex.getBindingOperationInfo();
        if (bop == null) {
            return;
        }
        
        if (bop.isUnwrapped()) {
            bop = bop.getWrappedOperation();
        }
        
        boolean client = isRequestor(message);
        BindingMessageInfo bmi = client ? bop.getInput() : bop.getOutput();
        
        if (bmi == null) {
            return;
        }
        
        Boolean newAttachment = false;
        Message exOutMsg = ex.getOutMessage();
        if (exOutMsg != null) {
            newAttachment = MessageUtils.isTrue(exOutMsg.getContextualProperty("cxf.add.attachments"));
            LOG.log(Level.FINE, "Request context attachment property: cxf.add.attachments is set to: " + newAttachment);
        }
        
        SoapBodyInfo sbi = bmi.getExtensor(SoapBodyInfo.class);
        
        if (sbi == null || sbi.getAttachments() == null || sbi.getAttachments().size() == 0) {
            Service s = ex.getService();
            DataBinding db = s.getDataBinding();
            if (db instanceof JAXBDataBinding
                && hasSwaRef((JAXBDataBinding) db)) {
                Boolean includeAttachs = false;
                Message exInpMsg = ex.getInMessage();
                LOG.log(Level.FINE, "Exchange Input message: " + exInpMsg);
                if (exInpMsg != null) {
                    includeAttachs = MessageUtils.isTrue(exInpMsg.getContextualProperty("cxf.add.attachments"));
                }   
                LOG.log(Level.FINE, "Add attachments message property: cxf.add.attachments value is " + includeAttachs);
                if (!skipHasSwaRef || includeAttachs || newAttachment) {
                    setupAttachmentOutput(message);
                } else {
                    skipAttachmentOutput(message);
                }                
            }
            return;
        }
        processAttachments(message, sbi);
    }
    
    protected void processAttachments(SoapMessage message, SoapBodyInfo sbi) {
        Collection<Attachment> atts = setupAttachmentOutput(message);
        List<Object> outObjects = CastUtils.cast(message.getContent(List.class));
        
        for (MessagePartInfo mpi : sbi.getAttachments()) {
            String partName = mpi.getConcreteName().getLocalPart();
            String ct = (String) mpi.getProperty(Message.CONTENT_TYPE);
            
            String id = new StringBuilder().append(partName)
                .append("=")
                .append(UUID.randomUUID())
                .append("@apache.org").toString();
            
            // this assumes things are in order...
            int idx = mpi.getIndex();
            Object o = outObjects.get(idx);
            
            if (o == null) {
                continue;
            }
            outObjects.set(idx, null);
            DataHandler dh = null;
            
            // This code could probably be refactored out somewhere...
            if (o instanceof Source) {
                dh = new DataHandler(createDataSource((Source)o, ct));
            } else if (o instanceof Image) {
                // TODO: make this streamable. This is one of my pet
                // peeves in JAXB RI as well, so if you fix this, submit the 
                // code to the JAXB RI as well (see RuntimeBuiltinLeafInfoImpl)! - DD
                ByteArrayOutputStream bos = new ByteArrayOutputStream(2048);
                Iterator<ImageWriter> writers = ImageIO.getImageWritersByMIMEType(ct);
                if (writers.hasNext()) {
                    ImageWriter writer = writers.next();
                    
                    try {
                        BufferedImage bimg = convertToBufferedImage((Image) o);
                        ImageOutputStream out = ImageIO.createImageOutputStream(bos); 
                        writer.setOutput(out);
                        writer.write(bimg);
                        writer.dispose();
                        out.flush();
                        out.close();
                        bos.close();
                    } catch (IOException e) {
                        throw new Fault(e);
                    }
                } else {
                    throw new Fault(new org.apache.cxf.common.i18n.Message("ATTACHMENT_NOT_SUPPORTED", 
                                     LOG, ct));                    
                }
                
                dh = new DataHandler(new ByteDataSource(bos.toByteArray(), ct));
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
            } else if (o instanceof byte[]) {
                if (ct == null) {
                    ct = "application/octet-stream";
                }
                dh = new DataHandler(new ByteDataSource((byte[])o, ct));                
            } else if (o instanceof String) {
                if (ct == null) {
                    ct = "text/plain; charset=\'UTF-8\'";
                }
                try {
                    dh = new DataHandler(new ByteDataSource(((String)o).getBytes("UTF-8"), ct));
                } catch (IOException e) {
                    throw new Fault(e);
                }                
            } else {
                throw new Fault(new org.apache.cxf.common.i18n.Message("ATTACHMENT_NOT_SUPPORTED", 
                                                                       LOG, o.getClass()));
            }
            
            AttachmentImpl att = new AttachmentImpl(id);
            att.setDataHandler(dh);
            att.setHeader("Content-Type", ct);
            atts.add(att);
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
                    ByteArrayOutputStream bos = new ByteArrayOutputStream(2048);
                    IOUtils.copy(src.getInputStream(), bos, 1024);
                    ds = new ByteDataSource(bos.toByteArray(), ct);
                } else {
                    ds = new ByteDataSource(IOUtils.toString(src.getReader()).getBytes("UTF-8"),
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
        if (atts == null) {
            atts = new ArrayList<Attachment>();
            message.setAttachments(atts);
        }
        return atts;
    }
    
    private Collection<Attachment> skipAttachmentOutput(SoapMessage message) {

        Collection<Attachment> atts = message.getAttachments();
        
        LOG.log(Level.FINE, "skipAttachmentOutput: getAttachments returned  " + atts);
        
        if (atts != null) {
            // We have attachments, so add the interceptor
            message.getInterceptorChain().add(attachOut);
            // We should probably come up with another property for this
            message.put(AttachmentOutInterceptor.WRITE_ATTACHMENTS, Boolean.TRUE);
        } else {    
            atts = new ArrayList<Attachment>();
            message.setAttachments(atts);
        }               
        
        return atts;
    }

}
