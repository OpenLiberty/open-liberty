<%
final org.springframework.web.context.WebApplicationContext springContext =
    org.springframework.web.context.support.WebApplicationContextUtils.getRequiredWebApplicationContext(request.getServletContext());

String path = springContext.getEnvironment().getProperty("idp.entityID.metadataFile");
if (path != null) {
    path = springContext.getEnvironment().resolvePlaceholders(path.replace("%{", "${"));
} else {
    path = springContext.getEnvironment().getProperty("idp.home") + "/metadata/idp-metadata.xml";	
}

if (path.isEmpty()) {
    response.sendError(404);
} else {
    java.io.InputStreamReader in = null;
    try {
        in = new java.io.InputStreamReader(new java.io.FileInputStream(path),"UTF8");
        int i;
        while ((i = in.read()) != -1) {
            out.write(i);
        }
    } catch (final java.io.IOException e) {
        out.println(e.getMessage());
        return;
    } finally {
        if (null != in) {
            try {
                in.close();
            } catch (java.io.IOException e) {
            }
        }
    }

    final String acceptHeader = request.getHeader("Accept");
    if (acceptHeader != null && !acceptHeader.contains("application/samlmetadata+xml")) {
        response.setContentType("application/xml");
    } else {
        response.setContentType("application/samlmetadata+xml");
    }
}
%>