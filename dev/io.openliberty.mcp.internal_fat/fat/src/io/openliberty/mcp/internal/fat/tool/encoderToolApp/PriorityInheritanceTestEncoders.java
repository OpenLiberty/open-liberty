/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.fat.tool.encoderToolApp;

import io.openliberty.mcp.content.Content;
import io.openliberty.mcp.content.ContentEncoder;
import io.openliberty.mcp.content.TextContent;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;

/**
 * Test encoders to verify that @Priority annotation is not inherited from superclasses.
 * This tests the fix for the encoder priority issue where the old implementation
 * incorrectly walked up the superclass hierarchy to find @Priority.
 */
public class PriorityInheritanceTestEncoders {

    private static final Jsonb jsonb = JsonbBuilder.create();

    public record InheritanceTestType(String message) {}

    /**
     * Base encoder with @Priority(200) - this priority should NOT be inherited by subclasses
     */
    @ApplicationScoped
    @Priority(200)
    public static class BaseEncoderWithPriority implements ContentEncoder<InheritanceTestType> {

        @Override
        public boolean supports(Class<?> runtimeType) {
            return InheritanceTestType.class.isAssignableFrom(runtimeType);
        }

        @Override
        public Content encode(InheritanceTestType value) {
            InheritanceTestType encodedValue = new InheritanceTestType("Encoded by BaseEncoderWithPriority (priority 200)");
            return new TextContent(jsonb.toJson(encodedValue));
        }
    }

    /**
     * Subclass encoder WITHOUT @Priority annotation.
     * Should get default priority (0), NOT inherit priority 200 from superclass.
     * With the fix, this encoder should have lower priority than BaseEncoderWithPriority.
     */
    @ApplicationScoped
    public static class SubclassEncoderNoPriority extends BaseEncoderWithPriority {

        @Override
        public Content encode(InheritanceTestType value) {
            InheritanceTestType encodedValue = new InheritanceTestType("Encoded by SubclassEncoderNoPriority (should be priority 0, not inherited 200)");
            return new TextContent(jsonb.toJson(encodedValue));
        }
    }

    /**
     * Subclass encoder WITH its own @Priority(50) annotation.
     * Should use priority 50, NOT inherit priority 200 from superclass.
     * With the fix, this encoder should have lower priority than BaseEncoderWithPriority (200)
     * but higher priority than SubclassEncoderNoPriority (0).
     */
    @ApplicationScoped
    @Priority(50)
    public static class SubclassEncoderWithOwnPriority extends BaseEncoderWithPriority {

        @Override
        public Content encode(InheritanceTestType value) {
            InheritanceTestType encodedValue = new InheritanceTestType("Encoded by SubclassEncoderWithOwnPriority (priority 50)");
            return new TextContent(jsonb.toJson(encodedValue));
        }
    }
}