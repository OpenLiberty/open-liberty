/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.kernel.feature.resolver.util;

import static com.ibm.ws.kernel.feature.resolver.util.VerifyXMLConstants.CASE_TAG;
import static com.ibm.ws.kernel.feature.resolver.util.VerifyXMLConstants.CLIENT_TAG;
import static com.ibm.ws.kernel.feature.resolver.util.VerifyXMLConstants.DESCRIPTION_TAG;
import static com.ibm.ws.kernel.feature.resolver.util.VerifyXMLConstants.INPUT_TAG;
import static com.ibm.ws.kernel.feature.resolver.util.VerifyXMLConstants.KERNEL_TAG;
import static com.ibm.ws.kernel.feature.resolver.util.VerifyXMLConstants.MULTIPLE_TAG;
import static com.ibm.ws.kernel.feature.resolver.util.VerifyXMLConstants.NAME_TAG;
import static com.ibm.ws.kernel.feature.resolver.util.VerifyXMLConstants.OUTPUT_TAG;
import static com.ibm.ws.kernel.feature.resolver.util.VerifyXMLConstants.RESOLVED_TAG;
import static com.ibm.ws.kernel.feature.resolver.util.VerifyXMLConstants.ROOT_TAG;
import static com.ibm.ws.kernel.feature.resolver.util.VerifyXMLConstants.SERVER_TAG;

import java.io.File;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.stream.Stream;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import com.ibm.ws.kernel.feature.resolver.util.VerifyData.VerifyCase;
import com.ibm.ws.kernel.feature.resolver.util.VerifyData.VerifyInput;
import com.ibm.ws.kernel.feature.resolver.util.VerifyData.VerifyOutput;

public class VerifyXML extends BaseXML {
    //

    public static void write(File file, Stream<VerifyCase> cases) throws Exception {
        write(file, (PrintWriter pW) -> {
            try (VerifyXMLWriter xW = new VerifyXMLWriter(pW)) {
                xW.write(cases);
            }
        });
    }

    public static void write(File file, VerifyData verifyData) throws Exception {
        write(file, (PrintWriter pW) -> {
            try (VerifyXMLWriter xW = new VerifyXMLWriter(pW)) {
                xW.write(verifyData);
            }
        });
    }

    public static class VerifyXMLWriter extends BaseXMLWriter {
        public VerifyXMLWriter(PrintWriter pW) {
            super(pW);
        }

        public void write(VerifyData verifyData) {
            verifyData.cases.forEach(this::write);
        }

        public void write(Stream<VerifyCase> cases) {
            cases.forEach(this::write);
        }

        public void write(VerifyCase verifyCase) {
            withinElement(CASE_TAG, () -> {
                printElement(NAME_TAG, verifyCase.name);
                printElement(DESCRIPTION_TAG, verifyCase.description);
                write(verifyCase.input);
                write(verifyCase.output);
            });
        }

        public void write(VerifyInput verifyInput) {
            withinElement(INPUT_TAG, () -> {
                if (verifyInput.isMultiple) {
                    printElement(MULTIPLE_TAG);
                }
                if (verifyInput.isClient) {
                    printElement(CLIENT_TAG);
                }
                if (verifyInput.isServer) {
                    printElement(SERVER_TAG);
                }
                verifyInput.kernel.forEach((String kernelFeature) -> printElement(KERNEL_TAG, kernelFeature));
                verifyInput.roots.forEach((String root) -> printElement(ROOT_TAG, root));
            });
        }

        public void write(VerifyOutput verifyOutput) {
            withinElement(OUTPUT_TAG, () -> {
                verifyOutput.resolved.forEach((String resolvedFeature) -> printElement(RESOLVED_TAG, resolvedFeature));
            });
        }
    }

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

        private final VerifyData verifyData = new VerifyData();

        private VerifyCase verifyCase = null;

        private void startCase() {
            verifyCase = verifyData.addCase();
        }

        private void endCase() {
            verifyCase = null;
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
            if (pushElement(localName, CASE_TAG, null)) {
                startCase();
            } else if (pushElement(localName, NAME_TAG, CASE_TAG)) {
                pushBuilder();
            } else if (pushElement(localName, DESCRIPTION_TAG, CASE_TAG)) {
                pushBuilder();

            } else if (pushElement(localName, INPUT_TAG, CASE_TAG)) {
                // ignore
            } else if (pushElement(localName, MULTIPLE_TAG, INPUT_TAG)) {
                // ignore
            } else if (pushElement(localName, CLIENT_TAG, INPUT_TAG)) {
                // ignore
            } else if (pushElement(localName, SERVER_TAG, INPUT_TAG)) {
                // ignore
            } else if (pushElement(localName, KERNEL_TAG, INPUT_TAG)) {
                pushBuilder();
            } else if (pushElement(localName, ROOT_TAG, INPUT_TAG)) {
                pushBuilder();

            } else if (pushElement(localName, OUTPUT_TAG, CASE_TAG)) {
                //ignore
            } else if (pushElement(localName, RESOLVED_TAG, OUTPUT_TAG)) {
                pushBuilder();

            } else {
                super.startElement(uri, localName, qName, atts);
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            String oldLast = popElement();

            if (oldLast.equals(CASE_TAG)) {
                endCase();
            } else if (oldLast.equals(NAME_TAG)) {
                verifyCase.name = popBuilder();
            } else if (oldLast.equals(DESCRIPTION_TAG)) {
                verifyCase.description = popBuilder();

            } else if (oldLast.equals(INPUT_TAG)) {
                // Ignore
            } else if (oldLast.contentEquals(MULTIPLE_TAG)) {
                verifyCase.input.setMultiple();
            } else if (oldLast.contentEquals(CLIENT_TAG)) {
                verifyCase.input.setClient();
            } else if (oldLast.contentEquals(SERVER_TAG)) {
                verifyCase.input.setServer();
            } else if (oldLast.equals(KERNEL_TAG)) {
                verifyCase.input.addKernel(popBuilder());
            } else if (oldLast.equals(ROOT_TAG)) {
                verifyCase.input.addRoot(popBuilder());

            } else if (oldLast.equals(OUTPUT_TAG)) {
                // Ignore
            } else if (oldLast.equals(RESOLVED_TAG)) {
                verifyCase.output.addResolved(popBuilder());

            } else {
                super.endElement(uri, localName, qName);
            }
        }
    }

    public static class VerifyErrorHandler extends BaseErrorHandler {
        public VerifyErrorHandler(PrintStream printer) {
            super(printer);
        }
    }
}
