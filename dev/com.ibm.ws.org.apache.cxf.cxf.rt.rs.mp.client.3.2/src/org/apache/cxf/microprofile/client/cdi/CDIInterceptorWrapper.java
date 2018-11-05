/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.apache.cxf.microprofile.client.cdi;

import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;

import com.ibm.websphere.ras.annotation.Trivial;

public interface CDIInterceptorWrapper {
    final static Logger LOG = LogUtils.getL7dLogger(CDIInterceptorWrapper.class);

    static class BasicCDIInterceptorWrapper implements CDIInterceptorWrapper {
        @Trivial
        BasicCDIInterceptorWrapper() {
        }

        @Trivial
        @Override
        public Object invoke(Object restClient, Method m, Object[] params, Callable callable)  throws Exception {
            return callable.call();
        }
    }

    public static CDIInterceptorWrapper createWrapper(Class<?> restClient) {
        try {
            return AccessController.doPrivileged((PrivilegedExceptionAction<CDIInterceptorWrapper>) () -> {
                Class<?> cdiClass = Class.forName("javax.enterprise.inject.spi.CDI", false,
                                                  restClient.getClassLoader());
                Method currentMethod = cdiClass.getMethod("current");
                Object cdiCurrent = currentMethod.invoke(null);

                Method getBeanMgrMethod = cdiClass.getMethod("getBeanManager");
                Object beanManager = getBeanMgrMethod.invoke(cdiCurrent);

                return new CDIInterceptorWrapperImpl(restClient);
            });
        } catch (PrivilegedActionException pae) {
            // expected for environments where CDI is not supported
            if (LOG.isLoggable(Level.FINEST)) {
                LOG.log(Level.FINEST, "Unable to load CDI SPI classes, assuming no CDI is available", pae);
            }
            return new BasicCDIInterceptorWrapper(); 
        }
        
    }

    Object invoke(Object restClient, Method m, Object[] params, Callable callable) throws Exception;
}
