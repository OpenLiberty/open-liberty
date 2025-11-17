/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.fat.tool.basicToolApp;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.openliberty.mcp.annotations.Schema;
import io.openliberty.mcp.annotations.Tool;
import io.openliberty.mcp.annotations.Tool.Annotations;
import io.openliberty.mcp.annotations.ToolArg;
import io.openliberty.mcp.content.AudioContent;
import io.openliberty.mcp.content.Content;
import io.openliberty.mcp.content.ImageContent;
import io.openliberty.mcp.content.Role;
import io.openliberty.mcp.content.TextContent;
import io.openliberty.mcp.tools.ToolResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.bind.adapter.JsonbAdapter;
import jakarta.json.bind.annotation.JsonbProperty;
import jakarta.json.bind.annotation.JsonbTransient;
import jakarta.json.bind.annotation.JsonbTypeAdapter;

/**
 *
 */
@ApplicationScoped
public class BasicTools {

    //////////
    // Custom Inner Tool
    //////////

    @Dependent
    public static class StaticInnerTool {

        @Tool(name = "staticInnerTool", title = "Static Inner Tool", description = "Defined in static inner class")
        public String staticInnerTool(@ToolArg(name = "input") String input) {
            return "Hello " + input;
        }
    }

    //////////
    // Content Types
    //////////

    @Tool(name = "mixedContentTool", title = "Mixed Content Tool", description = "Returns Text, Audio or Image Content")
    public ToolResponse mixedContentTool(@ToolArg(name = "input", description = "input to echo") String input) {
        TextContent text = new TextContent("Echo: " + input);

        ImageContent image = new ImageContent(
                                              "base64-encoded-image",
                                              "image/png");

        AudioContent audio = new AudioContent(
                                              "base64-encoded-audio",
                                              "audio/mpeg");

        return ToolResponse.success(List.of(text, image, audio));

    }

    @Tool(name = "mixedContentListTool", title = "Mixed Content List Tool", description = "Returns Text, Audio or Image Content List")
    public List<Content> mixedContentListTool(@ToolArg(name = "input", description = "input to echo") String input) {
        return List.of(
                       new TextContent("Echo: " + input),

                       new ImageContent(
                                        "base64-encoded-image",
                                        "image/png"),

                       new AudioContent(
                                        "base64-encoded-audio",
                                        "audio/mpeg"));
    }

    @Tool(name = "textContentTool", title = "Text Content Tool", description = "Returns text content object")
    public TextContent textContentTool(@ToolArg(name = "input", description = "input string to echo back as content") String input) {
        return new TextContent("Echo: " + input);
    }

    @Tool(name = "textContentToolWithContentAnnotation", title = "Text Content Tool With Content Annotation", description = "Returns text content object with annotation")
    public TextContent textContentToolWithContentAnnotation(@ToolArg(name = "input", description = "input string to echo back as content") String input) {
        Content.Annotations annotations = new Content.Annotations(Role.ASSISTANT,
                                                                  ZonedDateTime.of(2025, 8, 26, 8, 40, 0, 0, ZoneOffset.UTC)
                                                                               .format(DateTimeFormatter.ISO_INSTANT),
                                                                  0.5);
        return new TextContent("Echo: " + input, null, annotations);
    }

    @Tool(name = "imageContentTool", title = "Image Content Tool", description = "Returns image content object")
    public ImageContent imageContentTool(@ToolArg(name = "imageData", description = "Base64-encoded image") String imageData) {
        return new ImageContent(
                                imageData,
                                "image/png");
    }

    @Tool(name = "imageContentToolWithContentAnnotation", title = "Image Content Tool With Content Annotation", description = "Returns image content object with annotation")
    public ImageContent imageContentToolWithContentAnnotation(@ToolArg(name = "imageData", description = "Base64-encoded image") String imageData) {
        Content.Annotations annotations = new Content.Annotations(Role.USER,
                                                                  ZonedDateTime.of(2025, 8, 26, 8, 40, 0, 0, ZoneOffset.UTC)
                                                                               .format(DateTimeFormatter.ISO_INSTANT),
                                                                  0.8);
        return new ImageContent(
                                imageData,
                                "image/png",
                                null,
                                annotations);
    }

    @Tool(name = "audioContentTool", title = "Audio Content Tool", description = "Returns audio content object")
    public AudioContent audioContentTool(@ToolArg(name = "audioData", description = "Base64-encoded audio") String audioData) {
        return new AudioContent(
                                audioData,
                                "audio/mpeg");
    }

