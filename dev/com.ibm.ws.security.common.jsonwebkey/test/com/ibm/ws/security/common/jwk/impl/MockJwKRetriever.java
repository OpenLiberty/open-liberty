package com.ibm.ws.security.common.jwk.impl;

import javax.net.ssl.SSLSocketFactory;

import org.apache.http.impl.client.HttpClientBuilder;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ssl.SSLException;
import com.ibm.wsspi.ssl.SSLSupport;



public class MockJwKRetriever extends JwKRetriever{
    public boolean jvmPropWasSet = false;
    
    public MockJwKRetriever(String configId, String sslConfigurationName, String jwkEndpointUrl, JWKSet jwkSet, SSLSupport sslSupport, boolean hnvEnabled, String jwkClientId, String jwkClientSecret, String publicKeyText, String keyLocation) {
        super(configId, sslConfigurationName, jwkEndpointUrl, jwkSet, sslSupport, hnvEnabled, jwkClientId, jwkClientSecret, publicKeyText, keyLocation);
        
    }
    
    protected HttpClientBuilder getBuilder(boolean useSystemPropertiesForHttpClientConnections)
    {        
        jvmPropWasSet = useSystemPropertiesForHttpClientConnections;
        return null;
    }
    
    protected SSLSocketFactory getSSLSocketFactory(String requestUrl, String sslConfigurationName,
            SSLSupport sslSupport) throws SSLException {
       return null;
    }
}
