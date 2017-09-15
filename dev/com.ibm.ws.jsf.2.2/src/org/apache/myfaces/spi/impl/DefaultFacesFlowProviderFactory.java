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
package org.apache.myfaces.spi.impl;

import org.apache.myfaces.spi.FacesFlowProvider;
import org.apache.myfaces.spi.FacesFlowProviderFactory;
import javax.faces.context.ExternalContext;
import org.apache.myfaces.flow.impl.DefaultFacesFlowProvider;
import org.apache.myfaces.shared.util.ClassUtils;
import org.apache.myfaces.util.ExternalSpecifications;

/**
 * @author Leonardo Uribe
 */
public class DefaultFacesFlowProviderFactory extends FacesFlowProviderFactory
{

    public static final String FACES_CONFIGURATION_MERGER = FacesFlowProvider.class.getName();
    public static final String FACES_CONFIGURATION_MERGER_INSTANCE_KEY = FACES_CONFIGURATION_MERGER + ".INSTANCE";

    @Override
    public FacesFlowProvider getFacesFlowProvider(ExternalContext externalContext)
    {
        // check for cached instance
        FacesFlowProvider returnValue = (FacesFlowProvider)
                externalContext.getApplicationMap().get(FACES_CONFIGURATION_MERGER_INSTANCE_KEY);

        if (returnValue == null)
        {
            if (ExternalSpecifications.isCDIAvailable(externalContext))
            {
                returnValue = (FacesFlowProvider) ClassUtils.newInstance(
                    "org.apache.myfaces.flow.cdi.DefaultCDIFacesFlowProvider");
            }
            else
            {
                returnValue = (FacesFlowProvider) new DefaultFacesFlowProvider();
            }
        }


        return returnValue;
    }

}
