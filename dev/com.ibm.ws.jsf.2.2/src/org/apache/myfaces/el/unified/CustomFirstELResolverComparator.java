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
package org.apache.myfaces.el.unified;

import java.util.Comparator;
import java.util.List;

import javax.el.ELResolver;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;

import org.apache.myfaces.config.RuntimeConfig;

/**
 * Comparator for ELResolvers that shifts the Resolvers from
 * the faces-config to the front.
 * 
 * @author Jakob Korherr (latest modification by $Author: jakobk $)
 * @version $Revision: 986157 $ $Date: 2010-08-16 23:07:17 +0000 (Mon, 16 Aug 2010) $
 *
 * @since 1.2.10, 2.0.2
 */
public class CustomFirstELResolverComparator implements Comparator<ELResolver>
{
    
    private List<ELResolver> _facesConfigResolvers;
    
    public int compare(ELResolver r1, ELResolver r2)
    {
        List<ELResolver> facesConfigResolvers = _getFacesConfigElResolvers();
        
        if (facesConfigResolvers == null)
        {
            // no el-resolvers in faces-config
            return 0; // keep order
        }
        
        boolean r1FromFacesConfig = facesConfigResolvers.contains(r1);
        boolean r2FromFacesConfig = facesConfigResolvers.contains(r2);
        
        if (r1FromFacesConfig)
        {
            if (r2FromFacesConfig)
            {
                // both are from faces-config
                return 0; // keep order
            }
            else
            {
                // only r1 is from faces-config
                return -1;
            }
        }
        else
        {
            if (r2FromFacesConfig)
            {
                // only r2 is from faces-config
                return 1;
            }
            else
            {
                // neither r1 nor r2 are from faces-config
                return 0; // keep order
            }
        }
    }
    
    /**
     * Returns a List of all ELResolvers from the faces-config.
     * @return
     */
    private List<ELResolver> _getFacesConfigElResolvers()
    {
        if (_facesConfigResolvers == null)
        {
            ExternalContext externalContext
                    = FacesContext.getCurrentInstance().getExternalContext();
            RuntimeConfig runtimeConfig
                    = RuntimeConfig.getCurrentInstance(externalContext);
            _facesConfigResolvers 
                    = runtimeConfig.getFacesConfigElResolvers();
        }
        
        return _facesConfigResolvers;
    }
    
}
