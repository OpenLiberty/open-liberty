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
package org.apache.myfaces.view.facelets.component;

import java.io.Serializable;

/**
 * This class is used as the bean that contains all the status
 * information of UIRepeat during iteration. It is stored in the 
 * RequestScope under the key specified in varStatus of UIRepeat.
 * 
 * @author Curtiss Howard (latest modification by $Author: struberg $)
 * @version $Revision: 1189297 $ $Date: 2011-10-26 16:28:38 +0000 (Wed, 26 Oct 2011) $
 */
public final class RepeatStatus implements Serializable
{

    private static final long serialVersionUID = 1L;
    
    private final int count;
    
    private final int index;

    private final boolean first;

    private final boolean last;

    private final Integer begin;

    private final Integer end;

    private final Integer step;
    
    public RepeatStatus(boolean first, boolean last, int count, int index, 
            Integer begin, Integer end, Integer step)
    {
        this.count = count;
        this.index = index;
        this.begin = begin;
        this.end = end;
        this.step = step;
        this.first = first;
        this.last = last;
    }

    public boolean isFirst()
    {
        return first;
    }

    public boolean isLast()
    {
        return last;
    }
    
    public boolean isEven ()
    {
        return ((count % 2) == 0);
    }
    
    public boolean isOdd ()
    {
        return !isEven();
    }
    
    public Integer getBegin()
    {
        if (begin == -1)
        {
            return null;
        }
        
        return begin;
    }
    
    public Integer getEnd()
    {
        if (end == -1)
        {
            return null;
        }
        
        return end;
    }

    public int getIndex()
    {
        return index;
    }

    public Integer getStep()
    {
        if (step == -1)
        {
            return null;
        }
        
        return step;
    }

}
