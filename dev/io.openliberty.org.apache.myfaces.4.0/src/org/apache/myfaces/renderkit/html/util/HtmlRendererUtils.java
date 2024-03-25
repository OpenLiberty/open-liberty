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
package org.apache.myfaces.renderkit.html.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.faces.FacesException;
import jakarta.faces.component.Doctype;
import jakarta.faces.component.EditableValueHolder;
import jakarta.faces.component.UIComponent;
import jakarta.faces.component.UIForm;
import jakarta.faces.component.UIInput;
import jakarta.faces.component.UIOutcomeTarget;
import jakarta.faces.component.UIOutput;
import jakarta.faces.component.UIParameter;
import jakarta.faces.component.UISelectBoolean;
import jakarta.faces.component.UISelectMany;
import jakarta.faces.component.UISelectOne;
import jakarta.faces.component.UIViewRoot;
import jakarta.faces.component.ValueHolder;
import jakarta.faces.component.behavior.ClientBehavior;
import jakarta.faces.component.behavior.ClientBehaviorContext;
import jakarta.faces.component.html.HtmlDataTable;
import jakarta.faces.component.html.HtmlMessages;
import jakarta.faces.component.html.HtmlPanelGrid;
import jakarta.faces.component.visit.VisitCallback;
import jakarta.faces.component.visit.VisitContext;
import jakarta.faces.component.visit.VisitResult;
import jakarta.faces.context.FacesContext;
import jakarta.faces.context.PartialViewContext;
import jakarta.faces.context.ResponseWriter;
import jakarta.faces.convert.Converter;
import jakarta.faces.model.SelectItem;
import jakarta.faces.model.SelectItemGroup;

import org.apache.myfaces.renderkit.ClientBehaviorEvents;
import org.apache.myfaces.renderkit.RendererUtils;
import org.apache.myfaces.component.visit.MyFacesVisitHints;
import org.apache.myfaces.core.api.shared.ComponentUtils;
import org.apache.myfaces.core.api.shared.SharedRendererUtils;

public final class HtmlRendererUtils
{
    private static final Logger log = Logger.getLogger(HtmlRendererUtils.class.getName());

    private static final char TABULATOR = '\t';
    public static final String HIDDEN_COMMANDLINK_FIELD_NAME = "_idcl";
    public static final String CLEAR_HIDDEN_FIELD_FN_NAME = "clearFormHiddenParams";
    public static final String SUBMIT_FORM_FN_NAME_JSF2 = "myfaces.oam.submitForm";
    public static final String NON_SUBMITTED_VALUE_WARNING 
            = "There should always be a submitted value for an input if it is rendered,"
            + " its form is submitted, and it was not originally rendered disabled or read-only."
            + "  You cannot submit a form after disabling an input element via javascript."
            + "  Consider setting read-only to true instead"
            + " or resetting the disabled value back to false prior to form submission.";

    private HtmlRendererUtils()
    {
        // utility class, do not instantiate
    }

    /**
     * Utility to set the submitted value of the provided component from the
     * data in the current request object.
     * <p>
     * Param component is required to be an EditableValueHolder. On return
     * from this method, the component's submittedValue property will be
     * set if the submitted form contained that component.</p>
     */
    public static void decodeUIInput(FacesContext facesContext, UIComponent component)
    {
        if (!(component instanceof EditableValueHolder))
        {
            throw new IllegalArgumentException("Component "
                    + component.getClientId(facesContext)
                    + " is not an EditableValueHolder");
        }
        Map paramMap = facesContext.getExternalContext().getRequestParameterMap();
        String clientId = component.getClientId(facesContext);
        if (isDisabledOrReadOnly(component))
        {
            return;
        }
        if (paramMap.containsKey(clientId))
        {
            ((EditableValueHolder) component).setSubmittedValue(paramMap
                    .get(clientId));
        }
        else
        {
            log.warning(NON_SUBMITTED_VALUE_WARNING + " Component : " + ComponentUtils.getPathToComponent(component));
        }
    }

    /**
     * X-CHECKED: tlddoc h:selectBooleanCheckbox
     *
     * @param facesContext
     * @param component
     */
    public static void decodeUISelectBoolean(FacesContext facesContext, UIComponent component)
    {
        if (!(component instanceof EditableValueHolder))
        {
            throw new IllegalArgumentException("Component "
                    + component.getClientId(facesContext)
                    + " is not an EditableValueHolder");
        }
        if (isDisabledOrReadOnly(component))
        {
            return;
        }
        Map paramMap = facesContext.getExternalContext().getRequestParameterMap();
        String clientId = component.getClientId(facesContext);
        if (paramMap.containsKey(clientId))
        {
            String reqValue = (String) paramMap.get(clientId);
            if ((reqValue.equalsIgnoreCase("on")
                    || reqValue.equalsIgnoreCase("yes")
                    || reqValue.equalsIgnoreCase("true")))
            {
                ((EditableValueHolder) component).setSubmittedValue(Boolean.TRUE);
            }
            else
            {
                ((EditableValueHolder) component).setSubmittedValue(Boolean.FALSE);
            }
        }
        else
        {
            ((EditableValueHolder) component).setSubmittedValue(Boolean.FALSE);
        }
    }

    public static boolean isDisabledOrReadOnly(UIComponent component)
    {
        return isDisabled(component) || isReadOnly(component);
    }

    public static boolean isDisabled(UIComponent component)
    {
        return isTrue(component.getAttributes().get("disabled"));
    }

    public static boolean isReadOnly(UIComponent component)
    {
        return isTrue(component.getAttributes().get("readonly"));
    }

    private static boolean isTrue(Object obj)
    {
        if (obj instanceof String)
        {
            return Boolean.valueOf((String) obj);
        }
        if (!(obj instanceof Boolean))
        {
            return false;
        }
        return ((Boolean) obj);
    }

    /**
     * X-CHECKED: tlddoc h:selectManyListbox
     *
     * @param facesContext
     * @param component
     */
    public static void decodeUISelectMany(FacesContext facesContext, UIComponent component)
    {
        if (!(component instanceof EditableValueHolder))
        {
            throw new IllegalArgumentException("Component "
                    + component.getClientId(facesContext)
                    + " is not an EditableValueHolder");
        }
        
        Map paramValuesMap = facesContext.getExternalContext().getRequestParameterValuesMap();
        String clientId = component.getClientId(facesContext);
        if (isDisabledOrReadOnly(component))
        {
            return;
        }
        
        if (paramValuesMap.containsKey(clientId))
        {
            ArrayList<String> reqValues = new ArrayList<String>(Arrays.asList((String[]) paramValuesMap.get(clientId)));

            List<SelectItemInfo> selections = SelectItemsUtils.getSelectItemInfoList(
                (UISelectMany) component, facesContext);

            // if disabled value is submitted, do not use it
            for(SelectItemInfo itemInfo: selections)
            {
                if(itemInfo.getItem().isDisabled())
                {
                    String result = SharedRendererUtils.getConvertedStringValue(
                        facesContext,component,((ValueHolder) component).getConverter(), itemInfo.getItem().getValue());
                    reqValues.remove(result);
                }
            }
            // submitted value needs to be of type String[]
            String[] submittedValue = reqValues.toArray(new String[reqValues.size()]);
            ((EditableValueHolder) component).setSubmittedValue(submittedValue);
        }
        else
        {
            /* request parameter not found, nothing to decode - set submitted value to an empty array
               as we should get here only if the component is on a submitted form, is rendered
               and if the component is not readonly or has not been disabled.
               So in fact, there must be component value at this location, but for listboxes, comboboxes etc.
               the submitted value is not posted if no item is selected. */
            ((EditableValueHolder) component).setSubmittedValue(new String[] {});
        }
    }

