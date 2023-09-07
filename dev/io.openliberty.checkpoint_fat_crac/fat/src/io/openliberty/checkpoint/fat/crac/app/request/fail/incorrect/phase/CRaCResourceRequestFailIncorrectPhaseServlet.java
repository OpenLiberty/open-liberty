/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.checkpoint.fat.crac.app.request.fail.incorrect.phase;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;

import org.crac.CheckpointException;
import org.crac.Core;
import org.crac.RestoreException;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/crac-test", loadOnStartup = 1)
public class CRaCResourceRequestFailIncorrectPhaseServlet extends HttpServlet {

    @Override
    public void init(ServletConfig config) throws ServletException {
        System.out.println(getClass().getSimpleName() + ": Registering test resources.");
        try {
            Core.checkpointRestore();
            String testMessage = "FAILED - control back after restore";
            System.out.println(testMessage);
            throw new ServletException(testMessage);
        } catch (RestoreException e) {
            System.out.println("TESTING - got RestoreException.");
            throw new ServletException(e);
        } catch (CheckpointException e) {
            // expected
            config.getServletContext().log("Got CheckpointException", e);
            System.out.println("TESTING - got CheckpointException: " + e.getMessage());
        }
        System.out.println(getClass().getSimpleName() + ": end init()");
    }
}
