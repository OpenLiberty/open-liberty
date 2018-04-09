/*******************************************************************************
 * Copyright (c) 2017, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.javaeesec;

import java.util.HashSet;
import java.util.Set;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.security.enterprise.identitystore.IdentityStore;
import javax.security.enterprise.identitystore.IdentityStoreHandler;
import javax.security.enterprise.identitystore.RememberMeIdentityStore;

import org.jmock.Expectations;
import org.jmock.Mockery;

import com.ibm.ws.cdi.CDIService;

/**
 * Expose CDIHelper's protected methods for unit testing.
 */
public class CDIHelperTestWrapper {

    CDIHelper cdiHelper;
    private final Mockery mockery;
    protected BeanManager currentModuleBeanManager;

    public CDIHelperTestWrapper(Mockery mockery, final RememberMeIdentityStore rememberMeIdentityStore) {
        cdiHelper = new CDIHelper();
        this.mockery = mockery;
        currentModuleBeanManager = mockery.mock(BeanManager.class);

        final Set<Bean<?>> beans = new HashSet<Bean<?>>();
        final Set<Bean<?>> beans1 = new HashSet<Bean<?>>();
        final Bean<?> bean = mockery.mock(Bean.class);
        final Bean<?> bean1 = mockery.mock(Bean.class, "identitystorehandler");
        beans1.add(bean1);
        final CreationalContext creationalContext = mockery.mock(CreationalContext.class);

        mockery.checking(new Expectations() {
            {
                allowing(currentModuleBeanManager).getBeans(RememberMeIdentityStore.class);
                will(returnValue(beans));
                allowing(currentModuleBeanManager).resolve(beans);
                will(returnValue(bean));
                allowing(currentModuleBeanManager).createCreationalContext(bean);
                will(returnValue(creationalContext));
                allowing(currentModuleBeanManager).getReference(bean, RememberMeIdentityStore.class, creationalContext);
                will(returnValue(rememberMeIdentityStore));
                allowing(currentModuleBeanManager).getBeans(IdentityStore.class);
                will(returnValue(beans));
                allowing(currentModuleBeanManager).getBeans(IdentityStoreHandler.class);
                will(returnValue(beans1));
                allowing(currentModuleBeanManager).resolve(beans1);
                will(returnValue(bean1));
                allowing(currentModuleBeanManager).createCreationalContext(bean1);
                will(returnValue(creationalContext));
                allowing(currentModuleBeanManager).getReference(bean1, IdentityStoreHandler.class, creationalContext);
                will(returnValue(null));
            }
        });
    }

    public void setCDIService(final CDIService cdiService) {
        cdiHelper.setCDIService(cdiService);

        mockery.checking(new Expectations() {
            {
                allowing(cdiService).getCurrentModuleBeanManager();
                will(returnValue(currentModuleBeanManager));
            }
        });
    }

    public void unsetCDIService(CDIService cdiService) {
        cdiHelper.unsetCDIService(cdiService);
    }

}