    /**
     * X-CHECKED: tlddoc h:selectManyListbox
     *
     * @param facesContext
     * @param component
     */
    public static void decodeUISelectOne(FacesContext facesContext, UIComponent component)
    {
        if (!(component instanceof EditableValueHolder))
        {
            throw new IllegalArgumentException("Component "
                    + component.getClientId(facesContext)
                    + " is not an EditableValueHolder");
        }
        if (isDisabledOrReadOnly(component))
        {
            return;
        }
        
        Map paramMap = facesContext.getExternalContext().getRequestParameterMap();
        if (component instanceof UISelectOne)
        {
            String group = ((UISelectOne) component).getGroup();
            if (group != null && !group.isEmpty())
            {
                UIForm form = ComponentUtils.findClosest(UIForm.class, component);
                String fullGroupId = form.getClientId(facesContext) +
                        facesContext.getNamingContainerSeparatorChar() + group;
                if (paramMap.containsKey(fullGroupId))
                {
                    String submittedValue = (String) paramMap.get(fullGroupId);
                    String submittedValueNamespace = component.getClientId(facesContext) +
                            facesContext.getNamingContainerSeparatorChar();
                    if (submittedValue.startsWith(submittedValueNamespace))
                    {
                        submittedValue = submittedValue.substring(submittedValueNamespace.length());
                        SelectOneGroupSetSubmittedValueCallback callback = 
                                new SelectOneGroupSetSubmittedValueCallback(group,
                                        submittedValue,
                                        component.getClientId(facesContext),
                                        component.getValueExpression("value") != null);
                        form.visitTree(VisitContext.createVisitContext(facesContext,
                                null, MyFacesVisitHints.SET_SKIP_UNRENDERED),
                                callback);
                    }
                }
                else 
                {
                    // means input was not submitted. set to empty string so we can validate required fields
                    // if not set, a null value will skip validation -- see beginning of UIInput#validate
                    ((EditableValueHolder)component).setSubmittedValue(RendererUtils.EMPTY_STRING);
                } 
                return;
            }
        }
        
        String clientId = component.getClientId(facesContext);
        if (paramMap.containsKey(clientId))
        {
            String submittedValue = (String) paramMap.get(clientId); 
            List<SelectItemInfo> selections = SelectItemsUtils.getSelectItemInfoList(
                (UISelectOne) component, facesContext);

            // if disabled value is submitted, do not use it
            for(SelectItemInfo itemInfo: selections)
            {
                if(itemInfo.getItem().isDisabled())
                {
                    Object selectItemValue = itemInfo.getItem().getValue();
                    String convertedValue = SharedRendererUtils.getConvertedStringValue(
                        facesContext,component,((ValueHolder) component).getConverter(), selectItemValue);
                    if(convertedValue.equals(submittedValue))
                    {   // disabled value matches submitted value
                        submittedValue = RendererUtils.EMPTY_STRING;
                    }
                }
            }
            //request parameter found, set submitted value
            ((EditableValueHolder) component).setSubmittedValue(submittedValue);
        }
        else
        {
            //see reason for this action at decodeUISelectMany
            ((EditableValueHolder) component).setSubmittedValue(RendererUtils.EMPTY_STRING);
        }
    }
    
    private static class SelectOneGroupSetSubmittedValueCallback implements VisitCallback
    {
        private String group;
        private String submittedValue;
        private String submittedClientId;
        private boolean sourceComponentHasValueVE;
        private boolean submittedValueSet;

        public SelectOneGroupSetSubmittedValueCallback(String group, String submittedValue, String submittedClientId,
                boolean sourceComponentHasValueVE)
        {
            this.group = group;
            this.submittedValue = submittedValue;
            this.submittedClientId = submittedClientId;
            this.sourceComponentHasValueVE = sourceComponentHasValueVE;
            this.submittedValueSet = false;
        }

        @Override
        public VisitResult visit(VisitContext context, UIComponent target)
        {
            if (target instanceof UISelectOne)
            {
                UISelectOne targetSelectOneRadio = ((UISelectOne) target);
                String targetGroup = targetSelectOneRadio.getGroup();
                if (group.equals(targetGroup))
                {
                    if (this.sourceComponentHasValueVE)
                    {
                        // dataTable case or original case. Set submittedValue on that component and
                        // in the others ones of the group empty.
                        if (submittedClientId.equals(targetSelectOneRadio.getClientId(context.getFacesContext())))
                        {
                            targetSelectOneRadio.setSubmittedValue(submittedValue);
                        }
                        else
                        {
                            targetSelectOneRadio.resetValue();
                        }
                    }
                    else
                    {
                        // Find the first component with "value" VE set and set the submitted value there.
                        // For all other components set as submitted value empty.
                        if (!this.submittedValueSet)
                        {
                            if (targetSelectOneRadio.getValueExpression("value") != null)
                            {
                                targetSelectOneRadio.setSubmittedValue(submittedValue);
                                this.submittedValueSet = true;
                            }
                            else
                            {
                                targetSelectOneRadio.resetValue();
                            }
                        }
                        else
                        {
                            targetSelectOneRadio.resetValue();
                        }
                    }
                    return VisitResult.REJECT;
                }
            }
            return VisitResult.ACCEPT;
        }
    }

    public static Set getSubmittedOrSelectedValuesAsSet(boolean selectMany,
            UIComponent uiComponent, FacesContext facesContext, Converter converter)
    {
        Set lookupSet;
        if (selectMany)
        {
            UISelectMany uiSelectMany = (UISelectMany) uiComponent;
            lookupSet = RendererUtils.getSubmittedValuesAsSet(facesContext, uiComponent, converter, uiSelectMany);
            if (lookupSet == null)
            {
                lookupSet = RendererUtils.getSelectedValuesAsSet(facesContext,
                        uiComponent, converter, uiSelectMany);
            }
        }
        else
        {
            UISelectOne uiSelectOne = (UISelectOne) uiComponent;
            Object lookup = uiSelectOne.getSubmittedValue();
            if (lookup == null)
            {
                lookup = uiSelectOne.getValue();
                String lookupString =
                        SharedRendererUtils.getConvertedStringValue(facesContext, uiComponent, converter, lookup);
                lookupSet = Collections.singleton(lookupString);
            }
            else if (RendererUtils.EMPTY_STRING.equals(lookup))
            {
                lookupSet = Collections.EMPTY_SET;
            }
            else
            {
                lookupSet = Collections.singleton(lookup);
            }
        }
        return lookupSet;
    }

    public static Converter findUISelectManyConverterFailsafe(FacesContext facesContext, UIComponent uiComponent)
    {
        // invoke with considerValueType = false
        return findUISelectManyConverterFailsafe(facesContext, uiComponent, false);
    }

    public static Converter findUISelectManyConverterFailsafe(FacesContext facesContext, UIComponent uiComponent,
            boolean considerValueType)
    {
        Converter converter;
        try
        {
            converter = RendererUtils.findUISelectManyConverter(facesContext,
                    (UISelectMany) uiComponent, considerValueType);
        }
        catch (FacesException e)
        {
            log.log(Level.SEVERE,
                    "Error finding Converter for component with id "
                            + uiComponent.getClientId(facesContext), e);
            converter = null;
        }
        return converter;
    }

    public static Converter findUIOutputConverterFailSafe(FacesContext facesContext, UIComponent uiComponent)
    {
        Converter converter;
        try
        {
            converter = RendererUtils.findUIOutputConverter(facesContext, (UIOutput) uiComponent);
        }
        catch (FacesException e)
        {
            log.log(Level.SEVERE,
                    "Error finding Converter for component with id "
                            + uiComponent.getClientId(facesContext) + " "
                            + ComponentUtils.getPathToComponent(uiComponent), e);
            converter = null;
        }
        return converter;
    }

    /**
     * @return true, if the attribute was written
     * @throws java.io.IOException
     */
    public static boolean renderHTMLAttribute(ResponseWriter writer,
            String componentProperty, String attrName, Object value)
            throws IOException
    {

        if(attrName.equals(HTML.ONCHANGE_ATTR) && value != null && value.toString().length() == 0)
        {
            // don't write onchange attribute if value is ""
            return false; 
        }

        if (!RendererUtils.isDefaultAttributeValue(value))
        {
            // render Faces "styleClass" and "itemStyleClass" attributes as "class"
            String htmlAttrName = attrName.equals(HTML.STYLE_CLASS_ATTR) ? HTML.CLASS_ATTR : attrName;
            writer.writeAttribute(htmlAttrName, value, componentProperty);
            return true;
        }

        return false;
    }

    /**
     * @return true, if the attribute was written
     * @throws java.io.IOException
     */
    public static boolean renderHTMLAttribute(ResponseWriter writer,
            UIComponent component, String componentProperty, String htmlAttrName)
            throws IOException
    {
        Object value = component.getAttributes().get(componentProperty);
        return renderHTMLAttribute(writer, componentProperty, htmlAttrName,
                value);
    }

    /**
     * @return true, if an attribute was written
     * @throws java.io.IOException
     */
    public static boolean renderHTMLAttributes(ResponseWriter writer,
            UIComponent component, String[] attributes) throws IOException
    {
        boolean somethingDone = false;
        for (int i = 0, len = attributes.length; i < len; i++)
        {
            String attrName = attributes[i];
            if (renderHTMLAttribute(writer, component, attrName, attrName))
            {
                somethingDone = true;
            }
        }
        return somethingDone;
    }

