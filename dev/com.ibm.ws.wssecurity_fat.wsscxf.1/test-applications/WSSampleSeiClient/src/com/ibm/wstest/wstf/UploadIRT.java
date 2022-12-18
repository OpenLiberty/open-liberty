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

package com.ibm.wstest.wstf;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Servlet implementation class UploadIRT
 */
public class UploadIRT extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final String CLIENT_SERVLET_LOCATION = "/ClientWeb";
    private static final String CLIENT_JSP_LOCATION = "/clientirt.jsp";

    /**
     * @see HttpServlet#HttpServlet()
     */
    public UploadIRT() {
        super();
    }

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
     *      response)
     */
    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response) throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
     *      response)
     */
    @Override
    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response) throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * processRequest Reads the posted parameters and calls the service
     * 
     * @param req
     *                 - HttpServletRequest
     * @param resp
     *                 - HttpServletResponse
     * @throws ServletException
     * @throws IOException
     */
    private void processRequest(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        ServletContext context = getServletContext();

        // See if the form was posted
        String contentType = req.getContentType();
        System.out.println(">>SERVLET: Content type is: " + contentType);

        // Set up the default values to use
        InetAddress addr = InetAddress.getLocalHost();
        String contextString = getServletContext().getContextPath();

        String uriString = "http://" + addr.getCanonicalHostName() + ":" + req.getServerPort();
        req.setAttribute("serviceURL", uriString);
        req.setAttribute("msgcount", "1");
        req.setAttribute("scenario", contextString.substring(1));

        // verify we have multipart/form-data
        if ((contentType != null)
            && (contentType.indexOf("multipart/form-data") >= 0)) {

            // Read the post data
            DataInputStream in = new DataInputStream(req.getInputStream());
            int formDataLength = req.getContentLength();

            byte dataBytes[] = new byte[formDataLength];
            int byteRead = 0;
            int totalBytesRead = 0;
            while (totalBytesRead < formDataLength) {
                byteRead = in.read(dataBytes, totalBytesRead, formDataLength);
                totalBytesRead += byteRead;
            }

            // Change it to a string
            String data = new String(dataBytes);

            // Filename is part of the file data block.
            String fileName = data.substring(data.indexOf("filename=\"") + 10);
            fileName = fileName.substring(0, fileName.indexOf("\n"));

            // Strip path info out, windows or unix
            fileName = fileName.substring(fileName.lastIndexOf("\\") + 1,
                                          fileName.indexOf("\""));
            fileName = fileName.substring(fileName.lastIndexOf("/") + 1);

            // Now look for the file data
            int lastIndex = contentType.lastIndexOf("=");
            String boundary = contentType.substring(lastIndex + 1, contentType.length());

            int pos;
            pos = data.indexOf("filename=\"");
            pos = data.indexOf("\n", pos) + 1;
            pos = data.indexOf("\n", pos) + 1;
            pos = data.indexOf("\n", pos) + 1;

            // Determine the boundaries
            int boundaryLocation = data.indexOf(boundary, pos) - 4;
            int startPos = ((data.substring(0, pos)).getBytes()).length;
            int endPos = ((data.substring(0, boundaryLocation)).getBytes()).length;

            // Write the file locally
            FileOutputStream fileOut = new FileOutputStream(fileName);
            fileOut.write(dataBytes, startPos, (endPos - startPos));
            fileOut.flush();
            fileOut.close();

            System.out.println(">>SERVLET: File saved as " + fileName);
            ArrayList<IrtEndpoint> endpoints = readIRT(fileName);
            for (int index = 0; index < endpoints.size(); index++) {
                IrtEndpoint endpoint = endpoints.get(index);
                req.setAttribute(endpoint.getFeatureName() + endpoint.getScenarioName(), endpoint.getAddress());
                req.setAttribute(endpoint.getFeatureName() + endpoint.getScenarioName() + "wsdl", endpoint.getWsdlUri());
            }
            req.setAttribute("irtcount", endpoints.size());
            req.setAttribute("irtfile", fileName);
            System.out.println(">>SERVLET: attributes set ...");
            context.getRequestDispatcher(CLIENT_JSP_LOCATION).forward(req, resp);
        }

        else {
            // No file posted, Redirect back to main servlet
            context.getRequestDispatcher(CLIENT_SERVLET_LOCATION).forward(req,
                                                                          resp);
        }

    }

    private ArrayList<IrtEndpoint> readIRT(String filename) {
        ArrayList<IrtEndpoint> endpoints = new ArrayList<IrtEndpoint>();
        String feature = "";
        String scenario = "";
        String actor = "";
        String address = "";
        String wsdl = "";
        try {
            File file = new File(filename);
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(file);
            doc.getDocumentElement().normalize();
            System.out.println("Root element "
                               + doc.getDocumentElement().getNodeName());
            NodeList nodeLst = doc.getElementsByTagName("Service");

            for (int index = 0; index < nodeLst.getLength(); index++) {

                Node docNode = nodeLst.item(index);

                if (docNode.getNodeType() == Node.ELEMENT_NODE) {

                    Element element = (Element) docNode;
                    NodeList nodelist = element.getElementsByTagName("FeatureName");
                    Element thisElement = (Element) nodelist.item(0);
                    NodeList thisList = thisElement.getChildNodes();
                    feature = thisList.item(0).getNodeValue();
                    //System.out.println("FeatureName : "  + ((Node) thisList.item(0)).getNodeValue());

                    nodelist = element.getElementsByTagName("ScenarioName");
                    thisElement = (Element) nodelist.item(0);
                    thisList = thisElement.getChildNodes();
                    scenario = thisList.item(0).getNodeValue();
                    //System.out.println("ScenarioName : "  + ((Node) thisList.item(0)).getNodeValue());

                    nodelist = element.getElementsByTagName("ActorName");
                    thisElement = (Element) nodelist.item(0);
                    thisList = thisElement.getChildNodes();
                    actor = thisList.item(0).getNodeValue();
                    //System.out.println("ActorName : "  + ((Node) thisList.item(0)).getNodeValue());

                    nodelist = element.getElementsByTagName("Implementation");
                    Element element1 = (Element) nodelist.item(0);
                    NodeList nodelist1 = element1.getElementsByTagName("Endpoint");
                    Element element2 = (Element) nodelist1.item(0);

                    NodeList nodelist2 = element2.getElementsByTagName("Address");
                    Element thisElement2 = (Element) nodelist2.item(0);
                    NodeList thisList2 = thisElement2.getChildNodes();
                    address = thisList2.item(0).getNodeValue();
                    //System.out.println("Address : "  + ((Node) thisList2.item(0)).getNodeValue());			          
                    nodelist2 = element2.getElementsByTagName("WsdlUri");
                    thisElement2 = (Element) nodelist2.item(0);
                    thisList2 = thisElement2.getChildNodes();
                    wsdl = thisList2.item(0).getNodeValue();
                    //System.out.println("WsdlUri : "  + ((Node) thisList2.item(0)).getNodeValue());

                    endpoints.add(new IrtEndpoint(feature, scenario, actor, address, wsdl));
                } else {
                    System.out.println("1 - Not an element.");
                }

            }
            System.out.println("Returning " + endpoints.size() + " endpoints.");
            return endpoints;
        } catch (Exception e) {
            System.out.println("EXCEPTION: " + e.getMessage() + "\n" + e.getStackTrace());
            return null;
        }
    }

}
