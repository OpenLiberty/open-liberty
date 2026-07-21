/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.fat.encodertools.ejbjarEncoder;

import org.mcpjava.server.ContentEncoder;
import org.mcpjava.server.content.ContentBlock;
import org.mcpjava.server.content.TextContent;
import org.mcpjava.server.tools.ToolResponse;

import org.mcpjava.server.tools.Tool;
import io.openliberty.mcp.internal.fat.encodertools.sharedEncoders.Person;
import io.openliberty.mcp.tools.ToolManager;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.Startup;
import jakarta.inject.Inject;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;

@ApplicationScoped
public class EarToolBean {

    private static final Jsonb jsonb = JsonbBuilder.create();

    @Inject
    private ToolManager toolManager;

    @Tool
    public String ejbJarMethodTool() {
        return "From EarToolBean";
    }

    void startup(@Observes Startup startup) {
        toolManager.newTool("ejbJarApiTool")
                   .setHandler(a -> ToolResponse.ofText("From EarToolBean"))
                   .register();
    }

    /* This is never picked up even though it has a high priority */
    @ApplicationScoped
    @Priority(5000)
    public static class PersonContentEncoder implements ContentEncoder<Person> {

        @Override
        public Class<Person> getType() {
            return Person.class;
        }

        @Override
        public ContentBlock encode(Person person) {
            Person encodedPerson = new Person(person.fistName(), "Encoded by PersonContentEncoder in EarToolBean", person.age());
            return TextContent.of(jsonb.toJson(encodedPerson));
        }
    }
}
