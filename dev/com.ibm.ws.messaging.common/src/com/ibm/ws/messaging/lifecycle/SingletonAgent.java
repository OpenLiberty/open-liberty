package com.ibm.ws.messaging.lifecycle;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;

@Component(service = SingletonAgent.class, configurationPolicy = ConfigurationPolicy.REQUIRE)
final class SingletonAgent {
    private final static int PREFIX_LENGTH = SingletonAgent.class.getPackage().getName().length() + 1;
    private final Singleton singleton;

    @Activate
    SingletonAgent(@Reference(name="singleton", target="unbound") Singleton singleton) {
        this.singleton = singleton;
    }
    
    Singleton getSingleton() {
        return singleton;
    }
    
    @Override
    public String toString() {
        return String.format("%s[%s]", super.toString().substring(PREFIX_LENGTH), singleton);
    }
}
