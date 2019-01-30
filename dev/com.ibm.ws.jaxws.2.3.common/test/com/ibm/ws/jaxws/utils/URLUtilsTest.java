/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxws.utils;

import junit.framework.Assert;

import org.junit.Test;

/**
 *
 */
public class URLUtilsTest {

    @Test
    public void testIsAbsolutePath() throws Exception {

        String[] absoluteURIs = { "file:/WEB-INF/lib/a.jar", "file:\\WEB-INF\\lib\\b.jar", "jar://c:/test.jar", "http://www.ibm.com/abc.wsdl" };
        for (String absoluteURI : absoluteURIs) {
            Assert.assertTrue("False is returned for a absolute URI from URLUtils.isAbsolutePath(absoluteURI)", URLUtils.isAbsolutePath(absoluteURI));
        }

        String[] relativeURIs = { "WEB-INF/lib/a.jar", "\\WEB-INF\\lib\\b.jar", "WEB-INF\\lib\\a.jar", "/WEB-INF/lib/a.jar" };
        for (String relativeURI : relativeURIs) {
            Assert.assertFalse("True is returned for a absolute URI from URLUtils.isAbsolutePath(relativeURI)", URLUtils.isAbsolutePath(relativeURI));
        }
    }
}
