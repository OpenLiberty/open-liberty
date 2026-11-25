/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.fat.utils;

import org.hamcrest.Description;
import org.json.JSONArray;
import org.json.JSONObject;
import org.skyscreamer.jsonassert.JSONCompare;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.JSONCompareResult;

/**
 * A part of a response that a matcher expects, together with how strictly to compare it. It is
 * shared by {@link ToolResponseMatcher} and {@link ToolsListResponseMatcher} so that both
 * store, describe and compare their expectations in the same way.
 *
 * @param json        the expected JSON: a {@link JSONObject} or a {@link JSONArray}. Any other type
 *                        is not meaningful as it is only used as its JSON text
 * @param compareMode how strictly the actual JSON is compared with the expected JSON
 */
record JsonExpectation(Object json, JSONCompareMode compareMode) {

    /** Appends {@code <name> of <expected json> (<compare mode>)}, for the description of a matcher. */
    void describeTo(Description description, String name) {
        description.appendText(name).appendText(" of ").appendText(json.toString())
                   .appendText(" (").appendText(compareMode.name()).appendText(")");
    }

    /**
     * Compares the actual JSON with the expected JSON using the compare mode.
     *
     * @param actual the actual JSON, a {@link JSONObject} or a {@link JSONArray}
     * @return {@code null} if the actual JSON matches, otherwise a description of why it does not,
     *         starting with {@code differed:} or {@code could not be compared:}
     */
    String difference(Object actual) {
        try {
            JSONCompareResult comparison = JSONCompare.compareJSON(json.toString(), actual.toString(), compareMode);
            return comparison.failed() ? "differed: " + comparison.getMessage() : null;
        } catch (Exception e) {
            return "could not be compared: " + e;
        }
    }
}
