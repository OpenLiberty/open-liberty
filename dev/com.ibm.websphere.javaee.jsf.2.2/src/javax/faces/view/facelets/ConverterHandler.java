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

import javax.faces.view.ValueHolderAttachedObjectHandler;

import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFFaceletTag;

/**
 * Handles setting a Converter instance on a ValueHolder. Will wire all attributes set to the Converter instance
 * created/fetched. Uses the "binding" attribute for grabbing instances to apply attributes to. <p/> Will only
 * set/create Converter is the passed UIComponent's parent is null, signifying that it wasn't restored from an existing
 * tree.
 * 
 * @see javax.faces.webapp.ConverterELTag
 * @see javax.faces.convert.Converter
 * @see javax.faces.component.ValueHolder
 */
@JSFFaceletTag
public class ConverterHandler extends FaceletsAttachedObjectHandler implements ValueHolderAttachedObjectHandler
{
    private String converterId;
    private TagHandlerDelegate helper;
    
    public ConverterHandler(ConverterConfig config)
    {
        super(config);
        
        converterId = config.getConverterId();
    }
    
    public String getConverterId (FaceletContext ctx)
    {
        return converterId;
    }
    
    protected TagHandlerDelegate getTagHandlerDelegate()
    {
        if (helper == null)
        {
            // Spec seems to indicate that the helper is created here, as opposed to other Handler
            // instances, where it's presumably a new instance for every getter call.
            
            this.helper = delegateFactory.createConverterHandlerDelegate (this);
        }
        return helper;
    }
}
