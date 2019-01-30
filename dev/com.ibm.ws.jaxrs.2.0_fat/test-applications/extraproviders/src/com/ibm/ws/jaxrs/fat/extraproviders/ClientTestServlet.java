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
package com.ibm.ws.jaxrs.fat.extraproviders;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBElement;

import com.ibm.ws.jaxrs.fat.extraprovider.jaxb.bean.Address;
import com.ibm.ws.jaxrs.fat.extraprovider.jaxb.bean.Employee;
import com.ibm.ws.jaxrs.fat.extraprovider.jaxb.bean.ObjectFactory;
import com.ibm.ws.jaxrs.fat.extraproviders.jaxb.book.Author;
import com.ibm.ws.jaxrs.fat.extraproviders.jaxb.book.Book;
import com.ibm.ws.jaxrs.fat.extraproviders.jaxb.person.Person;

@WebServlet("/TestServlet")
public class ClientTestServlet extends HttpServlet {

    private static final long serialVersionUID = -6241534589062372914L;
    private static final String CONTEXT_ROOT = "extraproviders";
    private static String serverIP;
    private static String serverPort;

    private static Client client;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        PrintWriter pw = resp.getWriter();

        String testMethod = req.getParameter("test");
        if (testMethod == null) {
            pw.write("no test to run");
            return;
        }

        runTest(testMethod, pw, req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doGet(req, resp);
    }

    private void runTest(String testMethod, PrintWriter pw, HttpServletRequest req, HttpServletResponse resp) {
        try {
            ClientBuilder cb = ClientBuilder.newBuilder();
            cb.property("com.ibm.ws.jaxrs.client.connection.timeout", "120000");
            cb.property("com.ibm.ws.jaxrs.client.receive.timeout", "120000");
            client = cb.build();

            Method testM = this.getClass().getDeclaredMethod(testMethod, new Class[] { Map.class, StringBuilder.class });
            Map<String, String> m = new HashMap<String, String>();

            Iterator<String> itr = req.getParameterMap().keySet().iterator();

            while (itr.hasNext()) {
                String key = itr.next();
                if (key.indexOf("@") == 0) {
                    m.put(key.substring(1), req.getParameter(key));
                }
            }

            serverIP = req.getLocalAddr();
            serverPort = String.valueOf(req.getLocalPort());
            m.put("serverIP", serverIP);
            m.put("serverPort", serverPort);

            StringBuilder ret = new StringBuilder();
            testM.invoke(this, m, ret);
            pw.write(ret.toString());

        } catch (Exception e) {
            e.printStackTrace(); // print to the logs too since the test client only reads the first line of the pw output
            if (e instanceof InvocationTargetException) {
                e.getCause().printStackTrace(pw);
            } else {
                e.printStackTrace(pw);
            }
        } finally {
            client.close();
        }
    }

    private static String getAddress(String path) {
        return "http://" + serverIP + ":" + serverPort + "/" + CONTEXT_ROOT + "/optionalproviders/jaxbresource/" + path;
    }

    /*
     * All the rest from JAXBCollectionTest
     */
    public void testXMLRootWithObjectFactoryList(Map<String, String> param, StringBuilder ret) throws Exception {
        List<Book> source = getBookSource();
        Response resp = client.target(getAddress("booklist")).request(MediaType.APPLICATION_XML).accept(MediaType.APPLICATION_XML).post(Entity.xml(source));
        List<Book> responseEntity = resp.readEntity(new GenericType<List<Book>>() {});

        verifyResponse(responseEntity, Book.class);
        ret.append("OK");
    }

    public void testXMLRootWithObjectFactoryListResponse(Map<String, String> param, StringBuilder ret) throws Exception {
        List<Book> source = getBookSource();
        Response resp = client.target(getAddress("booklistresponse")).request(MediaType.APPLICATION_XML).accept(MediaType.APPLICATION_XML).post(Entity.xml(source));
        List<Book> responseEntity = resp.readEntity(new GenericType<List<Book>>() {});

        verifyResponse(responseEntity, Book.class);
        ret.append("OK");
    }

    public void testXMLRootWithObjectFactoryJAXBElement(Map<String, String> param, StringBuilder ret) throws Exception {
        JAXBElement<Employee> source = getEmployeeSource();
        Response resp = client.target(getAddress("employeejaxbelement")).request(MediaType.APPLICATION_XML).accept(MediaType.APPLICATION_XML).post(Entity.xml(source));
        JAXBElement<Employee> responseEntity = resp.readEntity(new GenericType<JAXBElement<Employee>>() {});

        verifyResponse(responseEntity, JAXBElement.class);
        ret.append("OK");
    }

    public void testXMLRootNoObjectFactoryList(Map<String, String> param, StringBuilder ret) throws Exception {
        List<Person> source = getPersonSource();
        Response resp = client.target(getAddress("personlist")).request(MediaType.APPLICATION_XML).accept(MediaType.APPLICATION_XML).post(Entity.xml(source));
        List<Person> responseEntity = resp.readEntity(new GenericType<List<Person>>() {});

        verifyResponse(responseEntity, Person.class);
        ret.append("OK");
    }

    public void testXMLRootNoObjectFactoryArray(Map<String, String> param, StringBuilder ret) throws Exception {
        Person[] source = getPersonSource().toArray(new Person[] {});
        Response resp = client.target(getAddress("personarray")).request(MediaType.APPLICATION_XML).accept(MediaType.APPLICATION_XML).post(Entity.xml(source));
        Person[] responseEntity = resp.readEntity(new GenericType<Person[]>() {});

        verifyResponse(responseEntity, Person.class);
        ret.append("OK");
    }

