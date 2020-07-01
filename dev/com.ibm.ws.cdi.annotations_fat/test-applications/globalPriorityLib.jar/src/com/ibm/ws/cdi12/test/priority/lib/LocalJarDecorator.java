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
package com.ibm.ws.cdi12.test.priority.lib;

import javax.decorator.Decorator;
import javax.decorator.Delegate;
import javax.enterprise.inject.Any;
import javax.inject.Inject;

/**
 * Enabled for this bean archive in beans.xml.
 */
@Decorator
public abstract class LocalJarDecorator extends AbstractDecorator {

    @Inject
    public LocalJarDecorator(@Delegate @Any Bean decoratedBean) {
        super(decoratedBean, LocalJarDecorator.class);
    }

}
