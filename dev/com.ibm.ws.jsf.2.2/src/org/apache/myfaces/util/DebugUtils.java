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
package org.apache.myfaces.util;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.el.MethodExpression;
import javax.el.ValueExpression;
import javax.faces.FacesException;
import javax.faces.component.UICommand;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.component.UIViewRoot;
import javax.faces.component.ValueHolder;
import javax.faces.context.FacesContext;
import javax.faces.event.FacesListener;
import javax.faces.validator.Validator;

/**
 * Utilities for logging.
 * 
 * @author Manfred Geiler (latest modification by $Author: lu4242 $)
 * @version $Revision: 1470770 $ $Date: 2013-04-23 01:08:02 +0000 (Tue, 23 Apr 2013) $
 */
public class DebugUtils
{
    //private static final Log log = LogFactory.getLog(DebugUtils.class);
    private static final Logger log = Logger.getLogger(DebugUtils.class.getName());

    // Attributes that should not be printed
    private static final HashSet<String> IGNORE_ATTRIBUTES;
    static
    {
        IGNORE_ATTRIBUTES = new HashSet<String>();
        IGNORE_ATTRIBUTES.add("attributes");
        IGNORE_ATTRIBUTES.add("children");
        IGNORE_ATTRIBUTES.add("childCount");
        IGNORE_ATTRIBUTES.add("class");
        IGNORE_ATTRIBUTES.add("facets");
        IGNORE_ATTRIBUTES.add("facetsAndChildren");
        IGNORE_ATTRIBUTES.add("parent");
        IGNORE_ATTRIBUTES.add("actionListeners");
        IGNORE_ATTRIBUTES.add("valueChangeListeners");
        IGNORE_ATTRIBUTES.add("validators");
        IGNORE_ATTRIBUTES.add("rowData");
        IGNORE_ATTRIBUTES.add("javax.faces.webapp.COMPONENT_IDS");
        IGNORE_ATTRIBUTES.add("javax.faces.webapp.FACET_NAMES");
        IGNORE_ATTRIBUTES.add("javax.faces.webapp.CURRENT_VIEW_ROOT");
    }

    private static final String JSF_COMPONENT_PACKAGE = "javax.faces.component.";
    private static final String MYFACES_COMPONENT_PACKAGE = "org.apache.myfaces.component.";

    private DebugUtils()
    {
        // hide from public access
    }

    public static void assertError(boolean condition, Logger logger, String msg) throws FacesException
    {
        if (!condition)
        {
            logger.severe(msg);
            throw new FacesException(msg);
        }
    }

    public static void assertFatal(boolean condition, Logger logger, String msg) throws FacesException
    {
        if (!condition)
        {
            logger.severe(msg);
            throw new FacesException(msg);
        }
    }

    public static void traceView(String additionalMsg)
    {
        if (log.isLoggable(Level.FINEST))
        {
            FacesContext facesContext = FacesContext.getCurrentInstance();
            if (facesContext == null)
            {
                log.severe("Cannot not print view to console (no FacesContext).");
                return;
            }
            UIViewRoot viewRoot = facesContext.getViewRoot();
            if (viewRoot == null)
            {
                log.severe("Cannot not print view to console (no ViewRoot in FacesContext).");
                return;
            }

            traceView(additionalMsg, viewRoot);
        }
    }

