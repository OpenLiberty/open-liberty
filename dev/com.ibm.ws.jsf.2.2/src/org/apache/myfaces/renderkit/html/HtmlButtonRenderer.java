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

import javax.faces.context.FacesContext;

import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFRenderer;
import org.apache.myfaces.shared.renderkit.html.HtmlButtonRendererBase;


/**
 * 
 * @author Manfred Geiler (latest modification by $Author: lu4242 $)
 * @version $Revision: 1230371 $ $Date: 2012-01-12 02:09:45 +0000 (Thu, 12 Jan 2012) $
 */
@JSFRenderer(
    renderKitId="HTML_BASIC",
    family="javax.faces.Command",
    type="javax.faces.Button")
public class HtmlButtonRenderer
    extends HtmlButtonRendererBase
{
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
