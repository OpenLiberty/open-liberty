<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<!--
    Copyright (c) 2019 IBM Corporation and others.
    All rights reserved. This program and the accompanying materials
    are made available under the terms of the Eclipse Public License v1.0
    which accompanies this distribution, and is available at
    http://www.eclipse.org/legal/epl-v10.html

    Contributors:
        IBM Corporation - initial API and implementation
-->
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>Authorization Revocation Sample Page</title>
<script language="javascript">
<%
	String path = request.getRequestURL().toString();
	String context = request.getContextPath();
	int pos = path.indexOf(context);
	
	path = path.substring(0, pos + context.length());
	String componentId = request.getParameter("componentId");
	path = path + "/authzMgmt/" + componentId;
%>
	var componentId = "<%=componentId%>";
	
	if(!componentId || componentId == "null") {
		alert("Use componentId parameter to specify the OAuth20 provider.");
	}
	
	var baseUrl = "<%=path%>";
	function getXhr() {
		var xhr;
		if (window.XMLHttpRequest) {
			xhr = new XMLHttpRequest();
		} else {
			xhr = new ActiveXObject("Microsoft.XMLHTTP");
		}
		return xhr;
	}
	
	function parseToJson(text) {
		if (/^[\],:{}\s]*$/.test(text.replace(/\\['"\\\/b-ux]/g, '@').
				replace(/"[^"\\\n\r]*"|true|false|null|-?\d+(?:\.\d*)?(?:[eE][+\-]?\d+)?/g, ']').
				replace(/(?:^|:|,)(?:\s*\[)+/g, ''))) {
			return eval('(' + text + ')'); // JSLINT-IGNORE: We have to use eval here
		}
		// If the text is not JSON parseable, then return false.
		return false;
	};
	
	function createElement(name, value) {
		var retVal = document.createElement(name);
		if(value) {
			retVal.innerHTML = value;
		}
		return retVal;
	}
	
	function removeAuthz(id) {
		if(!id){
			return;
		}
		
		var xhr = getXhr();
		xhr.onreadystatechange=function() {
			if (xhr.readyState == 4) {
				if(xhr.status == 204) {
					var msgBlock = document.getElementById("msg");
					msgBlock.innerHTML = "";
					var div = document.createElement("div");
					div.innerHTML = xhr.statusText + " : " + xhr.responseText;					
					msgBlock.appendChild(div);
					var tr = document.getElementById(id);
					if(tr) {
						tr.parentNode.removeChild(tr);
					}
				} else {
					var msgBlock = document.getElementById("msg");
					msgBlock.innerHTML = "";
					var div = document.createElement("div");
					div.innerHTML = xhr.statusText + " : " + xhr.responseText;
					msgBlock.appendChild(div);
				}
			}
		};
	
		xhr.open("DELETE",baseUrl + "/" + id,true);
		xhr.send(null);
	}
	
	function getAllAuthz() {
		if(!componentId || componentId == "null") {
			return;
		}
	
		var xhr = getXhr();
		
		xhr.onreadystatechange=function() {
			if (xhr.readyState == 4) {
				if(xhr.status == 200) {
					var msgBlock = document.getElementById("msg");
					msgBlock.innerHTML = "";
					var div = document.createElement("div");
					div.innerHTML = xhr.statusText + " : " + xhr.responseText;					
					msgBlock.appendChild(div);
					
					var json = parseToJson(xhr.responseText);
					var table = document.getElementById("authzTab");
					for(var i=0; i < json.length; i++) {
						var authz = json[i];
						var tr = createElement("tr");
						tr.appendChild(createElement("td", authz.clientDisplayName));
						tr.appendChild(createElement("td", authz.scope.join(" ")));
						tr.appendChild(createElement("td", authz.createdAt));
						tr.appendChild(createElement("td", authz.lifetimeSeconds));
						tr.appendChild(createElement("td", "<input type=\"button\" onclick=\"javascript:removeAuthz('" + authz.id + "');\" value=\"remove\" />"));
						tr.setAttribute("id", authz.id);
						table.appendChild(tr);
					}
				} else {
					var msgBlock = document.getElementById("msg");
					msgBlock.innerHTML = "";
					var div = document.createElement("div");
					div.innerHTML = xhr.statusText + " : " + xhr.responseText;
					msgBlock.appendChild(div);
				}
			}
		};
	
		xhr.open("GET",baseUrl,true);
		xhr.send(null);
	}
</script>
</head>
<body onload="getAllAuthz()">
<h3>Authorization Revocation Sample Page</h3>
<table border="1px" id="authzTab">
<tr>
<th>Client Name</th><th>Scope</th><th>Created At</th><th>Lifetime</th><th>Operation</th></tr>
</tr>
</table>
<p/>
<div><b>API invocation response: </b><span id="msg"/></div>
</body>
</html>
