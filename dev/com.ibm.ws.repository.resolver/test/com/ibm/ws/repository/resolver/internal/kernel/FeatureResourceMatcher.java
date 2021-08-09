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

import java.util.Arrays;
import java.util.List;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

import com.ibm.ws.kernel.feature.provisioning.FeatureResource;

/**
 * Checks a FeatureResource has the right symbolic name and tolerated versions
 */
public class FeatureResourceMatcher extends TypeSafeMatcher<FeatureResource> {

    private final List<String> tolerates;
    private final String symbolicName;

    public static FeatureResourceMatcher featureResource(String symbolicName, String... tolerates) {
        return new FeatureResourceMatcher(symbolicName, Arrays.asList(tolerates));
    }

    public FeatureResourceMatcher(String symbolicName, List<String> tolerates) {
        super();
        this.tolerates = tolerates;
        this.symbolicName = symbolicName;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("FeatureResource with symbolic name ")
                   .appendText(symbolicName)
                   .appendText(" and tolerates ")
                   .appendValueList("<", ", ", ">", tolerates);
    }

    @Override
    protected boolean matchesSafely(FeatureResource item) {
        if (!symbolicName.equals(item.getSymbolicName())) {
            return false;
        }

        if (!tolerates.equals(item.getTolerates())) {
            return false;
        }

        return true;
    }

    @Override
    protected void describeMismatchSafely(FeatureResource item, Description mismatchDescription) {
        mismatchDescription.appendText("FeatureResource with symbolic name ")
                           .appendText(item.getSymbolicName())
                           .appendText(" and tolerates ")
                           .appendValueList("<", ", ", ">", item.getTolerates());
    }

}
