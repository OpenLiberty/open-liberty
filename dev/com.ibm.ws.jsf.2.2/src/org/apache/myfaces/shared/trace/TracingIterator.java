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
package org.apache.myfaces.shared.trace;

import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Trace method calls to an iterator
 */
public class TracingIterator<E> implements Iterator<E>
{
    private final Iterator<E> _iterator;

    private final TracingSupport _tracer;

    public TracingIterator(Iterator<E> iterator)
    {
        _iterator = iterator;
        _tracer = new TracingSupport(iterator.getClass());
    }

    public static <E> TracingIterator<E> create(Iterator<E> iterator)
    {
        return new TracingIterator<E>(iterator);
    }

    /**
     * @return the iterator
     */
    public Iterator<E> getIterator()
    {
        return _iterator;
    }

    public boolean hasNext()
    {
        return _tracer.trace("hasNext", new Closure<Boolean>()
        {
            public Boolean call()
            {
                return _iterator.hasNext();
            }
        });
    }

    public E next()
    {
        return _tracer.trace("next", new Closure<E>()
        {
            public E call()
            {
                return _iterator.next();
            }
        });
    }

    public void remove()
    {
        _tracer.trace("remove", new Closure<Object>()
        {
            public Object call()
            {
                _iterator.remove();
                return Void.class;
            }
        });
    }

    /**
     * @return
     * @see org.apache.myfaces.shared.trace.TracingSupport#getLevel()
     */
    public Level getLevel()
    {
        return _tracer.getLevel();
    }

    /**
     * @return
     * @see org.apache.myfaces.shared.trace.TracingSupport#getLogger()
     */
    public Logger getLogger()
    {
        return _tracer.getLogger();
    }

    /**
     * @return
     * @see org.apache.myfaces.shared.trace.TracingSupport#getSourceClass()
     */
    public String getSourceClass()
    {
        return _tracer.getSourceClass();
    }

    /**
     * @param level
     * @see org.apache.myfaces.shared.trace.TracingSupport#setLevel(java.util.logging.Level)
     */
    public void setLevel(Level level)
    {
        _tracer.setLevel(level);
    }

    /**
     * @param logger
     * @see org.apache.myfaces.shared.trace.TracingSupport#setLogger(java.util.logging.Logger)
     */
    public void setLogger(Logger logger)
    {
        _tracer.setLogger(logger);
    }

    /**
     * @param className
     * @see org.apache.myfaces.shared.trace.TracingSupport#setSourceClass(java.lang.String)
     */
    public void setSourceClass(String className)
    {
        _tracer.setSourceClass(className);
    }
}
