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
package org.apache.myfaces.view.facelets.compiler;

import java.io.IOException;
import javax.faces.component.UIComponent;
import javax.faces.view.facelets.FaceletContext;
import javax.faces.view.facelets.FaceletHandler;
import org.apache.myfaces.view.facelets.FaceletCompositionContext;

/**
 *
 * @author lu4242
 */
public class DynamicComponentFacelet implements FaceletHandler
{
    //public static final String CREATE_CC_ON_POST_ADD_TO_VIEW = "oam.facelet.cc.CREATE_CC_ON_POST_ADD_TO_VIEW";
    
    private NamespaceHandler next;

    public DynamicComponentFacelet(NamespaceHandler next)
    {
        this.next = next;
    }

    public void apply(FaceletContext ctx, UIComponent parent) throws IOException
    {
        /*
        if (isNextHandlerCompositeComponent())
        {
            ctx.getFacesContext().getAttributes().put(CREATE_CC_ON_POST_ADD_TO_VIEW, Boolean.TRUE);
        }*/
        FaceletCompositionContext fcc = FaceletCompositionContext.getCurrentInstance(ctx);
        boolean nextHandlerCompositeComponent = isNextHandlerCompositeComponent();
        try
        {
            if (nextHandlerCompositeComponent)
            {
                fcc.setDynamicCompositeComponentHandler(true);
            }
            next.apply(ctx, parent);
        }
        finally
        {
            if (nextHandlerCompositeComponent)
            {
                fcc.setDynamicCompositeComponentHandler(false);
            }
        }
    }
    
    public boolean isNextHandlerComponent()
    {
        return next.isNextHandlerComponent();
    }
    
    public boolean isNextHandlerCompositeComponent()
    {
        return next.isNextHandlerCompositeComponent();
    }    
}
