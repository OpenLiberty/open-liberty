/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.beansxml.fat.apps.aftertypediscovery;

import javax.decorator.Decorator;
import javax.decorator.Delegate;
import javax.enterprise.inject.Any;
import javax.inject.Inject;

@Decorator
public class AfterTypeBeanDecorator implements AfterTypeInterface {

    public static final String DECORATED = "The decorated message is: ";

    @Inject
    @Delegate
    @Any
    AfterTypeInterface bean;

    @Override
    public String getMsg() {
        return DECORATED + bean.getMsg();
    }

}
