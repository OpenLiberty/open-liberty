/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.microprofile.opentracing.jaeger;

import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.List;

import com.ibm.ws.microprofile.opentracing.jaeger.adapter.AppLibraryClassLoader;
import com.ibm.ws.microprofile.opentracing.jaeger.adapter.Configuration;
import com.ibm.ws.microprofile.opentracing.jaeger.adapter.JaegerAdapter;
import com.ibm.ws.microprofile.opentracing.jaeger.adapter.JaegerAdapterException;
import com.ibm.ws.microprofile.opentracing.jaeger.adapter.JaegerAdapterFactory;
import com.ibm.ws.microprofile.opentracing.jaeger.adapter.JaegerTracer;
import com.ibm.ws.microprofile.opentracing.jaeger.adapter.impl.AbstractJaegerAdapter;

public class AdapterFactoryImpl extends JaegerAdapterFactory {

    private ClassLoader appLibLoader;

    public AdapterFactoryImpl() {
        super();
        this.appLibLoader = AccessController.doPrivileged((PrivilegedAction<ClassLoader>) () -> {
            List<Class<?>> interfaces = Arrays.asList(
                    JaegerAdapter.class,
                    JaegerAdapterFactory.class,
                    JaegerAdapterException.class,
                    Configuration.class,
                    Configuration.SenderConfiguration.class,
                    Configuration.ReporterConfiguration.class,
                    Configuration.SamplerConfiguration.class,
                    Configuration.Propagation.class,
                    Configuration.CodecConfiguration.class,
                    JaegerTracer.class,
                    JaegerTracer.Builder.class
            );
            URL[] urls = new URL[] { AbstractJaegerAdapter.class.getProtectionDomain().getCodeSource().getLocation() };// URL for the adapter impl bundle

            return new AppLibraryClassLoader(urls, interfaces, Thread.currentThread().getContextClassLoader());
        });
    }

    @Override
    protected ClassLoader getClassLoader() {
        return this.appLibLoader;
    }

}
