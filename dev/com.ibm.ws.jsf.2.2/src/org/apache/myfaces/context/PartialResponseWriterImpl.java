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

package org.apache.myfaces.context;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import javax.faces.component.UIComponent;
import javax.faces.context.PartialResponseWriter;
import javax.faces.context.ResponseWriter;

import org.apache.myfaces.util.CDataEndEscapeFilterWriter;

/**
 * <p/>
 * Double buffering partial response writer
 * to take care if embedded CDATA blocks in update delete etc...
 * <p/>
 * According to the spec 13.4.4.1 Writing The Partial Response
 * implementations have to take care to handle nested cdata blocks properly
 * <p/>
 * This means we cannot allow nested CDATA
 * according to the xml spec http://www.w3.org/TR/REC-xml/#sec-cdata-sect
 * everything within a CDATA block is unparsed except for ]]>
 * <p/>
 * Now we have following problem, that CDATA inserts can happen everywhere
 * not only within the CDATA instructions.
 * <p/>
 * What we have to do now is to double buffer CDATA blocks until their end
 * and also!!! parse their content for CDATA embedding and replace it with an escaped end sequence.
 * <p/>
 * Now parsing CDATA embedding is a little bit problematic in case of PPR because
 * it can happen that someone simply adds a CDATA in a javascript string or somewhere else.
 * Because he/she is not aware that we wrap the entire content into CDATA.
 * Simply encoding and decoding of the CDATA is similarly problematic
 * because the browser then chokes on embedded //<![CDATA[ //]]> sections
 * <p/>
 * What we do for now is to simply remove //<![CDATA[ and //]]>
 * and replace all other pending cdatas with their cdata escapes
 * ]]&gt; becomes &lt;![CDATA[]]]]&gt;&lt;![CDATA[&gt;
 * <p/>
 * If this causes problems in corner cases we also can add a second encoding step in
 * case of the cdata Javascript comment removal is not enough to cover all corner cases.
 * <p/>
 * For now I will only implement this in the impl, due to the spec stating
 * that implementations are responsible of the correct CDATA handling!
 *
 * @author Werner Punz (latest modification by $Author: lu4242 $)
 * @version $Revision: 1542444 $ $Date: 2013-11-16 01:41:08 +0000 (Sat, 16 Nov 2013) $
 */

public class PartialResponseWriterImpl extends PartialResponseWriter
{

    class StackEntry
    {
        ResponseWriter writer;
        Writer _doubleBuffer;

        StackEntry(ResponseWriter writer, Writer doubleBuffer)
        {
            this.writer = writer;
            _doubleBuffer = doubleBuffer;
        }

        public ResponseWriter getWriter()
        {
            return writer;
        }

        public void setWriter(ResponseWriter writer)
        {
            this.writer = writer;
        }

        public Writer getDoubleBuffer()
        {
            return _doubleBuffer;
        }

        public void setDoubleBuffer(Writer doubleBuffer)
        {
            _doubleBuffer = doubleBuffer;
        }
    }

    ResponseWriter _cdataDoubleBufferWriter = null;
    Writer _doubleBuffer = null;
    List<StackEntry> _nestingStack = new ArrayList<StackEntry>(4);

    public PartialResponseWriterImpl(ResponseWriter writer)
    {
        super(writer);
    }

    @Override
    public void startCDATA() throws IOException
    {
        if (!isDoubleBufferEnabled())
        {
            super.startCDATA();
        }
        else
        {
            _cdataDoubleBufferWriter.write("<![CDATA[");
        }
        openDoubleBuffer();
    }

    private void openDoubleBuffer()
    {
        _doubleBuffer = new CDataEndEscapeFilterWriter(_cdataDoubleBufferWriter == null ? 
                this.getWrapped() : _cdataDoubleBufferWriter );
        _cdataDoubleBufferWriter = getWrapped().cloneWithWriter(_doubleBuffer);

        StackEntry entry = new StackEntry(_cdataDoubleBufferWriter, _doubleBuffer);

        _nestingStack.add(0, entry);
    }

