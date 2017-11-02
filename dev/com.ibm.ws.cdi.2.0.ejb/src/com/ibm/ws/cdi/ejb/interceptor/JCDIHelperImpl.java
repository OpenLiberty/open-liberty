/*******************************************************************************
 * Copyright (c) 2013, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.ejb.interceptor;

import com.ibm.websphere.csi.J2EEName;
import com.ibm.ws.ejbcontainer.JCDIHelper;

public class JCDIHelperImpl implements JCDIHelper {

    private final Class<?> ejbInterceptor;

    public JCDIHelperImpl(Class<?> ejbInterceptor) {
        this.ejbInterceptor = ejbInterceptor;
    }

    @Override
    public Class<?> getFirstEJBInterceptor(J2EEName j2eeName, Class<?> ejbImpl) {
        return WeldSessionBeanInterceptorWrapper.class;
    }

    @Override
    public Class<?> getEJBInterceptor(J2EEName j2eeName, Class<?> ejbImpl) {
        return ejbInterceptor;
    }

}
