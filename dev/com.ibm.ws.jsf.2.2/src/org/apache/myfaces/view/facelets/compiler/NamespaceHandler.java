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
import java.lang.reflect.Method;
import java.util.Map;

import javax.el.ELException;
import javax.el.FunctionMapper;
import javax.faces.FacesException;
import javax.faces.component.UIComponent;
import javax.faces.view.facelets.ComponentHandler;
import javax.faces.view.facelets.FaceletContext;
import javax.faces.view.facelets.FaceletException;
import javax.faces.view.facelets.FaceletHandler;

import org.apache.myfaces.view.facelets.el.CompositeFunctionMapper;
import org.apache.myfaces.view.facelets.tag.TagLibrary;
import org.apache.myfaces.view.facelets.tag.composite.CompositeComponentResourceTagHandler;

final class NamespaceHandler extends FunctionMapper implements FaceletHandler
{

    private final TagLibrary library;
    private final Map<String, String> ns;
    private FaceletHandler next;

    public NamespaceHandler(FaceletHandler next, TagLibrary library, Map<String, String> ns)
    {
        this.library = library;
        this.ns = ns;
        this.next = next;
    }

    public void apply(FaceletContext ctx, UIComponent parent) throws IOException, FacesException, FaceletException,
            ELException
    {
        FunctionMapper orig = ctx.getFunctionMapper();
        ctx.setFunctionMapper(new CompositeFunctionMapper(this, orig));
        try
        {
            next.apply(ctx, parent);
        }
        finally
        {
            ctx.setFunctionMapper(orig);
        }
    }

    public Method resolveFunction(String prefix, String localName)
    {
        String uri = (String) this.ns.get(prefix);
        if (uri != null)
        {
            return this.library.createFunction(uri, localName);
        }
        return null;
    }

    public boolean isNextHandlerComponent()
    {
        return (next instanceof ComponentHandler);
    }
    
    public boolean isNextHandlerCompositeComponent()
    {
        return (next instanceof CompositeComponentResourceTagHandler);
    }
}