    public static boolean renderHTMLAttributeWithOptionalStartElement(
            ResponseWriter writer, UIComponent component, String elementName,
            String attrName, Object value, boolean startElementWritten)
            throws IOException
    {
        if (!RendererUtils.isDefaultAttributeValue(value))
        {
            if (!startElementWritten)
            {
                writer.startElement(elementName, component);
                startElementWritten = true;
            }
            renderHTMLAttribute(writer, attrName, attrName, value);
        }
        return startElementWritten;
    }

    public static boolean renderHTMLAttributesWithOptionalStartElement(
            ResponseWriter writer, UIComponent component, String elementName,
            String[] attributes) throws IOException
    {
        boolean startElementWritten = false;
        for (int i = 0, len = attributes.length; i < len; i++)
        {
            String attrName = attributes[i];
            Object value = component.getAttributes().get(attrName);
            if (!RendererUtils.isDefaultAttributeValue(value))
            {
                if (!startElementWritten)
                {
                    writer.startElement(elementName, component);
                    startElementWritten = true;
                }
                renderHTMLAttribute(writer, attrName, attrName, value);
            }
        }
        return startElementWritten;
    }

    public static boolean renderOptionalEndElement(ResponseWriter writer,
            UIComponent component, String elementName, String[] attributes)
            throws IOException
    {
        boolean endElementNeeded = false;
        for (int i = 0, len = attributes.length; i < len; i++)
        {
            String attrName = attributes[i];
            Object value = component.getAttributes().get(attrName);
            if (!RendererUtils.isDefaultAttributeValue(value))
            {
                endElementNeeded = true;
                break;
            }
        }
        if (endElementNeeded)
        {
            writer.endElement(elementName);
            return true;
        }

        return false;
    }

    public static void writeId(ResponseWriter writer, UIComponent component, FacesContext facesContext)
            throws IOException
    {
        writer.writeAttribute(HTML.ID_ATTR, component.getClientId(facesContext), null);
    }
    
    public static void writeIdIfNecessary(ResponseWriter writer, UIComponent component, FacesContext facesContext)
            throws IOException
    {
        String id = component.getId();
        if (id != null && !id.startsWith(UIViewRoot.UNIQUE_ID_PREFIX))
        {
            writer.writeAttribute(HTML.ID_ATTR, component.getClientId(facesContext), null);
        }
    }

    public static void writeIdAndNameIfNecessary(ResponseWriter writer,
            UIComponent component, FacesContext facesContext)
            throws IOException
    {
        if (component.getId() != null
                && !component.getId().startsWith(UIViewRoot.UNIQUE_ID_PREFIX))
        {
            String clientId = component.getClientId(facesContext);
            writer.writeAttribute(HTML.ID_ATTR, clientId, null);
            writer.writeAttribute(HTML.NAME_ATTR, clientId, null);
        }
    }
    
    /**
     * Renders a html string type attribute. If the value retrieved from the component 
     * property is "", the attribute is rendered.
     * 
     * @param writer
     * @param component
     * @param componentProperty
     * @param htmlAttrName
     * @return
     * @throws IOException
     */
    public static final boolean renderHTMLStringPreserveEmptyAttribute(ResponseWriter writer,
            UIComponent component, String componentProperty, String htmlAttrName)
            throws IOException
    {
        String value = (String) component.getAttributes().get(componentProperty);        
        if (value != null)
        {
            writer.writeAttribute(htmlAttrName, value, componentProperty);
            return true;
        }
        return false;
    }
    
    /**
     * Renders a html string type attribute. If the value retrieved from the component 
     * property is "", the attribute is rendered.
     * 
     * @param writer
     * @param componentProperty
     * @param htmlAttrName
     * @return
     * @throws IOException
     */
    public static boolean renderHTMLStringPreserveEmptyAttribute(ResponseWriter writer,
            String componentProperty, String htmlAttrName, String value)
            throws IOException
    {
        if (value != null)
        {
            writer.writeAttribute(htmlAttrName, value, componentProperty);
            return true;
        }
        return false;
    }

    /**
     * Renders a html string type attribute. If the value retrieved from the component 
     * property is "" or null, the attribute is not rendered.
     * 
     * @param writer
     * @param component
     * @param componentProperty
     * @param htmlAttrName
     * @return
     * @throws IOException
     */
    public static boolean renderHTMLStringAttribute(ResponseWriter writer,
            UIComponent component, String componentProperty, String htmlAttrName)
            throws IOException
    {
        String value = (String) component.getAttributes().get(componentProperty);
        if (value != null && !value.isEmpty())
        {
            writer.writeAttribute(htmlAttrName, value, componentProperty);
            return true;
        }
        return false;
    }

    /**
     * Renders a html string type attribute. If the value retrieved from the component 
     * property is "" or null, the attribute is not rendered.
     * 
     * @param writer
     * @param componentProperty
     * @param htmlAttrName
     * @param value
     * @return
     * @throws IOException
     */
    public static boolean renderHTMLStringAttribute(ResponseWriter writer,
            String componentProperty, String htmlAttrName, String value)
            throws IOException
    {
        if (value != null && !value.isEmpty())
        {
            writer.writeAttribute(htmlAttrName, value, componentProperty);
            return true;
        }
        return false;
    }
    
    public static boolean renderHTMLStringNoStyleAttributes(ResponseWriter writer,
            UIComponent component, String[] attributes) throws IOException
    {
        boolean somethingDone = false;
        for (int i = 0, len = attributes.length; i < len; i++)
        {
            String attrName = attributes[i];
            if (renderHTMLStringAttribute(writer, component, attrName, attrName))
            {
                somethingDone = true;
            }
        }
        return somethingDone;
    }

    public static void writeIdAndName(ResponseWriter writer, UIComponent component, FacesContext facesContext)
            throws IOException
    {
        String clientId = component.getClientId(facesContext);
        writer.writeAttribute(HTML.ID_ATTR, clientId, null);
        writer.writeAttribute(HTML.NAME_ATTR, clientId, null);
    }

    public static void renderDisplayValueOnlyForSelects(FacesContext facesContext, UIComponent uiComponent)
            throws IOException
    {
        // invoke renderDisplayValueOnlyForSelects with considerValueType = false
        renderDisplayValueOnlyForSelects(facesContext, uiComponent, false);
    }

    public static void renderDisplayValueOnlyForSelects(FacesContext facesContext, UIComponent uiComponent,
            boolean considerValueType) throws IOException
    {
        ResponseWriter writer = facesContext.getResponseWriter();

        List<SelectItem> selectItemList = null;
        Converter converter = null;
        boolean isSelectOne = false;

        if (uiComponent instanceof UISelectBoolean)
        {
            converter = findUIOutputConverterFailSafe(facesContext, uiComponent);

            writer.startElement(HTML.SPAN_ELEM, uiComponent);
            writeIdIfNecessary(writer, uiComponent, facesContext);
            writer.writeText(SharedRendererUtils.getConvertedStringValue(
                    facesContext, uiComponent, converter,
                    ((UISelectBoolean) uiComponent).getValue()),
                    ComponentAttrs.VALUE_ATTR);
            writer.endElement(HTML.SPAN_ELEM);

        }
        else
        {
            if (uiComponent instanceof UISelectMany)
            {
                isSelectOne = false;
                selectItemList = RendererUtils.getSelectItemList(uiComponent, facesContext);
                converter = findUISelectManyConverterFailsafe(facesContext, uiComponent, considerValueType);
            }
            else if (uiComponent instanceof UISelectOne)
            {
                isSelectOne = true;
                selectItemList = RendererUtils.getSelectItemList(uiComponent, facesContext);
                converter = findUIOutputConverterFailSafe(facesContext, uiComponent);
            }

            writer.startElement(isSelectOne ? HTML.SPAN_ELEM : HTML.UL_ELEM, uiComponent);
            writeIdIfNecessary(writer, uiComponent, facesContext);

            Set lookupSet = getSubmittedOrSelectedValuesAsSet(
                    uiComponent instanceof UISelectMany, uiComponent,
                    facesContext, converter);

            renderSelectOptionsAsText(facesContext, uiComponent, converter,
                    lookupSet, selectItemList, isSelectOne);

            // bug #970747: force separate end tag
            writer.writeText(RendererUtils.EMPTY_STRING, null);
            writer.endElement(isSelectOne ? HTML.SPAN_ELEM : HTML.UL_ELEM);
        }

    }

