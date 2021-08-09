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
package beanvalidationcdi.validation;

import java.util.Locale;

import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.validation.MessageInterpolator;

import beanvalidationcdi.beans.TestBean;

/**
 * Simple custom {@link MessageInterpolator} implementation that tests whether
 * CDI managed beans can be injected into this bean.
 */
public class CustomMessageInterpolator implements MessageInterpolator {

    @Inject
    TestBean bean;

    @Override
    public String interpolate(String arg0, Context arg1) {
        if (bean == null) {
            throw new IllegalStateException("bean is null, CDI must not have injected it");
        }
        return bean.getSomething();
    }

    @Override
    public String interpolate(String arg0, Context arg1, Locale arg2) {
        if (bean == null) {
            throw new IllegalStateException("bean is null, CDI must not have injected it");
        }
        return bean.getSomething();
    }

    @PreDestroy
    public void preDestroy() {
        System.out.println(CustomMessageInterpolator.class.getSimpleName() + " is getting destroyed.");
    }

}
