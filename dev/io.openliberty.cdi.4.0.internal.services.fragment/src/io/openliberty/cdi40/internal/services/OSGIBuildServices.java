/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.cdi40.internal.services;

import java.util.Optional;

import com.ibm.ws.kernel.service.util.ServiceCaller;

import jakarta.enterprise.inject.build.compatible.spi.AnnotationBuilderFactory;
import jakarta.enterprise.inject.build.compatible.spi.BuildServices;

/**
 * A service loaded implementation of BuildServices which looks up the real implementation via OSGi
 */
public class OSGIBuildServices implements BuildServices {

    /** {@inheritDoc} */
    @Override
    public int getPriority() {
        return 0; //default value, don't know what it should be
    }

    /** {@inheritDoc} */
    @Override
    public AnnotationBuilderFactory annotationBuilderFactory() {
        //Use OSGi to lookup an instance of BuildServices
        //OSGIBuildServices itself will not be found because it is only Service Loaded and is not an OSGi Service
        Optional<AnnotationBuilderFactory> factoryOpt = ServiceCaller.callOnce(OSGIBuildServices.class, BuildServices.class,
                                                                               (bs) -> {
                                                                                   return bs.annotationBuilderFactory();
                                                                               });
        //could throw a NoSuchElementException... should we catch and re-throw with a message??
        AnnotationBuilderFactory factory = factoryOpt.get();
        return factory;
    }

}