    private static void renderSelectOptionsAsText(FacesContext context,
            UIComponent component, Converter converter, Set lookupSet,
            List selectItemList, boolean isSelectOne) throws IOException
    {
        ResponseWriter writer = context.getResponseWriter();

        for (int i = 0; i < selectItemList.size(); i++)
        {
            SelectItem selectItem = (SelectItem) selectItemList.get(i);

            if (selectItem instanceof SelectItemGroup)
            {
                SelectItem[] selectItems = ((SelectItemGroup) selectItem).getSelectItems();
                renderSelectOptionsAsText(context, component, converter,
                        lookupSet, Arrays.asList(selectItems), isSelectOne);
            }
            else
            {
                String itemStrValue = SharedRendererUtils.getConvertedStringValue(
                        context, component, converter, selectItem);

                if (lookupSet.contains(itemStrValue))
                {
                    //TODO/FIX: we always compare the String values, better fill lookupSet with Strings 
                    //only when useSubmittedValue==true, else use the real item value Objects
                    if (!isSelectOne)
                    {
                        writer.startElement(HTML.LI_ELEM, null); // component);
                    }
                    writer.writeText(selectItem.getLabel(), null);
                    if (!isSelectOne)
                    {
                        writer.endElement(HTML.LI_ELEM);
                    }
                    if (isSelectOne)
                    {
                        //take care of several choices with the same value; use only the first one
                        return;
                    }
                }
            }
        }
    }

    public static void renderTableCaption(FacesContext context, ResponseWriter writer, UIComponent component)
            throws IOException
    {
        UIComponent captionFacet = component.getFacet("caption");
        if (captionFacet == null)
        {
            return;
        }
        String captionClass;
        String captionStyle;
        if (component instanceof HtmlPanelGrid)
        {
            HtmlPanelGrid panelGrid = (HtmlPanelGrid) component;
            captionClass = panelGrid.getCaptionClass();
            captionStyle = panelGrid.getCaptionStyle();
        }
        else if (component instanceof HtmlDataTable)
        {
            HtmlDataTable dataTable = (HtmlDataTable) component;
            captionClass = dataTable.getCaptionClass();
            captionStyle = dataTable.getCaptionStyle();
        }
        else
        {
            captionClass = (String) component.getAttributes().get(ComponentAttrs.CAPTION_CLASS_ATTR);
            captionStyle = (String) component.getAttributes().get(ComponentAttrs.CAPTION_STYLE_ATTR);
        }
        writer.startElement(HTML.CAPTION_ELEM, null); // component);
        if (captionClass != null)
        {
            writer.writeAttribute(HTML.CLASS_ATTR, captionClass, null);
        }

        if (captionStyle != null)
        {
            writer.writeAttribute(HTML.STYLE_ATTR, captionStyle, null);
        }
        //RendererUtils.renderChild(context, captionFacet);
        captionFacet.encodeAll(context);
        writer.endElement(HTML.CAPTION_ELEM);
    }

    public static void renderDisplayValueOnly(FacesContext facesContext, UIInput input)
            throws IOException
    {
        ResponseWriter writer = facesContext.getResponseWriter();
        writer.startElement(HTML.SPAN_ELEM, input);
        writeIdIfNecessary(writer, input, facesContext);

        String strValue = RendererUtils.getStringValue(facesContext, input);
        writer.write(HTMLEncoder.encode(facesContext, strValue, true, true));
        writer.endElement(HTML.SPAN_ELEM);
    }

    public static void appendClearHiddenCommandFormParamsFunctionCall(
            StringBuilder buf, String formName)
    {
        HtmlJavaScriptUtils.appendClearHiddenCommandFormParamsFunctionCall(buf, formName);
    }

    @SuppressWarnings("unchecked")
    public static void renderFormSubmitScript(FacesContext facesContext)
            throws IOException
    {
        HtmlJavaScriptUtils.renderFormSubmitScript(facesContext);
    }

    public static void renderHiddenCommandFormParams(ResponseWriter writer, Set dummyFormParams)
            throws IOException
    {
        for (Iterator it = dummyFormParams.iterator(); it.hasNext();)
        {
            Object name = it.next();
            renderHiddenInputField(writer, name, null);
        }
    }

    public static void renderHiddenInputField(ResponseWriter writer, Object name, Object value)
            throws IOException
    {
        writer.startElement(HTML.INPUT_ELEM, null);
        writer.writeAttribute(HTML.TYPE_ATTR, HTML.INPUT_TYPE_HIDDEN, null);
        writer.writeAttribute(HTML.NAME_ATTR, name, null);
        if (value != null)
        {
            writer.writeAttribute(HTML.VALUE_ATTR, value, null);
        }
        writer.endElement(HTML.INPUT_ELEM);
    }

    /**
     * Renders a label HTML element
     */
    public static void renderLabel(ResponseWriter writer,
            UIComponent component, String forClientId, SelectItem item,
            boolean disabled) throws IOException
    {
        writer.startElement(HTML.LABEL_ELEM, null); // component);
        writer.writeAttribute(HTML.FOR_ATTR, forClientId, null);
        String labelClass = null;
        if (disabled)
        {
            labelClass = (String) component.getAttributes().get(ComponentAttrs.DISABLED_CLASS_ATTR);
        }
        else
        {
            labelClass = (String) component.getAttributes().get(ComponentAttrs.ENABLED_CLASS_ATTR);
        }
        if (labelClass != null)
        {
            writer.writeAttribute("class", labelClass, "labelClass");
        }
        if ((item.getLabel() != null) && (item.getLabel().length() > 0))
        {
            writer.write(" ");
            if (item.isEscape())
            {
                writer.writeText(item.getLabel(), null);
            }
            else
            {
                writer.write(item.getLabel());
            }
        }
        writer.endElement(HTML.LABEL_ELEM);
    }

    /**
     * Renders a label HTML element
     */
    public static void renderLabel(ResponseWriter writer,
            UIComponent component, String forClientId, SelectItem item,
            boolean disabled, boolean selected) throws IOException
    {
        writer.startElement(HTML.LABEL_ELEM, null); // component);
        writer.writeAttribute(HTML.FOR_ATTR, forClientId, null);
        String labelClass = null;
        if (disabled)
        {
            labelClass = (String) component.getAttributes().get(ComponentAttrs.DISABLED_CLASS_ATTR);
        }
        else
        {
            labelClass = (String) component.getAttributes()
                    .get(ComponentAttrs.ENABLED_CLASS_ATTR);
        }
        String labelSelectedClass = null;
        if (selected)
        {
            labelSelectedClass = (String) component.getAttributes().get(ComponentAttrs.SELECTED_CLASS_ATTR);
        }
        else
        {
            labelSelectedClass = (String) component.getAttributes().get(ComponentAttrs.UNSELECTED_CLASS_ATTR);
        }
        if (labelSelectedClass != null)
        {
            if (labelClass == null)
            {
                labelClass = labelSelectedClass;
            }
            else
            {
                labelClass = labelClass + ' ' + labelSelectedClass;
            }
        }
        if (labelClass != null)
        {
            writer.writeAttribute("class", labelClass, "labelClass");
        }
        if ((item.getLabel() != null) && (item.getLabel().length() > 0))
        {
            writer.write(HTML.NBSP_ENTITY);
            if (item.isEscape())
            {
                //writer.write(item.getLabel());
                writer.writeText(item.getLabel(), null);
            }
            else
            {
                //writer.write(HTMLEncoder.encode (item.getLabel()));
                writer.write(item.getLabel());
            }
        }
        writer.endElement(HTML.LABEL_ELEM);
    }

    /**
     * Get the name of the request parameter that holds the id of the
     * link-type component that caused the form to be submitted.
     * <p>
     * Within each page there may be multiple "link" type components that
     * cause page submission. On the server it is necessary to know which
     * of these actually caused the submit, in order to invoke the correct
     * listeners. Such components therefore store their id into the
     * "hidden command link field" in their associated form before
     * submitting it.
     * </p><p>
     * The field is always a direct child of each form, and has the same
     * <i>name</i> in each form. The id of the form component is therefore
     * both necessary and sufficient to determine the full name of the
     * field.</p>
     */
    public static String getHiddenCommandLinkFieldName(UIComponent form, FacesContext facesContext)
    {
        return form.getClientId(facesContext) + ':' + HIDDEN_COMMANDLINK_FIELD_NAME;
    }

