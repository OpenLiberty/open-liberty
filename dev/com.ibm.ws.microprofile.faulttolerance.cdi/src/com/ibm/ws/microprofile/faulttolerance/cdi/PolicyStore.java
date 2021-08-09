/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.faulttolerance.cdi;

import java.lang.reflect.Method;
import java.util.function.Supplier;

import javax.enterprise.inject.spi.Bean;

/**
 * Stores instances of {@link AggregatedFTPolicy} and allows them to be looked up again later.
 * <p>
 * An bean of this type is used by {@link FaultToleranceInterceptor} to store computed policies. The scope and implementation of this bean will affect whether a new policy instance
 * is created or an old one is reused for a given method invocation.
 */
public interface PolicyStore {

    /**
     * Get an existing policy from the store or create a new one and store it.
     * <p>
     * The implementation may use the {@code bean} and {@code method} to decide whether to create a new policy or return an existing one.
     * <p>
     * The implementation will use the {@code supplier} to create a new policy if one is required.
     * <p>
     * In some cases, the implementation may call the {@code supplier} but not use the returned instance.
     *
     * @param bean     the bean associated with the policy
     * @param method   the method associated with the policy
     * @param supplier a supplier of a new policy for the given bean or method
     * @return a policy for the given bean or method which was previously stored, or a new policy created using {@code supplier}
     */
    public AggregatedFTPolicy getOrCreate(Bean<?> bean, Method method, Supplier<AggregatedFTPolicy> supplier);

}
