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
package org.apache.myfaces.view.facelets.tag.jsf.html;

import java.util.Arrays;
import javax.faces.render.Renderer;
import javax.faces.view.facelets.FaceletException;
import javax.faces.view.facelets.Tag;
import javax.faces.view.facelets.TagAttribute;
import javax.faces.view.facelets.TagAttributes;
import javax.faces.view.facelets.TagDecorator;
import org.apache.myfaces.view.facelets.tag.TagAttributeImpl;
import org.apache.myfaces.view.facelets.tag.TagAttributesImpl;
import org.apache.myfaces.view.facelets.tag.jsf.JsfLibrary;
import org.apache.myfaces.view.facelets.tag.jsf.PassThroughLibrary;
import org.apache.myfaces.view.facelets.tag.jsf.core.CoreLibrary;

/**
 * Default implementation of TagDecorator as described in JSF 2.2 javadoc of
 * javax.faces.view.facelets.TagDecorator
 * 
 * @since 2.2
 * @author Leonardo Uribe
 */
public class DefaultTagDecorator implements TagDecorator
{
    public final static String XHTML_NAMESPACE = "http://www.w3.org/1999/xhtml";
    public final static String JSF_NAMESPACE = JsfLibrary.NAMESPACE;
    public final static String JSF_ALIAS_NAMESPACE = JsfLibrary.ALIAS_NAMESPACE;
    public final static String PASS_THROUGH_NAMESPACE = PassThroughLibrary.NAMESPACE;
    public final static String PASS_THROUGH_ALIAS_NAMESPACE = PassThroughLibrary.ALIAS_NAMESPACE;
    private final static String EMPTY_NAMESPACE = "";
    
    private final static String P_ELEMENTNAME = "p:"+Renderer.PASSTHROUGH_RENDERER_LOCALNAME_KEY;
    
    /**
     * Fast array for lookup of local names to be inspected. 
     */
    static private final Object[][] LOCAL_NAME_ARR = new Object[256][];
    
    static private final Object[] A_NAMES = new Object[]
    {
      "a", new Object[]
      {
        new TagSelectorImpl("jsf:action", "h:commandLink"),
        new TagSelectorImpl("jsf:actionListener", "h:commandLink"),
        new TagSelectorImpl("jsf:value", "h:outputLink"),
        new TagSelectorImpl("jsf:outcome", "h:link")
      }
    };

    static private final Object[] B_NAMES = new Object[]
    {
      "body",   new Object[]{new TagSelectorImpl(null, "h:body")},
      "button", new Object[]{
          new TagSelectorImpl("jsf:outcome", "h:button"),
          new TagSelectorImpl(null, "h:commandButton")
      }
    };
    
    static private final Object[] F_NAMES = new Object[]
    {
      "form", new Object[]{new TagSelectorImpl(null, "h:form")}
    };
    
    static private final Object[] H_NAMES = new Object[]
    {
      "head", new Object[]{new TagSelectorImpl(null, "h:head")}
    };
    
    static private final Object[] I_NAMES = new Object[]
    {
      "img", new Object[]{new TagSelectorImpl(null, "h:graphicImage")},
      // We can optimize this part, but note the decoration step is done at
      // compile time, so at the end it does not matter. The important 
      // optimization is the outer one.      
      "input", new Object[]{
          new TagSelectorImpl("type=\"button\"", "h:commandButton"),
          new TagSelectorImpl("type=\"checkbox\"", "h:selectBooleanCheckbox"),          
          
          new TagSelectorImpl("type=\"color\"", "h:inputText"),
          new TagSelectorImpl("type=\"date\"", "h:inputText"),
          new TagSelectorImpl("type=\"datetime\"", "h:inputText"),
          new TagSelectorImpl("type=\"datetime-local\"", "h:inputText"),          
          new TagSelectorImpl("type=\"email\"", "h:inputText"),
          new TagSelectorImpl("type=\"month\"", "h:inputText"),
          new TagSelectorImpl("type=\"number\"", "h:inputText"),
          new TagSelectorImpl("type=\"range\"", "h:inputText"),
          new TagSelectorImpl("type=\"search\"", "h:inputText"),
          new TagSelectorImpl("type=\"time\"", "h:inputText"),
          new TagSelectorImpl("type=\"url\"", "h:inputText"),
          new TagSelectorImpl("type=\"week\"", "h:inputText"),
          
          new TagSelectorImpl("type=\"file\"", "h:inputFile"),
          new TagSelectorImpl("type=\"hidden\"", "h:inputHidden"),
          new TagSelectorImpl("type=\"password\"", "h:inputSecret"),
          new TagSelectorImpl("type=\"reset\"", "h:commandButton"),
          new TagSelectorImpl("type=\"submit\"", "h:commandButton"),
          new TagSelectorImpl("type=\"*\"", "h:inputText")
      }
    };
    
