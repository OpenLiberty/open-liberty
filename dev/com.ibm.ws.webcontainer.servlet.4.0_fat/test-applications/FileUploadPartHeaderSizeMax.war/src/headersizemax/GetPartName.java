/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package headersizemax;

import java.io.IOException;
import java.util.Collection;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

/**
 * Request with "multipart/form-data" to test the commons-fileupload's partHeaderSizeMax APIs.
 * Each field is considered a file/part with its own headers
 */
@WebServlet("/GetPartName")
@MultipartConfig
public class GetPartName extends HttpServlet {
    private static String CLASS_NAME = "GetPartName";
    private static final long serialVersionUID = 1L;

    public GetPartName() {
        super();
        // TODO Auto-generated constructor stub
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        this.doWork(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        this.doWork(request, response);
    }

    private void doWork(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        ServletOutputStream respToClient = response.getOutputStream();

        try {
            //request.getParameter triggers parseMultiPart process for multipart/form-data request.
            System.out.println(CLASS_NAME + " request.getParameter(\"Name_2000\") = [" + request.getParameter("Name_2000") + "]");

            Collection<Part> myParts = request.getParts();
            Part[] myArrayParts = myParts.toArray(new Part[0]);
            int size = myArrayParts.length;

            String firstPartName = myArrayParts[0].getName();
            String lastPartName = myArrayParts[size - 1].getName();

            //These may not show up if there is parsing exception.
            System.out.println(CLASS_NAME + " request.getParts() - total parts [" + size + "]");
            System.out.println(CLASS_NAME + " First part name [" + firstPartName + "] , this part size [" + myArrayParts[0].getSize() + "]");
            System.out.println(CLASS_NAME + " Last part name [" + lastPartName + "], this part size [" + myArrayParts[size - 1].getSize() + "]");

            respToClient.println("Test Complete. Received parts size [" + size + "] , first part name [" + firstPartName + "] , last part name [" + lastPartName + "]");
        } catch (Exception e) {
            System.out.println(CLASS_NAME + " Exception [" + e + "]");
            respToClient.println("Exception [" + e + "]");      //Client asserts on this message to test against the property.
        }
    }
}
