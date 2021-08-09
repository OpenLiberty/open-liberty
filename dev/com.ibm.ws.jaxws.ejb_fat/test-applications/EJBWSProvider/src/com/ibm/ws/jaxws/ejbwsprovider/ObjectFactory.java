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
package com.ibm.ws.jaxws.ejbwsprovider;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;

/**
 * This object contains factory methods for each
 * Java content interface and Java element interface
 * generated in the com.ibm.ws.jaxws.ejbwsprovider package.
 * <p>An ObjectFactory allows you to programatically
 * construct new instances of the Java representation
 * for XML content. The Java representation of XML
 * content can consist of schema derived interfaces
 * and classes representing the binding of schema
 * type definitions, element declarations and model
 * groups. Factory methods for each of these are
 * provided in this class.
 *
 */
@XmlRegistry
public class ObjectFactory {

    private final static QName _GetUser_QNAME = new QName("http://ejbbasic.jaxws.ws.ibm.com/", "getUser");
    private final static QName _ListUsers_QNAME = new QName("http://ejbbasic.jaxws.ws.ibm.com/", "listUsers");
    private final static QName _UserNotFoundException_QNAME = new QName("http://ejbbasic.jaxws.ws.ibm.com/", "UserNotFoundException");
    private final static QName _ListUsersResponse_QNAME = new QName("http://ejbbasic.jaxws.ws.ibm.com/", "listUsersResponse");
    private final static QName _GetUserResponse_QNAME = new QName("http://ejbbasic.jaxws.ws.ibm.com/", "getUserResponse");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: com.ibm.ws.jaxws.ejbwsprovider
     *
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link User }
     *
     */
    public User createUser() {
        return new User();
    }

    /**
     * Create an instance of {@link ListUsersResponse }
     *
     */
    public ListUsersResponse createListUsersResponse() {
        return new ListUsersResponse();
    }

    /**
     * Create an instance of {@link GetUser }
     *
     */
    public GetUser createGetUser() {
        return new GetUser();
    }

    /**
     * Create an instance of {@link ListUsers }
     *
     */
    public ListUsers createListUsers() {
        return new ListUsers();
    }

    /**
     * Create an instance of {@link UserNotFoundException }
     *
     */
    public UserNotFoundException createUserNotFoundException() {
        return new UserNotFoundException();
    }

    /**
     * Create an instance of {@link GetUserResponse }
     *
     */
    public GetUserResponse createGetUserResponse() {
        return new GetUserResponse();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetUser }{@code >}}
     *
     */
    @XmlElementDecl(namespace = "http://ejbbasic.jaxws.ws.ibm.com/", name = "getUser")
    public JAXBElement<GetUser> createGetUser(GetUser value) {
        return new JAXBElement<GetUser>(_GetUser_QNAME, GetUser.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link ListUsers }{@code >}}
     *
     */
    @XmlElementDecl(namespace = "http://ejbbasic.jaxws.ws.ibm.com/", name = "listUsers")
    public JAXBElement<ListUsers> createListUsers(ListUsers value) {
        return new JAXBElement<ListUsers>(_ListUsers_QNAME, ListUsers.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link UserNotFoundException }{@code >}}
     *
     */
    @XmlElementDecl(namespace = "http://ejbbasic.jaxws.ws.ibm.com/", name = "UserNotFoundException")
    public JAXBElement<UserNotFoundException> createUserNotFoundException(UserNotFoundException value) {
        return new JAXBElement<UserNotFoundException>(_UserNotFoundException_QNAME, UserNotFoundException.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link ListUsersResponse }{@code >}}
     *
     */
    @XmlElementDecl(namespace = "http://ejbbasic.jaxws.ws.ibm.com/", name = "listUsersResponse")
    public JAXBElement<ListUsersResponse> createListUsersResponse(ListUsersResponse value) {
        return new JAXBElement<ListUsersResponse>(_ListUsersResponse_QNAME, ListUsersResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetUserResponse }{@code >}}
     *
     */
    @XmlElementDecl(namespace = "http://ejbbasic.jaxws.ws.ibm.com/", name = "getUserResponse")
    public JAXBElement<GetUserResponse> createGetUserResponse(GetUserResponse value) {
        return new JAXBElement<GetUserResponse>(_GetUserResponse_QNAME, GetUserResponse.class, null, value);
    }

}
