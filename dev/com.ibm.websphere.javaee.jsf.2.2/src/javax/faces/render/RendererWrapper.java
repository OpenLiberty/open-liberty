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

import java.io.IOException;
import javax.faces.FacesWrapper;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.ConverterException;

/**
 *
 * @since 2.2
 */
public abstract class RendererWrapper extends Renderer implements FacesWrapper<Renderer>
{

    public void decode(FacesContext context, UIComponent component)
    {
        getWrapped().decode(context, component);
    }

    public void encodeBegin(FacesContext context, UIComponent component) throws IOException
    {
        getWrapped().encodeBegin(context, component);
    }

    public void encodeChildren(FacesContext context, UIComponent component) throws IOException
    {
        getWrapped().encodeChildren(context, component);
    }

    public void encodeEnd(FacesContext context, UIComponent component) throws IOException
    {
        getWrapped().encodeEnd(context, component);
    }

    public String convertClientId(FacesContext context, String clientId)
    {
        return getWrapped().convertClientId(context, clientId);
    }

    public boolean getRendersChildren()
    {
        return getWrapped().getRendersChildren();
    }

    public Object getConvertedValue(FacesContext context, UIComponent component, 
        Object submittedValue) throws ConverterException
    {
        return getWrapped().getConvertedValue(context, component, submittedValue);
    }
    
    public abstract Renderer getWrapped();
}
