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

import static com.ibm.ws.kernel.feature.internal.util.VerifyXMLConstants.CASES_TAG;
import static com.ibm.ws.kernel.feature.internal.util.VerifyXMLConstants.CASE_TAG;
import static com.ibm.ws.kernel.feature.internal.util.VerifyXMLConstants.CLIENT_TAG;
import static com.ibm.ws.kernel.feature.internal.util.VerifyXMLConstants.CONFLICT_FEATURE_TAG;
import static com.ibm.ws.kernel.feature.internal.util.VerifyXMLConstants.DESCRIPTION_TAG;
import static com.ibm.ws.kernel.feature.internal.util.VerifyXMLConstants.DURATION_TAG;
import static com.ibm.ws.kernel.feature.internal.util.VerifyXMLConstants.ENV_CHAR;
import static com.ibm.ws.kernel.feature.internal.util.VerifyXMLConstants.ENV_TAG;
import static com.ibm.ws.kernel.feature.internal.util.VerifyXMLConstants.INPUT_TAG;
import static com.ibm.ws.kernel.feature.internal.util.VerifyXMLConstants.KERNEL_BLOCKED_TAG;
import static com.ibm.ws.kernel.feature.internal.util.VerifyXMLConstants.KERNEL_ONLY_TAG;
import static com.ibm.ws.kernel.feature.internal.util.VerifyXMLConstants.KERNEL_TAG;
import static com.ibm.ws.kernel.feature.internal.util.VerifyXMLConstants.MISSING_FEATURE_TAG;
import static com.ibm.ws.kernel.feature.internal.util.VerifyXMLConstants.MULTIPLE_TAG;
import static com.ibm.ws.kernel.feature.internal.util.VerifyXMLConstants.NAME_TAG;
import static com.ibm.ws.kernel.feature.internal.util.VerifyXMLConstants.NON_PUBLIC_FEATURE_TAG;
import static com.ibm.ws.kernel.feature.internal.util.VerifyXMLConstants.OUTPUT_TAG;
import static com.ibm.ws.kernel.feature.internal.util.VerifyXMLConstants.PLATFORM_DUPLICATE_TAG;
import static com.ibm.ws.kernel.feature.internal.util.VerifyXMLConstants.PLATFORM_MISSING_TAG;
import static com.ibm.ws.kernel.feature.internal.util.VerifyXMLConstants.PLATFORM_RESOLVED_TAG;
import static com.ibm.ws.kernel.feature.internal.util.VerifyXMLConstants.PLATFORM_TAG;
import static com.ibm.ws.kernel.feature.internal.util.VerifyXMLConstants.RESOLVED_CHAR;
import static com.ibm.ws.kernel.feature.internal.util.VerifyXMLConstants.RESOLVED_FEATURE_TAG;
import static com.ibm.ws.kernel.feature.internal.util.VerifyXMLConstants.RESOLVED_TAG;
import static com.ibm.ws.kernel.feature.internal.util.VerifyXMLConstants.ROOT_TAG;
import static com.ibm.ws.kernel.feature.internal.util.VerifyXMLConstants.SERVER_TAG;
import static com.ibm.ws.kernel.feature.internal.util.VerifyXMLConstants.VERSIONLESS_RESOLVED_TAG;
import static com.ibm.ws.kernel.feature.internal.util.VerifyXMLConstants.WRONG_PROCESS_FEATURE_TAG;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import com.ibm.ws.kernel.feature.internal.util.VerifyData.ResultData;
import com.ibm.ws.kernel.feature.internal.util.VerifyData.VerifyCase;
import com.ibm.ws.kernel.feature.internal.util.VerifyData.VerifyInput;
import com.ibm.ws.kernel.feature.internal.util.VerifyData.VerifyOutput;

public class VerifyXML extends BaseXML {
    public static void writeDurations(File file, List<LazySupplierImpl<VerifyCase>> cases) throws Exception {
        try (FileWriter fW = new FileWriter(file, !DO_APPEND);
                        PrintWriter pW = new PrintWriter(fW)) {
            writeDurations(pW, cases);
        }
    }

