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
package com.ibm.ws.springboot.fat40.test.app;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

// Updated for Spring 3.0 per:
//
// https://spring.io/blog/2022/02/21/spring-security-without-the-websecurityconfigureradapter
//
// https://docs.spring.io/spring-security/reference/servlet/authorization/authorize-http-requests.html
//
// https://stackoverflow.com/questions/72381114/spring-security-upgrading-the-deprecated-websecurityconfigureradapter-in-spring
//
// https://spring.io/guides/gs/securing-web/

@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		
		 http
         .authorizeHttpRequests(authorizeRequests ->
             authorizeRequests
                 .requestMatchers("/", "/hello").hasRole("USER") 
                 .anyRequest()
         )
         .formLogin(login -> 
        		 login.loginPage("/login")
        		 .permitAll());
//		 
//		http.authorizeHttpRequests()
//			.requestMatchers("/", "/hello").hasRole("USER")
//			.anyRequest();
//		http.formLogin()
//				.loginPage("/login")
//				.permitAll();
		http.logout((logout) -> logout.permitAll());

		return http.build();
	}

	@Bean
	public UserDetailsService userDetailsService() {
		UserDetails user =
			 User.withDefaultPasswordEncoder()
				.username("user")
				.password("password")
				.roles("USER")
				.build();

		return new InMemoryUserDetailsManager(user);
	}
}
