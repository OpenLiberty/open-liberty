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
package com.ibm.ws.jaxrs.fat.extraproviders.jaxb;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import com.ibm.ws.jaxrs.fat.extraprovider.jaxb.bean.Address;
import com.ibm.ws.jaxrs.fat.extraprovider.jaxb.bean.Employee;
import com.ibm.ws.jaxrs.fat.extraprovider.jaxb.bean.ObjectFactory;
import com.ibm.ws.jaxrs.fat.extraproviders.jaxb.book.Author;
import com.ibm.ws.jaxrs.fat.extraproviders.jaxb.book.Book;
import com.ibm.ws.jaxrs.fat.extraproviders.jaxb.person.Person;

@Path("jaxbresource")
public class JAXBResource {

    @Path("booklist")
    @Produces(MediaType.APPLICATION_XML)
    @Consumes(MediaType.APPLICATION_XML)
    @POST
    public List<Book> echoBookList(List<Book> books) {
        List<Book> ret = new ArrayList<Book>();
        Author author = null;
        Author retAuthor = null;
        Book retBook = null;
        for (Book book : books) {
            author = book.getAuthor();
            retAuthor = new Author();
            retAuthor.setFirstName("echo " + author.getFirstName());
            retAuthor.setLastName("echo " + author.getLastName());
            retBook = new Book();
            retBook.setAuthor(retAuthor);
            retBook.setTitle("echo " + book.getTitle());
            ret.add(retBook);
        }
        return ret;
    }

    @Path("bookarray")
    @Produces(MediaType.APPLICATION_XML)
    @Consumes(MediaType.APPLICATION_XML)
    @POST
    public Book[] echoBookArray(Book[] books) {
        Book[] ret = new Book[books.length];
        Author author = null;
        Author retAuthor = null;
        Book retBook = null;
        int i = 0;
        for (Book book : books) {
            author = book.getAuthor();
            retAuthor = new Author();
            retAuthor.setFirstName("echo " + author.getFirstName());
            retAuthor.setLastName("echo " + author.getLastName());
            retBook = new Book();
            retBook.setAuthor(retAuthor);
            retBook.setTitle("echo " + book.getTitle());
            ret[i++] = retBook;
        }
        return ret;
    }

    @Path("booklistresponse")
    @Produces(MediaType.APPLICATION_XML)
    @Consumes(MediaType.APPLICATION_XML)
    @POST
    public Response echoBookListResponse(List<Book> books) {
        List<Book> ret = echoBookList(books);
        Response response = Response.ok(new GenericEntity<List<Book>>(ret) {}, MediaType.APPLICATION_XML).build();
        return response;
    }

    @Path("employeejaxbelement")
    @Produces(MediaType.APPLICATION_XML)
    @Consumes(MediaType.APPLICATION_XML)
    @POST
    public JAXBElement<Employee> echoJAXBElementAddress(JAXBElement<Employee> employeeElement) {
        ObjectFactory objFac = new ObjectFactory();
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
        JAXBElement<Employee> element = objFac.createEmployee(retEmployee);

        return element;
    }

    @Path("personlist")
    @Produces(MediaType.APPLICATION_XML)
    @Consumes(MediaType.APPLICATION_XML)
    @POST
    public List<Person> echoPersonList(List<Person> people) {
        List<Person> ret = new ArrayList<Person>();
        Person retPerson = null;
        for (Person person : people) {
            retPerson = new Person();
            retPerson.setName("echo " + person.getName());
            retPerson.setDesc("echo " + person.getDesc());
            ret.add(retPerson);
        }
        return ret;
    }

    @Path("personlistresponse")
    @Produces(MediaType.APPLICATION_XML)
    @Consumes(MediaType.APPLICATION_XML)
    @POST
    public Response echoPersonListResponse(List<Person> people) {
        List<Person> ret = echoPersonList(people);
        Response response = Response.ok(new GenericEntity<List<Person>>(ret) {}, MediaType.APPLICATION_XML).build();
        return response;
    }

    @Path("personarray")
    @Produces(MediaType.APPLICATION_XML)
    @Consumes(MediaType.APPLICATION_XML)
    @POST
    public Person[] echoPersonArray(Person[] people) {
        Person[] ret = new Person[people.length];
        Person retPerson = null;
        int i = 0;
        for (Person person : people) {
            retPerson = new Person();
            retPerson.setName("echo " + person.getName());
            retPerson.setDesc("echo " + person.getDesc());
            ret[i++] = retPerson;
        }
        return ret;
    }

    @Path("personjaxbelement")
    @Produces(MediaType.APPLICATION_XML)
    @Consumes(MediaType.APPLICATION_XML)
    @POST
    public JAXBElement<Person> echoPersonJAXBElementList(JAXBElement<Person> personElement) {

        Person retPerson = new Person();
        retPerson.setName("echo " + personElement.getValue().getName());
        retPerson.setDesc("echo " + personElement.getValue().getDesc());
        JAXBElement<Person> element = new JAXBElement<Person>(new QName("person"), Person.class, retPerson);

        return element;
    }
}
