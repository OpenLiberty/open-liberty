/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.clientcontainer.fat;

import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

public class HelloAppClient {

    @Inject
    private static AppBean appBean;

    public static void main(String[] args) {
        System.out.println("Client App Start");

//        BeanManager cdiBeanManager = CDI.current().getBeanManager();
//        if (cdiBeanManager != null) {
//            System.out.println("Got BeanManager from CDI: " + cdiBeanManager.getClass());
//        }

        Context c;
        BeanManager jndiBeanManager = null;
        try {
            c = new InitialContext();
            jndiBeanManager = (BeanManager) c.lookup("java:comp/BeanManager");
            if (jndiBeanManager != null) {
                System.out.println("Got BeanManager from JNDI: " + jndiBeanManager.getClass());
            }
        } catch (NamingException e) {
            System.out.println("JNDI lookup failed");
            e.printStackTrace();
        }

//        if (cdiBeanManager != null && jndiBeanManager != null) {
//            System.out.println("Bean managers are " + (cdiBeanManager == jndiBeanManager ? "the same" : "different"));
//            System.out.println("Bean managers are " + (cdiBeanManager.equals(jndiBeanManager) ? "equal" : "not equal"));
//        }

//        Type beanType = AppBean.class;
//        Set<Bean<?>> beans = jndiBeanManager.getBeans(beanType);
//        Bean<?> bean = jndiBeanManager.resolve(beans);
//        CreationalContext<?> creationalContext = jndiBeanManager.createCreationalContext(bean);
//
//        AppBean appBean = (AppBean) jndiBeanManager.getReference(bean, beanType, creationalContext);
        if (appBean != null) {
            System.out.println("Got AppBean");
        }
        appBean.run();

        System.out.println("Client App End");
    }
}