    @Tool(name = "audioContentToolWithContentAnnotation", title = "Audio Content Tool With Content Annotation", description = "Returns audio content object with annotation")
    public AudioContent audioContentToolWithContentAnnotation(@ToolArg(name = "audioData", description = "Base64-encoded audio") String audioData) {
        Content.Annotations annotations = new Content.Annotations(Role.ASSISTANT,
                                                                  ZonedDateTime.of(2025, 8, 26, 8, 40, 0, 0, ZoneOffset.UTC)
                                                                               .format(DateTimeFormatter.ISO_INSTANT),
                                                                  0.3);
        return new AudioContent(
                                audioData,
                                "audio/mpeg",
                                null,
                                annotations);
    }

    //tool name is not present -> use method name
    //tool title not present -> ignore
    //tool description is not present -> ignore
    //arg  description is not present -> ignore
    @Tool()
    public String ignoredEcho(@ToolArg(name = "input") String input) {
        return input;
    }

    //tool name is empty string -> allow empty string
    //tool title is empty string -> ignore
    //tool description is empty string -> ignore
    //arg  description is empty string -> ignore
    @Tool(name = "", title = "", description = "")
    public String emptyEcho(@ToolArg(name = "input", description = "") String input) {
        return input;
    }

    //////////
    // Strings
    //////////
    @Tool(name = "echo", title = "Echoes the input", description = "Returns the input unchanged")
    public String echo(@ToolArg(name = "input", description = "input to echo") String input) {
        if (input.equals("throw error")) {
            throw new RuntimeException("Method call caused runtime exception");
        }
        return input;
    }

    @Tool(name = "privateEcho", title = "Echoes the input", description = "Returns the input unchanged")
    private String privateEcho(@ToolArg(name = "input", description = "input to echo") String input) {
        return input;
    }

    @Tool(name = "testJSONCharacter", title = "testJSONCharacter", description = "testJSONCharacter")
    public String testJSONCharacter(@ToolArg(name = "c", description = "Character") Character c) {
        return c.toString();
    }

    @Tool(name = "testJSONcharacter", title = "testJSONcharacter", description = "testJSONcharacter")
    public String testJSONCharacter(@ToolArg(name = "c", description = "char") char c) {
        return new String(c + "");
    }

    /////////////
    // Primitives
    /////////////
    @Tool(name = "testJSONlong", title = "testJSONlong", description = "testJSONlong")
    public long testJSONlong(@ToolArg(name = "num1", description = "long") long number1) {
        return number1;
    }

    @Tool(name = "testJSONdouble", title = "testJSONdouble", description = "testJSONdouble")
    public double testJSONdouble(@ToolArg(name = "num1", description = "double") double number1) {
        return number1;
    }

    @Tool(name = "testJSONbyte", title = "testJSONbyte", description = "testJSONbyte")
    public byte testJSONbyte(@ToolArg(name = "num1", description = "byte") byte number1) {
        return number1;
    }

    @Tool(name = "testJSONfloat", title = "testJSONfloat", description = "testJSONfloat")
    public float testJSONfloat(@ToolArg(name = "num1", description = "float") float number1) {
        return number1;
    }

    @Tool(name = "testJSONshort", title = "testJSONshort", description = "testJSONshort")
    public short testJSONshort(@ToolArg(name = "num1", description = "short") short number1) {
        return number1;
    }

    ///////////
    // Wrappers
    ///////////
    @Tool(name = "testJSONLong", title = "testJSONLong", description = "testJSONLong")
    public Long testJSONLong(@ToolArg(name = "num1", description = "Long") Long number1) {
        return number1;
    }

    @Tool(name = "testJSONDouble", title = "testJSONDouble", description = "testJSONDouble")
    public Double testJSONDouble(@ToolArg(name = "num1", description = "Double") Double number1) {
        return number1;
    }

    @Tool(name = "testJSONByte", title = "testJSONByte", description = "testJSONByte")
    public Byte testJSONByte(@ToolArg(name = "num1", description = "Byte") Byte number1) {
        return number1;
    }

    @Tool(name = "testJSONFloat", title = "testJSONFloat", description = "testJSONFloat")
    public Float testJSONFloat(@ToolArg(name = "num1", description = "Float") Float number1) {
        return number1;
    }

    @Tool(name = "testJSONShort", title = "testJSONShort", description = "testJSONShort")
    public Short testJSONShort(@ToolArg(name = "num1", description = "Short") Short number1) {
        return number1;
    }

