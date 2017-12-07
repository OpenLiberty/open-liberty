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
package org.apache.myfaces.flow.util;

import java.util.List;
import javax.faces.context.FacesContext;
import javax.faces.flow.Flow;
import org.apache.myfaces.flow.FlowHandlerImpl;
import org.apache.myfaces.flow.FlowReference;

/**
 *
 */
public class FlowUtils
{
    public static String getFlowMapKey(FacesContext facesContext, FlowReference flowReference)
    {
        Flow flow = null;
        if (flowReference.getDocumentId() == null)
        {
            flow = facesContext.getApplication().getFlowHandler().getFlow(
                facesContext, "", flowReference.getId());
        }
        else
        {
            flow = facesContext.getApplication().getFlowHandler().getFlow(
                facesContext, flowReference.getDocumentId(), flowReference.getId());
        }
        if (flow != null)
        {
            return FlowUtils.getFlowMapKey(facesContext, flow);
        }
        return null;
    }    
    
    public static String getFlowMapKey(FacesContext facesContext, Flow flow)
    {
        String flowMapKey = flow.getClientWindowFlowId(
            facesContext.getExternalContext().getClientWindow());
        int flowIndex = getFlowIndex(facesContext, flow);
        if (flowIndex > 0)
        {
            flowMapKey = flowMapKey + "_" + flowIndex;
        }
        return flowMapKey;
    }
    
    private static int getFlowIndex(FacesContext facesContext, Flow flow)
    {
        List<Flow> list = FlowHandlerImpl.getActiveFlows(facesContext, 
                facesContext.getApplication().getFlowHandler());
        FlowReference flowRef = new FlowReference(flow.getDefiningDocumentId(), flow.getId());
        int flowIndex = 0;
        for (Flow f : list)
        {
            FlowReference fr = new FlowReference(f.getDefiningDocumentId(), f.getId());
            if (flowRef.equals(fr))
            {
                flowIndex++;
            }
        }
        return flowIndex;
    }

}
