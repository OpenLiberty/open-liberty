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
package com.ibm.ws.microprofile.config14.cdi;

import java.lang.annotation.Annotation;

import javax.enterprise.util.AnnotationLiteral;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.ibm.websphere.ras.annotation.Trivial;

/**
 * Config14PropertyLiteral represents an instance of the ConfigProperty annotation
 */
@Trivial
public class Config14PropertyLiteral extends AnnotationLiteral<ConfigProperty> implements ConfigProperty {

    /**  */
    private static final long serialVersionUID = 1L;
    public static final Annotation INSTANCE = new Config14PropertyLiteral();

    /** {@inheritDoc} */
    @Override
    public String name() {
        return "";
    }

    /** {@inheritDoc} */
    @Override
    public String defaultValue() {
        return UNCONFIGURED_VALUE;
    }

}
