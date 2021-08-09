This sample project contains two no-ship features that are strictly for testing / demonstrative purposes. 

    1.  jaccProxyProvider-1.0
        This feature is Java EE 7 and 8 compliant and works with the `jacc-1.5` feature.
    2.  jaccProxyProvider-2.0
        This feature is Jakarta EE 9+ compliant and works with the `appAuthorization-2.0` feature.

The features each provide a custom instance of a `com.ibm.wsspi.security.authorization.jacc.ProviderService` that is used to return provider specific implementations of `java.security.Policy` and `javax.security.jacc.PolicyConfigurationFactory` or in the case of Jakarta EE 9+, `jakarta.security.jacc.PolicyConfigurationFactory`.

To use the features start by building the features from the 'dev' directory: 

    bash$ ./gradlew com.ibm.ws.security.authorization.jacc.sample.proxyprovider:assemble

This will generate two feature bundles (JAR files) in `com.ibm.ws.security.authorization.jacc.sample.proxyprovider/build/libs`:

    1. com.ibm.ws.security.authorization.jacc.sample.proxyprovider.jar           (Java EE 7/8 enabled)
    2. com.ibm.ws.security.authorization.jacc.sample.proxyprovider.jakarta.jar   (Jakarta EE 9+ enabled)
    
Copy the feature(s) bundles into your Liberty install:

    cp com.ibm.ws.security.authorization.jacc.sample.proxyprovider/build/libs/com.ibm.ws.security.authorization.jacc.sample.proxyprovider.jar ${wlp.install.dir}/usr/extension/lib/com.ibm.ws.security.authorization.jacc.sample.proxyprovider_1.0.jar
    cp com.ibm.ws.security.authorization.jacc.sample.proxyprovider/build/libs/com.ibm.ws.security.authorization.jacc.sample.proxyprovider.jar ${wlp.install.dir}/usr/extension/lib/com.ibm.ws.security.authorization.jacc.sample.proxyprovider_2.0.jar
    
Copy the feature manifest files into your Liberty install:

    cp com.ibm.ws.security.authorization.jacc.sample.proxyprovider/publish/usr/extension/lib/features/jaccProxyProvider-1.0.mf ${wlp.install.dir}/usr/extension/lib/features/
    cp com.ibm.ws.security.authorization.jacc.sample.proxyprovider/publish/usr/extension/lib/features/jaccProxyProvider-2.0.mf ${wlp.install.dir}/usr/extension/lib/features/
    
To use the feature in your server.xml file (with jaccProxyProvider-1.0 as an example):

```
<server>
   <featureManager>
      <feature>usr:jaccProxyProvider-1.0</feature>
   </featureManager>
   
   <usr_jaccProxyProvider policyProviderClass="org.acme.myJaccPolicyProvider" policyConfigurationFactoryClass="org.acme.myJaccPolicyConfigurationFactory" />
</server>
```

The configuration is not modifiable at runtime. This limitation is because PolicyConfigurationFactory caches away the class once it is loaded and changing the properties at runtime will no longer have an effect. The server will need to be restarted for server.xml configuration updates to take effect to the runtime.

Ensure that the classes specified in the `usr_jaccProxyProvider` configuration are in Liberty's shared global library by putting them into one of the following locations.

- ${shared.config.dir}/lib/global/
- ${server.config.dir}/lib/global/