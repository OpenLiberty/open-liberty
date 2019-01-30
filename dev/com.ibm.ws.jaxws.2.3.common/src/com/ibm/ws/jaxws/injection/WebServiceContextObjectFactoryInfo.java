/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxws.injection;

import java.lang.annotation.Annotation;

import javax.annotation.Resource;
import javax.naming.spi.ObjectFactory;
import javax.xml.ws.WebServiceContext;

import com.ibm.wsspi.injectionengine.ObjectFactoryInfo;

/**
 *
 */
public class WebServiceContextObjectFactoryInfo extends ObjectFactoryInfo {

    /** {@inheritDoc} */
    @Override
    public Class<? extends Annotation> getAnnotationClass() {
        return Resource.class;
    }

    /** {@inheritDoc} */
    @Override
    public Class<? extends ObjectFactory> getObjectFactoryClass() {
        return WebServiceContextObjectFactory.class;
    }

    /** {@inheritDoc} */
    @Override
    public Class<?> getType() {
        return WebServiceContext.class;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isOverrideAllowed() {
        return false;
    }

}
