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
package com.ibm.ws.jsp.translator.visitor.validator;

import java.lang.reflect.Method;

import javax.servlet.jsp.tagext.FunctionInfo;

import org.apache.jasper.compiler.ELNode;

import com.ibm.ws.jsp.JspCoreException;
import com.ibm.ws.jsp.translator.JspTranslationException;
import com.ibm.ws.jsp.translator.utils.FunctionSignature;
import com.ibm.ws.jsp.translator.utils.JspTranslatorUtil;

class MapperELVisitor extends ELNode.Visitor {
	ValidateFunctionMapper fmapper;
	ClassLoader loader=null;

	MapperELVisitor(ValidateFunctionMapper fmapper, ClassLoader loader) {
		this.fmapper = fmapper;
		this.loader = loader;
	}

	public void visit(ELNode.Function n) throws JspTranslationException {
        FunctionInfo fInfo = null;
		if (n!=null) {	
			Class c = null;
			Method method = null;
			try {
		        fInfo = n.getFunctionInfo();
		        if (fInfo!=null) {
			        String fClass = fInfo.getFunctionClass();
			        if (loader!=null) {
			        	c = loader.loadClass(fInfo.getFunctionClass());
			        }
		        }
			} catch (ClassNotFoundException e){					
                throw new JspTranslationException("jsp.error.function.classnotfound", new Object[] { n.getFunctionInfo().getFunctionClass()+" "+
                		n.getPrefix()+ ':' + n.getName(), e.getMessage()});					
			}
			if (c!=null) {
				String paramTypes[] = n.getParameters();
				int size = paramTypes.length;
				Class params[] = new Class[size];
				int i = 0;
				try {
					for (i = 0; i < size; i++) {
						params[i] = JspTranslatorUtil.toClass(paramTypes[i], loader);
					}
					method = c.getDeclaredMethod(n.getMethodName(), params);
				} catch (ClassNotFoundException e) {
	                throw new JspTranslationException("jsp.error.function.classnotfound", new Object[] { paramTypes[i]+" "+
	                		n.getPrefix() + ':' + n.getName(), e.getMessage()});					
				} catch (NoSuchMethodException e) {
	                throw new JspTranslationException("jsp.error.noFunctionMethod", new Object[] { n.getMethodName()+" "+
	                		n.getName()+" "+ c.getName(), e.getMessage()});					
				}
				fmapper.mapFunction(n.getPrefix() + ':' + n.getName(), method);
	            FunctionSignature fnSignature;
				try {
					fnSignature = new FunctionSignature(fInfo.getFunctionClass(), fInfo.getFunctionSignature(), n.getUri(), loader);
				} catch (JspCoreException e) {
	                throw new JspTranslationException("jsp.error.el.function.cannot.parse", e);
				}
				fmapper.mapSignature(n.getPrefix() + ':' + n.getName(), fnSignature);
			}
		}				
	}
}
