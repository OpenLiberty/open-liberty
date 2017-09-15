/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.websphere.event;

import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import com.ibm.ws.event.internal.TopicData;

/**
 * Representation of a topic to be used by event sources.
 */
// TODO: Evaluate removal of this object from the programming model.
// The performance impact of using strings in the topic map may not
// be an issue.
public final class Topic {

    private final static Pattern topicPattern = Pattern.compile("[\\w\\-]+(/[\\w\\-]+)*");

    final String name;

    // Double nesting of the atomic reference since we're forced to expose the
    // reference itself - and that needs memory model compliance too
    final AtomicReference<AtomicReference<TopicData>> topicDataReference = new AtomicReference<AtomicReference<TopicData>>(new AtomicReference<TopicData>());

    public Topic(String name) {
        validateTopic(name);
        this.name = name;
    }

    void validateTopic(String name) {
        if (name == null) {
            throw new NullPointerException("Topic name must not be null");
        }

        if (!topicPattern.matcher(name).matches()) {
            throw new IllegalArgumentException("\"" + name + "\" is not a valid topic name");
        }
    }

    public String getName() {
        return name;
    }

    // TODO: Hide the topicData from code outside the bundle
    public TopicData getTopicData() {
        return topicDataReference.get().get();
    }

    // TODO: Hide the topicData from code outside the bundle
    public void setTopicDataReference(AtomicReference<TopicData> topicDataReference) {
        this.topicDataReference.set(topicDataReference);
    }

    public String toString() {
        return name;
    }

    public int hashCode() {
        return name.hashCode();
    }

    public boolean equals(Object o) {
        if (o instanceof Topic) {
            Topic that = (Topic) o;
            return this.name.equals(that.name);
        }
        return false;
    }
}
