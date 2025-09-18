/*******************************************************************************
 * Copyright (c) 2018, 2025 IBM Corporation and others.
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

import org.springframework.boot.web.error.ErrorPage;
import org.springframework.boot.web.error.ErrorPageRegistrar;
import org.springframework.boot.web.error.ErrorPageRegistry;
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
