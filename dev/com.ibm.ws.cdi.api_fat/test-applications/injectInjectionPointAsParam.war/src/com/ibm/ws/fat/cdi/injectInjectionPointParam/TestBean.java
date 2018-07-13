/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.fat.cdi.injectInjectionPointParam;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.InjectionPoint;

@Dependent
public class TestBean {

    private BeanManager bm;
    private InjectionPoint ip;

    @Inject
    public TestBean(BeanManager bm, InjectionPoint ip){ 
        this.bm = bm;
        this.ip = ip;
    }

    public boolean test() {
        return (bm != null && ip != null);
    }

}
