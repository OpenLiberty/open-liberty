/*******************************************************************************
 * Copyright (c) 2015, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.collector.internal;

import java.util.Arrays;

/**
 * Base class for holding configuration data
 */
public class TaskConfig {

    private final String location;
    private final String sourceName;
    //private final Builder myBuilder;

    private final boolean enabled;

    private final String[] tags;

    private final int maxFieldLength;

    public static class Builder {

        /* Mandatory fields */
        private final String location;
        private final String source;

        /* Optional fields */
        private boolean enabled = TaskConstants.DEFAULT_TASK_STATUS;
        private String[] tags;

        private int maxFieldLength;

        public Builder(String source, String location) {
            this.source = source;
            this.location = location;
        }

        public Builder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public TaskConfig build() {
            return new TaskConfig(this);
        }

        public Builder tags(String[] tags) {

            if (tags != null) {
                this.tags = Arrays.copyOf(tags, tags.length);
            }

            return this;
        }

        public Builder maxFieldLength(int maxFieldLength) {
            this.maxFieldLength = maxFieldLength;
            return this;
        }

    }

    private TaskConfig(Builder builder) {

        location = builder.location;
        sourceName = builder.source;

        enabled = builder.enabled;
        tags = builder.tags;

        maxFieldLength = builder.maxFieldLength;
    }

    public String sourceId() {
        return sourceName + TaskConstants.SOURCE_ID_SEPARATOR + location;
    }

    public String getLocation() {
        return location;
    }

    public String getSourceName() {
        return sourceName;
    }

    public boolean getEnabled() {
        return enabled;
    }

    public String[] getTags() {

        return (tags != null) ? Arrays.copyOf(tags, tags.length) : null;
    }

    public int getMaxFieldLength() {
        return maxFieldLength;
    }

    @Override
    public String toString() {
        return "TaskConfig [location=" + location + ", sourceName=" + sourceName + ", enabled=" + enabled + ", tags=" + ((tags != null) ? Arrays.toString(tags) : tags)
               + ", maxFieldLength=" + maxFieldLength + "]";
    }

}
