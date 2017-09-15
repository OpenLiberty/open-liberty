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

import java.io.OutputStream;
import java.io.Writer;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.faces.context.ResponseStream;
import javax.faces.context.ResponseWriter;

/**
 * see Javadoc of <a href="http://java.sun.com/javaee/javaserverfaces/1.2/docs/api/index.html">JSF Specification</a>
 */
public abstract class RenderKit
{
    private HashMap<String, ClientBehaviorRenderer> clientBehaviorRenderers;
    
    public RenderKit ()
    {
        this.clientBehaviorRenderers = new HashMap<String, ClientBehaviorRenderer>();
    }
    
    public void addClientBehaviorRenderer(String type, ClientBehaviorRenderer renderer)
    {
        if (type == null)
        {
            throw new NullPointerException ("type is null");
        }
        
        if (renderer == null)
        {
            throw new NullPointerException ("renderer is null");
        }
        
        this.clientBehaviorRenderers.put (type, renderer);
    }

    public abstract void addRenderer(String family, String rendererType, Renderer renderer);

    public abstract ResponseStream createResponseStream(OutputStream out);

    public abstract ResponseWriter createResponseWriter(Writer writer, String contentTypeList,
                                                        String characterEncoding);
    
    public ClientBehaviorRenderer getClientBehaviorRenderer(String type)
    {
        if (type == null)
        {
            throw new NullPointerException ("type is null");
        }
        
        return this.clientBehaviorRenderers.get (type);
    }
    
    public Iterator<String> getClientBehaviorRendererTypes()
    {
        return this.clientBehaviorRenderers.keySet().iterator();
    }

    /**
     * <p>
     * Return an <code>Iterator</code> over the component-family entries supported by this <code>RenderKit</code>
     * instance.
     * </p>
     * 
     * <p>
     * The default implementation of this method returns an empty <code>Iterator</code>
     * </p>
     * 
     * @return an iterator over the component families supported by this <code>RenderKit</code>.
     * 
     * @since 2.0
     */
    public Iterator<String> getComponentFamilies()
    {
        List<String> emptyList = Collections.emptyList();

        return emptyList.iterator();
    }

    public abstract Renderer getRenderer(String family, String rendererType);

    /**
     * <p>
     * Return an <code>Iterator</code> over the renderer-type entries for the given component-family.
     * </p>
     * 
     * <p>
     * If the specified <code>componentFamily</code> is not known to this <code>RenderKit</code> implementation, return
     * an empty <code>Iterator</code>
     * </p>
     * 
     * <p>
     * The default implementation of this method returns an empty <code>Iterator</code>
     * </p>
     * 
     * @param componentFamily
     *            one of the members of the <code>Iterator</code> returned by {@link #getComponentFamilies()}
     * 
     * @return an iterator over the renderer-type entries for the given component-family.
     * 
     * @since 2.0
     */
    public Iterator<String> getRendererTypes(String componentFamily)
    {
        List<String> emptyList = Collections.emptyList();

        return emptyList.iterator();
    }

    public abstract ResponseStateManager getResponseStateManager();
}
