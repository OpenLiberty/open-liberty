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
package com.ibm.ws.cdi.annotations.fat.apps.globalPriority.lib;

import com.ibm.ws.cdi.annotations.fat.apps.utils.ChainableList;
import com.ibm.ws.cdi.annotations.fat.apps.utils.Utils;


public abstract class AbstractDecorator implements Bean {

    private final String name;
    protected final Bean decoratedBean;

    public AbstractDecorator(final Bean decoratedBean, final Class<?> subclass) {
        this.decoratedBean = decoratedBean;
        this.name = Utils.id(subclass);
    }

    @Override
    public ChainableList<String> getDecorators() {
        return decoratedBean.getDecorators().chainAdd(name);
    }

}
