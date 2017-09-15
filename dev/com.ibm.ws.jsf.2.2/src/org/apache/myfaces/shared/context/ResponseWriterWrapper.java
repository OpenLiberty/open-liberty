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
package org.apache.myfaces.shared.context;

import javax.faces.component.UIComponent;
import javax.faces.context.ResponseWriter;
import java.io.IOException;
import java.io.Writer;

/**
 * Convenient base class for writing a ResponseWriter wrapper class.
 */
public abstract class ResponseWriterWrapper
        extends ResponseWriter
{
    protected ResponseWriter _responseWriter;

    public ResponseWriterWrapper(ResponseWriter responseWriter)
    {
        _responseWriter = responseWriter;
    }

    public String getContentType()
    {
        return _responseWriter.getContentType();
    }

    public String getCharacterEncoding()
    {
        return _responseWriter.getCharacterEncoding();
    }

    public void flush() throws IOException
    {
        _responseWriter.flush();
    }

    public void startDocument() throws IOException
    {
        _responseWriter.startDocument();
    }

    public void endDocument() throws IOException
    {
        _responseWriter.endDocument();
    }

    public void startElement(String s, UIComponent uicomponent) throws IOException
    {
        _responseWriter.startElement(s, uicomponent);
    }

    public void endElement(String s) throws IOException
    {
        _responseWriter.endElement(s);
    }

    public void writeAttribute(String s, Object obj, String s1) throws IOException
    {
        _responseWriter.writeAttribute(s, obj, s1);
    }

    public void writeURIAttribute(String s, Object obj, String s1) throws IOException
    {
        _responseWriter.writeURIAttribute(s, obj, s1);
    }

    public void writeComment(Object obj) throws IOException
    {
        _responseWriter.writeComment(obj);
    }

    public void writeText(Object obj, String s) throws IOException
    {
        _responseWriter.writeText(obj, s);
    }

    public void writeText(char ac[], int i, int j) throws IOException
    {
        _responseWriter.writeText(ac, i, j);
    }

    public abstract ResponseWriter cloneWithWriter(Writer writer);

    public void close() throws IOException
    {
        _responseWriter.close();
    }

    public void write(char cbuf[], int off, int len) throws IOException
    {
        _responseWriter.write(cbuf, off, len);
    }

    public void write(int c) throws IOException
    {
        _responseWriter.write(c);
    }

    public void write(char cbuf[]) throws IOException
    {
        _responseWriter.write(cbuf);
    }

    public void write(String str) throws IOException
    {
        _responseWriter.write(str);
    }

    public void write(String str, int off, int len) throws IOException
    {
        _responseWriter.write(str, off, len);
    }
}
