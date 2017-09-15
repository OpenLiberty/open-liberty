/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
function buildAdminCenterButton() {
	var request = new XMLHttpRequest();
	request.open('HEAD', "/adminCenter", true);
	request.onreadystatechange = function() {
		if (request.readyState === 4) {
			if (request.status === 200) {
				buildAdminCenterDirectLink();
			} else {
				buildHowToSetupAdminCenterButton();
			}
		}
	};
	request.send();
}
function buildAdminCenterDirectLink() {
	var adminCenterURL = 
		window.location.protocol + "//" + window.location.host + "/adminCenter";
	var element = document.getElementById("welcome-section-content");
	var button = buildButton(adminCenterURL, "Open Admin Center");
	element.appendChild(button);
}
function buildHowToSetupAdminCenterButton() {
	var element = document.getElementById("welcome-section-content");
	var url = "https://developer.ibm.com/wasdev/downloads/#asset/features-com.ibm.websphere.appserver.adminCenter-1.0";
	var button = buildButton(url, "How to Setup Admin Center");
	element.appendChild(button);
}
function buildButton(url, description) {
	var anchor = document.createElement("a");
	anchor.href = url;
	var button = document.createElement("button");
	var buttonText = document.createElement("h3");
	buttonText.innerHTML = description;
	var arrowImage = document.createElement("div");
	arrowImage.className = "right-carrot";
	button.appendChild(buttonText).appendChild(arrowImage);
	anchor.appendChild(button);
	return anchor;
}
function closeUpdateBanner() {
	var updateBanner = document.getElementById("update-banner");
	updateBanner.setAttribute("style", "display: none");
	var welcomeSection = document.getElementById("welcome-section");
	welcomeSection.setAttribute("style", "margin-top: 75px");
}
function buildUpdateBanner() {
	if(isLibertyUpdateAvailable) {
		var section = document.createElement("section");
		section.setAttribute("id", "update-banner");

		var button = document.createElement("button");
		button.setAttribute("class", "x-close");
		button.setAttribute("onclick", "closeUpdateBanner()");

		var right = document.createElement("div");
		right.setAttribute("class", "x-close-right-tilt");

		var left = document.createElement("div");
		left.setAttribute("class", "x-close-left-tilt");

		section.appendChild(button);
		button.appendChild(right);
		button.appendChild(left);

		var article = document.createElement("article");
		article.setAttribute("id", "banner-container");

		var h3 = document.createElement("article");
		h3.setAttribute("id", "banner-text");
		h3.setAttribute("class", "banner-text");

		var span = document.createElement("span");
		span.setAttribute("class", "bolded");
		span.innerHTML = "Update Available. ";

		article.appendChild(h3);
		h3.appendChild(span);
		h3.innerHTML += "Click to download ";
		h3.innerHTML += 
			"<a href='" + latestReleasedVersion.availableFrom  + "'>" + 
			latestReleasedVersion.productName + 
			"</a>";

		section.appendChild(article);

		var welcomeSection = document.getElementById("welcome-section");
		document.body.insertBefore(section, document.body.firstChild);
	}
}
function loadCss() {
	var link = document.createElement("link");
	link.href = urlForCssEnhancements; // urlForCssEnhancements variable should be set in index.html
	link.type = "text/css";
	link.rel = "stylesheet";
	document.getElementsByTagName( "head" )[0].appendChild(link);
}
loadCss();
buildAdminCenterButton();
buildUpdateBanner();