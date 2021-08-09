/*******************************************************************************
 * Copyright (c) 1997, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jsp.taglib;


import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.jsp.tagext.BodyTag;
import javax.servlet.jsp.tagext.DynamicAttributes;
import javax.servlet.jsp.tagext.IterationTag;
import javax.servlet.jsp.tagext.JspIdConsumer;
import javax.servlet.jsp.tagext.SimpleTag;
import javax.servlet.jsp.tagext.TryCatchFinally;

import com.ibm.ws.jsp.JspCoreException;
import com.ibm.wsspi.webcontainer.util.ThreadContextHelper;
import com.ibm.wsspi.jsp.context.JspCoreContext;  //PK36246

public class TagClassInfo {
    protected Class tagClass = null;
    protected boolean implementsIterationTag = false;
    protected boolean implementsBodyTag = false;
    protected boolean implementsTryCatchFinally = false;
    protected boolean implementsSimpleTag = false;
    protected boolean implementsDynamicAttributes = false;
    protected boolean implementsJspIdConsumer = false;// jsp2.1work
    protected Map setterMethodNameMap = null;
    protected Map propertyEditorClassNameMap = null;
    protected Map parameterClassNameMap = null;
    
    public TagClassInfo(Class tagClass) {
        this.tagClass = tagClass;
        implementsIterationTag = IterationTag.class.isAssignableFrom(tagClass);
        implementsBodyTag = BodyTag.class.isAssignableFrom(tagClass);
        implementsTryCatchFinally = TryCatchFinally.class.isAssignableFrom(tagClass);
        implementsSimpleTag = SimpleTag.class.isAssignableFrom(tagClass);
        implementsDynamicAttributes = DynamicAttributes.class.isAssignableFrom(tagClass);
        implementsJspIdConsumer = JspIdConsumer.class.isAssignableFrom(tagClass);// jsp2.1work
    }
 
     public TagClassInfo() {}
   
     
    public String getTagClassName() {
        return tagClass.getName();
    }
    
    public boolean implementsIterationTag() {
        return implementsIterationTag;
    }
    
    public boolean implementsBodyTag() {
        return implementsBodyTag;
    }
    
    public boolean implementsTryCatchFinally() {
        return implementsTryCatchFinally;
    }
    
    public boolean implementsSimpleTag() {
        return implementsSimpleTag;
    }
    
    public boolean implementsDynamicAttributes() {
        return implementsDynamicAttributes;
    }
    
    //  jsp2.1work
    public boolean implementsJspIdConsumer() {
        return implementsJspIdConsumer;
    }
    
    public String getSetterMethodName(String attributeName) throws JspCoreException {
        String setterMethodName = null;
        
        if (setterMethodNameMap == null) {
            setterMethodNameMap = new HashMap();
        }
        
        setterMethodName = (String)setterMethodNameMap.get(attributeName);
        
        if (setterMethodName == null) { 
            try {
                BeanInfo beanInfo = Introspector.getBeanInfo(tagClass);
                PropertyDescriptor[] pd = beanInfo.getPropertyDescriptors();
                for (int i = 0; i < pd.length; i++) {
                    if (pd[i].getName().equals(attributeName)) {
                        Method m = pd[i].getWriteMethod();
                        if (m == null) {
                            throw new JspCoreException("jsp.error.unable.locate.setter.method.attribute.for.tagname", new Object[] { tagClass.getName(), attributeName});
                        }
                        setterMethodName = m.getName();
                        setterMethodNameMap.put(attributeName, setterMethodName);
                        break;
                    }    
                }
            }
            catch (IntrospectionException e) {
                throw new JspCoreException("jsp.error.introspect.taghandle", new Object[] {tagClass.getName()}, e);
            }
        
            if (setterMethodName == null){
                throw new JspCoreException("jsp.error.unable.locate.setter.method.attribute.for.tagname", new Object[] { tagClass.getName(), attributeName});
            }
        }
                         
        
        return (setterMethodName);
    }
    
    public String getPropertyEditorClassName(String attributeName) throws JspCoreException {
        String propertyEditorClassName = null;
        
        if (propertyEditorClassNameMap == null) {
            propertyEditorClassNameMap = new HashMap();
        }
        
        propertyEditorClassName = (String)propertyEditorClassNameMap.get(attributeName);
        
        if (propertyEditorClassName == null) {
            try {
                BeanInfo beanInfo = Introspector.getBeanInfo(tagClass);
                PropertyDescriptor[] pd = beanInfo.getPropertyDescriptors();
                for (int i = 0; i < pd.length; i++) {
                    if (pd[i].getName().equals(attributeName)) {
                        Class propertyEditorClass = pd[i].getPropertyEditorClass();
                        if (propertyEditorClass != null) {
                            propertyEditorClassName = propertyEditorClass.getName();
                            propertyEditorClassNameMap.put(attributeName, propertyEditorClassName);
                        }
                    }    
                }
            }
            catch (IntrospectionException e) {
                throw new JspCoreException("jsp.error.introspect.taghandle", new Object[] {tagClass.getName()}, e);
            }
        }
        
        return (propertyEditorClassName);
    }
    
    public String getParameterClassName(String attributeName, JspCoreContext context) throws JspCoreException {
        String parameterClassName = null;
        
        if (parameterClassNameMap == null) {
            parameterClassNameMap = new HashMap();
        }
        
        parameterClassName = (String)parameterClassNameMap.get(attributeName);
        
        if (parameterClassName == null) {
            //PK36246 Setting the correct classLoader to load a class from a jsp
            ClassLoader oldLoader = ThreadContextHelper.getContextClassLoader();
            ThreadContextHelper.setClassLoader(context.getJspClassloaderContext().getClassLoader());//PK36246
            //PK36246
            try {
                BeanInfo beanInfo = Introspector.getBeanInfo(tagClass);
                PropertyDescriptor[] pd = beanInfo.getPropertyDescriptors();
                for (int i = 0; i < pd.length; i++) {
                    if (pd[i].getName().equals(attributeName)) {
                        Method m = pd[i].getWriteMethod();
                        if (m == null) {
                            throw new JspCoreException("jsp.error.unable.locate.setter.method.attribute.for.tagname", new Object[] { tagClass.getName(), attributeName});
                        }
                        Class[] parameterClasses = m.getParameterTypes();
                        parameterClassName = parameterClasses[0].getName();
                        parameterClassNameMap.put(attributeName, parameterClassName);
                        break;
                    }    
                }
            }
            catch (IntrospectionException e) {
                throw new JspCoreException("jsp.error.introspect.taghandle", new Object[] {tagClass.getName()}, e);
            }
            finally {
                ThreadContextHelper.setClassLoader(oldLoader);//PK36246 Resetting old classloader
            }

        
            if(parameterClassName == null){
                throw new JspCoreException("jsp.error.unable.locate.setter.method.attribute.for.tagname", new Object[] { tagClass.getName(), attributeName});
            }
        }
        
        return (parameterClassName);
    }

	public String toString(){
		return new String ("" +
		"tagClass = [" + getTagClassName() +"]" +
		"implementsIterationTag = [" + implementsIterationTag +"]" +
		"implementsBodyTag = [" + implementsBodyTag +"]" +
		"implementsTryCatchFinally = [" + implementsTryCatchFinally +"]" +
		"implementsSimpleTag = [" + implementsSimpleTag +"]" +
		"implementsDynamicAttributes = [" + implementsDynamicAttributes+"]" + 
		"implementsJspIdConsumer = [" + implementsJspIdConsumer+"]" //jsp2.1work
		);	
	}
	
}
