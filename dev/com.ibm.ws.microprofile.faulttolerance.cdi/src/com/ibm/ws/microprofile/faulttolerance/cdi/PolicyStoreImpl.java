/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
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
package com.ibm.ws.microprofile.faulttolerance.cdi;

import java.lang.reflect.Method;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.spi.Bean;

/**
 * A {@link PolicyStore} which is {@link Dependent} scoped and uses just the {@code method} to look up the policy.
 * <p>
 * This results in storing one policy per bean instance per method.
 */
@Dependent
public class PolicyStoreImpl extends AbstractPolicyStore<Method> {

    @Override
    protected Method getKey(Bean<?> bean, Method method) {
        return method;
    }

}
