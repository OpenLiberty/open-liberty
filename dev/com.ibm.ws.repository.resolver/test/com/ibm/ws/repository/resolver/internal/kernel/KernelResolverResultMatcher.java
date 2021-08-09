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

import static org.hamcrest.Matchers.containsInAnyOrder;

import java.util.Arrays;
import java.util.Collection;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

import com.ibm.ws.kernel.feature.resolver.FeatureResolver;
import com.ibm.ws.kernel.feature.resolver.FeatureResolver.Result;

/**
 * Verify results from a {@link FeatureResolver.Result}
 * <p>
 * E.g. {@code result().withResolvedFeatures("a", "b", "c")}
 * <p>
 * E.g. {@code result().withErrors().withResolvedFeatures("a")}
 */
public class KernelResolverResultMatcher extends TypeSafeMatcher<FeatureResolver.Result> {

    private boolean hasErrors = false;
    private Collection<String> resolvedFeatures;

    public static KernelResolverResultMatcher result() {
        return new KernelResolverResultMatcher();
    }

    public KernelResolverResultMatcher withErrors() {
        hasErrors = true;
        return this;
    }

    public KernelResolverResultMatcher withResolvedFeatures(String... resolvedFeatures) {
        this.resolvedFeatures = Arrays.asList(resolvedFeatures);
        return this;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("Result")
                   .appendText("\n\tHas errors: ")
                   .appendValue(hasErrors);

        if (resolvedFeatures != null) {
            description.appendText("\n\tResolved features: ")
                       .appendValueList("[", ", ", "]", resolvedFeatures);
        }
    }

    @Override
    protected boolean matchesSafely(Result item) {
        if (item.hasErrors() != hasErrors) {
            return false;
        }

        if (resolvedFeatures != null && !containsInAnyOrder(resolvedFeatures.toArray()).matches(item.getResolvedFeatures())) {
            return false;
        }

        return true;
    }

    @Override
    protected void describeMismatchSafely(Result item, Description description) {
        description.appendText("Result")
                   .appendText("\n\tHas errors: ")
                   .appendValue(item.hasErrors())
                   .appendText("\n\tResolved features: ")
                   .appendValueList("[", ", ", "]", item.getResolvedFeatures())
                   .appendText("\n\tMissing features: ")
                   .appendValueList("[", ", ", "]", item.getMissing())
                   .appendText("\n\tNon public roots: ")
                   .appendValueList("[", ", ", "]", item.getNonPublicRoots());
    }

}
