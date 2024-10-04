/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.cdi.api.fat.apps.threads.extension;

import java.util.HashSet;
import java.util.Set;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.AfterDeploymentValidation;
import javax.enterprise.inject.spi.AfterTypeDiscovery;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.CDI;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.inject.spi.ProcessBean;
import javax.enterprise.inject.spi.ProcessBeanAttributes;
import javax.enterprise.inject.spi.ProcessInjectionPoint;
import javax.enterprise.inject.spi.ProcessInjectionTarget;
import javax.enterprise.inject.spi.ProcessManagedBean;

/**
 *
 */
@SuppressWarnings("rawtypes")
public class CDIExtension implements Extension {

    private final Set<String> duplicatesFilter = new HashSet<String>();

    public void eventListener(@Observes BeforeBeanDiscovery event) {
        checkForBeanManagerInUnmanagedThread("BeforeBeanDiscovery");
    }

    public void eventListener(@Observes ProcessAnnotatedType event) {
        checkForBeanManagerInUnmanagedThread("ProcessAnnotatedType");
    }

    public void eventListener(@Observes AfterTypeDiscovery event) {
        checkForBeanManagerInUnmanagedThread("AfterTypeDiscovery");
    }

    public void eventListener(@Observes ProcessInjectionTarget event) {
        checkForBeanManagerInUnmanagedThread("ProcessInjectionTarget");
    }

    public void eventListener(@Observes ProcessInjectionPoint event) {
        checkForBeanManagerInUnmanagedThread("ProcessInjectionPoint");
    }

    public void eventListener(@Observes ProcessBeanAttributes event) {
        checkForBeanManagerInUnmanagedThread("ProcessBeanAttributes");
    }

    public void eventListener(@Observes ProcessBean event) {
        checkForBeanManagerInUnmanagedThread("ProcessBean");
    }

    public void eventListener(@Observes ProcessManagedBean event) {
        checkForBeanManagerInUnmanagedThread("ProcessManagedBean");
    }

    public void eventListener(@Observes AfterBeanDiscovery event) {
        checkForBeanManagerInUnmanagedThread("AfterBeanDiscovery");
    }

    public void eventListener(@Observes AfterDeploymentValidation event) {
        checkForBeanManagerInUnmanagedThread("AfterDeploymentValidation");
    }

    private void checkForBeanManagerInUnmanagedThread(final String eventName) {
        Thread thread = new Thread() {
            @Override
            public void run() {
                try {
                    BeanManager bm = CDI.current().getBeanManager();
                    String s = "found beanmanager in " + eventName + " : " + (bm instanceof BeanManager);
                    if (duplicatesFilter.add(s)) {
                        System.out.println(s);
                    }
                } catch (Exception e) {
                    String s = "failed to find beanmanager in " + eventName + " : " + e.toString();
                    if (duplicatesFilter.add(s)) {
                        System.out.println(s);
                        e.printStackTrace(System.out);
                    }
                }
            }
        };

        thread.start();
    }

}
