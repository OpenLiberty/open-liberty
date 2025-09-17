/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.cdi41.internal.fat.invokers.app;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

import java.util.stream.Collectors;

import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.AnnotatedMethod;
import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.ProcessManagedBean;
import jakarta.enterprise.invoke.Invoker;

public class InvokersExtension implements Extension {

    private Invoker<?, ?> basic;
    private Invoker<?, ?> reference;
    private Invoker<?, ?> basicWithLookup;
    private Invoker<?, ?> varargs;
    private Invoker<?, ?> varargsWithLookup;
    private Invoker<?, ?> noargs;

    public void createInvokers(@Observes ProcessManagedBean<InvokedBean> event) {
        var type = event.getAnnotatedBeanClass();

        basic = event.createInvoker(getMethod(type, "basicMethod"))
                     .build();
        reference = event.createInvoker(getMethod(type, "referenceMethod"))
                         .build();
        basicWithLookup = event.createInvoker(getMethod(type, "basicMethod"))
                               .withInstanceLookup()
                               .build();
        varargs = event.createInvoker(getMethod(type, "varargsMethod"))
                       .build();
        varargsWithLookup = event.createInvoker(getMethod(type, "varargsMethod"))
                                 .withInstanceLookup()
                                 .build();
        noargs = event.createInvoker(getMethod(type, "noargMethod")).build();
    }

    private <X> AnnotatedMethod<? super X> getMethod(AnnotatedType<X> type, String name) {
        var methods = type.getMethods()
                          .stream()
                          .filter(m -> m.getJavaMember().getName().equals(name))
                          .collect(Collectors.toList());
        assertThat("Single method not found with name " + name, methods, hasSize(1));
        return methods.get(0);
    }

    @SuppressWarnings("unchecked")
    public Invoker<Object, ?> getBasic() {
        return (Invoker<Object, ?>) basic;
    }

    @SuppressWarnings("unchecked")
    public Invoker<Object, ?> getReference() {
        return (Invoker<Object, ?>) reference;
    }

    @SuppressWarnings("unchecked")
    public Invoker<Object, ?> getBasicWithLookup() {
        return (Invoker<Object, ?>) basicWithLookup;
    }

    @SuppressWarnings("unchecked")
    public Invoker<Object, ?> getVarargs() {
        return (Invoker<Object, ?>) varargs;
    }

    @SuppressWarnings("unchecked")
    public Invoker<Object, ?> getVarargsWithLookup() {
        return (Invoker<Object, ?>) varargsWithLookup;
    }

    @SuppressWarnings("unchecked")
    public Invoker<Object, ?> getNoargs() {
        return (Invoker<Object, ?>) noargs;
    }
}
