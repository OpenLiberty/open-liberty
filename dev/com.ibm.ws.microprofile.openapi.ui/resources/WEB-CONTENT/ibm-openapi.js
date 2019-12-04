function getDocsUrl(filter) {
	// grab only path portion of browser's URL;
	var path = location.pathname;

	// derive URI with queryParams;
	var index = location.href.indexOf(path);
	var uriPlusParams = location.href.substring(index);

	index = uriPlusParams.indexOf('#');
	if (index > -1) {
		uriPlusParams = uriPlusParams.substring(0, index);
	}

	// grab all query parameters
	index = uriPlusParams.indexOf('?');
	var queryParams = "?compact=true";
	if (index > -1) {
		queryParams += '&' + uriPlusParams.substring(index + 1, uriPlusParams.length);
	}

	if (filter !== undefined && filter != null && filter.length > 0) {
		var params = parseFilter(filter);

		for (var i = 0; i < params.length; i++) {
			if (params[i][0] != '/') {
				params[i] = '/' + params[i];
			}
			queryParams = queryParams + "&root="
					+ encodeURIComponent(params[i]);
		}
	}

	// replace /explorer with /docs
	index = path.lastIndexOf('/explorer');
	var base = path.substring(0, index);

	var new_url = base + '/docs' + queryParams;

	return new_url;
}

function parseFilter(filter) {
	var quoteChar = "";
	var buffer = "";
	var res = [];
	for (var i = 0; i < filter.length; i++) {
		var c = filter.charAt(i);
		if (quoteChar) {
			if (c == quoteChar) {
				quoteChar = "";
				if (buffer) {
					res.push(buffer);
				}
				buffer = "";
			} else {
				buffer = buffer + c;
			}
		} else {
			if ((c == "'") || (c == '"')) {
				quoteChar = c;
			} else if (c == ',') {
				if (buffer) {
					res.push(buffer);
					buffer = "";
				}
			} else {
				buffer = buffer + c;
			}
		}
	}
	if ((!quoteChar) && (buffer)) {
		res.push(buffer);
		buffer = "";
	}
	return res;
}

function getOAuth2Url() {
	var url = location.href;
	var path = location.pathname;
	var index = url.indexOf(path);
	
	return url.substring(0, index) + "/openapi/ui/oauth2-redirect.html";
}