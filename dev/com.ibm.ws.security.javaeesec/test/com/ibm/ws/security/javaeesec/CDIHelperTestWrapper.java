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
package com.ibm.ws.security.javaeesec;

import java.util.HashSet;
import java.util.Set;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
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

    public CDIHelperTestWrapper(Mockery mockery, BeanManager beanManager) {
        cdiHelper = new CDIHelper();
        this.mockery = mockery;
        currentModuleBeanManager = beanManager;
    }

    public void getsBeanFromCurrentModule(final RememberMeIdentityStore rememberMeIdentityStore) {
        final Set<Bean<?>> beans = new HashSet<Bean<?>>();
        final Bean<?> bean = mockery.mock(Bean.class);
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
            }
        });
    }

    @SuppressWarnings("rawtypes")
    public <T> void getsBeansFromCurrentModule(final Class<T> beanClass, final Set<Bean<?>> beans, final T beanObject) {
        final Bean<?> bean = mockery.mock(Bean.class);
        final CreationalContext creationalContext = mockery.mock(CreationalContext.class);

        mockery.checking(new Expectations() {
            {
                allowing(currentModuleBeanManager).getBeans(beanClass);
                will(returnValue(beans));
                allowing(currentModuleBeanManager).createCreationalContext(bean);
                will(returnValue(creationalContext));
                allowing(currentModuleBeanManager).getReference(bean, beanClass, creationalContext);
                will(returnValue(beanObject));
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
