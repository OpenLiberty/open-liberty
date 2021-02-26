package org.jboss.resteasy.microprofile.config;

import java.security.AccessController;
import java.security.PrivilegedAction;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigBuilder;

import io.openliberty.restfulWS.config.ConfigProviderResolverImpl;

public final class ResteasyConfigProvider
{
   private static final ConfigProviderResolverImpl INSTANCE = new ConfigProviderResolverImpl(); // Liberty change

   //Liberty change start
   public static ConfigProviderResolverImpl getInstance() {
       return INSTANCE;
   }
   //Liberty change end

   public static Config getConfig() {
      // Liberty change start
      //return ConfigProvider.getConfig(ResteasyConfigProvider.class.getClassLoader());
      return INSTANCE.getConfig();
      //
   }

   public static void registerConfig(Config config) {
      // Liberty change start
      //ConfigProviderResolver.instance().registerConfig(config, ResteasyConfigProvider.class.getClassLoader());
      INSTANCE.registerConfig(config, getTCCL());
      // Liberty change end
   }

   public static ConfigBuilder getBuilder() {
      // Liberty change start
      //return ConfigProviderResolver.instance().getBuilder().forClassLoader(ResteasyConfigProvider.class.getClassLoader());
      return INSTANCE.getBuilder().forClassLoader(ResteasyConfigProvider.class.getClassLoader());
      // Liberty change end
   }

   private static ClassLoader getTCCL() {
       return AccessController.doPrivileged((PrivilegedAction<ClassLoader>) () ->
           Thread.currentThread().getContextClassLoader());
   }
}
