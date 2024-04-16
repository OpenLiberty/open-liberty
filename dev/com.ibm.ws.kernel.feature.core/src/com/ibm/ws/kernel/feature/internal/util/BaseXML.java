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

    public static void write(PrintStream output, FailableConsumer<PrintWriter, Exception> writer) throws Exception {
        PrintWriter pW = new PrintWriter(output);
        try {
            writer.accept(pW);
        } finally {
            pW.flush();
        }
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

        public void flush() {
            pW.flush();
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

        // <element>text</element>
        protected String assembleMultiLine(String element, String... values) {
            lineBuilder.append(getIndentation());

            lineBuilder.append(OPEN_BRACE_CHAR);
            lineBuilder.append(element);
            lineBuilder.append(CLOSE_BRACE_CHAR);

            for (String value : values) {
                lineBuilder.append(value);
            }

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

        public void printMultiElement(String element, String... values) {
            println(assembleMultiLine(element, values));
        }

        public void printElement(String element, long value) {
            printElement(element, Long.toString(value));
        }

        public void printElementNsAsS(String element, long valueNs) {
            long ns = valueNs % NS_IN_S;
            long s = (valueNs - ns) / NS_IN_S;

            String nsText = Long.toString(ns);
            String sText = Long.toString(s);

            printMultiElement(element, sText, ".", gap(ns), nsText, " s");
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

            this.whitespaceStack = new ArrayList<>();
            this.lastWhitespace = WHITESPACE_ONLY;

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

        public long parseSAsNS(String text) throws SAXException {
            int len = text.length();
            if ((len < 3) || (text.charAt(len - 1) != 's') || (text.charAt(len - 2) != ' ')) {
                throw new SAXParseException("Incorrect seconds format [ " + text + " ]", getDocumentLocator());
            }
            len -= 2;

            int dot = text.indexOf('.');
            if ((dot == -1) || (dot == 0) || (dot == (len - 1))) {
                throw new SAXParseException("Incorrect seconds format [ " + text + " ]", getDocumentLocator());
            }

            long s;
            long ns;
            try {
                s = Long.parseLong(text.substring(0, dot), 10); // substring is needed before java 9
                ns = Long.parseLong(text.substring(dot + 1, len), 10);
            } catch (NumberFormatException e) {
                throw new SAXParseException("Incorrect seconds format [ " + text + " ]", getDocumentLocator(), e);
            }
            return (s * NS_IN_S) + ns;
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

        private final List<String> elementStack; // Contains all but the last element
        private String lastElement;

        protected String popElement() throws SAXParseException {
            String oldLast = lastElement;
            if (oldLast == null) {
                throw new SAXParseException("Pop with no element", getDocumentLocator());
            }

            int size = elementStack.size();
            lastElement = ((size == 0) ? null : elementStack.remove(size - 1));

            // System.out.println("Pop [ " + oldLast + " ] leaving [ " + lastElement + " ]");
            return oldLast;
        }

        protected void pushElement(String newLastElement) {
            // System.out.println("Push [ " + newLastElement + " ] onto [ " + lastElement + " ]");
            if (lastElement != null) {
                elementStack.add(lastElement);
            }
            lastElement = newLastElement;
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

        protected boolean pushElement(String qName, String elementName, String requiredElement) throws SAXParseException {
            if (qName.equals(elementName)) {
                pushElement(qName, requiredElement);
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

            // System.out.println("Release text [ " + text + " ]");
            return text;
        }

        private final List<Boolean> whitespaceStack;
        private boolean lastWhitespace;

        private final List<StringBuilder> builderStack;
        private StringBuilder lastBuilder;

        protected static final boolean WHITESPACE_ONLY = true;

        protected void pushBuilder(boolean whitespaceOnly) {
            whitespaceStack.add(lastWhitespace);
            lastWhitespace = whitespaceOnly;

            if (!whitespaceOnly) {
                if (lastBuilder != null) {
                    builderStack.add(lastBuilder);
                }
                lastBuilder = acquireBuilder();
            }
        }

        protected String popBuilder() {
            boolean oldWhitespace = lastWhitespace;

            int whiteSize = whitespaceStack.size();
            if (whiteSize == 0) {
                lastWhitespace = WHITESPACE_ONLY;
            } else {
                lastWhitespace = whitespaceStack.remove(whiteSize - 1);
            }

            if (oldWhitespace) {
                return null;
            }

            StringBuilder oldBuilder = lastBuilder;
            int builderSize = builderStack.size();
            if (builderSize == 0) {
                lastBuilder = null;
            } else {
                lastBuilder = builderStack.remove(builderSize - 1);
            }
            return releaseBuilder(oldBuilder);
        }

        protected void addText(String text) throws SAXParseException {
            if (lastWhitespace) {
                if (text.trim().isEmpty()) {
                    return;
                } else {
                    throw new SAXParseException("Unexpected non-whitespace text [ " + text + " ]", getDocumentLocator());
                }

            } else {
                if (lastBuilder == null) {
                    throw new SAXParseException("Unexpected text [ " + text + " ]", getDocumentLocator());
                } else {
                    lastBuilder.append(text);
                }
            }
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
            throw new SAXParseException("Unknown element [ " + qName + " ]", getDocumentLocator());
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            throw new SAXParseException("Unknown element [ " + qName + " ]", getDocumentLocator());
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            String text = new String(ch, start, length);
            // System.out.println("Characters [ " + text + " ]");
            addText(text); // throws SAXParseException
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

    public static final long NS_IN_S = 1000000000L;

    private static final long[] GAP_LIMIT = { NS_IN_S / 10, // 100,000,000
                                              NS_IN_S / 100, //  10,000,000
                                              NS_IN_S / 1000, //   1,000,000
                                              NS_IN_S / 10000, //     100,000
                                              NS_IN_S / 100000, //      10,000
                                              NS_IN_S / 1000000, //       1,000
                                              NS_IN_S / 10000000, //         100
                                              NS_IN_S / 100000000 }; //          10

    private static final String[] GAPS = { "",
                                           "0",
                                           "00",
                                           "000",
                                           "0000",
                                           "00000",
                                           "000000",
                                           "0000000",
                                           "00000000" };

    public static String gap(long value) {
        int limitNo = 0;
        while ((limitNo < GAP_LIMIT.length) && (value < GAP_LIMIT[limitNo])) {
            limitNo++;
        }
        return GAPS[limitNo];
    }
}
