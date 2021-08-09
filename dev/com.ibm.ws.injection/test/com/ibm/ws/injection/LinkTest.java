/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.injection;

import org.junit.Assert;
import org.junit.Test;

import com.ibm.ws.injectionengine.osgi.util.Link;

public class LinkTest {

    @Test
    public void testLinkParser() {
        test("origin.jar", "", null, null, "", true);
        test("origin.jar", "name", null, null, "name", true);
        test("origin.jar", "modname/name", null, "modname", "name", true);
        test("origin.jar", "weirdmodprefix/modname/name", null, "weirdmodprefix/modname", "name", true);
        test("origin.jar", "uri.jar#name", "uri.jar", null, "name", true);
        test("origin.jar", "dir/uri.jar#name", "dir/uri.jar", null, "name", true);
        test("origin.jar", "../uri.jar#name", "uri.jar", null, "name", true);
        test("origin.jar", "../dir/uri.jar#name", "dir/uri.jar", null, "name", true);
        test("origin.jar", "../../uri.jar#name", "uri.jar", null, "name", true);
        test("origin.jar", "../../dir/uri.jar#name", "dir/uri.jar", null, "name", true);
        test("origindir/origin.jar", "uri.jar#name", "origindir/uri.jar", null, "name", true);
        test("origindir/origin.jar", "dir/uri.jar#name", "origindir/dir/uri.jar", null, "name", true);
        test("origindir/origin.jar", "../uri.jar#name", "origindir/uri.jar", null, "name", true);
        test("origindir/origin.jar", "../dir/uri.jar#name", "origindir/dir/uri.jar", null, "name", true);
        test("origindir/origin.jar", "../../uri.jar#name", "uri.jar", null, "name", true);
        test("origindir/origin.jar", "../../dir/uri.jar#name", "dir/uri.jar", null, "name", true);

        test("origin.jar", "jndi/linkname", null, null, "jndi/linkname", false);
        test("origin.jar", "weirdmodprefix/link/name", null, null, "weirdmodprefix/link/name", false);
        test(null, "name", null, null, "name", false);
        test(null, "mod#name", "mod", null, "name", false);

    }

    private static boolean equals(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }

    private static void test(String origin, String input, String expectedModuleURI, String expectedModuleName, String expectedName, boolean allowModule) {
        Link link = Link.parse(origin, input, allowModule);
        Assert.assertTrue("A link did not parse correctly: origin=" + origin + ", input=" + input + ", link=" + link,
                          equals(expectedModuleURI, link.moduleURI) && equals(expectedModuleName, link.moduleName) && equals(expectedName, link.name));
    }

}
