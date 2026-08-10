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
import java.util.List;

import org.mcpjava.server.ContentEncoder;
import org.mcpjava.server.content.ContentBlock;
import org.mcpjava.server.content.TextContent;
import org.mcpjava.server.tools.Tool;
import org.mcpjava.server.tools.ToolArg;
import org.mcpjava.server.tools.ToolResponse;

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
        public Class<Person> getType() {
            return Person.class;
        }

        @Override
        public ContentBlock encode(Person person) {
            Person encodedPerson = new Person(person.fistName, "Encoded by PersonContentEncoder", person.age);
            return TextContent.of(jsonb.toJson(encodedPerson));
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
        public Class<PriorityOrderTestType> getType() {
            return PriorityOrderTestType.class;
        }

        @Override
        public ContentBlock encode(PriorityOrderTestType value) {
            PriorityOrderTestType encodedValue = new PriorityOrderTestType("Hello from HigerPriorityEncoder");
            return TextContent.of(jsonb.toJson(encodedValue));
        }
    }

    @ApplicationScoped
    @Priority(5)
    public static class LowerPriorityEncoder implements ContentEncoder<PriorityOrderTestType> {

        @Override
        public Class<PriorityOrderTestType> getType() {
            return PriorityOrderTestType.class;
        }

        @Override
        public ContentBlock encode(PriorityOrderTestType value) {
            PriorityOrderTestType encodedValue = new PriorityOrderTestType("Hello from LowerPriorityEncoder");
            return TextContent.of(jsonb.toJson(encodedValue));
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
        public Class<DatabaseQueryResult> getType() {
            return DatabaseQueryResult.class;
        }

        @Override
        public ToolResponse encode(DatabaseQueryResult result) {

            if (!result.isSuccessfull) {
                return ToolResponse.ofError("Database Query failed with error: " + result.errorMessage);
            }

            return ToolResponse.ofText(jsonb.toJson(result.rows));
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
        public Class<DependentBeanAnnotationTestType> getType() {
            return DependentBeanAnnotationTestType.class;
        }

        @Override
        public ContentBlock encode(DependentBeanAnnotationTestType value) {
            DependentBeanAnnotationTestType encodedValue = new DependentBeanAnnotationTestType(value.hello + " from DependantBeanEncoder");
            return TextContent.of(jsonb.toJson(encodedValue));
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
        public Class<SingletonBeanAnnotationTestType> getType() {
            return SingletonBeanAnnotationTestType.class;
        }

        @Override
        public ContentBlock encode(SingletonBeanAnnotationTestType value) {
            SingletonBeanAnnotationTestType encodedValue = new SingletonBeanAnnotationTestType(value.hello + " from SingletonBeanEncoder");
            return TextContent.of(jsonb.toJson(encodedValue));
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
        public Class<RequestScopedBeanAnnotationTestType> getType() {
            return RequestScopedBeanAnnotationTestType.class;
        }

        @Override
        public ContentBlock encode(RequestScopedBeanAnnotationTestType value) {
            RequestScopedBeanAnnotationTestType encodedValue = new RequestScopedBeanAnnotationTestType(value.hello + " from RequestScopedBeanEncoder");
            return TextContent.of(jsonb.toJson(encodedValue));
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
        public Class<SessionScopedBeanAnnotationTestType> getType() {
            return SessionScopedBeanAnnotationTestType.class;
        }

        @Override
        public ContentBlock encode(SessionScopedBeanAnnotationTestType value) {
            SessionScopedBeanAnnotationTestType encodedValue = new SessionScopedBeanAnnotationTestType(value.hello + " from SessionScopedBeanEncoder");
            return TextContent.of(jsonb.toJson(encodedValue));
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
        public Class<HttpEndpointResponse> getType() {
            return HttpEndpointResponse.class;
        }

        @Override
        public ToolResponse encode(HttpEndpointResponse response) {
            if (!response.isSuccessfull) {
                return ToolResponse.ofError("Endpoint request failed with error: " + response.errorMessage);
            }

            HttpEndpointResponse encodedResponse = new HttpEndpointResponse(response.isSuccessfull,
                                                                            response.statusCode,
                                                                            "Encoded by RestResponseToolResponseEncoder",
                                                                            null);
            return ToolResponse.ofText(jsonb.toJson(encodedResponse));
        }
    }

    @ApplicationScoped
    public static class RestResponseContentEncoder implements ContentEncoder<HttpEndpointResponse> {

        @Override
        public Class<HttpEndpointResponse> getType() {
            return HttpEndpointResponse.class;
        }

        @Override
        public ContentBlock encode(HttpEndpointResponse response) {

            HttpEndpointResponse encodedResponse = new HttpEndpointResponse(response.isSuccessfull,
                                                                            response.statusCode,
                                                                            "Encoded by RestResponseContentEncoder",
                                                                            null);
            return TextContent.of(jsonb.toJson(encodedResponse));
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

    public record InheritanceTestType(String message) {}

    @Priority(400)
    // This base class does not need to be an encoder.
    // A @Priority annotation on a superclass should not affect the priority of an encoder.
    public static class BaseEncoderWithPriority {}

    /**
     * Subclass encoder WITHOUT @Priority annotation.
     *
     * This encoder should get the default priority, not priority 400 from
     * BaseEncoderWithPriority.
     */
    @ApplicationScoped
    public static class SubclassEncoderNoPriority extends BaseEncoderWithPriority implements ContentEncoder<InheritanceTestType> {

        @Override
        public Class<InheritanceTestType> getType() {
            return InheritanceTestType.class;
        }

        @Override
        public ContentBlock encode(InheritanceTestType value) {
            InheritanceTestType encodedValue = new InheritanceTestType("Encoded by SubclassEncoderNoPriority (should be priority 0, not inherited 400)");
            return TextContent.of(jsonb.toJson(encodedValue));
        }
    }

    /**
     * Subclass encoder WITH its own @Priority(50) annotation.
     *
     * This encoder should use its own direct priority 50. It should not inherit
     * priority 400 from BaseEncoderWithPriority.
     */
    @ApplicationScoped
    @Priority(50)
    public static class SubclassEncoderWithOwnPriority extends BaseEncoderWithPriority implements ContentEncoder<InheritanceTestType> {

        @Override
        public Class<InheritanceTestType> getType() {
            return InheritanceTestType.class;
        }

        @Override
        public ContentBlock encode(InheritanceTestType value) {
            InheritanceTestType encodedValue = new InheritanceTestType("Encoded by BaseEncoderWithPriority (priority 50)");
            return TextContent.of(jsonb.toJson(encodedValue));
        }
    }

    @Tool(name = "testPriorityNotInherited",
          description = "tests that @Priority annotation is not inherited from superclass")
    public InheritanceTestType testPriorityNotInherited() {
        return new InheritanceTestType("Original message");
    }

    /*******************************************************************************
     * Test that a CDI base ToolResponseEncoder with the highest priority is selected
     *******************************************************************************/

    public record CdiBasePriorityTestType(String message) {}

    /**
     * This base class IS a CDI bean and IS an encoder.
     * It has the highest direct @Priority, so it should be selected.
     */
    @ApplicationScoped
    @Priority(400)
    public static class CdiBaseEncoderWithHighestPriority implements ToolResponseEncoder<CdiBasePriorityTestType> {

        @Override
        public Class<CdiBasePriorityTestType> getType() {
            return CdiBasePriorityTestType.class;
        }

        @Override
        public ToolResponse encode(CdiBasePriorityTestType value) {
            CdiBasePriorityTestType encodedValue = new CdiBasePriorityTestType("Encoded by CdiBaseEncoderWithHighestPriority (priority 400)");

            return ToolResponse.ofText(jsonb.toJson(encodedValue));
        }
    }

    /**
     * This child class is also a CDI encoder, but it has lower priority.
     * It should NOT be selected because the base encoder has priority 400.
     */
    @ApplicationScoped
    @Priority(50)
    public static class CdiSubclassEncoderWithLowerPriority extends CdiBaseEncoderWithHighestPriority {

        @Override
        public ToolResponse encode(CdiBasePriorityTestType value) {
            CdiBasePriorityTestType encodedValue = new CdiBasePriorityTestType("Encoded by CdiSubclassEncoderWithLowerPriority (priority 50)");

            return ToolResponse.ofText(jsonb.toJson(encodedValue));
        }
    }

    @Tool(name = "testCdiBasePriorityHighest",
          description = "tests that a CDI base ToolResponseEncoder with the highest priority is selected")
    public CdiBasePriorityTestType testCdiBasePriorityHighest() {
        return new CdiBasePriorityTestType("Original message");
    }

    /*******************************************************************************
     * Test that a ToolResponseEncoder declared for an interface type (IShape) also
     * handles implementing records (Circle), via getType().isInstance(result).
     * The encoder identifies the handled type by its concrete runtime class name.
     *******************************************************************************/

    public interface IShape {}

    public record Shape(int id) implements IShape {}

    public record Circle(int radius) implements IShape {}

    @ApplicationScoped
    public static class ShapeEncoder implements ToolResponseEncoder<IShape> {

        @Override
        public Class<IShape> getType() {
            return IShape.class;
        }

        @Override
        public ToolResponse encode(IShape shape) {
            return ToolResponse.ofText("encoded by ShapeEncoder: " + shape.getClass().getSimpleName());
        }
    }

    @Tool(name = "testGetTypeExactMatch",
          description = "tests that a ToolResponseEncoder is selected when the tool return type is a concrete class (Shape) that directly implements the encoder's registered interface (IShape)")
    public Shape testGetTypeExactMatch() {
        return new Shape(1);
    }

    @Tool(name = "testGetTypeSubtypeMatch",
          description = "tests that a ToolResponseEncoder declared for an interface (IShape) handles a different implementing record (Circle) via getType().isAssignableFrom()")
    public Circle testGetTypeSubtypeMatch() {
        return new Circle(42);
    }
}
