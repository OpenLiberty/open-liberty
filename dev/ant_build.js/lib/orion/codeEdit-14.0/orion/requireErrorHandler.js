	
	function createErrorPane() {
		var theDiv = document.createElement("div");
		theDiv.style.display = "block";
		theDiv.style.position = "absolute";
		theDiv.style.width = "100%";
		theDiv.style.height = "100%";
		theDiv.style.background = "white";
		return theDiv;
	}

	function createElement(type, text, padding) {
		var theElement = document.createElement(type);
		theElement.textContent = text;
		theElement.style.width = "100%";
		theElement.style.textAlign = "center";
		if (padding) {
			theElement.style.paddingTop = padding + "px";
		}
		return theElement;
	}

	function createRefreshButton(y) {
		var button  = document.createElement("button");
		button.textContent = "Refresh";
		button.style.width = "75px";
		button.style.height = "25px";	
		button.onclick = function () {
			window.location.reload();
		}
		var btnDiv = createElement("div", "", y);
		btnDiv.appendChild(button);
		return btnDiv;
	}

	self.errback = function(err) {
		var errorPane = createErrorPane();
		errorPane.appendChild(createElement("h1", "An error occurred loading the page"));
		errorPane.appendChild(createRefreshButton());
		errorPane.appendChild(createElement("h2", "Error: " + err.requireType, 50));
		
		if (err.requireModules.length > 0) {
			errorPane.appendChild(createElement("h2", "Module: " +  err.requireModules[0]));
		}
		document.body.innerHTML = "";
		document.body.appendChild(errorPane);
	};
	
	
