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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;

import javax.ejb.Stateless;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.ws.BindingType;
import javax.xml.ws.Provider;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.WebServiceProvider;
import javax.xml.ws.soap.Addressing;

/**
 *
 */
@WebServiceProvider(
                    portName = "UserQueryPort",
                    serviceName = "UserQueryService",
                    targetNamespace = "http://ejbbasic.jaxws.ws.ibm.com/")
@BindingType(javax.xml.ws.soap.SOAPBinding.SOAP11HTTP_BINDING)
@Stateless(name = "UserQuery")
@Addressing(enabled = true, required = false)
public class UserQueryProvider implements Provider<Source> {

    private static final JAXBContext USER_QUERY_JAXB_CONTEXT;

    private static final ObjectFactory USER_QUERY_OBJECT_FACTORY = new ObjectFactory();

    static {
        try {
            USER_QUERY_JAXB_CONTEXT = JAXBContext.newInstance(ObjectFactory.class);
        } catch (JAXBException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    @javax.xml.ws.Action(input = "http://ejbbasic.jaxws.ws.ibm.com/UserQuery#getUser")
    public Source invoke(Source request) {
        try {
            JAXBElement element = (JAXBElement) USER_QUERY_JAXB_CONTEXT.createUnmarshaller().unmarshal(request);
            Object requestObject = element.getValue();
            System.out.println("Request JAXB Element = [" + element.getName() + "] Type = [" + requestObject.getClass().getName() + "]");

            if (requestObject instanceof GetUser) {
                GetUser getUser = (GetUser) requestObject;
                try {
                    User user = StaticUserRepository.getUser(getUser.getArg0());
                    GetUserResponse getUserResponse = new GetUserResponse();
                    getUserResponse.setReturn(user);

                    return createStreamSource(USER_QUERY_OBJECT_FACTORY.createGetUserResponse(getUserResponse));
                } catch (UserNotFoundException_Exception e) {
                    return createStreamSource(USER_QUERY_OBJECT_FACTORY.createUserNotFoundException(e.getFaultInfo()));
                }
            } else if (requestObject instanceof ListUsers) {
                ListUsersResponse listUsersResponse = new ListUsersResponse();
                listUsersResponse._return = Arrays.asList(StaticUserRepository.listUsers());

                return createStreamSource(USER_QUERY_OBJECT_FACTORY.createListUsersResponse(listUsersResponse));
            } else {
                throw new WebServiceException("Un-recongnized request");
            }

        } catch (JAXBException e) {
            throw new WebServiceException("Provider endpoint failed", e);
        }
    }

    private StreamSource createStreamSource(Object value) throws JAXBException {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        Marshaller m = USER_QUERY_JAXB_CONTEXT.createMarshaller();
        m.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
        m.marshal(value, bout);
        return new StreamSource(new ByteArrayInputStream(bout.toByteArray()));
    }

}
