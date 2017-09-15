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
package com.ibm.websphere.event;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class TopicTest {

    @Test(expected = NullPointerException.class)
    public void testTopicNullTopic() {
        new Topic(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testTopicTrailingSlash() {
        new Topic("topic/");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testTopicTrailingDoubleSlash() {
        new Topic("topic//");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testTopicLeadingSlash() {
        new Topic("/topic");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testTopicLeadingDoubleSlash() {
        new Topic("//topic");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testTopicEmptyString() {
        new Topic("");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testTopicEmptyLevel() {
        new Topic("com//ibm");
    }

    @Test
    public void testHashCode() {
        String topicName = "com/ibm/websphere/context/test/Topic";
        Topic topic = new Topic(topicName);
        Topic topic1 = new Topic(topicName);

        assertEquals(topicName.hashCode(), topic.hashCode());
        assertEquals(topic.hashCode(), topic1.hashCode());
    }

    @Test
    public void testGetName() {
        String topicName = "com/ibm/websphere/context/test/Topic";
        Topic topic = new Topic(topicName);

        assertEquals(topicName, topic.getName());
    }

    @Test
    public void testToString() {
        String topicName = "com/ibm/websphere/context/test/Topic";
        Topic topic = new Topic(topicName);
        String string = topic.toString();

        assertNotNull(string);
        assertTrue(string.contains(topic.getName()));
    }

    @Test
    public void testEqualsObject() {
        String topicName = "com/ibm/websphere/context/test/Topic";
        Topic topic = new Topic(topicName);
        Topic topic1 = new Topic(topicName);
        Topic topic2 = new Topic(topicName + "1");

        assertNotSame(topic, topic1);
        assertEquals(topic, topic1);
        assertFalse(topic1.equals(topic2));
        assertFalse(topic1.equals(new Object()));
    }

}
