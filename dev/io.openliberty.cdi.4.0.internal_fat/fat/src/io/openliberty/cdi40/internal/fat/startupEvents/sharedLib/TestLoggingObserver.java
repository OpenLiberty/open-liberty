/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.cdi40.internal.fat.startupEvents.sharedLib;

import jakarta.annotation.Priority;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

@LoggedTest
@Interceptor
@Priority(1)
public class TestLoggingObserver {

    @AroundInvoke
    public Object runlogTestMethod(InvocationContext invocationContext) {

        String name = "" + invocationContext.getTarget().getClass().getName().replaceAll("\\$Proxy\\$_\\$\\$_WeldSubclass", "") + "." + invocationContext.getMethod().getName();
        Object returnValue = null;

        try {
            System.out.println("===TEST START " + name);
            returnValue = invocationContext.proceed();
            System.out.println("===TEST PASS " + name);
        } catch (AssertionError e) {
            //Omitting a stack because all these test classes already log out their stack
            //(The nature of these tests means that if one fails you want the other stacks for
            //comparason)
            System.out.println("===TEST FAIL " + name + " " + e);
        } catch (Exception e) {
            System.out.println("===TEST EXCEPTION " + name + " " + e);
        } catch (Throwable e) {
            System.out.println("===TEST EXCEPTION " + name + " " + e);
            throw e;
        }

        return returnValue;

    }
}