    /**
     * Be careful, when using this version of traceView: Some component properties (e.g. getRenderer) assume, that there
     * is a valid viewRoot set in the FacesContext!
     * 
     * @param additionalMsg
     * @param viewRoot
     */
    private static void traceView(String additionalMsg, UIViewRoot viewRoot)
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        if (additionalMsg != null)
        {
            ps.println(additionalMsg);
        }
        ps.println("========================================");
        printView(viewRoot, ps);
        ps.println("========================================");
        ps.close();
        log.finest(baos.toString());
    }

    public static void printView(UIViewRoot uiViewRoot, PrintStream stream)
    {
        printComponent(uiViewRoot, stream, 0, true, null);
    }

    public static void printComponent(UIComponent comp, PrintStream stream)
    {
        printComponent(comp, stream, 0, false, null);
    }

    private static void printComponent(UIComponent comp, PrintStream stream, int indent, boolean withChildrenAndFacets,
                                       String facetName)
    {
        printIndent(stream, indent);
        stream.print('<');

        String compType = comp.getClass().getName();
        if (compType.startsWith(JSF_COMPONENT_PACKAGE))
        {
            compType = compType.substring(JSF_COMPONENT_PACKAGE.length());
        }
        else if (compType.startsWith(MYFACES_COMPONENT_PACKAGE))
        {
            compType = compType.substring(MYFACES_COMPONENT_PACKAGE.length());
        }
        stream.print(compType);
        
        printAttribute(stream, "id", comp.getId());

        if (facetName != null)
        {
            printAttribute(stream, "facetName", facetName);
        }
        
        for (Map.Entry<String, Object> entry : comp.getAttributes().entrySet())
        {
            if (!"id".equals(entry.getKey()))
            {
                printAttribute(stream, entry.getKey(), entry.getValue());
            }
        }
        
        // HACK: comp.getAttributes() only returns attributes, that are NOT backed
        // by a corresponding component property. So, we must explicitly get the
        // available properties by Introspection:
        BeanInfo beanInfo;
        try
        {
            beanInfo = Introspector.getBeanInfo(comp.getClass());
        }
        catch (IntrospectionException e)
        {
            throw new RuntimeException(e);
        }

        if (!compType.startsWith("org.apache.myfaces.view.facelets.compiler"))
        {
            PropertyDescriptor propDescriptors[] = beanInfo.getPropertyDescriptors();
            for (int i = 0; i < propDescriptors.length; i++)
            {
                if (propDescriptors[i].getReadMethod() != null)
                {
                    String name = propDescriptors[i].getName();
                    if (!"id".equals(name))
                    {
                        ValueExpression ve = comp.getValueExpression(name);
                        if (ve != null)
                        {
                            printAttribute(stream, name, ve.getExpressionString());
                        }
                        else
                        {
                            if (name.equals("value") && comp instanceof ValueHolder)
                            {
                                // -> localValue
                            }
                            else if (!IGNORE_ATTRIBUTES.contains(name))
                            {
                                try
                                {
                                    Object value = comp.getAttributes().get(name);
                                    printAttribute(stream, name, value);
                                }
                                catch (Exception e)
                                {
                                    log.log(Level.SEVERE, e.getMessage() , e);
                                    printAttribute(stream, name, null);
                                }
                            }
                        }
                    }
                }
            }
        }

        boolean mustClose = true;
        boolean nestedObjects = false;

        if (comp instanceof UICommand)
        {
            FacesListener[] listeners = ((UICommand)comp).getActionListeners();
            if (listeners != null && listeners.length > 0)
            {
                nestedObjects = true;
                stream.println('>');
                mustClose = false;
                for (int i = 0; i < listeners.length; i++)
                {
                    FacesListener listener = listeners[i];
                    printIndent(stream, indent + 1);
                    stream.print('<');
                    stream.print(listener.getClass().getName());
                    stream.println("/>");
                }
            }
        }

        if (comp instanceof UIInput)
        {
            FacesListener[] listeners = ((UIInput)comp).getValueChangeListeners();
            if (listeners != null && listeners.length > 0)
            {
                nestedObjects = true;
                stream.println('>');
                mustClose = false;
                for (int i = 0; i < listeners.length; i++)
                {
                    FacesListener listener = listeners[i];
                    printIndent(stream, indent + 1);
                    stream.print('<');
                    stream.print(listener.getClass().getName());
                    stream.println("/>");
                }
            }

            Validator[] validators = ((UIInput)comp).getValidators();
            if (validators != null && validators.length > 0)
            {
                nestedObjects = true;
                stream.println('>');
                mustClose = false;
                for (int i = 0; i < validators.length; i++)
                {
                    Validator validator = validators[i];
                    printIndent(stream, indent + 1);
                    stream.print('<');
                    stream.print(validator.getClass().getName());
                    stream.println("/>");
                }
            }
        }

        if (withChildrenAndFacets)
        {
            int childCount = comp.getChildCount();
            if (childCount > 0 || comp.getFacetCount() > 0)
            {
                Map<String, UIComponent> facetsMap = comp.getFacets();
                nestedObjects = true;
                if (mustClose)
                {
                    stream.println('>');
                    mustClose = false;
                }

                if (childCount > 0)
                {
                    for (int i = 0; i < childCount; i++)
                    {
                        UIComponent child = comp.getChildren().get(i);
                        printComponent(child, stream, indent + 1, true, null);
                    }
                }

                for (Map.Entry<String, UIComponent> entry : facetsMap.entrySet())
                {
                    printComponent(entry.getValue(), stream, indent + 1, true, entry.getKey());
                }
            }
        }

        if (nestedObjects)
        {
            if (mustClose)
            {
                stream.println("/>");
            }
            else
            {
                printIndent(stream, indent);
                stream.print("</");
                stream.print(compType);
                stream.println('>');
            }
        }
        else
        {
            stream.println("/>");
        }
    }

    private static void printAttribute(PrintStream stream, String name, Object value)
    {
        if (IGNORE_ATTRIBUTES.contains(name))
        {
            return;
        }
        if (name.startsWith("javax.faces.webapp.UIComponentTag."))
        {
            name = name.substring("javax.faces.webapp.UIComponentTag.".length());
        }
        stream.print(' ');
        stream.print(name.toString());
        stream.print("=\"");
        if (value != null)
        {
            if (value instanceof UIComponent)
            {
                stream.print("[id:");
                stream.print(((UIComponent)value).getId());
                stream.print(']');
            }
            else if (value instanceof MethodExpression)
            {
                stream.print(((MethodExpression)value).getExpressionString());
            }
            else
            {
                stream.print(value.toString());
            }
        }
        else
        {
            stream.print("NULL");
        }
        stream.print('"');
    }

    private static void printIndent(PrintStream stream, int depth)
    {
        for (int i = 0; i < depth; i++)
        {
            stream.print("  ");
        }
    }

    public static String componentAsString(UIComponent comp)
    {
        try
        {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            printComponent(comp, new PrintStream(baos));
            baos.close();
            return baos.toString();
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }
}
