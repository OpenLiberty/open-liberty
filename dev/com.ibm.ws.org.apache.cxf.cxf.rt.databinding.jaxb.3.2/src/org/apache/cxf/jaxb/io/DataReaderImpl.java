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
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.bind.JAXBException;
import javax.xml.bind.PropertyException;
import javax.xml.bind.UnmarshalException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.ValidationEvent;
import javax.xml.bind.ValidationEventHandler;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.namespace.QName;

import org.apache.cxf.attachment.AttachmentUtil;
import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.jaxb.JAXBUtils;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.PropertyUtils;
import org.apache.cxf.databinding.DataReader;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.jaxb.JAXBDataBase;
import org.apache.cxf.jaxb.JAXBDataBinding;
import org.apache.cxf.jaxb.JAXBEncoderDecoder;
import org.apache.cxf.jaxb.UnmarshallerEventHandler;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.service.model.MessagePartInfo;

public class DataReaderImpl<T> extends JAXBDataBase implements DataReader<T> {
    private static final Logger LOG = LogUtils.getLogger(DataReaderImpl.class);  // Liberty change
    JAXBDataBinding databinding;
    boolean unwrapJAXBElement;
    ValidationEventHandler veventHandler;
    boolean setEventHandler = true;

    public DataReaderImpl(JAXBDataBinding binding, boolean unwrap) {
        super(binding.getContext());
        unwrapJAXBElement = unwrap;
        databinding = binding;
    }

    public Object read(T input) {
        return read(null, input);
    }

    private static class WSUIDValidationHandler implements ValidationEventHandler {
        ValidationEventHandler origHandler;
        WSUIDValidationHandler(ValidationEventHandler o) {
            origHandler = o;
        }

        public boolean handleEvent(ValidationEvent event) {
            // if the original handler has already handled the event, no need for us
            // to do anything, otherwise if not yet handled, then do this 'hack'
            if (origHandler != null && origHandler.handleEvent(event)) {
                return true;
            }
            // hack for CXF-3453
            String msg = event.getMessage();
	    // Liberty change begin
	    if (LOG.isLoggable(Level.FINEST)) {  
		LOG.finest("ValidationEvent message: " + msg);
	    } 
	    // Liberty change end
            return msg != null
                && msg.contains(":Id")
                && (msg.startsWith("cvc-type.3.1.1")
                    || msg.startsWith("cvc-type.3.2.2")
                    || msg.startsWith("cvc-complex-type.3.1.1")
                    || msg.startsWith("cvc-complex-type.3.2.2"));
        }
    }

    public void setProperty(String prop, Object value) {
        boolean isLoggableFinest = LOG.isLoggable(Level.FINEST);  // Liberty change
        if (prop.equals(JAXBDataBinding.UNWRAP_JAXB_ELEMENT)) {
            unwrapJAXBElement = Boolean.TRUE.equals(value);
	    // Liberty change begin
	    if (isLoggableFinest) {  
            	LOG.finest("UnwrapJAXBElement is set to: " + unwrapJAXBElement + " trough property"); // Liberty change
	    } 
	    // Liberty change end
        } else if (prop.equals(org.apache.cxf.message.Message.class.getName())) {
            org.apache.cxf.message.Message m = (org.apache.cxf.message.Message)value;
            veventHandler = getValidationEventHandler(m, JAXBDataBinding.READER_VALIDATION_EVENT_HANDLER);
            if (veventHandler == null) {
                veventHandler = databinding.getValidationEventHandler();
            }
             // Liberty change begin
            setEventHandler = AttachmentUtil.mtomOverride(m, MessageUtils.getContextualBoolean(m, JAXBDataBinding.SET_VALIDATION_EVENT_HANDLER, true));
            if (isLoggableFinest) {  
                LOG.finest("SetEventHandler is set to: " + setEventHandler); // Liberty change
            } 
            // Liberty change end
            Object unwrapProperty = m.get(JAXBDataBinding.UNWRAP_JAXB_ELEMENT);
            if (unwrapProperty == null) {
                unwrapProperty = m.getExchange().get(JAXBDataBinding.UNWRAP_JAXB_ELEMENT);
            }
            if (unwrapProperty != null) {
                unwrapJAXBElement = Boolean.TRUE.equals(unwrapProperty);
		// Liberty change begin
	        if (isLoggableFinest) {  
                   LOG.finest("UnwrapJAXBElement is set to: " + unwrapJAXBElement); // Liberty change
	        } // Liberty change end
            }
        }
    }