    public static boolean isPartialOrBehaviorSubmit(FacesContext facesContext, String clientId)
    {
        Map<String, String> params = facesContext.getExternalContext().getRequestParameterMap();
        String sourceId = params.get(ClientBehaviorContext.BEHAVIOR_SOURCE_PARAM_NAME);
        if (sourceId == null || !sourceId.equals(clientId))
        {
            return false;
        }
        boolean partialOrBehaviorSubmit = false;
        String behaviorEvent = params.get(ClientBehaviorContext.BEHAVIOR_EVENT_PARAM_NAME);
        if (behaviorEvent != null)
        {
            partialOrBehaviorSubmit = ClientBehaviorEvents.ACTION.equals(behaviorEvent);
            if (partialOrBehaviorSubmit)
            {
                return partialOrBehaviorSubmit;
            }
        }
        String partialEvent = params.get(PartialViewContext.PARTIAL_EVENT_PARAM_NAME);
        if (partialEvent != null)
        {
            partialOrBehaviorSubmit = ClientBehaviorEvents.CLICK.equals(partialEvent);
        }
        return partialOrBehaviorSubmit;
    }

    public static String getOutcomeTargetHref(FacesContext facesContext, UIOutcomeTarget component)
            throws IOException
    {
        return OutcomeTargetUtils.getOutcomeTargetHref(facesContext, component);
    }




    public static Map<String, String> mapAttachedParamsToStringValues(
            FacesContext facesContext, UIComponent uiComponent)
    {
        Map<String, String> retVal = null;
        if (uiComponent.getChildCount() > 0)
        {
            List<UIParameter> validParams = getValidUIParameterChildren(
                    facesContext, uiComponent.getChildren(), true, true);
            for (int i = 0, size = validParams.size(); i < size; i++)
            {
                UIParameter param = validParams.get(i);
                String name = param.getName();
                Object value = param.getValue();

                if (retVal == null)
                {
                    retVal = new HashMap<>(3);
                }

                if (value instanceof String)
                {
                    retVal.put(name, (String) value);
                }
                else
                {
                    retVal.put(name, value.toString());
                }
            }
        }
        if (retVal == null)
        {
            retVal = Collections.emptyMap();
        }
        return retVal;
    }

    /**
     * Calls getValidUIParameterChildren(facesContext, children, skipNullValue, skipUnrendered, true);
     *
     * @param facesContext
     * @param children
     * @param skipNullValue
     * @param skipUnrendered
     * @return ArrayList size &gt; 0 if any parameter found
     */
    public static List<UIParameter> getValidUIParameterChildren(
            FacesContext facesContext, List<UIComponent> children,
            boolean skipNullValue, boolean skipUnrendered)
    {
        return getValidUIParameterChildren(facesContext, children,
                skipNullValue, skipUnrendered, true);
    }

    /**
     * Returns a List of all valid UIParameter children from the given children.
     * Valid means that the UIParameter is not disabled, its name is not null
     * (if skipNullName is true), its value is not null (if skipNullValue is true)
     * and it is rendered (if skipUnrendered is true). This method also creates a
     * warning for every UIParameter with a null-name (again, if skipNullName is true)
     * and, if ProjectStage is Development and skipNullValue is true, it informs the
     * user about every null-value.
     *
     * @param facesContext
     * @param children
     * @param skipNullValue  should UIParameters with a null value be skipped
     * @param skipUnrendered should UIParameters with isRendered() returning false be skipped
     * @param skipNullName   should UIParameters with a null name be skipped
     *                       (normally true, but in the case of h:outputFormat false)
     * @return ArrayList size &gt; 0 if any parameter found 
     */
    public static List<UIParameter> getValidUIParameterChildren(
            FacesContext facesContext, List<UIComponent> children,
            boolean skipNullValue, boolean skipUnrendered, boolean skipNullName)
    {
        return OutcomeTargetUtils.getValidUIParameterChildren(
            facesContext, children, skipNullValue, skipUnrendered, skipNullName);
    }

    /**
     * Render an attribute taking into account the passed event and
     * the component property. It will be rendered as "componentProperty"
     * attribute.
     *
     * @param facesContext
     * @param writer
     * @param componentProperty
     * @param component
     * @param eventName
     * @param clientBehaviors
     * @return
     * @throws IOException
     */
    public static boolean renderBehaviorizedAttribute(
            FacesContext facesContext, ResponseWriter writer,
            String componentProperty, UIComponent component, String eventName,
            Map<String, List<ClientBehavior>> clientBehaviors)
            throws IOException
    {
        return renderBehaviorizedAttribute(facesContext, writer,
                componentProperty, component, eventName, clientBehaviors, componentProperty);
    }

    public static boolean renderBehaviorizedAttribute(
            FacesContext facesContext, ResponseWriter writer,
            String componentProperty, UIComponent component,
            String sourceId, String eventName,
            Map<String, List<ClientBehavior>> clientBehaviors)
            throws IOException
    {
        return renderBehaviorizedAttribute(facesContext, writer,
                componentProperty, component, sourceId, eventName, clientBehaviors, componentProperty);
    }

    /**
     * Render an attribute taking into account the passed event and
     * the component property. The event will be rendered on the selected
     * htmlAttrName
     *
     * @param facesContext
     * @param writer
     * @param component
     * @param clientBehaviors
     * @param eventName
     * @param componentProperty
     * @param htmlAttrName
     * @return
     * @throws IOException
     */
    public static boolean renderBehaviorizedAttribute(
            FacesContext facesContext, ResponseWriter writer,
            String componentProperty, UIComponent component, String eventName,
            Map<String, List<ClientBehavior>> clientBehaviors,
            String htmlAttrName) throws IOException
    {
        return renderBehaviorizedAttribute(facesContext, writer,
                componentProperty, component, eventName, null, clientBehaviors,
                htmlAttrName, (String) component.getAttributes().get(componentProperty));
    }

    public static boolean renderBehaviorizedAttribute(
            FacesContext facesContext, ResponseWriter writer, String componentProperty, UIComponent component,
            String sourceId, String eventName, Map<String, List<ClientBehavior>> clientBehaviors,
            String htmlAttrName) throws IOException
    {
        return renderBehaviorizedAttribute(facesContext, writer,
                componentProperty, component, sourceId, eventName, null,
                clientBehaviors, htmlAttrName, (String) component.getAttributes().get(componentProperty));
    }

    /**
     * Render an attribute taking into account the passed event,
     * the component property and the passed attribute value for the component
     * property. The event will be rendered on the selected htmlAttrName.
     *
     * @param facesContext
     * @param writer
     * @param componentProperty
     * @param component
     * @param eventName
     * @param clientBehaviors
     * @param htmlAttrName
     * @param attributeValue
     * @return
     * @throws IOException
     */
    public static boolean renderBehaviorizedAttribute(
            FacesContext facesContext, ResponseWriter writer,
            String componentProperty, UIComponent component, String eventName,
            Collection<ClientBehaviorContext.Parameter> eventParameters,
            Map<String, List<ClientBehavior>> clientBehaviors,
            String htmlAttrName, String attributeValue) throws IOException
    {
        return renderBehaviorizedAttribute(facesContext, writer,
                componentProperty, component,
                null, eventName,
                eventParameters, clientBehaviors, htmlAttrName, attributeValue);
    }

    public static boolean renderBehaviorizedAttribute(
            FacesContext facesContext, ResponseWriter writer,
            String componentProperty, UIComponent component,
            String sourceId, String eventName,
            Collection<ClientBehaviorContext.Parameter> eventParameters,
            Map<String, List<ClientBehavior>> clientBehaviors,
            String htmlAttrName, String attributeValue) throws IOException
    {

        List<ClientBehavior> cbl = (clientBehaviors != null) ? clientBehaviors
                .get(eventName) : null;
        if (cbl == null || cbl.isEmpty())
        {
            return renderHTMLAttribute(writer, componentProperty, htmlAttrName, attributeValue);
        }
        if (cbl.size() > 1 || (cbl.size() == 1 && attributeValue != null))
        {
            return renderHTMLAttribute(writer, componentProperty, htmlAttrName,
                    ClientBehaviorRendererUtils.buildBehaviorChain(facesContext,
                            component, sourceId, eventName,
                            eventParameters, clientBehaviors, attributeValue,
                            RendererUtils.EMPTY_STRING));
        }
        else
        {
            //Only 1 behavior and attrValue == null, so just render it directly
            return renderHTMLAttribute(
                    writer, componentProperty, htmlAttrName,
                    cbl.get(0).getScript(
                            ClientBehaviorContext.createClientBehaviorContext(
                                    facesContext, component, eventName,
                                    sourceId, eventParameters)));
        }
    }

