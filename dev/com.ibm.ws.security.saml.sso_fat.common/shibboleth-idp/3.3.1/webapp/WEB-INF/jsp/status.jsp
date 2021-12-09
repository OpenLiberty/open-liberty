<%@ page language="java" contentType="text/plain; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page trimDirectiveWhitespaces="true" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.util.Collection" %>
<%@ page import="java.util.Collections" %>
<%@ page import="org.joda.time.DateTime" %>
<%@ page import="org.joda.time.format.DateTimeFormatter" %>
<%@ page import="org.joda.time.format.ISODateTimeFormat" %>
<%@ page import="org.springframework.webflow.execution.RequestContext" %>
<%@ page import="org.opensaml.saml.metadata.resolver.ChainingMetadataResolver" %>
<%@ page import="org.opensaml.saml.metadata.resolver.MetadataResolver" %>
<%@ page import="org.opensaml.saml.metadata.resolver.RefreshableMetadataResolver" %>
<%@ page import="net.shibboleth.idp.Version" %>
<%@ page import="net.shibboleth.idp.saml.metadata.RelyingPartyMetadataProvider" %>
<%@ page import="net.shibboleth.idp.attribute.resolver.AttributeResolver" %>
<%@ page import="net.shibboleth.idp.attribute.resolver.DataConnector" %>
<%@ page import="net.shibboleth.idp.attribute.resolver.DataConnectorEx" %>
<%@ page import="net.shibboleth.utilities.java.support.component.IdentifiedComponent" %>
<%@ page import="net.shibboleth.utilities.java.support.service.ReloadableService" %>
<%@ page import="net.shibboleth.utilities.java.support.service.ServiceableComponent" %>
<%
final RequestContext requestContext = (RequestContext) request.getAttribute("flowRequestContext");
final DateTimeFormatter dateTimeFormatter = ISODateTimeFormat.dateTimeNoMillis();
final DateTime now = DateTime.now();
final DateTime startupTime = new DateTime(requestContext.getActiveFlow().getApplicationContext().getStartupDate());
%>### Operating Environment Information
operating_system: <%= System.getProperty("os.name") %>
operating_system_version: <%= System.getProperty("os.version") %>
operating_system_architecture: <%= System.getProperty("os.arch") %>
jdk_version: <%= System.getProperty("java.version") %>
available_cores: <%= Runtime.getRuntime().availableProcessors() %>
used_memory: <%= (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1048576 %> MB
maximum_memory: <%= Runtime.getRuntime().maxMemory() / 1048576 %> MB

### Identity Provider Information
idp_version: <%= Version.getVersion() %>
start_time: <%= startupTime.toString(dateTimeFormatter) %>
current_time: <%= now.toString(dateTimeFormatter) %>
uptime: <%= now.getMillis() - startupTime.getMillis() %> ms

<%
for (final ReloadableService service : (Collection<ReloadableService>) request.getAttribute("services")) {
    final DateTime successfulReload = service.getLastSuccessfulReloadInstant();
    final DateTime lastReload = service.getLastReloadAttemptInstant();
    final Throwable cause = service.getReloadFailureCause();

    out.println("service: " + ((IdentifiedComponent) service).getId());
    if (successfulReload != null) {
        out.println("last successful reload attempt: " + successfulReload.toString(dateTimeFormatter));
    }
    if (lastReload != null) {
        out.println("last reload attempt: " + lastReload.toString(dateTimeFormatter));
    }
    if (cause != null) {
        out.println("last failure cause: " + cause.getClass().getName() + ": " + cause.getMessage());
    }
    
    out.println();
    
    if (((IdentifiedComponent) service).getId().contains("Metadata")) {
        final ServiceableComponent<MetadataResolver> component = service.getServiceableComponent();
        if (null != component) {
            try {
                MetadataResolver rootResolver = component.getComponent();
                Collection<RefreshableMetadataResolver> resolvers = Collections.emptyList();
                
                // Step down into wrapping component.
                if (rootResolver instanceof RelyingPartyMetadataProvider) {
                    rootResolver = ((RelyingPartyMetadataProvider) rootResolver).getEmbeddedResolver();
                }
                
                if (rootResolver instanceof ChainingMetadataResolver) {
                    resolvers = new ArrayList<RefreshableMetadataResolver>();
                    for (final MetadataResolver childResolver : ((ChainingMetadataResolver) rootResolver).getResolvers()) {
                        if (childResolver instanceof RefreshableMetadataResolver) {
                            resolvers.add((RefreshableMetadataResolver) childResolver);
                        }
                    }
                } else if (rootResolver instanceof RefreshableMetadataResolver) {
                    resolvers = Collections.<RefreshableMetadataResolver>singletonList((RefreshableMetadataResolver) rootResolver);
                }
                
                for (final RefreshableMetadataResolver resolver : resolvers) {
                    final DateTime lastRefresh = resolver.getLastRefresh();
                    final DateTime lastUpdate = resolver.getLastUpdate();
    
                    out.println("\tmetadata source: " + resolver.getId());
                    if (lastRefresh != null) {
                        out.println("\tlast refresh attempt: " + lastRefresh.toString(dateTimeFormatter));
                    }
                    if (lastUpdate != null) {
                        out.println("\tlast update: " + lastUpdate.toString(dateTimeFormatter));
                    }
                    out.println();
                }
            } finally {
                component.unpinComponent();
            }
        }
    } else if (((IdentifiedComponent) service).getId().contains("AttributeResolver")) {
        final ServiceableComponent<AttributeResolver> component = service.getServiceableComponent();
        if (null != component) {
            try {
                AttributeResolver resolver = component.getComponent();
                final Collection<DataConnector> connectors = resolver.getDataConnectors().values();
                
                for (final DataConnector connector: connectors) {
                    if (connector instanceof DataConnectorEx) {
                        DataConnectorEx connectorEx = (DataConnectorEx) connector;
                    
                        final long lastFail = connectorEx.getLastFail();
                        if (0 != lastFail) {
                            DateTime failDateTime = new DateTime(lastFail);
                            out.println("\tDataConnector " +  connectorEx.getId() + ": last failed at " + failDateTime.toString(dateTimeFormatter));
                        } else {
                            out.println("\tDataConnector " +  connectorEx.getId() + ": has never failed");
                        }
                        out.println();
                    }
                }
            } finally {
                component.unpinComponent();
            }
        }
    
    }    
}
%>
