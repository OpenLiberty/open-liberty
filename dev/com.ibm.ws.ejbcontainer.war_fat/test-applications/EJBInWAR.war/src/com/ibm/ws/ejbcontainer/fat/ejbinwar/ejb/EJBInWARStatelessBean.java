/*******************************************************************************
 * Copyright (c) 2012, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.fat.ejbinwar.ejb;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;

import org.junit.Assert;

@Stateless
public class EJBInWARStatelessBean implements EJBInWARLocal {
    @Resource
    SessionContext context;

    @EJB(name = "ejb/statelessdef/stateless", beanName = "EJBInWARStatelessBean")
    EJBInWARLocal stateless;

    @Override
    public Class<?> getEJBClass() {
        return getClass();
    }

    @Override
    public void verifyInjection() {
        Assert.assertNotNull(context);
        Assert.assertEquals(getEJBClass(), context.getBusinessObject(EJBInWARLocal.class).getEJBClass());
    }

    @Override
    public void verifySharedLookup() {
        Assert.assertEquals(EJBInWARStatelessBean.class, ((EJBInWARLocal) context.lookup("java:comp/env/ejb/servletdef/stateless")).getEJBClass());
        Assert.assertEquals(EJBInWARStatelessBean.class, ((EJBInWARLocal) context.lookup("java:comp/env/ejb/singletondef/stateless")).getEJBClass());
        Assert.assertEquals(EJBInWARStatelessBean.class, ((EJBInWARLocal) context.lookup("java:comp/env/ejb/statelessdef/stateless")).getEJBClass());
    }

    @Override
    public void verifyJavaColonLookup() {
        Assert.assertEquals(EJBInWARStatelessBean.class,
                            ((EJBInWARLocal) context.lookup("java:module/EJBInWARStatelessBean!com.ibm.ws.ejbcontainer.fat.ejbinwar.ejb.EJBInWARLocal")).getEJBClass());
        Assert.assertEquals(EJBInWARSingletonBean.class,
                            ((EJBInWARLocal) context.lookup("java:module/EJBInWARSingletonBean!com.ibm.ws.ejbcontainer.fat.ejbinwar.ejb.EJBInWARLocal")).getEJBClass());

        Assert.assertEquals(EJBInWARStatelessBean.class,
                            ((EJBInWARLocal) context.lookup("java:app/EJBInWAR/EJBInWARStatelessBean!com.ibm.ws.ejbcontainer.fat.ejbinwar.ejb.EJBInWARLocal")).getEJBClass());
        Assert.assertEquals(EJBInWARSingletonBean.class,
                            ((EJBInWARLocal) context.lookup("java:app/EJBInWAR/EJBInWARSingletonBean!com.ibm.ws.ejbcontainer.fat.ejbinwar.ejb.EJBInWARLocal")).getEJBClass());

        Assert.assertEquals(EJBInWARStatelessBean.class,
                            ((EJBInWARLocal) context.lookup("java:global/EJBInWAR/EJBInWARStatelessBean!com.ibm.ws.ejbcontainer.fat.ejbinwar.ejb.EJBInWARLocal")).getEJBClass());
        Assert.assertEquals(EJBInWARSingletonBean.class,
                            ((EJBInWARLocal) context.lookup("java:global/EJBInWAR/EJBInWARSingletonBean!com.ibm.ws.ejbcontainer.fat.ejbinwar.ejb.EJBInWARLocal")).getEJBClass());
    }
}
