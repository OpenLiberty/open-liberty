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
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.bind.JAXBException;
import javax.xml.bind.MarshalException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.PropertyException;
import javax.xml.bind.ValidationEvent;
import javax.xml.bind.ValidationEventHandler;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.attachment.AttachmentMarshaller;

import org.apache.cxf.Bus;
import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.jaxb.JAXBUtils;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.ReflectionUtil;
import org.apache.cxf.databinding.DataWriter;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.jaxb.JAXBDataBase;
import org.apache.cxf.jaxb.JAXBDataBinding;
import org.apache.cxf.jaxb.JAXBEncoderDecoder;
import org.apache.cxf.jaxb.MarshallerEventHandler;
import org.apache.cxf.jaxb.attachment.JAXBAttachmentMarshaller;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.service.model.MessagePartInfo;
import org.apache.ws.commons.schema.XmlSchemaElement;

public class DataWriterImpl<T> extends JAXBDataBase implements DataWriter<T> {
    private static final Logger LOG = LogUtils.getLogger(JAXBDataBinding.class);

    ValidationEventHandler veventHandler;
    boolean setEventHandler = true;
    boolean noEscape;
    private JAXBDataBinding databinding;
    private Bus bus;

    public DataWriterImpl(Bus bus, JAXBDataBinding binding) {
        this(bus, binding, false);
    }
    public DataWriterImpl(Bus bus, JAXBDataBinding binding, boolean noEsc) {
        super(binding.getContext());
        databinding = binding;
        noEscape = noEsc;
        this.bus = bus;
    }

    public void write(Object obj, T output) {
        write(obj, null, output);
    }

    public void setProperty(String prop, Object value) {
        if (prop.equals(org.apache.cxf.message.Message.class.getName())) {
            org.apache.cxf.message.Message m = (org.apache.cxf.message.Message)value;
            veventHandler = getValidationEventHandler(m, JAXBDataBinding.WRITER_VALIDATION_EVENT_HANDLER);
            if (veventHandler == null) {
                veventHandler = databinding.getValidationEventHandler();
            }
            setEventHandler = MessageUtils.getContextualBoolean(m,
                    JAXBDataBinding.SET_VALIDATION_EVENT_HANDLER, true);
        }
    }

    private static class MtomValidationHandler implements ValidationEventHandler {
        ValidationEventHandler origHandler;
        JAXBAttachmentMarshaller marshaller;
        MtomValidationHandler(ValidationEventHandler v,
                                     JAXBAttachmentMarshaller m) {
            origHandler = v;
            marshaller = m;
        }

        public boolean handleEvent(ValidationEvent event) {
            // CXF-1194/CXF-7438 this hack is specific to MTOM, so pretty safe to leave in
            // here before calling the origHandler.
            String msg = event.getMessage();
            if ((msg.startsWith("cvc-type.3.1.2") || msg.startsWith("cvc-complex-type.2.2"))
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

            marshaller = context.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_ENCODING, StandardCharsets.UTF_8.name());
            marshaller.setProperty(Marshaller.JAXB_FRAGMENT, true);
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.FALSE);
            marshaller.setListener(databinding.getMarshallerListener());
            databinding.applyEscapeHandler(!noEscape, eh -> JAXBUtils.setEscapeHandler(marshaller, eh));

            if (setEventHandler) {
                ValidationEventHandler h = veventHandler;
                if (veventHandler == null) {
                    h = new ValidationEventHandler() {
                        public boolean handleEvent(ValidationEvent event) {
                            //continue on warnings only
                            return event.getSeverity() == ValidationEvent.WARNING;
                        }
                    };
                }
                marshaller.setEventHandler(h);
            }

