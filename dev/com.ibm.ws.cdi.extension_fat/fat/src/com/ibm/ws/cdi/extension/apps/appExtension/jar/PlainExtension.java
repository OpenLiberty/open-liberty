/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
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
package com.ibm.ws.cdi.extension.apps.appExtension.jar;

/**
 *
 */
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.inject.spi.BeanManager;

public class PlainExtension implements Extension {
    void beforeBeanDiscovery(@Observes BeforeBeanDiscovery bbd) {
        System.out.println("PlainExtension: beginning the scanning process");
    }

    <T> void processAnnotatedType(@Observes ProcessAnnotatedType<T> pat) {
        System.out.println("PlainExtension: scanning type->" + pat.getAnnotatedType().getJavaClass().getName());
    }

    void afterBeanDiscovery(@Observes AfterBeanDiscovery abd, BeanManager bm) {
        System.out.println("PlainExtension: finished the scanning process");
        System.out.println(bm.toString());
    }

}
