/*******************************************************************************
 * Copyright (c) 2017, 2021 IBM Corporation and others.
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
package com.ibm.ws.cdi20.fat.apps.configurator;

import static org.junit.Assert.assertTrue;

import java.lang.annotation.Annotation;
import java.util.Set;

import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
public abstract class ConfiguratorTestBase extends FATServlet {

    @Inject
    protected BeanManager bm;

    protected <T> Bean<T> getBean(Class<T> type, Annotation... bindings) {
        Set<Bean<T>> beans = getBeans(type, bindings);
        assertTrue("No Beans found of type " + type, beans.size() > 0);
        return beans.iterator().next();
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected <T> Set<Bean<T>> getBeans(Class<T> type, Annotation... bindings) {
        return (Set) bm.getBeans(type, bindings);
    }
}