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
package configuratorApp.web;

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
        return beans.iterator().next();
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected <T> Set<Bean<T>> getBeans(Class<T> type, Annotation... bindings) {
        return (Set) bm.getBeans(type, bindings);
    }
}