    //////////
    // Integer
    //////////
    @Tool(name = "testJSONInteger", title = "testJSONInteger", description = "testJSONInteger")
    public int testJSONInteger(@ToolArg(name = "num1", description = "Integer") Integer number1) {
        return number1;
    }

    @Tool(name = "add", title = "Addition calculator", description = "Returns the sum of the two inputs")
    public int add(@ToolArg(name = "num1", description = "first number") int number1, @ToolArg(name = "num2", description = "second number") int number2) {
        return number1 + number2;
    }

    @Tool(name = "subtract", title = "Subtraction calculator", description = "Minus number 2 from number 1")
    public int subtract(@ToolArg(name = "num1", description = "") int number1, @ToolArg(name = "num2", description = "") int number2) {
        return number1 - number2;
    }

    /////////
    //Boolean
    /////////
    @Tool(name = "testJSONBoolean", title = "testJSONBoolean", description = "testJSONBoolean")
    public boolean testJSONBoolean(@ToolArg(name = "b", description = "Boolean") Boolean b) {
        return b;
    }

    @Tool(name = "toggle", title = "Boolean toggle", description = "toggles the boolean input")
    public boolean toggle(@ToolArg(name = "value", description = "boolean value") boolean value) {
        return !value;
    }

    /////////
    //Annotations
    /////////

    @Tool(name = "readOnlyTool", title = "Read Only Tool", description = "A tool that is read-only",
          annotations = @Annotations(readOnlyHint = true))
    public String readOnlyTool(@ToolArg(name = "input", description = "input string") String input) {
        return input;
    }

    @Tool(name = "destructiveTool", title = "Destructive Tool", description = "A tool that performs a destructive operation",
          annotations = @Annotations(readOnlyHint = false, destructiveHint = true, idempotentHint = false, openWorldHint = false, title = "Destructive Tool"))
    public String destructiveTool(@ToolArg(name = "input", description = "input string") String input) {
        return input;
    }

    @Tool(name = "openWorldTool", title = "Open to World Tool", description = "A tool in an open world context",
          annotations = @Annotations(readOnlyHint = false, destructiveHint = true, idempotentHint = false, openWorldHint = true, title = "Open to World Tool"))
    public String openWorldTool(@ToolArg(name = "input", description = "input string") String input) {
        return input;
    }

    @Tool(name = "idempotentTool", title = "Idempotent Tool", description = "A tool with idempotent context",
          annotations = @Annotations(idempotentHint = true, title = "Idempotent Tool"))
    public String idempotentTool(@ToolArg(name = "input", description = "input string") String input) {
        return input;
    }

    @Tool(name = "missingTitle", title = "", description = "A tool that does not have a title",
          annotations = @Annotations(readOnlyHint = false, destructiveHint = true, idempotentHint = false, openWorldHint = true))
    public String missingTitle(@ToolArg(name = "input", description = "input string") String input) {
        return input;
    }

    @Tool(name = "testListObjectResponse", title = "City List",
          description = "A tool to return a list of cities", structuredContent = true)
    public List<City> testListObjectResponse() {
        City city1 = new City("Paris", "France", 8000, true);
        City city2 = new City("Manchester", "England", 15000, false);
        return List.of(city1, city2);
    }

    @Tool(name = "testListStringResponse", title = "String List",
          description = "A tool to return a list of strings", structuredContent = true)
    public List<String> testListStringResponse() {
        return List.of("red", "blue", "yellow");
    }

    @Tool(name = "testArrayResponse", title = "Array of ints",
          description = "A tool to return an array of ints", structuredContent = true)
    public int[] testArrayResponse() {
        return new int[] { 1, 2, 3, 4, 5 };
    }

    @Tool(name = "testStringStructuredContentResponse", title = "Structured Content String Response",
          description = "A tool to return a string with structuredContent set. The tool should ignore this and not return a structuredContent field when the response is string.",
          structuredContent = true)
    public String testStringStructuredContentResponse() {
        return "Hello World";
    }

    @Tool(name = "testObjectResponse", title = "Create a city",
          description = "A tool to return a city object you've named", structuredContent = true)
    public City testObjectResponse(@ToolArg(name = "name", description = "name of your city") String name) {
        return new City(name, "England", 8000, false);
    }

    public record City(String name, String country, int population, boolean isCapital) {};

    // Test ToolArg.required is always true by default, check that it works when it is set to true
    @Tool(name = "testToolArgIsNotRequired", title = "ToolArgNotRequired", description = "ToolArgNotRequired")
    public boolean testToolArgNotRequired(@ToolArg(name = "value", description = "boolean value", required = false) boolean value) {
        return false;
    }

