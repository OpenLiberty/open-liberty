/*
 * Copyright (c)  2016  IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.ws.jsf22.fat.PI64714;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.RequestScoped;
import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.validator.ValidatorException;
import javax.inject.Named;

@Named
@RequestScoped
public class IndexBeanPI64714 {

	private String data1;
	
	public String getData1() {
		return data1;
	}

	public void validateData1(FacesContext context, UIComponent component, Object value) {
		if (value == null) {
			List<FacesMessage> messageList = new ArrayList();
			messageList.add(new FacesMessage(FacesMessage.SEVERITY_FATAL,
				"ValidatorException#SEVERITY_FATAL", "ValidatorException#SEVERITY_FATAL"));
			throw new ValidatorException(messageList);
		}
	}

	public String execute() {
		FacesContext context = FacesContext.getCurrentInstance();
		context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_FATAL,
			"FacesContext#SEVERITY_FATAL", "FacesContext#SEVERITY_FATAL"));
		return "index2";
	}

}
