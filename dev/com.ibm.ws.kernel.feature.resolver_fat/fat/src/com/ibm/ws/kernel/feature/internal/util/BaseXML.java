/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.kernel.feature.internal.util;

import java.io.Closeable;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.ErrorHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;

//Source restricted to java7.

public class BaseXML {
    public static final boolean DO_APPEND = true;

    public interface FailableConsumer<T, E extends Exception> {
        void accept(T consumed) throws E;
    }

    public static void write(File file, FailableConsumer<PrintWriter, Exception> writer) throws Exception {
        try (FileWriter fW = new FileWriter(file, !DO_APPEND);
                        PrintWriter pW = new PrintWriter(fW)) {
            writer.accept(pW);
        }
    }

    public static class BaseXMLWriter implements Closeable {
        public BaseXMLWriter(PrintWriter pW) {
            this.pW = pW;
            this.indent = 0;
        }

        //

        private final PrintWriter pW;

        protected void println() {
            pW.println();
        }

        protected void println(String line) {
            pW.println(line);
        }

        @Override
        public void close() throws IOException {
            pW.close();
        }

        //

        private int indent;

        protected void upIndent() {
            indent++;
        }

        protected void downIndent() {
            indent--;
        }

        protected static final String INDENT_0 = "";
        protected static final String INDENT_4 = "    ";
        protected static final String INDENT_8 = "        ";
        protected static final String INDENT_12 = "            ";

        protected String getIndentation() {
            if (indent == 0) {
                return INDENT_0;
            } else if (indent == 1) {
                return INDENT_4;
            } else if (indent == 2) {
                return INDENT_8;
            } else {
                return INDENT_12;
            }
        }

        //

        protected final StringBuilder lineBuilder = new StringBuilder();

        public static final char OPEN_BRACE_CHAR = '<';
        public static final char SLASH_CHAR = '/';
        public static final char CLOSE_BRACE_CHAR = '>';

        protected static final boolean DO_CLOSE = true;

        // <element/>
        protected String assembleLine(String element) {
            lineBuilder.append(getIndentation());

            lineBuilder.append(OPEN_BRACE_CHAR);
            lineBuilder.append(element);
            lineBuilder.append(SLASH_CHAR);
            lineBuilder.append(CLOSE_BRACE_CHAR);

            String line = lineBuilder.toString();
            lineBuilder.setLength(0);
            return line;
        }

        // <element> or </element>
        protected String assembleLine(String element, boolean doClose) {
            lineBuilder.append(getIndentation());

            lineBuilder.append(OPEN_BRACE_CHAR);
            if (doClose) {
                lineBuilder.append(SLASH_CHAR);
            }
            lineBuilder.append(element);
            lineBuilder.append(CLOSE_BRACE_CHAR);

            String line = lineBuilder.toString();
            lineBuilder.setLength(0);
            return line;
        }

        // <element>text</element>
        protected String assembleLine(String element, String value) {
            lineBuilder.append(getIndentation());

            lineBuilder.append(OPEN_BRACE_CHAR);
            lineBuilder.append(element);
            lineBuilder.append(CLOSE_BRACE_CHAR);

            lineBuilder.append(value);

            lineBuilder.append(OPEN_BRACE_CHAR);
            lineBuilder.append(SLASH_CHAR);
            lineBuilder.append(element);
            lineBuilder.append(CLOSE_BRACE_CHAR);

            String line = lineBuilder.toString();
            lineBuilder.setLength(0);
            return line;
        }

        public void openElement(String element) {
            println(assembleLine(element, !DO_CLOSE));
        }

        public void closeElement(String element) {
            println(assembleLine(element, DO_CLOSE));
        }

        public void printElement(String element) {
            println(assembleLine(element));
        }

        public void printElement(String element, String value) {
            println(assembleLine(element, value));
        }

        public void printElement(String element, long value) {
            printElement(element, Long.toString(value));
        }

        public void printElement(String element, boolean value) {
            println(assembleLine(element, Boolean.toString(value)));
        }

        public String optionalToString(Object value) {
            return ((value == null) ? null : value.toString());
        }

        public void printOptionalElement(String element, Object value) {
            if (value != null) {
                println(assembleLine(element, value.toString()));
            }
        }

        public void printOptionalElement(String element, boolean value) {
            if (value) {
                printElement(element);
            }
        }

        public void withinElement(String element, Runnable action) {
            openElement(element);
            upIndent();
            action.run();
            downIndent();
            closeElement(element);
        }
    }

    public static void read(File file, ContentHandler contentHandler, ErrorHandler errorHandler) throws Exception {
        SAXParserFactory spf = SAXParserFactory.newInstance();
        SAXParser saxParser = spf.newSAXParser();
        XMLReader xmlReader = saxParser.getXMLReader();
        xmlReader.setContentHandler(contentHandler);
        xmlReader.setErrorHandler(errorHandler);

        xmlReader.parse(file.toURI().toASCIIString());
    }

    public static class BaseContentHandler implements ContentHandler {
        public BaseContentHandler() {
            this.locator = null;

            this.elementStack = new ArrayList<>();
            this.lastElement = null;

            this.builderPool = new ArrayList<>();

            this.builderStack = new ArrayList<>();
            this.lastBuilder = null;
        }

