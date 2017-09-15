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
package org.apache.myfaces.view.facelets.tag.jstl.core;

import java.io.Serializable;

/**
 * @author Jacob Hookom
 * @version $Id: IterationStatus.java 1194861 2011-10-29 10:02:34Z struberg $
 */
public final class IterationStatus implements Serializable
{

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    private final int index;

    private final boolean first;

    private final boolean last;

    private final Integer begin;

    private final Integer end;

    private final Integer step;
    
    private final Object value;
    
    /**
     * 
     */
    public IterationStatus(boolean first, boolean last, int index, Integer begin, Integer end,
                           Integer step, Object value)
    {
        this.index = index;
        this.begin = begin;
        this.end = end;
        this.step = step;
        this.first = first;
        this.last = last;
        this.value = value;
    }

    public boolean isFirst()
    {
        return this.first;
    }

    public boolean isLast()
    {
        return this.last;
    }

    public Integer getBegin()
    {
        return begin;
    }
    
    public Integer getCount()
    {
        if ((step == null) || (step == 1))
        {
            return (index + 1);
        }
        
        return ((index / step) + 1);
    }
    
    public Object getCurrent()
    {
        return value;
    }
    
    public Integer getEnd()
    {
        return end;
    }

    public int getIndex()
    {
        return index;
    }

    public Integer getStep()
    {
        return step;
    }

}
