package io.openliberty.http.monitor;

import static org.osgi.service.component.annotations.ConfigurationPolicy.IGNORE;

import java.time.Duration;

import org.eclipse.microprofile.metrics.Histogram;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.MetadataBuilder;
import org.eclipse.microprofile.metrics.MetricID;
import org.eclipse.microprofile.metrics.Tag;
import org.eclipse.microprofile.metrics.Timer;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.MetricUnits;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import io.openliberty.microprofile.metrics50.SharedMetricRegistries;

@Component(configurationPolicy = IGNORE)
public class RestMetricManager {

    static SharedMetricRegistries sharedMetricRegistries;
	
	@Reference
	public void setSharedMetricRegistries(SharedMetricRegistries sharedMetricRegistries) {
		this.sharedMetricRegistries = sharedMetricRegistries;
	}
	
	
	public static void updateHttpMetrics(HttpStatAttributes httpStatAttributes, Duration duration) {
		
		if(sharedMetricRegistries == null) {
			//System.out.println("did not acquire shared metric registries");
			return;
		}
		
		MetricRegistry vendorRegistry = sharedMetricRegistries.getOrCreate(MetricRegistry.VENDOR_SCOPE);
		
		Metadata md = new MetadataBuilder().withName("http.metric").withUnit(MetricUnits.SECONDS).build();
		
		//MetricID mid = new MetricID("http.metric", retrieveTags(httpStatAttributes));

		//Histogram httpHistogram = vendorRegistry.histogram(md,retrieveTags(httpStatAttributes));
		//httpHistogram.update(duration.toMillis());
		
		Timer httpTimer = vendorRegistry.timer(md,retrieveTags(httpStatAttributes));
		httpTimer.update(duration);
		
	}
	
	private static Tag[] retrieveTags(HttpStatAttributes httpStatAttributes) {

		
		Tag requestMethod = new Tag("request_method", httpStatAttributes.getRequestMethod() );
		Tag scheme = new Tag("http_scheme", httpStatAttributes.getScheme());

		
		Integer status = httpStatAttributes.getResponseStatus().orElse(-1);
		Tag responseStatus = new Tag("response_status", status == -1 ? "" : status.toString());
		
		Tag httpRoute = new Tag("http_route", httpStatAttributes.getHttpRoute().orElse(""));
		
		Tag networkProtoclName = new Tag("network_name",httpStatAttributes.getNetworkProtocolName());
		Tag networkProtocolVersion = new Tag("network_version",httpStatAttributes.getNetworkProtocolVersion());
		
		
		Tag serverName = new Tag("server_name",httpStatAttributes.getServerName());
		Tag serverPort = new Tag("server_port",String.valueOf(httpStatAttributes.getServerPort()));
		
		Tag[] ret = new Tag[] {requestMethod, scheme, responseStatus, httpRoute, networkProtoclName, networkProtocolVersion, serverName, serverPort};
		
		return ret;
	}
	
}
