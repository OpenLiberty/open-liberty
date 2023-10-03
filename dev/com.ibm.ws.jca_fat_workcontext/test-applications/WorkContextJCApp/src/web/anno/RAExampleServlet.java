/*******************************************************************************
 * Copyright (c) 2017, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package web.anno;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

//import com.ibm.workcontext.jca.ResourceAdapterImpl;

import jakarta.annotation.Resource;
import jakarta.resource.cci.Connection;
import jakarta.resource.cci.ConnectionFactory;
import jakarta.resource.cci.ConnectionSpec;
import jakarta.resource.cci.IndexedRecord;
import jakarta.resource.cci.Interaction;
import jakarta.resource.cci.InteractionSpec;
import jakarta.resource.cci.MappedRecord;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/*")
public class RAExampleServlet extends HttpServlet {
    private static final long serialVersionUID = 7709282314904580334L;

    // LH 5/31
    //private transient ResourceAdapterImpl adapter;

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
        System.out.println("Entering servlet. Query: " + request.getQueryString());

        try {
            IndexedRecord<String> output = conFactory.getRecordFactory().createIndexedRecord("output");
            MappedRecord<String, String> input = conFactory.getRecordFactory().createMappedRecord("input");
            for (Map.Entry<String, String[]> param : request.getParameterMap().entrySet())
                if (!"functionName".equalsIgnoreCase(param.getKey()))
                    input.put(param.getKey(), param.getValue()[0]);

            InteractionSpec ispec = "ADD"
                            .equalsIgnoreCase(function) ? iSpec_ADD : "FIND".equalsIgnoreCase(function) ? iSpec_FIND : "REMOVE".equalsIgnoreCase(function) ? iSpec_REMOVE : null;

            String message;
            Connection con = conFactory.getConnection(conSpec);
            try {
                Interaction interaction = con.createInteraction();
                message = interaction.execute(ispec, input,
                                              output) ? ("Successfully performed " + function + " with output: " + output) : ("Did not " + function + " any entries.");

                interaction.close();
            } finally {
                con.close();
            }
            out.println(message);
            // LH 5/31
            //adapter.bootstrapContext.getWorkManager().scheduleWork(new WorkContextMsgWork(message));

            System.out.println("Exiting servlet. " + message);
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