/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.logging.internal;

import java.util.Set;

import org.hamcrest.Description;
import org.jmock.api.Action;
import org.jmock.api.Invocation;

public class InternalPackageChecker<T> implements Action {

    private final Set<String> internalPackages;

    /**
     * @param internalPackages
     */
    public InternalPackageChecker(Set<String> internalPackages) {
        this.internalPackages = internalPackages;
    }

    public void describeTo(Description description) {
        description.appendText("testing internal package");
    }

    public Object invoke(Invocation invocation) throws Throwable {
        return internalPackages.contains(invocation.getParameter(0));
    }

    public static <T> Action checkIfPackageIsInSet(Set<String> internalPackages) {
        return new InternalPackageChecker<T>(internalPackages);
    }
}
