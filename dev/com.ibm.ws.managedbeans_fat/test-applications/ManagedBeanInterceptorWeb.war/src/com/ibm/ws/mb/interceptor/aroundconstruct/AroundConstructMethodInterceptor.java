/*******************************************************************************
 * Copyright (c) 2014, 2020 IBM Corporation and others.
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
package com.ibm.ws.mb.interceptor.aroundconstruct;

import static org.junit.Assert.fail;

import javax.interceptor.AroundConstruct;
import javax.interceptor.InvocationContext;

/**
 *
 */
public class AroundConstructMethodInterceptor {

    @AroundConstruct
    Object AroundConstruct(InvocationContext inv) {
        try {
            fail("AroundConstructMethodInterceptor was invoked");
            return null;
        } catch (Exception e) {
            e.printStackTrace(System.out);
            throw new RuntimeException("unexpected Exception", e);
        }
    }

}
