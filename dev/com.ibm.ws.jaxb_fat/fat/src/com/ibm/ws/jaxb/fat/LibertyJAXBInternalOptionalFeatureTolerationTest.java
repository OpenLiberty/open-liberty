/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxb.fat;

import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 * Separate server startup needed because the goal is to test override of internal.optional.jaxb-2.2 by jaxb-2.3
 * Since both features are marked as singleton, fail of this test shows toleration failure
 * server.xml has jaxb-2.3 and jaspic-1.1 features. jaspic-1.1 contains internal.optional.jaxb-2.2 that we are testing it's override
 */
@RunWith(FATRunner.class)
@SkipForRepeat("JAXB-2.3")
public class LibertyJAXBInternalOptionalFeatureTolerationTest extends FATServletClient {

    private static final String APP_NAME = "jaxbApp";

    @Server("jaxb_internal_optional_feature_toleration_fat")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultDropinApp(server, APP_NAME, "jaxb.web");
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }

    @Test
    public void testJaxbPrivateFeatureToleration() throws Exception {
        // Searching for exception raised by loading 2 singleton classes at the same time. Here we are looking jaxb-2.3 and indirectly loaded jaxb-2.2
        List<String> errMsgs = server.findStringsInLogsAndTrace(".* CWWKF0033E: .* jaxb-2.3");

        assertTrue("jaxb-2.3 internal optional feature toleration failed. Check following info for more details" + errMsgs, errMsgs.isEmpty());
    }

}
