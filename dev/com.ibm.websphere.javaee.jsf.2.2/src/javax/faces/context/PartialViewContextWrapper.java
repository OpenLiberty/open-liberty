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
package javax.faces.context;

import java.util.Collection;

import javax.faces.FacesWrapper;
import javax.faces.event.PhaseId;

/**
 * @since 2.0
 */
public abstract class PartialViewContextWrapper extends PartialViewContext implements FacesWrapper<PartialViewContext>
{
    /**
     * 
     */
    public PartialViewContextWrapper()
    {
    }

    @Override
    public Collection<String> getExecuteIds()
    {
        return getWrapped().getExecuteIds();
    }

    @Override
    public PartialResponseWriter getPartialResponseWriter()
    {
        return getWrapped().getPartialResponseWriter();
    }

    @Override
    public Collection<String> getRenderIds()
    {
        return getWrapped().getRenderIds();
    }

    public abstract PartialViewContext getWrapped();

    @Override
    public boolean isAjaxRequest()
    {
        return getWrapped().isAjaxRequest();
    }

    @Override
    public boolean isExecuteAll()
    {
        return getWrapped().isExecuteAll();
    }

    @Override
    public boolean isPartialRequest()
    {
        return getWrapped().isPartialRequest();
    }

    @Override
    public boolean isRenderAll()
    {
        return getWrapped().isRenderAll();
    }

    @Override
    public void processPartial(PhaseId phaseId)
    {
        getWrapped().processPartial(phaseId);
    }

    @Override
    public void release()
    {
        getWrapped().release();
    }

    // -= Leonardo Uribe=- This method breaks signature test!
    //@Override
    //public void setPartialRequest(boolean isPartialRequest)
    //{
    //    getWrapped().setPartialRequest(isPartialRequest);
    //}

    @Override
    public void setRenderAll(boolean renderAll)
    {
        getWrapped().setRenderAll(renderAll);
    }
    
    @Override
    public boolean isResetValues()
    {
        return getWrapped().isResetValues();
    }

    @Override
    public void setPartialRequest(boolean isPartialRequest)
    {
        getWrapped().setPartialRequest(isPartialRequest);
    }
    
}
