/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi12.test.current.extension;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.AfterDeploymentValidation;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.CDI;
import javax.enterprise.inject.spi.Extension;

public class MyDeploymentVerifier implements Extension {

    private static List<String> messages = new ArrayList<String>();

    public void afterBeanDiscovery(@Observes AfterBeanDiscovery event) {
        event.addBean(new CDICurrentTestBean());
    }

    public void afterDeploymentValidation(@Observes AfterDeploymentValidation event) {
        BeanManager beanManager = CDI.current().getBeanManager();

        Set<Bean<?>> beans = beanManager.getBeans(CDICurrent.class, DefaultLiteral.INSTANCE);
        if (beans != null && beans.size() == 1) {
            Bean<?> bean = beans.iterator().next();
            if (bean.getBeanClass() == CDICurrent.class) {
                messages.add("SUCCESS");
            }
            else {
                messages.add("FAIL: Bean Class = " + bean.getBeanClass());
            }
        }
        else {
            messages.add("FAIL: number of beans = " + beans == null ? "NULL" : "" + beans.size());
        }
    }

    public static List<String> getMessages() {
        return messages;
    }
}