    public void testXMLRootNoObjectFactoryListResponse(Map<String, String> param, StringBuilder ret) throws Exception {
        List<Person> source = getPersonSource();
        Response resp = client.target(getAddress("personlistresponse")).request(MediaType.APPLICATION_XML).accept(MediaType.APPLICATION_XML).post(Entity.xml(source));
        List<Person> responseEntity = resp.readEntity(new GenericType<List<Person>>() {});

        verifyResponse(responseEntity, Person.class);
        ret.append("OK");
    }

    public void testXMLRootNoObjectFactoryJAXBElement(Map<String, String> param, StringBuilder ret) throws Exception {
        Person source = null;
        for (Iterator<Person> it = getPersonSource().iterator(); it.hasNext();) {
            source = it.next();
        }
        Response resp = client.target(getAddress("personjaxbelement")).request(MediaType.APPLICATION_XML).accept(MediaType.APPLICATION_XML).post(Entity.xml(source));
        Person responseEntity = resp.readEntity(Person.class);

        verifyResponse(responseEntity, Person.class);
        ret.append("OK");
    }

    private List<Book> getBookSource() {
        List<Book> source = new ArrayList<Book>();
        Book book = new Book();
        Author author = new Author();
        author.setFirstName("Eddie");
        author.setLastName("Vedder");
        book.setAuthor(author);
        book.setTitle("Vitalogy");
        source.add(book);
        book = new Book();
        author = new Author();
        author.setFirstName("Stone");
        author.setLastName("Gossard");
        book.setAuthor(author);
        book.setTitle("Ten");
        source.add(book);
        return source;
    }

    @SuppressWarnings("unchecked")
    private <T> void verifyResponse(Object response, Class<T> type) {
        List<?> expected = null;
        JAXBElement<Employee> expectedEmployee = null;
        JAXBElement<Employee> actualEmployee = null;

        List<Object> actual = null;
        if (type == Book.class) {
            expected = getBookSource();
            actual = new ArrayList<Object>();
        } else if (type == JAXBElement.class) {
            expectedEmployee = getExpectedEmployeeSource(getEmployeeSource());
        }

        else {
            expected = getPersonSource();
            actual = new ArrayList<Object>();
        }
        if (response.getClass().isArray()) {
            for (int i = 0; i < ((T[]) response).length; ++i)
                actual.add(((T[]) response)[i]);
        } else if (JAXBElement.class.isAssignableFrom(response.getClass())) {
            actualEmployee = (JAXBElement<Employee>) response;
        } else if (Person.class.isAssignableFrom(response.getClass())) {
            actual.add(response);
        } else
            actual = (List<Object>) response;

        if (expected != null) {
            for (Object o : expected) {
                if (type == Book.class) {
                    Book b = (Book) o;
                    Author author = b.getAuthor();
                    author.setFirstName("echo " + author.getFirstName());
                    author.setLastName("echo " + author.getLastName());
                    b.setTitle("echo " + b.getTitle());
                } else {
                    Person person = (Person) o;
                    person.setName("echo " + person.getName());
                    person.setDesc("echo " + person.getDesc());
                }
            }
            assertEquals(expected, actual);
        }
        if (expectedEmployee != null) {
            assertEquals(expectedEmployee.getValue().getAddress().getCity(), actualEmployee.getValue().getAddress().getCity());
        }

    }

    private List<Person> getPersonSource() {
        List<Person> people = new ArrayList<Person>();
        Person person = new Person();
        person.setName("Eddie Vedder");
        person.setDesc("Author of Vitalogy");
        people.add(person);
        return people;
    }

    private JAXBElement<Employee> getEmployeeSource() {

        Employee employee = new Employee();
        Address address = new Address();
        address.setCity("NYC");
        address.setLine1("tom");
        address.setLine2("Lisa");
        address.setState("NYC");
        address.setZipcode(12345);
        employee.setAddress(address);
        employee.setDesignation("des");
        employee.setId(1);
        employee.setName("Jack");
        employee.setSalary(10000);
        ObjectFactory obj = new ObjectFactory();
        JAXBElement<Employee> emp = obj.createEmployee(employee);
        return emp;

    }

    private JAXBElement<Employee> getExpectedEmployeeSource(JAXBElement<Employee> employeeElement) {
        Address address = null;
        Address retAddress = null;
        Employee retEmployee = null;

        address = employeeElement.getValue().getAddress();
        retAddress = new Address();
        retAddress.setCity("echo " + address.getCity());
        retAddress.setLine1("echo " + address.getLine1());
        retAddress.setLine2("echo" + address.getLine2());
        retAddress.setState("echo " + address.getState());
        retAddress.setZipcode(address.getZipcode());
        retEmployee = new Employee();
        retEmployee.setAddress(retAddress);
        retEmployee.setDesignation("echo " + employeeElement.getValue().getDesignation());
        retEmployee.setId(employeeElement.getValue().getId());
        retEmployee.setName("echo " + employeeElement.getValue().getName());
        retEmployee.setSalary(employeeElement.getValue().getSalary());
        ObjectFactory objFac = new ObjectFactory();
        JAXBElement<Employee> element = objFac.createEmployee(retEmployee);
        return element;
    }
}
