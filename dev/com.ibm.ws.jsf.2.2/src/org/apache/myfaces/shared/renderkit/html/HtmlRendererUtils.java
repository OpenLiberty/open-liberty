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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.RandomAccess;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.faces.FacesException;
import javax.faces.component.EditableValueHolder;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.component.UIOutcomeTarget;
import javax.faces.component.UIOutput;
import javax.faces.component.UIParameter;
import javax.faces.component.UISelectBoolean;
import javax.faces.component.UISelectMany;
import javax.faces.component.UISelectOne;
import javax.faces.component.UIViewRoot;
import javax.faces.component.behavior.ClientBehavior;
import javax.faces.component.behavior.ClientBehaviorContext;
import javax.faces.component.behavior.ClientBehaviorHint;
import javax.faces.component.behavior.ClientBehaviorHolder;
import javax.faces.component.html.HtmlDataTable;
import javax.faces.component.html.HtmlMessages;
import javax.faces.component.html.HtmlPanelGrid;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.convert.Converter;
import javax.faces.model.SelectItem;
import javax.faces.model.SelectItemGroup;

import org.apache.myfaces.shared.component.DisplayValueOnlyCapable;
import org.apache.myfaces.shared.component.EscapeCapable;
import org.apache.myfaces.shared.renderkit.ClientBehaviorEvents;
import org.apache.myfaces.shared.renderkit.JSFAttr;
import org.apache.myfaces.shared.renderkit.RendererUtils;
import org.apache.myfaces.shared.renderkit.html.util.FormInfo;
import org.apache.myfaces.shared.renderkit.html.util.HTMLEncoder;
import org.apache.myfaces.shared.renderkit.html.util.OutcomeTargetUtils;

public final class HtmlRendererUtils
{
    //private static final Log log = LogFactory.getLog(HtmlRendererUtils.class);
    private static final Logger log = Logger.getLogger(HtmlRendererUtils.class
            .getName());
    //private static final String[] EMPTY_STRING_ARRAY = new String[0];
    private static final String LINE_SEPARATOR = System.getProperty(
            "line.separator", "\r\n");
    private static final char TABULATOR = '\t';
    public static final String HIDDEN_COMMANDLINK_FIELD_NAME = "_idcl";
    public static final String HIDDEN_COMMANDLINK_FIELD_NAME_MYFACES_OLD = "_link_hidden_";
    public static final String HIDDEN_COMMANDLINK_FIELD_NAME_TRINIDAD = "source";
    public static final String CLEAR_HIDDEN_FIELD_FN_NAME = "clearFormHiddenParams";
    public static final String SUBMIT_FORM_FN_NAME = "oamSubmitForm";
    public static final String SUBMIT_FORM_FN_NAME_JSF2 = "myfaces.oam.submitForm";
    public static final String ALLOW_CDATA_SECTION_ON = "org.apache.myfaces.ResponseWriter.CdataSectionOn";
    public static final String NON_SUBMITTED_VALUE_WARNING 
            = "There should always be a submitted value for an input if it is rendered,"
            + " its form is submitted, and it was not originally rendered disabled or read-only."
            + "  You cannot submit a form after disabling an input element via javascript."
            + "  Consider setting read-only to true instead"
            + " or resetting the disabled value back to false prior to form submission.";
    public static final String STR_EMPTY = "";

    private HtmlRendererUtils()
    {
        // utility class, do not instantiate
    }

