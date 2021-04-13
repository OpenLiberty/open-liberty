<%@ page language="java" contentType="text/plain; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page trimDirectiveWhitespaces="true" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.Set" %>
<%@ page import="java.util.Map.Entry" %>
<%@ page import="java.util.Collection" %>
<%@ page import="java.util.Collections" %>
<%@ page import="java.util.Optional" %>
<%@ page import="java.util.ServiceLoader" %>
<%@ page import="java.util.ServiceLoader.Provider" %>
<%@ page import="java.time.Duration" %>
<%@ page import="java.time.Instant" %>
<%@ page import="java.time.format.DateTimeFormatter" %>
<%@ page import="org.springframework.core.env.Environment" %>
<%@ page import="org.springframework.webflow.execution.RequestContext" %>
<%@ page import="net.shibboleth.idp.Version" %>
<%@ page import="com.codahale.metrics.MetricSet" %>
<%@ page import="com.codahale.metrics.Gauge" %>
<%@ page import="net.shibboleth.idp.module.IdPModule" %>
<%@ page import="net.shibboleth.idp.plugin.IdPPlugin" %>
<%@ page import="net.shibboleth.idp.module.ModuleContext" %>
<%@ page import="net.shibboleth.idp.saml.metadata.impl.ReloadingRelyingPartyMetadataProvider" %>
<%@ page import="net.shibboleth.idp.attribute.resolver.AttributeResolver" %>
<%@ page import="net.shibboleth.idp.attribute.resolver.impl.AttributeResolverImpl" %>
<%@ page import="net.shibboleth.idp.attribute.resolver.DataConnector" %>
<%@ page import="net.shibboleth.utilities.java.support.component.IdentifiedComponent" %>
<%@ page import="net.shibboleth.utilities.java.support.service.ReloadableService" %>
<%@ page import="net.shibboleth.utilities.java.support.service.ServiceableComponent" %>
<%
final RequestContext requestContext = (RequestContext) request.getAttribute("flowRequestContext");
final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ISO_INSTANT;
final Instant now = Instant.now();
final Instant startupTime = Instant.ofEpochMilli(requestContext.getActiveFlow().getApplicationContext().getStartupDate());
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
start_time: <%= dateTimeFormatter.format(startupTime) %>
current_time: <%= dateTimeFormatter.format(now) %>
uptime: <%= Duration.ofMillis(now.toEpochMilli() - startupTime.toEpochMilli()).toString() %>

<%
out.println();
out.println();
out.println("enabled modules: ");
final ModuleContext moduleContext =
    new ModuleContext(((Environment) request.getAttribute("environment")).getProperty("idp.home"));
for (final IdPModule module : ServiceLoader.load(IdPModule.class)) {
    if (module.isEnabled(moduleContext)) {
        out.println("\t" + module.getId() + " (" + module.getName(moduleContext) + ")");
    }
}
out.println();

out.println("installed plugins: ");
for (final IdPPlugin plugin : ServiceLoader.load(IdPPlugin.class)) {
    out.println("\t" + plugin.getPluginId() + " Version " + plugin.getMajorVersion() + "." + plugin.getMinorVersion() + "." + plugin.getPatchVersion());
}
out.println();


