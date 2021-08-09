/*******************************************************************************
 * Copyright (c) 2012, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.osgi.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Assert;

import com.ibm.ws.ejbcontainer.InternalConstants;
import com.ibm.ws.javaee.dd.commonbnd.Interceptor;

public class ModuleInitDataChecker {
    static final ClassLoader mockClassLoader = new ClassLoader() {};

    private final ModuleInitDataImpl mid;
    private final List<BeanInitDataChecker> beans = new ArrayList<BeanInitDataChecker>();
    private boolean metadataComplete;
    private boolean timers;
    private int numInterceptors = 0;
    private int numManagedBeans = 0;
    private final Set<String> ejbInterceptors = new HashSet<String>();
    private final Set<String> mbInterceptors = new HashSet<String>();

    ModuleInitDataChecker(ModuleInitDataImpl mid) {
        this.mid = mid;
    }

    ModuleInitDataChecker metadataComplete() {
        this.metadataComplete = true;
        return this;
    }

    ModuleInitDataChecker bean(BeanInitDataChecker bidChecker) {
        beans.add(bidChecker);
        if (bidChecker.type == InternalConstants.TYPE_MANAGED_BEAN) {
            numManagedBeans++;
        }
        return this;
    }

    ModuleInitDataChecker timers() {
        this.timers = true;
        return this;
    }

    ModuleInitDataChecker numInterceptors(int num) {
        this.numInterceptors = num;
        return this;
    }

    ModuleInitDataChecker ejbInterceptors(String... ejbInterceptors) {
        this.ejbInterceptors.addAll(Arrays.asList(ejbInterceptors));
        return this;
    }

    ModuleInitDataChecker mbInterceptors(String... mbInterceptors) {
        this.mbInterceptors.addAll(Arrays.asList(mbInterceptors));
        return this;
    }

    void check() {
        Assert.assertNotNull("name", mid.ivName);
        Assert.assertNotNull("logical name", mid.ivLogicalName);
        Assert.assertNotNull("app name", mid.ivAppName);
        Assert.assertNotNull("J2EEName", mid.ivJ2EEName);
        Assert.assertEquals("J2EEName.getApplication", mid.ivAppName, mid.ivJ2EEName.getApplication());
        Assert.assertEquals("J2EEName.getModule", mid.ivName, mid.ivJ2EEName.getModule());
        Assert.assertNull("J2EEName.getComponent", mid.ivJ2EEName.getComponent());
        Assert.assertEquals("metadata-complete", metadataComplete, mid.ivMetadataComplete);
        Assert.assertSame("classloader", mockClassLoader, mid.ivClassLoader);

        Assert.assertEquals(mid.ivBeans.toString(), beans.size(), mid.ivBeans.size());
        for (BeanInitDataChecker bidChecker : beans) {
            bidChecker.check(mid);
        }

        // Core container handles null ivHasTimers, but for performance, it should always be set.
        Assert.assertEquals("timers", timers, mid.ivHasTimers);

        Assert.assertEquals(mid.getEJBEndpoints().toString(), beans.size() - numManagedBeans, mid.getEJBEndpoints().size());
        try {
            mid.getEJBEndpoints().add(null);
            Assert.fail();
        } catch (UnsupportedOperationException e) {
        }

        Assert.assertEquals(mid.getManagedBeanEndpoints().toString(), numManagedBeans, mid.getManagedBeanEndpoints().size());
        try {
            mid.getManagedBeanEndpoints().add(null);
            Assert.fail();
        } catch (UnsupportedOperationException e) {
        }

        if (numInterceptors > 0) {
            Assert.assertNotNull("interceptor map should not be null", mid.ejbJarInterceptorBindings);
            Assert.assertEquals(mid.ejbJarInterceptorBindings.size(), numInterceptors);
            for (Interceptor interceptor : mid.ejbJarInterceptorBindings.values()) {
                Assert.assertNotNull("Interceptors should not be null", interceptor);
            }
        }

        if (numManagedBeans > 0 && numManagedBeans == beans.size()) {
            Assert.assertTrue("containsManagedBeansOnly should be true", mid.containsManagedBeansOnly());
        } else {
            Assert.assertFalse("containsManagedBeansOnly should be false", mid.containsManagedBeansOnly());
        }

        if (ejbInterceptors.isEmpty()) {
            Assert.assertNull(" EJB interceptors: " + mid.ivEJBInterceptorClassNames, mid.ivEJBInterceptorClassNames);
        } else {
            Assert.assertEquals(" EJB interceptors: ", ejbInterceptors, mid.ivEJBInterceptorClassNames);
        }
        Assert.assertEquals(" getEJBInterceptorClassNames: ", ejbInterceptors, mid.getEJBInterceptorClassNames());
        try {
            mid.getEJBInterceptorClassNames().add(null);
            Assert.fail();
        } catch (UnsupportedOperationException e) {
        }

        if (mbInterceptors.isEmpty()) {
            Assert.assertNull(" MB interceptors: " + mid.ivMBInterceptorClassNames, mid.ivMBInterceptorClassNames);
        } else {
            Assert.assertEquals(" MB interceptors: ", mbInterceptors, mid.ivMBInterceptorClassNames);
        }
        Assert.assertEquals(" getManagedBeanInterceptorClassNames: ", mbInterceptors, mid.getManagedBeanInterceptorClassNames());
        try {
            mid.getManagedBeanInterceptorClassNames().add(null);
            Assert.fail();
        } catch (UnsupportedOperationException e) {
        }
    }
}
