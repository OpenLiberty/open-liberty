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
import static com.ibm.ws.kernel.feature.internal.util.VerifyXMLConstants.DESCRIPTION_TAG;
import static com.ibm.ws.kernel.feature.internal.util.VerifyXMLConstants.DURATION_TAG;
import static com.ibm.ws.kernel.feature.internal.util.VerifyXMLConstants.INPUT_TAG;
import static com.ibm.ws.kernel.feature.internal.util.VerifyXMLConstants.KERNEL_TAG;
import static com.ibm.ws.kernel.feature.internal.util.VerifyXMLConstants.MULTIPLE_TAG;
import static com.ibm.ws.kernel.feature.internal.util.VerifyXMLConstants.NAME_TAG;
import static com.ibm.ws.kernel.feature.internal.util.VerifyXMLConstants.OUTPUT_TAG;
import static com.ibm.ws.kernel.feature.internal.util.VerifyXMLConstants.RESOLVED_TAG;
import static com.ibm.ws.kernel.feature.internal.util.VerifyXMLConstants.ROOT_TAG;
import static com.ibm.ws.kernel.feature.internal.util.VerifyXMLConstants.SERVER_TAG;

import java.io.File;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.List;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import com.ibm.ws.kernel.feature.internal.util.VerifyData.VerifyCase;
import com.ibm.ws.kernel.feature.internal.util.VerifyData.VerifyInput;
import com.ibm.ws.kernel.feature.internal.util.VerifyData.VerifyOutput;

public class VerifyXML extends BaseXML {
    //

    public static void write(File file, final List<LazySupplier<VerifyCase>> cases) throws Exception {
        write(file, new FailableConsumer<PrintWriter, Exception>() {
            @Override
            public void accept(PrintWriter pW) throws Exception {
                try (VerifyXMLWriter xW = new VerifyXMLWriter(pW)) {
                    xW.write(cases);
                }
            }
        });
    }

    public static void write(File file, final VerifyData verifyData) throws Exception {
        write(file, new FailableConsumer<PrintWriter, Exception>() {
            @Override
            public void accept(PrintWriter pW) throws Exception {
                try (VerifyXMLWriter xW = new VerifyXMLWriter(pW)) {
                    xW.write(verifyData);
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

        public void write(List<LazySupplier<VerifyCase>> cases) throws Exception {
            openElement(CASES_TAG);
            upIndent();

            for (LazySupplier<VerifyCase> verifyCase : cases) {
                write(verifyCase.get());
            }

            downIndent();
            closeElement(CASES_TAG);
        }

        public void write(VerifyCase verifyCase) {
            openElement(CASE_TAG);
            upIndent();

            printElement(NAME_TAG, verifyCase.name);
            printElement(DESCRIPTION_TAG, verifyCase.description);
            printElementNsAsS(DURATION_TAG, verifyCase.durationNs);

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

            downIndent();
            closeElement(INPUT_TAG);
        }

        public void write(VerifyOutput verifyOutput) {
            openElement(OUTPUT_TAG);
            upIndent();

            for (String resolved : verifyOutput.resolved) {
                printElement(RESOLVED_TAG, resolved);
            }

            downIndent();
            closeElement(OUTPUT_TAG);
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

            } else if (pushElement(qName, OUTPUT_TAG, CASE_TAG)) {
                whitespaceOnly = true;
            } else if (pushElement(qName, RESOLVED_TAG, OUTPUT_TAG)) {
                // ignore

            } else {
                super.startElement(uri, localName, qName, atts);
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

            } else if (oldLast.equals(OUTPUT_TAG)) {
                // ignore
            } else if (oldLast.equals(RESOLVED_TAG)) {
                verifyCase.output.addResolved(elementText);

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
