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
package com.ibm.ws.jaxrs20.fat.contextresolver;

import static com.ibm.ws.jaxrs20.fat.TestUtils.getBaseTestUri;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.StringWriter;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import javax.xml.transform.stream.StreamSource;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.jaxrs.fat.contextresolver.Department;
import com.ibm.ws.jaxrs.fat.contextresolver.DepartmentDatabase;
import com.ibm.ws.jaxrs.fat.contextresolver.DepartmentListWrapper;
import com.ibm.ws.jaxrs.fat.contextresolver.User;

import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

@RunWith(FATRunner.class)
@SkipForRepeat("EE9_FEATURES") // currently broken due to multiple issues
public class DepartmentTest {

    @Server("com.ibm.ws.jaxrs.fat.contextresolver")
    public static LibertyServer server;

    private static final String contextResolverwar = "contextresolver";

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultDropinApp(server, contextResolverwar, "com.ibm.ws.jaxrs.fat.contextresolver",
                                      "com.ibm.ws.jaxrs.fat.contextresolver.jaxb");

        // Make sure we don't fail because we try to start an
        // already started server
        try {
            server.startServer(true);
        } catch (Exception e) {
            System.out.println(e.toString());
        }
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null) {
            server.stopServer();
        }
    }

    private final String departmentMappedUri = getBaseTestUri(contextResolverwar, "departments");

    /**
     * This will drive several different requests that interact with the
     * Departments resource class.
     */
    @Test
    public void testDepartmentsResourceJAXB() throws Exception {
        HttpPost postMethod = null;
        HttpGet getAllMethod = null;
        HttpGet getOneMethod = null;
        HttpHead httpHead = null;
        HttpDelete HttpDelete = null;
        try {

            // make sure everything is clear before testing
            DepartmentDatabase.clearEntries();

            // create a new Department
            Department newDepartment = new Department();
            newDepartment.setDepartmentId("1");
            newDepartment.setDepartmentName("Marketing");
            JAXBContext context = JAXBContext.newInstance(new Class<?>[] { Department.class, DepartmentListWrapper.class });
            Marshaller marshaller = context.createMarshaller();
            StringWriter sw = new StringWriter();
            marshaller.marshal(newDepartment, sw);
            HttpClient client = new DefaultHttpClient();
            postMethod = new HttpPost(departmentMappedUri);
            ByteArrayEntity reqEntity = new ByteArrayEntity(sw.toString().getBytes());
            reqEntity.setContentType("text/xml");
            postMethod.setEntity(reqEntity);
            client.execute(postMethod);

            newDepartment = new Department();
            newDepartment.setDepartmentId("2");
            newDepartment.setDepartmentName("Sales");
            sw = new StringWriter();
            marshaller.marshal(newDepartment, sw);
            client = new DefaultHttpClient();
            postMethod = new HttpPost(departmentMappedUri);
            reqEntity = new ByteArrayEntity(sw.toString().getBytes());
            reqEntity.setContentType("text/xml");
            postMethod.setEntity(reqEntity);
            client.execute(postMethod);

            // now let's get the list of Departments that we just created (should be 2)
            client = new DefaultHttpClient();
            getAllMethod = new HttpGet(departmentMappedUri);
            HttpResponse response = client.execute(getAllMethod);
            assertNotNull(response);
            Unmarshaller unmarshaller = context.createUnmarshaller();
            Object obj = unmarshaller.unmarshal(response.getEntity().getContent());
            assertTrue(obj instanceof DepartmentListWrapper);
            DepartmentListWrapper wrapper = (DepartmentListWrapper) obj;
            List<Department> dptList = wrapper.getDepartmentList();
            assertNotNull(dptList);
            assertEquals(2, dptList.size());

            // now get a specific Department that was created
            client = new DefaultHttpClient();
            getOneMethod = new HttpGet(departmentMappedUri + "/1");
            response = client.execute(getOneMethod);
            assertNotNull(response);
            obj = unmarshaller.unmarshal(response.getEntity().getContent());
            assertTrue(obj instanceof Department);
            Department dept = (Department) obj;
            assertEquals("1", dept.getDepartmentId());
            assertEquals("Marketing", dept.getDepartmentName());

            // let's send a Head request for both an existent and non-existent resource
            // we are testing to see if header values being set in the resource
            // implementation are sent back appropriately
            client = new DefaultHttpClient();
            httpHead = new HttpHead(departmentMappedUri + "/3");
            response = client.execute(httpHead);
            assertNotNull(response.getAllHeaders());
            Header header = response.getFirstHeader("unresolved-id");
            assertNotNull(header);
            assertEquals("3", header.getValue());

            // now the resource that should exist
            httpHead = new HttpHead(departmentMappedUri + "/1");
            response = client.execute(httpHead);
            assertNotNull(response.getAllHeaders());
            header = response.getFirstHeader("resolved-id");
            assertNotNull(header);
            assertEquals("1", header.getValue());
            HttpDelete = new HttpDelete(departmentMappedUri + "/1");
            response = client.execute(HttpDelete);
            assertEquals(204, response.getStatusLine().getStatusCode());
            HttpDelete = new HttpDelete(departmentMappedUri + "/2");
            response = client.execute(HttpDelete);
            assertEquals(204, response.getStatusLine().getStatusCode());
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    private final String contextMappedUri = getBaseTestUri(contextResolverwar, "user");

    @Test
    public void testUserContextProvider() throws Exception {
        HttpClient httpClient = new DefaultHttpClient();

        User user = new User();
        user.setUserName("joedoe@example.com");
        JAXBElement<User> element = new JAXBElement<User>(new QName("http://jaxb.context.tests", "user"), User.class, user);
        JAXBContext context = JAXBContext.newInstance(com.ibm.ws.jaxrs.fat.contextresolver.jaxb.ObjectFactory.class);
        StringWriter sw = new StringWriter();
        Marshaller m = context.createMarshaller();
        m.marshal(element, sw);
        HttpPost postMethod = new HttpPost(contextMappedUri);
        try {
            StringEntity reqEntity = new StringEntity(sw.toString());
            reqEntity.setContentType("text/xml");
            postMethod.setEntity(reqEntity);
            HttpResponse response = httpClient.execute(postMethod);
            assertEquals(204, response.getStatusLine().getStatusCode());
        } finally {
            httpClient = new DefaultHttpClient();
        }

        HttpGet getMethod = new HttpGet(contextMappedUri + "/joedoe@example.com");
        try {
            HttpResponse response = httpClient.execute(getMethod);
            assertEquals(200, response.getStatusLine().getStatusCode());
            Unmarshaller u = context.createUnmarshaller();
            element = u.unmarshal(new StreamSource(response.getEntity().getContent()), User.class);
            assertNotNull(element);
            user = element.getValue();
            assertNotNull(user);
            assertEquals("joedoe@example.com", user.getUserName());
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }
}