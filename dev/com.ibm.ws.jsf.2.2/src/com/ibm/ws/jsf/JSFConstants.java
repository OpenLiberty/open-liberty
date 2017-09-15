/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jsf;

public class JSFConstants {

    public static String SERIALIZED_CONFIG = "com/ibm/ws/jsf/FacesConfigParser.ser";
    
    public static final String IBM_JSF_PACKAGE = "com.ibm.ws.jsf";
    
    public static String FACES_SERVLET_MAPPINGS = IBM_JSF_PACKAGE + ".servlet.mappings";
    
    //TODO: defect 209676: add ability to load JSF Configuration at webmodule startup 
    public static String LOAD_FACES_CONFIG_STARTUP = IBM_JSF_PACKAGE + ".LOAD_FACES_CONFIG_AT_STARTUP";
    //end: defect 209676:

    public static final String JSP_UPDATE_CHECK = IBM_JSF_PACKAGE + ".JSP_UPDATE_CHECK";
    
    public static String JSF_IMPL_CHECK = IBM_JSF_PACKAGE + ".JSF_IMPL_CHECK";
        
    public static final String JSP_URI_MATCHER = IBM_JSF_PACKAGE + ".JSP_URI_MATCHER";

    public static final String JSF_IMPL_ENABLED_PARAM = "com.ibm.ws.jsf.JSF_IMPL_ENABLED";

    public static final String DATA_TABLE_TAG_NULL_VAR = "com.ibm.wsspi.jsf.datatabletagnullvarattribute";  //PM05659 (original APAR was PK33787)

    public enum JSFImplEnabled{
        MyFaces, SunRI, Custom, None
    }

}
