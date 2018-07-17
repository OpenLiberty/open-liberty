package com.ibm.ws.microprofile.reactive.streams.spi;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;


@Component(service = { ReactiveStreamsEngineImpl.class }, configurationPolicy = ConfigurationPolicy.IGNORE, immediate = true)
public class ReactiveStreamsEngineImpl{
}
