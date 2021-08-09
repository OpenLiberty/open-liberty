/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.container.was;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.wsspi.anno.info.AnnotationInfo;
import com.ibm.wsspi.anno.info.AnnotationValue;

public class SipServletAnnotation {
	
	private static final TraceComponent tc = Tr.register(SipServletAnnotation.class);

	private String _servletname = null;
	private String _applicationName = null;
	private String _description = null;
	private String _className = null;
	private int _loadOnStartup = 0;
	
	private static String APP_NAME = "applicationName";
	private static String SERVLET_NAME = "name";
	private static String DESCRIPTION = "description";
	private static String LOAD_ON_STARTUP= "loadOnStartup";
    
	public SipServletAnnotation(String className, AnnotationInfo annotationValue) {
		_className = className;
		if(annotationValue != null){
            AnnotationValue value = null;

            value = annotationValue.getValue(APP_NAME);
            if(value != null){
            	_applicationName = value.getStringValue();
            }
            
            value = annotationValue.getValue(SERVLET_NAME);
            if(value != null){
            	_servletname = value.getStringValue();
            }
            
            value = annotationValue.getValue(DESCRIPTION);
            if(value != null){
            	_description = value.getStringValue();
            }
            
            value = annotationValue.getValue(LOAD_ON_STARTUP);
            if(value != null){
            	_loadOnStartup = value.getIntValue();
            }
        }
        
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
        	StringBuffer buff = new StringBuffer();
        	buff.append("Received following annotations: \nservletname: " + _servletname);
        	buff.append(" applicationName: " + _applicationName);
        	buff.append(" description: " + _description);
        	buff.append(" loadOnStartup: " + _loadOnStartup);
        	buff.append(" className: " + _className);
        	Tr.debug(tc, "processSipServletAnnotations", buff.toString());
        }
	}

}
