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
package com.ibm.ws.cdi.client.fat;

import java.lang.reflect.Type;
import java.util.Set;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * A more advanced application which exercises decorators, interceptors and events.
 */
public class AdvancedAppClass {

    public static void main(String[] args) {

        Context c;
        BeanManager beanManager = null;
        try {
            c = new InitialContext();
            beanManager = (BeanManager) c.lookup("java:comp/BeanManager");

            Type beanType = AppBean.class;
            Set<Bean<?>> beans = beanManager.getBeans(beanType);
            Bean<?> bean = beanManager.resolve(beans);
            CreationalContext<?> creationalContext = beanManager.createCreationalContext(bean);

            AppBean appBean = (AppBean) beanManager.getReference(bean, beanType, creationalContext);
            appBean.run();
        } catch (NamingException e) {
            System.out.println("JNDI lookup failed");
            e.printStackTrace();
        }

    }

}
