package com.ibm.ws.springboot.support.version20.test.app;

import org.springframework.boot.web.server.ErrorPage;
import org.springframework.boot.web.server.ErrorPageRegistrar;
import org.springframework.boot.web.server.ErrorPageRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;

@Configuration
public class TestErrorPageRegistrar implements ErrorPageRegistrar  {
    @Override
    public void registerErrorPages(ErrorPageRegistry registry) {
        registry.addErrorPages(new ErrorPage(IllegalArgumentException.class, "/errorPageException.html"),
                               new ErrorPage("/defaultErrorPage.html"),
                               new ErrorPage(HttpStatus.NOT_FOUND, "/noSuchPage.html"));
    }

    @Bean
    public ErrorPageRegistrar errorPageRegistrar(){
        return new TestErrorPageRegistrar();
    }	
}