    /////////////////////////////////////////////
    // Special characters in Tool and  parameters

    @Tool(name = "specialCharactersInToolName@!><={}'().%:")
    public String specialCharactersInToolName(@ToolArg(name = "arg1", description = "specialCharactersInToolName") String arg1) {
        return arg1;
    }

    @Tool(name = "specialCharactersInToolArgName")
    public String specialCharactersInToolArgName(@ToolArg(name = "@arg1!><", description = "specialCharactersInToolArgName") String arg1,
                                                 @ToolArg(name = "@arg2={}", description = "specialCharactersInToolArgName") String arg2) {
        return arg1;
    }

    @Tool(name = "specialCharactersInToolArgNameVariant2")
    public String specialCharactersInToolArgNameVariant2(@ToolArg(name = "@arg1'()", description = "specialCharactersInToolArgName") String arg1,
                                                         @ToolArg(name = "@arg2.%:", description = "specialCharactersInToolArgName") String arg2) {
        return arg1;
    }

    ////////////////////////////////////////
    // reserved names in Tool and parameters

    @Tool(name = "package")
    public String reservedWordInToolName(@ToolArg(name = "arg1", description = "reservedWordsInToolName") String arg1) {
        return arg1;
    }

    @Tool(name = "reservedNamesInToolArgName")
    public String reservedNamesInToolArgName(@ToolArg(name = "package", description = "reservedNamesInToolArgName") String arg1,
                                             @ToolArg(name = "int", description = "reservedNamesInToolArgName") String arg2) {
        return arg1;
    }

    @Tool(name = "reservedNamesInToolArgNameVariant")
    public String reservedNamesInToolArgNameVariant(@ToolArg(name = "class", description = "reservedNamesInToolArgName") String arg1,
                                                    @ToolArg(name = "void", description = "reservedNamesInToolArgName") String arg2) {
        return arg1;
    }

    // Complex Schema

    @Schema(description = "A person object contains address, company objects")
    public static record Person(@JsonbProperty("fullname") String name, Address address, Company company) {};

    public static record Address(int number, @Schema(description = "A street object to represent complex streets") Street street, String postcode,
                                 @JsonbTransient String directions) {};

    @JsonbTypeAdapter(StreetAdapter.class)
    @Schema("{\"properties\": {  \"streetName\": { \"type\": \"string\" }, \"roadType\": { \"type\": \"string\" } }, \"required\": [ \"streetName\" ], \"type\": \"object\"}")
    public static record Street(String streetname, String roadtype) {}

    public static record Company(String name, Address address, @Schema(description = "A list of shareholder (person object)") List<Person> shareholders,
                                 @Schema(value = "{\"properties\": {\"key\":{ \"type\": \"integer\" }, \"value\":{ \"$ref\": \"#/$defs/person\" }},\"required\": [ ], \"type\": \"object\"}") Optional<Map<String, Person>> shareholderRegistry) {};

    public static class StreetAdapter implements JsonbAdapter<Street, JsonObject> {

        /** {@inheritDoc} */
        @Override
        public Street adaptFromJson(JsonObject arg0) throws Exception {
            return new Street(arg0.getString("streetName"), arg0.getString("roadType"));
        }

        /** {@inheritDoc} */
        @Override
        public JsonObject adaptToJson(Street arg0) throws Exception {
            return Json.createObjectBuilder().add("streetName", arg0.streetname()).add("roadType", arg0.roadtype()).build();
        }
    }

    public static interface NumberRestrictor {
        public Number getMax();

        public Number getMin();

        public void setMax(Number number);

        public void setMin(Number number);
    }

    @Tool(name = "checkPerson", title = "checks if person is shareholder", description = "Returns boolean", structuredContent = false)
    public boolean checkPerson(@ToolArg(name = "person", description = "Person object") Person person, @ToolArg(name = "company", description = "Company object") Company company) {
        return true;
    }

    @Tool(name = "addPersonToList", title = "adds person to people list", description = "adds person to people list", structuredContent = true)
    public @Schema(description = "Returns list of person object") List<Person> addPersonToList(@ToolArg(name = "employeeList",
                                                                                                        description = "List of people") List<Person> employeeList,
                                                                                               @ToolArg(name = "person", description = "Person object") Optional<Person> person) {
        employeeList.add(person.get());
        return employeeList;
    }
}
