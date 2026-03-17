/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.fat.tool.ejbjar;

import io.openliberty.mcp.annotations.Tool;
import io.openliberty.mcp.content.Content;
import io.openliberty.mcp.content.ContentEncoder;
import io.openliberty.mcp.content.TextContent;
import io.openliberty.mcp.internal.fat.tool.sharedEncoders.SharedEncoders.Person;
import io.openliberty.mcp.tools.ToolManager;
import io.openliberty.mcp.tools.ToolResponse;
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
                   .setHandler(a -> ToolResponse.success("From EarToolBean"))
                   .register();
    }

    /* This is never picked up even though it has a high priority */
    @ApplicationScoped
    @Priority(5000)
    public static class PersonContentEncoder implements ContentEncoder<Person> {

        @Override
        public boolean supports(Class<?> runtimeType) {
            return Person.class.isAssignableFrom(runtimeType);
        }

        @Override
        public Content encode(Person person) {
            Person encodedPerson = new Person(person.fistName(), "Encoded by PersonContentEncoder in EarToolBean", person.age());
            return new TextContent(jsonb.toJson(encodedPerson));
        }
    }
}
