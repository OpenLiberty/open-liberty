/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.fat.tool.war1init;

import io.openliberty.mcp.annotations.Tool;
import io.openliberty.mcp.content.Content;
import io.openliberty.mcp.content.ContentEncoder;
import io.openliberty.mcp.content.TextContent;
import io.openliberty.mcp.internal.fat.tool.sharedEncoders.SharedEncoders.Person;
import io.openliberty.mcp.tools.ToolManager;
import io.openliberty.mcp.tools.ToolResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Initialized;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;

@ApplicationScoped
public class War1ToolBeanInitializedEvent {

    private static final Jsonb jsonb = JsonbBuilder.create();

    @Inject
    private ToolManager toolManager;

    @Tool
    public String methodTool() {
        return "War1ToolBeanInitializedEvent";
    }

    public void init(@Observes @Initialized(ApplicationScoped.class) Object initializationObject) {
        toolManager.newTool("apiTool")
                   .setHandler(a -> ToolResponse.success("War1ToolBeanInitializedEvent"))
                   .register();
    }

    @ApplicationScoped
    public static class PersonContentEncoder implements ContentEncoder<Person> {

        @Override
        public boolean supports(Class<?> runtimeType) {
            return Person.class.isAssignableFrom(runtimeType);
        }

        @Override
        public Content encode(Person person) {
            Person encodedPerson = new Person(person.fistName(), "Encoded by PersonContentEncoder in War1ToolBeanInitializedEvent", person.age());
            return new TextContent(jsonb.toJson(encodedPerson));
        }
    }

    /**
     * Tool to test that shared encoders from EAR/lib are accessible to all modules.
     * Uses Person class from SharedEncoders which has a corresponding PersonContentEncoder.
     */
    @Tool(name = "testContentEncoderSharing")
    public Person testContentEncoderSharing() {
        return new Person("Jon", "Doe", 32);
    }

    /**
     * A type that only war1WithInitializedEvent can encode
     * Fields are in alphabetical order for JSON-B serialization
     */
    public record Company(int employees, String industry, String name) {}

    @ApplicationScoped
    public static class CompanyContentEncoder implements ContentEncoder<Company> {

        @Override
        public boolean supports(Class<?> runtimeType) {
            return Company.class.isAssignableFrom(runtimeType);
        }

        @Override
        public Content encode(Company company) {
            Company encoded = new Company(
                                          company.employees,
                                          company.industry,
                                          company.name + " (encoded by War1)");
            return new TextContent(jsonb.toJson(encoded));
        }
    }

    /**
     * Tool that returns a Company object - should be encoded by CompanyContentEncoder
     * This encoder is ONLY available in war1WithInitializedEvent
     */
    @Tool(name = "testWar1SpecificEncoder", description = "Returns a Company object that can only be encoded in War1")
    public Company testWar1SpecificEncoder() {
        return new Company(350000, "Technology", "IBM");
    }

}
