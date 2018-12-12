/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.config.cdi;

import java.lang.annotation.Annotation;
import java.util.concurrent.TimeUnit;

import javax.enterprise.util.AnnotationLiteral;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.ibm.websphere.ras.annotation.Trivial;

/**
 * ConfigPropertyLiteral represents an instance of the ConfigProperty annotation
 */
@Trivial
public class ConfigPropertyLiteral extends AnnotationLiteral<ConfigProperty> implements ConfigProperty {

    /**  */
    private static final long serialVersionUID = 1L;
    public static final Annotation INSTANCE = new ConfigPropertyLiteral();

    /** {@inheritDoc} */
    @Override
    public String name() {
        return "";
    }

    /** {@inheritDoc} */
    @Override
    public String defaultValue() {
        return "";
    }

    //introduced in 1.4
    public boolean evaluateVariables() {
        return true;
    }

    //introduced in 1.4
    public TimeUnit cacheTimeUnit() {
        return TimeUnit.SECONDS;
    }

    //introduced in 1.4
    public long cacheFor() {
        return 0L;
    }

}
