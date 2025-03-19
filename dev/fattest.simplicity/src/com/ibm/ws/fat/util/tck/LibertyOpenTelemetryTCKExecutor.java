package com.ibm.ws.fat.util.tck;

import java.util.concurrent.Executor;
import java.util.logging.Logger;

import javax.naming.InitialContext;
import javax.naming.NamingException;
//Use jakarta because this code goes in applications and will not be transformed.
import jakarta.enterprise.concurrent.ManagedExecutorService;

import com.ibm.websphere.simplicity.log.Log;

public class LibertyOpenTelemetryTCKExecutor implements Executor {

    private static final Logger LOG = Logger.getLogger(LibertyOpenTelemetryTCKExecutor.class.getName());

    @Override
    public void execute(Runnable command) {
        try {
            ManagedExecutorService mes = (ManagedExecutorService) InitialContext.doLookup("java:comp/DefaultManagedExecutorService");
            mes.execute(command);
        } catch (NamingException e) {
            Log.error(LibertyOpenTelemetryTCKExecutor.class, "execute", e);
            throw new RuntimeException(e);
        }

    }

}
