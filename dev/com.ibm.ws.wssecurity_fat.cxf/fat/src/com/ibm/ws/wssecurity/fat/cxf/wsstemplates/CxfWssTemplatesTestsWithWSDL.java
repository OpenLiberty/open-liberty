/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.wssecurity.fat.cxf.wsstemplates;

import org.junit.BeforeClass;

import com.ibm.ws.wssecurity.fat.cxf.usernametoken.CxfWssTemplatesTests;
import com.ibm.ws.wssecurity.fat.utils.common.UpdateWSDLPortNum;

public class CxfWssTemplatesTestsWithWSDL extends CxfWssTemplatesTests {

    static private UpdateWSDLPortNum newWsdl = null;
    static final private String serverName = "com.ibm.ws.wssecurity_fat.wsstemplates";

    @BeforeClass
    public static void setUp() throws Exception {
        commonSetUp(serverName, true, "/wsstemplatesclient/CxfWssTemplatesSvcClient");
    }

}