    public static void writeDurations(PrintWriter output, List<LazySupplierImpl<VerifyCase>> cases) throws Exception {
        StringBuilder lineBuilder = new StringBuilder();

        for (LazySupplierImpl<VerifyCase> caseSupplier : cases) {
            // Only write the case if the supplier was previously
            // successful.

            VerifyCase verifyCase = caseSupplier.getSupplied();
            if (verifyCase == null) {
                continue;
            }

            long durationNs = verifyCase.durationNs;
            long ns = durationNs % BaseXML.NS_IN_S;
            long s = (durationNs - ns) / BaseXML.NS_IN_S;

            String nsText = Long.toString(ns);
            String sText = Long.toString(s);

            lineBuilder.append(verifyCase.name);
            lineBuilder.append(' ');

            lineBuilder.append(sText);
            lineBuilder.append('.');
            lineBuilder.append(BaseXML.gap(ns));
            lineBuilder.append(nsText);
            lineBuilder.append(' ');
            lineBuilder.append('s');

            output.println(lineBuilder);

            lineBuilder.setLength(0);
        }
    }

    //

    public static void writeCases(PrintStream output, final List<? extends VerifyCase> cases) throws Exception {
        write(output, new FailableConsumer<PrintWriter, Exception>() {
            @Override
            public void accept(PrintWriter pW) throws Exception {
                @SuppressWarnings("resource")
                VerifyXMLWriter xW = new VerifyXMLWriter(pW);
                try {
                    xW.writeCases(cases);
                } finally {
                    xW.flush();
                }
            }
        });
    }

    public static void write(File file, final List<LazySupplierImpl<VerifyCase>> cases) throws Exception {
        write(file, new FailableConsumer<PrintWriter, Exception>() {
            @Override
            public void accept(PrintWriter pW) throws Exception {
                @SuppressWarnings("resource")
                VerifyXMLWriter xW = new VerifyXMLWriter(pW);
                try {
                    xW.write(cases);
                } finally {
                    xW.flush();
                }
            }
        });
    }

    public static void write(PrintStream output, final VerifyData verifyData) throws Exception {
        write(output, new FailableConsumer<PrintWriter, Exception>() {
            @Override
            public void accept(PrintWriter pW) throws Exception {
                @SuppressWarnings("resource")
                VerifyXMLWriter xW = new VerifyXMLWriter(pW);
                try {
                    xW.write(verifyData);
                } finally {
                    xW.flush();
                }
            }
        });
    }

    public static void write(File file, final VerifyData verifyData) throws Exception {
        write(file, new FailableConsumer<PrintWriter, Exception>() {
            @Override
            public void accept(PrintWriter pW) throws Exception {
                @SuppressWarnings("resource")
                VerifyXMLWriter xW = new VerifyXMLWriter(pW);
                try {
                    xW.write(verifyData);
                } finally {
                    xW.flush();
                }
            }
        });
    }

    public static class VerifyXMLWriter extends BaseXMLWriter {
        public VerifyXMLWriter(PrintWriter pW) {
            super(pW);
        }

        public void write(VerifyData verifyData) {
            openElement(CASES_TAG);
            upIndent();

            for (VerifyCase verifyCase : verifyData.cases) {
                write(verifyCase);
            }

            downIndent();
            closeElement(CASES_TAG);
        }

        public void writeCases(List<? extends VerifyCase> cases) throws Exception {
            openElement(CASES_TAG);
            upIndent();

            for (VerifyCase verifyCase : cases) {
                write(verifyCase);
            }

            downIndent();
            closeElement(CASES_TAG);
        }

        public void write(List<LazySupplierImpl<VerifyCase>> cases) throws Exception {
            openElement(CASES_TAG);
            upIndent();

            for (LazySupplierImpl<VerifyCase> verifyCase : cases) {
                write(verifyCase.supply());
            }

            downIndent();
            closeElement(CASES_TAG);
        }

        public void write(VerifyCase verifyCase) {
            openElement(CASE_TAG);
            upIndent();

            printElement(NAME_TAG, verifyCase.name);
            printElement(DESCRIPTION_TAG, verifyCase.description);

            // Don't print this, since it makes comparing repository listings
            // difficult.

            // printElementNsAsS(DURATION_TAG, verifyCase.durationNs);

            write(verifyCase.input);
            write(verifyCase.output);

            downIndent();
            closeElement(CASE_TAG);
        }

        public void write(VerifyInput verifyInput) {
            openElement(INPUT_TAG);
            upIndent();

            if (verifyInput.isMultiple) {
                printElement(MULTIPLE_TAG);
            }
            if (verifyInput.isClient) {
                printElement(CLIENT_TAG);
            }
            if (verifyInput.isServer) {
                printElement(SERVER_TAG);
            }
            for (String kernelFeature : verifyInput.kernel) {
                printElement(KERNEL_TAG, kernelFeature);
            }
            for (String root : verifyInput.roots) {
                printElement(ROOT_TAG, root);
            }
            for (String platform : verifyInput.platforms) {
                printElement(PLATFORM_TAG, platform);
            }
            for (Map.Entry<String, String> envEntry : verifyInput.envMap.entrySet()) {
                printElement(ENV_TAG, envEntry.getKey() + ENV_CHAR + envEntry.getValue());
            }

            downIndent();
            closeElement(INPUT_TAG);
        }

