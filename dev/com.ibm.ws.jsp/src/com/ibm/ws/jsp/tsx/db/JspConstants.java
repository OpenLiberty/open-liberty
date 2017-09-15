/*******************************************************************************
 * Copyright (c) 1997,2002 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jsp.tsx.db;

/* -----------------------------------------------------------------
** Copyright 1997-98 IBM Corporation.  All rights reserved.
**
** -----------------------------------------------------------------
*/
public interface JspConstants
{
//  Error Constants
final static public String IntError                     = "InternalError";
final static public String NotYetImpl                   = "NotYetImpl";
final static public String SQLException                 = "SQLException";
final static public String NullDbDriver                 = "NullDbDriver";
final static public String NullQueryString              = "NullQueryString";
final static public String NullUrl                              = "NullUrl";
final static public String InvalidRowIndex              = "InvalidRowIndex";
final static public String InvalidDbDriver              = "InvalidDbDriver";
final static public String CurrRowNotInit               = "CurrRowNotInit";
final static public String InvalidCurrRowRef            = "InvalidCurrRowRef";
final static public String NamingException              = "NamingException";
final static public String DatasourceException          = "DatasourceException";
final static public String InvalidAttrName="InvalidAttrName";
//  class names
final static String   NlsClass  = "com.ibm.servlet.jsp.db.JspNLS";
final static String   SETracer  = "com.ibm.servlet.debug.SETracer";
                                          
}
