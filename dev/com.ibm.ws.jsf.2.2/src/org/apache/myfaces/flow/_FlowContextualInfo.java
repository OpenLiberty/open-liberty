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
package org.apache.myfaces.flow;

import java.io.Serializable;

/**
 *
 * @author lu4242
 */
class _FlowContextualInfo implements Serializable
{
    private static final long serialVersionUID = -3732849062185115847L;
    
    private FlowReference flowReference;
    private String lastDisplayedViewId;
    private FlowReference sourceFlowReference;

    public _FlowContextualInfo()
    {
    }
    
    public _FlowContextualInfo(FlowReference flowReference, String lastDisplayedViewId, 
        FlowReference souFlowReference)
    {
        this.flowReference = flowReference;
        this.lastDisplayedViewId = lastDisplayedViewId;
        this.sourceFlowReference = souFlowReference;
    }

    @Override
    public int hashCode()
    {
        int hash = 3;
        hash = 23 * hash + (this.flowReference != null ? this.flowReference.hashCode() : 0);
        hash = 23 * hash + (this.lastDisplayedViewId != null ? this.lastDisplayedViewId.hashCode() : 0);
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
        final _FlowContextualInfo other = (_FlowContextualInfo) obj;
        if (this.flowReference != other.flowReference && (this.flowReference == null || 
            !this.flowReference.equals(other.flowReference)))
        {
            return false;
        }
        if ((this.lastDisplayedViewId == null) ? (other.lastDisplayedViewId != null) : 
            !this.lastDisplayedViewId.equals(other.lastDisplayedViewId))
        {
            return false;
        }
        return true;
    }
    

    /**
     * @return the flowReference
     */
    public FlowReference getFlowReference()
    {
        return flowReference;
    }

    /**
     * @param flowReference the flowReference to set
     */
    public void setFlowReference(FlowReference flowReference)
    {
        this.flowReference = flowReference;
    }

    /**
     * @return the lastDisplayedViewId
     */
    public String getLastDisplayedViewId()
    {
        return lastDisplayedViewId;
    }

    /**
     * @param lastDisplayedViewId the lastDisplayedViewId to set
     */
    public void setLastDisplayedViewId(String lastDisplayedViewId)
    {
        this.lastDisplayedViewId = lastDisplayedViewId;
    }

    /**
     * @return the sourceFlowReference
     */
    public FlowReference getSourceFlowReference()
    {
        return sourceFlowReference;
    }

    /**
     * @param sourceFlowReference the sourceFlowReference to set
     */
    public void setSourceFlowReference(FlowReference sourceFlowReference)
    {
        this.sourceFlowReference = sourceFlowReference;
    }
}
