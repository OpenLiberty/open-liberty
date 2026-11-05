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

import java.util.List;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.json.JSONObject;

/**
 * Hamcrest matchers for asserting on a {@link ToolResponse} without spelling out the whole
 * JSON-RPC response.
 *
 * <pre>{@code
 * ToolResponse response = client.callTool("myTool");
 * assertThat(response, ToolResponseMatcher.withTextContent("expected text"));
 * }</pre>
 */
public class ToolResponseMatcher extends TypeSafeDiagnosingMatcher<ToolResponse> {

    private final String expectedText;

    private ToolResponseMatcher(String expectedText) {
        this.expectedText = expectedText;
    }

    /**
     * Matches a successful tool response ({@code isError} is not {@code true}) whose content is
     * exactly one {@code text} item with exactly the given text.
     */
    public static ToolResponseMatcher withTextContent(String expectedText) {
        return new ToolResponseMatcher(expectedText);
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("a successful tool response with a single text content of ").appendValue(expectedText);
    }

    @Override
    protected boolean matchesSafely(ToolResponse response, Description mismatch) {
        if (response.getResult() == null) {
            mismatch.appendText("had no result: ").appendText(response.toString());
            return false;
        }
        if (response.isError()) {
            mismatch.appendText("was a tool error: ").appendText(response.toString());
            return false;
        }
        List<JSONObject> content = response.getContent();
        if (content.size() != 1) {
            mismatch.appendText("had ").appendValue(content.size()).appendText(" content items: ").appendText(response.toString());
            return false;
        }
        JSONObject item = content.get(0);
        if (item == null || !"text".equals(item.optString("type"))) {
            mismatch.appendText("content was not of type text: ").appendText(response.toString());
            return false;
        }
        String actualText = item.optString("text", null);
        if (!expectedText.equals(actualText)) {
            mismatch.appendText("text content was ").appendValue(actualText);
            return false;
        }
        return true;
    }
}
