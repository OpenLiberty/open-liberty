/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package jdbc.fat.derby.notice.web;

import static org.junit.Assert.assertNotNull;

import javax.naming.InitialContext;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import componenttest.app.FATServlet;

/**
 * Looks up data sources by JNDI name, which causes the data source service for each
 * to be activated. The data source service is a delayed component, so without a lookup
 * it is never activated and cannot issue the Derby notices.
 */
@SuppressWarnings("serial")
@WebServlet("/NoticeServlet")
public class NoticeServlet extends FATServlet {

    private static void lookup(String... jndiNames) throws Exception {
        InitialContext ctx = new InitialContext();
        for (String jndiName : jndiNames)
            assertNotNull(jndiName, ctx.lookup(jndiName));
    }

    @Test
    public void lookupEmbedded() throws Exception {
        lookup("jdbc/derbyEmbedded1", "jdbc/derbyEmbedded2");
    }

    @Test
    public void lookupClient() throws Exception {
        lookup("jdbc/derbyClient");
    }

    @Test
    public void lookupGeneric() throws Exception {
        lookup("jdbc/derbyGeneric");
    }

    @Test
    public void lookupAll() throws Exception {
        lookup("jdbc/derbyEmbedded1", "jdbc/derbyEmbedded2", "jdbc/derbyClient", "jdbc/derbyGeneric");
    }
}
