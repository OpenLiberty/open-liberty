/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi12.test.withAnnotations;

import static com.ibm.ws.cdi12.test.utils.Utils.id;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.RequestScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.inject.spi.WithAnnotations;

public class WithAnnotationsExtension implements Extension {

    private static final List<String> allAnnotatedTypes = new ArrayList<String>();
    private static final List<String> requestScopedTypes = new ArrayList<String>();

    <T> void listAnnotatedTypes(@Observes ProcessAnnotatedType<T> processEvent) {
        allAnnotatedTypes.add(id(processEvent.getAnnotatedType()));
    }

    <T> void listRequestScopedTypes(@Observes @WithAnnotations(RequestScoped.class) ProcessAnnotatedType<T> processEvent) {
        requestScopedTypes.add(id(processEvent.getAnnotatedType()));
    }

    public static List<String> getAllAnnotatedTypes() {
        return allAnnotatedTypes;
    }

    public static List<String> getRequestScopedTypes() {
        return requestScopedTypes;
    }

}
