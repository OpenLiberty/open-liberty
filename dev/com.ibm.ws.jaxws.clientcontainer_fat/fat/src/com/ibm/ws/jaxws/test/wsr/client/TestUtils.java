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
package com.ibm.ws.jaxws.test.wsr.client;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Iterator;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.ws.BindingProvider;

import com.ibm.websphere.simplicity.RemoteFile;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;

public class TestUtils {
    /*
     * This method is to work around the issue of the hard-coded address and port value which specified in the wsdl file.
     * A servlet could use this util to reset the EndpointAddress with the correct addr and value of a test server.
     * In future, if the we support vendor plan, we could specify the values there.
     */
    public static void setEndpointAddressProperty(BindingProvider bp, String serverAddr, int serverPort) throws IOException {
        String endpointAddr = bp.getRequestContext().get(BindingProvider.ENDPOINT_ADDRESS_PROPERTY).toString();
        URL endpointUrl = new URL(endpointAddr);
        String newEndpointAddr = endpointUrl.getProtocol() + "://" + serverAddr + ":" + serverPort + endpointUrl.getPath();
        bp.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, newEndpointAddr);
    }

    /**
     * Validate the XML attributes
     *
     * @param is
     * @param attributeParams The Map.Entry key likes "targetNamespace=http://com.ibm/jaxws/testmerge/replace/", value likes "{http://schemas.xmlsoap.org/wsdl/}definitions"
     * @return
     * @throws XMLStreamException
     * @throws FactoryConfigurationError
     */
    public static Map<String, QName> validateXMLAttributes(InputStream is, Map<String, QName> attributeParams) throws XMLStreamException, FactoryConfigurationError {
        XMLStreamReader reader = null;
        try {
            reader = XMLInputFactory.newInstance().createXMLStreamReader(is);
            while (reader.hasNext()) {
                int event = reader.next();
                if (XMLStreamConstants.START_ELEMENT == event) {
                    Iterator<Map.Entry<String, QName>> iter = attributeParams.entrySet().iterator();
                    while (iter.hasNext()) {
                        Map.Entry<String, QName> entry = iter.next();
                        String[] keyValuePair = entry.getKey().split("=");
                        QName elementQName = entry.getValue();

                        if (reader.getLocalName().equals(elementQName.getLocalPart())
                            && reader.getNamespaceURI().equals(elementQName.getNamespaceURI())
                            && compareAttribute(reader, keyValuePair[0], keyValuePair[1])) {
                            iter.remove();
                        }
                    }
                    if (attributeParams.isEmpty()) {
                        break;
                    }
                }
            }
        } finally {
            if (reader != null) {
                reader.close();
            }
        }

        return attributeParams;
    }

    private static boolean compareAttribute(XMLStreamReader reader, String attrName, String attrValue) {
        int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            String name = reader.getAttributeLocalName(i);
            String value = reader.getAttributeValue(i);
            if (name.equals(attrName) && value.equals(attrValue)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Copy the file to server, rename if the targetFileName is not equal to srcFileName.
     *
     * @param server,                   the LibertyServer
     * @param srcPathFromPubishFolder,  the folder path relative to publish folder
     * @param srcFileName,              the source file name to copy
     * @param targetPathFromServerRoot, the target path relative to server root
     * @param targetFileName,           the target file name
     * @throws Exception
     */
    public static void publishFileToServer(LibertyServer server, String srcPathFromPubishFolder, String srcFileName, String targetPathFromServerRoot,
                                           String targetFileName) throws Exception {
        server.copyFileToLibertyServerRoot(targetPathFromServerRoot, srcPathFromPubishFolder + "/" + srcFileName);
        if (targetFileName != null && !targetFileName.isEmpty() && !targetFileName.equals(srcFileName)) {
            server.renameLibertyServerRootFile(targetPathFromServerRoot + "/" + srcFileName, targetPathFromServerRoot + "/" + targetFileName);
        }
    }

    /**
     * Replace the string in a server file.
     *
     * @param server,                 the LibertyServer
     * @param filePathFromServerRoot, the file path relative to server root
     * @param fromStr,                the string that need replace in the file.
     * @param toStr,                  the string to replace the original one.
     * @throws Exception
     */
    public static void replaceServerFileString(LibertyServer server, String filePathFromServerRoot, String fromStr, String toStr) throws Exception {
        InputStream is = null;
        OutputStream os = null;
        try {
            RemoteFile serverFile = server.getFileFromLibertyServerRoot(filePathFromServerRoot);
            is = serverFile.openForReading();

            StringBuilder builder = new StringBuilder();

            //read
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader bfin = new BufferedReader(isr);
            String rLine = "";
            while ((rLine = bfin.readLine()) != null) {
                builder.append(rLine);
            }
            is.close();

            //replace
            String xmiText = builder.toString();
            String updatedText = xmiText.replaceAll(fromStr, toStr);

            //write
            os = serverFile.openForWriting(false);
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os));
            writer.write(updatedText);
            writer.close();
        } catch (Exception e) {
            throw e;
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    //ignore;
                }
            }
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e) {
                    //ignore;
                }
            }
        }
    }

    public static String getServletResponse(String servletUrl) throws Exception {
        URL url = new URL(servletUrl);
        HttpURLConnection con = HttpUtils.getHttpConnection(url, HttpURLConnection.HTTP_OK, 10);
        BufferedReader br = HttpUtils.getConnectionStream(con);
        String result = br.readLine();
        String line;
        while ((line = br.readLine()) != null) {
            result += line;
        }
        return result;
    }
}
