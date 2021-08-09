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
package javax.faces.webapp;

import javax.servlet.jsp.PageContext;
import java.io.IOException;
import java.io.Writer;

/**
 * This Writer writes always to the current pageContext.getOut() Writer.
 */
class _PageContextOutWriter extends Writer
{
    private PageContext _pageContext;

    public _PageContextOutWriter(PageContext pageContext)
    {
        _pageContext = pageContext;
    }

    @Override
    public void close() throws IOException
    {
        _pageContext.getOut().close();
    }

    @Override
    public void flush() throws IOException
    {
        _pageContext.getOut().flush();
    }

    @Override
    public void write(char cbuf[], int off, int len) throws IOException
    {
        _pageContext.getOut().write(cbuf, off, len);
    }

    @Override
    public void write(int c) throws IOException
    {
        _pageContext.getOut().write(c);
    }

    @Override
    public void write(char cbuf[]) throws IOException
    {
        _pageContext.getOut().write(cbuf);
    }

    @Override
    public void write(String str) throws IOException
    {
        _pageContext.getOut().write(str);
    }

    @Override
    public void write(String str, int off, int len) throws IOException
    {
        _pageContext.getOut().write(str, off, len);
    }

}
