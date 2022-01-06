/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package componenttest.app;

import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.json.JsonReaderFactory;
import javax.json.JsonWriter;
import javax.json.JsonWriterFactory;

/**
 * Serializes and Deserializes an AssertionError to/from JSON
 */
public class AssertionErrorSerializer {

    //eye-catcher tags to denote the start and end of JSON content
    public static final String START_TAG = "###AssertionError Json Start###";
    public static final String END_TAG = "###AssertionError Json End###";
    //JSON property names
    private static final String CLASS_NAME_KEY = "className";
    private static final String METHOD_NAME_KEY = "methodName";
    private static final String EXCEPTION_TYPE_KEY = "exceptionType";
    private static final String MESSAGE_KEY = "message";
    private static final String STACK_KEY = "stack";
    private static final String FILE_NAME_KEY = "fileName";
    private static final String LINE_NUMBER_KEY = "lineNumber";
    //The FATServlet.doGet method name
    private static final String DO_GET = "doGet";

    //JSON Factories
    private static final JsonBuilderFactory BUILDER_FACTORY = Json.createBuilderFactory(null);
    private static final JsonReaderFactory READER_FACTORY = Json.createReaderFactory(null);
    private static final JsonWriterFactory WRITER_FACTORY = Json.createWriterFactory(null);

    /**
     * Serialize out an AssertionError as a JSON String
     *
     * @param e The AssertionError
     * @return A JSON String
     */
    public static String serialize(AssertionError e) {
        StringWriter writer = new StringWriter();
        serialize(e, writer);
        return writer.toString();
    }

    /**
     * Serialize out an AssertionError as a JSON String via the given writer
     *
     * @param e      The AssertionError
     * @param writer A Writer to writer out the JSON String to
     */
    public static void serialize(AssertionError e, Writer writer) {
        JsonObject json = serializeAssertionError(e);
        JsonWriter jsonWriter = WRITER_FACTORY.createWriter(writer);
        jsonWriter.writeObject(json);
    }

    /**
     * @param json A json string containing the serialized form of an AssertionError
     * @return An instance of AssertionError
     */
    public static AssertionError deserialize(String json) {
        StringReader reader = new StringReader(json);
        AssertionError e = deserialize(reader);
        return e;
    }

    /**
     * @param reader A Reader to read the json from; the serialized form of an AssertionError
     * @return An instance of AssertionError
     */
    public static AssertionError deserialize(Reader reader) {
        JsonReader jsonReader = READER_FACTORY.createReader(reader);
        JsonObject jsonObject = jsonReader.readObject();
        jsonReader.close();

        AssertionError e = deserializeAssertionError(jsonObject);
        return e;
    }

    /**
     * Serialize out an AssertionError as a JsonObject
     *
     * @param e The AssertionError
     * @return A JsonObject
     */
    private static JsonObject serializeAssertionError(AssertionError e) {
        JsonObjectBuilder builder = BUILDER_FACTORY.createObjectBuilder();
        builder.add(EXCEPTION_TYPE_KEY, "AssertionError");
        builder.add(MESSAGE_KEY, e.getMessage());

        StackTraceElement[] stack = e.getStackTrace();
        JsonArray stackJson = serializeStack(stack);
        builder.add(STACK_KEY, stackJson);

        return builder.build();
    }

    /**
     * Deserialize an AssertionError from a JsonObject
     *
     * @param jsonObject The JsonObject which represents the AssertionError
     * @return An instance of AssertionError
     * @throws IllegalStateException if the exception type specified was not AssertionError
     */
    private static AssertionError deserializeAssertionError(JsonObject jsonObject) {

        String exceptionType = jsonObject.getString(EXCEPTION_TYPE_KEY);
        if (!"AssertionError".equals(exceptionType)) {
            throw new IllegalStateException("Unknown exception type: " + exceptionType);
        }

        String message = jsonObject.getString(MESSAGE_KEY);

        JsonArray stackJson = jsonObject.getJsonArray(STACK_KEY);
        StackTraceElement[] stack = deserializeStack(stackJson);

        AssertionError assertionError = new AssertionError(message);
        assertionError.setStackTrace(stack);

        return assertionError;
    }

