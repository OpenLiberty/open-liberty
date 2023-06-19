/*******************************************************************************
 * Copyright (c) 2018,2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.springboot.fat30.test.app;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

// Updated for Spring 3.0 per:
//
// https://spring.io/blog/2022/02/21/spring-security-without-the-websecurityconfigureradapter
//
// https://docs.spring.io/spring-security/reference/servlet/authorization/authorize-http-requests.html
//
// https://stackoverflow.com/questions/72381114/spring-security-upgrading-the-deprecated-websecurityconfigureradapter-in-spring

@Configuration
public class SecurityConfiguration {

    @Bean
    @SuppressWarnings("deprecation")
    protected SecurityFilterChain configure(HttpSecurity httpSecurity) throws Exception {
    	// 'csrf', 'authorizedHttpRequests', and 'formLogin' are deprecated, possibly to
    	// be removed in spring security 7.0.
    	// However, replacing them as recommended adds quite a lot complexity.
    	
        httpSecurity
            .csrf().disable()
            .authorizeHttpRequests()
            	.requestMatchers("/hello").fullyAuthenticated()
            	.anyRequest().permitAll();
        httpSecurity
        	.formLogin().loginPage("/login").permitAll();

        return httpSecurity.build();
    }

    @Bean
    @SuppressWarnings("deprecation")
    public InMemoryUserDetailsManager userDetailsService() {
        // 'withDefaultPasswordEncoder' is deprecated, but not really.
        //
        // Deprecation is artificially set to highlight that the method
        // is unsafe in production environments.

        UserDetails user = User.withDefaultPasswordEncoder()
            .username("user")
            .password("password")
            .roles("USER")
            .build();

        return new InMemoryUserDetailsManager(user);
    }
}
