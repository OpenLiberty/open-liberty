/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.cxf.microprofile.client.cdi;

import java.util.NoSuchElementException;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.CDI;

import com.ibm.ws.ffdc.annotation.FFDCIgnore;

public final class CDIUtils {

    private static boolean cdiAvailable = true;

    private CDIUtils() {}

    @FFDCIgnore({Throwable.class})
    static <T> T getInstanceFromCDI(Class<T> clazz) {
        T bean;
        if (cdiAvailable) {
            try {
                bean = findBean(clazz);
            } catch (Throwable t) {
                // NoSuchElementException - expected if ClientHeadersFactory is not a registered CDI Bean
                // ExceptionInInitializerError | NoClassDefFoundError | IllegalStateException expected if CDI is not enabled
                if (!(t instanceof NoSuchElementException)) {
                    cdiAvailable = false;
                }
                bean = null;
            }
        } else {
            bean = null;
        }
        return bean;
    }

    @SuppressWarnings("unchecked")
    private static <T> T findBean(Class<T> clazz) {
        BeanManager beanManager = CDI.current().getBeanManager();
        Bean<?> bean = beanManager.getBeans(clazz).iterator().next();
        CreationalContext<?> ctx = beanManager.createCreationalContext(bean);
        T instance = (T) beanManager.getReference(bean, clazz, ctx);
        return instance;
    }
}