package com.ibm.ws.jndi.iiop;

import java.util.concurrent.atomic.AtomicBoolean;

import javax.naming.OperationNotSupportedException;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.jndi.WSName;
import com.ibm.ws.jndi.internal.Messages;

import io.openliberty.checkpoint.spi.CheckpointHook;
import io.openliberty.checkpoint.spi.CheckpointPhase;

public class CheckpointOperationNotSupportedException extends OperationNotSupportedException {

    private static final TraceComponent tc = Tr.register(CheckpointOperationNotSupportedException.class);

    private CheckpointOperationNotSupportedException(String explanation) {
        super();
    }

    public CheckpointOperationNotSupportedException(String contextName, String appName) {
        super(getMessage("jndi.orb.context.failed.checkpoint", contextName.toString(), appName));
        if (!CheckpointPhase.getPhase().restored()) {
            CheckpointHookForOrbContext.add(contextName, appName);
        }
    }

    @Trivial
    public static final String getMessage(String key, Object... params) {
        return TraceNLS.getFormattedMessage(CheckpointOperationNotSupportedException.class, tc.getResourceBundleName(), key, params, key);
    }

    /**
     * Fail checkpoint when attempting to access the ORB context.
     */
    private static class CheckpointHookForOrbContext implements CheckpointHook {

        @Override
        public void prepare() {
            throw new IllegalStateException(getMessage("jndi.orb.context.failed.checkpoint", contextName.toString(), appName));
        }

        private final String contextName;
        private final String appName;

        private CheckpointHookForOrbContext(String contextName, String appName) {
            this.contextName = contextName;
            this.appName = appName;
        }

        private static final AtomicBoolean alreadyAdded = new AtomicBoolean(false);

        private static void add(String contextName, String appName) {
            if (alreadyAdded.compareAndSet(false, true)) {
                CheckpointPhase.getPhase().addMultiThreadedHook(new CheckpointHookForOrbContext(contextName, appName));
            }
        }
    }
}
