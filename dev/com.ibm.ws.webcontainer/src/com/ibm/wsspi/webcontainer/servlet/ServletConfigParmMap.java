/*******************************************************************************
 * Copyright (c) 1997, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.webcontainer.servlet;

import java.util.HashMap;

/**
 * 
 * ServletConfigParmMap is an spi for a map that can be passed in to 
 * configure various parts of a ServletConfig. It restricts the values that
 * can be placed in the map based on the ServletConfigParmKey keys.
 * 
 * @ibm-private-in-use
 * 
 * @since   WAS7.0
 *
 */
@SuppressWarnings("unchecked")
public class ServletConfigParmMap {
	static class ServletConfigParmKey
	{
		String key = null;
	     public ServletConfigParmKey(String key)
	     {
	     	this.key = key;
	     }
	     public String getKey (){
	     	return key;
	     }
	}
	public static final ServletConfigParmKey ATTRIBUTE = new ServletConfigParmKey("Attribute");
	public static final ServletConfigParmKey CLASSNAME = new ServletConfigParmKey("ClassName");
	public static final ServletConfigParmKey FILENAME = new ServletConfigParmKey("FileName");
	public static final ServletConfigParmKey CACHINGENABLED = new ServletConfigParmKey("CachingEnabled");
	public static final ServletConfigParmKey INITPARAMS = new ServletConfigParmKey("InitParams");
	public static final ServletConfigParmKey DISPLAYNAME = new ServletConfigParmKey("DisplayName");
	public static final ServletConfigParmKey ISJSP = new ServletConfigParmKey("IsJsp");
	public static final ServletConfigParmKey SERVLETCONTEXT = new ServletConfigParmKey("ServletContext");
	public static final ServletConfigParmKey STARTUPWEIGHT = new ServletConfigParmKey("StartUpWeight");
	public static final ServletConfigParmKey SERVLETNAME= new ServletConfigParmKey("ServletName");
	public static final ServletConfigParmKey STATISTICSENABLED = new ServletConfigParmKey("StatisticsEnabled");
	private HashMap _map=null;
	
	public ServletConfigParmMap(){
		_map = new HashMap (10,1);
	}
	
	public void put(ServletConfigParmKey key,Object value){
		_map.put(key.getKey(),value);
	}
	
	public Object get(ServletConfigParmKey key){
		return _map.get(key.getKey());
	}
	
}
