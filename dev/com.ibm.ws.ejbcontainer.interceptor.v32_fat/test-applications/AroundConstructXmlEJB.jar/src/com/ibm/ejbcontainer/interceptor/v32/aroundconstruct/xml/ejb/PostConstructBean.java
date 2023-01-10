/*******************************************************************************
 * Copyright (c) 2014, 2021 IBM Corporation and others.
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
package com.ibm.ejbcontainer.interceptor.v32.aroundconstruct.xml.ejb;

//@Stateless
//@Interceptors({ PostConstructInterceptor1.class, PostConstructInterceptor2.class })
public class PostConstructBean {

    protected String beanName = PostConstructBean.class.getSimpleName();

    public PostConstructBean() {}

    public String getBeanName() {
        return beanName;
    }

    public void verifyLifeCycleNonNullReturn() {}

}
