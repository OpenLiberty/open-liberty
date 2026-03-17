/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.fat.tool.encoderToolApp;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import io.openliberty.mcp.annotations.Tool;
import io.openliberty.mcp.annotations.ToolArg;
import io.openliberty.mcp.content.Content;
import io.openliberty.mcp.content.ContentEncoder;
import io.openliberty.mcp.content.TextContent;
import io.openliberty.mcp.tools.ToolResponse;
import io.openliberty.mcp.tools.ToolResponseEncoder;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Singleton;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;

@ApplicationScoped
public class EncoderTools {
    private static final Jsonb jsonb = JsonbBuilder.create();

    /*******************************************************************************
     * Test encoding when there is no encoder for the specified type
     *******************************************************************************/

    public record City(String name, String country, int population, boolean isCapital) {};

    @Tool(name = "testDefaultEncoderResponse", title = "Create a city",
          description = "A tool to return a city object encoded by the default built in Json encoder")
    public City testDefaultEncoderResponse(@ToolArg(name = "name", description = "name of your city") String name) {
        return new City(name, "England", 8000, false);
    }

    /*******************************************************************************
     * Test encoding with a given content encoder
     *******************************************************************************/

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

    @Tool(name = "testContentEncoder", description = "tests that a Person object is encoded to content correctly by the PersonContentEncoder")
    public Person testContentEncoder() {
        return new Person("Jon", "Doe", 32);
    }

    /*******************************************************************************
     * Test encoding a list of objects with a given content encoder
     *******************************************************************************/

    @Tool(name = "testContentEncoderEncodingAList", description = "tests that a Person object is encoded to content correctly by the PersonContentEncoder")
    public List<Person> testContentEncoderEncodingAList() {
        return List.of(new Person("Jon", "Doe", 32));
    }

    /*******************************************************************************
     * Test encoding with more than 1 encoders specified for the same type, the
     * priority value has been specified, a high priority encoder is used
     *******************************************************************************/

    public record PriorityOrderTestType(String hello) {}

    @ApplicationScoped
    @Priority(100)
    public static class HigerPriorityEncoder implements ContentEncoder<PriorityOrderTestType> {

        @Override
        public boolean supports(Class<?> runtimeType) {
            return PriorityOrderTestType.class.isAssignableFrom(runtimeType);
        }

        @Override
        public Content encode(PriorityOrderTestType value) {
            PriorityOrderTestType encodedValue = new PriorityOrderTestType("Hello from HigerPriorityEncoder");
            return new TextContent(jsonb.toJson(encodedValue));
        }
    }

    @ApplicationScoped
    @Priority(5)
    public static class LowerPriorityEncoder implements ContentEncoder<PriorityOrderTestType> {

        @Override
        public boolean supports(Class<?> runtimeType) {
            return PriorityOrderTestType.class.isAssignableFrom(runtimeType);
        }

        @Override
        public Content encode(PriorityOrderTestType value) {
            PriorityOrderTestType encodedValue = new PriorityOrderTestType("Hello from LowerPriorityEncoder");
            return new TextContent(jsonb.toJson(encodedValue));
        }

    }

    @Tool(name = "testEncoderPriority", description = "tests that given 2 encoders encoding the same type, the one with a higher priority is used")
    public PriorityOrderTestType testEncoderPriority() {
        return new PriorityOrderTestType("Hello");
    }

    /*******************************************************************************
     * Test encoding with a ToolResponseEncoder for complete control over the
     * success/error responses
     *******************************************************************************/

    public record DatabaseQueryResult(boolean isSuccessfull,
                                      String errorMessage,
                                      int rowCount,
                                      List<Object> rows) {}

    @ApplicationScoped
    public static class QueryResultEncoder implements ToolResponseEncoder<DatabaseQueryResult> {

        @Override
        public boolean supports(Class<?> runtimeType) {
            return DatabaseQueryResult.class.isAssignableFrom(runtimeType);
        }

