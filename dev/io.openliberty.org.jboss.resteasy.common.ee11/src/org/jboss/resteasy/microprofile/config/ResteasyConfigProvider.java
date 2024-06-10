/*
 * JBoss, Home of Professional Open Source.
 *
 * Copyright 2024 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.resteasy.microprofile.config;

import java.security.AccessController;
import java.security.PrivilegedAction;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.spi.ConfigBuilder;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;

/**
 * @see org.jboss.resteasy.spi.config.Configuration
 * @see org.jboss.resteasy.spi.config.ConfigurationFactory
 * @deprecated Use the {@link org.jboss.resteasy.spi.config.Configuration}
 */
@Deprecated
public final class ResteasyConfigProvider
{
   public static Config getConfig() {
      return ConfigProvider.getConfig(getThreadContextClassLoader());
   }

   public static void registerConfig(Config config) {
      ConfigProviderResolver.instance().registerConfig(config, getThreadContextClassLoader());
   }

   public static ConfigBuilder getBuilder() {
      return ConfigProviderResolver.instance().getBuilder().forClassLoader(getThreadContextClassLoader());
   }

   // Liberty change - use TCCL instead of this class's loader - also note the 3 methods above that call this method
   private static ClassLoader getThreadContextClassLoader() {
       if (System.getSecurityManager() == null) {
           return Thread.currentThread().getContextClassLoader();
       }
       return AccessController.doPrivileged((PrivilegedAction<ClassLoader>) () -> 
           Thread.currentThread().getContextClassLoader());
   }
}
