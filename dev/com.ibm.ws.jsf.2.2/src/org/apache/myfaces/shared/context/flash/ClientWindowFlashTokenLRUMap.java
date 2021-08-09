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

package org.apache.myfaces.shared.context.flash;

import java.io.Serializable;
import java.util.Map;
import javax.faces.FacesWrapper;
import javax.faces.context.FacesContext;
import javax.faces.context.Flash;
import org.apache.commons.collections.map.LRUMap;

/**
 *
 */
class ClientWindowFlashTokenLRUMap extends LRUMap implements Serializable
{

    public ClientWindowFlashTokenLRUMap()
    {
    }

    public ClientWindowFlashTokenLRUMap(int maxSize)
    {
        super(maxSize);
    }

    public ClientWindowFlashTokenLRUMap(int maxSize, boolean scanUntilRemovable)
    {
        super(maxSize, scanUntilRemovable);
    }

    public ClientWindowFlashTokenLRUMap(int maxSize, float loadFactor)
    {
        super(maxSize, loadFactor);
    }

    public ClientWindowFlashTokenLRUMap(int maxSize, float loadFactor, boolean scanUntilRemovable)
    {
        super(maxSize, loadFactor, scanUntilRemovable);
    }

    public ClientWindowFlashTokenLRUMap(Map map)
    {
        super(map);
    }

    public ClientWindowFlashTokenLRUMap(Map map, boolean scanUntilRemovable)
    {
        super(map, scanUntilRemovable);
    }
    
    @Override
    protected boolean removeLRU(LinkEntry entry)
    {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        Flash flash = facesContext.getExternalContext().getFlash();
        if (flash != null)
        {
            ReleasableFlash rf = null;
            while (flash != null)
            {
                if (flash instanceof ReleasableFlash)
                {
                    rf = (ReleasableFlash) flash;
                    break;
                }
                if (flash instanceof FacesWrapper)
                {
                    flash = ((FacesWrapper<? extends Flash>) flash).getWrapped();
                }
                else
                {
                    flash = null;
                }
            }
            if (rf != null)
            {
                rf.clearFlashMap(facesContext, (String) entry.getKey(), (String) entry.getValue());
            }
        }
        return super.removeLRU(entry);
    }
}
