/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi20.fat.apps.configurator.producer;

import java.lang.annotation.Annotation;

import javax.enterprise.inject.Vetoed;

@Vetoed
public class ParameterInjectedBean {

    private Class<? extends Annotation> annotation;

    public ParameterInjectedBean(Class<? extends Annotation> annotation) {
        this.annotation = annotation;
    }

    public Class<?> getAnnotation() {
        return annotation;
    }
}
