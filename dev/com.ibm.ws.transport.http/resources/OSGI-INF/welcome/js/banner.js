function closeUpdateBanner() {
	var updateBanner = document.getElementById("update-banner");
	updateBanner.setAttribute("style", "display: none");
	var welcomeSection = document.getElementById("welcome-section");
	welcomeSection.setAttribute("style", "margin-top: 75px");
}

function updateBannerTabOrder(media) {
	var section = document.getElementById("update-banner");
	var firstChild = (section != null) ? section.firstChild : null;
	if (media.matches) { // If media query matches width <= 850px
		if (firstChild) {
			if (firstChild.id === "banner-container") {
				section.removeChild(firstChild);
				section.appendChild(firstChild);
			}
		}
	} else {
		if (firstChild) {
			if (firstChild.id !== "banner-container") {
				section.removeChild(firstChild);
				section.appendChild(firstChild);
			}
		}
	}
}

function createCloseButton(section, messages) {
	var button = document.createElement("button");
	button.setAttribute("class", "x-close");
	button.setAttribute("onclick", "closeUpdateBanner()");
	button.setAttribute("aria-label", messages.UPDATE_BANNER_CLOSE_BUTTON);
	button.setAttribute("title", messages.UPDATE_BANNER_CLOSE_BUTTON);

	var right = document.createElement("div");
	right.setAttribute("class", "x-close-right-tilt");

	var left = document.createElement("div");
	left.setAttribute("class", "x-close-left-tilt");

	section.appendChild(button);
	button.appendChild(right);
	button.appendChild(left);
}

function createDownloadLink(section, messages) {
	var article = document.createElement("article");
	article.setAttribute("id", "banner-container");
	article.setAttribute("aria-label", messages.UPDATE_BANNER_SECTION);

	var h3 = document.createElement("article");
	h3.setAttribute("id", "banner-text");
	h3.setAttribute("class", "banner-text");
	h3.setAttribute("aria-label", messages.UPDATE_BANNER_SECTION_CONTENT);

	var span = document.createElement("span");
	span.setAttribute("class", "bolded");
	span.innerHTML = messages.HEADER_UPDATE_AVAILABLE;

	article.appendChild(h3);
	h3.appendChild(span);

    var hrefLink = "<a href='" + latestReleasedVersion.availableFrom + "' target='_blank' rel='noopener'>" + 
		               latestReleasedVersion.productName + 
                   "</a>";
    var msgDownloadLink = formatString(messages.HEADER_DOWNLOAD_LINK, [hrefLink]);
    h3.innerHTML += msgDownloadLink;

	section.appendChild(article);
}

function formatString(value, args) {
	for (var i = 0; i < args.length; i++) {
		var regexp = new RegExp('\\{' + i + '\\}', 'gi');
		value = value.replace(regexp, args[i]);
	}
	return value;
}

function buildUpdateBanner(messages) {
	if(isLibertyUpdateAvailable) {		
		var section = document.createElement("section");
		section.setAttribute("id", "update-banner");

		var media = window.matchMedia("(max-width : 850px)");
		if (media.matches) {
			createCloseButton(section, messages);
			createDownloadLink(section, messages);
		} else {
			createDownloadLink(section, messages);
			createCloseButton(section, messages);
		}
		media.addListener(updateBannerTabOrder);

		var welcomeSection = document.getElementById("welcome-section");
		document.body.insertBefore(section, document.body.firstChild);
	}
}
