/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
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
package com.ibm.ws.cdi.client.fat.greeting.impl;

import static javax.interceptor.Interceptor.Priority.APPLICATION;

import javax.annotation.Priority;
import javax.decorator.Decorator;
import javax.decorator.Delegate;
import javax.enterprise.inject.Any;
import javax.inject.Inject;

import com.ibm.ws.cdi.client.fat.greeting.Greeter;

/**
 * A decorator that makes a greeter a little more piratey
 */
@Decorator
@Priority(APPLICATION)
public class PirateGreeterDecorator implements Greeter {

    @Delegate
    @Inject
    @Any
    private Greeter delegate;

    @Override
    public String getHello() {
        return delegate.getHello() + ", I mean... Ahoy!";
    }

}
