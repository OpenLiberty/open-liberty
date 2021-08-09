/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.fat.common.expectations;

import java.util.Arrays;
import java.util.List;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.Constants;

import componenttest.topology.impl.LibertyServer;

public class ServerMessageExpectation extends Expectation {

    public static final String DEFAULT_FAILURE_MSG = "Did not find the expected error message [%s] in the server log.";

    private LibertyServer server = null;

    public ServerMessageExpectation(LibertyServer server, String searchFor) {
        this(null, server, searchFor);
    }

    //    public ServerMessageExpectation(LibertyServer server, String logFile, String searchFor) {
    //        super(null, logFile, Constants.STRING_MATCHES, searchFor, String.format(DEFAULT_FAILURE_MSG, searchFor));
    //        this.server = server;
    //    }

    public ServerMessageExpectation(LibertyServer server, String searchFor, String failureMsg) {
        super(null, Constants.MESSAGES_LOG, Constants.STRING_MATCHES, searchFor, failureMsg);
        this.server = server;
    }

    public ServerMessageExpectation(String testAction, LibertyServer server, String searchFor) {
        this(testAction, server, searchFor, String.format(DEFAULT_FAILURE_MSG, searchFor));
    }

    public ServerMessageExpectation(String testAction, LibertyServer server, String searchFor, String failureMsg) {
        super(testAction, Constants.MESSAGES_LOG, Constants.STRING_MATCHES, searchFor, failureMsg);
        this.server = server;
    }

    @Override
    protected void validate(Object contentToValidate) throws Exception {
        addMessageToIgnoredErrors();
        if (!isMessageLogged()) {
            throw new Exception(failureMsg);
        }
    }

    void addMessageToIgnoredErrors() {
        List<String> msgs = Arrays.asList(validationValue);
        server.addIgnoredErrors(msgs);
    }

    boolean isMessageLogged() {
        String errorMsg = waitForStringInLogFile();
        boolean isMessageLogged = errorMsg != null;
        String logMsg = isMessageLogged ? ("Found message: " + errorMsg) : "Did NOT find message [" + validationValue + "] in " + server.getServerName() + " server log!";
        Log.info(getClass(), "isMessageLogged", logMsg);
        return isMessageLogged;
    }

    String waitForStringInLogFile() {
        if (Constants.TRACE_LOG.equals(searchLocation)) {
            return server.waitForStringInTraceUsingMark(validationValue, 100);
        } else {
            return server.waitForStringInLogUsingMark(validationValue, 100);
        }
    }

}