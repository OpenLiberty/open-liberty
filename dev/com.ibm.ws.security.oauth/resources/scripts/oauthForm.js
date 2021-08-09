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
function submitForm(data) {
	var form = createForm(data);
	form.submit();
}

function createForm(data) {
	var form = document.createElement("form");
	form.setAttribute("action", data.authorizationUrl);
	form.setAttribute("method", "POST");
	
	form.appendChild(createInput("consentNonce", data.consentNonce));
	form.appendChild(createInput("client_id", data.client_id));
	form.appendChild(createInput("response_type", data.response_type));
	if(data.scope && data.scope.length > 0) {
		var scopes = [];
		for (var i = 0; i < data.scope.length; i++) {
			var scope = data.scope[i];
			var checkbox = document.getElementById(scope + "_checkbox");
			if (checkbox && checkbox.checked) {
				scopes[scopes.length] = data.scope[i];
			}
		}
		form.appendChild(createInput("scope", scopes.join(" ")));
	}
	if(data.state) {
		form.appendChild(createInput("state", data.state));
	}
	if(data.redirect_uri) {
		form.appendChild(createInput("redirect_uri", data.redirect_uri));
	}
	if(data.resource) {
		form.appendChild(createInput("resource", data.resource));
	}
	
	form.appendChild(createInput("prompt", (data.prompt != undefined) ? data.prompt : "none"));
	
	document.body.appendChild(form);
	return form;
}

function cancel(data) {
	var form = document.createElement("form");
	var errorType = "access_denied";
	var errorDescription = "The user has canceled the authorization request.";

	form.setAttribute("action", data.authorizationUrl);
	form.setAttribute("method", "POST");
	
	form.appendChild(createInput("error", errorType));
	form.appendChild(createInput("error_description", errorDescription));
	form.appendChild(createInput("response_type", data.response_type));
	form.appendChild(createInput("client_id", data.client_id));
	if (data.state) {
		form.appendChild(createInput("state", data.state));
	}

	document.body.appendChild(form);
	form.submit();
}

function createInput(name, value) {
	var input = document.createElement("input");
	input.setAttribute("type", "hidden");
	input.setAttribute("name", name);
	input.setAttribute("value", value);
	return input;
}
