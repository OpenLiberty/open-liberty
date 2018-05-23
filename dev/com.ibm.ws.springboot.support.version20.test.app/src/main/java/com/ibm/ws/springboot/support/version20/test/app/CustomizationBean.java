package com.ibm.ws.springboot.support.version20.test.app;


import org.springframework.boot.web.server.MimeMappings;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.stereotype.Component;

@Component
public class CustomizationBean implements WebServerFactoryCustomizer<ConfigurableServletWebServerFactory> {

    @Override
    public void customize(ConfigurableServletWebServerFactory server) {
        MimeMappings mappings = new MimeMappings(MimeMappings.DEFAULT);
        mappings.add("weby","application/json");
        server.setMimeMappings(mappings);
    }

}