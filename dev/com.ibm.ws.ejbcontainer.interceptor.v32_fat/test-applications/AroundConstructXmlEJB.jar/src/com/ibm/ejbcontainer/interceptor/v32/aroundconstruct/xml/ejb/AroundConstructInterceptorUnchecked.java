/*******************************************************************************
 * Copyright (c) 2014, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejbcontainer.interceptor.v32.aroundconstruct.xml.ejb;

import javax.interceptor.InvocationContext;

public class AroundConstructInterceptorUnchecked {

    //@AroundConstruct
    public Object aroundConstruct(InvocationContext ctx) throws TestRuntimeException {
        throw new TestRuntimeException();
    }
}
