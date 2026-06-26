/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.fat.encodertools.moduleLevelEncoder;

import io.openliberty.mcp.content.Content;
import io.openliberty.mcp.content.ContentEncoder;
import io.openliberty.mcp.content.TextContent;
import io.openliberty.mcp.internal.fat.encodertools.sharedEncoders.Person;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;

/**
 * Module-specific Person encoder for war1WithInitializedEvent.
 * This encoder adds a custom message to demonstrate module-level encoder isolation.
 */
@ApplicationScoped
public class PersonContentEncoder implements ContentEncoder<Person> {

    private static final Jsonb jsonb = JsonbBuilder.create();

    @Override
    public boolean supports(Class<?> runtimeType) {
        return Person.class.isAssignableFrom(runtimeType);
    }

    @Override
    public Content encode(Person person) {
        Person encodedPerson = new Person(person.fistName(), "Encoded by PersonContentEncoder", person.age());
        return new TextContent(jsonb.toJson(encodedPerson));
    }
}
