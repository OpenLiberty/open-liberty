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
package org.apache.myfaces.renderkit.html;

import java.io.IOException;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;

import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFRenderer;
import org.apache.myfaces.shared.renderkit.html.HtmlFormRendererBase;


/**
 *   
 * @author Manfred Geiler (latest modification by $Author: lu4242 $)
 * @author Thomas Spiegl
 * @author Anton Koinov
 * @version $Revision: 1542428 $ $Date: 2013-11-15 23:06:36 +0000 (Fri, 15 Nov 2013) $
 */
@JSFRenderer(
    renderKitId="HTML_BASIC",
    family="javax.faces.Form",
    type="javax.faces.Form")
public class HtmlFormRenderer
        extends HtmlFormRendererBase
{    
    //private static final Log log = LogFactory.getLog(HtmlFormRenderer.class);
    
    @Override
    protected void afterFormElementsEnd(FacesContext facesContext, UIComponent component) throws IOException
    {
        super.afterFormElementsEnd(facesContext, component);
    }
    
    @Override
    protected boolean isCommonPropertiesOptimizationEnabled(FacesContext facesContext)
    {
        return true;
    }

    @Override
    protected boolean isCommonEventsOptimizationEnabled(FacesContext facesContext)
    {
        return true;
    }

}