    /**
     * Serialize an exception stacktrace as a JsonArray
     *
     * @param stack an array of stack trace elements
     * @return A JsonArray
     */
    private static JsonArray serializeStack(StackTraceElement[] stack) {
        JsonArrayBuilder stackBuilder = BUILDER_FACTORY.createArrayBuilder();

        for (StackTraceElement element : stack) {
            JsonObject elementJson = serializeStackTraceElement(element);
            stackBuilder.add(elementJson);
        }

        return stackBuilder.build();
    }

    /**
     * Deserialize an exception stacktrace from a JsonArray
     *
     * @param jsonArray A JsonArray that represents the stacktrace
     * @return An array of StackTraceElements
     */
    private static StackTraceElement[] deserializeStack(JsonArray jsonArray) {
        int size = jsonArray.size();
        StackTraceElement[] stack = new StackTraceElement[size];
        for (int i = 0; i < size; i++) {
            JsonObject elementJson = jsonArray.getJsonObject(i);
            StackTraceElement element = deserializeStackTraceElement(elementJson);
            stack[i] = element;
        }

        return stack;
    }

    /**
     * Serialize a StackTraceElement out as a JsonObject
     *
     * @param stackTraceElement A stack trace element
     * @return A JsonObject
     */
    private static JsonObject serializeStackTraceElement(StackTraceElement stackTraceElement) {
        JsonObjectBuilder builder = BUILDER_FACTORY.createObjectBuilder();
        builder.add(CLASS_NAME_KEY, stackTraceElement.getClassName());
        builder.add(METHOD_NAME_KEY, stackTraceElement.getMethodName());
        builder.add(FILE_NAME_KEY, stackTraceElement.getFileName());
        builder.add(LINE_NUMBER_KEY, stackTraceElement.getLineNumber());
        return builder.build();
    }

    /**
     * Deserialize a StackTraceElement from a JsonObject
     *
     * @param jsonObject A JsonObject that represents a stack trace element
     * @return A StackTraceElement instance
     */
    private static StackTraceElement deserializeStackTraceElement(JsonObject jsonObject) {
        String declaringClass = jsonObject.getString(CLASS_NAME_KEY);
        String methodName = jsonObject.getString(METHOD_NAME_KEY);
        String fileName = jsonObject.getString(FILE_NAME_KEY);
        int lineNumber = jsonObject.getInt(LINE_NUMBER_KEY);
        StackTraceElement element = new StackTraceElement(declaringClass, methodName, fileName, lineNumber);
        return element;
    }

    /**
     * Simplify an AssertionError that has been thrown from a test in a subclass of FATServlet.
     * This will change the error message to include the FAT classname and test method name.
     * It will also shorten the stacktrace to stop at FATServlet.doGet because everything after that
     * will always be the same and is not of any interest.
     *
     * @param fatClass      The FAT class which originally threw the AssertionError
     * @param fatMethodName The FAT method which originally threw the AssertionError
     * @param e             The original AssertionError
     * @return A simplified AssertionError
     */
    public static <T extends FATServlet> AssertionError simplify(Class<T> fatClass, String fatMethodName, AssertionError e) {
        AssertionError assertionError = new AssertionError(fatClass.getSimpleName() + "." + fatMethodName + ": " + e.getMessage());
        StackTraceElement[] originalStack = e.getStackTrace();
        ArrayList<StackTraceElement> shortenedStack = new ArrayList<>();

        for (StackTraceElement element : originalStack) {
            String declaringClass = element.getClassName();
            String methodName = element.getMethodName();
            String fileName = element.getFileName();
            int lineNumber = element.getLineNumber();
            StackTraceElement newElement = new StackTraceElement(declaringClass, methodName, fileName, lineNumber);

            shortenedStack.add(newElement);

            //the stack beyond doGet is always the same so don't output any more
            if ((FATServlet.class.getSimpleName() + ".java").equals(fileName) && DO_GET.equals(methodName)) {
                break;
            }
        }

        assertionError.setStackTrace(shortenedStack.toArray(new StackTraceElement[shortenedStack.size()]));
        return assertionError;
    }
}
