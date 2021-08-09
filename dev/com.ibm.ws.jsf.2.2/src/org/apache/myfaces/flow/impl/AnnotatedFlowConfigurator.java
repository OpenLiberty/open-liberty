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
package org.apache.myfaces.flow.impl;

import java.util.Iterator;
import javax.faces.context.FacesContext;
import javax.faces.flow.Flow;
import org.apache.myfaces.config.FacesConfigurator;
import org.apache.myfaces.flow.FlowImpl;
import org.apache.myfaces.spi.FacesFlowProvider;
import org.apache.myfaces.spi.FacesFlowProviderFactory;

/**
 *
 * @author Leonardo Uribe
 */
public class AnnotatedFlowConfigurator
{
    
    public static void configureAnnotatedFlows(FacesContext facesContext)
    {
        FacesFlowProviderFactory factory = 
            FacesFlowProviderFactory.getFacesFlowProviderFactory(facesContext.getExternalContext());
        FacesFlowProvider provider = factory.getFacesFlowProvider(facesContext.getExternalContext());
        
        Iterator<Flow> it = provider.getAnnotatedFlows(facesContext);
        
        if (it != null)
        {
            if (it.hasNext())
            {
                FacesConfigurator.enableDefaultWindowMode(facesContext);
            }
            while (it.hasNext())
            {
                Flow flow = it.next();

                if (flow instanceof FlowImpl)
                {
                    ((FlowImpl)flow).freeze();
                }

                facesContext.getApplication().getFlowHandler().addFlow(facesContext, flow);
            }
        }
    }
}
