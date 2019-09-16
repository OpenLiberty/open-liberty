/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.graphql.component;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import graphql.schema.GraphQLSchema;
import graphql.schema.idl.SchemaPrinter;

@SuppressWarnings("serial")
public class GraphQLSchemaServlet extends HttpServlet {

    private final GraphQLSchema schema;

    GraphQLSchemaServlet(GraphQLSchema schema) {
        this.schema = schema;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)throws ServletException, IOException {
        SchemaPrinter schemaPrinter = new SchemaPrinter(SchemaPrinter.Options.defaultOptions());
        String json = schemaPrinter.print(schema);
        response.setContentType("plain/text");
        PrintWriter out = response.getWriter();
        out.print(json);
        out.flush();
    }
    
    @Override
    public String getServletInfo() {
        return "The graphQL schema";
    }
}
