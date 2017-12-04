package com.ibm.ws.cdi12.fat.jarinrar.ejb;

import javax.annotation.PostConstruct;
import javax.ejb.LocalBean;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;

import com.ibm.ws.cdi12.fat.jarinrar.rar.Amigo;

/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2015
 *
 * The source code for this program is not published or otherwise divested 
 * of its trade secrets, irrespective of what has been deposited with the 
 * U.S. Copyright Office.
 */

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
