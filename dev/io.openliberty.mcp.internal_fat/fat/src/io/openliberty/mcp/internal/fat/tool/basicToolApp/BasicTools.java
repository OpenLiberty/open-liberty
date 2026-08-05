/*******************************************************************************
 * Copyright (c) 2025, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.fat.tool.basicToolApp;

import static org.junit.Assert.assertEquals;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.mcpjava.server.ImplementationInfo;
import org.mcpjava.server.McpRequest;
import org.mcpjava.server.MetaField;
import org.mcpjava.server.MetaField.Type;
import org.mcpjava.server.Role;
import org.mcpjava.server.content.Annotations;
import org.mcpjava.server.content.AudioContent;
import org.mcpjava.server.content.ContentBlock;
import org.mcpjava.server.content.EmbeddedResource;
import org.mcpjava.server.content.ImageContent;
import org.mcpjava.server.content.ResourceLink;
import org.mcpjava.server.content.TextContent;
import org.mcpjava.server.tools.Tool;
import org.mcpjava.server.tools.ToolArg;
import org.mcpjava.server.tools.ToolResponse;

import io.openliberty.mcp.annotations.Schema;
import io.openliberty.mcp.internal.fat.utils.TestConstants;
import io.openliberty.mcp.tools.ToolCallException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
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
        TextContent text = TextContent.of("Echo: " + input);

        ImageContent image = ImageContent.of("base64-encoded-image".getBytes(), "image/png");

        AudioContent audio = AudioContent.of("base64-encoded-audio".getBytes(), "audio/mpeg");

        return ToolResponse.builder()
                           .addContent(text)
                           .addContent(image)
                           .addContent(audio)
                           .build();

    }

    @Tool(name = "mixedContentListTool", title = "Mixed Content List Tool", description = "Returns Text, Audio or Image Content List")
    public List<ContentBlock> mixedContentListTool(@ToolArg(name = "input", description = "input to echo") String input) {
        return List.of(TextContent.of("Echo: " + input),
                       ImageContent.of(TestConstants.TEST_IMAGE_DATA, "image/png"),
                       AudioContent.of(TestConstants.TEST_AUDIO_DATA, "audio/mpeg"));
    }

    @Tool(name = "textContentTool", title = "Text Content Tool", description = "Returns text content object")
    public TextContent textContentTool(@ToolArg(name = "input", description = "input string to echo back as content") String input) {
        return TextContent.of("Echo: " + input);
    }

    @Tool(name = "textContentToolWithContentAnnotation", title = "Text Content Tool With Content Annotation", description = "Returns text content object with annotation")
    public TextContent textContentToolWithContentAnnotation(@ToolArg(name = "input", description = "input string to echo back as content") String input) {
        Annotations annotations = Annotations.builder()
                                             .setAudience(Role.ASSISTANT)
                                             .setLastModified(ZonedDateTime.of(2025, 8, 26, 8, 40, 0, 0, ZoneOffset.UTC).toInstant())
                                             .setPriority(0.5)
                                             .build();

        return TextContent.builder("Echo: " + input)
                          .setAnnotations(annotations)
                          .build();
    }

    @Tool(name = "imageContentTool", title = "Image Content Tool", description = "Returns image content object")
    public ImageContent imageContentTool(@ToolArg(name = "imageData", description = "Base64-encoded image") String imageData64) {
        byte[] imageData = Base64.getDecoder().decode(imageData64);
        return ImageContent.of(
                               imageData,
                               "image/png");
    }

    @Tool(name = "imageContentToolWithContentAnnotation", title = "Image Content Tool With Content Annotation", description = "Returns image content object with annotation")
    public ImageContent imageContentToolWithContentAnnotation(@ToolArg(name = "imageData", description = "Base64-encoded image") String imageData) {
        Annotations annotations = Annotations.builder()
                                             .setAudience(Role.USER)
                                             .setLastModified(ZonedDateTime.of(2025, 8, 26, 8, 40, 0, 0, ZoneOffset.UTC).toInstant())
                                             .setPriority(0.8)
                                             .build();

        return ImageContent.builder(Base64.getDecoder().decode(imageData), "image/png")
                           .setAnnotations(annotations)
                           .build();
    }

    @Tool(name = "audioContentTool", title = "Audio Content Tool", description = "Returns audio content object")
    public AudioContent audioContentTool(@ToolArg(name = "audioData", description = "Base64-encoded audio") String audioData64) {
        byte[] audioData = Base64.getDecoder().decode(audioData64);
        return AudioContent.of(
                               audioData,
                               "audio/mpeg");
    }

    @Tool(name = "audioContentToolWithContentAnnotation", title = "Audio Content Tool With Content Annotation", description = "Returns audio content object with annotation")
    public AudioContent audioContentToolWithContentAnnotation(@ToolArg(name = "audioData", description = "Base64-encoded audio") String audioData64) {
        org.mcpjava.server.content.Annotations annotations = Annotations.builder()
                                                                        .setAudience(Role.ASSISTANT)
                                                                        .setLastModified(ZonedDateTime.of(2025, 8, 26, 8, 40, 0, 0, ZoneOffset.UTC).toInstant())
                                                                        .setPriority(0.3)
                                                                        .build();
        byte[] audioData = Base64.getDecoder().decode(audioData64);
        return AudioContent.builder(audioData, "audio/mpeg")
                           .setAnnotations(annotations)
                           .build();
    }

    @Tool(name = "resourceLinkTool", title = "Resource Link Tool", description = "Returns a resource link content object")
    public ResourceLink resourceLinkTool(@ToolArg(name = "name", description = "Resource name") String name,
                                         @ToolArg(name = "uri", description = "Resource URI") String uri) {
        return ResourceLink.builder(name, uri)
                           .setTitle("Resource: " + name)
                           .setMimeType("text/plain")
                           .build();
    }

    @Tool(name = "resourceLinkToolWithAnnotation", title = "Resource Link Tool With Annotation", description = "Returns a resource link content object with annotation")
    public ResourceLink resourceLinkToolWithAnnotation(@ToolArg(name = "name", description = "Resource name") String name,
                                                       @ToolArg(name = "uri", description = "Resource URI") String uri) {
        Annotations annotations = Annotations.builder()
                                             .setAudience(Role.USER)
                                             .setLastModified(ZonedDateTime.of(2025, 8, 26, 8, 40, 0, 0, ZoneOffset.UTC).toInstant())
                                             .build();
        return ResourceLink.builder(name, uri)
                           .setAnnotations(annotations)
                           .build();
    }

    @Tool(name = "embeddedTextResourceTool", title = "Embedded Text Resource Tool", description = "Returns an embedded text resource content object")
    public EmbeddedResource embeddedTextResourceTool(@ToolArg(name = "text", description = "Text content") String text,
                                                     @ToolArg(name = "uri", description = "Resource URI") String uri) {
        return EmbeddedResource.builder(text, uri)
                               .setMimeType("text/plain")
                               .build();
    }

    @Tool(name = "embeddedBlobResourceTool", title = "Embedded Blob Resource Tool", description = "Returns an embedded blob resource content object")
    public EmbeddedResource embeddedBlobResourceTool(@ToolArg(name = "imageData", description = "Base64-encoded image data") String imageData64,
                                                     @ToolArg(name = "uri", description = "Resource URI") String uri) {
        byte[] imageData = Base64.getDecoder().decode(imageData64);
        return EmbeddedResource.builder(imageData, uri)
                               .setMimeType("image/png")
                               .build();
    }

    //tool name is not present -> use method name
    //tool title not present -> ignore
    //tool description is not present -> ignore
    //arg  description is not present -> ignore
    @Tool()
    public String ignoredEcho(@ToolArg(name = "input") String input) {
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

    @Tool(name = "echoRequestId", title = "Echo RequestId", description = "Returns the incoming request ID")
    public String echoRequestId(McpRequest request, @ToolArg(name = "input") String input) {
        return request.id() + ": " + input;
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
          annotations = @Tool.Annotations(readOnlyHint = true))
    public String readOnlyTool(@ToolArg(name = "input", description = "input string") String input) {
        return input;
    }

    @Tool(name = "destructiveTool", title = "Destructive Tool", description = "A tool that performs a destructive operation",
          annotations = @Tool.Annotations(readOnlyHint = false, destructiveHint = true, idempotentHint = false, openWorldHint = false, title = "Destructive Tool"))
    public String destructiveTool(@ToolArg(name = "input", description = "input string") String input) {
        return input;
    }

    @Tool(name = "openWorldTool", title = "Open to World Tool", description = "A tool in an open world context",
          annotations = @Tool.Annotations(readOnlyHint = false, destructiveHint = true, idempotentHint = false, openWorldHint = true, title = "Open to World Tool"))
    public String openWorldTool(@ToolArg(name = "input", description = "input string") String input) {
        return input;
    }

    @Tool(name = "idempotentTool", title = "Idempotent Tool", description = "A tool with idempotent context",
          annotations = @Tool.Annotations(idempotentHint = true, title = "Idempotent Tool"))
    public String idempotentTool(@ToolArg(name = "input", description = "input string") String input) {
        return input;
    }

    @Tool(name = "missingTitle", title = "", description = "A tool that does not have a title",
          annotations = @Tool.Annotations(readOnlyHint = false, destructiveHint = true, idempotentHint = false, openWorldHint = true))
    public String missingTitle(@ToolArg(name = "input", description = "input string") String input) {
        return input;
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

    public static record City(String name, String country, int population, boolean isCapital) {};

    // Test ToolArg.required is always true by default, check that it works when it is set to true
    @Tool(name = "testToolArgIsNotRequired", title = "ToolArgNotRequired", description = "ToolArgNotRequired")
    public boolean testToolArgNotRequired(@ToolArg(name = "value", description = "boolean value", required = false) boolean value) {
        return false;
    }

    @Tool(name = "testToolArgStringNotRequired", title = "ToolArgStringNotRequired", description = "ToolArgNotRequired")
    public String testToolArgStringNotRequired(@ToolArg(name = "value", description = "String value", required = false) String value) {
        return value;
    }

    @Tool(name = "testToolArgIntNotRequired", title = "ToolArgIntNotRequired", description = "ToolArgNotRequired")
    public int testToolArgIntNotRequired(@ToolArg(name = "value", description = "int value", required = false) int value) {
        return value;
    }

    @Tool(name = "testToolArgArrayNotRequired", title = "ToolArgArrayNotRequired", description = "ToolArgNotRequired")
    public int[] testToolArgArrayNotRequired(@ToolArg(name = "value", description = "Array of ints", required = false) int[] value) {
        return value;
    }

    @Tool(name = "testMultipleToolArgsOneNotRequired", title = "testMultipleToolArgsOneNotRequired", description = "MultipleToolArgsOneNotRequired")
    public String testMultipleToolArgsOneNotRequired(@ToolArg(name = "planet", description = "planet you live in") String planet,
                                                     @ToolArg(name = "year", description = "current year", required = false) int year) {
        return "Planet " + planet + " was created in the year " + year;
    }

    @Tool(name = "testToolArgObjectNotRequired", title = "ToolArgObjectNotRequired", description = "ToolArgNotRequired")
    public City testToolArgObjectNotRequired(@ToolArg(name = "value", description = "City object value", required = false) City value) {
        return value;
    }

    /////////////////////////////////////////////
    // Special characters in parameters
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

    public record ListWrapper(List<Person> returnList) {}

    @Tool(name = "addPersonToList", title = "adds person to people list", description = "adds person to people list", structuredContent = true)
    @Schema(description = "Returns list of person object")
    public ListWrapper addPersonToList(
                                       @ToolArg(name = "employeeList", description = "List of people") List<Person> employeeList,
                                       @ToolArg(name = "person", description = "Person object") Optional<Person> person) {
        employeeList.add(person.get());
        return new ListWrapper(employeeList);
    }

    @Tool(name = "addPersonToListToolResponse", title = "adds person to people list", description = "adds person to people list", structuredContent = true)
    public @Schema(value = "{ \"$defs\": { \"Address\": { \"type\": \"object\", \"properties\": { \"number\": { \"type\": \"integer\" }, \"street\": { \"description\": \"A street object to represent complex streets\", \"type\": \"object\", \"properties\": { \"streetName\": { \"type\": \"string\" }, \"roadType\": { \"type\": \"string\" } }, \"required\": [ \"streetName\" ] }, \"postcode\": { \"type\": \"string\" } }, \"required\": [ \"number\", \"street\", \"postcode\" ] }, \"Person\": { \"description\": \"A person object contains address, company objects\", \"type\": \"object\", \"properties\": { \"address\": { \"$ref\": \"#/$defs/Address\" }, \"company\": { \"type\": \"object\", \"properties\": { \"address\": { \"$ref\": \"#/$defs/Address\" }, \"name\": { \"type\": \"string\" }, \"shareholders\": { \"description\": \"A list of shareholder (person object)\", \"type\": \"array\", \"items\": { \"$ref\": \"#/$defs/Person\" } }, \"shareholderRegistry\": { \"type\": \"object\", \"properties\": { \"value\": { \"$ref\": \"#/$defs/person\" }, \"key\": { \"type\": \"integer\" } }, \"required\": [] } }, \"required\": [ \"name\", \"address\", \"shareholders\" ] }, \"fullname\": { \"type\": \"string\" } }, \"required\": [ \"fullname\", \"address\", \"company\" ] } }, \"type\": \"object\", \"properties\":{ \"returnList\":{ \"type\": \"array\", \"items\": { \"$ref\": \"#/$defs/Person\" } } }, \"description\": \"Returns list of person object\", \"required\": [\"returnList\"] }",
                   description = "Returns list of person object") ToolResponse addPersonToListToolResponse(@ToolArg(name = "employeeList",
                                                                                                                    description = "List of people") List<Person> employeeList,
                                                                                                           @ToolArg(name = "person",
                                                                                                                    description = "Person object") Optional<Person> person) {
        Person personInstance = person.get();
        employeeList.add(personInstance);
        Jsonb jsonb = JsonbBuilder.create();
        ListWrapper returnObj = new ListWrapper(employeeList);
        return ToolResponse.builder()
                           .addContent(TextContent.of(jsonb.toJson(returnObj)))
                           .setStructuredContent(returnObj)
                           .putMetadata("timestamp", 1762860699)
                           .putMetadata("api.ibmtest.org/location", "Hursley")
                           .putMetadata("api.libertytest.org/person", personInstance)
                           .build();
    }

    @Tool(name = "addPersonToListToolResponseWithMetaRequest", title = "adds person to people list", description = "adds person to people list", structuredContent = true)
    public @Schema(value = "{ \"$defs\": { \"Address\": { \"type\": \"object\", \"properties\": { \"number\": { \"type\": \"integer\" }, \"street\": { \"description\": \"A street object to represent complex streets\", \"type\": \"object\", \"properties\": { \"streetName\": { \"type\": \"string\" }, \"roadType\": { \"type\": \"string\" } }, \"required\": [ \"streetName\" ] }, \"postcode\": { \"type\": \"string\" } }, \"required\": [ \"number\", \"street\", \"postcode\" ] }, \"Person\": { \"description\": \"A person object contains address, company objects\", \"type\": \"object\", \"properties\": { \"address\": { \"$ref\": \"#/$defs/Address\" }, \"company\": { \"type\": \"object\", \"properties\": { \"address\": { \"$ref\": \"#/$defs/Address\" }, \"name\": { \"type\": \"string\" }, \"shareholders\": { \"description\": \"A list of shareholder (person object)\", \"type\": \"array\", \"items\": { \"$ref\": \"#/$defs/Person\" } }, \"shareholderRegistry\": { \"type\": \"object\", \"properties\": { \"value\": { \"$ref\": \"#/$defs/person\" }, \"key\": { \"type\": \"integer\" } }, \"required\": [] } }, \"required\": [ \"name\", \"address\", \"shareholders\" ] }, \"fullname\": { \"type\": \"string\" } }, \"required\": [ \"fullname\", \"address\", \"company\" ] } }, \"type\": \"object\", \"properties\":{ \"returnList\":{ \"type\": \"array\", \"items\": { \"$ref\": \"#/$defs/Person\" } } }, \"description\": \"Returns list of person object\", \"required\": [\"returnList\"]}",
                   description = "Returns list of person object") ToolResponse addPersonToListToolResponseWithMetaRequest(@ToolArg(name = "employeeList",
                                                                                                                                   description = "List of people") List<Person> employeeList,
                                                                                                                          @ToolArg(name = "person",
                                                                                                                                   description = "Person object") Optional<Person> person,
                                                                                                                          McpRequest request) {
        Person personInstance = person.get();
        employeeList.add(personInstance);
        Jsonb jsonb = JsonbBuilder.create();

        Map<String, Object> _meta = request.metadata();
        ListWrapper returnObj = new ListWrapper(employeeList);
        return ToolResponse.builder()
                           .setMetadata(_meta)
                           .addContent(TextContent.of(jsonb.toJson(returnObj)))
                           .setStructuredContent(returnObj)
                           .build();
    }

    @Tool(name = "simpleMetaRequest", title = "return string made from args and metadata", description = "return string made from args and metadata", structuredContent = false)
    public String simpleMetaRequest(@ToolArg(name = "name", description = "name of person") String name,
                                    McpRequest request) {

        String location = (String) request.metadata().get("api.ibmtest.org/location");
        BigDecimal timestamp = (BigDecimal) request.metadata().get("timestamp");
        String result = "Hello " + name + " you have called this tool from " + location + " at timestamp " + timestamp.toString();
        return result;
    }

    @Tool(name = "get-user-jp",
          title = "ユーザー情報取得", // Retrieve user information
          description = "指定されたユーザー ID の名前とロールを取得します。") // Retrieve the name and role of the specified user ID
    public String getUserJp(@ToolArg(name = "userid",
                                     description = "対象ユーザーのユーザーID。") String userId) { // The user ID of the target user
        return "ID: " + userId + ", Name: 仮名, role: user";
    }

    //////////
    // Error Testing Tools
    //////////

    @Tool(name = "businessErrorTool", title = "Business Error Tool", description = "Sync tool that throws ToolCallException for testing error metrics")
    public String businessErrorTool(@ToolArg(name = "input", description = "input value") String input) {
        if ("bad-value".equals(input) || "bad".equals(input)) {
            throw new ToolCallException("Invalid business input: " + input);
        }
        return "Success: " + input;
    }

    @Tool(name = "nonBusinessErrorTool", title = "Non-Business Error Tool", description = "Sync tool that throws generic exception for testing error metrics")
    public String nonBusinessErrorTool(@ToolArg(name = "input", description = "input value") String input) {
        if ("trigger-error".equals(input) || "error".equals(input)) {
            throw new RuntimeException("Non-business error occurred");
        }
        return "Success: " + input;
    }

    @Tool(name = "asyncBusinessErrorTool", title = "Async Business Error Tool", description = "Async tool that throws ToolCallException for testing async error metrics")
    public CompletionStage<String> asyncBusinessErrorTool(@ToolArg(name = "input", description = "input value") String input) {
        if ("bad-value".equals(input) || "bad".equals(input)) {
            throw new ToolCallException("Async invalid business input: " + input);
        }
        return CompletableFuture.completedFuture("Async success: " + input);
    }

    @Tool(name = "asyncNonBusinessErrorTool", title = "Async Non-Business Error Tool", description = "Async tool that throws generic exception for testing async error metrics")
    public CompletionStage<String> asyncNonBusinessErrorTool(@ToolArg(name = "input", description = "input value") String input) {
        if ("trigger-error".equals(input) || "error".equals(input)) {
            throw new RuntimeException("Async non-business error occurred");
        }
        return CompletableFuture.completedFuture("Async success: " + input);
    }

    @Tool(name = "failedStageTool", title = "Failed Stage Tool", description = "Synchronous tool that throws an exception for testing error metrics")
    public String failedStageTool(@ToolArg(name = "input", description = "input value") String input) throws ToolCallException {
        if ("fail".equals(input) || "error".equals(input)) {
            throw new ToolCallException("Failed stage error");
        }
        return "Success: " + input;
    }

    @Tool(name = "asyncFailedStageTool", title = "Async Failed Stage Tool", description = "Async tool that returns failed CompletionStage for testing async error metrics")
    public CompletionStage<String> asyncFailedStageTool(@ToolArg(name = "input", description = "input value") String input) {
        if ("trigger-error".equals(input) || "error".equals(input)) {
            return CompletableFuture.failedStage(new ToolCallException("Async failed stage error"));
        }
        return CompletableFuture.completedFuture("Async success: " + input);
    }

    @Tool(name = "testNonLatinStringStructuredContent", title = "Not Latin String Structured Content Response",
          description = "A tool to return a string with structuredContent set. The response should successully return non-latin characters",
          structuredContent = true)
    public City testNonLatinStringStructuredContent() {
        return new City("東京", "日本", 14000000, true);
    }

    @Tool(name = "noArgsRequest", title = "call tool without propviding arguments in params", description = "return string made from args and metadata", structuredContent = false)
    public String noArgsRequest(McpRequest request) {

        String location = (String) request.metadata().get("api.ibmtest.org/location");
        BigDecimal timestamp = (BigDecimal) request.metadata().get("timestamp");
        String result = "You have called this tool from " + location + " at timestamp " + timestamp.toString();
        return result;
    }

    @Tool
    public String checkMcpRequest(McpRequest request) {
        return wrapErrors(() -> {
            // Note: expected caps matches McpClient.before()
            Map<String, Object> expectedCaps = Map.of("roots", Map.of("listChanged", true),
                                                      "sampling", Map.of(),
                                                      "elicitation", Map.of());
            assertEquals("capabilities", expectedCaps, request.rawClientCapabilities());

            assertEquals("protocolVersion", "2025-11-25", request.protocolVersion());

            ImplementationInfo clientInfo = request.clientInfo();
            assertEquals("icons", List.of(), clientInfo.icons());
            assertEquals("name", "fat-test-client", clientInfo.name());
            assertEquals("title", "FAT Test Client", clientInfo.title());
            assertEquals("version", "1.0.0", clientInfo.version());
            assertEquals("description", Optional.empty(), clientInfo.description());
            assertEquals("websiteUrl", Optional.empty(), clientInfo.websiteUrl());

            return "OK";
        });
    }

    @Tool
    public String readSessionIdReversed(McpRequest request) {
        return request.sessionId()
                      .map(s -> new StringBuilder(s).reverse().toString())
                      .orElse("No Session ID");
    }

    @Tool
    public String unannotatedArgTool(String name, int count, McpRequest req) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < count; i++) {
            result.append("hello ");
        }
        result.append(name);
        result.append(" of ").append(req.clientInfo().title());
        return result.toString();
    }

    @Tool
    @MetaField(name = "test", value = "foo")
    public String metaFieldSimple() {
        return "OK";
    }

    @Tool
    @MetaField(name = "testString", value = "foo")
    @MetaField(name = "testInt", value = "42", type = Type.INT)
    @MetaField(name = "testBool", value = "false", type = Type.BOOLEAN)
    @MetaField(name = "testJson", type = Type.JSON, value = """
                    {
                        "letters": ["a", "b", "c"],
                        "numbers": [1, 2, 3],
                        "booleans": [true, false]
                    }
                    """)
    public String metaFieldComplex() {
        return "OK";
    }

    /**
     * Run a callable and translate any exceptions or assertion errors into a String
     */
    private String wrapErrors(Callable<String> runnable) {
        try {
            return runnable.call();
        } catch (Exception | AssertionError e) {
            StringWriter writer = new StringWriter();
            try (var pw = new PrintWriter(writer)) {
                e.printStackTrace(pw);
            }
            return writer.toString();
        }
    }

}
