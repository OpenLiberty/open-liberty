/*
 **********************************************************************
 * Copyright (c) 2018, 2019 Contributors to the Eclipse Foundation
 *               2018, 2019 IBM Corporation and others
 *               and other contributors as indicated by the @author tags.
 *
 * See the NOTICES file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 **********************************************************************/
package org.eclipse.microprofile.metrics;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Tag represents a singular metric tag key and value pair.
 *
 * The Tag contains:
 * <ul>
 * <li>
 * {@code TagName}: (Required) The name of the tag. Must match the regex [a-zA-Z_][a-zA-Z0-9_]*.
 * </li>
 * <li>
 * {@code TagValue}: (Required) The value of the tag.
 * </li>
 * </ul>
 *
 */
public class Tag {

    /**
     * Name of the Tag. Must match the regex [a-zA-Z_][a-zA-Z0-9_]*.
     * <p>
     * A required field which holds the name of the tag.
     * </p>
     */
    private final String tagName;

    /**
     * Value of the Tag.
     * <p>
     * A required field which holds the value of the tag.
     * </p>
     */
    private final String tagValue;
    /**
     * Pattern that matches regex [a-zA-Z_][a-zA-Z0-9_]*
     */
    private static final Pattern PATTERN = Pattern.compile("[a-zA-Z_][a-zA-Z0-9_]*");

    /**
     * Constructs the Tag object with the given tag name and tag value
     *
     * @param tagName The tag name, must match the regex [a-zA-Z_][a-zA-Z0-9_]*.
     * @param tagValue The tag value
     * @throws IllegalArgumentException If the tagName does not match [a-zA-Z_][a-zA-Z0-9_]*
     */
    public Tag(String tagName, String tagValue) throws IllegalArgumentException {
        if (tagName == null ||
            tagValue == null ||
            !PATTERN.matcher(tagName).matches()) {
            throw new IllegalArgumentException("Invalid Tag name. Tag names must match the following regex [a-zA-Z_][a-zA-Z0-9_]*");
        }
        this.tagName = tagName;
        this.tagValue = tagValue;
    }

    /**
     * @return the tagName
     */
    public String getTagName() {
        return tagName;
    }

    /**
     * @return the tagValue
     */
    public String getTagValue() {
        return tagValue;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return Objects.hash(tagName, tagValue);
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Tag)) {
            return false;
        }
        Tag that = (Tag) o;
        return Objects.equals(this.tagName, that.getTagName()) && Objects.equals(this.tagValue, that.getTagValue());
    }

    @Override
    public String toString() {
        return "Tag{"
            + tagName + '=' + tagValue
            + '}';
    }

}