        //

        private Locator locator;

        @Override
        public void setDocumentLocator(Locator locator) {
            this.locator = locator;
        }

        public Locator getDocumentLocator() {
            return locator;
        }

        //

        @Override
        public void startDocument() throws SAXException {
            // Ignore
        }

        @Override
        public void endDocument() throws SAXException {
            // Ignore
        }

        @Override
        public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
            // Ignore
        }

        //

        private final List<String> elementStack;
        private String lastElement;

        protected String popElement() {
            String oldLast = lastElement;
            int size = elementStack.size();
            lastElement = ((size > 1) ? elementStack.get(size - 2) : null);
            return oldLast;
        }

        protected void pushElement(String newLastElement) {
            elementStack.add(lastElement = newLastElement);
        }

        protected void pushElement(String newLastElement, String requiredElement) throws SAXParseException {
            String errorMsg;

            if (requiredElement == null) {
                if (lastElement != null) {
                    errorMsg = "Element [ " + newLastElement + " ]" +
                               " within [ " + lastElement + " ]" +
                               " but root is required";
                } else {
                    errorMsg = null;
                }
            } else {
                if (lastElement == null) {
                    errorMsg = "Element [ " + newLastElement + " ] as root" +
                               " but [ " + requiredElement + " ] is required";
                } else if (!lastElement.equals(requiredElement)) {
                    errorMsg = "Element [ " + newLastElement + " ]" +
                               " beneath [ " + lastElement + " ]" +
                               " but [ " + requiredElement + " ] is required";
                } else {
                    errorMsg = null;
                }
            }

            if (errorMsg != null) {
                throw new SAXParseException(errorMsg, getDocumentLocator());
            }

            pushElement(newLastElement);
        }

        protected boolean pushElement(String localName, String elementName, String requiredElement) throws SAXParseException {
            if (localName.equals(elementName)) {
                pushElement(localName, requiredElement);
                return true;
            } else {
                return false;
            }
        }

        private final List<StringBuilder> builderPool;

        protected StringBuilder acquireBuilder() {
            if (builderPool.isEmpty()) {
                return new StringBuilder();
            } else {
                return builderPool.remove(builderPool.size() - 1);
            }
        }

        protected String releaseBuilder(StringBuilder builder) {
            String text = builder.toString();
            builder.setLength(0);
            builderPool.add(builder);
            return text;
        }

        private final List<StringBuilder> builderStack;
        private StringBuilder lastBuilder;

        protected String popBuilder() {
            StringBuilder oldLast = lastBuilder;

            int size = builderStack.size();
            lastBuilder = ((size > 1) ? builderStack.get(size - 2) : null);

            return releaseBuilder(oldLast);
        }

        protected void pushBuilder() {
            builderStack.add(lastBuilder = acquireBuilder());
        }

        protected void addText(String text) throws SAXParseException {
            if (lastBuilder == null) {
                throw new SAXParseException("Unexpected text [ " + text + " ]", getDocumentLocator());
            }
            lastBuilder.append(text);
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
            throw new SAXParseException("Unknown element [ " + localName + " ]", getDocumentLocator());
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            throw new SAXParseException("Unknown element [ " + localName + " ]", getDocumentLocator());
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            addText(new String(ch, start, length)); // throws SAXParseException
        }

        //

        @Override
        public void startPrefixMapping(String prefix, String uri) throws SAXException {
            // Ignore
        }

        @Override
        public void endPrefixMapping(String prefix) throws SAXException {
            // Ignore
        }

        @Override
        public void processingInstruction(String target, String data) throws SAXException {
            // Ignore
        }

        @Override
        public void skippedEntity(String name) throws SAXException {
            // Ignore
        }
    }

    public static class BaseErrorHandler implements ErrorHandler {
        BaseErrorHandler(PrintStream printer) {
            this.printer = printer;
            this.captured = new ArrayList<>();
        }

        private final List<SAXParseException> captured;

        protected void capture(SAXParseException e) {
            captured.add(e);
        }

        public List<? extends SAXParseException> getCaptured() {
            return captured;
        }

        private final PrintStream printer;

        public void println(String message) {
            printer.println(message);
        }

        public String println(String severity, SAXParseException e) {
            String message;
            println(message = severity + ": " + getMessage(e));
            e.printStackTrace(printer);
            return message;
        }

        protected static String getMessage(SAXParseException e) {
            String systemId = e.getSystemId();
            if (systemId == null) {
                systemId = "null";
            }
            return "URI [ " + systemId + " ]" +
                   " Line [ " + e.getLineNumber() + " ]" +
                   ": " + e.getMessage();
        }

        @Override
        public void warning(SAXParseException e) throws SAXParseException {
            println("Warning", e);
            throw e;
        }

        @Override
        public void error(SAXParseException e) throws SAXParseException {
            println("Error", e);
            throw e;
        }

        @Override
        public void fatalError(SAXParseException e) throws SAXParseException {
            println("Fatal", e);
            throw e;
        }
    }
}