        public void write(VerifyOutput verifyOutput) {
            openElement(OUTPUT_TAG);
            upIndent();

            for (String feature : verifyOutput.kernelOnly) {
                printElement(KERNEL_ONLY_TAG, feature);
            }
            for (String feature : verifyOutput.kernelBlocked) {
                printElement(KERNEL_BLOCKED_TAG, feature);
            }

            for (ResultData dataType : ResultData.values()) {
                String elementTag = resultTags.get(dataType);

                if (dataType != ResultData.FEATURE_VERSIONLESS_RESOLVED) {
                    for (String value : verifyOutput.get(dataType)) {
                        printElement(elementTag, value);
                    }
                } else {
                    Map<String, String> versionlessResolved = verifyOutput.getVersionlessResolved();
                    for (Map.Entry<String, String> resolvedEntry : versionlessResolved.entrySet()) {
                        String value = resolvedEntry.getKey() + RESOLVED_CHAR + resolvedEntry.getValue();
                        printElement(elementTag, value);
                    }
                }
            }

            downIndent();
            closeElement(OUTPUT_TAG);
        }
    }

    public static final EnumMap<ResultData, String> resultTags;
    public static final Map<String, ResultData> tagResults;

    // @formatter:off
    static {
        EnumMap<ResultData, String> useTags = new EnumMap<ResultData, String>(ResultData.class);
        Map<String, ResultData> useResults = new HashMap<String, ResultData>();

        put(useTags, useResults, ResultData.PLATFORM_RESOLVED, PLATFORM_RESOLVED_TAG);
        put(useTags, useResults, ResultData.PLATFORM_MISSING, PLATFORM_MISSING_TAG);
        put(useTags, useResults, ResultData.PLATFORM_DUPLICATE, PLATFORM_DUPLICATE_TAG);
        put(useTags, useResults, ResultData.FEATURE_VERSIONLESS_RESOLVED, VERSIONLESS_RESOLVED_TAG);
        put(useTags, useResults, ResultData.FEATURE_RESOLVED, RESOLVED_FEATURE_TAG);
        put(useTags, useResults, ResultData.FEATURE_MISSING, MISSING_FEATURE_TAG);
        put(useTags, useResults, ResultData.FEATURE_NON_PUBLIC, NON_PUBLIC_FEATURE_TAG);
        put(useTags, useResults, ResultData.FEATURE_WRONG_PROCESS, WRONG_PROCESS_FEATURE_TAG);
        put(useTags, useResults, ResultData.FEATURE_CONFLICT, CONFLICT_FEATURE_TAG);

        resultTags = useTags;
        tagResults = useResults;
    }

    private static void put(EnumMap<ResultData, String> useTags,
                            Map<String, ResultData> useResults,
                            ResultData dataType, String tag) {
        useTags.put(dataType, tag);
        useResults.put(tag, dataType);
    }

    // @formatter:on

    public static VerifyData read(File file) throws Exception {
        VerifyContentHandler contentHandler = new VerifyContentHandler();
        VerifyErrorHandler errorHandler = new VerifyErrorHandler(System.out);

        BaseXML.read(file, contentHandler, errorHandler);

        return contentHandler.verifyData;
    }

    public static class VerifyContentHandler extends BaseContentHandler {
        public VerifyContentHandler() {
            super();
        }

        //

        private VerifyData verifyData;

        private void startData() {
            verifyData = new VerifyData();
        }

        private void endData() {
            // Nothing
        }

        private VerifyCase verifyCase;

        private void startCase() {
            verifyCase = verifyData.addCase();
        }

