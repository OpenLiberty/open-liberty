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
package com.ibm.ws.repository.common.enums.test;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collection;

import org.junit.Test;

import com.ibm.ws.repository.common.enums.FilterPredicate;
import com.ibm.ws.repository.common.enums.FilterableAttribute;
import com.ibm.ws.repository.common.enums.ResourceType;
import com.ibm.ws.repository.common.enums.Visibility;
import com.ibm.ws.repository.exceptions.RepositoryBackendException;

public class FilterPredicateTest {

    /**
     * Test that all FilterableAttributes can be used in a FilterPredicate supplied to
     * getMatchingResources. This implicitly tests the FilterPredicate as well.
     */
    @Test
    public void testFilterPredicates() throws RepositoryBackendException {

        // This test should test all the possible attributes, so assert that the number
        // of values hasn't changed. If it does, add a new test!
        assertTrue("An attribute has been added/or removed, so a new test is needed (or one removed)",
                   FilterableAttribute.values().length == 9);

        FilterPredicate pred = FilterPredicate.areEqual(FilterableAttribute.TYPE, ResourceType.ADDON);
        checkPredicateType(pred, FilterableAttribute.TYPE, ResourceType.ADDON.getValue());

        pred = FilterPredicate.areEqual(FilterableAttribute.PRODUCT_ID, "id");
        checkPredicateType(pred, FilterableAttribute.PRODUCT_ID, "id");

        pred = FilterPredicate.areEqual(FilterableAttribute.VISIBILITY, Visibility.INSTALL);
        checkPredicateType(pred, FilterableAttribute.VISIBILITY, Visibility.INSTALL.toString());

        pred = FilterPredicate.areEqual(FilterableAttribute.PRODUCT_MIN_VERSION, "8550");
        checkPredicateType(pred, FilterableAttribute.PRODUCT_MIN_VERSION, "8550");

        pred = FilterPredicate.areEqual(FilterableAttribute.PRODUCT_HAS_MAX_VERSION, Boolean.FALSE);
        checkPredicateType(pred, FilterableAttribute.PRODUCT_HAS_MAX_VERSION, Boolean.FALSE.toString());

        pred = FilterPredicate.areEqual(FilterableAttribute.SYMBOLIC_NAME, "sym");
        checkPredicateType(pred, FilterableAttribute.SYMBOLIC_NAME, "sym");

        pred = FilterPredicate.areEqual(FilterableAttribute.SHORT_NAME, "short");
        checkPredicateType(pred, FilterableAttribute.SHORT_NAME, "short");

        pred = FilterPredicate.areEqual(FilterableAttribute.LOWER_CASE_SHORT_NAME, "lower");
        checkPredicateType(pred, FilterableAttribute.LOWER_CASE_SHORT_NAME, "lower");

        pred = FilterPredicate.areEqual(FilterableAttribute.VANITY_URL, "example.com");
        checkPredicateType(pred, FilterableAttribute.VANITY_URL, "example.com");

        try {
            FilterPredicate.areEqual(FilterableAttribute.VANITY_URL, Boolean.FALSE);
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), containsString("The value must be of the correct type for the FilterableAttribute"));
            return;
        }
        fail("The expected exception was not thrown");
    }

    private static void checkPredicateType(FilterPredicate pred, FilterableAttribute type, String expectedValue) {
        assertEquals("The predicate's attribute type was not set correctly", type, pred.getAttribute());
        Collection<String> values = pred.getValues();
        assertEquals("There should only have been 1 value in the values collection", 1, values.size());
        assertEquals("The predicate contained the wrong value", expectedValue, values.iterator().next());
    }

}
