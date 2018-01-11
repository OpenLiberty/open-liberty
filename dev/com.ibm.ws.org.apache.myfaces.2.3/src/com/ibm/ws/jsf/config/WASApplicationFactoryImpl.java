/*
* IBM Confidential
*
* OCO Source Materials
*
* Copyright IBM Corp. 2018
*
* The source code for this program is not published or otherwise divested 
* of its trade secrets, irrespective of what has been deposited with the 
* U.S. Copyright Office.
*/
package com.ibm.ws.jsf.config;

import javax.faces.application.Application;

import org.apache.myfaces.application.ApplicationFactoryImpl;

import com.ibm.ws.jsf.extprocessor.JSFExtensionFactory;

/**
 * WAS custom application factory that initializes CDI per application
 */
public class WASApplicationFactoryImpl extends ApplicationFactoryImpl {
    
    private volatile boolean initialized = false;

    @Override
    public Application getApplication() {
        Application app = super.getApplication();
        if (!initialized) {
            synchronized (this) {
                if (!initialized) {
                    JSFExtensionFactory.initializeCDI(app);
                    initialized = true;
                }
            }
        }
        return app;
    }

}
