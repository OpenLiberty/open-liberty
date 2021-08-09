<!--
    Copyright (c) 2001, 2012 IBM Corporation and others.
    All rights reserved. This program and the accompanying materials
    are made available under the terms of the Eclipse Public License v1.0
    which accompanies this distribution, and is available at
    http://www.eclipse.org/legal/epl-v10.html

    Contributors:
        IBM Corporation - initial API and implementation
-->
<%@ page language="java" contentType="text/html; charset=UTF-8"

	pageEncoding="UTF-8"
	
	import="java.util.List"
	import="java.util.Map"
	import="java.util.HashMap"
	import="java.util.Set"	
	import="java.util.ArrayList"
	import="java.util.Collection"
	
	import="com.ibm.oauth.core.api.OAuthComponentFactory"
	import="com.ibm.oauth.core.api.OAuthComponentInstance"
	import="com.ibm.oauth.core.api.config.OAuthComponentConfiguration"
	import="com.ibm.oauth.core.api.config.OAuthComponentConfigurationConstants"
	import="com.ibm.oauth.core.api.oauth20.OAuth20Component"
	import="com.ibm.oauth.core.api.OAuthResult"
	
	import="com.ibm.ws.security.oauth20.plugins.db.CachedDBClientProvider"
	import="com.ibm.ws.security.oauth20.plugins.BaseClient"
	import="com.ibm.ws.security.oauth20.api.OAuth20Provider"
	import="com.ibm.ws.security.oauth20.api.OAuth20ProviderFactory"
	import="com.ibm.ws.security.oauth20.api.OAuth20ProviderConfiguration"
	import="com.ibm.ws.security.oauth20.util.OAuth20Parameter"
%>
<%

	if ("changeprovider".equals(request.getParameter("action"))) {
		String providerName = request.getParameter("providerdropdown");
		session.setAttribute("providerName", providerName);
	}
	String defaultProvider = (String)session.getAttribute("providerName");

	String dbClientProviderClassName = "com.ibm.ws.security.oauth20.plugins.db.CachedDBClientProvider";
	String clientProviderClassParamName = "oauth20.client.provider.classname";
	
	HashMap<String, OAuth20Provider> providers = new HashMap<String, OAuth20Provider>(); 
	Map<String, OAuth20Provider> allProviders = OAuth20ProviderFactory.getAllOAuth20Providers();
	if (allProviders != null) {
    	Set<String>	providerIDs = allProviders.keySet();
	    for (String providerID : providerIDs) {
		    OAuth20Provider provider = allProviders.get(providerID);
		    OAuth20ProviderConfiguration oauthconfig = provider.getConfiguration();
		    List<OAuth20Parameter> parms = oauthconfig.getParameters();
		    for (OAuth20Parameter parm : parms) {
		        if (parm.getName().equals("oauth20.client.provider.classname")) {
			        List<String> values = parm.getValues();
			        if (values != null && !values.isEmpty() && 
			    	    values.get(0).equals(dbClientProviderClassName)) {
			        	providers.put(providerID, provider);
			    	    break;
			        }
		        }
		    }
	    }
	}
	if (providers.isEmpty()) {
		%><b><font COLOR="RED">No OAuth providers with database client store found</b></font><%
		return;
	}
	
	if (defaultProvider == null || "".equals(defaultProvider)) {
		defaultProvider = providers.values().iterator().next().getID();
	}
	
	OAuth20Provider provider = OAuth20ProviderFactory.getOAuth20Provider(defaultProvider);
	OAuth20Component component = provider.getComponent();
	OAuthComponentConfiguration oauthconfig = provider.getConfiguration();
	
	CachedDBClientProvider clientProvider = new CachedDBClientProvider();
	clientProvider.init(oauthconfig);
	
	if ("add".equals(request.getParameter("action"))) {
		String displayName = request.getParameter("display_name");
		String id = request.getParameter("client_id");
		String secret = request.getParameter("client_secret");
		String redirect = request.getParameter("redirect_uri");
		BaseClient newClient = new BaseClient(oauthconfig.getUniqueId(), id, secret, displayName, redirect, true);
		clientProvider.put(newClient);
	}
	else if ("delete".equals(request.getParameter("action"))) {
		String id = request.getParameter("client_id");
		clientProvider.delete(id);
	}