    static private final Object[] L_NAMES = new Object[]
    {
      "label", new Object[]{new TagSelectorImpl(null, "h:outputLabel")},
      "link",  new Object[]{new TagSelectorImpl(null, "h:outputStylesheet")}
    };

    static private final Object[] S_NAMES = new Object[]
    {
      "script", new Object[]{new TagSelectorImpl(null, "h:outputScript")},
      "select", new Object[]
      {
        new TagSelectorImpl("multiple=\"*\"", "h:selectManyListbox"),
        new TagSelectorImpl(null, "h:selectOneListbox")
      }
    };
    
    static private final Object[] T_NAMES = new Object[]
    {
     "textarea", new Object[]{new TagSelectorImpl(null, "h:inputTextarea")}
    };    

    static
    {
      LOCAL_NAME_ARR['a'] = A_NAMES;
      LOCAL_NAME_ARR['A'] = A_NAMES;
      LOCAL_NAME_ARR['b'] = B_NAMES;
      LOCAL_NAME_ARR['B'] = B_NAMES;
      LOCAL_NAME_ARR['f'] = F_NAMES;
      LOCAL_NAME_ARR['F'] = F_NAMES;
      LOCAL_NAME_ARR['h'] = H_NAMES;
      LOCAL_NAME_ARR['H'] = H_NAMES;
      LOCAL_NAME_ARR['i'] = I_NAMES;
      LOCAL_NAME_ARR['I'] = I_NAMES;
      LOCAL_NAME_ARR['l'] = L_NAMES;
      LOCAL_NAME_ARR['L'] = L_NAMES;
      LOCAL_NAME_ARR['s'] = S_NAMES;
      LOCAL_NAME_ARR['S'] = S_NAMES;
      LOCAL_NAME_ARR['t'] = T_NAMES;
      LOCAL_NAME_ARR['T'] = T_NAMES;
    }
    
    static private final String[][] RESERVED_JSF_ATTRS_ARR =  new String[256][];
    
    static private final String[] JSF_ATTRS_B_NAMES = {"binding"};

    static private final String[] JSF_ATTRS_I_NAMES = {"id"};
    
    static private final String[] JSF_ATTRS_R_NAMES = {"rendered"};
    
    static private final String[] JSF_ATTRS_T_NAMES = {"transient"};

    static 
    {
      RESERVED_JSF_ATTRS_ARR['b'] = JSF_ATTRS_B_NAMES;
      RESERVED_JSF_ATTRS_ARR['B'] = JSF_ATTRS_B_NAMES;
      RESERVED_JSF_ATTRS_ARR['i'] = JSF_ATTRS_I_NAMES;
      RESERVED_JSF_ATTRS_ARR['I'] = JSF_ATTRS_I_NAMES;
      RESERVED_JSF_ATTRS_ARR['r'] = JSF_ATTRS_R_NAMES;
      RESERVED_JSF_ATTRS_ARR['R'] = JSF_ATTRS_R_NAMES;
      RESERVED_JSF_ATTRS_ARR['t'] = JSF_ATTRS_T_NAMES;
      RESERVED_JSF_ATTRS_ARR['T'] = JSF_ATTRS_T_NAMES;
    }
    
    private static final TagDecoratorExecutor NO_MATCH_SELECTOR = new TagSelectorImpl(null, "jsf:element");
    
