/*******************************************************************************
 * Copyright (c) 2017, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package configmod.web.lookup;

import static junit.framework.Assert.fail;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import componenttest.app.FATServlet;

/**
 * This servlet intentionally has no dependency on the Cloudant API, so that it can be used
 * independently of whether the Cloudant library is included in the application.
 */
@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/CloudantLookupTestServlet")
public class CloudantLookupServlet extends FATServlet {
    public void testDatabaseInsert(HttpServletRequest request, HttpServletResponse response) throws Exception {
        Object db = InitialContext.doLookup(request.getParameter("jndiName"));

        // Copied from CloudantConfigModServlet.testInsert so that database entries will be compatible across both
        Map<String, String> animal = new LinkedHashMap<String, String>();
        for (String attr : Arrays.asList("kingdom", "phylum", "order", "class", "family", "genus", "species"))
            animal.put(attr, request.getParameter(attr));
        animal.put("_id", animal.get("genus") + ' ' + animal.get("species"));

        // Reflectively invoke: db.save(animal);
        db.getClass().getMethod("save", Object.class).invoke(db, animal);
    }

    public void testLookupFailsClassNotFoundException(HttpServletRequest request, HttpServletResponse response) throws Exception {
        try {
            Object o = InitialContext.doLookup(request.getParameter("jndiName"));
            fail("Should not be able to look up " + o);
        } catch (NamingException x) {
            for (Throwable cause = x; cause != null; cause = cause.getCause())
                if (cause instanceof ClassNotFoundException)
                    return; // expected
            throw x;
        }
    }
}