    @Override
    public void endCDATA() throws IOException
    {
        closeDoubleBuffer(false);
        if (isDoubleBufferEnabled())
        {
            _cdataDoubleBufferWriter.write("]]>");
        }
        else
        {
            super.endCDATA();
        }
    }

    /**
     * Close double buffer condition
     * This does either a normal close or a force
     * close in case of a force close
     * the entire buffer  is pushed with the post processing
     * operations into the originating writer
     *
     * @param force if set to true the close is a forced close which in any condition
     *              immediately pushes the buffer content into our writer with a pre operation
     *              done upfront, in case of a false, the buffer is only swept out if our
     *              internal CDATA nesting counter is at the nesting depth 1
     * @throws IOException
     */
    private void closeDoubleBuffer(boolean force) throws IOException
    {
        if (!isDoubleBufferEnabled())
        {
            return;
        }
        /*
        * if a force close is issued we reset the condition
        * to 1 to reach the underlying closing block
        */

        if (force)
        {
            while (!_nestingStack.isEmpty())
            {
                popAndEncodeCurrentStackEntry();

            }
        }
        else
        {
            popAndEncodeCurrentStackEntry();
        }
    }

    private void popAndEncodeCurrentStackEntry() throws IOException
    {
        _nestingStack.remove(0);
        StackEntry parent = (_nestingStack.isEmpty()) ? null : _nestingStack.get(0);
        if (parent != null)
        {
            _cdataDoubleBufferWriter = parent.getWriter();
            _doubleBuffer = parent.getDoubleBuffer();
        }
        else
        {
            _cdataDoubleBufferWriter = null;
            _doubleBuffer = null;
        }
    }

    //--- we need to override ppr specifics to cover the case

    @Override
    public void endInsert() throws IOException
    {
        //we use a force close here to fix possible user CDATA corrections
        //under normal conditions the force close just processes the same
        //the underlying close cdata does, but nevertheless
        //it is better to have an additional layer of fixup
        closeDoubleBuffer(true);
        super.endInsert();
    }

