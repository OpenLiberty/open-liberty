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
package javax.faces.component.behavior;

import java.util.Collections;
import java.util.Set;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.render.ClientBehaviorRenderer;

/**
 * @since 2.0
 */
public class ClientBehaviorBase extends BehaviorBase implements ClientBehavior
{

    private transient FacesContext _facesContext;
    
    /**
     * 
     */
    public ClientBehaviorBase()
    {
    }

    /**
     * {@inheritDoc}
     */
    public void decode(FacesContext context, UIComponent component)
    {
        if (context == null)
        {
            throw new NullPointerException("context");
        }
        
        if (component == null)
        {
            throw new NullPointerException("component");
        }
        
        // If a BehaviorRenderer is available for the specified behavior renderer type, this method delegates 
        // to the BehaviorRenderer's decode() method. Otherwise, no decoding is performed. 
        ClientBehaviorRenderer renderer = getRenderer(context);
        if (renderer != null)
        {
            renderer.decode(context, component, this);
        }
    }

    /**
     * {@inheritDoc}
     */
     public Set<ClientBehaviorHint> getHints()
    {
        return Collections.<ClientBehaviorHint>emptySet();
    }

    /**
     * {@inheritDoc}
     */
    public String getRendererType()
    {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public String getScript(ClientBehaviorContext behaviorContext)
    {
        if (behaviorContext == null)
        {
            throw new NullPointerException("behaviorContext");
        }
        
        ClientBehaviorRenderer renderer = getRenderer(behaviorContext.getFacesContext());
        if (renderer != null)
        {
            // If a BehaviorRenderer is available for the specified behavior renderer type, this method delegates 
            // to the BehaviorRenderer.getScript method.
            try
            {
                setCachedFacesContext(behaviorContext.getFacesContext());
                return renderer.getScript(behaviorContext, this);
            }
            finally
            {
                setCachedFacesContext(null);
            }
        }
        
        // Otherwise, this method returns null.
        return null;
    }
    
    protected ClientBehaviorRenderer getRenderer(FacesContext context)
    {
        if (context == null)
        {
            throw new NullPointerException("context");
        }
        
        String rendererType = getRendererType();
        if (rendererType != null)
        {
            return context.getRenderKit().getClientBehaviorRenderer(rendererType);
        }
        
        return null;
    }
    
    FacesContext getFacesContext()
    {
        if (_facesContext == null)
        {
            return FacesContext.getCurrentInstance();
        }
        else
        {
            return _facesContext;
        }
    }
    
    boolean isCachedFacesContext()
    {
        return _facesContext != null;
    }
    
    void setCachedFacesContext(FacesContext facesContext)
    {
        _facesContext = facesContext;
    }
}