    public Tag decorate(Tag tag)
    {
        boolean jsfNamespaceFound = false;

        for (String namespace : tag.getAttributes().getNamespaces())
        {
            if (JSF_NAMESPACE.equals(namespace) || JSF_ALIAS_NAMESPACE.equals(namespace))
            {
                jsfNamespaceFound = true;
                break;
            }
        }
        if (!jsfNamespaceFound)
        {
            // Return null, so the outer CompositeTagDecorator can process the tag.
            return null;
        }
        
        // One or many attributes has the JSF_NAMESPACE attribute set. Check empty or
        // xhtml namespace
        if (EMPTY_NAMESPACE.equals(tag.getNamespace()) ||
            XHTML_NAMESPACE.equals(tag.getNamespace()))
        {
            String localName = tag.getLocalName();
            boolean processed = false;
            if (isLocalNameDecorated(localName))
            {
                Object[] array = LOCAL_NAME_ARR[localName.charAt(0)];
                int localNameIndex = -1;
                if (array != null)
                {
                    for (int i = array.length - 2; i >= 0; i-=2)
                    {
                        if (localName.equalsIgnoreCase((String)array[i]))
                        {
                            localNameIndex = i;
                            break;
                        }
                    }
                    if (localNameIndex >= 0)
                    {
                        Object[] tagSelectorArray = (Object[]) array[localNameIndex+1];

                        for (int i = 0; i < tagSelectorArray.length; i++)
                        {
                            TagSelector tagSelector = (TagSelector) tagSelectorArray[i];
                            TagDecoratorExecutor executor = tagSelector.getExecutorIfApplies(tag);

                            if (executor != null)
                            {
                                return executor.decorate(tag, convertTagAttributes(tag));
                            }
                        }
                    }
                }
            }
            if (!processed)
            {
                //If no matching entry is found, let jsf:element be the value of targetTag
                return NO_MATCH_SELECTOR.decorate(tag, convertTagAttributes(tag));
            }
            return null;
        }
        else
        {
            throw new FaceletException("Attributes under "+JSF_NAMESPACE+
                " can only be used for tags under "+ XHTML_NAMESPACE +" or tags with no namespace defined" );
        }
    }
    
    private TagAttributes convertTagAttributes(Tag tag)
    {        
        TagAttribute[] sourceTagAttributes = tag.getAttributes().getAll();
        
        String elementNameTagLocalName = tag.getLocalName();

        TagAttribute elementNameTagAttribute = new TagAttributeImpl(
            tag.getLocation(), PASS_THROUGH_NAMESPACE , Renderer.PASSTHROUGH_RENDERER_LOCALNAME_KEY,
            P_ELEMENTNAME, elementNameTagLocalName );
        
        // 1. Count how many attributes requires to be duplicated
        int duplicateCount = 0;
        
        TagAttribute[] convertedTagAttributes = new TagAttribute[
            sourceTagAttributes.length+1+duplicateCount];
        boolean elementNameTagAttributeSet = false;
        int j = 0;

        for (int i = 0; i < sourceTagAttributes.length; i++)
        {
            TagAttribute tagAttribute = sourceTagAttributes[i];
            String convertedNamespace;
            String qname;
            String namespace = tagAttribute.getNamespace();
            
            /*
                -= Leonardo Uribe =- After check the javadoc and compare it with the code and try some
                examples with the implementation done in the RI, we found that the javadoc of 
                TagDecorator has some bugs. Below is the description of the implementation done, which
                resembles the behavior found on the RI.

                "...
                For each of argument tag's attributes obtain a reference to a TagAttribute 
                with the following characteristics. For discussion let such an attribute be 
                convertedTagAttribute.

                    * convertedTagAttribute's location: from the argument tag's location.

                    * If the current attribute's namespace is http://xmlns.jcp.org/jsf, 
                        convertedTagAttribute's qualified name must be the current attribute's 
                        local name and convertedTagAttribute's namespace must be the empty string. 
                        This will have the effect of setting the current attribute as a proper 
                        property on the UIComponent instance represented by this markup.

                    * If the current attribute's namespace is empty, assume the current 
                        attribute's namespace is http://xmlns.jcp.org/jsf/passthrough. 
                        ConvertedTagAttribute's qualified name is the current attribute's 
                        local name prefixed by "p:". convertedTagAttribute's namespace must be 
                        http://xmlns.jcp.org/jsf/passthrough.

                    * Otherwise, if the current attribute's namespace is not empty, let 
                        the current attribute be convertedTagAttribute. This will have the 
                        effect of let the attribute be processed by the meta rules defined
                        by the TagHandler instance associated with the generated target 
                        component.
                ..."        
            */            
            if (JSF_NAMESPACE.equals(namespace) || JSF_ALIAS_NAMESPACE.equals(namespace))
            {
                // "... If the current attribute's namespace is http://xmlns.jcp.org/jsf, convertedTagAttribute's 
                //  qualified name must be the current attribute's local name and convertedTagAttribute's 
                // namespace must be the empty string. This will have the effect of setting the current 
                // attribute as a proper property on the UIComponent instance represented by this markup.
                convertedNamespace = "";
                qname = tagAttribute.getLocalName();
                
                convertedTagAttributes[j] = new TagAttributeImpl(tagAttribute.getLocation(), 
                    convertedNamespace, tagAttribute.getLocalName(), qname, tagAttribute.getValue());
            }
            else if (namespace == null)
            {
                // should not happen, but let it because org.xml.sax.Attributes considers it
                // -= Leonardo Uribe =- after conversation with Frank Caputo, who was the main contributor for
                // this feature in JSF 2.2, he said that if the namespace is empty the intention is pass the
                // attribute to the passthrough attribute map, so there is an error in the spec documentation.
                //convertedTagAttributes[j] = tagAttribute;
                
                convertedNamespace = PASS_THROUGH_NAMESPACE;
                qname = "p:"+tagAttribute.getLocalName();
                
                convertedTagAttributes[j] = new TagAttributeImpl(tagAttribute.getLocation(), 
                    convertedNamespace, tagAttribute.getLocalName(), qname, tagAttribute.getValue());
            }
            else if (tagAttribute.getNamespace().length() == 0)
            {
                // "... If the current attribute's namespace is empty 
                // let the current attribute be convertedTagAttribute. ..."
                // -= Leonardo Uribe =- after conversation with Frank Caputo, who was the main contributor for
                // this feature in JSF 2.2, he said that if the namespace is empty the intention is pass the
                // attribute to the passthrough attribute map, so there is an error in the spec documentation.
                //convertedTagAttributes[j] = tagAttribute;
                
                convertedNamespace = PASS_THROUGH_NAMESPACE;
                qname = "p:"+tagAttribute.getLocalName();
                
                convertedTagAttributes[j] = new TagAttributeImpl(tagAttribute.getLocation(), 
                    convertedNamespace, tagAttribute.getLocalName(), qname, tagAttribute.getValue());
            }
            else /*if (!tag.getNamespace().equals(tagAttribute.getNamespace()))*/
            {
                // "... or different from the argument tag's namespace, 
                // let the current attribute be convertedTagAttribute. ..."
                convertedTagAttributes[j] = tagAttribute;
            }
            
            if (Renderer.PASSTHROUGH_RENDERER_LOCALNAME_KEY.equals(convertedTagAttributes[j].getLocalName()) && (
                PASS_THROUGH_NAMESPACE.equals(convertedTagAttributes[j].getNamespace()) || 
                PASS_THROUGH_ALIAS_NAMESPACE.equals(convertedTagAttributes[j].getNamespace()) ) )
            {
                elementNameTagAttributeSet = true;
            }
            j++;
        }
        
        if (elementNameTagAttributeSet)
        {
            // This is unlikely, but theorically possible.
            return new TagAttributesImpl(Arrays.copyOf(convertedTagAttributes, convertedTagAttributes.length-1));
        }
        else
        {
            convertedTagAttributes[convertedTagAttributes.length-1] = elementNameTagAttribute;
            return new TagAttributesImpl(convertedTagAttributes);
        }
    }
    
