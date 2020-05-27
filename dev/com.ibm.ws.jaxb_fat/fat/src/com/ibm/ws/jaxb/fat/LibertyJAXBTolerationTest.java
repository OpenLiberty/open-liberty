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

import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

@RunWith(FATRunner.class)
@SkipForRepeat({ "JAXRS", SkipForRepeat.EE9_FEATURES })
public class LibertyJAXBTolerationTest extends FATServletClient {

    @Server("jaxb_optional_feature_toleration_fat")
    public static LibertyServer optionalServer;

    @Server("jaxb_internal_optional_feature_toleration_fat")
    public static LibertyServer internalOptionalServer;

    /**
     * Separate server startup needed because the goal is to test override of optional.jaxb-2.2 by jaxb-2.3
     * Since both features are marked as singleton, fail of this test shows toleration failure
     * server.xml has jaxb-2.3 and jpaContainer-2.1 features. jpaContainer-2.1 contains optional.jaxb-2.2 that we are testing it's override
     */
    @Test
    @Mode(TestMode.FULL)
    public void testJaxbOptionalFeatureToleration() throws Exception {

        optionalServer.startServer();

        // Searching for exception raised by loading 2 singleton classes at the same time. Here we are looking jaxb-2.3 and indirectly loaded jaxb-2.2
        List<String> errMsgs = optionalServer.findStringsInLogsAndTrace(".* CWWKF0033E: .* jaxb-2.3");

        try {
            assertTrue("jaxb-2.3 optional feature toleration failed. Check following info for more details" + errMsgs, errMsgs.isEmpty());
        } finally {
            optionalServer.stopServer();
        }
    }

    /**
     * Separate server startup needed because the goal is to test override of internal.optional.jaxb-2.2 by jaxb-2.3
     * Since both features are marked as singleton, fail of this test shows toleration failure
     * server.xml has jaxb-2.3 and jaspic-1.1 features. jaspic-1.1 contains internal.optional.jaxb-2.2 that we are testing it's override
     */
    @Test
    @Mode(TestMode.FULL)
    public void testJaxbInternalOptionalFeatureToleration() throws Exception {

        internalOptionalServer.startServer();

        // Searching for exception raised by loading 2 singleton classes at the same time. Here we are looking jaxb-2.3 and indirectly loaded jaxb-2.2
        List<String> errMsgs = internalOptionalServer.findStringsInLogsAndTrace(".* CWWKF0033E: .* jaxb-2.3");

        try {
            assertTrue("jaxb-2.3 internal optional feature toleration failed. Check following info for more details" + errMsgs, errMsgs.isEmpty());
        } finally {
            internalOptionalServer.stopServer();
        }
    }
}
