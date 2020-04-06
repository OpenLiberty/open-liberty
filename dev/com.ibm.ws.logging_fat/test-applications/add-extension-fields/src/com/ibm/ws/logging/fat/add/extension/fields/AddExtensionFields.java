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
package com.ibm.ws.logging.fat.add.extension.fields;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.logging.hpel.LogRecordContext;
import com.ibm.websphere.logging.hpel.LogRecordContext.Extension;

@WebServlet("/addExtFields")
public class AddExtensionFields extends HttpServlet {

    /**  */
    private static final long serialVersionUID = 1L;
    public static final String TEST_EXT_NAME = "testExtension";
    public static final String TEST_EXT_VALUE = "extensionValue";

    private final static Extension MY_EXTENSION = new Extension() {
        @Override
        public String getValue() {
            return TEST_EXT_VALUE;
        }
    };

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Logger logger = Logger.getLogger("com.ibm.ws.logging.fat.add.extension.fields.AddExtensionFields");

        //to see endpoint
        PrintWriter pw = response.getWriter();
        pw.print("Creating extension fields.");

        //add extension field to see from create logs
        LogRecordContext.registerExtension(TEST_EXT_NAME, MY_EXTENSION);

        //see extension name
        logger.info("Created extension field: ext_testExtension.");
    }
}
