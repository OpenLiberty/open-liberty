package com.ibm.ws.messaging.lifecycle;

import static com.ibm.websphere.ras.Tr.debug;
import static com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled;

import static java.util.Objects.requireNonNull;
import static org.osgi.service.component.annotations.ConfigurationPolicy.REQUIRE;

import java.util.Map;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

@Component(service = SingletonAgent.class, configurationPolicy = REQUIRE)
public final class SingletonAgent {
	public static final TraceComponent tc = Tr.register(SingletonAgent.class);
    private final static int PREFIX_LENGTH = SingletonAgent.class.getPackage().getName().length() + 1;
    private final Singleton singleton;
    private final String type;
    
    @Activate
    public SingletonAgent(Map<String, String> props, @Reference(name="singleton", target="(id=unbound)") Singleton singleton) {
    	if (isAnyTracingEnabled() && tc.isDebugEnabled()) debug(this, tc, "SingletonAgent", props, singleton);
    	this.type = requireNonNull(props.get("id"));
        this.singleton = singleton;
    }
    String getSingletonType() { return type; }
    Singleton getSingleton() { return singleton; }
    public String toString() { return type; }
}
