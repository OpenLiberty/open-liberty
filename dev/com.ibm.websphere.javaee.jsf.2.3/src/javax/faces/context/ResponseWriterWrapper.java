/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package javax.faces.context;

import javax.faces.component.UIComponent;
import javax.faces.FacesWrapper;
import java.io.IOException;
import java.io.Writer;

/**
 * see Javadoc of <a href="http://java.sun.com/j2ee/javaserverfaces/1.2/docs/api/index.html">JSF Specification</a>
 */
public abstract class ResponseWriterWrapper extends ResponseWriter implements FacesWrapper<ResponseWriter>
{
    private ResponseWriter delegate;

    @Deprecated
    public ResponseWriterWrapper()
    {
    }

    public ResponseWriterWrapper(ResponseWriter delegate)
    {
        this.delegate = delegate;
    }
    
    @Override
    public ResponseWriter cloneWithWriter(Writer writer)
    {
        return getWrapped().cloneWithWriter(writer);
    }

    @Override
    public void close() throws IOException
    {
        getWrapped().close();
    }

    @Override
    public void endCDATA() throws IOException
    {
        getWrapped().endCDATA();
    }

    @Override
    public void endDocument() throws IOException
    {
        getWrapped().endDocument();
    }

    @Override
    public void endElement(String name) throws IOException
    {
        getWrapped().endElement(name);
    }

    @Override
    public void flush() throws IOException
    {
        getWrapped().flush();
    }

    @Override
    public String getCharacterEncoding()
    {
        return getWrapped().getCharacterEncoding();
    }

    @Override
    public String getContentType()
    {
        return getWrapped().getContentType();
    }

    @Override
    public ResponseWriter getWrapped()
    {
        return delegate;
    }

    @Override
    public void startCDATA() throws IOException
    {
        getWrapped().startCDATA();
    }
    
    @Override
    public void startDocument() throws IOException
    {
        getWrapped().startDocument();
    }

    @Override
    public void startElement(String name, UIComponent component) throws IOException
    {
        getWrapped().startElement(name, component);
    }

    @Override
    public void write(char[] cbuf, int off, int len) throws IOException
    {
        getWrapped().write(cbuf, off, len);
    }

    @Override
    public void writeAttribute(String name, Object value, String property) throws IOException
    {
        getWrapped().writeAttribute(name, value, property);
    }

    @Override
    public void writeComment(Object comment) throws IOException
    {
        getWrapped().writeComment(comment);
    }

    @Override
    public void writeText(char[] text, int off, int len) throws IOException
    {
        getWrapped().writeText(text, off, len);
    }
    
    @Override
    public void writeText(Object text, String property) throws IOException
    {
        getWrapped().writeText(text, property);
    }

    @Override
    public void writeText(Object object, UIComponent component, String string) throws IOException
    {
        getWrapped().writeText(object, component, string);
    }
    
    @Override
    public void writeURIAttribute(String name, Object value, String property) throws IOException
    {
        getWrapped().writeURIAttribute(name, value, property);
    }

    @Override
    public void writeDoctype(String doctype) throws IOException
    {
        getWrapped().writeDoctype(doctype);
    }

    @Override
    public void writePreamble(String preamble) throws IOException
    {
        getWrapped().writePreamble(preamble);
    }
}