    @Override
    public void endUpdate() throws IOException
    {
        //we use a force close here to fix possible user CDATA corrections
        //under normal conditions the force close just processes the same
        //the underlying close cdata does, but nevertheless
        //it is better to have an additional layer of fixup
        closeDoubleBuffer(true);
        super.endUpdate();    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public void endExtension() throws IOException
    {
        //we use a force close here to fix possible user CDATA corrections
        //under normal conditions the force close just processes the same
        //the underlying close cdata does, but nevertheless
        //it is better to have an additional layer of fixup
        closeDoubleBuffer(true);
        super.endExtension();    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public void endEval() throws IOException
    {
        //we use a force close here to fix possible user CDATA corrections
        //under normal conditions the force close just processes the same
        //the underlying close cdata does, but nevertheless
        //it is better to have an additional layer of fixup
        closeDoubleBuffer(true);
        super.endEval();    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public void endError() throws IOException
    {
        //we use a force close here to fix possible user CDATA corrections
        //under normal conditions the force close just processes the same
        //the underlying close cdata does, but nevertheless
        //it is better to have an additional layer of fixup
        closeDoubleBuffer(true);
        super.endError();    //To change body of overridden methods use File | Settings | File Templates.
    }

    //--- optional delegation method ---

    @Override
    public void endElement(String name) throws IOException
    {
        if (isDoubleBufferEnabled())
        {
            _cdataDoubleBufferWriter.endElement(name);
        }
        else
        {
            super.endElement(name);
        }
    }

    @Override
    public void writeComment(Object comment) throws IOException
    {
        if (isDoubleBufferEnabled())
        {
            _cdataDoubleBufferWriter.writeComment(comment);
        }
        else
        {
            super.writeComment(comment);
        }
    }

    private boolean isDoubleBufferEnabled()
    {
        return !_nestingStack.isEmpty();
    }

    @Override
    public void startElement(String name, UIComponent component) throws IOException
    {
        if (isDoubleBufferEnabled())
        {
            _cdataDoubleBufferWriter.startElement(name, component);
        }
        else
        {
            super.startElement(name, component);
        }
    }

    @Override
    public void writeText(Object text, String property) throws IOException
    {
        if (isDoubleBufferEnabled())
        {
            _cdataDoubleBufferWriter.writeText(text, property);
        }
        else
        {
            super.writeText(text, property);
        }
    }

    @Override
    public void writeText(char[] text, int off, int len) throws IOException
    {
        if (isDoubleBufferEnabled())
        {
            _cdataDoubleBufferWriter.writeText(text, off, len);
        }
        else
        {
            super.writeText(text, off, len);
        }
    }

    @Override
    public void write(char[] cbuf, int off, int len) throws IOException
    {
        if (isDoubleBufferEnabled())
        {
            _cdataDoubleBufferWriter.write(cbuf, off, len);
        }
        else
        {
            super.write(cbuf, off, len);
        }
    }

    @Override
    public ResponseWriter cloneWithWriter(Writer writer)
    {
        return super.cloneWithWriter(writer);
    }

    @Override
    public void writeURIAttribute(String name, Object value, String property) throws IOException
    {
        if (isDoubleBufferEnabled())
        {
            _cdataDoubleBufferWriter.writeURIAttribute(name, value, property);
        }
        else
        {
            super.writeURIAttribute(name, value, property);
        }
    }

    @Override
    public void close() throws IOException
    {
        //in case of a close
        //we have a user error of a final CDATA block
        //we do some error correction here
        //since a close is issued we do not care about
        //a proper closure of the cdata block here anymore
        if (isDoubleBufferEnabled())
        {
            //we have to properly close all nested cdata stacks
            //end end our cdata block if open
            closeDoubleBuffer(true);

            super.endCDATA();
        }
        super.close();
    }

    @Override
    public void flush() throws IOException
    {
        if (isDoubleBufferEnabled())
        {
            _cdataDoubleBufferWriter.flush();
        }
        super.flush();
    }

    @Override
    public void writeAttribute(String name, Object value, String property) throws IOException
    {
        if (isDoubleBufferEnabled())
        {
            _cdataDoubleBufferWriter.writeAttribute(name, value, property);
        }
        else
        {
            super.writeAttribute(name, value, property);
        }
    }

    @Override
    public void writeText(Object object, UIComponent component, String string) throws IOException
    {
        if (isDoubleBufferEnabled())
        {
            _cdataDoubleBufferWriter.writeText(object, component, string);
        }
        else
        {
            super.writeText(object, component, string);
        }
    }

    @Override
    public Writer append(char c) throws IOException
    {
        if (isDoubleBufferEnabled())
        {
            _cdataDoubleBufferWriter.append(c);
            return this;
        }
        else
        {
            return super.append(c);
        }
    }

    @Override
    public Writer append(CharSequence csq, int start, int end) throws IOException
    {
        if (isDoubleBufferEnabled())
        {
            _cdataDoubleBufferWriter.append(csq, start, end);
            return this;
        }
        else
        {
            return super.append(csq, start, end);
        }
    }

    @Override
    public Writer append(CharSequence csq) throws IOException
    {
        if (isDoubleBufferEnabled())
        {
            _cdataDoubleBufferWriter.append(csq);
            return this;
        }
        else
        {
            return super.append(csq);
        }
    }

    @Override
    public void write(char[] cbuf) throws IOException
    {
        if (isDoubleBufferEnabled())
        {
            _cdataDoubleBufferWriter.write(cbuf);
        }
        else
        {
            super.write(cbuf);
        }
    }

    @Override
    public void write(int c) throws IOException
    {
        if (isDoubleBufferEnabled())
        {
            _cdataDoubleBufferWriter.write(c);
        }
        else
        {
            super.write(c);
        }
    }

    @Override
    public void write(String str, int off, int len) throws IOException
    {
        if (isDoubleBufferEnabled())
        {
            _cdataDoubleBufferWriter.write(str, off, len);
        }
        else
        {
            super.write(str, off, len);
        }
    }

    @Override
    public void write(String str) throws IOException
    {
        if (isDoubleBufferEnabled())
        {
            _cdataDoubleBufferWriter.write(str);
        }
        else
        {
            super.write(str);
        }
    }
}