        private void endCase() {
            verifyCase = null;
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
            // System.out.println("Start [ " + qName + " ]");

            boolean whitespaceOnly = false;

            if (pushElement(qName, CASES_TAG, null)) {
                startData();
                whitespaceOnly = true;

            } else if (pushElement(qName, CASE_TAG, CASES_TAG)) {
                startCase();
                whitespaceOnly = true;
            } else if (pushElement(qName, NAME_TAG, CASE_TAG)) {
                // ignore
            } else if (pushElement(qName, DESCRIPTION_TAG, CASE_TAG)) {
                // ignore
            } else if (pushElement(qName, DURATION_TAG, CASE_TAG)) {
                // ignore

            } else if (pushElement(qName, INPUT_TAG, CASE_TAG)) {
                whitespaceOnly = true;
            } else if (pushElement(qName, MULTIPLE_TAG, INPUT_TAG)) {
                // ignore
            } else if (pushElement(qName, CLIENT_TAG, INPUT_TAG)) {
                // ignore
            } else if (pushElement(qName, SERVER_TAG, INPUT_TAG)) {
                // ignore
            } else if (pushElement(qName, KERNEL_TAG, INPUT_TAG)) {
                // ignore
            } else if (pushElement(qName, ROOT_TAG, INPUT_TAG)) {
                // ignore
            } else if (pushElement(qName, PLATFORM_TAG, INPUT_TAG)) {
                // ignore
            } else if (pushElement(qName, ENV_TAG, INPUT_TAG)) {
                // ignore

            } else if (pushElement(qName, OUTPUT_TAG, CASE_TAG)) {
                whitespaceOnly = true;
            } else if (pushElement(qName, RESOLVED_TAG, OUTPUT_TAG)) {
                // Provide compatibility with the original format.
                // ignore
            } else {
                boolean isResultElement = false;
                for (String tag : resultTags.values()) {
                    if (pushElement(qName, tag, OUTPUT_TAG)) {
                        isResultElement = true;
                        break;
                    }
                }
                if (!isResultElement) {
                    super.startElement(uri, localName, qName, atts);
                }
            }

            pushBuilder(whitespaceOnly);
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            // System.out.println("End [ " + qName + " ]");

            String elementText = popBuilder();
            String oldLast = popElement();

            if (oldLast.equals(CASES_TAG)) {
                endData();
            } else if (oldLast.equals(CASE_TAG)) {
                endCase();
            } else if (oldLast.equals(NAME_TAG)) {
                verifyCase.name = elementText;
            } else if (oldLast.equals(DESCRIPTION_TAG)) {
                verifyCase.description = elementText;
            } else if (oldLast.equals(DURATION_TAG)) {
                verifyCase.durationNs = parseSAsNS(elementText);

            } else if (oldLast.equals(INPUT_TAG)) {
                // ignore
            } else if (oldLast.contentEquals(MULTIPLE_TAG)) {
                verifyCase.input.setMultiple();
            } else if (oldLast.contentEquals(CLIENT_TAG)) {
                verifyCase.input.setClient();
            } else if (oldLast.contentEquals(SERVER_TAG)) {
                verifyCase.input.setServer();
            } else if (oldLast.equals(KERNEL_TAG)) {
                verifyCase.input.addKernel(elementText);
            } else if (oldLast.equals(ROOT_TAG)) {
                verifyCase.input.addRoot(elementText);
            } else if (oldLast.equals(PLATFORM_TAG)) {
                verifyCase.input.addPlatform(elementText);
            } else if (oldLast.equals(ENV_TAG)) {
                putEnvironment(elementText);

            } else if (oldLast.equals(OUTPUT_TAG)) {
                // ignore
            } else if (oldLast.equals(RESOLVED_TAG)) {
                // Provide compatibility with the original format.
                verifyCase.output.addResolved(elementText);
            } else {
                boolean isResultTag = false;
                for (String tag : resultTags.values()) {
                    if (oldLast.equals(tag)) {
                        if (tag.equals(VERSIONLESS_RESOLVED_TAG)) {
                            putVersionless(elementText);
                        } else {
                            verifyCase.output.add(tagResults.get(tag), elementText);
                        }
                        isResultTag = true;
                        break;
                    }
                }

                if (!isResultTag) {
                    super.endElement(uri, localName, qName);
                }
            }
        }

        private void putEnvironment(String elementText) {
            String name;
            String value;

            int sepOffset = elementText.indexOf(ENV_CHAR);
            if (sepOffset == -1) {
                name = elementText;
                value = "";
            } else {
                name = elementText.substring(0, sepOffset);
                value = elementText.substring(sepOffset + 1);
            }

            verifyCase.input.putEnvironment(name, value);
        }

        private void putVersionless(String elementText) {
            String name;
            String value;

            int sepOffset = elementText.indexOf(RESOLVED_CHAR);
            if (sepOffset == -1) {
                name = elementText;
                value = "";
            } else {
                name = elementText.substring(0, sepOffset);
                value = elementText.substring(sepOffset + 1);
            }

            verifyCase.output.putVersionlessResolved(name, value);
        }
    }

    public static class VerifyErrorHandler extends BaseErrorHandler {
        public VerifyErrorHandler(PrintStream printer) {
            super(printer);
        }
    }
}
