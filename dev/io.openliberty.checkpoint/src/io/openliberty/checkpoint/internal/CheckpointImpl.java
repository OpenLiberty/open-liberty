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

import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;
import com.ibm.wsspi.kernel.service.location.WsLocationConstants;

import io.openliberty.checkpoint.internal.criu.ExecuteCRIU;
import io.openliberty.checkpoint.spi.Checkpoint;
import io.openliberty.checkpoint.spi.SnapshotFailed;
import io.openliberty.checkpoint.spi.SnapshotFailed.Type;
import io.openliberty.checkpoint.spi.SnapshotHook;
import io.openliberty.checkpoint.spi.SnapshotHookFactory;

@Component(
           reference = @Reference(name = "hookFactories", service = SnapshotHookFactory.class, cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC,
                                  policyOption = ReferencePolicyOption.GREEDY),
           property = { "osgi.command.function=snapshot", "osgi.command.scope=criu" })
public class CheckpointImpl implements Checkpoint {
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
     * Before the snapshot is taken the registered {@link SnapshotHookFactory#create(Phase)}
     * methods are called to obtain the {@link SnapshotHook} instances which will participate
     * in the prepare and restore steps for the snapshot process.
     * Each snapshot hook instance will their {@link SnapshotHook#prepare(Phase)}
     * methods are called before the snapshot is taken. After the snapshot
     * is taken each snapshot hook instance will have their {@link SnapshotHook#restore(Phase)}
     * methods called.
     *
     * @param phase the phase to take the snapshot
     * @throws SnapshotFailed if the snapshot fails
     */
    @Descriptor("Take a snapshot")
    public void snapshot(@Parameter(names = "-p", absentValue = "") @Descriptor("The phase to snapshot") Phase phase,
                         @Descriptor("Directory to store the snapshot") File directory) throws SnapshotFailed {
        doSnapshot(phase, directory);
    }

    @Override
    @Descriptor("Take a snapshot")
    public void snapshot(Phase phase) throws SnapshotFailed {
        doSnapshot(phase,
                   locAdmin.resolveResource(WsLocationConstants.SYMBOL_SERVER_WORKAREA_DIR + "snapshot").asFile());
    }

    private void doSnapshot(Phase phase, File directory) throws SnapshotFailed {
        directory.mkdirs();
        Object[] factories = cc.locateServices("hookFactories");
        List<SnapshotHook> snapshotHooks = getHooks(factories, phase);
        prepare(snapshotHooks);
        try {
            ExecuteCRIU currentCriu = criu;
            if (currentCriu == null) {
                throw new SnapshotFailed(Type.SNAPSHOT_FAILED, "The criu command is not available.", null);
            } else {
                currentCriu.dump(directory);
            }
        } catch (Exception e) {
            abortPrepare(snapshotHooks, e);
            throw new SnapshotFailed(Type.SNAPSHOT_FAILED, "Failed to create snapshot.", e);
        }
        restore(phase, snapshotHooks);
    }

    List<SnapshotHook> getHooks(Object[] factories, Phase phase) {
        if (factories == null) {
            return Collections.emptyList();
        }
        List<SnapshotHook> hooks = new ArrayList<>(factories.length);
        for (Object o : factories) {
            // if o is anything other than a SnapshotHookFactory then
            // there is a bug in SCR
            SnapshotHook hook = ((SnapshotHookFactory) o).create(phase);
            if (hook != null) {
                hooks.add(hook);
            }
        }
        return hooks;
    }

    private void callHooks(List<SnapshotHook> snapshotHooks,
                           Consumer<SnapshotHook> perform,
                           BiConsumer<SnapshotHook, Exception> abort,
                           Function<Exception, SnapshotFailed> failed) throws SnapshotFailed {
        List<SnapshotHook> called = new ArrayList<>(snapshotHooks.size());
        for (SnapshotHook snapshotHook : snapshotHooks) {
            try {
                perform.accept(snapshotHook);
                called.add(snapshotHook);
            } catch (Exception abortCause) {
                for (SnapshotHook abortHook : called) {
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

    private void prepare(List<SnapshotHook> snapshotHooks) throws SnapshotFailed {
        callHooks(snapshotHooks,
                  SnapshotHook::prepare,
                  SnapshotHook::abortPrepare,
                  CheckpointImpl::failedPrepare);
    }

    private static SnapshotFailed failedPrepare(Exception cause) {
        return new SnapshotFailed(Type.PREPARE_ABORT, "Failed to prepare for a snapshot.", cause);
    }

    private void abortPrepare(List<SnapshotHook> snapshotHooks, Exception cause) {
        for (SnapshotHook hook : snapshotHooks) {
            try {
                hook.abortPrepare(cause);
            } catch (Exception e) {
                // ignore
            }
        }
    }

    private void restore(Phase phase, List<SnapshotHook> snapshotHooks) throws SnapshotFailed {
        callHooks(snapshotHooks,
                  SnapshotHook::restore,
                  SnapshotHook::abortRestore,
                  CheckpointImpl::failedRestore);
    }

    private static SnapshotFailed failedRestore(Exception cause) {
        return new SnapshotFailed(Type.RESTORE_ABORT, "Failed to restore from snapshot.", cause);
    }
}
