/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.cdi.liberty;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.cdi.CDIException;
import com.ibm.ws.cdi.internal.interfaces.Application;
import com.ibm.ws.cdi.internal.interfaces.CDIArchive;
import com.ibm.ws.cdi.internal.interfaces.CDIUtils;
import com.ibm.ws.cdi.internal.interfaces.ContextBeginnerEnder;
import com.ibm.ws.runtime.metadata.MetaData;
import com.ibm.ws.runtime.metadata.ModuleMetaData;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;

public class ContextBeginnerEnderImpl implements ContextBeginnerEnder {

    private static final TraceComponent tc = Tr.register(ContextBeginnerEnderImpl.class);

    private static ThreadLocal<ContextBeginnerEnderImpl> currentlyActive = new ThreadLocal<ContextBeginnerEnderImpl>();

    private ClassLoader newTheadContextClassLoader = null;
    private ClassLoader oldTheadContextClassLoader = null;
    private JndiHelperComponentMetaData cmd = null;
    private boolean began = false;
    private String cmdLogStringSuffix = "";
    private String tcclLogStringSuffix = "";

    public ContextBeginnerEnderImpl() {

    }

    private ContextBeginnerEnderImpl(ClassLoader newTheadContextClassLoader, JndiHelperComponentMetaData cmd,
                                     String cmdLogStringSuffix, String tcclLogStringSuffix) {
        this.newTheadContextClassLoader = newTheadContextClassLoader;
        this.cmd = cmd;
        this.cmdLogStringSuffix = cmdLogStringSuffix;
        this.tcclLogStringSuffix = tcclLogStringSuffix;
    }

    @Override
    public ContextBeginnerEnder extractTCCL(Application application) {
        tcclLogStringSuffix = application.getName();
        newTheadContextClassLoader = application.getTCCL();
        return this;
    }

    //Extracting from the application should only be done if you cannot start and stop contexts for individual moduals
    //See the comment about JNDI below for an example
    @Override
    public ContextBeginnerEnder extractComponentMetaData(Application application) throws CDIException {

        //You will notice that this gets its metadata from an arbitary module rather than
        //getting application metadata. This behaviour goes back to when I was trying to
        //get JNDI lookups to work inside a CDI observer. I discovered that using application
        //metadata did not work. But using an arbitary module metadata was sufficent for the
        //java:app namespace but not for the java module namespace.

        //And because weld fires observers in all modules when endInitialization() is called
        //using more than one module's metadata was not and option.
        if (application.getModuleArchives().size() > 0 &&
            application.getApplicationMetaData() != null) {
            CDIArchive archive = application.getModuleArchives().iterator().next();
            extractComponentMetaData(archive);
            cmdLogStringSuffix = cmdLogStringSuffix + " as a stand in for the whole application"; //append to the string set in extractComponentMetaData(CDIArchive archive)
        }
        return this;
    }

    @Override
    public ContextBeginnerEnder extractComponentMetaData(CDIArchive archive) throws CDIException {
        if (archive.getApplication() == null) {
            return this; //This will happen for runtime extensions
        }

        CDIArchive moduleArchive = null;
        if (archive.isModule()) {
            moduleArchive = archive;
        } else {
            moduleArchive = archive.getApplication().getModuleArchives().iterator().next();
        }

        MetaData metaData = moduleArchive.getMetaData();
        ModuleMetaData moduleMetaData = (ModuleMetaData) metaData;
        cmd = new JndiHelperComponentMetaData(moduleMetaData);

        cmdLogStringSuffix = archive.equals(moduleArchive) ? moduleArchive.getName() : moduleArchive.getName() + " as a stand in for " + archive.getName();

        return this;
    }

    @Override
    public ContextBeginnerEnder beginContext() {
        if (began) {
            throw new IllegalStateException("beginContext cannot be called twice");
        }
        if (currentlyActive.get() != null) {
            throw new IllegalStateException("Annother ContextBeginnerEnder is already active");
        }

        began = true;
        currentlyActive.set(this);

        if (cmd != null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "beginContext", "setting component medata data using archive: " + cmdLogStringSuffix);
            }
            ComponentMetaDataAccessorImpl accessor = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor();
            accessor.beginContext(cmd);
        }

        if (newTheadContextClassLoader != null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "beginContext", "setting tccl with classloader for app: " + tcclLogStringSuffix);
            }
            oldTheadContextClassLoader = CDIUtils.getAndSetLoader(newTheadContextClassLoader);
        }

        return this;
    }

    @Override
    public void close() {

        if (!began) {
            throw new IllegalStateException("close invoked without beginContext");
        }
        currentlyActive.set(null);

        if (cmd != null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "beginContext", "unsetting component medata data for archive: " + cmdLogStringSuffix);
            }
            ComponentMetaDataAccessorImpl accessor = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor();
            accessor.endContext();
            cmd = null;
        }

        if (oldTheadContextClassLoader != null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "beginContext", "unsetting tccl classloader for app: " + tcclLogStringSuffix);
            }
            CDIUtils.getAndSetLoader(oldTheadContextClassLoader);
            oldTheadContextClassLoader = null;
        }
    }

    @Override
    public ContextBeginnerEnder clone() {
        return new ContextBeginnerEnderImpl(newTheadContextClassLoader, cmd, cmdLogStringSuffix, tcclLogStringSuffix);
    }

    public static ContextBeginnerEnderImpl getCurrentlyActive() {
        return currentlyActive.get();
    }

    public static boolean isActive() {
        return currentlyActive.get() != null;
    }

}
