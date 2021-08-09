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
package javax.faces.render;

import javax.faces.FacesWrapper;
import javax.faces.context.ResponseStream;
import javax.faces.context.ResponseWriter;
import java.io.OutputStream;
import java.io.Writer;
import java.util.Iterator;

/**
 * <p>
 *   Provides a simple implementation of RenderKit that can be subclassed by developers wishing 
 *   to provide specialized behavior to an existing RenderKit instance. The default implementation
 *   of all methods is to call through to the wrapped RenderKit.
 * </p>
 * <p>
 *   Usage: extend this class and override getWrapped() to return the  wrapped instance.
 * </p>
 *
 * @since 2.0
 */
public abstract class RenderKitWrapper extends RenderKit implements FacesWrapper<RenderKit>
{
    @Override
    public void addClientBehaviorRenderer(String type, ClientBehaviorRenderer renderer)
    {
        getWrapped().addClientBehaviorRenderer(type,renderer);
    }
    
    /** {@inheritDoc} */
    @Override
    public void addRenderer(String family, String rendererType, Renderer renderer)
    {
        getWrapped().addRenderer(family, rendererType, renderer);
    }

    /** {@inheritDoc} */
    @Override
    public ResponseStream createResponseStream(OutputStream out)
    {
        return getWrapped().createResponseStream(out);
    }

    /** {@inheritDoc} */
    @Override
    public ResponseWriter createResponseWriter(Writer writer, String contentTypeList, String characterEncoding)
    {
        return getWrapped().createResponseWriter(writer, contentTypeList, characterEncoding);
    }

    @Override
    public ClientBehaviorRenderer getClientBehaviorRenderer(String type)
    {
        return getWrapped().getClientBehaviorRenderer(type);
    }
    
    @Override
    public Iterator<String> getClientBehaviorRendererTypes()
    {
        return getWrapped().getClientBehaviorRendererTypes();
    }
    
    /** {@inheritDoc} */
    @Override
    public Renderer getRenderer(String family, String rendererType)
    {
        return getWrapped().getRenderer(family, rendererType);
    }

    /** {@inheritDoc} */
    @Override
    public ResponseStateManager getResponseStateManager()
    {
        return getWrapped().getResponseStateManager();
    }

    /** {@inheritDoc} */
    public abstract RenderKit getWrapped();

    /** {@inheritDoc} */
    @Override
    public Iterator<String> getComponentFamilies()
    {
        return getWrapped().getComponentFamilies();
    }

    /** {@inheritDoc} */
    @Override
    public Iterator<String> getRendererTypes(String componentFamily)
    {
        return getWrapped().getRendererTypes(componentFamily);
    }
}