    /**
     * Render an attribute taking into account the passed event,
     * the passed attribute value for the component property.
     * and the specific server code.
     * The event will be rendered on the selected htmlAttrName.
     *
     * @param facesContext
     * @param writer
     * @param componentProperty
     * @param component
     * @param eventName
     * @param clientBehaviors
     * @param htmlAttrName
     * @param attributeValue
     * @param serverSideScript
     * @return
     * @throws IOException
     */
    public static boolean renderBehaviorizedAttribute(
            FacesContext facesContext, ResponseWriter writer,
            String componentProperty, UIComponent component, String eventName,
            Collection<ClientBehaviorContext.Parameter> eventParameters,
            Map<String, List<ClientBehavior>> clientBehaviors,
            String htmlAttrName, String attributeValue, String serverSideScript)
            throws IOException
    {
        return renderBehaviorizedAttribute(facesContext, writer,
                componentProperty, component,
                null, eventName,
                eventParameters, clientBehaviors, htmlAttrName, attributeValue,
                serverSideScript);
    }

    // CHECKSTYLE:OFF
    public static boolean renderBehaviorizedAttribute(
            FacesContext facesContext, ResponseWriter writer,
            String componentProperty, UIComponent component,
            String sourceId, String eventName,
            Collection<ClientBehaviorContext.Parameter> eventParameters,
            Map<String, List<ClientBehavior>> clientBehaviors,
            String htmlAttrName, String attributeValue, String serverSideScript)
            throws IOException
    {

        List<ClientBehavior> cbl = (clientBehaviors != null) ? clientBehaviors.get(eventName) : null;
        if (((cbl != null) ? cbl.size() : 0) + (attributeValue != null ? 1 : 0)
                + (serverSideScript != null ? 1 : 0) <= 1)
        {
            if (cbl == null || cbl.isEmpty())
            {
                if (attributeValue != null)
                {
                    return renderHTMLStringAttribute(writer, componentProperty, htmlAttrName, attributeValue);
                }
                else
                {
                    return renderHTMLStringAttribute(writer, componentProperty, htmlAttrName, serverSideScript);
                }
            }
            else
            {
                return renderHTMLStringAttribute(
                        writer, componentProperty, htmlAttrName,
                        cbl.get(0).getScript(
                                ClientBehaviorContext
                                        .createClientBehaviorContext(
                                                facesContext, component,
                                                eventName, sourceId,
                                                eventParameters)));
            }
        }
        else
        {
            return renderHTMLStringAttribute(writer, componentProperty, htmlAttrName,
                    ClientBehaviorRendererUtils.buildBehaviorChain(facesContext,
                            component, sourceId, eventName,
                            eventParameters, clientBehaviors, attributeValue,
                            serverSideScript));
        }
    }

    public static boolean renderBehaviorizedAttribute(
            FacesContext facesContext, ResponseWriter writer,
            String componentProperty, UIComponent component, String eventName,
            Collection<ClientBehaviorContext.Parameter> eventParameters,
            String eventName2,
            Collection<ClientBehaviorContext.Parameter> eventParameters2,
            Map<String, List<ClientBehavior>> clientBehaviors,
            String htmlAttrName, String attributeValue, String serverSideScript)
            throws IOException
    {
        return renderBehaviorizedAttribute(facesContext, writer,
                componentProperty, component,
                null, eventName,
                eventParameters, eventName2, eventParameters2, clientBehaviors,
                htmlAttrName, attributeValue, serverSideScript);
    }

    public static boolean renderBehaviorizedAttribute(
            FacesContext facesContext, ResponseWriter writer,
            String componentProperty, UIComponent component,
            String sourceId, String eventName,
            Collection<ClientBehaviorContext.Parameter> eventParameters,
            String eventName2,
            Collection<ClientBehaviorContext.Parameter> eventParameters2,
            Map<String, List<ClientBehavior>> clientBehaviors,
            String htmlAttrName, String attributeValue, String serverSideScript)
            throws IOException
    {
        List<ClientBehavior> cb1 = (clientBehaviors != null) ? clientBehaviors.get(eventName) : null;
        List<ClientBehavior> cb2 = (clientBehaviors != null) ? clientBehaviors.get(eventName2) : null;
        if (((cb1 != null) ? cb1.size() : 0) + ((cb2 != null) ? cb2.size() : 0)
                + (attributeValue != null ? 1 : 0) <= 1)
        {
            if (attributeValue != null)
            {
                return renderHTMLStringAttribute(writer, componentProperty, htmlAttrName, attributeValue);
            }
            else if (serverSideScript != null)
            {
                return renderHTMLStringAttribute(writer, componentProperty, htmlAttrName, serverSideScript);
            }
            else if (((cb1 != null) ? cb1.size() : 0) > 0)
            {
                return renderHTMLStringAttribute(
                        writer, componentProperty, htmlAttrName,
                        cb1.get(0).getScript(ClientBehaviorContext
                                        .createClientBehaviorContext(
                                                facesContext, component,
                                                eventName, sourceId,
                                                eventParameters)));
            }
            else
            {
                return renderHTMLStringAttribute(
                        writer, componentProperty, htmlAttrName,
                        cb2.get(0).getScript(ClientBehaviorContext
                                        .createClientBehaviorContext(
                                                facesContext, component,
                                                eventName2, sourceId,
                                                eventParameters2)));
            }
        }
        else
        {
            return renderHTMLStringAttribute(writer, componentProperty, htmlAttrName,
                    ClientBehaviorRendererUtils.buildBehaviorChain(facesContext,
                            component, sourceId, eventName,
                            eventParameters, eventName2, eventParameters2,
                            clientBehaviors, attributeValue, serverSideScript));
        }
    }
    // CHECKSTYLE: ON

    public static void renderBehaviorizedEventHandlers(
            FacesContext facesContext, ResponseWriter writer,
            UIComponent uiComponent,
            Map<String, List<ClientBehavior>> clientBehaviors)
            throws IOException
    {
        renderBehaviorizedEventHandlers(facesContext, writer, uiComponent,
                null, clientBehaviors);
    }

    public static void renderBehaviorizedEventHandlers(
            FacesContext facesContext, ResponseWriter writer,
            UIComponent uiComponent, String sourceId,
            Map<String, List<ClientBehavior>> clientBehaviors)
            throws IOException
    {
        renderBehaviorizedAttribute(facesContext, writer, HTML.ONCLICK_ATTR,
                uiComponent, sourceId, ClientBehaviorEvents.CLICK,
                clientBehaviors, HTML.ONCLICK_ATTR);
        renderBehaviorizedAttribute(facesContext, writer, HTML.ONDBLCLICK_ATTR,
                uiComponent, sourceId, ClientBehaviorEvents.DBLCLICK,
                clientBehaviors, HTML.ONDBLCLICK_ATTR);
        renderBehaviorizedAttribute(facesContext, writer,
                HTML.ONMOUSEDOWN_ATTR, uiComponent, sourceId,
                ClientBehaviorEvents.MOUSEDOWN, clientBehaviors,
                HTML.ONMOUSEDOWN_ATTR);
        renderBehaviorizedAttribute(facesContext, writer, HTML.ONMOUSEUP_ATTR,
                uiComponent, sourceId, ClientBehaviorEvents.MOUSEUP,
                clientBehaviors, HTML.ONMOUSEUP_ATTR);
        renderBehaviorizedAttribute(facesContext, writer,
                HTML.ONMOUSEOVER_ATTR, uiComponent, sourceId,
                ClientBehaviorEvents.MOUSEOVER, clientBehaviors,
                HTML.ONMOUSEOVER_ATTR);
        renderBehaviorizedAttribute(facesContext, writer,
                HTML.ONMOUSEMOVE_ATTR, uiComponent, sourceId,
                ClientBehaviorEvents.MOUSEMOVE, clientBehaviors,
                HTML.ONMOUSEMOVE_ATTR);
        renderBehaviorizedAttribute(facesContext, writer, HTML.ONMOUSEOUT_ATTR,
                uiComponent, sourceId, ClientBehaviorEvents.MOUSEOUT,
                clientBehaviors, HTML.ONMOUSEOUT_ATTR);
        renderBehaviorizedAttribute(facesContext, writer, HTML.ONKEYPRESS_ATTR,
                uiComponent, sourceId, ClientBehaviorEvents.KEYPRESS,
                clientBehaviors, HTML.ONKEYPRESS_ATTR);
        renderBehaviorizedAttribute(facesContext, writer, HTML.ONKEYDOWN_ATTR,
                uiComponent, sourceId, ClientBehaviorEvents.KEYDOWN,
                clientBehaviors, HTML.ONKEYDOWN_ATTR);
        renderBehaviorizedAttribute(facesContext, writer, HTML.ONKEYUP_ATTR,
                uiComponent, sourceId, ClientBehaviorEvents.KEYUP,
                clientBehaviors, HTML.ONKEYUP_ATTR);
    }

