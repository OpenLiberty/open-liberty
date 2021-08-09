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
package org.apache.myfaces.view.facelets;

import org.apache.myfaces.view.facelets.util.FastWriter;

import javax.faces.context.FacesContext;
import java.io.IOException;
import java.io.Writer;

/**
 * A class for handling state insertion. Content is written directly to "out" until an attempt to write state; at that
 * point, it's redirected into a buffer that can be picked through in theory, this buffer should be very small, since it
 * only needs to be enough to contain all the content after the close of the first (and, hopefully, only) form.
 * <p>
 * Potential optimizations:
 * <ul>
 * <li>If we created a new FastWriter at each call to writingState(), and stored a List of them, then we'd know that
 * state tokens could only possibly be near the start of each buffer (and might not be there at all). (There might be a
 * close-element before the state token). Then, we'd only need to check the start of the buffer for the state token; if
 * it's there, write out the real state, then blast the rest of the buffer out. This wouldn't even require toString(),
 * which for large buffers is expensive. However, this optimization is only going to be especially meaningful for the
 * multi-form case.</li>
 * <li>More of a FastWriter optimization than a StateWriter, but: it is far faster to create a set of small 1K buffers
 * than constantly reallocating one big buffer.</li>
 * </ul>
 * 
 * @author Adam Winer
 * @version $Id: StateWriter.java 1526465 2013-09-26 12:39:16Z lu4242 $
 */
public final class StateWriter extends Writer
{

    private static final String CURRENT_WRITER_KEY = "org.apache.myfaces.view.facelets.StateWriter.CURRENT_WRITER";

    private int initialSize;
    private Writer out;
    private FastWriter fast;
    private boolean writtenState;
    private boolean writtenStateWithoutWrapper;

    static public StateWriter getCurrentInstance()
    {
        FacesContext facesContext = FacesContext.getCurrentInstance();

        return (StateWriter)facesContext.getAttributes().get(CURRENT_WRITER_KEY);
    }
        
    static public StateWriter getCurrentInstance(FacesContext facesContext)
    {
        return (StateWriter)facesContext.getAttributes().get(CURRENT_WRITER_KEY);
    }

    private static void setCurrentInstance(StateWriter stateWriter)
    {
        FacesContext facesContext = FacesContext.getCurrentInstance();

        if (stateWriter == null)
        {
            facesContext.getAttributes().remove(CURRENT_WRITER_KEY);
        }
        else
        {
            facesContext.getAttributes().put(CURRENT_WRITER_KEY, stateWriter);
        }
    }
    
    private static void setCurrentInstance(StateWriter stateWriter, FacesContext facesContext)
    {
        //FacesContext facesContext = FacesContext.getCurrentInstance();

        if (stateWriter == null)
        {
            facesContext.getAttributes().remove(CURRENT_WRITER_KEY);
        }
        else
        {
            facesContext.getAttributes().put(CURRENT_WRITER_KEY, stateWriter);
        }
    }

    public StateWriter(Writer initialOut, int initialSize)
    {
        if (initialSize < 0)
        {
            throw new IllegalArgumentException("Initial Size cannot be less than 0");
        }

        this.initialSize = initialSize;
        this.out = initialOut;
        setCurrentInstance(this);
    }
    
    public StateWriter(Writer initialOut, int initialSize, FacesContext facesContext)
    {
        if (initialSize < 0)
        {
            throw new IllegalArgumentException("Initial Size cannot be less than 0");
        }

        this.initialSize = initialSize;
        this.out = initialOut;
        setCurrentInstance(this, facesContext);
    }

    /**
     * Mark that state is about to be written. Contrary to what you'd expect, we cannot and should not assume that this
     * location is really going to have state; it is perfectly legit to have a ResponseWriter that filters out content,
     * and ignores an attempt to write out state at this point. So, we have to check after the fact to see if there
     * really are state markers.
     */
    public void writingState()
    {
        if (!this.writtenState)
        {
            this.writtenState = true;
            this.writtenStateWithoutWrapper = false;
            this.fast = new FastWriter(this.initialSize);
            this.out = this.fast;
        }
    }
    
    public boolean isStateWritten()
    {
        return this.writtenState;
    }

    public void writingStateWithoutWrapper()
    {
        if (!this.writtenState && !this.writtenStateWithoutWrapper)
        {
            this.writtenStateWithoutWrapper = true;
        }
    }    

    public boolean isStateWrittenWithoutWrapper()
    {
        return this.writtenStateWithoutWrapper;
    }

    public void close() throws IOException
    {
        // do nothing
    }

    public void flush() throws IOException
    {
        if (!this.writtenState)
        {
            this.out.flush();
        }
    }

    public void write(char[] cbuf, int off, int len) throws IOException
    {
        this.out.write(cbuf, off, len);
    }

    public void write(char[] cbuf) throws IOException
    {
        this.out.write(cbuf);
    }

    public void write(int c) throws IOException
    {
        this.out.write(c);
    }

    public void write(String str, int off, int len) throws IOException
    {
        this.out.write(str, off, len);
    }

    public void write(String str) throws IOException
    {
        this.out.write(str);
    }

    public String getAndResetBuffer()
    {
        if (!this.writtenState)
        {
            throw new IllegalStateException("Did not write state;  no buffer is available");
        }

        String result = this.fast.toString();
        this.fast.reset();
        return result;
    }

    public void release()
    {
        // remove from FacesContext attribute Map
        setCurrentInstance(null);
    }
    
    public void release(FacesContext facesContext)
    {
        // remove from FacesContext attribute Map
        setCurrentInstance(null, facesContext);
    }

}
