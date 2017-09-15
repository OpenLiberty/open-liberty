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
package org.apache.myfaces.view.facelets.tag.composite;

import java.beans.FeatureDescriptor;
import java.util.Arrays;

import javax.faces.view.facelets.FaceletContext;
import javax.faces.view.facelets.Tag;
import javax.faces.view.facelets.TagAttribute;

/**
 * TagAttribute utils for <composite:xxx> TagHandlers.
 * 
 * @author Jakob Korherr (latest modification by $Author: jakobk $)
 * @version $Revision: 964806 $ $Date: 2010-07-16 14:24:28 +0000 (Fri, 16 Jul 2010) $
 */
public final class CompositeTagAttributeUtils
{
    
    // prevent this from being instantiated
    private CompositeTagAttributeUtils()
    {
    }
    
    /**
     * Adds all attributes from the given Tag which are NOT listed in 
     * standardAttributesSorted as a ValueExpression to the given BeanDescriptor.
     * NOTE that standardAttributesSorted has to be alphabetically sorted in
     * order to use binary search.
     * @param descriptor
     * @param tag
     * @param standardAttributesSorted
     * @param ctx
     */
    public static void addUnspecifiedAttributes(FeatureDescriptor descriptor, 
            Tag tag, String[] standardAttributesSorted, FaceletContext ctx)
    {
        for (TagAttribute attribute : tag.getAttributes().getAll())
        {
            final String name = attribute.getLocalName();
            if (Arrays.binarySearch(standardAttributesSorted, name) < 0)
            {
                // attribute not found in standard attributes
                // --> put it on the BeanDescriptor
                descriptor.setValue(name, attribute.getValueExpression(ctx, Object.class));
            }
        }
    }
    
    /**
     * Returns true if the given Tag contains attributes that are not
     * specified in standardAttributesSorted.
     * NOTE that standardAttributesSorted has to be alphabetically sorted in
     * order to use binary search.
     * @param tag
     * @param standardAttributesSorted
     * @return
     */
    public static boolean containsUnspecifiedAttributes(Tag tag, String[] standardAttributesSorted)
    {
        for (TagAttribute attribute : tag.getAttributes().getAll())
        {
            final String name = attribute.getLocalName();
            if (Arrays.binarySearch(standardAttributesSorted, name) < 0)
            {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Applies the "displayName", "shortDescription", "expert", "hidden",
     * and "preferred" attributes to the BeanDescriptor.
     * @param descriptor
     * @param ctx
     * @param displayName
     * @param shortDescription
     * @param expert
     * @param hidden
     * @param preferred
     */
    public static void addDevelopmentAttributes(FeatureDescriptor descriptor, 
            FaceletContext ctx, TagAttribute displayName, TagAttribute shortDescription, 
            TagAttribute expert, TagAttribute hidden, TagAttribute preferred)
    {
        if (displayName != null)
        {
            descriptor.setDisplayName(displayName.getValue(ctx));
        }
        if (shortDescription != null)
        {
            descriptor.setShortDescription(shortDescription.getValue(ctx));
        }
        if (expert != null)
        {
            descriptor.setExpert(expert.getBoolean(ctx));
        }
        if (hidden != null)
        {
            descriptor.setHidden(hidden.getBoolean(ctx));
        }
        if (preferred != null)
        {
            descriptor.setPreferred(preferred.getBoolean(ctx));
        }
    }
    
    /**
     * Applies the "displayName", "shortDescription", "expert", "hidden",
     * and "preferred" attributes to the BeanDescriptor if they are all literal values.
     * Thus no FaceletContext is necessary.
     * @param descriptor
     * @param displayName
     * @param shortDescription
     * @param expert
     * @param hidden
     * @param preferred
     */
    public static void addDevelopmentAttributesLiteral(FeatureDescriptor descriptor, 
            TagAttribute displayName, TagAttribute shortDescription, 
            TagAttribute expert, TagAttribute hidden, TagAttribute preferred)
    {
        if (displayName != null)
        {
            descriptor.setDisplayName(displayName.getValue());
        }
        if (shortDescription != null)
        {
            descriptor.setShortDescription(shortDescription.getValue());
        }
        if (expert != null)
        {
            descriptor.setExpert(Boolean.valueOf(expert.getValue()));
        }
        if (hidden != null)
        {
            descriptor.setHidden(Boolean.valueOf(hidden.getValue()));
        }
        if (preferred != null)
        {
            descriptor.setPreferred(Boolean.valueOf(preferred.getValue()));
        }
    }
    
    /**
     * Returns true if all specified attributes are either null or literal.
     * @param attributes
     */
    public static boolean areAttributesLiteral(TagAttribute... attributes)
    {
        for (TagAttribute attribute : attributes)
        {
            if (attribute != null && !attribute.isLiteral())
            {
                // the attribute exists and is not literal
                return false;
            }
        }
        // all attributes are literal
        return true;
    }
    
}