    public static void renderBehaviorizedEventHandlersWithoutOnclick(
            FacesContext facesContext, ResponseWriter writer,
            UIComponent uiComponent,
            Map<String, List<ClientBehavior>> clientBehaviors)
            throws IOException
    {
        renderBehaviorizedAttribute(facesContext, writer, HTML.ONDBLCLICK_ATTR,
                uiComponent, ClientBehaviorEvents.DBLCLICK, clientBehaviors,
                HTML.ONDBLCLICK_ATTR);
        renderBehaviorizedAttribute(facesContext, writer,
                HTML.ONMOUSEDOWN_ATTR, uiComponent,
                ClientBehaviorEvents.MOUSEDOWN, clientBehaviors,
                HTML.ONMOUSEDOWN_ATTR);
        renderBehaviorizedAttribute(facesContext, writer, HTML.ONMOUSEUP_ATTR,
                uiComponent, ClientBehaviorEvents.MOUSEUP, clientBehaviors,
                HTML.ONMOUSEUP_ATTR);
        renderBehaviorizedAttribute(facesContext, writer,
                HTML.ONMOUSEOVER_ATTR, uiComponent,
                ClientBehaviorEvents.MOUSEOVER, clientBehaviors,
                HTML.ONMOUSEOVER_ATTR);
        renderBehaviorizedAttribute(facesContext, writer,
                HTML.ONMOUSEMOVE_ATTR, uiComponent,
                ClientBehaviorEvents.MOUSEMOVE, clientBehaviors,
                HTML.ONMOUSEMOVE_ATTR);
        renderBehaviorizedAttribute(facesContext, writer, HTML.ONMOUSEOUT_ATTR,
                uiComponent, ClientBehaviorEvents.MOUSEOUT, clientBehaviors,
                HTML.ONMOUSEOUT_ATTR);
        renderBehaviorizedAttribute(facesContext, writer, HTML.ONKEYPRESS_ATTR,
                uiComponent, ClientBehaviorEvents.KEYPRESS, clientBehaviors,
                HTML.ONKEYPRESS_ATTR);
        renderBehaviorizedAttribute(facesContext, writer, HTML.ONKEYDOWN_ATTR,
                uiComponent, ClientBehaviorEvents.KEYDOWN, clientBehaviors,
                HTML.ONKEYDOWN_ATTR);
        renderBehaviorizedAttribute(facesContext, writer, HTML.ONKEYUP_ATTR,
                uiComponent, ClientBehaviorEvents.KEYUP, clientBehaviors,
                HTML.ONKEYUP_ATTR);
    }

    public static void renderBehaviorizedEventHandlersWithoutOnmouseoverAndOnmouseout(
            FacesContext facesContext, ResponseWriter writer,
            UIComponent uiComponent,
            Map<String, List<ClientBehavior>> clientBehaviors)
            throws IOException
    {
        renderBehaviorizedAttribute(facesContext, writer, HTML.ONCLICK_ATTR,
                uiComponent, ClientBehaviorEvents.CLICK, clientBehaviors,
                HTML.ONCLICK_ATTR);
        renderBehaviorizedAttribute(facesContext, writer, HTML.ONDBLCLICK_ATTR,
                uiComponent, ClientBehaviorEvents.DBLCLICK, clientBehaviors,
                HTML.ONDBLCLICK_ATTR);
        renderBehaviorizedAttribute(facesContext, writer,
                HTML.ONMOUSEDOWN_ATTR, uiComponent,
                ClientBehaviorEvents.MOUSEDOWN, clientBehaviors,
                HTML.ONMOUSEDOWN_ATTR);
        renderBehaviorizedAttribute(facesContext, writer, HTML.ONMOUSEUP_ATTR,
                uiComponent, ClientBehaviorEvents.MOUSEUP, clientBehaviors,
                HTML.ONMOUSEUP_ATTR);
        renderBehaviorizedAttribute(facesContext, writer,
                HTML.ONMOUSEMOVE_ATTR, uiComponent,
                ClientBehaviorEvents.MOUSEMOVE, clientBehaviors,
                HTML.ONMOUSEMOVE_ATTR);
        renderBehaviorizedAttribute(facesContext, writer, HTML.ONKEYPRESS_ATTR,
                uiComponent, ClientBehaviorEvents.KEYPRESS, clientBehaviors,
                HTML.ONKEYPRESS_ATTR);
        renderBehaviorizedAttribute(facesContext, writer, HTML.ONKEYDOWN_ATTR,
                uiComponent, ClientBehaviorEvents.KEYDOWN, clientBehaviors,
                HTML.ONKEYDOWN_ATTR);
        renderBehaviorizedAttribute(facesContext, writer, HTML.ONKEYUP_ATTR,
                uiComponent, ClientBehaviorEvents.KEYUP, clientBehaviors,
                HTML.ONKEYUP_ATTR);
    }

    public static void renderBehaviorizedFieldEventHandlers(
            FacesContext facesContext, ResponseWriter writer,
            UIComponent uiComponent,
            Map<String, List<ClientBehavior>> clientBehaviors)
            throws IOException
    {
        renderBehaviorizedAttribute(facesContext, writer, HTML.ONFOCUS_ATTR,
                uiComponent, ClientBehaviorEvents.FOCUS, clientBehaviors, HTML.ONFOCUS_ATTR);
        renderBehaviorizedAttribute(facesContext, writer, HTML.ONBLUR_ATTR,
                uiComponent, ClientBehaviorEvents.BLUR, clientBehaviors, HTML.ONBLUR_ATTR);
        renderBehaviorizedAttribute(facesContext, writer, HTML.ONCHANGE_ATTR,
                uiComponent, ClientBehaviorEvents.CHANGE, clientBehaviors, HTML.ONCHANGE_ATTR);
        renderBehaviorizedAttribute(facesContext, writer, HTML.ONSELECT_ATTR,
                uiComponent, ClientBehaviorEvents.SELECT, clientBehaviors, HTML.ONSELECT_ATTR);
    }

    public static void renderBehaviorizedFieldEventHandlersWithoutOnfocus(
            FacesContext facesContext, ResponseWriter writer,
            UIComponent uiComponent,
            Map<String, List<ClientBehavior>> clientBehaviors)
            throws IOException
    {
        renderBehaviorizedAttribute(facesContext, writer, HTML.ONBLUR_ATTR,
                uiComponent, ClientBehaviorEvents.BLUR, clientBehaviors, HTML.ONBLUR_ATTR);
        renderBehaviorizedAttribute(facesContext, writer, HTML.ONCHANGE_ATTR,
                uiComponent, ClientBehaviorEvents.CHANGE, clientBehaviors, HTML.ONCHANGE_ATTR);
        renderBehaviorizedAttribute(facesContext, writer, HTML.ONSELECT_ATTR,
                uiComponent, ClientBehaviorEvents.SELECT, clientBehaviors, HTML.ONSELECT_ATTR);
    }

    public static void renderBehaviorizedFieldEventHandlersWithoutOnchange(
            FacesContext facesContext, ResponseWriter writer,
            UIComponent uiComponent,
            Map<String, List<ClientBehavior>> clientBehaviors)
            throws IOException
    {
        renderBehaviorizedAttribute(facesContext, writer, HTML.ONFOCUS_ATTR,
                uiComponent, ClientBehaviorEvents.FOCUS, clientBehaviors, HTML.ONFOCUS_ATTR);
        renderBehaviorizedAttribute(facesContext, writer, HTML.ONBLUR_ATTR,
                uiComponent, ClientBehaviorEvents.BLUR, clientBehaviors, HTML.ONBLUR_ATTR);
        renderBehaviorizedAttribute(facesContext, writer, HTML.ONSELECT_ATTR,
                uiComponent, ClientBehaviorEvents.SELECT, clientBehaviors, HTML.ONSELECT_ATTR);
    }

