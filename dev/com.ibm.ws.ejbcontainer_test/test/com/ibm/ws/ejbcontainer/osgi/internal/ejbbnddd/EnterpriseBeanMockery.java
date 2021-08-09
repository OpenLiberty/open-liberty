/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.osgi.internal.ejbbnddd;

import java.util.concurrent.atomic.AtomicInteger;

import org.jmock.Expectations;
import org.jmock.Mockery;

import com.ibm.ws.javaee.dd.ejbbnd.EnterpriseBean;

abstract class EnterpriseBeanMockery<T extends EnterpriseBeanMockery<T>> {
    private static final AtomicInteger numBeans = new AtomicInteger();

    final Mockery mockery;
    private final String name;

    EnterpriseBeanMockery(Mockery mockery, String name) {
        this.mockery = mockery;
        this.name = name;
    }

    protected <B extends EnterpriseBean> B mockEnterpriseBean(final Class<B> klass) {
        final B bean = mockery.mock(klass, "enterpriseBeanBnd-" + numBeans.incrementAndGet());
        mockery.checking(new Expectations() {
            {
                allowing(bean).getName();
                will(returnValue(name));
            }
        });
        return bean;
    }
}
