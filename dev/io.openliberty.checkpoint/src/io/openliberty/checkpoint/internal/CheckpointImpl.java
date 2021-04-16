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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import org.apache.felix.service.command.Descriptor;
import org.checkpoint.CheckpointRestore;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;

import io.openliberty.checkpoint.spi.Checkpoint;
import io.openliberty.checkpoint.spi.SnapshotFailed;
import io.openliberty.checkpoint.spi.SnapshotFailed.Type;
import io.openliberty.checkpoint.spi.SnapshotHook;

public class CheckpointImpl implements Checkpoint{
	private volatile ServiceTracker<SnapshotHook, SnapshotHook> hooks;
	private volatile ServiceRegistration<Checkpoint> reg;

    void register(BundleContext context)
    {
    	hooks = new ServiceTracker<>(context, SnapshotHook.class, null);
    	hooks.open();
        Hashtable<String, Object> props = new Hashtable<>();
        props.put("osgi.command.function", new String[] {"snapshot"});
        props.put("osgi.command.scope", "criu");
        reg = context.registerService(Checkpoint.class, this, props);
    }

    void unregister() {
    	reg.unregister();
    	hooks.close();
    }

	@Override
    @Descriptor("Take a snapshot")
	public void snapshot(@Descriptor("The phase to snapshot") Phase phase, @Descriptor("Name of snapshot") String name) throws SnapshotFailed {
		String directory = determineDirectory(name);
		doSnapshot(phase, directory);
	}

	private String determineDirectory(String name) {
		// TODO need to be more smart than use the current working directory
		File dir = new File(name);
		dir.mkdir();
		return dir.getAbsolutePath();
	}

	private void doSnapshot(Phase phase, String directory) throws SnapshotFailed {
		SnapshotHook[] snapshotHooks = hooks.getServices(new SnapshotHook[0]);
		prepare(phase, snapshotHooks);
		System.out.println("Go save the world");
		try {
			CheckpointRestore.saveTheWorld(directory);
		} catch (Exception e) {
			throw new SnapshotFailed(Type.SNAPSHOT_FAILED, "Failed to create snapshot.", e);
		} finally {
			restore(phase, snapshotHooks);
		}
		System.out.println("Saved the world");

	}

	private void prepare(Phase phase, SnapshotHook[] snapshotHooks) throws SnapshotFailed {
		List<SnapshotHook> called = new ArrayList<>(snapshotHooks.length);
		for (SnapshotHook snapshotHook : snapshotHooks) {
			try {
				snapshotHook.prepare(phase);
				called.add(snapshotHook);
			} catch (Exception abortCause) {
				for (SnapshotHook abortHook : called) {
					try {
						abortHook.abortPrepare(phase, abortCause);
					} catch (Exception unexpected) {
						// TODO should log
					}
				}
				throw new SnapshotFailed(Type.PREPARE_ABORT, "Failed to prepare for snapshot.", abortCause);
			}
		}
	}

	private void restore(Phase phase, SnapshotHook[] snapshotHooks) throws SnapshotFailed {
		List<SnapshotHook> called = new ArrayList<>(snapshotHooks.length);
		for (SnapshotHook snapshotHook : snapshotHooks) {
			try {
				snapshotHook.restore(phase);
				called.add(snapshotHook);
			} catch (Exception abortCause) {
				for (SnapshotHook abortHook : called) {
					try {
						abortHook.abortRestore(phase, abortCause);
					} catch (Exception unexpected) {
						// TODO should log
					}
				}
				throw new SnapshotFailed(Type.RESTORE_ABORT, "Failed to restore from snapshot.", abortCause);
			}
		}
	}
}