    private Unmarshaller createUnmarshaller() {
        try {
            // Liberty change begin
            // Move the logic that is 
            Unmarshaller um = databinding.getJAXBUnmarshaller(setEventHandler, setEventHandler ? new WSUIDValidationHandler(veventHandler) : null);
            /*if (databinding.getUnmarshallerListener() != null) {
                um.setListener(databinding.getUnmarshallerListener());
            }
            if (setEventHandler) {
                um.setEventHandler(new WSUIDValidationHandler(veventHandler));
            }
            if (databinding.getUnmarshallerProperties() != null) {
                for (Map.Entry<String, Object> propEntry
                    : databinding.getUnmarshallerProperties().entrySet()) {
                    try {
                        um.setProperty(propEntry.getKey(), propEntry.getValue());
                    } catch (PropertyException pe) {
                        LOG.log(Level.INFO, "PropertyException setting Marshaller properties", pe);
                    }
                }
            }*/
            // Liberty change end
            um.setSchema(schema);
            um.setAttachmentUnmarshaller(getAttachmentUnmarshaller());
            for (XmlAdapter<?, ?> adapter : databinding.getConfiguredXmlAdapters()) {
                um.setAdapter(adapter);
            }
            return um;
        } catch (javax.xml.bind.UnmarshalException ex) {
            throw new Fault(new Message("UNMARSHAL_ERROR", LOG, ex.getLinkedException()
                .getMessage()), ex);
        } catch (JAXBException ex) {
            throw new Fault(new Message("UNMARSHAL_ERROR", LOG, ex.getMessage()), ex);
        }
    }

    public Object read(MessagePartInfo part, T reader) {
        boolean honorJaxbAnnotation = honorJAXBAnnotations(part);
        if (honorJaxbAnnotation) {
            Annotation[] anns = getJAXBAnnotation(part);
            if (anns.length > 0) {
                // RpcLit will use the JAXB Bridge to unmarshall part message when it is
                // annotated with @XmlList,@XmlAttachmentRef,@XmlJavaTypeAdapter
                // TODO:Cache the JAXBRIContext
                QName qname = new QName(null, part.getConcreteName().getLocalPart());

                Object obj = JAXBEncoderDecoder.unmarshalWithBridge(qname,
                                                              part.getTypeClass(),
                                                              anns,
                                                              databinding.getContextClasses(),
                                                              reader,
                                                              getAttachmentUnmarshaller());

                onCompleteUnmarshalling();

                return obj;
            }
        }

        Unmarshaller um = createUnmarshaller();
        boolean noError = false; // Liberty change
        try {
            Object obj = JAXBEncoderDecoder.unmarshall(um, reader, part,
                                                 unwrapJAXBElement);
            onCompleteUnmarshalling();
            noError = true; // Liberty change

            return obj;
        } finally {
            // Liberty change begin
            if (noError) {
                databinding.releaseJAXBUnmarshaller(um);
            } else {
                JAXBUtils.closeUnmarshaller(um);
            }
            // Liberty change end
        }
    }

    public Object read(QName name, T input, Class<?> type) {
        Unmarshaller um = createUnmarshaller();

        boolean noError = false; // Liberty change
        try {
            Object obj = JAXBEncoderDecoder.unmarshall(um, input,
                                             name, type,
                                             unwrapJAXBElement);
            onCompleteUnmarshalling();
            noError = true; // Liberty change

            return obj;
        } finally {
            // Liberty change begin
            if (noError) {
                databinding.releaseJAXBUnmarshaller(um);
            } else {
                JAXBUtils.closeUnmarshaller(um);
            }
            // Liberty change end
        }

    }

    private void onCompleteUnmarshalling() {
        if (setEventHandler && veventHandler instanceof UnmarshallerEventHandler) {
            try {
                ((UnmarshallerEventHandler) veventHandler).onUnmarshalComplete();
            } catch (UnmarshalException e) {
                if (e.getLinkedException() != null) {
                    throw new Fault(new Message("UNMARSHAL_ERROR", LOG,
                            e.getLinkedException().getMessage()), e);
                }
                throw new Fault(new Message("UNMARSHAL_ERROR", LOG, e.getMessage()), e);
            }
        }
    }
}
