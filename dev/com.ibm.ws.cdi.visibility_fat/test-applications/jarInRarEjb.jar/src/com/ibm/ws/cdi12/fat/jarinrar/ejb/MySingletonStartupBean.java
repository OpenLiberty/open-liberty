package com.ibm.ws.cdi12.fat.jarinrar.ejb;

import javax.annotation.PostConstruct;
import javax.ejb.LocalBean;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;

import com.ibm.ws.cdi12.fat.jarinrar.rar.Amigo;

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

@LocalBean
@Singleton
@Startup
public class MySingletonStartupBean {

    @Inject
    BeanManager beanManager;

    @PostConstruct
    public void init() {
        System.out.println("MySingletonStartupBean - init - entry");
        for (Bean<?> bean : beanManager.getBeans(Amigo.class)) {
            try {
                System.out.println("MySingletonStartupBean - init - " + bean.getBeanClass().newInstance());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        System.out.println("MySingletonStartupBean - init - exit");
    }
}
