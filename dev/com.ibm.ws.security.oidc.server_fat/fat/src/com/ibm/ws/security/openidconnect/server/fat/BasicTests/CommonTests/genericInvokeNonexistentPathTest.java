/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.openidconnect.server.fat.BasicTests.CommonTests;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.ws.security.oauth_oidc.fat.commonTest.CommonTest;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.Constants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.ValidationData.validationData;
import com.meterware.httpunit.WebConversation;

import componenttest.custom.junit.runner.FATRunner;

@RunWith(FATRunner.class)
public class genericInvokeNonexistentPathTest extends CommonTest {

    public static final String ACTION_INVOKE_NONEXISTENT_PAGE = "invokeNonexistentPage";

    @Test
    public void test_invokeNonexistentPageInOidcContext() throws Exception {
        WebConversation wc = new WebConversation();

        String nonExistentUrl = testOPServer.getServerHttpsString() + "/oidc/endpoint";

        List<validationData> expectations = validationTools.getDefault404VDataExpectations(ACTION_INVOKE_NONEXISTENT_PAGE);

        testOPServer.addIgnoredServerExceptions("CWOAU0039W");
        
        genericInvokeEndpoint(testName.getMethodName(), wc, null, nonExistentUrl, Constants.GETMETHOD, ACTION_INVOKE_NONEXISTENT_PAGE, null, null, expectations);
    }

}