    /**
     * Utility to set the submitted value of the provided component from the
     * data in the current request object.
     * <p/>
     * Param component is required to be an EditableValueHolder. On return
     * from this method, the component's submittedValue property will be
     * set if the submitted form contained that component.
     */
    public static void decodeUIInput(FacesContext facesContext, UIComponent component)
    {
        if (!(component instanceof EditableValueHolder))
        {
            throw new IllegalArgumentException("Component "
                    + component.getClientId(facesContext)
                    + " is not an EditableValueHolder");
        }
        Map paramMap = facesContext.getExternalContext()
                .getRequestParameterMap();
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
            log.warning(NON_SUBMITTED_VALUE_WARNING + " Component : "
                    + RendererUtils.getPathToComponent(component));
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
        Map paramMap = facesContext.getExternalContext()
                .getRequestParameterMap();
        String clientId = component.getClientId(facesContext);
        if (paramMap.containsKey(clientId))
        {
            String reqValue = (String) paramMap.get(clientId);
            if ((reqValue.equalsIgnoreCase("on")
                    || reqValue.equalsIgnoreCase("yes") || reqValue
                    .equalsIgnoreCase("true")))
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
        return isDisplayValueOnly(component) || isDisabled(component) || isReadOnly(component);
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
        return ((Boolean) obj).booleanValue();
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
            String[] reqValues = (String[]) paramValuesMap.get(clientId);
            ((EditableValueHolder) component).setSubmittedValue(reqValues);
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
        String clientId = component.getClientId(facesContext);
        if (paramMap.containsKey(clientId))
        {
            //request parameter found, set submitted value
            ((EditableValueHolder) component).setSubmittedValue(paramMap.get(clientId));
        }
        else
        {
            //see reason for this action at decodeUISelectMany
            ((EditableValueHolder) component).setSubmittedValue(STR_EMPTY);
        }
    }

    /**
     * @since 4.0.0
     */
    public static void decodeClientBehaviors(FacesContext facesContext, UIComponent component)
    {
        if (component instanceof ClientBehaviorHolder)
        {
            ClientBehaviorHolder clientBehaviorHolder = (ClientBehaviorHolder) component;
            Map<String, List<ClientBehavior>> clientBehaviors = clientBehaviorHolder
                    .getClientBehaviors();
            if (clientBehaviors != null && !clientBehaviors.isEmpty())
            {
                Map<String, String> paramMap = facesContext
                        .getExternalContext().getRequestParameterMap();
                String behaviorEventName = paramMap
                        .get("javax.faces.behavior.event");
                if (behaviorEventName != null)
                {
                    List<ClientBehavior> clientBehaviorList = clientBehaviors
                            .get(behaviorEventName);
                    if (clientBehaviorList != null
                            && !clientBehaviorList.isEmpty())
                    {
                        String clientId = paramMap.get("javax.faces.source");
                        if (component.getClientId(facesContext).equals(clientId))
                        {
                            if (clientBehaviorList instanceof RandomAccess)
                            {
                                for (int i = 0, size = clientBehaviorList.size(); i < size; i++)
                                {
                                    ClientBehavior clientBehavior = clientBehaviorList.get(i);
                                    clientBehavior.decode(facesContext, component);
                                }
                            } 
                            else
                            {
                                for (ClientBehavior clientBehavior : clientBehaviorList)
                                {
                                    clientBehavior.decode(facesContext, component);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public static void renderListbox(FacesContext facesContext,
            UISelectOne selectOne, boolean disabled, int size,
            Converter converter) throws IOException
    {
        internalRenderSelect(facesContext, selectOne, disabled, size, false, converter);
    }

    public static void renderListbox(FacesContext facesContext,
            UISelectMany selectMany, boolean disabled, int size,
            Converter converter) throws IOException
    {
        internalRenderSelect(facesContext, selectMany, disabled, size, true, converter);
    }

    public static void renderMenu(FacesContext facesContext,
            UISelectOne selectOne, boolean disabled, Converter converter)
            throws IOException
    {
        internalRenderSelect(facesContext, selectOne, disabled, 1, false, converter);
    }

    public static void renderMenu(FacesContext facesContext,
            UISelectMany selectMany, boolean disabled, Converter converter)
            throws IOException
    {
        internalRenderSelect(facesContext, selectMany, disabled, 1, true, converter);
    }

    private static void internalRenderSelect(FacesContext facesContext,
            UIComponent uiComponent, boolean disabled, int size,
            boolean selectMany, Converter converter) throws IOException
    {
        ResponseWriter writer = facesContext.getResponseWriter();
        writer.startElement(HTML.SELECT_ELEM, uiComponent);
        if (uiComponent instanceof ClientBehaviorHolder
                && !((ClientBehaviorHolder) uiComponent).getClientBehaviors().isEmpty())
        {
            writer.writeAttribute(HTML.ID_ATTR, uiComponent.getClientId(facesContext), null);
        }
        else
        {
            HtmlRendererUtils.writeIdIfNecessary(writer, uiComponent, facesContext);
        }
        writer.writeAttribute(HTML.NAME_ATTR,
                uiComponent.getClientId(facesContext), null);
        List selectItemList;
        if (selectMany)
        {
            writer.writeAttribute(HTML.MULTIPLE_ATTR, HTML.MULTIPLE_ATTR, null);
            selectItemList = org.apache.myfaces.shared.renderkit.RendererUtils
                    .getSelectItemList((UISelectMany) uiComponent, facesContext);
        }
        else
        {
            selectItemList = RendererUtils.getSelectItemList(
                    (UISelectOne) uiComponent, facesContext);
        }

        if (size == Integer.MIN_VALUE)
        {
            //No size given (Listbox) --> size is number of select items
            writer.writeAttribute(HTML.SIZE_ATTR,
                    Integer.toString(selectItemList.size()), null);
        }
        else
        {
            writer.writeAttribute(HTML.SIZE_ATTR, Integer.toString(size), null);
        }
        Map<String, List<ClientBehavior>> behaviors = null;
        if (uiComponent instanceof ClientBehaviorHolder)
        {
            behaviors = ((ClientBehaviorHolder) uiComponent)
                    .getClientBehaviors();
            renderBehaviorizedOnchangeEventHandler(facesContext, writer, uiComponent, behaviors);
            renderBehaviorizedEventHandlers(facesContext, writer, uiComponent, behaviors);
            renderBehaviorizedFieldEventHandlersWithoutOnchange(facesContext, writer, uiComponent, behaviors);
            renderHTMLAttributes(
                    writer,
                    uiComponent,
                    HTML.SELECT_PASSTHROUGH_ATTRIBUTES_WITHOUT_DISABLED_AND_EVENTS);
        }
        else
        {
            renderHTMLAttributes(writer, uiComponent,
                    HTML.SELECT_PASSTHROUGH_ATTRIBUTES_WITHOUT_DISABLED);
        }

        if (disabled)
        {
            writer.writeAttribute(HTML.DISABLED_ATTR, Boolean.TRUE, null);
        }

        if (isReadOnly(uiComponent))
        {
            writer.writeAttribute(HTML.READONLY_ATTR, HTML.READONLY_ATTR, null);
        }

        Set lookupSet = getSubmittedOrSelectedValuesAsSet(selectMany,
                uiComponent, facesContext, converter);

        renderSelectOptions(facesContext, uiComponent, converter, lookupSet,
                selectItemList);
        // bug #970747: force separate end tag
        writer.writeText(STR_EMPTY, null);
        writer.endElement(HTML.SELECT_ELEM);
    }

    public static Set getSubmittedOrSelectedValuesAsSet(boolean selectMany,
            UIComponent uiComponent, FacesContext facesContext, Converter converter)
    {
        Set lookupSet;
        if (selectMany)
        {
            UISelectMany uiSelectMany = (UISelectMany) uiComponent;
            lookupSet = RendererUtils.getSubmittedValuesAsSet(facesContext,
                    uiComponent, converter, uiSelectMany);
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
                String lookupString = RendererUtils.getConvertedStringValue(
                        facesContext, uiComponent, converter, lookup);
                lookupSet = Collections.singleton(lookupString);
            }
            else if (STR_EMPTY.equals(lookup))
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

    public static Converter findUISelectManyConverterFailsafe(
            FacesContext facesContext, UIComponent uiComponent)
    {
        // invoke with considerValueType = false
        return findUISelectManyConverterFailsafe(facesContext, uiComponent, false);
    }

    public static Converter findUISelectManyConverterFailsafe(
            FacesContext facesContext, UIComponent uiComponent,
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
                            + uiComponent.getClientId(facesContext), e);
            converter = null;
        }
        return converter;
    }

    /**
     * Renders the select options for a <code>UIComponent</code> that is
     * rendered as an HTML select element.
     *
     * @param context        the current <code>FacesContext</code>.
     * @param component      the <code>UIComponent</code> whose options need to be
     *                       rendered.
     * @param converter      <code>component</code>'s converter
     * @param lookupSet      the <code>Set</code> to use to look up selected options
     * @param selectItemList the <code>List</code> of <code>SelectItem</code> s to be
     *                       rendered as HTML option elements.
     * @throws IOException
     */
    public static void renderSelectOptions(FacesContext context,
            UIComponent component, Converter converter, Set lookupSet,
            List selectItemList) throws IOException
    {
        ResponseWriter writer = context.getResponseWriter();
        // check for the hideNoSelectionOption attribute
        boolean hideNoSelectionOption = isHideNoSelectionOption(component);
        boolean componentDisabled = isTrue(component.getAttributes()
                .get("disabled"));

        for (Iterator it = selectItemList.iterator(); it.hasNext();)
        {
            SelectItem selectItem = (SelectItem) it.next();
            if (selectItem instanceof SelectItemGroup)
            {
                writer.startElement(HTML.OPTGROUP_ELEM, null); // component);
                writer.writeAttribute(HTML.LABEL_ATTR, selectItem.getLabel(),
                        null);
                SelectItem[] selectItems = ((SelectItemGroup) selectItem)
                        .getSelectItems();
                renderSelectOptions(context, component, converter, lookupSet,
                        Arrays.asList(selectItems));
                writer.endElement(HTML.OPTGROUP_ELEM);
            }
            else
            {
                String itemStrValue = org.apache.myfaces.shared.renderkit.RendererUtils
                        .getConvertedStringValue(context, component, converter,
                                selectItem);
                boolean selected = lookupSet.contains(itemStrValue); 
                //TODO/FIX: we always compare the String vales, better fill lookupSet with Strings 
                //only when useSubmittedValue==true, else use the real item value Objects

                // IF the hideNoSelectionOption attribute of the component is true
                // AND this selectItem is the "no selection option"
                // AND there are currently selected items 
                // AND this item (the "no selection option") is not selected
                // (if there is currently no value on UISelectOne, lookupSet contains "")
                if (hideNoSelectionOption && selectItem.isNoSelectionOption()
                        && lookupSet.size() != 0
                        && !(lookupSet.size() == 1 && lookupSet.contains(""))
                        && !selected)
                {
                    // do not render this selectItem
                    continue;
                }

                writer.write(TABULATOR);
                writer.startElement(HTML.OPTION_ELEM, null); // component);
                if (itemStrValue != null)
                {
                    writer.writeAttribute(HTML.VALUE_ATTR, itemStrValue, null);
                }
                else
                {
                    writer.writeAttribute(HTML.VALUE_ATTR, "", null);
                }

                if (selected)
                {
                    writer.writeAttribute(HTML.SELECTED_ATTR, HTML.SELECTED_ATTR, null);
                }

                boolean disabled = selectItem.isDisabled();
                if (disabled)
                {
                    writer.writeAttribute(HTML.DISABLED_ATTR, HTML.DISABLED_ATTR, null);
                }

                String labelClass = null;

                if (componentDisabled || disabled)
                {
                    labelClass = (String) component.getAttributes().get(
                            JSFAttr.DISABLED_CLASS_ATTR);
                }
                else
                {
                    labelClass = (String) component.getAttributes().get(
                            JSFAttr.ENABLED_CLASS_ATTR);
                }
                if (labelClass != null)
                {
                    writer.writeAttribute("class", labelClass, "labelClass");
                }

                boolean escape;
                if (component instanceof EscapeCapable)
                {
                    escape = ((EscapeCapable) component).isEscape();

                    // Preserve tomahawk semantic. If escape=false
                    // all items should be non escaped. If escape
                    // is true check if selectItem.isEscape() is
                    // true and do it.
                    // This is done for remain compatibility.
                    if (escape && selectItem.isEscape())
                    {
                        writer.writeText(selectItem.getLabel(), null);
                    }
                    else
                    {
                        writer.write(selectItem.getLabel());
                    }
                }
                else
                {
                    escape = RendererUtils.getBooleanAttribute(component,
                            JSFAttr.ESCAPE_ATTR, false);
                    //default is to escape
                    //In JSF 1.2, when a SelectItem is created by default 
                    //selectItem.isEscape() returns true (this property
                    //is not available on JSF 1.1).
                    //so, if we found a escape property on the component
                    //set to true, escape every item, but if not
                    //check if isEscape() = true first.
                    if (escape || selectItem.isEscape())
                    {
                        writer.writeText(selectItem.getLabel(), null);
                    }
                    else
                    {
                        writer.write(selectItem.getLabel());
                    }
                }

                writer.endElement(HTML.OPTION_ELEM);
            }
        }
    }

    /**
     * @return true, if the attribute was written
     * @throws java.io.IOException
     */
    public static boolean renderHTMLAttribute(ResponseWriter writer,
            String componentProperty, String attrName, Object value)
            throws IOException
    {
        if (!RendererUtils.isDefaultAttributeValue(value))
        {
            // render JSF "styleClass" and "itemStyleClass" attributes as "class"
            String htmlAttrName = attrName.equals(HTML.STYLE_CLASS_ATTR) ? HTML.CLASS_ATTR
                    : attrName;
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
        if (!org.apache.myfaces.shared.renderkit.RendererUtils
                .isDefaultAttributeValue(value))
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

    public static void writeIdIfNecessary(ResponseWriter writer,
            UIComponent component, FacesContext facesContext)
            throws IOException
    {
        if (component.getId() != null
                && !component.getId().startsWith(UIViewRoot.UNIQUE_ID_PREFIX))
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
        if (!isDefaultStringPreserveEmptyAttributeValue(value))
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
     * @param component
     * @param componentProperty
     * @param htmlAttrName
     * @return
     * @throws IOException
     */
    public static boolean renderHTMLStringPreserveEmptyAttribute(ResponseWriter writer,
            String componentProperty, String htmlAttrName, String value)
            throws IOException
    {
        if (!isDefaultStringPreserveEmptyAttributeValue(value))
        {
            writer.writeAttribute(htmlAttrName, value, componentProperty);
            return true;
        }
        return false;
    }

    /**
     * Check if the value is the default for String type attributes that requires preserve "" as
     * a valid value.
     * 
     * @param value
     * @return
     */
    private static boolean isDefaultStringPreserveEmptyAttributeValue(String value)
    {
        if (value == null)
        {
            return true;
        }
        else
        {
            return false;
        }
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
        if (!isDefaultStringAttributeValue(value))
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
        if (!isDefaultStringAttributeValue(value))
        {
            writer.writeAttribute(htmlAttrName, value, componentProperty);
            return true;
        }
        return false;
    }
    
    /**
     * Check if the value is the default for String type attributes (null or "").
     * 
     * @param value
     * @return
     */
    private static boolean isDefaultStringAttributeValue(String value)
    {
        if (value == null)
        {
            return true;
        }
        else if (value.length() == 0)
        {
            return true;
        }
        else
        {
            return false;
        }
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

    public static void renderDisplayValueOnlyForSelects(
            FacesContext facesContext, UIComponent uiComponent)
            throws IOException
    {
        // invoke renderDisplayValueOnlyForSelects with considerValueType = false
        renderDisplayValueOnlyForSelects(facesContext, uiComponent, false);
    }

    public static void renderDisplayValueOnlyForSelects(FacesContext facesContext, UIComponent uiComponent,
            boolean considerValueType) throws IOException
    {
        ResponseWriter writer = facesContext.getResponseWriter();

        List selectItemList = null;
        Converter converter = null;
        boolean isSelectOne = false;

        if (uiComponent instanceof UISelectBoolean)
        {
            converter = findUIOutputConverterFailSafe(facesContext, uiComponent);

            writer.startElement(HTML.SPAN_ELEM, uiComponent);
            writeIdIfNecessary(writer, uiComponent, facesContext);
            renderDisplayValueOnlyAttributes(uiComponent, writer);
            writer.writeText(RendererUtils.getConvertedStringValue(
                    facesContext, uiComponent, converter,
                    ((UISelectBoolean) uiComponent).getValue()),
                    JSFAttr.VALUE_ATTR);
            writer.endElement(HTML.SPAN_ELEM);

        }
        else
        {
            if (uiComponent instanceof UISelectMany)
            {
                isSelectOne = false;
                selectItemList = RendererUtils.getSelectItemList(
                        (UISelectMany) uiComponent, facesContext);
                converter = findUISelectManyConverterFailsafe(facesContext,
                        uiComponent, considerValueType);
            }
            else if (uiComponent instanceof UISelectOne)
            {
                isSelectOne = true;
                selectItemList = RendererUtils.getSelectItemList(
                        (UISelectOne) uiComponent, facesContext);
                converter = findUIOutputConverterFailSafe(facesContext,
                        uiComponent);
            }

            writer.startElement(isSelectOne ? HTML.SPAN_ELEM : HTML.UL_ELEM, uiComponent);
            writeIdIfNecessary(writer, uiComponent, facesContext);

            renderDisplayValueOnlyAttributes(uiComponent, writer);

            Set lookupSet = getSubmittedOrSelectedValuesAsSet(
                    uiComponent instanceof UISelectMany, uiComponent,
                    facesContext, converter);

            renderSelectOptionsAsText(facesContext, uiComponent, converter,
                    lookupSet, selectItemList, isSelectOne);

            // bug #970747: force separate end tag
            writer.writeText(STR_EMPTY, null);
            writer.endElement(isSelectOne ? HTML.SPAN_ELEM : HTML.UL_ELEM);
        }

    }

    public static void renderDisplayValueOnlyAttributes(
            UIComponent uiComponent, ResponseWriter writer) throws IOException
    {
        if (!(uiComponent instanceof org.apache.myfaces.shared.component.DisplayValueOnlyCapable))
        {
            log.severe("Wrong type of uiComponent. needs DisplayValueOnlyCapable.");
            renderHTMLAttributes(writer, uiComponent, HTML.COMMON_PASSTROUGH_ATTRIBUTES);

            return;
        }

        if (getDisplayValueOnlyStyle(uiComponent) != null
                || getDisplayValueOnlyStyleClass(uiComponent) != null)
        {
            if (getDisplayValueOnlyStyle(uiComponent) != null)
            {
                writer.writeAttribute(HTML.STYLE_ATTR, getDisplayValueOnlyStyle(uiComponent), null);
            }
            else if (uiComponent.getAttributes().get("style") != null)
            {
                writer.writeAttribute(HTML.STYLE_ATTR, uiComponent.getAttributes().get("style"), null);
            }

            if (getDisplayValueOnlyStyleClass(uiComponent) != null)
            {
                writer.writeAttribute(HTML.CLASS_ATTR, getDisplayValueOnlyStyleClass(uiComponent), null);
            }
            else if (uiComponent.getAttributes().get("styleClass") != null)
            {
                writer.writeAttribute(HTML.CLASS_ATTR, uiComponent.getAttributes().get("styleClass"), null);
            }

            renderHTMLAttributes(writer, uiComponent, HTML.COMMON_PASSTROUGH_ATTRIBUTES_WITHOUT_STYLE);
        }
        else
        {
            renderHTMLAttributes(writer, uiComponent, HTML.COMMON_PASSTROUGH_ATTRIBUTES);
        }
    }

    private static void renderSelectOptionsAsText(FacesContext context,
            UIComponent component, Converter converter, Set lookupSet,
            List selectItemList, boolean isSelectOne) throws IOException
    {
        ResponseWriter writer = context.getResponseWriter();

        for (Iterator it = selectItemList.iterator(); it.hasNext();)
        {
            SelectItem selectItem = (SelectItem) it.next();

            if (selectItem instanceof SelectItemGroup)
            {
                SelectItem[] selectItems = ((SelectItemGroup) selectItem).getSelectItems();
                renderSelectOptionsAsText(context, component, converter,
                        lookupSet, Arrays.asList(selectItems), isSelectOne);
            }
            else
            {
                String itemStrValue = RendererUtils.getConvertedStringValue(
                        context, component, converter, selectItem);

                if (lookupSet.contains(itemStrValue))
                {
                    //TODO/FIX: we always compare the String vales, better fill lookupSet with Strings 
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

    public static void renderTableCaption(FacesContext context,
            ResponseWriter writer, UIComponent component) throws IOException
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
            captionClass = (String) component.getAttributes()
                    .get(org.apache.myfaces.shared.renderkit.JSFAttr.CAPTION_CLASS_ATTR);
            captionStyle = (String) component.getAttributes()
                    .get(org.apache.myfaces.shared.renderkit.JSFAttr.CAPTION_STYLE_ATTR);
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

    public static String getDisplayValueOnlyStyleClass(UIComponent component)
    {
        if (component instanceof org.apache.myfaces.shared.component.DisplayValueOnlyCapable)
        {
            if (((org.apache.myfaces.shared.component.DisplayValueOnlyCapable) component)
                    .getDisplayValueOnlyStyleClass() != null)
            {
                return ((DisplayValueOnlyCapable) component)
                        .getDisplayValueOnlyStyleClass();
            }
            UIComponent parent = component;
            while ((parent = parent.getParent()) != null)
            {
                if (parent instanceof org.apache.myfaces.shared.component.DisplayValueOnlyCapable
                        && ((org.apache.myfaces.shared.component.DisplayValueOnlyCapable) parent)
                                .getDisplayValueOnlyStyleClass() != null)
                {
                    return ((org.apache.myfaces.shared.component.DisplayValueOnlyCapable) parent)
                            .getDisplayValueOnlyStyleClass();
                }
            }
        }
        return null;
    }

    public static String getDisplayValueOnlyStyle(UIComponent component)
    {
        if (component instanceof DisplayValueOnlyCapable)
        {
            if (((org.apache.myfaces.shared.component.DisplayValueOnlyCapable) component)
                    .getDisplayValueOnlyStyle() != null)
            {
                return ((DisplayValueOnlyCapable) component)
                        .getDisplayValueOnlyStyle();
            }
            UIComponent parent = component;
            while ((parent = parent.getParent()) != null)
            {
                if (parent instanceof org.apache.myfaces.shared.component.DisplayValueOnlyCapable
                        && ((DisplayValueOnlyCapable) parent)
                                .getDisplayValueOnlyStyle() != null)
                {
                    return ((DisplayValueOnlyCapable) parent)
                            .getDisplayValueOnlyStyle();
                }
            }
        }
        return null;
    }

    public static boolean isDisplayValueOnly(UIComponent component)
    {
        if (component instanceof DisplayValueOnlyCapable)
        {
            if (((DisplayValueOnlyCapable) component).isSetDisplayValueOnly())
            {
                return ((DisplayValueOnlyCapable) component).isDisplayValueOnly();
            }
            UIComponent parent = component;
            while ((parent = parent.getParent()) != null)
            {
                if (parent instanceof DisplayValueOnlyCapable
                        && ((DisplayValueOnlyCapable) parent).isSetDisplayValueOnly())
                {
                    return ((org.apache.myfaces.shared.component.DisplayValueOnlyCapable) parent)
                            .isDisplayValueOnly();
                }
            }
        }
        return false;
    }

    public static void renderDisplayValueOnly(FacesContext facesContext,
            UIInput input) throws IOException
    {
        ResponseWriter writer = facesContext.getResponseWriter();
        writer.startElement(org.apache.myfaces.shared.renderkit.html.HTML.SPAN_ELEM, input);
        writeIdIfNecessary(writer, input, facesContext);
        renderDisplayValueOnlyAttributes(input, writer);
        String strValue = RendererUtils.getStringValue(facesContext, input);
        writer.write(HTMLEncoder.encode(strValue, true, true));
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

    /**
     * Adds the hidden form input value assignment that is necessary for the autoscroll
     * feature to an html link or button onclick attribute.
     */
    public static void appendAutoScrollAssignment(StringBuilder onClickValue,
            String formName)
    {
        HtmlJavaScriptUtils.appendAutoScrollAssignment(onClickValue, formName);
    }

    /**
     * Adds the hidden form input value assignment that is necessary for the autoscroll
     * feature to an html link or button onclick attribute.
     */
    public static void appendAutoScrollAssignment(FacesContext context,
            StringBuilder onClickValue, String formName)
    {
        HtmlJavaScriptUtils.appendAutoScrollAssignment(context, onClickValue, formName);
    }

    /**
     * Renders the hidden form input that is necessary for the autoscroll feature.
     */
    public static void renderAutoScrollHiddenInput(FacesContext facesContext,
            ResponseWriter writer) throws IOException
    {
        HtmlJavaScriptUtils.renderAutoScrollHiddenInput(facesContext, writer);
    }

    /**
     * Renders the autoscroll javascript function.
     */
    public static void renderAutoScrollFunction(FacesContext facesContext,
            ResponseWriter writer) throws IOException
    {
        HtmlJavaScriptUtils.renderAutoScrollFunction(facesContext, writer);
    }

    public static String getAutoScrollFunction(FacesContext facesContext)
    {
        return HtmlJavaScriptUtils.getAutoScrollFunction(facesContext);
    }

    public static boolean isAllowedCdataSection(FacesContext fc)
    {
        Boolean value = null;
        if (fc != null)
        {
            value = (Boolean) fc.getExternalContext().getRequestMap().get(ALLOW_CDATA_SECTION_ON);
        }
        return value != null && ((Boolean) value).booleanValue();
    }

    public static void allowCdataSection(FacesContext fc, boolean cdataSectionAllowed)
    {
        fc.getExternalContext().getRequestMap().put(ALLOW_CDATA_SECTION_ON, Boolean.valueOf(cdataSectionAllowed));
    }

    public static class LinkParameter
    {
        private String _name;

        private Object _value;

        public String getName()
        {
            return _name;
        }

        public void setName(String name)
        {
            _name = name;
        }

        public Object getValue()
        {
            return _value;
        }

        public void setValue(Object value)
        {
            _value = value;
        }
    }

    public static void renderHiddenCommandFormParams(ResponseWriter writer,
            Set dummyFormParams) throws IOException
    {
        for (Iterator it = dummyFormParams.iterator(); it.hasNext();)
        {
            Object name = it.next();
            renderHiddenInputField(writer, name, null);
        }
    }

    public static void renderHiddenInputField(ResponseWriter writer,
            Object name, Object value) throws IOException
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
     * @deprecated Replaced by
     *             renderLabel(ResponseWriter writer,
     *             UIComponent component,
     *             String forClientId,
     *             SelectItem item,
     *             boolean disabled).
     *             Renders a label HTML element
     */
    @Deprecated
    public static void renderLabel(ResponseWriter writer,
            UIComponent component, String forClientId, String labelValue,
            boolean disabled) throws IOException
    {
        writer.startElement(HTML.LABEL_ELEM, null); // component);
        writer.writeAttribute(HTML.FOR_ATTR, forClientId, null);
        String labelClass = null;
        if (disabled)
        {
            labelClass = (String) component.getAttributes().get(JSFAttr.DISABLED_CLASS_ATTR);
        }
        else
        {
            labelClass = (String) component.getAttributes()
                    .get(org.apache.myfaces.shared.renderkit.JSFAttr.ENABLED_CLASS_ATTR);
        }
        if (labelClass != null)
        {
            writer.writeAttribute("class", labelClass, "labelClass");
        }
        if ((labelValue != null) && (labelValue.length() > 0))
        {
            writer.write(HTML.NBSP_ENTITY);
            writer.writeText(labelValue, null);
        }
        writer.endElement(HTML.LABEL_ELEM);
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
            labelClass = (String) component.getAttributes().get(JSFAttr.DISABLED_CLASS_ATTR);
        }
        else
        {
            labelClass = (String) component.getAttributes()
                    .get(org.apache.myfaces.shared.renderkit.JSFAttr.ENABLED_CLASS_ATTR);
        }
        if (labelClass != null)
        {
            writer.writeAttribute("class", labelClass, "labelClass");
        }
        if ((item.getLabel() != null) && (item.getLabel().length() > 0))
        {
            // writer.write(HTML.NBSP_ENTITY);
            writer.write(" ");
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
            labelClass = (String) component.getAttributes().get(JSFAttr.DISABLED_CLASS_ATTR);
        }
        else
        {
            labelClass = (String) component.getAttributes()
                    .get(org.apache.myfaces.shared.renderkit.JSFAttr.ENABLED_CLASS_ATTR);
        }
        String labelSelectedClass = null;
        if (selected)
        {
            labelSelectedClass = (String) component.getAttributes().get(JSFAttr.SELECTED_CLASS_ATTR);
        }
        else
        {
            labelSelectedClass = (String) component.getAttributes().get(JSFAttr.UNSELECTED_CLASS_ATTR);
        }
        if (labelSelectedClass != null)
        {
            if (labelClass == null)
            {
                labelClass = labelSelectedClass;
            }
            else
            {
                labelClass = labelClass + " " + labelSelectedClass;
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
     * Render the javascript function that is called on a click on a commandLink
     * to clear the hidden inputs. This is necessary because on a browser back,
     * each hidden input still has it's old value (browser cache!) and therefore
     * a new submit would cause the according action once more!
     *
     * @param writer
     * @param formName
     * @param dummyFormParams
     * @param formTarget
     * @throws IOException
     */
    public static void renderClearHiddenCommandFormParamsFunction(
            ResponseWriter writer, String formName, Set dummyFormParams,
            String formTarget) throws IOException
    {
        HtmlJavaScriptUtils.renderClearHiddenCommandFormParamsFunction(writer, formName, dummyFormParams, formTarget);
    }

    /**
     * Prefixes the given String with "clear_" and removes special characters
     *
     * @param formName
     * @return String
     */
    public static String getClearHiddenCommandFormParamsFunctionName(
            String formName)
    {
        return HtmlJavaScriptUtils.getClearHiddenCommandFormParamsFunctionName(formName);
    }

    public static String getClearHiddenCommandFormParamsFunctionNameMyfacesLegacy(
            String formName)
    {
        return HtmlJavaScriptUtils.getClearHiddenCommandFormParamsFunctionNameMyfacesLegacy(formName);
    }

    /**
     * Get the name of the request parameter that holds the id of the
     * link-type component that caused the form to be submitted.
     * <p/>
     * Within each page there may be multiple "link" type components that
     * cause page submission. On the server it is necessary to know which
     * of these actually caused the submit, in order to invoke the correct
     * listeners. Such components therefore store their id into the
     * "hidden command link field" in their associated form before
     * submitting it.
     * <p/>
     * The field is always a direct child of each form, and has the same
     * <i>name</i> in each form. The id of the form component is therefore
     * both necessary and sufficient to determine the full name of the
     * field.
     */
    public static String getHiddenCommandLinkFieldName(FormInfo formInfo)
    {
        if (RendererUtils.isAdfOrTrinidadForm(formInfo.getForm()))
        {
            return HIDDEN_COMMANDLINK_FIELD_NAME_TRINIDAD;
        }
        return formInfo.getFormName() + ':' 
                + HIDDEN_COMMANDLINK_FIELD_NAME;
    }
    
    public static String getHiddenCommandLinkFieldName(
            FormInfo formInfo, FacesContext facesContext)
    {
        if (RendererUtils.isAdfOrTrinidadForm(formInfo.getForm()))
        {
            return HIDDEN_COMMANDLINK_FIELD_NAME_TRINIDAD;
        }
        return formInfo.getFormName() + ':'
                + HIDDEN_COMMANDLINK_FIELD_NAME;
    }

    public static boolean isPartialOrBehaviorSubmit(FacesContext facesContext,
            String clientId)
    {
        Map<String, String> params = facesContext.getExternalContext().getRequestParameterMap();
        String sourceId = params.get("javax.faces.source");
        if (sourceId == null || !sourceId.equals(clientId))
        {
            return false;
        }
        boolean partialOrBehaviorSubmit = false;
        String behaviorEvent = params.get("javax.faces.behavior.event");
        if (behaviorEvent != null)
        {
            partialOrBehaviorSubmit = ClientBehaviorEvents.ACTION.equals(behaviorEvent);
            if (partialOrBehaviorSubmit)
            {
                return partialOrBehaviorSubmit;
            }
        }
        String partialEvent = params.get("javax.faces.partial.event");
        if (partialEvent != null)
        {
            partialOrBehaviorSubmit = ClientBehaviorEvents.CLICK.equals(partialEvent);
        }
        return partialOrBehaviorSubmit;
    }

    public static String getHiddenCommandLinkFieldNameMyfacesOld(
            FormInfo formInfo)
    {
        return formInfo.getFormName() + ':'
                + HIDDEN_COMMANDLINK_FIELD_NAME_MYFACES_OLD;
    }

    public static String getOutcomeTargetHref(FacesContext facesContext,
            UIOutcomeTarget component) throws IOException
    {
        return OutcomeTargetUtils.getOutcomeTargetHref(facesContext, component);
    }

    private static final String HTML_CONTENT_TYPE = "text/html";
    private static final String TEXT_ANY_CONTENT_TYPE = "text/*";
    private static final String ANY_CONTENT_TYPE = "*/*";
    public static final String DEFAULT_CHAR_ENCODING = "ISO-8859-1";
    private static final String XHTML_CONTENT_TYPE = "application/xhtml+xml";
    private static final String APPLICATION_XML_CONTENT_TYPE = "application/xml";
    private static final String TEXT_XML_CONTENT_TYPE = "text/xml";
    // The order is important in this case.
    private static final String[] SUPPORTED_CONTENT_TYPES = {
            HTML_CONTENT_TYPE, //Prefer this over any other, because IE does not support XHTML content type
            XHTML_CONTENT_TYPE, APPLICATION_XML_CONTENT_TYPE,
            TEXT_XML_CONTENT_TYPE, TEXT_ANY_CONTENT_TYPE, ANY_CONTENT_TYPE };
    /**
     * @deprecated use ContentTypeUtils instead
     */
    @Deprecated
    public static String selectContentType(String contentTypeListString)
    {
        if (contentTypeListString == null)
        {
            FacesContext context = FacesContext.getCurrentInstance();
            if (context != null)
            {
                contentTypeListString = (String) context.getExternalContext()
                        .getRequestHeaderMap().get("Accept");
                // There is a windows mobile IE client (6.12) sending
                // "application/vnd.wap.mms-message;*/*"
                // Note that the Accept header should be written as 
                // "application/vnd.wap.mms-message,*/*" ,
                // so this is bug of the client. Anyway, this is a workaround ...
                if (contentTypeListString != null
                        && contentTypeListString.startsWith("application/vnd.wap.mms-message;*/*"))
                {
                    contentTypeListString = "*/*";
                }
            }
            if (contentTypeListString == null)
            {
                if (log.isLoggable(Level.FINE))
                {
                    log.fine("No content type list given, creating HtmlResponseWriterImpl with default content type.");
                }
                contentTypeListString = HTML_CONTENT_TYPE;
            }
        }
        List contentTypeList = splitContentTypeListString(contentTypeListString);
        String[] supportedContentTypeArray = getSupportedContentTypes();
        String selectedContentType = null;
        for (int i = 0; i < supportedContentTypeArray.length; i++)
        {
            String supportedContentType = supportedContentTypeArray[i].trim();

            for (int j = 0; j < contentTypeList.size(); j++)
            {
                String contentType = (String) contentTypeList.get(j);

                if (contentType.indexOf(supportedContentType) != -1)
                {
                    if (isHTMLContentType(contentType))
                    {
                        selectedContentType = HTML_CONTENT_TYPE;
                    }
                    else if (isXHTMLContentType(contentType))
                    {
                        selectedContentType = XHTML_CONTENT_TYPE;
                    }
                    break;
                }
            }
            if (selectedContentType != null)
            {
                break;
            }
        }
        if (selectedContentType == null)
        {
            throw new IllegalArgumentException(
                    "ContentTypeList does not contain a supported content type: "
                            + contentTypeListString);
        }
        return selectedContentType;
    }

    public static String[] getSupportedContentTypes()
    {
        //String[] supportedContentTypeArray = new String[]{
        // HTML_CONTENT_TYPE,TEXT_ANY_CONTENT_TYPE,ANY_CONTENT_TYPE,
        // XHTML_CONTENT_TYPE,APPLICATION_XML_CONTENT_TYPE,TEXT_XML_CONTENT_TYPE};
        return SUPPORTED_CONTENT_TYPES;
    }

    private static boolean isHTMLContentType(String contentType)
    {
        return contentType.indexOf(HTML_CONTENT_TYPE) != -1
                || contentType.indexOf(ANY_CONTENT_TYPE) != -1
                || contentType.indexOf(TEXT_ANY_CONTENT_TYPE) != -1;
    }

    public static boolean isXHTMLContentType(String contentType)
    {
        return contentType.indexOf(XHTML_CONTENT_TYPE) != -1
                || contentType.indexOf(APPLICATION_XML_CONTENT_TYPE) != -1
                || contentType.indexOf(TEXT_XML_CONTENT_TYPE) != -1;
    }

    private static List splitContentTypeListString(String contentTypeListString)
    {
        List contentTypeList = new ArrayList();
        StringTokenizer st = new StringTokenizer(contentTypeListString, ",");
        while (st.hasMoreTokens())
        {
            String contentType = st.nextToken().trim();
            int semicolonIndex = contentType.indexOf(";");
            if (semicolonIndex != -1)
            {
                contentType = contentType.substring(0, semicolonIndex);
            }
            contentTypeList.add(contentType);
        }
        return contentTypeList;
    }

    public static String getJavascriptLocation(UIComponent component)
    {
        if (component == null)
        {
            return null;
        }
        return (String) component.getAttributes().get(JSFAttr.JAVASCRIPT_LOCATION);
    }

    public static String getImageLocation(UIComponent component)
    {
        if (component == null)
        {
            return null;
        }
        return (String) component.getAttributes().get(JSFAttr.IMAGE_LOCATION);
    }

    public static String getStyleLocation(UIComponent component)
    {
        if (component == null)
        {
            return null;
        }
        return (String) component.getAttributes().get(JSFAttr.STYLE_LOCATION);
    }

    /**
     * Checks if the given component has a behavior attachment with a given name.
     *
     * @param eventName the event name to be checked for
     * @param behaviors map of behaviors attached to the component
     * @return true if client behavior with given name is attached, false otherwise
     * @since 4.0.0
     */
    public static boolean hasClientBehavior(String eventName,
            Map<String, List<ClientBehavior>> behaviors,
            FacesContext facesContext)
    {
        if (behaviors == null)
        {
            return false;
        }
        return (behaviors.get(eventName) != null);
    }

    public static Collection<ClientBehaviorContext.Parameter> getClientBehaviorContextParameters(
            Map<String, String> params)
    {
        List<ClientBehaviorContext.Parameter> paramList = null;
        if (params != null)
        {
            paramList = new ArrayList<ClientBehaviorContext.Parameter>(params.size());
            for (Map.Entry<String, String> paramEntry : params.entrySet())
            {
                paramList.add(new ClientBehaviorContext.Parameter(paramEntry
                        .getKey(), paramEntry.getValue()));
            }
        }
        return paramList;
    }

    /**
     * builds the chained behavior script which then can be reused
     * in following order by the other script building parts
     * <p/>
     * user defined event handling script
     * behavior script
     * renderer default script
     *
     * @param eventName    event name ("onclick" etc...)
     * @param uiComponent  the component which has the attachement (or should have)
     * @param facesContext the facesContext
     * @param params       params map of params which have to be dragged into the request
     * @return a string representation of the javascripts for the attached event behavior,
     *         an empty string if none is present
     * @since 4.0.0
     */
    private static boolean getClientBehaviorScript(FacesContext facesContext,
            UIComponent uiComponent, String targetClientId, String eventName,
            Map<String, List<ClientBehavior>> clientBehaviors,
            ScriptContext target,
            Collection<ClientBehaviorContext.Parameter> params)
    {
        if (!(uiComponent instanceof ClientBehaviorHolder))
        {
            target.append(STR_EMPTY);
            return false;
        }
        boolean renderClientBehavior = clientBehaviors != null && clientBehaviors.size() > 0;
        if (!renderClientBehavior)
        {
            target.append(STR_EMPTY);
            return false;
        }
        List<ClientBehavior> attachedEventBehaviors = clientBehaviors
                .get(eventName);
        if (attachedEventBehaviors == null
                || attachedEventBehaviors.size() == 0)
        {
            target.append(STR_EMPTY);
            return false;
        }
        ClientBehaviorContext context = ClientBehaviorContext
                .createClientBehaviorContext(facesContext, uiComponent,
                        eventName, targetClientId, params);
        boolean submitting = false;
        
        // List<ClientBehavior>  attachedEventBehaviors is  99% _DeltaList created in
        // javax.faces.component.UIComponentBase.addClientBehavior
        if (attachedEventBehaviors instanceof RandomAccess)
        {
            for (int i = 0, size = attachedEventBehaviors.size(); i < size; i++)
            {
                ClientBehavior clientBehavior = attachedEventBehaviors.get(i);
                submitting =  _appendClientBehaviourScript(target, context, 
                        submitting, i < (size -1), clientBehavior);   
            }
        }
        else 
        {
            Iterator<ClientBehavior> clientIterator = attachedEventBehaviors.iterator();
            while (clientIterator.hasNext())
            {
                ClientBehavior clientBehavior = clientIterator.next();
                submitting = _appendClientBehaviourScript(target, context, submitting, 
                        clientIterator.hasNext(), clientBehavior);
            }
        }
        
        return submitting;
    }

    private static boolean _appendClientBehaviourScript(ScriptContext target, ClientBehaviorContext context, 
            boolean submitting, boolean hasNext, ClientBehavior clientBehavior)
    {
        String script = clientBehavior.getScript(context);
        // The script _can_ be null, and in fact is for <f:ajax disabled="true" />
        if (script != null)
        {
            //either strings or functions, but I assume string is more appropriate 
            //since it allows access to the
            //origin as this!
            target.append("'" + escapeJavaScriptForChain(script) + "'");
            if (hasNext)
            {
                target.append(", ");
            }
        }
        // MYFACES-3836 If no script provided by the client behavior, ignore the 
        // submitting hint because. it is evidence the client behavior is disabled.
        if (script != null && !submitting)
        {
            submitting = clientBehavior.getHints().contains(
                    ClientBehaviorHint.SUBMITTING);
        }
        return submitting;
    }

    /**
     * @since 4.0.0
     */
    public static String buildBehaviorChain(FacesContext facesContext,
            UIComponent uiComponent, String eventName,
            Collection<ClientBehaviorContext.Parameter> params,
            Map<String, List<ClientBehavior>> clientBehaviors,
            String userEventCode, String serverEventCode)
    {
        return buildBehaviorChain(facesContext, uiComponent,
                uiComponent.getClientId(facesContext), eventName, params,
                clientBehaviors, userEventCode, serverEventCode);
    }

    public static String buildBehaviorChain(FacesContext facesContext,
            UIComponent uiComponent, String targetClientId, String eventName,
            Collection<ClientBehaviorContext.Parameter> params,
            Map<String, List<ClientBehavior>> clientBehaviors,
            String userEventCode, String serverEventCode)
    {
        List<String> finalParams = new ArrayList<String>(3);
        if (userEventCode != null && !userEventCode.trim().equals(STR_EMPTY))
        {
            // escape every ' in the user event code since it will
            // be a string attribute of jsf.util.chain
            finalParams.add('\'' + escapeJavaScriptForChain(userEventCode) + '\'');
        }
        ScriptContext behaviorCode = new ScriptContext();
        ScriptContext retVal = new ScriptContext();
        getClientBehaviorScript(facesContext, uiComponent, targetClientId,
                eventName, clientBehaviors, behaviorCode, params);
        if (behaviorCode != null
                && !behaviorCode.toString().trim().equals(STR_EMPTY))
        {
            finalParams.add(behaviorCode.toString());
        }
        if (serverEventCode != null
                && !serverEventCode.trim().equals(STR_EMPTY))
        {
            finalParams
                    .add('\'' + escapeJavaScriptForChain(serverEventCode) + '\'');
        }
        Iterator<String> it = finalParams.iterator();
        // It's possible that there are no behaviors to render.  For example, if we have
        // <f:ajax disabled="true" /> as the only behavior.
        if (it.hasNext())
        {
            //according to the spec jsf.util.chain has to be used to build up the 
            //behavior and scripts
            retVal.append("jsf.util.chain(document.getElementById('"
                    + targetClientId + "'), event,");
            while (it.hasNext())
            {
                retVal.append(it.next());
                if (it.hasNext())
                {
                    retVal.append(", ");
                }
            }
            retVal.append(");");
        }

        return retVal.toString();
    }

    /**
     * @param facesContext
     * @param uiComponent
     * @param clientBehaviors
     * @param eventName1
     * @param eventName2
     * @param userEventCode
     * @param serverEventCode
     * @param params
     * @return
     * @since 4.0.0
     */
    public static String buildBehaviorChain(FacesContext facesContext,
            UIComponent uiComponent, String eventName1,
            Collection<ClientBehaviorContext.Parameter> params,
            String eventName2,
            Collection<ClientBehaviorContext.Parameter> params2,
            Map<String, List<ClientBehavior>> clientBehaviors,
            String userEventCode, String serverEventCode)
    {
        return buildBehaviorChain(facesContext, uiComponent,
                uiComponent.getClientId(facesContext), eventName1, params,
                eventName2, params2, clientBehaviors, userEventCode,
                serverEventCode);
    }

    public static String buildBehaviorChain(FacesContext facesContext,
            UIComponent uiComponent, String targetClientId, String eventName1,
            Collection<ClientBehaviorContext.Parameter> params,
            String eventName2,
            Collection<ClientBehaviorContext.Parameter> params2,
            Map<String, List<ClientBehavior>> clientBehaviors,
            String userEventCode, String serverEventCode)
    {
        ExternalContext externalContext = facesContext.getExternalContext();
        List<String> finalParams = new ArrayList<String>(3);
        if (userEventCode != null && !userEventCode.trim().equals(STR_EMPTY))
        {
            finalParams.add('\'' + escapeJavaScriptForChain(userEventCode) + '\'');
        }

        ScriptContext behaviorCode = new ScriptContext();
        ScriptContext retVal = new ScriptContext();
        boolean submitting1 = getClientBehaviorScript(facesContext,
                uiComponent, targetClientId, eventName1, clientBehaviors,
                behaviorCode, params);
        ScriptContext behaviorCode2 = new ScriptContext();
        boolean submitting2 = getClientBehaviorScript(facesContext,
                uiComponent, targetClientId, eventName2, clientBehaviors,
                behaviorCode2, params2);

        // ClientBehaviors for both events have to be checked for the Submitting hint
        boolean submitting = submitting1 || submitting2;
        if (behaviorCode != null
                && !behaviorCode.toString().trim().equals(STR_EMPTY))
        {
            finalParams.add(behaviorCode.toString());
        }
        if (behaviorCode2 != null
                && !behaviorCode2.toString().trim().equals(STR_EMPTY))
        {
            finalParams.add(behaviorCode2.toString());
        }
        if (serverEventCode != null
                && !serverEventCode.trim().equals(STR_EMPTY))
        {
            finalParams.add('\'' + escapeJavaScriptForChain(serverEventCode) + '\'');
        }
        
        // It's possible that there are no behaviors to render.  For example, if we have
        // <f:ajax disabled="true" /> as the only behavior.
        
        int size = finalParams.size();
        if (size > 0)
        {
            if (!submitting)
            {
                retVal.append("return ");
            }
            //according to the spec jsf.util.chain has to be used to build up the 
            //behavior and scripts
            retVal.append("jsf.util.chain(document.getElementById('"
                    + targetClientId + "'), event,");
            int cursor = 0;
            while (cursor != size)
            {
                retVal.append(finalParams.get(cursor));
                cursor++;
                if (cursor != size)
                {
                    retVal.append(", ");
                }
            }
            retVal.append(");");
            if (submitting)
            {
                retVal.append(" return false;");
            }
        }

        return retVal.toString();

    }

    /**
     * This function correctly escapes the given JavaScript code
     * for the use in the jsf.util.chain() JavaScript function.
     * It also handles double-escaping correclty.
     *
     * @param javaScript
     * @return
     */
    public static String escapeJavaScriptForChain(String javaScript)
    {
        return HtmlJavaScriptUtils.escapeJavaScriptForChain(javaScript);
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
                    retVal = new HashMap<String, String>();
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
     * @return ArrayList size > 0 if any parameter found
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
     * @return ArrayList size > 0 if any parameter found 
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
     * @since 4.0.1
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
            String targetClientId, String eventName,
            Map<String, List<ClientBehavior>> clientBehaviors)
            throws IOException
    {
        return renderBehaviorizedAttribute(facesContext, writer,
                componentProperty, component, targetClientId, eventName, clientBehaviors, componentProperty);
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
     * @since 4.0.1
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
            String targetClientId, String eventName, Map<String, List<ClientBehavior>> clientBehaviors,
            String htmlAttrName) throws IOException
    {
        return renderBehaviorizedAttribute(facesContext, writer,
                componentProperty, component, targetClientId, eventName, null,
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
                component.getClientId(facesContext), eventName,
                eventParameters, clientBehaviors, htmlAttrName, attributeValue);
    }

    public static boolean renderBehaviorizedAttribute(
            FacesContext facesContext, ResponseWriter writer,
            String componentProperty, UIComponent component,
            String targetClientId, String eventName,
            Collection<ClientBehaviorContext.Parameter> eventParameters,
            Map<String, List<ClientBehavior>> clientBehaviors,
            String htmlAttrName, String attributeValue) throws IOException
    {

        List<ClientBehavior> cbl = (clientBehaviors != null) ? clientBehaviors
                .get(eventName) : null;
        if (cbl == null || cbl.size() == 0)
        {
            return renderHTMLAttribute(writer, componentProperty, htmlAttrName,
                    attributeValue);
        }
        if (cbl.size() > 1 || (cbl.size() == 1 && attributeValue != null))
        {
            return renderHTMLAttribute(writer, componentProperty, htmlAttrName,
                    HtmlRendererUtils.buildBehaviorChain(facesContext,
                            component, targetClientId, eventName,
                            eventParameters, clientBehaviors, attributeValue,
                            STR_EMPTY));
        }
        else
        {
            //Only 1 behavior and attrValue == null, so just render it directly
            return renderHTMLAttribute(
                    writer, componentProperty, htmlAttrName,
                    cbl.get(0).getScript(
                            ClientBehaviorContext.createClientBehaviorContext(
                                    facesContext, component, eventName,
                                    targetClientId, eventParameters)));
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
                component.getClientId(facesContext), eventName,
                eventParameters, clientBehaviors, htmlAttrName, attributeValue,
                serverSideScript);
    }

    // CHECKSTYLE:OFF
    public static boolean renderBehaviorizedAttribute(
            FacesContext facesContext, ResponseWriter writer,
            String componentProperty, UIComponent component,
            String targetClientId, String eventName,
            Collection<ClientBehaviorContext.Parameter> eventParameters,
            Map<String, List<ClientBehavior>> clientBehaviors,
            String htmlAttrName, String attributeValue, String serverSideScript)
            throws IOException
    {

        List<ClientBehavior> cbl = (clientBehaviors != null) ? clientBehaviors
                .get(eventName) : null;
        if (((cbl != null) ? cbl.size() : 0) + (attributeValue != null ? 1 : 0)
                + (serverSideScript != null ? 1 : 0) <= 1)
        {
            if (cbl == null || cbl.size() == 0)
            {
                if (attributeValue != null)
                {
                    return renderHTMLStringAttribute(writer, componentProperty,
                            htmlAttrName, attributeValue);
                }
                else
                {
                    return renderHTMLStringAttribute(writer, componentProperty,
                            htmlAttrName, serverSideScript);
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
                                                eventName, targetClientId,
                                                eventParameters)));
            }
        }
        else
        {
            return renderHTMLStringAttribute(writer, componentProperty, htmlAttrName,
                    HtmlRendererUtils.buildBehaviorChain(facesContext,
                            component, targetClientId, eventName,
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
                component.getClientId(facesContext), eventName,
                eventParameters, eventName2, eventParameters2, clientBehaviors,
                htmlAttrName, attributeValue, serverSideScript);
    }

    public static boolean renderBehaviorizedAttribute(
            FacesContext facesContext, ResponseWriter writer,
            String componentProperty, UIComponent component,
            String targetClientId, String eventName,
            Collection<ClientBehaviorContext.Parameter> eventParameters,
            String eventName2,
            Collection<ClientBehaviorContext.Parameter> eventParameters2,
            Map<String, List<ClientBehavior>> clientBehaviors,
            String htmlAttrName, String attributeValue, String serverSideScript)
            throws IOException
    {
        List<ClientBehavior> cb1 = (clientBehaviors != null) ? clientBehaviors
                .get(eventName) : null;
        List<ClientBehavior> cb2 = (clientBehaviors != null) ? clientBehaviors
                .get(eventName2) : null;
        if (((cb1 != null) ? cb1.size() : 0) + ((cb2 != null) ? cb2.size() : 0)
                + (attributeValue != null ? 1 : 0) <= 1)
        {
            if (attributeValue != null)
            {
                return renderHTMLStringAttribute(writer, componentProperty,
                        htmlAttrName, attributeValue);
            }
            else if (serverSideScript != null)
            {
                return renderHTMLStringAttribute(writer, componentProperty,
                        htmlAttrName, serverSideScript);
            }
            else if (((cb1 != null) ? cb1.size() : 0) > 0)
            {
                return renderHTMLStringAttribute(
                        writer, componentProperty, htmlAttrName,
                        cb1.get(0).getScript(ClientBehaviorContext
                                        .createClientBehaviorContext(
                                                facesContext, component,
                                                eventName, targetClientId,
                                                eventParameters)));
            }
            else
            {
                return renderHTMLStringAttribute(
                        writer, componentProperty, htmlAttrName,
                        cb2.get(0).getScript(ClientBehaviorContext
                                        .createClientBehaviorContext(
                                                facesContext, component,
                                                eventName2, targetClientId,
                                                eventParameters2)));
            }
        }
        else
        {
            return renderHTMLStringAttribute(writer, componentProperty, htmlAttrName,
                    HtmlRendererUtils.buildBehaviorChain(facesContext,
                            component, targetClientId, eventName,
                            eventParameters, eventName2, eventParameters2,
                            clientBehaviors, attributeValue, serverSideScript));
        }
    }
    // CHECKSTYLE: ON
    
    /**
     * @since 4.0.0
     */
    public static void renderBehaviorizedEventHandlers(
            FacesContext facesContext, ResponseWriter writer,
            UIComponent uiComponent,
            Map<String, List<ClientBehavior>> clientBehaviors)
            throws IOException
    {
        renderBehaviorizedEventHandlers(facesContext, writer, uiComponent,
                uiComponent.getClientId(facesContext), clientBehaviors);
    }

    public static void renderBehaviorizedEventHandlers(
            FacesContext facesContext, ResponseWriter writer,
            UIComponent uiComponent, String targetClientId,
            Map<String, List<ClientBehavior>> clientBehaviors)
            throws IOException
    {
        renderBehaviorizedAttribute(facesContext, writer, HTML.ONCLICK_ATTR,
                uiComponent, targetClientId, ClientBehaviorEvents.CLICK,
                clientBehaviors, HTML.ONCLICK_ATTR);
        renderBehaviorizedAttribute(facesContext, writer, HTML.ONDBLCLICK_ATTR,
                uiComponent, targetClientId, ClientBehaviorEvents.DBLCLICK,
                clientBehaviors, HTML.ONDBLCLICK_ATTR);
        renderBehaviorizedAttribute(facesContext, writer,
                HTML.ONMOUSEDOWN_ATTR, uiComponent, targetClientId,
                ClientBehaviorEvents.MOUSEDOWN, clientBehaviors,
                HTML.ONMOUSEDOWN_ATTR);
        renderBehaviorizedAttribute(facesContext, writer, HTML.ONMOUSEUP_ATTR,
                uiComponent, targetClientId, ClientBehaviorEvents.MOUSEUP,
                clientBehaviors, HTML.ONMOUSEUP_ATTR);
        renderBehaviorizedAttribute(facesContext, writer,
                HTML.ONMOUSEOVER_ATTR, uiComponent, targetClientId,
                ClientBehaviorEvents.MOUSEOVER, clientBehaviors,
                HTML.ONMOUSEOVER_ATTR);
        renderBehaviorizedAttribute(facesContext, writer,
                HTML.ONMOUSEMOVE_ATTR, uiComponent, targetClientId,
                ClientBehaviorEvents.MOUSEMOVE, clientBehaviors,
                HTML.ONMOUSEMOVE_ATTR);
        renderBehaviorizedAttribute(facesContext, writer, HTML.ONMOUSEOUT_ATTR,
                uiComponent, targetClientId, ClientBehaviorEvents.MOUSEOUT,
                clientBehaviors, HTML.ONMOUSEOUT_ATTR);
        renderBehaviorizedAttribute(facesContext, writer, HTML.ONKEYPRESS_ATTR,
                uiComponent, targetClientId, ClientBehaviorEvents.KEYPRESS,
                clientBehaviors, HTML.ONKEYPRESS_ATTR);
        renderBehaviorizedAttribute(facesContext, writer, HTML.ONKEYDOWN_ATTR,
                uiComponent, targetClientId, ClientBehaviorEvents.KEYDOWN,
                clientBehaviors, HTML.ONKEYDOWN_ATTR);
        renderBehaviorizedAttribute(facesContext, writer, HTML.ONKEYUP_ATTR,
                uiComponent, targetClientId, ClientBehaviorEvents.KEYUP,
                clientBehaviors, HTML.ONKEYUP_ATTR);
    }

    /**
     * @since 4.0.0
     */
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

    /**
     * @since 4.0.0
     */
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

    /**
     * @since 4.0.0
     */
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
            UIComponent uiComponent, String targetClientId,
            Map<String, List<ClientBehavior>> clientBehaviors)
            throws IOException
    {
        renderBehaviorizedAttribute(facesContext, writer, HTML.ONFOCUS_ATTR,
                uiComponent, targetClientId, ClientBehaviorEvents.FOCUS, clientBehaviors, HTML.ONFOCUS_ATTR);
        renderBehaviorizedAttribute(facesContext, writer, HTML.ONBLUR_ATTR,
                uiComponent, targetClientId, ClientBehaviorEvents.BLUR, clientBehaviors, HTML.ONBLUR_ATTR);
        renderBehaviorizedAttribute(facesContext, writer, HTML.ONSELECT_ATTR,
                uiComponent, targetClientId, ClientBehaviorEvents.SELECT, clientBehaviors, HTML.ONSELECT_ATTR);
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

    /**
     * @since 4.0.0
     */
    public static boolean renderBehaviorizedOnchangeEventHandler(
            FacesContext facesContext, ResponseWriter writer,
            UIComponent uiComponent,
            Map<String, List<ClientBehavior>> clientBehaviors)
            throws IOException
    {
        boolean hasChange = HtmlRendererUtils.hasClientBehavior(
                ClientBehaviorEvents.CHANGE, clientBehaviors, facesContext);
        boolean hasValueChange = HtmlRendererUtils
                .hasClientBehavior(ClientBehaviorEvents.VALUECHANGE,
                        clientBehaviors, facesContext);

        if (hasChange && hasValueChange)
        {
            String chain = HtmlRendererUtils.buildBehaviorChain(facesContext,
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
            UIComponent uiComponent, String targetClientId,
            Map<String, List<ClientBehavior>> clientBehaviors)
            throws IOException
    {
        boolean hasChange = HtmlRendererUtils.hasClientBehavior(
                ClientBehaviorEvents.CHANGE, clientBehaviors, facesContext);
        boolean hasValueChange = HtmlRendererUtils
                .hasClientBehavior(ClientBehaviorEvents.VALUECHANGE,
                        clientBehaviors, facesContext);

        if (hasChange && hasValueChange)
        {
            String chain = HtmlRendererUtils.buildBehaviorChain(facesContext,
                    uiComponent, targetClientId, ClientBehaviorEvents.CHANGE,
                    null, ClientBehaviorEvents.VALUECHANGE, null,
                    clientBehaviors,
                    (String) uiComponent.getAttributes().get(HTML.ONCHANGE_ATTR), null);

            return HtmlRendererUtils.renderHTMLStringAttribute(writer,
                    HTML.ONCHANGE_ATTR, HTML.ONCHANGE_ATTR, chain);
        }
        else if (hasChange)
        {
            return HtmlRendererUtils.renderBehaviorizedAttribute(facesContext,
                    writer, HTML.ONCHANGE_ATTR, uiComponent, targetClientId,
                    ClientBehaviorEvents.CHANGE, clientBehaviors, HTML.ONCHANGE_ATTR);
        }
        else if (hasValueChange)
        {
            return HtmlRendererUtils.renderBehaviorizedAttribute(facesContext,
                    writer, HTML.ONCHANGE_ATTR, uiComponent, targetClientId,
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
        Object hideNoSelectionOptionAttr = component.getAttributes().get(
                JSFAttr.HIDE_NO_SELECTION_OPTION_ATTR);
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
        messages.setId("javax_faces_developmentstage_messages");
        messages.setTitle("Project Stage[Development]: Unhandled Messages");
        messages.setStyle("color:orange");
        messages.setRedisplay(false);
        // render the component
        messages.encodeAll(facesContext);
    }

    /**
     * The ScriptContext offers methods and fields
     * to help with rendering out a script and keeping a
     * proper formatting.
     */
    public static class ScriptContext extends JavascriptContext
    {
        public ScriptContext()
        {
            super();
        }
        public ScriptContext(boolean prettyPrint)
        {
            super(prettyPrint);
        }
        public ScriptContext(StringBuilder buf, boolean prettyPrint)
        {
            super(buf, prettyPrint);
        }
    }
}
