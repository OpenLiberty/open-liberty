/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi12.test.priority;

import javax.annotation.Priority;
import javax.decorator.Decorator;
import javax.decorator.Delegate;
import javax.enterprise.inject.Any;
import javax.inject.Inject;
import javax.interceptor.Interceptor;

import com.ibm.ws.cdi12.test.priority.helpers.AbstractDecorator;
import com.ibm.ws.cdi12.test.priority.helpers.Bean;

@Decorator
@Priority(Interceptor.Priority.APPLICATION)
public abstract class JarDecorator extends AbstractDecorator {

    @Inject
    public JarDecorator(@Delegate @Any Bean decoratedBean) {
        super(decoratedBean, JarDecorator.class);
    }

}
