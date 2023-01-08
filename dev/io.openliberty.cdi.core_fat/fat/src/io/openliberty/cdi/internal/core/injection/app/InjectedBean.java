/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package io.openliberty.cdi.internal.core.injection.app;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class InjectedBean {

    @Inject
    private InjectionTestBean injectedField;

    private InjectionTestBean fromMethod;

    private InjectionTestBean fromConstructor;

    // Required no-arg constructor
    InjectedBean() {}

    @Inject
    private InjectedBean(InjectionTestBean testBean) {
        fromConstructor = testBean;
    }

    @Inject
    private void injectionMethod(InjectionTestBean testBean) {
        fromMethod = testBean;
    }

    public void testInjectedField() {
        assertThat(injectedField, notNullValue());
        assertThat(injectedField.test(), is(true));
    }

    public void testFromMethod() {
        assertThat(fromMethod, notNullValue());
        assertThat(fromMethod.test(), is(true));
    }

    public void testFromConstructor() {
        assertThat(fromConstructor, notNullValue());
        assertThat(fromConstructor.test(), is(true));
    }

}
