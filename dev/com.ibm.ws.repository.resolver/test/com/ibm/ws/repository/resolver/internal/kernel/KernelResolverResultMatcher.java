/*******************************************************************************
 * Copyright (c) 2018, 2022 IBM Corporation and others.
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
public class KernelResolverResultMatcher extends TypeSafeMatcher<Result> {

    private boolean hasErrors = false;
    private Collection<String> conflicts;
    private Collection<String> resolvedFeatures;
    private Collection<String> missingFeatures;

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

    public KernelResolverResultMatcher withMissingFeatures(String... missingFeatures) {
        this.missingFeatures = Arrays.asList(missingFeatures);
        this.hasErrors = true;
        return this;
    }

    public KernelResolverResultMatcher withConflicts(String... conflicts) {
        this.conflicts = Arrays.asList(conflicts);
        this.hasErrors = true;
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

        if (missingFeatures != null) {
            description.appendText("\n\tMissing features: ")
                       .appendValueList("[", ", ", "]", missingFeatures);
        }

        if (conflicts != null) {
            description.appendText("\n\tConflicts: ")
                       .appendValueList("[", ", ", "]", conflicts);
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

        if (missingFeatures != null && !containsInAnyOrder(missingFeatures.toArray()).matches(item.getMissing())) {
            return false;
        }

        if (conflicts != null && !containsInAnyOrder(conflicts.toArray()).matches(item.getConflicts().keySet())) {
            return false;
        }

        return true;
    }

    @Override
    protected void describeMismatchSafely(Result item, Description description) {
        description.appendText("was Result")
                   .appendText("\n\tHas errors: ")
                   .appendValue(item.hasErrors())
                   .appendText("\n\tResolved features: ")
                   .appendValueList("[", ", ", "]", item.getResolvedFeatures())
                   .appendText("\n\tMissing features: ")
                   .appendValueList("[", ", ", "]", item.getMissing())
                   .appendText("\n\tNon public roots: ")
                   .appendValueList("[", ", ", "]", item.getNonPublicRoots())
                   .appendText("\n\tConflicts: ")
                   .appendValueList("[", ", ", "]", item.getConflicts().keySet());
    }

}
