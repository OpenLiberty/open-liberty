/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.fat.tool.sharedEncoders;

import io.openliberty.mcp.content.Content;
import io.openliberty.mcp.content.ContentEncoder;
import io.openliberty.mcp.content.TextContent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;

/**
 * Shared encoders that can be placed in EAR/lib for use across multiple WAR modules.
 * 
 * IMPORTANT: This class contains ONLY encoder implementations, NO @Tool annotations.
 * Tools with @Tool annotations require module association and cannot be in EAR libraries.
 */
public class SharedEncoders {
    private static final Jsonb jsonb = JsonbBuilder.create();

    public record Person(String fistName, String lastName, int age) {}

    @ApplicationScoped
    public static class PersonContentEncoder implements ContentEncoder<Person> {

        @Override
        public boolean supports(Class<?> runtimeType) {
            return Person.class.isAssignableFrom(runtimeType);
        }

        @Override
        public Content encode(Person person) {
            Person encodedPerson = new Person(person.fistName, "Encoded by PersonContentEncoder", person.age);
            return new TextContent(jsonb.toJson(encodedPerson));
        }
    }
}
