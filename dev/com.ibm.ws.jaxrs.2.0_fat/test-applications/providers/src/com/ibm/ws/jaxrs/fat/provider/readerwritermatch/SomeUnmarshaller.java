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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.PropertyException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.UnmarshallerHandler;
import javax.xml.bind.ValidationEventHandler;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.attachment.AttachmentUnmarshaller;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Source;
import javax.xml.validation.Schema;

import org.w3c.dom.Node;
import org.xml.sax.InputSource;

public class SomeUnmarshaller
                implements Unmarshaller
{
    @Override
    public <A extends XmlAdapter> A getAdapter(Class<A> type)
    {
        return null;
    }

    @Override
    public AttachmentUnmarshaller getAttachmentUnmarshaller()
    {
        return null;
    }

    @Override
    public ValidationEventHandler getEventHandler() throws JAXBException
    {
        return null;
    }

    @Override
    public Unmarshaller.Listener getListener()
    {
        return null;
    }

    @Override
    public Object getProperty(String name) throws PropertyException
    {
        return null;
    }

    @Override
    public Schema getSchema()
    {
        return null;
    }

    @Override
    public UnmarshallerHandler getUnmarshallerHandler()
    {
        return null;
    }

    @Override
    public boolean isValidating() throws JAXBException
    {
        return false;
    }

    @Override
    public void setAdapter(XmlAdapter adapter)
    {}

    @Override
    public <A extends XmlAdapter> void setAdapter(Class<A> type, A adapter)
    {}

    @Override
    public void setAttachmentUnmarshaller(AttachmentUnmarshaller au)
    {}

    @Override
    public void setEventHandler(ValidationEventHandler handler) throws JAXBException
    {}

    @Override
    public void setListener(Unmarshaller.Listener listener)
    {}

    @Override
    public void setProperty(String name, Object value) throws PropertyException
    {}

    @Override
    public void setSchema(Schema schema)
    {}

    @Override
    public void setValidating(boolean validating) throws JAXBException
    {}

    @Override
    public Object unmarshal(File f) throws JAXBException
    {
        try
        {
            FileReader fr = new FileReader(f);
            return unmarshal(fr);
        } catch (FileNotFoundException e) {

            throw new JAXBException(e);
        }
    }

    @Override
    public Object unmarshal(InputStream is)
                    throws JAXBException
    {
        InputStreamReader isr = new InputStreamReader(is);
        return unmarshal(isr);
    }

    @Override
    public Object unmarshal(Reader reader) throws JAXBException
    {
        BufferedReader bf = new BufferedReader(reader);
        try {
            return bf.readLine();
        } catch (IOException e) {
            throw new JAXBException(e);
        }
    }

    @Override
    public Object unmarshal(URL url) throws JAXBException
    {
        try
        {
            return unmarshal(url.openStream());
        } catch (IOException e) {

            throw new JAXBException(e);
        }
    }

    @Override
    public Object unmarshal(InputSource source)
                    throws JAXBException
    {
        return unmarshal((Source) null, String.class);
    }

    @Override
    public Object unmarshal(Node node) throws JAXBException
    {
        return node.toString();
    }

    @Override
    public Object unmarshal(Source source) throws JAXBException
    {
        return unmarshal((Source) null, String.class);
    }

    @Override
    public Object unmarshal(XMLStreamReader reader) throws JAXBException
    {
        return getClass().getSimpleName();
    }

    @Override
    public Object unmarshal(XMLEventReader reader) throws JAXBException
    {
        return getClass().getSimpleName();
    }

    @Override
    public <T> JAXBElement<T> unmarshal(Node node, Class<T> declaredType)
                    throws JAXBException
    {
        return unmarshal((Source) null, declaredType);
    }

    @Override
    public <T> JAXBElement<T> unmarshal(Source source, Class<T> declaredType)
                    throws JAXBException
    {
        String name = getClass().getSimpleName();

        JAXBElement el = new JAXBElement(new QName(name), declaredType, name);

        return el;
    }

    @Override
    public <T> JAXBElement<T> unmarshal(XMLStreamReader reader, Class<T> declaredType)
                    throws JAXBException
    {
        return unmarshal((Source) null, declaredType);
    }

    @Override
    public <T> JAXBElement<T> unmarshal(XMLEventReader reader, Class<T> declaredType)
                    throws JAXBException
    {
        return unmarshal((Source) null, declaredType);
    }
}
