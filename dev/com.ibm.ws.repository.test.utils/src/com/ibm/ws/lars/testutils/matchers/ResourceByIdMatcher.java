/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.lars.testutils.matchers;

import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.TypeSafeMatcher;

import com.ibm.ws.repository.resources.RepositoryResource;

/**
 * Matches Resources against each other by comparing only the IDs.
 * <p>
 * If you have added some resources and then retrieved them, you can check the list of retrieved
 * resources is correct with
 *
 * <pre>
 * assertThat(returnedResources, containsInAnyOrder(byId(res1), byId(res2), ...))
 * </pre>
 */
public class ResourceByIdMatcher extends TypeSafeMatcher<RepositoryResource> {

    private final String expectedId;

    private ResourceByIdMatcher(String expectedId) {
        this.expectedId = expectedId;
    }

    /** {@inheritDoc} */
    @Override
    public void describeTo(Description description) {
        description.appendText("resource id should be ").appendValue(expectedId);
    }

    /** {@inheritDoc} */
    @Override
    protected boolean matchesSafely(RepositoryResource resource) {
        return resource.getId().equals(expectedId);
    }

    @Factory
    public static ResourceByIdMatcher hasId(String id) {
        return new ResourceByIdMatcher(id);
    }

    @Factory
    public static ResourceByIdMatcher hasId(RepositoryResource res) {
        return new ResourceByIdMatcher(res.getId());
    }

}
