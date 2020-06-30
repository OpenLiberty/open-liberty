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
package io.openliberty.microprofile.faulttolerance30.internal.cdi;

import java.lang.reflect.Method;
import java.util.Objects;

import javax.annotation.Priority;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Alternative;
import javax.enterprise.inject.spi.Bean;

import com.ibm.ws.microprofile.faulttolerance.cdi.AbstractPolicyStore;
import com.ibm.ws.microprofile.faulttolerance.cdi.PolicyStore;
import com.ibm.ws.microprofile.faulttolerance.cdi.PolicyStoreImpl;

/**
 * A {@link PolicyStore} which is {@link ApplicationScoped} and uses both the {@code bean} and {@code method} to look up the policy.
 * <p>
 * This results in storing one policy per bean per method, as required by the FT 3.0 spec.
 * <p>
 * This class is annotated as an alternative with a priority so that it will always be used in preference to {@link PolicyStoreImpl} when both are present.
 */
@ApplicationScoped
@Alternative
@Priority(10)
public class PolicyStoreImpl30 extends AbstractPolicyStore<PolicyStoreImpl30.Key> {

    @Override
    protected Key getKey(Bean<?> bean, Method method) {
        return new Key(bean, method);
    }

    /**
     * Holds both a {@link Bean} and a {@link Method}
     * <p>
     * Suitable for use as a map key.
     */
    static class Key {
        private final Bean<?> bean;
        private final Method method;

        public Key(Bean<?> bean, Method method) {
            this.bean = bean;
            this.method = method;
        }

        @Override
        public int hashCode() {
            return Objects.hash(bean, method);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            Key other = (Key) obj;
            return Objects.equals(bean, other.bean) && Objects.equals(method, other.method);
        }
    }

}
