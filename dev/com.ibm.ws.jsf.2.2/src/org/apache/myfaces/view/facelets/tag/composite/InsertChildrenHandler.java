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
package org.apache.myfaces.view.facelets.tag.composite;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.faces.component.UIComponent;
import javax.faces.view.facelets.FaceletContext;
import javax.faces.view.facelets.TagConfig;
import javax.faces.view.facelets.TagHandler;

import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFFaceletTag;
import org.apache.myfaces.view.facelets.AbstractFaceletContext;
import org.apache.myfaces.view.facelets.FaceletCompositionContext;
import org.apache.myfaces.view.facelets.tag.ComponentContainerHandler;

/**
 * @author Leonardo Uribe (latest modification by $Author: lu4242 $)
 * @version $Revision: 1306699 $ $Date: 2012-03-29 03:14:59 +0000 (Thu, 29 Mar 2012) $
 */
@JSFFaceletTag(name="composite:insertChildren")
public class InsertChildrenHandler extends TagHandler implements ComponentContainerHandler
{
    public static final String INSERT_CHILDREN_USED = "org.apache.myfaces.INSERT_CHILDREN_USED";
    
    private static final Logger log = Logger.getLogger(InsertChildrenHandler.class.getName());

    public InsertChildrenHandler(TagConfig config)
    {
        super(config);
    }

    public void apply(FaceletContext ctx, UIComponent parent)
            throws IOException
    {
        UIComponent parentCompositeComponent
                = FaceletCompositionContext.getCurrentInstance(ctx).getCompositeComponentFromStack();
        
        AbstractFaceletContext actx = (AbstractFaceletContext) ctx;

        if (actx.isBuildingCompositeComponentMetadata())
        {
            CompositeComponentBeanInfo beanInfo = 
                (CompositeComponentBeanInfo) parentCompositeComponent.getAttributes()
                .get(UIComponent.BEANINFO_KEY);
            
            if (beanInfo == null)
            {
                if (log.isLoggable(Level.SEVERE))
                {
                    log.severe("Cannot find composite bean descriptor UIComponent.BEANINFO_KEY ");
                }
                return;
            }
            
            beanInfo.getBeanDescriptor().setValue(INSERT_CHILDREN_USED, Boolean.TRUE);
        }
        else
        {
            actx.includeCompositeComponentDefinition(parent, null);
        }
    }

}
