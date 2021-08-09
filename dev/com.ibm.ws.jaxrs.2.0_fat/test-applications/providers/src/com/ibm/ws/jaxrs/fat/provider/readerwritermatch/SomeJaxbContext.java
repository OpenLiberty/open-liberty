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
package com.ibm.ws.jaxrs.fat.provider.readerwritermatch;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.Validator;

public class SomeJaxbContext extends JAXBContext
{
    @Override
    public Marshaller createMarshaller()
                    throws JAXBException
    {
        return new SomeMarshaller();
    }

    @Override
    public Unmarshaller createUnmarshaller() throws JAXBException
    {
        return new SomeUnmarshaller();
    }

    @Override
    public Validator createValidator() throws JAXBException
    {
        return null;
    }
}