for (final ReloadableService service : (Collection<ReloadableService>) request.getAttribute("services")) {
    final Instant successfulReload = service.getLastSuccessfulReloadInstant();
    final Instant lastReload = service.getLastReloadAttemptInstant();
    final Throwable cause = service.getReloadFailureCause();

    out.println("service: " + ((IdentifiedComponent) service).getId());
    if (successfulReload != null) {
        out.println("last successful reload attempt: " + dateTimeFormatter.format(successfulReload));
    }
    if (lastReload != null) {
        out.println("last reload attempt: " + dateTimeFormatter.format(lastReload));
    }
    if (cause != null) {
        out.println("last failure cause: " + cause.getClass().getName() + ": " + cause.getMessage());
    }
    
    out.println();
    
    if (((IdentifiedComponent) service).getId().contains("Metadata")) {
    
    	final MetricSet metrics = (MetricSet) request.getAttribute("metadataResolverGaugeSet");
    	if (metrics == null || metrics.getMetrics().get("net.shibboleth.idp.metadata.refresh") == null) {
    		out.println("No Metadata Resolver Gauge Set Found");
    		continue;
    	}
    	final Gauge<Map<String,Instant>> refreshes = (Gauge<Map<String,Instant>>) metrics.getMetrics().get("net.shibboleth.idp.metadata.refresh");
    	final Gauge<Map<String,Instant>> updates = (Gauge<Map<String,Instant>>) metrics.getMetrics().get("net.shibboleth.idp.metadata.update");
    	final Gauge<Map<String,Instant>> successes = (Gauge<Map<String,Instant>>) metrics.getMetrics().get("net.shibboleth.idp.metadata.successfulRefresh");
    	final Gauge<Map<String,Instant>> rootValids = (Gauge<Map<String,Instant>>) metrics.getMetrics().get("net.shibboleth.idp.metadata.rootValidUntil");
    	final Gauge<Map<String,String>> errors = (Gauge<Map<String,String>>) metrics.getMetrics().get("net.shibboleth.idp.metadata.error");
    	
		Set<Entry<String, Instant>> entrySet = refreshes.getValue().entrySet();
    	if (entrySet.isEmpty()) {
	    	out.println("\tNo Metadata Resolver has ever attempted a reload");
			out.println();
			continue;
		}
		for (final Entry<String, Instant> mr : entrySet) {
			final String resolverId = mr.getKey();
			final Instant lastRefresh = mr.getValue();
            final Instant lastUpdate = updates == null ? null : updates.getValue().get(resolverId);
			final Instant lastSuccessfulRefresh = successes == null ? null : successes.getValue().get(resolverId);
			final Instant rootValidUntil = rootValids == null ? null : rootValids.getValue().get(resolverId);
		    final String lastError = errors == null ? null : errors.getValue().get(resolverId);
 
            out.println("\tmetadata source: " + resolverId);
            if (lastRefresh != null) {
                out.println("\tlast refresh attempt: " + dateTimeFormatter.format(lastRefresh));
            }
            if (lastSuccessfulRefresh != null) {
                out.println("\tlast successful refresh: " + dateTimeFormatter.format(lastSuccessfulRefresh));
            }
            if (lastUpdate != null) {
                out.println("\tlast update: " + dateTimeFormatter.format(lastUpdate));
            }
            if (lastError != null) {
                out.println("\tlast error: " + lastError);
            }
            if (rootValidUntil != null) {
                out.println("\troot validUntil: " + dateTimeFormatter.format(rootValidUntil));
            }
            out.println();
		}		    
    } else if (((IdentifiedComponent) service).getId().contains("AttributeResolver")) {
    
    	final MetricSet metrics = (MetricSet) request.getAttribute("attributeResolverGaugeSet");
    	if (metrics == null || metrics.getMetrics().get("net.shibboleth.idp.attribute.resolver.failure") == null) {
    		out.println("No Attribute Resolver Gauge Set Found");
    		continue;
    	}
    	final Gauge<Map<String,Instant>> failGauge =
    	        (Gauge<Map<String,Instant>>) metrics.getMetrics().get("net.shibboleth.idp.attribute.resolver.failure");
    	final Set<Entry<String,Instant>> failSet = failGauge.getValue().entrySet();
    	if (failSet.isEmpty()) {
	    	out.println("\tNo Data Connector has ever failed");
			out.println();
			continue;
		}
        final Gauge<Map<String,Instant>> successGauge =
                (Gauge<Map<String,Instant>>) metrics.getMetrics().get("net.shibboleth.idp.attribute.resolver.success");
        final Map<String,Instant> successMap = successGauge.getValue();
        final ArrayList<String> failingConnectors = new ArrayList<>();
    	for (final Entry<String, Instant> en : failSet) {
			final String connectorId = en.getKey();
            final Instant lastFail = en.getValue();
			out.println("\tDataConnector " +  connectorId + ": last failed at " + dateTimeFormatter.format(lastFail));
            out.println();
            final Instant lastSuccess = successMap.get(connectorId);
            if (lastSuccess == null || lastSuccess.isBefore(lastFail)) {
                failingConnectors.add(connectorId);
            }
        }
        out.println("\tCurrently failing: " + failingConnectors);
        out.println();
    }
}
%>
