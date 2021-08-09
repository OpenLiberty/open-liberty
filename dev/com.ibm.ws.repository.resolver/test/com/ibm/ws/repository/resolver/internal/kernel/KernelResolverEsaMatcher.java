/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.repository.resolver.internal.kernel;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

import com.ibm.ws.kernel.feature.provisioning.ProvisioningFeatureDefinition;
import com.ibm.ws.repository.resources.EsaResource;

/**
 * Checks whether a ProvisioningFeatureDefinition is actually a ResolverEsa wrapping a specific EsaResource instance
 */
public class KernelResolverEsaMatcher extends TypeSafeMatcher<KernelResolverEsa> {

    private final EsaResource esaResource;

    @SuppressWarnings("unchecked")
    public static <T extends ProvisioningFeatureDefinition> TypeSafeMatcher<T> resolverEsaWrapping(EsaResource esaResource) {
        return (TypeSafeMatcher<T>) new KernelResolverEsaMatcher(esaResource);
    }

    public KernelResolverEsaMatcher(EsaResource esaResource) {
        this.esaResource = esaResource;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("ResolverEsa wrapping ").appendValue(esaResource);
    }

    @Override
    protected boolean matchesSafely(KernelResolverEsa item) {
        if (item.getResource() == esaResource) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    protected void describeMismatchSafely(KernelResolverEsa item, Description mismatchDescription) {
        mismatchDescription.appendText("ResolverEsa wrapping ").appendValue(item.getResource());
    }
}
