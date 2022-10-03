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
package org.apache.myfaces.view.facelets.tag.faces.core;

import jakarta.faces.component.UIImportConstants;
import jakarta.faces.component.UIParameter;
import jakarta.faces.component.UISelectItem;
import jakarta.faces.component.UISelectItemGroups;
import jakarta.faces.component.UISelectItems;
import jakarta.faces.component.UIViewAction;
import jakarta.faces.component.UIViewParameter;
import jakarta.faces.convert.DateTimeConverter;
import jakarta.faces.convert.NumberConverter;
import jakarta.faces.validator.BeanValidator;
import jakarta.faces.validator.DoubleRangeValidator;
import jakarta.faces.validator.LengthValidator;
import jakarta.faces.validator.LongRangeValidator;
import jakarta.faces.validator.RegexValidator;
import jakarta.faces.validator.RequiredValidator;
import org.apache.myfaces.component.validate.ValidateWholeBeanComponent;

import org.apache.myfaces.view.facelets.tag.AbstractTagLibrary;

/**
 * For Tag details, see JSF Core <a target="_new"
 * href="http://java.sun.com/j2ee/javaserverfaces/1.1_01/docs/tlddocs/f/tld-summary.html">taglib documentation</a>.
 *
 * @author Jacob Hookom
 * @version $Id$
 */
public final class CoreLibrary extends AbstractTagLibrary
{
    public final static String NAMESPACE = "jakarta.faces.core";
    public final static String JCP_NAMESPACE = "http://xmlns.jcp.org/jsf/core";
    public final static String SUN_NAMESPACE = "http://java.sun.com/jsf/core";

    public final static CoreLibrary INSTANCE = new CoreLibrary();

    public CoreLibrary()
    {
        super(NAMESPACE, JCP_NAMESPACE, SUN_NAMESPACE);

        this.addTagHandler("actionListener", ActionListenerHandler.class);

        this.addTagHandler("ajax", AjaxHandler.class);
        
        this.addTagHandler("attribute", AttributeHandler.class);
        
        this.addTagHandler("attributes", AttributesHandler.class);

        this.addConverter("convertDateTime", DateTimeConverter.CONVERTER_ID, ConvertDateTimeHandler.class);

        this.addConverter("convertNumber", NumberConverter.CONVERTER_ID, ConvertNumberHandler.class);

        this.addConverter("converter", null, ConvertDelegateHandler.class);
        
        this.addTagHandler ("event", EventHandler.class);
        
        this.addTagHandler("facet", FacetHandler.class);

        this.addTagHandler("loadBundle", LoadBundleHandler.class);

        this.addTagHandler("metadata", ViewMetadataHandler.class);
        
        this.addComponent("param", UIParameter.COMPONENT_TYPE, null);
        
        this.addTagHandler("passThroughAttribute", PassThroughAttributeHandler.class);
        
        this.addTagHandler("passThroughAttributes", PassThroughAttributesHandler.class);

        this.addTagHandler("phaseListener", PhaseListenerHandler.class);
        
        this.addTagHandler("resetValues", ResetValuesActionListenerHandler.class);

        this.addComponent("selectItem", UISelectItem.COMPONENT_TYPE, null, SelectItemHandler.class);

        this.addComponent("selectItems", UISelectItems.COMPONENT_TYPE, null, SelectItemsHandler.class);
        
        this.addComponent("selectItemGroups", UISelectItemGroups.COMPONENT_TYPE, null, SelectItemGroupsHandler.class);

        this.addTagHandler("setPropertyActionListener", SetPropertyActionListenerHandler.class);

        this.addComponent("subview", "jakarta.faces.NamingContainer", null);

        this.addValidator("validateBean", BeanValidator.VALIDATOR_ID);
        
        this.addValidator("validateLength", LengthValidator.VALIDATOR_ID);

        this.addValidator("validateLongRange", LongRangeValidator.VALIDATOR_ID);

        this.addValidator("validateDoubleRange", DoubleRangeValidator.VALIDATOR_ID);

        this.addValidator("validateRegex", RegexValidator.VALIDATOR_ID);
        
        this.addValidator("validateRequired", RequiredValidator.VALIDATOR_ID);

        this.addValidator("validator", null, ValidateDelegateHandler.class);

        this.addTagHandler("valueChangeListener", ValueChangeListenerHandler.class);

        this.addTagHandler("view", ViewHandler.class);
        
        this.addComponent("viewAction", UIViewAction.COMPONENT_TYPE, null);
        
        this.addComponent("viewParam", UIViewParameter.COMPONENT_TYPE, null);
        
        this.addComponent("importConstants", UIImportConstants.COMPONENT_TYPE, null);
        
        this.addComponent("validateWholeBean", ValidateWholeBeanComponent.COMPONENT_TYPE, null);
        
        this.addComponent("websocket", "jakarta.faces.Websocket", 
                "jakarta.faces.Websocket", WebsocketHandler.class);
    }
}
