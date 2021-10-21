/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.org.jboss.resteasy.common.cdi;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Type;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.MessageBodyReader;

import org.jboss.resteasy.core.InjectorFactoryImpl;
import org.jboss.resteasy.plugins.providers.multipart.IAttachmentImpl;
import org.jboss.resteasy.plugins.providers.multipart.IBMMultipartProvider;
import org.jboss.resteasy.spi.ConstructorInjector;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.HttpResponse;
import org.jboss.resteasy.spi.InjectorFactory;
import org.jboss.resteasy.spi.MethodInjector;
import org.jboss.resteasy.spi.PropertyInjector;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.jboss.resteasy.spi.ValueInjector;
import org.jboss.resteasy.spi.metadata.Parameter;
import org.jboss.resteasy.spi.metadata.Parameter.ParamType;
import org.jboss.resteasy.spi.metadata.ResourceClass;
import org.jboss.resteasy.spi.metadata.ResourceConstructor;
import org.jboss.resteasy.spi.metadata.ResourceLocator;

import com.ibm.websphere.jaxrs20.multipart.IAttachment;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

@SuppressWarnings("rawtypes")
public class LibertyFallbackInjectorFactory implements InjectorFactory {
    private static final TraceComponent tc = Tr.register(LibertyFallbackInjectorFactory.class);

    private final InjectorFactory delegate;
    
    public LibertyFallbackInjectorFactory() {
        InjectorFactory factory;
        try {
            factory = new LibertyCdiInjectorFactory();
        } catch (Throwable t) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Caught exception initializing LibertyCdiInjectorFactory - expected if module declines CDI", t);
            }
            factory = new InjectorFactoryImpl();
        }
        delegate = factory;
    }
    @Override
    public ConstructorInjector createConstructor(Constructor constructor, ResteasyProviderFactory factory) {
        return delegate.createConstructor(constructor, factory);
    }

    @Override
    public PropertyInjector createPropertyInjector(Class resourceClass, ResteasyProviderFactory factory) {
        return delegate.createPropertyInjector(resourceClass, factory);
    }

    @Override
    public ValueInjector createParameterExtractor(Class injectTargetClass, AccessibleObject injectTarget, String defaultName, Class type, Type genericType, Annotation[] annotations, ResteasyProviderFactory factory) {
        return delegate.createParameterExtractor(injectTargetClass, injectTarget, defaultName, type, genericType, annotations, factory);
    }

    @Override
    public ValueInjector createParameterExtractor(Class injectTargetClass, AccessibleObject injectTarget, String defaultName, Class type, Type genericType, Annotation[] annotations, boolean useDefault, ResteasyProviderFactory factory) {
        return delegate.createParameterExtractor(injectTargetClass, injectTarget, defaultName, type, genericType, annotations, useDefault, factory);
    }
    
    @Override
    public ValueInjector createParameterExtractor(Parameter parameter, ResteasyProviderFactory providerFactory) {
        if (ParamType.FORM_PARAM.equals(parameter.getParamType()) && IAttachment.class.equals(parameter.getType())) {
            Type type = new GenericType<List<IAttachment>>() {}.getType();
            MessageBodyReader<Object> mbr = providerFactory.getMessageBodyReader((Class)List.class, type, null, MediaType.MULTIPART_FORM_DATA_TYPE);
            
            return new ValueInjector() {
                @Override
                public Object inject(boolean unwrapAsync) {
                    return null;
                }

                @SuppressWarnings("unchecked")
                @Override
                public Object inject(HttpRequest request, HttpResponse response, boolean unwrapAsync) {
                    List<IAttachment> atts;
                    try {
                        atts = (List<IAttachment>) request.getAttribute("io.openliberty.org.jboss.resteasy.common.cdi.LibertyFallbackInjectorFactory.attachmentList");
                        if (atts == null) {
                            atts = (List) mbr.readFrom((Class)List.class, type, null, MediaType.valueOf(request.getMutableHeaders().getFirst("Content-Type")),
                                                       request.getMutableHeaders(), request.getInputStream());
                            request.setAttribute("io.openliberty.org.jboss.resteasy.common.cdi.LibertyFallbackInjectorFactory.attachmentList", atts);
                        }
                    } catch (IOException ex) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "Caught IOException while reading multipart form params", ex);
                        }
                        return null;
                    }
                    for (IAttachment att : atts) {
                        if (((IAttachmentImpl)att).getFieldName().equals(parameter.getParamName())) {
                            return att;
                        }
                    }
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Request did not contain expected multipart parameter, " + parameter.getParamName()
                            + " request parameters included: " + atts.stream().map(a -> a.getHeader("Content-Disposition"))
                                                                              .map(IAttachmentImpl::getFieldNameFromHeader)
                                                                              .collect(Collectors.joining(",")));
                    }
                    return null;
                }
            };
        }
        ValueInjector injector = delegate.createParameterExtractor(parameter, providerFactory);
        return injector;
    }

    @Override
    public MethodInjector createMethodInjector(ResourceLocator method, ResteasyProviderFactory factory) {
        return delegate.createMethodInjector(method, factory);
    }

    @Override
    public PropertyInjector createPropertyInjector(ResourceClass resourceClass, ResteasyProviderFactory providerFactory) {
        return delegate.createPropertyInjector(resourceClass, providerFactory);
    }

    @Override
    public ConstructorInjector createConstructor(ResourceConstructor constructor, ResteasyProviderFactory providerFactory) {
        return delegate.createConstructor(constructor, providerFactory);
    }
}
