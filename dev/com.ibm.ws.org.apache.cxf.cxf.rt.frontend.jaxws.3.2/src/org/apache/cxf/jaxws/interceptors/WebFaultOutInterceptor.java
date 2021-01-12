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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.validation.Schema;
import javax.xml.ws.WebFault;
import javax.xml.ws.soap.SOAPFaultException;

import org.apache.cxf.annotations.SchemaValidation.SchemaValidationType;
import org.apache.cxf.binding.soap.SoapFault;
import org.apache.cxf.binding.soap.SoapVersion;
import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.common.i18n.BundleUtils;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.databinding.DataWriter;
import org.apache.cxf.helpers.ServiceUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.FaultOutInterceptor;
import org.apache.cxf.message.FaultMode;
import org.apache.cxf.message.Message;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.model.FaultInfo;
import org.apache.cxf.service.model.MessagePartInfo;
import org.apache.cxf.service.model.OperationInfo;
import org.apache.cxf.staxutils.W3CDOMStreamWriter;
import org.apache.cxf.ws.addressing.EndpointReferenceUtils;

public class WebFaultOutInterceptor extends FaultOutInterceptor {

    private static final Logger LOG = LogUtils.getL7dLogger(WebFaultOutInterceptor.class);
    private static final ResourceBundle BUNDLE = BundleUtils.getBundle(WebFaultOutInterceptor.class);

    public WebFaultOutInterceptor() {
        super();
    }

    private QName getFaultName(WebFault wf, Class<?> cls, OperationInfo op) {
        String ns = wf.targetNamespace();
        if (StringUtils.isEmpty(ns) && op != null) {
            ns = op.getName().getNamespaceURI();
        }
        String name = wf.name();
        if (StringUtils.isEmpty(name)) {
            name = cls.getSimpleName();
        }
        return new QName(ns, name);
    }


    private WebFault getWebFaultAnnotation(Class<?> t) {
        WebFault fault = t.getAnnotation(WebFault.class);
        if (fault == null
            && t.getSuperclass() != null
            && Throwable.class.isAssignableFrom(t.getSuperclass())) {
            fault = getWebFaultAnnotation(t.getSuperclass());
        }
        return fault;
    }

