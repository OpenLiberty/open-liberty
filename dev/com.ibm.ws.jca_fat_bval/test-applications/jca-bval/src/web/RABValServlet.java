/*******************************************************************************
 * Copyright (c) 2014, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package web;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

import javax.annotation.Resource;
import javax.resource.cci.Connection;
import javax.resource.cci.ConnectionFactory;
import javax.resource.cci.ConnectionSpec;
import javax.resource.cci.Interaction;
import javax.resource.cci.InteractionSpec;
import javax.resource.cci.MappedRecord;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/*")
public class RABValServlet extends HttpServlet {
    private static final long serialVersionUID = 7709282314904580335L;

    @Resource(lookup = "eis/conFactory")
    private ConnectionFactory conFactory;

    @Resource(lookup = "eis/conSpec")
    private ConnectionSpec conSpec;

    @Resource(lookup = "eis/iSpec_ADD")
    private InteractionSpec iSpec_ADD;

    @Resource(lookup = "eis/iSpec_FIND")
    private InteractionSpec iSpec_FIND;

    @Resource(lookup = "eis/iSpec_REMOVE")
    private InteractionSpec iSpec_REMOVE;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String function = request.getParameter("functionName");
        if (function == null)
            return;
        PrintWriter out = response.getWriter();
        System.out.println(">>>> Entering servlet. Query: " + request.getQueryString());

        try {
            MappedRecord output = conFactory.getRecordFactory().createMappedRecord("output");
            MappedRecord input = conFactory.getRecordFactory().createMappedRecord("input");
            for (Map.Entry<String, String[]> param : request.getParameterMap().entrySet())
                if (!"functionName".equalsIgnoreCase(param.getKey()) && !"testMethod".equalsIgnoreCase(param.getKey()))
                    input.put(param.getKey(), param.getValue()[0]);

            InteractionSpec ispec = "ADD"
                            .equalsIgnoreCase(function) ? iSpec_ADD : "FIND".equalsIgnoreCase(function) ? iSpec_FIND : "REMOVE".equalsIgnoreCase(function) ? iSpec_REMOVE : null;

            String message;
            Connection con = conFactory.getConnection(conSpec);
            try {
                Interaction interaction = con.createInteraction();
                message = interaction.execute(ispec, input, output) ? //
                                ("Successfully performed " + function + " with output: " + output) : //
                                ("Did not " + function + " any entries.");
                interaction.close();
            } finally {
                con.close();
            }

            out.println(message);
            out.println("SUCCESS");
            System.out.println("<<<< Exiting servlet. " + message);
        } catch (Throwable x) {
            while (x instanceof InvocationTargetException)
                x = x.getCause();
            System.out.println("Exiting servlet with exception. " + x);
            x.printStackTrace(System.out);
            out.println("<pre>ERROR: ");
            x.printStackTrace(out);
            out.println("</pre>");
        }
    }
}