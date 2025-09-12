package com.ibm.ws.fat.util.tck;

import java.net.URL;

import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;

import tck.arquillian.porting.lib.spi.AbstractTestArchiveProcessor;

//Stub file because I'm only running the CDI tests that don't need any of this
public class PlatformTCKArchiveProcessor extends AbstractTestArchiveProcessor {

    @Override
    public void processClientArchive(JavaArchive arg0, Class<?> arg1, URL arg2) {
        // TODO Auto-generated method stub
    }

    @Override
    public void processEarArchive(EnterpriseArchive arg0, Class<?> arg1, URL arg2) {
        // TODO Auto-generated method stub
    }

    @Override
    public void processEjbArchive(JavaArchive arg0, Class<?> arg1, URL arg2) {
        // TODO Auto-generated method stub
    }

    @Override
    public void processParArchive(JavaArchive arg0, Class<?> arg1, URL arg2) {
        // TODO Auto-generated method stub
    }

    @Override
    public void processRarArchive(JavaArchive arg0, Class<?> arg1, URL arg2) {
        // TODO Auto-generated method stub
    }

    @Override
    public void processWebArchive(WebArchive arg0, Class<?> arg1, URL arg2) {
        // TODO Auto-generated method stub
    }
}
