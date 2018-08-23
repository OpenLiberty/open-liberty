package com.ibm.ws.springboot.support.version15.test.multi.context.app.child1;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;


@Configuration
@ComponentScan("com.ibm.ws.springboot.support.version15.test.multi.context.app.child1")
@PropertySource("classpath:servlet1.properties")
@EnableAutoConfiguration
public class ServletConfig1 {
    
}
