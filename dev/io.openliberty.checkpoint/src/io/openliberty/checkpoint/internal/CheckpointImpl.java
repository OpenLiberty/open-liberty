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
import io.openliberty.checkpoint.spi.Checkpoint;
import io.openliberty.checkpoint.spi.SnapshotHook;
import io.openliberty.checkpoint.spi.SnapshotHookFactory;
import io.openliberty.checkpoint.spi.SnapshotResult;
import io.openliberty.checkpoint.spi.SnapshotResult.SnapshotResultType;

@Component(
           reference = @Reference(name = "hookFactories", service = SnapshotHookFactory.class, cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC,
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
     * Before the snapshot is taken the registered {@link SnapshotHookFactory#create(Phase)}
     * methods are called to obtain the {@link SnapshotHook} instances which will participate
     * in the prepare and restore steps for the snapshot process.
     * Each snapshot hook instance will their {@link SnapshotHook#prepare(Phase)}
     * methods are called before the snapshot is taken. After the snapshot
     * is taken each snapshot hook instance will have their {@link SnapshotHook#restore(Phase)}
     * methods called.
     *
     * @deprecated Provided to support debugging from osgi console.
     * @param phase the phase to take the snapshot
     * @returns SnapshotResult if the snapshot fails
     */
    @Deprecated
    @Descriptor("Take a snapshot")
    public SnapshotResult snapshot(@Parameter(names = "-p", absentValue = "") @Descriptor("The phase to snapshot") Phase phase,
                                   @Descriptor("Directory to store the snapshot") File directory) {
        SnapshotResult result = doSnapshot(phase, directory);
        return result;
    }

    @Override
    @Descriptor("Take a snapshot")
    public SnapshotResult snapshot(Phase phase) {
        SnapshotResult result = doSnapshot(phase,
                                           locAdmin.resolveResource(WsLocationConstants.SYMBOL_SERVER_WORKAREA_DIR + "checkpoint/image/").asFile());
        return result;
    }

    private SnapshotResult doSnapshot(Phase phase, File directory) {
        directory.mkdirs();
        Object[] factories = cc.locateServices("hookFactories");
        List<SnapshotHook> snapshotHooks = getHooks(factories, phase);
        SnapshotResult prepareResult = prepare(snapshotHooks);

        if (prepareResult.getType() != SnapshotResultType.SUCCESS) {
            return prepareResult;
        }

        ExecuteCRIU currentCriu = criu;
        SnapshotResult dumpResult = null;

        if (currentCriu == null) {
            return new SnapshotResult(SnapshotResultType.SNAPSHOT_FAILED, "The criu command is not available.", null);
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "criu attempt dump to '" + directory + "' and exit process.");
            }

            dumpResult = currentCriu.dump(directory);

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "criu dump to " + directory + " in recovery.");
            }

            if (dumpResult.getType() == SnapshotResultType.SUCCESS) {
                SnapshotResult restoreResult = restore(phase, snapshotHooks);
                if (restoreResult.getType() != SnapshotResultType.SUCCESS) {
                    return restoreResult;
                }
            } else if (dumpResult.getType() == SnapshotResultType.JVM_RESTORE_FAILURE) {
                return new SnapshotResult(SnapshotResultType.JVM_RESTORE_FAILURE, "Unexpected JVM failure.", null);
            } else {
                abortPrepare(snapshotHooks, dumpResult);
            }
        }

        if (dumpResult == null) {
            return new SnapshotResult(SnapshotResultType.SNAPSHOT_FAILED, "Unexpected failure.", null);
        }
        return dumpResult;
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

    private SnapshotResult callHooks(List<SnapshotHook> snapshotHooks,
                                     Consumer<SnapshotHook> perform,
                                     BiConsumer<SnapshotHook, SnapshotResult> abort,
                                     Function<Exception, SnapshotResult> snapshotResult) {
        List<SnapshotHook> called = new ArrayList<>(snapshotHooks.size());
        SnapshotResult result = new SnapshotResult(SnapshotResultType.SUCCESS, "Prepare Success", null);
        for (SnapshotHook snapshotHook : snapshotHooks) {
            try {
                perform.accept(snapshotHook);
                called.add(snapshotHook);
            } catch (Exception abortCause) {
                for (SnapshotHook abortHook : called) {
                    try {
                        result = snapshotResult.apply(abortCause);
                        abort.accept(abortHook, result);
                    } catch (Exception unexpected) {
                        // auto FFDC is fine here
                    }
                }
                return result;
            }
        }
        return result;
    }

    private SnapshotResult prepare(List<SnapshotHook> snapshotHooks) {
        return callHooks(snapshotHooks,
                         SnapshotHook::prepare,
                         SnapshotHook::abortPrepare,
                         CheckpointImpl::failedPrepare);
    }

    private static SnapshotResult failedPrepare(Exception cause) {
        return new SnapshotResult(SnapshotResultType.PREPARE_ABORT, "Failed to prepare for a snapshot.", cause);
    }

    private void abortPrepare(List<SnapshotHook> snapshotHooks, SnapshotResult snapshotResult) {
        for (SnapshotHook hook : snapshotHooks) {
            hook.abortPrepare(snapshotResult);
        }
    }

    private SnapshotResult restore(Phase phase, List<SnapshotHook> snapshotHooks) {
        return callHooks(snapshotHooks,
                         SnapshotHook::restore,
                         SnapshotHook::abortRestore,
                         CheckpointImpl::failedRestore);
    }

    private static SnapshotResult failedRestore(Exception cause) {
        return new SnapshotResult(SnapshotResultType.RESTORE_ABORT, "Failed to restore from snapshot.", cause);
    }
}
