package com.ibm.ws.springboot.support.version15.test.multi.context.app.child2;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;


@Configuration
@ComponentScan("com.ibm.ws.springboot.support.version15.test.multi.context.app.child2")
@PropertySource("classpath:servlet2.properties")
@EnableAutoConfiguration
public class ServletConfig2 {
    
}
