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
package configuratorApp.web.producer;

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