    public void handleMessage(Message message) throws Fault {
        Fault f = (Fault)message.getContent(Exception.class);
        if (f == null) {
            return;
        }
        try {
            Throwable thr = f.getCause();
            SOAPFaultException sf = null;
            if (thr instanceof SOAPFaultException) {
                sf = (SOAPFaultException)thr;
            } else if (thr.getCause() instanceof SOAPFaultException) {
                sf = (SOAPFaultException)thr.getCause();
            }
            if (sf != null) {
                SoapVersion soapVersion = (SoapVersion)message.get(SoapVersion.class.getName());
                if (soapVersion != null && soapVersion.getVersion() != 1.1) {
/*                  Liberty change: if block below is removed
                    if (f instanceof SoapFault) {
                        for (Iterator<QName> it = CastUtils.cast(sf.getFault().getFaultSubcodes()); it.hasNext();) {
                            ((SoapFault) f).addSubCode(it.next());
                        }
                    } Liberty change: end */
                    // Liberty change: if block below is added
                    if (sf.getFault().getFaultSubcodes().hasNext()
                            && f.getClass().equals(SoapFault.class)) {
                        String subcode = sf.getFault().getFaultSubcodes().next()
                               .toString();
                        String nameSpace = subcode.substring(
                               subcode.indexOf("{") + 1, subcode.indexOf("}"));
                        String localPart = subcode
                               .substring(subcode.indexOf("}") + 1);
                        QName subcodeQName = new QName(nameSpace, localPart);
                        ((SoapFault) f).setSubCode(subcodeQName);
                    } // Liberty change: end
                    if (sf.getFault().getFaultReasonLocales().hasNext()) {
                        Locale lang = (Locale) sf.getFault()
                                .getFaultReasonLocales().next();
                        String convertedLang = lang.getLanguage();
                        String country = lang.getCountry();
                        if (country.length() > 0) {
                            convertedLang = convertedLang + '-' + country;
                        }
                        f.setLang(convertedLang);
                    }
                }
                message.setContent(Exception.class, f);
            }
        } catch (Exception e) {
          // do nothing;
        }
        Throwable cause = f.getCause();
        WebFault fault = null;
        if (cause != null) {
            fault = getWebFaultAnnotation(cause.getClass());
            if (fault == null && cause.getCause() != null) {
                fault = getWebFaultAnnotation(cause.getCause().getClass());
                if (fault != null || cause instanceof RuntimeException) {
                    cause = cause.getCause();
                }
            }
        }
        if (cause instanceof Exception && fault != null) {
            Exception ex = (Exception)cause;
            Object faultInfo = null;
            try {
                Method method = cause.getClass().getMethod("getFaultInfo", new Class[0]);
                faultInfo = method.invoke(cause, new Object[0]);
            } catch (NoSuchMethodException e) {
                faultInfo = createFaultInfoBean(fault, cause);

            } catch (InvocationTargetException e) {
                throw new Fault(new org.apache.cxf.common.i18n.Message("INVOCATION_TARGET_EXC", BUNDLE), e);
            } catch (IllegalAccessException | IllegalArgumentException e) {
                throw new Fault(new org.apache.cxf.common.i18n.Message("COULD_NOT_INVOKE", BUNDLE), e);
            }
            Service service = message.getExchange().getService();

            try {
                DataWriter<XMLStreamWriter> writer
                    = service.getDataBinding().createWriter(XMLStreamWriter.class);

                if (ServiceUtils.isSchemaValidationEnabled(SchemaValidationType.OUT, message)) {
                    Schema schema = EndpointReferenceUtils.getSchema(service.getServiceInfos().get(0),
                                                                     message.getExchange().getBus());
                    writer.setSchema(schema);
                }

                OperationInfo op = null;
                // Prevent a NPE if we can't match the operation
                if (message.getExchange().getBindingOperationInfo() != null) {
                    op = message.getExchange().getBindingOperationInfo().getOperationInfo();
                }
                QName faultName = getFaultName(fault, cause.getClass(), op);
                MessagePartInfo part = op != null ? getFaultMessagePart(faultName, op) : null;

                if (f.hasDetails()) {
                    writer.write(faultInfo, part, new W3CDOMStreamWriter(f.getDetail()));
                } else {
                    writer.write(faultInfo, part, new W3CDOMStreamWriter(f.getOrCreateDetail()));
                    if (!f.getDetail().hasChildNodes()) {
                        f.setDetail(null);
                    }
                }

                f.setMessage(ex.getMessage());
            } catch (Fault f2) {
                message.setContent(Exception.class, f2);
                super.handleMessage(message);
            } catch (Exception nex) {
                //if exception occurs while writing a fault, we'll just let things continue
                //and let the rest of the chain try handling it as is.
                LOG.log(Level.WARNING, "EXCEPTION_WHILE_WRITING_FAULT", nex);
            }
        } else if (cause instanceof SOAPFaultException && ((SOAPFaultException)cause).getFault().hasDetail()) {
            return;
        } else {
            FaultMode mode = message.get(FaultMode.class);
            if (mode == FaultMode.CHECKED_APPLICATION_FAULT) {
                //only convert checked exceptions with this
                //otherwise delegate down to the normal protocol specific stuff
                super.handleMessage(message);
            }
        }
    }

    private Object createFaultInfoBean(WebFault fault, Throwable cause) {
        if (!StringUtils.isEmpty(fault.faultBean())) {
            try {
                Class<?> cls = ClassLoaderUtils.loadClass(fault.faultBean(),
                                                          cause.getClass());
                if (cls != null) {
                    Object ret = cls.newInstance();
                    //copy props
                    Method[] meth = cause.getClass().getMethods();
                    for (Method m : meth) {
                        if (m.getParameterTypes().length == 0
                            && (m.getName().startsWith("get")
                            || m.getName().startsWith("is"))) {
                            try {
                                String name;
                                if (m.getName().startsWith("get")) {
                                    name = "set" + m.getName().substring(3);
                                } else {
                                    name = "set" + m.getName().substring(2);
                                }
                                Method m2 = cls.getMethod(name, m.getReturnType());
                                m2.invoke(ret, m.invoke(cause));
                            } catch (Exception e) {
                                //ignore
                            }
                        }
                    }
                    return ret;
                }
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e1) {
                //ignore
            }
        }

        LOG.fine("Using @WebFault annotated class "
                 + cause.getClass().getName()
                 + " as faultInfo since getFaultInfo() was not found");
        return cause;
    }

    private MessagePartInfo getFaultMessagePart(QName qname, OperationInfo op) {
        for (FaultInfo faultInfo : op.getFaults()) {
            for (MessagePartInfo mpi : faultInfo.getMessageParts()) {
                String ns = null;
                if (mpi.isElement()) {
                    ns = mpi.getElementQName().getNamespaceURI();
                } else {
                    ns = mpi.getTypeQName().getNamespaceURI();
                }
                if (qname.getLocalPart().equals(mpi.getConcreteName().getLocalPart())
                        && qname.getNamespaceURI().equals(ns)) {
                    return mpi;
                }
            }

        }
        return null;
    }

}
