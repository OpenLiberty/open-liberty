package io.openliberty.mcp.internal.monitor.metrics;

import java.time.Duration;
import java.util.List;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import io.openliberty.mcp.internal.monitor.McpStatAttributes;

@Component(configurationPolicy = ConfigurationPolicy.IGNORE, immediate = true)
public class MetricsManager {
private static MetricsManager instance;
	
	private static final TraceComponent tc = Tr.register(MetricsManager.class);
    
    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)
    private volatile List<McpMetricAdapter> mcpMetricRuntimes;
	
    
    @Activate
    public void activate() {
    	instance = this;
    }
    
    @Deactivate
    public void deactivate() {

    	instance = null;
    }
   
    public static MetricsManager getInstance() {
    	if (instance != null) {
        	return instance;
    	} 
    	
    	if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){
        	Tr.debug(tc, "No RestMetricManager Instance available ");
    	}
    	return null;
    }
    
    /**
     * 
     * @param httpStatAttributes
     * @param duration
     */
	public void updateMcpToolDurationMetrics(McpStatAttributes mcpStatsAttribute , Duration duration) {
        for (McpMetricAdapter adapter : mcpMetricRuntimes) {
            adapter.updateMcpMetrics(mcpStatsAttribute, duration);
        }
	}

}
