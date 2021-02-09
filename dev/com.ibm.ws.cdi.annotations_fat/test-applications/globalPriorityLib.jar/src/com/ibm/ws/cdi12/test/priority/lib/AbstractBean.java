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

import javax.enterprise.context.RequestScoped;

import com.ibm.ws.cdi12.test.utils.ChainableList;
import com.ibm.ws.cdi12.test.utils.ChainableListImpl;
import com.ibm.ws.cdi12.test.utils.Intercepted;


@RequestScoped
public abstract class AbstractBean implements Bean {

    @Override
    public ChainableList<String> getDecorators() {
        return new ChainableListImpl<String>();
    }

    @Intercepted
    @Override
    public ChainableList<String> getInterceptors() {
        return new ChainableListImpl<String>();
    }

    public Class<? extends AbstractBean> getBean() {
        return this.getClass();
    }

}