        @Override
        public ToolResponse encode(DatabaseQueryResult result) {

            if (!result.isSuccessfull) {
                return ToolResponse.error("Database Query failed with error: " + result.errorMessage);
            }

            List<Content> queryResultContents = new ArrayList<>();

            if (result.rowCount > 0) {
                queryResultContents.add(new TextContent(jsonb.toJson(result.rows)));
            }

            return ToolResponse.success(queryResultContents);
        }
    }

    @Tool(name = "testToolResponseEncoder", description = "tests if a tool response is returned properly encoded")
    public DatabaseQueryResult testToolResponseEncoder(@ToolArg(name = "isSuccessful") boolean isSuccessful) {
        List<Object> objectsFromDB = List.of(new City("London", "England", 18000, true),
                                             new City("Machester", "England", 8000, false));
        if (isSuccessful) {
            return new DatabaseQueryResult(isSuccessful, null, objectsFromDB.size(), objectsFromDB);
        }
        return new DatabaseQueryResult(isSuccessful, "Some SQL execution error", 0, null);
    }

    /*******************************************************************************
     * Test encoding with @Dependent type of encoder bean annotation
     *******************************************************************************/

    public record DependentBeanAnnotationTestType(String hello) {}

    @Dependent
    public static class DependantBeanEncoder implements ContentEncoder<DependentBeanAnnotationTestType> {

        @Override
        public boolean supports(Class<?> runtimeType) {
            return DependentBeanAnnotationTestType.class.isAssignableFrom(runtimeType);
        }

        @Override
        public Content encode(DependentBeanAnnotationTestType value) {
            DependentBeanAnnotationTestType encodedValue = new DependentBeanAnnotationTestType(value.hello + " from DependantBeanEncoder");
            return new TextContent(jsonb.toJson(encodedValue));
        }
    }

    @Tool(name = "testDependantBeanEncoder", description = "tests that an encoder annotated with @Dependent will be discovered and used")
    public DependentBeanAnnotationTestType testDependantBeanEncoder() {
        return new DependentBeanAnnotationTestType("Hello");
    }

    /*******************************************************************************
     * Test encoding with @Singleton type of encoder bean annotation
     *******************************************************************************/

    public record SingletonBeanAnnotationTestType(String hello) {}

    @Singleton
    public static class SingletonBeanEncoder implements ContentEncoder<SingletonBeanAnnotationTestType> {

        @Override
        public boolean supports(Class<?> runtimeType) {
            return SingletonBeanAnnotationTestType.class.isAssignableFrom(runtimeType);
        }

        @Override
        public Content encode(SingletonBeanAnnotationTestType value) {
            SingletonBeanAnnotationTestType encodedValue = new SingletonBeanAnnotationTestType(value.hello + " from SingletonBeanEncoder");
            return new TextContent(jsonb.toJson(encodedValue));
        }
    }

    @Tool(name = "testSingletonBeanEncoder", description = "tests that an encoder annotated with @Singleton will NOT be discovered")
    public SingletonBeanAnnotationTestType testSingletonBeanEncoder() {
        return new SingletonBeanAnnotationTestType("Hello");
    }

    /*******************************************************************************
     * Test encoding with @RequestScoped type of encoder bean annotation
     *******************************************************************************/

    public record RequestScopedBeanAnnotationTestType(String hello) {}

    @RequestScoped
    public static class RequestScopedBeanEncoder implements ContentEncoder<RequestScopedBeanAnnotationTestType> {

        @Override
        public boolean supports(Class<?> runtimeType) {
            return RequestScopedBeanAnnotationTestType.class.isAssignableFrom(runtimeType);
        }

        @Override
        public Content encode(RequestScopedBeanAnnotationTestType value) {
            RequestScopedBeanAnnotationTestType encodedValue = new RequestScopedBeanAnnotationTestType(value.hello + " from RequestScopedBeanEncoder");
            return new TextContent(jsonb.toJson(encodedValue));
        }
    }

