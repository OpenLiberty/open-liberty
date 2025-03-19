/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.cdi.annotations.fat.apps.abstractDecorator;

import javax.annotation.Priority;
import javax.decorator.Decorator;
import javax.decorator.Delegate;
import javax.inject.Inject;

/**
 * Abstract decorator that only decorates some methods
 */
@Decorator
@Priority(1) // Enable for whole app
public abstract class AbstractDecorator implements AbstractDecoratorTestInterface {

    @Inject
    @Delegate
    private AbstractDecoratorTestInterface delegate;

    @Override
    public String test1() {
        return delegate.test1() + "-decorated";
    }

    @Override
    public abstract String test2();

    // test3() not implemented

}
