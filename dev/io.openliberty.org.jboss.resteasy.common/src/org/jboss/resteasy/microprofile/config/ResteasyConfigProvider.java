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
