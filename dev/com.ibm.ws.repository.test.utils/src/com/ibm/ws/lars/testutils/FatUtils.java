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

package com.ibm.ws.lars.testutils;

import java.net.URI;
import java.net.URISyntaxException;

import com.ibm.ws.lars.testutils.fixtures.LarsRepositoryFixture;
import com.ibm.ws.lars.testutils.fixtures.RepositoryFixture;

import componenttest.topology.impl.LibertyServer;

public class FatUtils {

    public static final String LARS_APPLICATION_ROOT = "/ma/v1";

    public static final String ADMIN_USERNAME = "admin";

    public static final String ADMIN_PASSWORD = "passw0rd";

    public static final String USER_ROLE_USERNAME = "user";

    public static final String USER_ROLE_PASSWORD = "passw0rd";

    private static RepositoryFixture fatRepo = null;

    public static int countLines(String input) {
        int lines = 0;
        // If the last character isn't \n, add one, as that will make
        // the count match what you would normally expect
        if (input.charAt(input.length() - 1) != '\n') {
            lines = 1;
        }
        while (true) {
            int nlIndex = input.indexOf('\n');
            if (nlIndex == -1) {
                break;
            }
            lines++;
            input = input.substring(nlIndex + 1);
        }
        return lines;
    }

    public static void setRestFixture(LibertyServer server) {
        try {
            URI serverUri = new URI("http", null, server.getHostname(), server.getHttpDefaultPort(), LARS_APPLICATION_ROOT, null, null);
            fatRepo = LarsRepositoryFixture.createFixture(serverUri.toString(), "1", ADMIN_USERNAME, ADMIN_PASSWORD, USER_ROLE_USERNAME, USER_ROLE_PASSWORD);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public static RepositoryFixture getRestFixture() {
        return fatRepo;
    }

}
