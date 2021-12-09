/*******************************************************************************
 * Copyright (c) 2013, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.fat.beaninterfaceholderlib;

public interface BasicInterceptorLocal {
    /** Simple method to verify the correct bean class is in use **/
    String getSimpleBeanName();

    /** Throws assertion error if PostConstruct has not been called. **/
    void verifyPostConstruct();

    /** Throws assertion error if interceptor's aroundInvoke wasn't called before calling this method. **/
    void verifyInterceptorAroundInvoke();

    /** removes the bean, if applicable. **/
    void remove();
}
