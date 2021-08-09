/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.wsoc.tests.all;

import io.openliberty.wsoc.util.wsoc.WsocTest;
import io.openliberty.wsoc.common.Constants;
import io.openliberty.wsoc.common.Utils;
import io.openliberty.wsoc.endpoints.client.basic.AnnotatedClientEP;
import io.openliberty.wsoc.endpoints.client.basic.ProgrammaticClientEP;

/**
 * Tests WebSocket Stuff
 * 
 * @author unknown
 */
public class WebSocketVersion11Test {

    public static int DEFAULT_TIMEOUT = Constants.getDefaultTimeout();

    private WsocTest wsocTest = null;

    public WebSocketVersion11Test(WsocTest test) {
        this.wsocTest = test;
    }

    public void testProgrammaticTextSuccess() throws Exception {

        String[] textValues = { "WEREWR", "ERERE" };
        wsocTest.runEchoTest(new ProgrammaticClientEP.TextTest(textValues), "/websocket11/codedText", textValues);

    }

    public void testProgrammaticReaderSuccess() throws Exception {

        String[] readerValues = { "blahblahblahblah", "12345678910" };
        // String[] readerValues = { "2blahblahblahblah", "343asfasdfasf" };
        wsocTest.runEchoTest(new ProgrammaticClientEP.ReaderTest(readerValues), "/websocket11/codedReader", readerValues);

    }

    public void testProgrammaticPartialTextSuccess() throws Exception {

        String[] textValues = { "WEREWR", "ERERE" };
        wsocTest.runEchoTest(new ProgrammaticClientEP.PartialTextTest(textValues), "/websocket11/codedPartialText", textValues, Constants.getLongTimeout());

    }

    public void testClientAnnoWholeServerProgPartial() throws Exception {

        String[] textValues = { "MESSAGE1", "SecondOne", "AndTheLast" };
        wsocTest.runEchoTest(new AnnotatedClientEP.AnnonotatedPartialTextTest(textValues), "/websocket11/codedPartialSenderText", textValues, Constants.getLongTimeout());

    }

    public void testProgrammaticInputStreamSuccess() throws Exception {

        // PLEASE DO NOT USE RANDOM TEST DATA!!!

        byte[][] data = Utils.getRandomBinaryByteArray(5, 100);
        byte[][] orig = Utils.duplicateByteArray(data);

        wsocTest.runEchoTest(new ProgrammaticClientEP.ByteArrayTest(data), "/websocket11/codedInputStream", orig);
    }

}