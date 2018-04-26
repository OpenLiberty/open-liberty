package com.ibm.ws.springboot.support.version15.test.app;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

@EnableWebSecurity
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {
    @Override
    protected void configure(HttpSecurity httpSecurity) throws Exception { 	
    	 httpSecurity
         .csrf().disable()
         .authorizeRequests()
         .antMatchers("/hello").fullyAuthenticated()
             .and()
         .formLogin()
         .loginPage("/login")
         .permitAll();
         
    	 
    	 httpSecurity
    	.authorizeRequests()
     	.anyRequest()
     	.permitAll(); 		
    }

    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
        auth
            .inMemoryAuthentication()
                .withUser("user").password("password").roles("USER");
    }
}
