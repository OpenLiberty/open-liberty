/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.checkpoint.internal;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import org.apache.felix.service.command.Descriptor;
import org.apache.felix.service.command.Parameter;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;
import com.ibm.wsspi.kernel.service.location.WsLocationConstants;

import io.openliberty.checkpoint.internal.criu.ExecuteCRIU;
import io.openliberty.checkpoint.internal.Checkpoint;
import io.openliberty.checkpoint.spi.CheckpointHook;
import io.openliberty.checkpoint.spi.CheckpointHookFactory;
import io.openliberty.checkpoint.spi.CheckpointHookFactory.Phase;
import io.openliberty.checkpoint.spi.SnapshotFailed;
import io.openliberty.checkpoint.spi.SnapshotFailed.Type;

@Component(
           reference = @Reference(name = "hookFactories", service = CheckpointHookFactory.class, cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC,
                                  policyOption = ReferencePolicyOption.GREEDY),
           property = { "osgi.command.function=snapshot", "osgi.command.scope=criu" })
public class CheckpointImpl implements Checkpoint {

    private static final TraceComponent tc = Tr.register(CheckpointImpl.class);
    private final ComponentContext cc;

    @Reference
    WsLocationAdmin locAdmin;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policyOption = ReferencePolicyOption.GREEDY)
    private volatile ExecuteCRIU criu;

    @Activate
    public CheckpointImpl(ComponentContext cc) {
        this.cc = cc;
    }

    // only for unit tests
    CheckpointImpl(ComponentContext cc, ExecuteCRIU criu) {
        this.cc = cc;
        this.criu = criu;
    }

    /**
     * Perform a snapshot for the specified phase. The result of the
     * snapshot will be stored in the specified directory.
     * Before the snapshot is taken the registered {@link CheckpointHookFactory#create(Phase)}
     * methods are called to obtain the {@link CheckpointHook} instances which will participate
     * in the prepare and restore steps for the snapshot process.
     * Each snapshot hook instance will their {@link CheckpointHook#prepare(Phase)}
     * methods are called before the snapshot is taken. After the snapshot
     * is taken each snapshot hook instance will have their {@link CheckpointHook#restore(Phase)}
     * methods called.
     *
     * @deprecated Provided to support debugging from osgi console.
     * @param phase the phase to take the snapshot
     * @throws SnapshotFailed if the snapshot fails
     */
    @Deprecated
    @Descriptor("Take a snapshot")
    public void snapshot(@Parameter(names = "-p", absentValue = "") @Descriptor("The phase to snapshot") Phase phase,
                         @Descriptor("Directory to store the snapshot") File directory) throws SnapshotFailed {
        doSnapshot(phase, directory);
    }

    @Override
    @Descriptor("Take a snapshot")
    public void snapshot(Phase phase) throws SnapshotFailed {
        doSnapshot(phase,
                   locAdmin.resolveResource(WsLocationConstants.SYMBOL_SERVER_WORKAREA_DIR + "checkpoint/image/").asFile());
    }

    private void doSnapshot(Phase phase, File directory) throws SnapshotFailed {
        directory.mkdirs();
        Object[] factories = cc.locateServices("hookFactories");
        List<CheckpointHook> checkpointHooks = getHooks(factories, phase);
        prepare(checkpointHooks);
        try {
            ExecuteCRIU currentCriu = criu;
            if (currentCriu == null) {
                throw new SnapshotFailed(Type.SNAPSHOT_FAILED, "The criu command is not available.", null, 0);
            } else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "criu attempt dump to '" + directory + "' and exit process.");
                }

                int dumpCode = currentCriu.dump(directory);
                if (dumpCode < 0) {
                    throw new SnapshotFailed(Type.SNAPSHOT_FAILED, "The criu dump command failed with error: " + dumpCode, null, dumpCode);
                }

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "criu dump to " + directory + " in recovery.");
                }
            }
        } catch (Exception e) {
            abortPrepare(checkpointHooks, e);
            if (e instanceof SnapshotFailed) {
                throw (SnapshotFailed) e;
            }
            throw new SnapshotFailed(Type.SNAPSHOT_FAILED, "Failed to create snapshot.", e, 0);
        }
        restore(phase, checkpointHooks);
    }

    List<CheckpointHook> getHooks(Object[] factories, Phase phase) {
        if (factories == null) {
            return Collections.emptyList();
        }
        List<CheckpointHook> hooks = new ArrayList<>(factories.length);
        for (Object o : factories) {
            // if o is anything other than a CheckpointHookFactory then
            // there is a bug in SCR
            CheckpointHook hook = ((CheckpointHookFactory) o).create(phase);
            if (hook != null) {
                hooks.add(hook);
            }
        }
        return hooks;
    }

    private void callHooks(List<CheckpointHook> checkpointHooks,
                           Consumer<CheckpointHook> perform,
                           BiConsumer<CheckpointHook, Exception> abort,
                           Function<Exception, SnapshotFailed> failed) throws SnapshotFailed {
        List<CheckpointHook> called = new ArrayList<>(checkpointHooks.size());
        for (CheckpointHook checkpointHook : checkpointHooks) {
            try {
                perform.accept(checkpointHook);
                called.add(checkpointHook);
            } catch (Exception abortCause) {
                for (CheckpointHook abortHook : called) {
                    try {
                        abort.accept(abortHook, abortCause);
                    } catch (Exception unexpected) {
                        // auto FFDC is fine here
                    }
                }
                throw failed.apply(abortCause);
            }
        }
    }

    private void prepare(List<CheckpointHook> checkpointHooks) throws SnapshotFailed {
        callHooks(checkpointHooks,
                  CheckpointHook::prepare,
                  CheckpointHook::abortPrepare,
                  CheckpointImpl::failedPrepare);
    }

    private static SnapshotFailed failedPrepare(Exception cause) {
        return new SnapshotFailed(Type.PREPARE_ABORT, "Failed to prepare for a snapshot.", cause, 0);
    }

    private void abortPrepare(List<CheckpointHook> checkpointHooks, Exception cause) {
        for (CheckpointHook hook : checkpointHooks) {
            try {
                hook.abortPrepare(cause);
            } catch (Exception e) {
                // ignore
            }
        }
    }

    private void restore(Phase phase, List<CheckpointHook> checkpointHooks) throws SnapshotFailed {
        callHooks(checkpointHooks,
                  CheckpointHook::restore,
                  CheckpointHook::abortRestore,
                  CheckpointImpl::failedRestore);
    }

    private static SnapshotFailed failedRestore(Exception cause) {
        return new SnapshotFailed(Type.RESTORE_ABORT, "Failed to restore from snapshot.", cause, 0);
    }
}