%>
	 
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<meta http-equiv="Pragma" content="no-cache">
<link rel="stylesheet" href="template.css" type="text/css">
<title>Clients Table</title>
<script type="text/javascript">
function editClient(displayName, clientId, clientSecret, redirectURI) {
	    var form = document.forms['addform'];
	    form.display_name.value = displayName;
	    form.client_id.value = clientId;
	    form.client_secret.value = clientSecret;
	    form.redirect_uri.value = redirectURI;
}
</script>
</head>
<body>
<%@ include file="header.jsp"%>
<h1>OAuth Client Manager for DB Client Store</h1>
Manage OAuth clients for providers that use a DB client store (clientStore="database").
<br><br>
For providers that use server config for the client store (clientStore="serverConfig") you can
<br> 
manage the clients by either editing server.xml or using the Liberty Profile development tool for Eclipse.
<br><br><br>
<form name="providerform" method="POST" action="clienttable.jsp">
<input type="hidden" name="action" value="changeprovider" />
<b>OAuth provider: </b><select name="providerdropdown">
<%	for (OAuth20Provider foundprovider : providers.values()) { %>
	<option VALUE="<%=foundprovider.getID()%>"<% if (defaultProvider.equals(foundprovider.getID())) {%>selected="selected"<%}%>><%=foundprovider.getID()%></option>
<%	} %>
</select>
<input type="submit" name="submit" value="Change" />
</form>
<%
	String clientProviderType = oauthconfig.getConfigPropertyValue(OAuthComponentConfigurationConstants.OAUTH20_CLIENT_PROVIDER_CLASSNAME);
	if (!dbClientProviderClassName.equals(clientProviderType)) {
		%>
			<font COLOR="dark-yellow"><b>Custom Client Provider Detected</b></font><br>
			This page requires a client provider of type: <b><%=dbClientProviderClassName%></b><br>
			Configuration is using a client provider of type: <b><%=clientProviderType%></b><br>
			</body></html>
		<% 
		return;
	} 

%>
<table border=1 cellpadding=5 cellspacing=0><tr>
<td><b>Display Name</b></td>
<td><b>Client ID</b></td>
<td><b>Client Secret</b></td>
<td><b>Redirect URI</b></td>
<td><b>Edit</b></td>
<td><b>Remove</b></td>
</tr>
<%
	Collection<BaseClient> clients = clientProvider.getAll();
	for (BaseClient client : clients) {
%>
  	<tr>
		<td><%=client.getDisplayName()%></td>
		<td><%=client.getClientId()%></td>
		<td><%=client.getClientSecret()%></td>
		<td><%=client.getRedirectUris()%></td>
		<td>
			<form name="edit<%=client.getClientId()%>">
			<input type="button" name="edit" value="Edit" onClick="javascript:editClient('<%=client.getDisplayName()%>','<%=client.getClientId()%>','<%=client.getClientSecret()%>','<%=client.getRedirectUris()%>' )" />
			</form>
		</td>
		<td>
			<form name="removeform<%=client.getClientId()%>" method="POST" action="clienttable.jsp">
			<input type="hidden" name="action" value="delete" />
			<input type="hidden" name="client_id" value="<%=client.getClientId()%>" />
			<input type="submit" name="delete" value="Delete" />
			</form>
		</td>
	</tr>
<%
	}
%>
</table><br>
<br>
<b>Add or update a client:</b><br>
<br>
<form name="addform" method="POST" action="clienttable.jsp">
<input type="hidden" name="action" value="add" />
<table cellspacing=6>
<tr><td>Display Name</td><td><input type="text" name="display_name" /></td></tr>
<tr><td>Client Id</td><td><input type="text" name="client_id" /></td></tr>
<tr><td>Client Secret</td><td><input type="text" name="client_secret" /></td></tr>
<tr><td>Redirect URI</td><td><input type="text" name="redirect_uri" size="50" /></td></tr>
<tr><td colspan="2"><input type="submit" name="submit" value="Add/Update" /></td></tr>
</table>
</form>


</body>
</html>