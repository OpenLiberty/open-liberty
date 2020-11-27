package com.ibm.was.wssample.sei.echo;


public interface EchoProxyInterface {
    public String getEchoResponse( Object object, String endpointURL, String input) throws Exception;

}