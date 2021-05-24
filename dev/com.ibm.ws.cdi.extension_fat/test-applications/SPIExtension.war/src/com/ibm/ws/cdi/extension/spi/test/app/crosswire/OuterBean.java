/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.extension.spi.test.app;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

@RequestScoped
@com.ibm.ws.cdi.extension.spi.test.bundle.getclass.interceptor.Intercept
@com.ibm.ws.cdi.misplaced.spi.test.bundle.getclass.interceptor.Intercept
public class OuterBean {

    @Inject
    WellPlacedBean bean;

    public String toString() {
        return bean.toString();
    }

}
