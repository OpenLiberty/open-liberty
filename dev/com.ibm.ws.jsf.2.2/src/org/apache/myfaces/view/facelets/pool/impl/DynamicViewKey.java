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
package org.apache.myfaces.view.facelets.pool.impl;

import java.io.Serializable;
import org.apache.myfaces.view.facelets.tag.jsf.FaceletState;

/**
 *
 * @author Leonardo Uribe
 */
public class DynamicViewKey implements Serializable
{
    private final FaceletState faceletState;

    public DynamicViewKey(FaceletState faceletState)
    {
        this.faceletState = faceletState;
    }

    public FaceletState getFaceletState()
    {
        return faceletState;
    }

    @Override
    public int hashCode()
    {
        int hash = 5;
        hash = 79 * hash + (this.faceletState != null ? this.faceletState.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == null)
        {
            return false;
        }
        if (getClass() != obj.getClass())
        {
            return false;
        }
        final DynamicViewKey other = (DynamicViewKey) obj;
        if (this.faceletState != other.faceletState && (this.faceletState == null || 
                !this.faceletState.equals(other.faceletState)))
        {
            return false;
        }
        return true;
    }
}
