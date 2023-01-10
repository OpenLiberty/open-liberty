/*******************************************************************************
 * Copyright (c) 2015, 2020 IBM Corporation and others.
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
package com.ibm.ws.cdi.api.fat.apps.current.extension;

import java.util.Set;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.AfterDeploymentValidation;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.CDI;
import javax.enterprise.inject.spi.Extension;

public class MyDeploymentVerifier implements Extension {

    public static final String FAIL = "FAIL";
    public static final String SUCCESS = "SUCCESS";
    private static String message = FAIL;

    public void afterBeanDiscovery(@Observes AfterBeanDiscovery event) {
        event.addBean(new CDICurrentTestBean());
    }

    public void afterDeploymentValidation(@Observes AfterDeploymentValidation event) {
        BeanManager beanManager = CDI.current().getBeanManager();

        Set<Bean<?>> beans = beanManager.getBeans(CDICurrent.class, DefaultLiteral.INSTANCE);
        if (beans != null && beans.size() == 1) {
            Bean<?> bean = beans.iterator().next();
            if (bean.getBeanClass() == CDICurrent.class) {
                message = SUCCESS;
            } else {
                message = FAIL + ": Bean Class = " + bean.getBeanClass();
            }
        } else {
            message = FAIL + ": number of beans = " + beans == null ? "NULL" : "" + beans.size();
        }
    }

    public static String getMessage() {
        return message;
    }
}