    @Tool(name = "testRequestScopedBeanEncoder", description = "tests that an encoder annotated with @RequestScoped will be discovered")
    public RequestScopedBeanAnnotationTestType testRequestScopedBeanEncoder() {
        return new RequestScopedBeanAnnotationTestType("Hello");
    }

    /*******************************************************************************
     * Test encoding with @SessionScoped type of encoder bean annotation
     * SessionScoped
     ***********************************************************************/

    public record SessionScopedBeanAnnotationTestType(String hello) {}

    @SessionScoped
    public static class SessionScopedBeanEncoder implements ContentEncoder<SessionScopedBeanAnnotationTestType>, Serializable {

        private static final long serialVersionUID = 1L;

        @Override
        public boolean supports(Class<?> runtimeType) {
            return SessionScopedBeanAnnotationTestType.class.isAssignableFrom(runtimeType);
        }

        @Override
        public Content encode(SessionScopedBeanAnnotationTestType value) {
            SessionScopedBeanAnnotationTestType encodedValue = new SessionScopedBeanAnnotationTestType(value.hello + " from SessionScopedBeanEncoder");
            return new TextContent(jsonb.toJson(encodedValue));
        }
    }

    @Tool(name = "testSessionScopedBeanEncoder", description = "tests that an encoder annotated with @SessionScoped will be discovered")
    public SessionScopedBeanAnnotationTestType testSessionScopedBeanEncoder() {
        return new SessionScopedBeanAnnotationTestType("Hello");
    }

    /*******************************************************************************
     * Test that a ToolResponseEncoder and a ContentEncoder encoding the same type,
     * ToolResponseEncoder takes priority
     *******************************************************************************/

    public record HttpEndpointResponse(boolean isSuccessfull,
                                       int statusCode,
                                       String content,
                                       String errorMessage) {}

    @ApplicationScoped
    public static class RestResponseToolResponseEncoder implements ToolResponseEncoder<HttpEndpointResponse> {

        @Override
        public boolean supports(Class<?> runtimeType) {
            return HttpEndpointResponse.class.isAssignableFrom(runtimeType);
        }

        @Override
        public ToolResponse encode(HttpEndpointResponse response) {
            if (!response.isSuccessfull) {
                return ToolResponse.error("Endpoint request failed with error: " + response.errorMessage);
            }

            HttpEndpointResponse encodedResponse = new HttpEndpointResponse(response.isSuccessfull,
                                                                            response.statusCode,
                                                                            "Encoded by RestResponseToolResponseEncoder",
                                                                            null);
            Content content = new TextContent(jsonb.toJson(encodedResponse));
            return ToolResponse.success(content);
        }
    }

    @ApplicationScoped
    public static class RestResponseContentEncoder implements ContentEncoder<HttpEndpointResponse> {

        @Override
        public boolean supports(Class<?> runtimeType) {
            return HttpEndpointResponse.class.isAssignableFrom(runtimeType);
        }

        @Override
        public Content encode(HttpEndpointResponse response) {

            HttpEndpointResponse encodedResponse = new HttpEndpointResponse(response.isSuccessfull,
                                                                            response.statusCode,
                                                                            "Encoded by RestResponseContentEncoder",
                                                                            null);
            return new TextContent(jsonb.toJson(encodedResponse));
        }
    }

    @Tool(name = "testToolResponseEncoderPriorityOverContentEncoder",
          description = "tests if a tool response encoder and a content encoder are used for the same type, a ToolResponseEncoder takes priority")
    public HttpEndpointResponse testToolResponseEncoderPriorityOverContentEncoder(@ToolArg(name = "isSuccessful") boolean isSuccessful) {

        if (isSuccessful) {
            return new HttpEndpointResponse(isSuccessful, 200, "", null);
        }
        return new HttpEndpointResponse(isSuccessful, 500, "", "Internal Server Error");
    }

}
