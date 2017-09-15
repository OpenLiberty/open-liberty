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
package org.apache.myfaces.view.facelets.el;

import javax.el.ELContext;
import javax.faces.context.FacesContext;

import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFWebConfigParam;
import org.apache.myfaces.shared.util.WebConfigParamUtils;

public class ContextAwareUtils
{
    /**
     * Wrap exception caused by calls to EL expressions, so information like
     * the location, expression string and tag name can be retrieved by
     * the ExceptionHandler implementation and used to output meaningful information about itself.
     * 
     * <p>Note in some cases this will wrap the original javax.el.ELException, so the
     * information will not be on the stack trace unless ExceptionHandler
     * retrieve checking if the exception implements ContextAware interface and calling getWrapped() method.
     * </p>
     * 
     */
    @JSFWebConfigParam(since="2.0.9, 2.1.3" , defaultValue="true", expectedValues="true, false")
    public static final String INIT_PARAM_WRAP_TAG_EXCEPTIONS_AS_CONTEXT_AWARE
            = "org.apache.myfaces.WRAP_TAG_EXCEPTIONS_AS_CONTEXT_AWARE";
    
    public static boolean isWrapTagExceptionsAsContextAware(ELContext context)
    {
        FacesContext facesContext = (FacesContext) context.getContext(FacesContext.class);
        facesContext = facesContext == null ? FacesContext.getCurrentInstance() : facesContext;
        if (facesContext != null)
        {
            return WebConfigParamUtils.getBooleanInitParameter(facesContext.getExternalContext(),
                    INIT_PARAM_WRAP_TAG_EXCEPTIONS_AS_CONTEXT_AWARE, true);
        }
        else
        {
            //No facesContext, return false
            return false;
        }
    }
}
