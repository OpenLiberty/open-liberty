/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package com.ibm.ws.repository.resolver;

import java.util.Objects;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

import com.ibm.ws.repository.resolver.RepositoryResolutionException.MissingRequirement;
import com.ibm.ws.repository.resources.RepositoryResource;

/**
 * Verify a {@link MissingRequirement}
 */
public class MissingRequirementMatcher extends TypeSafeMatcher<MissingRequirement> {

    private final String requirementName;

    private final RepositoryResource owningResource;

    public static MissingRequirementMatcher missingRequirement(String requirementName, RepositoryResource owningResource) {
        return new MissingRequirementMatcher(requirementName, owningResource);
    }

    private MissingRequirementMatcher(String requirementName, RepositoryResource owningResource) {
        this.requirementName = requirementName;
        this.owningResource = owningResource;
    }

    /** {@inheritDoc} */
    @Override
    public void describeTo(Description desc) {
        desc.appendText("MissingRequirement [");
        desc.appendText("name = ").appendValue(requirementName);
        desc.appendText(", ");
        desc.appendText("owning resource = ").appendValue(owningResource);
        desc.appendText("]");
    }

    /** {@inheritDoc} */
    @Override
    protected void describeMismatchSafely(MissingRequirement item, Description desc) {
        desc.appendText("MissingRequirement [");
        desc.appendText("name = ").appendValue(item.getRequirementName());
        desc.appendText(", ");
        desc.appendText("owning resource = ").appendValue(item.getOwningResource());
        desc.appendText("]");
    }

    /** {@inheritDoc} */
    @Override
    protected boolean matchesSafely(MissingRequirement other) {
        return Objects.equals(requirementName, other.getRequirementName())
               && Objects.equals(owningResource, other.getOwningResource());
    }

}
