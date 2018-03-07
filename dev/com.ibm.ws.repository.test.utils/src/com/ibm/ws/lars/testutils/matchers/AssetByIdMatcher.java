/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
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

import com.ibm.ws.repository.transport.model.Asset;

/**
 * Matches Assets against each other by comparing only the IDs.
 * <p>
 * If you have added some assets and then retrieved them, you can check the list of retrieved assets
 * is correct with
 *
 * <pre>
 * assertThat(returnedAssets, containsInAnyOrder(byId(asset1), byId(asset2), ...))
 * </pre>
 */
public class AssetByIdMatcher extends TypeSafeMatcher<Asset> {

    private final String expectedId;

    private AssetByIdMatcher(String expectedId) {
        this.expectedId = expectedId;
    }

    /** {@inheritDoc} */
    @Override
    public void describeTo(Description description) {
        description.appendText("asset id should be ").appendValue(expectedId);
    }

    /** {@inheritDoc} */
    @Override
    protected boolean matchesSafely(Asset asset) {
        return asset.get_id().equals(expectedId);
    }

    @Factory
    public static AssetByIdMatcher hasId(String id) {
        return new AssetByIdMatcher(id);
    }

    @Factory
    public static AssetByIdMatcher hasId(Asset asset) {
        return new AssetByIdMatcher(asset.get_id());
    }

}
