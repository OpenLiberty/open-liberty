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
package org.apache.myfaces.shared.renderkit.html;

import org.apache.myfaces.shared.renderkit.JSFAttr;
import org.apache.myfaces.shared.renderkit.RendererUtils;
import org.apache.myfaces.shared.renderkit.html.util.ResourceUtils;

import javax.faces.component.UIComponent;
import javax.faces.component.UISelectMany;
import javax.faces.component.UISelectOne;
import javax.faces.component.behavior.ClientBehavior;
import javax.faces.component.behavior.ClientBehaviorHolder;
import javax.faces.component.html.HtmlSelectManyListbox;
import javax.faces.component.html.HtmlSelectOneListbox;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.ConverterException;
import java.io.IOException;
import java.util.List;
import java.util.Map;


public class HtmlListboxRendererBase
        extends HtmlSelectableRendererBase
{
    public void encodeEnd(FacesContext facesContext, UIComponent uiComponent)
            throws IOException
    {
        org.apache.myfaces.shared.renderkit.RendererUtils.checkParamValidity(facesContext, uiComponent, null);
        
        Map<String, List<ClientBehavior>> behaviors = null;
        if (uiComponent instanceof ClientBehaviorHolder)
        {
            behaviors = ((ClientBehaviorHolder) uiComponent).getClientBehaviors();
            if (!behaviors.isEmpty())
            {
                ResourceUtils.renderDefaultJsfJsInlineIfNecessary(
                        facesContext, facesContext.getResponseWriter());
            }
        }

        Integer size = (Integer)uiComponent.getAttributes().get(JSFAttr.SIZE_ATTR);
        if (size == null)
        {
            size = Integer.MIN_VALUE;
        }

        if (uiComponent instanceof UISelectMany)
        {
            renderListbox(facesContext,
                                            (UISelectMany)uiComponent,
                                            isDisabled(facesContext, uiComponent),
                                            size, getConverter(facesContext, uiComponent));
        }
        else if (uiComponent instanceof HtmlSelectOneListbox)
        {
            renderListbox(facesContext,
                                            (UISelectOne)uiComponent,
                                            isDisabled(facesContext, uiComponent),
                                            size, getConverter(facesContext, uiComponent));
        }
        else
        {
            throw new IllegalArgumentException("Unsupported component class " + uiComponent.getClass().getName());
        }
    }
    
    protected void renderListbox(FacesContext facesContext,
            UISelectOne selectOne, boolean disabled, int size,
            Converter converter) throws IOException
    {
        internalRenderSelect(facesContext, selectOne, disabled, size, false,
                converter);
    }

    protected void renderListbox(FacesContext facesContext,
            UISelectMany selectMany, boolean disabled, int size,
            Converter converter) throws IOException
    {
        internalRenderSelect(facesContext, selectMany, disabled, size, true,
                converter);
    }

    protected boolean isDisabled(FacesContext facesContext, UIComponent uiComponent)
    {
        //TODO: overwrite in extended HtmlListboxRenderer and check for enabledOnUserRole
        if (uiComponent instanceof HtmlSelectManyListbox)
        {
            return ((HtmlSelectManyListbox)uiComponent).isDisabled();
        }
        else if (uiComponent instanceof HtmlSelectOneListbox)
        {
            return ((HtmlSelectOneListbox)uiComponent).isDisabled();
        }
        else
        {
            return org.apache.myfaces.shared.renderkit.RendererUtils.getBooleanAttribute(uiComponent, 
                    org.apache.myfaces.shared.renderkit.html.HTML.DISABLED_ATTR, false);
        }
    }


    public void decode(FacesContext facesContext, UIComponent uiComponent)
    {
        RendererUtils.checkParamValidity(facesContext, uiComponent, null);

        if (uiComponent instanceof UISelectMany)
        {
            HtmlRendererUtils.decodeUISelectMany(facesContext, uiComponent);
        }
        else if (uiComponent instanceof UISelectOne)
        {
            HtmlRendererUtils.decodeUISelectOne(facesContext, uiComponent);
        }
        else
        {
            throw new IllegalArgumentException("Unsupported component class " + uiComponent.getClass().getName());
        }
        if (uiComponent instanceof ClientBehaviorHolder &&
                !HtmlRendererUtils.isDisabled(uiComponent))
        {
            HtmlRendererUtils.decodeClientBehaviors(facesContext, uiComponent);
        }
    }

    public Object getConvertedValue(FacesContext facesContext, UIComponent uiComponent, Object submittedValue)
         throws ConverterException
    {
        org.apache.myfaces.shared.renderkit.RendererUtils.checkParamValidity(facesContext, uiComponent, null);

        if (uiComponent instanceof UISelectMany)
        {
            return org.apache.myfaces.shared.renderkit.RendererUtils.getConvertedUISelectManyValue(facesContext,
                                                               (UISelectMany)uiComponent,
                                                               submittedValue);
        }
        else if (uiComponent instanceof UISelectOne)
        {
            return RendererUtils.getConvertedUISelectOneValue(facesContext,
                                                           (UISelectOne)uiComponent,
                                                           submittedValue);
        }
        else
        {
            throw new IllegalArgumentException("Unsupported component class " + uiComponent.getClass().getName());
        }
    }
    
    /**
     * Gets the converter for the given component rendered by this renderer.
     * @param facesContext
     * @param component
     * @return
     */
    protected Converter getConverter(FacesContext facesContext,
            UIComponent component)
    {
        if (component instanceof UISelectMany)
        {
            return HtmlRendererUtils.findUISelectManyConverterFailsafe(facesContext, 
                    (UISelectMany) component);
        }
        else if (component instanceof UISelectOne)
        {
            return HtmlRendererUtils.findUIOutputConverterFailSafe(facesContext, component);
        }
        return null;
    }

}