            final Map<String, String> nspref = databinding.getDeclaredNamespaceMappings();
            final Map<String, String> nsctxt = databinding.getContextualNamespaceMap();
            // set the prefix mapper if either of the prefix map is configured
            if (nspref != null || nsctxt != null) {
                Object mapper = JAXBUtils.setNamespaceMapper(bus, nspref != null ? nspref : nsctxt, marshaller);
                if (nsctxt != null) {
                    setContextualNamespaceDecls(mapper, nsctxt);
                }
            }
            if (databinding.getMarshallerProperties() != null) {
                for (Map.Entry<String, Object> propEntry
                    : databinding.getMarshallerProperties().entrySet()) {
                    try {
                        marshaller.setProperty(propEntry.getKey(), propEntry.getValue());
                    } catch (PropertyException pe) {
                        LOG.log(Level.INFO, "PropertyException setting Marshaller properties", pe);
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
        } catch (javax.xml.bind.MarshalException ex) {
            Message faultMessage = new Message("MARSHAL_ERROR", LOG, ex.getLinkedException()
                .getMessage());
            throw new Fault(faultMessage, ex);
        } catch (JAXBException ex) {
            throw new Fault(new Message("MARSHAL_ERROR", LOG, ex.getMessage()), ex);
        }
        for (XmlAdapter<?, ?> adapter : databinding.getConfiguredXmlAdapters()) {
            marshaller.setAdapter(adapter);
        }
        return marshaller;
    }

    //REVISIT should this go into JAXBUtils?
    private static void setContextualNamespaceDecls(Object mapper, Map<String, String> nsctxt) {
        try {
            Method m = ReflectionUtil.getDeclaredMethod(mapper.getClass(),
                                                        "setContextualNamespaceDecls", new Class<?>[]{String[].class});
            String[] args = new String[nsctxt.size() * 2];
            int ai = 0;
            for (Entry<String, String> nsp : nsctxt.entrySet()) {
                args[ai++] = nsp.getValue();
                args[ai++] = nsp.getKey();
            }
            m.invoke(mapper, new Object[]{args});
        } catch (Exception e) {
            // ignore
            LOG.log(Level.WARNING, "Failed to set the contextual namespace map", e);
        }

    }

    public void write(Object obj, MessagePartInfo part, T output) {
        boolean honorJaxbAnnotation = honorJAXBAnnotations(part);
        if (part != null && !part.isElement() && part.getTypeClass() != null) {
            honorJaxbAnnotation = true;
        }
        checkPart(part, obj);

        if (obj != null
            || !(part.getXmlSchema() instanceof XmlSchemaElement)) {

            if (obj instanceof Exception
                && part != null
                && Boolean.TRUE.equals(part.getProperty(JAXBDataBinding.class.getName()
                                                        + ".CUSTOM_EXCEPTION"))) {
                JAXBEncoderDecoder.marshallException(createMarshaller(obj, part),
                                                     (Exception)obj,
                                                     part,
                                                     output);
                onCompleteMarshalling();
            } else {
                Annotation[] anns = getJAXBAnnotation(part);
                if (!honorJaxbAnnotation || anns.length == 0) {
                    JAXBEncoderDecoder.marshall(createMarshaller(obj, part), obj, part, output);
                    onCompleteMarshalling();
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
        } else if (needToRender(part)) {
            JAXBEncoderDecoder.marshallNullElement(createMarshaller(null, part),
                                                   output, part);

            onCompleteMarshalling();
        }
    }

    private void checkPart(MessagePartInfo part, Object object) {
        if (part == null || part.getTypeClass() == null || object == null) {
            return;
        }
        Class<?> typeClass = part.getTypeClass();
        if (typeClass == null) {
            return;
        }
        if (typeClass.isPrimitive()) {
            if (typeClass == Long.TYPE) {
                typeClass = Long.class;
            } else if (typeClass == Integer.TYPE) {
                typeClass = Integer.class;
            } else if (typeClass == Short.TYPE) {
                typeClass = Short.class;
            } else if (typeClass == Byte.TYPE) {
                typeClass = Byte.class;
            } else if (typeClass == Character.TYPE) {
                typeClass = Character.class;
            } else if (typeClass == Double.TYPE) {
                typeClass = Double.class;
            } else if (typeClass == Float.TYPE) {
                typeClass = Float.class;
            } else if (typeClass == Boolean.TYPE) {
                typeClass = Boolean.class;
            }
        } else if (typeClass.isArray() && object instanceof Collection) {
            //JAXB allows a pseudo [] <--> List equivalence
            return;
        }
        if (!typeClass.isInstance(object)) {
            throw new IllegalArgumentException("Part " + part.getName() + " should be of type "
                + typeClass.getName() + ", not "
                + object.getClass().getName());
        }
    }

    private boolean needToRender(MessagePartInfo part) {
        if (part != null && part.getXmlSchema() instanceof XmlSchemaElement) {
            XmlSchemaElement element = (XmlSchemaElement)part.getXmlSchema();
            return element.isNillable() && element.getMinOccurs() > 0;
        }
        return false;
    }

    private void onCompleteMarshalling() {
        if (setEventHandler && veventHandler instanceof MarshallerEventHandler) {
            try {
                ((MarshallerEventHandler) veventHandler).onMarshalComplete();
            } catch (MarshalException e) {
                if (e.getLinkedException() != null) {
                    throw new Fault(new Message("MARSHAL_ERROR", LOG,
                            e.getLinkedException().getMessage()), e);
                }
                throw new Fault(new Message("MARSHAL_ERROR", LOG, e.getMessage()), e);
            }
        }
    }
}
