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
buildUpdateBanner();
