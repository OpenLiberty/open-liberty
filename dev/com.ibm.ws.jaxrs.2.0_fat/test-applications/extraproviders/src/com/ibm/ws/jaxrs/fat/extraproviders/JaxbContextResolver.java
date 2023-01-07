/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs.fat.extraproviders;

import javax.ws.rs.ext.ContextResolver;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import com.ibm.ws.jaxrs.fat.extraprovider.jaxb.bean.Employee;
import com.ibm.ws.jaxrs.fat.extraproviders.jaxb.book.Author;
import com.ibm.ws.jaxrs.fat.extraproviders.jaxb.book.Book;
import com.ibm.ws.jaxrs.fat.extraproviders.jaxb.person.Person;

public class JaxbContextResolver implements ContextResolver<JAXBContext> {

    @Override
    public JAXBContext getContext(Class<?> arg0) {
        try {
            return JAXBContext.newInstance(Employee.class, Person.class, Author.class, Book.class);
        } catch (JAXBException e) {
            e.printStackTrace();
            return null;
        }
    }
}