    private boolean isLocalNameDecorated(String elem)
    {
        Object[] array = LOCAL_NAME_ARR[elem.charAt(0)];
        if (array != null)
        {
            for (int i = array.length - 2; i >= 0; i-=2)
            {
                if (elem.equalsIgnoreCase((String)array[i]))
                {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isReservedJSFAttribute(String attr)
    {
        String[] array = RESERVED_JSF_ATTRS_ARR[attr.charAt(0)];
        if (array != null)
        {
            for (int i = array.length - 1; i >= 0; i-=1)
            {
                if (attr.equalsIgnoreCase((String)array[i]))
                {
                    return true;
                }
            }
        }
        return false;
    }
    
    private static interface TagDecoratorExecutor
    {
        public Tag decorate(Tag orig, TagAttributes attributes);
        
    }
    
    private static abstract class TagSelector
    {
        public abstract TagDecoratorExecutor getExecutorIfApplies(Tag tag);
    }
    
    private static class TagSelectorImpl extends TagSelector implements TagDecoratorExecutor
    {
        //private String selector;
        private String attributeQName;
        private String attributeLocalName;
        private String attributePrefix;
        private final String attributeNamespace;
        private final String attributeAliasNamespace;
        private String matchValue;
        
        private String targetQName;
        private String targetNamespace;
        private String targetLocalName;
        
        public TagSelectorImpl(String selector, String targetQName)
        {
            // The idea in this constructor is do the parsing step of the selector
            // just once, so the check can be done quickly.
            //this.selector = selector;
            if (selector != null)
            {
                int i = selector.indexOf('=');
                if (i >= 0)
                {
                    this.attributeQName = selector.substring(0,i);
                    String value = selector.substring(i+1);
                    int s = value.indexOf('"');
                    int t = value.lastIndexOf('"');
                    if (s >= 0 && t >= 0 && t > s)
                    {
                        this.matchValue = value.substring(s+1,t);
                    }
                    else
                    {
                        this.matchValue = value;
                    }
                }
                else
                {
                    this.attributeQName = selector;
                    this.matchValue = null;
                }
                
                int j = attributeQName.indexOf(':');
                this.attributeLocalName = (j >= 0) ? attributeQName.substring(j+1) : attributeQName;
                this.attributePrefix = (j >= 0) ? attributeQName.substring(0, j) : null;
                this.attributeNamespace = resolveSelectorNamespace(this.attributePrefix);
                this.attributeAliasNamespace = resolveAliasSelectorNamespace(this.attributePrefix);
            }
            else
            {
                this.attributeQName = null;
                this.matchValue = null;
                this.attributeLocalName = null;
                this.attributePrefix = null;
                this.attributeNamespace = "";
                this.attributeAliasNamespace = null;
            }
            
            this.targetQName = targetQName;
            if (targetQName != null)
            {
                int j = targetQName.indexOf(':');
                if (j >= 0)
                {
                    //this.
                    if (j == 1 && targetQName.charAt(0) == 'h')
                    {
                        this.targetNamespace = HtmlLibrary.NAMESPACE;
                        this.targetLocalName = targetQName.substring(j+1);
                    }
                    else if (j == 3 && targetQName.startsWith("jsf"))
                    {
                        this.targetNamespace = JsfLibrary.ALIAS_NAMESPACE;
                        this.targetLocalName = targetQName.substring(j+1);
                    }
                }
                else
                {
                    this.targetLocalName = targetQName;
                }
            }
        }

        public TagDecoratorExecutor getExecutorIfApplies(Tag tag)
        {
            if (attributeQName != null)
            {
                 if (matchValue != null)
                 {
                     String attributeNS = attributeNamespace;
                     TagAttribute attr = tag.getAttributes().get(attributeNS, attributeLocalName);
                     if (attr == null && attributeAliasNamespace.length() > 0)
                     {
                         attributeNS = attributeAliasNamespace;
                         attr = tag.getAttributes().get(attributeAliasNamespace, attributeLocalName);
                     }
                     if (attr != null)
                     {
                         if (attributeNS.equals(attr.getNamespace()) )
                         {
                            // if namespace is the same match
                             if (matchValue.equals(attr.getValue()))
                             {
                                return this;
                             }
                             else if ("*".equals(matchValue) && attr.getValue() != null)
                             {
                                 return this;
                             }
                         }
                         else if (attributeNS == "" && attr.getNamespace() == null)
                         {
                             // if namespace is empty match
                             if (matchValue.equals(attr.getValue()))
                             {
                                 return this;
                             }
                             else if ("*".equals(matchValue) && attr.getValue() != null)
                             {
                                 return this;
                             }
                         }
                     }
                 }
                 else
                 {
                     String attributeNS = attributeNamespace;
                     TagAttribute attr = tag.getAttributes().get(attributeNS, attributeLocalName);
                     if (attr == null)
                     {
                         attributeNS = attributeAliasNamespace;
                         attr = tag.getAttributes().get(attributeNS, attributeLocalName);
                     }
                     if (attr != null)
                     {
                         if (attributeNS.equals(attr.getNamespace()))
                         {
                             // if namespace is the same match
                             return this;
                         }
                         else if (attributeNS == "" && attr.getNamespace() == null)
                         {
                             // if namespace is empty match
                             return this;
                         }
                     }
                 }
                return null;
            }
            else
            {
                return this;
            }
        }
        
        public Tag decorate(Tag orig, TagAttributes attributes)
        {
            return new Tag(orig.getLocation(), this.targetNamespace, 
                this.targetLocalName, this.targetQName, attributes);
        }
    }

    private static String resolveSelectorNamespace(String prefix)
    {
        if ("jsf".equals(prefix))
        {
            return JsfLibrary.NAMESPACE;
        }
        else if ("h".equals(prefix))
        {
            return HtmlLibrary.NAMESPACE;
        }
        else if ("f".equals(prefix))
        {
            return CoreLibrary.NAMESPACE;
        }
        return "";
    }

    private static String resolveAliasSelectorNamespace(String prefix)
    {
        if ("jsf".equals(prefix))
        {
            return JsfLibrary.ALIAS_NAMESPACE;
        }
        else if ("h".equals(prefix))
        {
            return HtmlLibrary.ALIAS_NAMESPACE;
        }
        else if ("f".equals(prefix))
        {
            return CoreLibrary.ALIAS_NAMESPACE;
        }
        return "";
    }
}
