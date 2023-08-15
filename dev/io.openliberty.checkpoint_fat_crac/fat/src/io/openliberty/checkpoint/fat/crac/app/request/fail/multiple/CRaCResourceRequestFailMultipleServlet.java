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
package io.openliberty.checkpoint.fat.crac.app.request.fail.multiple;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.crac.CheckpointException;
import org.crac.Core;
import org.crac.RestoreException;
import org.junit.Test;

import componenttest.app.FATServlet;
import junit.framework.Assert;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/crac-test", loadOnStartup = 1)
public class CRaCResourceRequestFailMultipleServlet extends FATServlet {

    @Override
    public void init() throws ServletException {
        super.init();
        try {
            Core.checkpointRestore();
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    @Test
    public void testCheckpointMultiple(HttpServletRequest request, HttpServletResponse resp) throws Exception {
        try {
            // expect checkpoint failure after already doing a checkpoint in init
            Core.checkpointRestore();
            Assert.fail("FAILED - control back after restore");
        } catch (RestoreException e) {
            Assert.fail("FAILED - got RestoreException: " + e.getMessage());
            e.printStackTrace();
        } catch (CheckpointException e) {
            // expected
            request.getServletContext().log("Got CheckpointException", e);
            System.out.println("TESTING - got CheckpointException.");
        }
    }
}
