/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.jaxws.attachment.servlet;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBContext;
import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Dispatch;
import javax.xml.ws.Service;
import javax.xml.ws.soap.SOAPBinding;

import com.ibm.jaxws.attachment.mtom.ImageDepot;
import com.ibm.jaxws.attachment.mtom.ObjectFactory;
import com.ibm.jaxws.attachment.mtom.SendImage;
import com.ibm.jaxws.attachment.mtom.SendImageResponse;

/**
 *
 */
@WebServlet("/AttachmentPropertiesTestServlet")
public class AttachmentPropertiesTestServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    public AttachmentPropertiesTestServlet() {
        super();
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doGet(req, resp);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        final String NAMESPACE = "http://com/ibm/jaxws/attachment/mtom/";
        final QName serviceName11 = new QName(NAMESPACE, "MtomSampleService");
        final QName portNameDispatch = new QName(NAMESPACE, "MtomSamplePortDispatch");

        try (PrintWriter writer = resp.getWriter()) {
            List<DataHandler> binaryContent = getBinaryContent("catalogfiles");
            // Set the data inside of the appropriate object
            ImageDepot imageDepot = new ObjectFactory().createImageDepot();
            imageDepot.setImageData(binaryContent);

            Service service = Service.create(serviceName11);
            String uriString = "http://" + req.getLocalAddr() + ":" + req.getLocalPort() + "/AttachmentProperties/MtomSampleService";

            service.addPort(portNameDispatch, SOAPBinding.SOAP11HTTP_BINDING, uriString);

            // Setup the necessary JAX-WS artifacts
            JAXBContext jbc = JAXBContext.newInstance("com.ibm.jaxws.attachment.mtom");
            Dispatch<Object> dispatch = service.createDispatch(portNameDispatch, jbc, Service.Mode.PAYLOAD);
            BindingProvider bp = dispatch;
            bp.getRequestContext().put(BindingProvider.SOAPACTION_USE_PROPERTY, Boolean.TRUE);
            bp.getRequestContext().put(BindingProvider.SOAPACTION_URI_PROPERTY, "sendImage");

            // Set the actual flag to enable MTOM
            SOAPBinding binding = (SOAPBinding) dispatch.getBinding();
            binding.setMTOMEnabled(true);

            // Create the request wrapper bean
            ObjectFactory factory = new ObjectFactory();
            SendImage request = factory.createSendImage();
            request.setData(imageDepot);

            // Send the image and process the response image

            SendImageResponse response = (SendImageResponse) dispatch.invoke(request);
            if (response != null) {
                ImageDepot responseContent = response.getData();
                if (isAnyDataSourceSizeZero(responseContent)) {
                    writer.write("FAILED: DataSource size is 0 ");//FAILED: DataSource size is 0
                    writer.flush();
                    return;
                }
            } else {
                writer.write("FAILED: Response is NULL");
                writer.flush();
                return;
            }

            writer.write("PASSED");
            writer.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * @param responseContent
     * @throws IOException
     * @return boolean Return true if any DataSource is 0
     */
    private boolean isAnyDataSourceSizeZero(ImageDepot responseContent) throws IOException {
        for (DataHandler dh : responseContent.getImageData()) {
            long size = 0;
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            dh.writeTo(baos);
            size = baos.size();
            baos = null;
            System.out.println("isAnyDataSourceSizeZero > dh name: " + dh.getDataSource().getName() + " , size: " + size);

            if (size == 0) {
                return true;
            }
        }
        return false;
    }

    /*
     * Get a DataHandler that contains the binary data we'd like to send.
     *
     * @return DataHandler
     *
     * @throws Exception
     */
    private List<DataHandler> getBinaryContent(String imageFolderName) throws Exception {
        List<DataHandler> listDh = new ArrayList<DataHandler>();
        if (imageFolderName != null) {
            File folder = new File(imageFolderName);
            if (!folder.exists()) {
                throw new FileNotFoundException("Files does not exist at " + imageFolderName);
            }
            System.out.println("Loading data from: '" + folder.toURI().toURL().toExternalForm() + "'");

            File[] files = folder.listFiles();
            for (File file : files) {
                FileDataSource fds = new FileDataSource(file);
                listDh.add(new DataHandler(fds));
            }
        }
        return listDh;
    }
}
