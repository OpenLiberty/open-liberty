/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *******************************************************************************/
package io.openliberty.springboot.support.version30.test.concurrency.app;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;

import io.openliberty.springboot.support.version30.test.concurrency.app.AppRunner;

@Configuration
@Service
public class AppRunnerConfig {
	
	private final MyScheduledTask myScheduledTask;
	public AppRunnerConfig(MyScheduledTask myScheduledTask) {
		this.myScheduledTask = myScheduledTask;
	}

    @Bean
    @ConditionalOnProperty(name = "test.concurrency", havingValue = "AppRunner", matchIfMissing = false)
    public AppRunner appRunnerBean() {
        return new AppRunner(myScheduledTask);
    }
}