    public static void renderBehaviorizedFieldEventHandlersWithoutOnchange(
            FacesContext facesContext, ResponseWriter writer,
            UIComponent uiComponent, String sourceId,
            Map<String, List<ClientBehavior>> clientBehaviors)
            throws IOException
    {
        renderBehaviorizedAttribute(facesContext, writer, HTML.ONFOCUS_ATTR,
                uiComponent, sourceId, ClientBehaviorEvents.FOCUS, clientBehaviors, HTML.ONFOCUS_ATTR);
        renderBehaviorizedAttribute(facesContext, writer, HTML.ONBLUR_ATTR,
                uiComponent, sourceId, ClientBehaviorEvents.BLUR, clientBehaviors, HTML.ONBLUR_ATTR);
        renderBehaviorizedAttribute(facesContext, writer, HTML.ONSELECT_ATTR,
                uiComponent, sourceId, ClientBehaviorEvents.SELECT, clientBehaviors, HTML.ONSELECT_ATTR);
    }

    public static void renderBehaviorizedFieldEventHandlersWithoutOnchangeAndOnselect(
            FacesContext facesContext, ResponseWriter writer,
            UIComponent uiComponent,
            Map<String, List<ClientBehavior>> clientBehaviors)
            throws IOException
    {
        renderBehaviorizedAttribute(facesContext, writer, HTML.ONFOCUS_ATTR,
                uiComponent, ClientBehaviorEvents.FOCUS, clientBehaviors, HTML.ONFOCUS_ATTR);
        renderBehaviorizedAttribute(facesContext, writer, HTML.ONBLUR_ATTR,
                uiComponent, ClientBehaviorEvents.BLUR, clientBehaviors, HTML.ONBLUR_ATTR);
    }

    public static boolean renderBehaviorizedOnchangeEventHandler(
            FacesContext facesContext, ResponseWriter writer,
            UIComponent uiComponent,
            Map<String, List<ClientBehavior>> clientBehaviors)
            throws IOException
    {
        boolean hasChange = ClientBehaviorRendererUtils.hasClientBehavior(
                ClientBehaviorEvents.CHANGE, clientBehaviors);
        boolean hasValueChange = ClientBehaviorRendererUtils.hasClientBehavior(
                ClientBehaviorEvents.VALUECHANGE, clientBehaviors);

        if (hasChange && hasValueChange)
        {
            String chain = ClientBehaviorRendererUtils.buildBehaviorChain(facesContext,
                    uiComponent, ClientBehaviorEvents.CHANGE, null,
                    ClientBehaviorEvents.VALUECHANGE, null, clientBehaviors,
                    (String) uiComponent.getAttributes().get(HTML.ONCHANGE_ATTR), null);

            return HtmlRendererUtils.renderHTMLStringAttribute(writer,
                    HTML.ONCHANGE_ATTR, HTML.ONCHANGE_ATTR, chain);
        }
        else if (hasChange)
        {
            return HtmlRendererUtils.renderBehaviorizedAttribute(facesContext,
                    writer, HTML.ONCHANGE_ATTR, uiComponent,
                    ClientBehaviorEvents.CHANGE, clientBehaviors, HTML.ONCHANGE_ATTR);
        }
        else if (hasValueChange)
        {
            return HtmlRendererUtils.renderBehaviorizedAttribute(facesContext,
                    writer, HTML.ONCHANGE_ATTR, uiComponent,
                    ClientBehaviorEvents.VALUECHANGE, clientBehaviors, HTML.ONCHANGE_ATTR);
        }
        else
        {
            return HtmlRendererUtils.renderHTMLStringAttribute(writer, uiComponent,
                    HTML.ONCHANGE_ATTR, HTML.ONCHANGE_ATTR);
        }
    }

    public static boolean renderBehaviorizedOnchangeEventHandler(
            FacesContext facesContext, ResponseWriter writer,
            UIComponent uiComponent, String sourceId,
            Map<String, List<ClientBehavior>> clientBehaviors)
            throws IOException
    {
        boolean hasChange = ClientBehaviorRendererUtils.hasClientBehavior(
                ClientBehaviorEvents.CHANGE, clientBehaviors);
        boolean hasValueChange = ClientBehaviorRendererUtils.hasClientBehavior(
                ClientBehaviorEvents.VALUECHANGE, clientBehaviors);

        if (hasChange && hasValueChange)
        {
            String chain = ClientBehaviorRendererUtils.buildBehaviorChain(facesContext,
                    uiComponent, sourceId, ClientBehaviorEvents.CHANGE,
                    null, ClientBehaviorEvents.VALUECHANGE, null,
                    clientBehaviors,
                    (String) uiComponent.getAttributes().get(HTML.ONCHANGE_ATTR), null);

            return HtmlRendererUtils.renderHTMLStringAttribute(writer,
                    HTML.ONCHANGE_ATTR, HTML.ONCHANGE_ATTR, chain);
        }
        else if (hasChange)
        {
            return HtmlRendererUtils.renderBehaviorizedAttribute(facesContext,
                    writer, HTML.ONCHANGE_ATTR, uiComponent, sourceId,
                    ClientBehaviorEvents.CHANGE, clientBehaviors, HTML.ONCHANGE_ATTR);
        }
        else if (hasValueChange)
        {
            return HtmlRendererUtils.renderBehaviorizedAttribute(facesContext,
                    writer, HTML.ONCHANGE_ATTR, uiComponent, sourceId,
                    ClientBehaviorEvents.VALUECHANGE, clientBehaviors, HTML.ONCHANGE_ATTR);
        }
        else
        {
            return HtmlRendererUtils.renderHTMLStringAttribute(writer, uiComponent,
                    HTML.ONCHANGE_ATTR, HTML.ONCHANGE_ATTR);
        }
    }

    /**
     * Returns the value of the hideNoSelectionOption attribute of the given UIComponent
     * @param component
     * @return
     */
    public static boolean isHideNoSelectionOption(UIComponent component)
    {
        // check hideNoSelectionOption for literal value (String) or ValueExpression (Boolean)
        Object hideNoSelectionOptionAttr = component.getAttributes().get(ComponentAttrs.HIDE_NO_SELECTION_OPTION_ATTR);
        return ((hideNoSelectionOptionAttr instanceof String && "true"
                .equalsIgnoreCase((String) hideNoSelectionOptionAttr)) || 
                (hideNoSelectionOptionAttr instanceof Boolean && ((Boolean) hideNoSelectionOptionAttr)));
    }

    /**
     * Renders all FacesMessages which have not been rendered yet with
     * the help of a HtmlMessages component.
     * @param facesContext
     */
    public static void renderUnhandledFacesMessages(FacesContext facesContext)
            throws IOException
    {
        // create and configure HtmlMessages component
        HtmlMessages messages = (HtmlMessages) facesContext.getApplication()
                .createComponent(HtmlMessages.COMPONENT_TYPE);
        messages.setId("jakarta_faces_developmentstage_messages");
        messages.setTitle("Project Stage[Development]: Unhandled Messages");
        messages.setStyle("color:orange");
        messages.setRedisplay(false);
        // render the component
        messages.encodeAll(facesContext);
    }

    /**
     * Returns <code>true</code> if the view root associated with the given faces context will be rendered with a HTML5 doctype.
     * @param context Involved faces context.
     * @return <code>true</code> if the view root associated with the given faces context will be rendered with a HTML5 doctype.
     */
    public static boolean isOutputHtml5Doctype(FacesContext context) {
        UIViewRoot viewRoot = context.getViewRoot();

        if (viewRoot == null) {
            return false;
        }

        Doctype doctype = viewRoot.getDoctype();

        if (doctype == null) {
            return false;
        }

        return "html".equalsIgnoreCase(doctype.getRootElement())
                && doctype.getPublic() == null
                && doctype.getSystem() == null;
    }

    /**
     * If HTML5 doctype do not render the "type='text/javascript`" attribute as its not necessary.
     *
     * @param context Involved faces context.
     * @param writer Involved response writer.
     * @throws IOException if any error occurs writing the response.
     */
    public static void renderScriptType(FacesContext context, ResponseWriter writer) throws IOException
    {
        if (!HtmlRendererUtils.isOutputHtml5Doctype(context))
        {
            writer.writeAttribute(HTML.SCRIPT_TYPE_ATTR, HTML.SCRIPT_TYPE_TEXT_JAVASCRIPT, null);
        }
    }
}
