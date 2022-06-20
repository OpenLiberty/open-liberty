package com.ibm.ws.messaging.lifecycle;

import static java.util.Objects.requireNonNull;
import static org.osgi.service.component.annotations.ConfigurationPolicy.REQUIRE;

import java.util.Map;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = SingletonAgent.class, configurationPolicy = REQUIRE, property = "service.vendor=IBM")
public final class SingletonAgent {
    private final static int PREFIX_LENGTH = SingletonAgent.class.getPackage().getName().length() + 1;
    private final Singleton singleton;
    private final String type;
    @Activate
    public SingletonAgent(Map<String, String> props, @Reference(name="singleton", target="(id=unbound)") Singleton singleton) {
        this.type = requireNonNull(props.get("id"));
        this.singleton = singleton;
    }
    String getSingletonType() { return type; }
    Singleton getSingleton() { return singleton; }
    public String toString() { return String.format("%s[%s]", super.toString().substring(PREFIX_LENGTH), singleton);}
}
