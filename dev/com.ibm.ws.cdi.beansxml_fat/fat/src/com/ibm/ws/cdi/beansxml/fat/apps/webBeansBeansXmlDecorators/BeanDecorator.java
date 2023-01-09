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
package com.ibm.ws.cdi.beansxml.fat.apps.webBeansBeansXmlDecorators;

import javax.decorator.Decorator;
import javax.decorator.Delegate;
import javax.enterprise.inject.Any;
import javax.inject.Inject;

/**
 * Adds a prefix to the message returned from Bean instances
 */
@Decorator
public class BeanDecorator implements Bean {

    @Inject
    @Delegate
    @Any
    Bean bean;

    @Override
    public String getMessage() {
        if (this.bean == null) {
            return "Delegate not injected";
        }
        return "decorated " + this.bean.getMessage();
    }

}
