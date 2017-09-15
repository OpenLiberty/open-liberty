/*******************************************************************************
 * Copyright (c) 1997, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jsp.tsx.tag;

import java.lang.reflect.Method;
import java.util.Hashtable;
import java.util.Stack;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.BodyTagSupport;

public class GetPropertyTag extends BodyTagSupport {
    /**
	 * Comment for <code>serialVersionUID</code>
	 */
	private static final long serialVersionUID = 3257844389760742707L;
	private String name = "";
    private String property = "";

    public GetPropertyTag() {}

    public String getName() {
        return (name);
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getProperty() {
        return (property);
    }

    public void setProperty(String property) {
        this.property = property;
    }

    public int doStartTag() throws JspException {
        Stack repeatStack = (Stack) pageContext.getAttribute("TSXRepeatStack", PageContext.PAGE_SCOPE);
        if (repeatStack == null) {
            repeatStack = new Stack();
            pageContext.setAttribute("TSXRepeatStack", repeatStack, PageContext.PAGE_SCOPE);
        }
        Hashtable repeatLookup = (Hashtable) pageContext.getAttribute("TSXRepeatLookup", PageContext.PAGE_SCOPE);
        if (repeatLookup == null) {
            repeatLookup = new Hashtable();
            pageContext.setAttribute("TSXRepeatLookup", repeatLookup, PageContext.PAGE_SCOPE);
        }

        Method method = null;
        boolean implementTsxDynPropBeanIndex = false;
        Class[] interfaces;
        StringBuffer outputString = new StringBuffer();

        if (name.equals("request")) {
            String methodName = "request" + ".get" + property.substring(0, 1).toUpperCase() + property.substring(1);
            outputString.append("out.println (JspRuntimeLibrary.toString(" + methodName + "()));\n");
        }
        else {
            Object bean = pageContext.findAttribute(name);
            if (bean == null)
                throw new JspException("Bean instance [" + name + "] not found");
            interfaces = bean.getClass().getInterfaces();
            for (int i = 0; i < interfaces.length; i++) {
                if (interfaces[i].getName().equals("com.ibm.ws.jsp.tsx.TsxDynamicPropertyBeanIndex")) {
                    implementTsxDynPropBeanIndex = true;
                    break;
                }
            }

            if (implementTsxDynPropBeanIndex) {
                try {
                    method = bean.getClass().getMethod("getValue", new Class[] { String.class, Integer.TYPE });
                    String index = null;
                    if (repeatStack.empty() == false)
                        index = (String) repeatStack.peek();
                    Integer indexValue = (Integer) repeatLookup.get(index);
                    Object o = method.invoke(bean, new Object[] { property, indexValue });
                    if (o != null)
                        outputString.append((String) o);
                    else {
                        outputString.append("");
                        pageContext.setAttribute("TSXBreakRepeat", new Boolean(true), PageContext.REQUEST_SCOPE);
                    }
                }
                catch (Exception e) {
                    //com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.webcontainer.jsp.tsx.tag.GetPropertyTag.doStartTag", "86", this);
                    throw new JspException("Exceptiomn e : " + e.toString());
                }

            }
            else {
                try {
                    char chars[] = property.toCharArray();
                    chars[0] = Character.toUpperCase(chars[0]);
                    property = new String(chars);
                    String methodName = "get" + property;
                    Class paramTypes[] = new Class[0];
                    method = bean.getClass().getMethod(methodName, paramTypes);
                    String value = (String) method.invoke(bean, new Object[0]);
                    if (value != null)
                        outputString.append(value);
                    else
                        outputString.append("");
                }
                catch (Exception e) {
                    //com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.webcontainer.jsp.tsx.tag.GetPropertyTag.doStartTag", "106", this);
                    throw new JspException("Exceptiomn e : " + e.toString());
                }
            }
        }

        JspWriter writer = pageContext.getOut();
        try {
            writer.print(outputString.toString());
        }
        catch (java.io.IOException e) {
            //com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.webcontainer.jsp.tsx.tag.GetPropertyTag.doStartTag", "117", this);
            throw new JspException("IOException writing tag : " + e.toString());
        }
        return (EVAL_BODY_INCLUDE);
    }

    public void release() {
        super.release();
        name = "";
        property = "";
    }

}
