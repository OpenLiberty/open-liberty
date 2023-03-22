/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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
package test.jakarta.data.inmemory.provider;

import java.util.Collections;
import java.util.Set;

import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.enterprise.inject.spi.Producer;
import jakarta.enterprise.inject.spi.ProducerFactory;

public class PalindromeRepositoryProducer<R, P> implements Producer<PalindromeRepository> {
    /**
     * Factory class for this repository producer.
     */
    static class Factory<P> implements ProducerFactory<P> {
        @Override
        @SuppressWarnings({ "rawtypes", "unchecked" })
        public <R> Producer<R> createProducer(Bean<R> bean) {
            return new PalindromeRepositoryProducer();
        }
    }

    @Override
    public void dispose(PalindromeRepository instance) {
        System.out.println("Palindrome CDI extension has disposed " + instance);
    }

    @Override
    public Set<InjectionPoint> getInjectionPoints() {
        return Collections.emptySet();
    }

    @Override
    public PalindromeRepository produce(CreationalContext<PalindromeRepository> cc) {
        PalindromeRepository instance = new PalindromeRepository();

        System.out.println("Palindrome CDI extension has produced " + instance);

        return instance;
    }
}