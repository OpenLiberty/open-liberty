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

package org.apache.cxf.jaxb.io;


import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.PropertyException;
import javax.xml.bind.ValidationEvent;
import javax.xml.bind.ValidationEventHandler;
import javax.xml.bind.attachment.AttachmentMarshaller;

import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.jaxb.JAXBUtils;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.databinding.DataWriter;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.jaxb.JAXBDataBase;
import org.apache.cxf.jaxb.JAXBDataBinding;
import org.apache.cxf.jaxb.JAXBEncoderDecoder;
import org.apache.cxf.jaxb.attachment.JAXBAttachmentMarshaller;
import org.apache.cxf.service.model.MessagePartInfo;
import org.apache.ws.commons.schema.XmlSchemaElement;

public class DataWriterImpl<T> extends JAXBDataBase implements DataWriter<T> {
    private static final Logger LOG = LogUtils.getLogger(JAXBDataBinding.class);

    private final JAXBDataBinding databinding;
    
    public DataWriterImpl(JAXBDataBinding binding) {
        super(binding.getContext());
        databinding = binding;
    }
    
    public void write(Object obj, T output) {
        write(obj, null, output);
    }
    private static class MtomValidationHandler implements ValidationEventHandler {
        ValidationEventHandler origHandler;
        JAXBAttachmentMarshaller marshaller;
        public MtomValidationHandler(ValidationEventHandler v,
                                     JAXBAttachmentMarshaller m) {
            origHandler = v;
            marshaller = m;
        }
        
        public boolean handleEvent(ValidationEvent event) {
            String msg = event.getMessage();
            if (msg.startsWith("cvc-type.3.1.2: ")
                && msg.contains(marshaller.getLastMTOMElementName().getLocalPart())) {
                return true;
            }
            if (origHandler != null) {
                return origHandler.handleEvent(event);
            }
            return false;
        }
        
    }
    public Marshaller createMarshaller(Object elValue, MessagePartInfo part) {
        Class<?> cls = null;
        if (part != null) {
            cls = part.getTypeClass();
        }

        if (cls == null) {
            cls = null != elValue ? elValue.getClass() : null;
        }

        if (cls != null && cls.isArray() && elValue instanceof Collection) {
            Collection<?> col = (Collection<?>)elValue;
            elValue = col.toArray((Object[])Array.newInstance(cls.getComponentType(), col.size()));
        }
        Marshaller marshaller;
        try {
            
            marshaller = databinding.getJAXBMarshaller();

            //If the marshaller has already been filled with all the initializing properties
            //and attributes, we don't have to set again.
            if (Boolean.FALSE.equals(marshaller.getProperty(Marshaller.JAXB_FRAGMENT))
                && marshaller.getAttachmentMarshaller() == null) {
                marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
                marshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
                marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.FALSE);

                marshaller.setListener(databinding.getMarshallerListener());
                if (databinding.getValidationEventHandler() != null) {
                    marshaller.setEventHandler(databinding.getValidationEventHandler());
                }

                final Map<String, String> nspref = databinding.getDeclaredNamespaceMappings();
                if (nspref != null) {
                    JAXBUtils.setNamespaceWrapper(nspref, marshaller);
                }
                if (databinding.getMarshallerProperties() != null) {
                    for (Map.Entry<String, Object> propEntry : databinding.getMarshallerProperties().entrySet()) {
                        try {
                            marshaller.setProperty(propEntry.getKey(), propEntry.getValue());
                        } catch (PropertyException pe) {
                            LOG.log(Level.INFO, "PropertyException setting Marshaller properties", pe);
                        }
                    }
                }
            }
            
            marshaller.setSchema(schema);
            AttachmentMarshaller atmarsh = getAttachmentMarshaller();
            marshaller.setAttachmentMarshaller(atmarsh);
            
            if (schema != null
                && atmarsh instanceof JAXBAttachmentMarshaller) {
                //we need a special even handler for XOP attachments 
                marshaller.setEventHandler(new MtomValidationHandler(marshaller.getEventHandler(),
                                                            (JAXBAttachmentMarshaller)atmarsh));
            }
        } catch (JAXBException ex) {
            if (ex instanceof javax.xml.bind.MarshalException) {
                javax.xml.bind.MarshalException marshalEx = (javax.xml.bind.MarshalException)ex;
                Message faultMessage = new Message("MARSHAL_ERROR", LOG, marshalEx.getLinkedException()
                    .getMessage());
                throw new Fault(faultMessage, ex);
            } else {
                throw new Fault(new Message("MARSHAL_ERROR", LOG, ex.getMessage()), ex);
            }
        }
        return marshaller;
    }
    
    public void write(Object obj, MessagePartInfo part, T output) {
        boolean honorJaxbAnnotation = honorJAXBAnnotations(part);
        if (part != null && !part.isElement() && part.getTypeClass() != null) {
            honorJaxbAnnotation = true;
        }

        Marshaller marshaller = null;
        if (obj != null
            || !(part.getXmlSchema() instanceof XmlSchemaElement)) {
            
            if (obj instanceof Exception 
                && part != null
                && Boolean.TRUE.equals(part.getProperty(JAXBDataBinding.class.getName() 
                                                        + ".CUSTOM_EXCEPTION"))) {
                marshaller = createMarshaller(obj, part);
                JAXBEncoderDecoder.marshallException(marshaller,
                                                     (Exception) obj,
                                                     part,
                                                     output);               
            } else {
                Annotation[] anns = getJAXBAnnotation(part);
                if (!honorJaxbAnnotation || anns.length == 0) {
                    marshaller = createMarshaller(obj, part);
                    JAXBEncoderDecoder.marshall(marshaller, obj, part, output);
                } else if (honorJaxbAnnotation && anns.length > 0) {
                    //RpcLit will use the JAXB Bridge to marshall part message when it is 
                    //annotated with @XmlList,@XmlAttachmentRef,@XmlJavaTypeAdapter
                    //TODO:Cache the JAXBRIContext
                    
                    JAXBEncoderDecoder.marshalWithBridge(part.getConcreteName(),
                                                         part.getTypeClass(),
                                                         anns, 
                                                         databinding.getContextClasses(), 
                                                         obj, 
                                                         output, 
                                                         getAttachmentMarshaller());
                }
            }
        } else if (needToRender(obj, part)) {
            marshaller = createMarshaller(obj, part);
            JAXBEncoderDecoder.marshallNullElement(marshaller, output, part);
        }
        databinding.releaseJAXBMarshaller(marshaller);
    }

    private boolean needToRender(Object obj, MessagePartInfo part) {
        if (part != null && part.getXmlSchema() instanceof XmlSchemaElement) {
            XmlSchemaElement element = (XmlSchemaElement)part.getXmlSchema();
            return element.isNillable() && element.getMinOccurs() > 0;
        }
        return false;
    }
    
}
