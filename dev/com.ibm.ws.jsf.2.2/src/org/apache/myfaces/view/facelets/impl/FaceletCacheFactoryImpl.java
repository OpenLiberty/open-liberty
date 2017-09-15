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
package org.apache.myfaces.view.facelets.impl;

import javax.faces.application.ProjectStage;
import javax.faces.application.ViewHandler;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.view.facelets.FaceletCache;
import javax.faces.view.facelets.FaceletCacheFactory;

import org.apache.myfaces.shared.util.WebConfigParamUtils;
import org.apache.myfaces.view.facelets.ELExpressionCacheMode;
import org.apache.myfaces.view.facelets.FaceletViewDeclarationLanguage;

/**
 * 
 * @author Leonardo Uribe
 * @since 2.1.0
 *
 */
public class FaceletCacheFactoryImpl extends FaceletCacheFactory
{
    private final static String PARAM_REFRESH_PERIOD_DEPRECATED = "facelets.REFRESH_PERIOD";
    
    private final static String[] PARAMS_REFRESH_PERIOD
            = {ViewHandler.FACELETS_REFRESH_PERIOD_PARAM_NAME, PARAM_REFRESH_PERIOD_DEPRECATED};


    @Override
    public FaceletCache getFaceletCache()
    {
        FacesContext context = FacesContext.getCurrentInstance();
        ExternalContext eContext = context.getExternalContext();
        // refresh period
        long refreshPeriod;
        if(context.isProjectStage(ProjectStage.Production))
        {
            refreshPeriod = WebConfigParamUtils.getLongInitParameter(eContext, PARAMS_REFRESH_PERIOD,
                    FaceletViewDeclarationLanguage.DEFAULT_REFRESH_PERIOD_PRODUCTION);
        }
        else
        {
            refreshPeriod = WebConfigParamUtils.getLongInitParameter(eContext, PARAMS_REFRESH_PERIOD,
                    FaceletViewDeclarationLanguage.DEFAULT_REFRESH_PERIOD);
        }
        
        String elMode = WebConfigParamUtils.getStringInitParameter(
                    context.getExternalContext(),
                    FaceletCompositionContextImpl.INIT_PARAM_CACHE_EL_EXPRESSIONS, 
                        ELExpressionCacheMode.noCache.name());
        
        if (ELExpressionCacheMode.alwaysRecompile.toString().equals(elMode))
        {
            return new CacheELFaceletCacheImpl(refreshPeriod);
        }
        else
        {
            return new FaceletCacheImpl(refreshPeriod);
        }
    }

}
