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
package javax.faces.view.facelets;

import java.io.IOException;

import javax.faces.FactoryFinder;
import javax.faces.component.UIComponent;

/**
 * @since 2.0
 */
public abstract class DelegatingMetaTagHandler extends MetaTagHandler
{
    protected TagHandlerDelegateFactory delegateFactory;
    private TagAttribute binding;
    private TagAttribute disabled;
    
    public DelegatingMetaTagHandler(TagConfig config)
    {
        super(config);
        
        delegateFactory = (TagHandlerDelegateFactory)
            FactoryFinder.getFactory (FactoryFinder.TAG_HANDLER_DELEGATE_FACTORY);
        binding = getAttribute ("binding");
        disabled = getAttribute ("disabled");
    }

    /**
     * {@inheritDoc}
     */
    public void apply(FaceletContext ctx, UIComponent parent) throws IOException
    {
        getTagHandlerDelegate().apply(ctx, parent);
    }

    public void applyNextHandler(FaceletContext ctx, UIComponent c) throws IOException
    {
        nextHandler.apply (ctx, c);
    }

    public TagAttribute getBinding()
    {
        return binding;
    }

    public Tag getTag()
    {
        return tag;
    }

    public TagAttribute getTagAttribute(String localName)
    {
        return super.getAttribute (localName);
    }

    public String getTagId()
    {
        return tagId;
    }

    public boolean isDisabled(FaceletContext ctx)
    {
        if (disabled == null)
        {
            return false;
        }
        
        return disabled.getBoolean (ctx);
    }

    public void setAttributes(FaceletContext ctx, Object instance)
    {
        super.setAttributes (ctx, instance);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected MetaRuleset createMetaRuleset(Class type)
    {
        return getTagHandlerDelegate().createMetaRuleset(type);
    }

    protected abstract TagHandlerDelegate getTagHandlerDelegate();

}
