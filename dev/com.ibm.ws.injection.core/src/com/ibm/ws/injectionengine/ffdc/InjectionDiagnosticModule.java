/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.injectionengine.ffdc;

import java.util.ArrayList;
import java.util.List;

import com.ibm.ejs.util.Util;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.DiagnosticModule;
import com.ibm.ws.ffdc.FFDC;
import com.ibm.ws.ffdc.IncidentStream;
import com.ibm.ws.injectionengine.AbstractInjectionEngine;
import com.ibm.wsspi.injectionengine.InjectionConfigConstants;

/**
 * Provides First Failure Data Capture (FFDC) function for the Injection
 * service in the form of a Diagnostic Module (DM). DMs are registered with
 * the FFDC service and called when exceptions occur in the packages for
 * which they are registered. The InjectionDiagnosticModule provides
 * coverage for injection code across all directories (injection,
 * injection.impl, injection.shared, was.injection.impl) by registering
 * for the following packages and classes: <p>
 *
 * <DL>
 * <DD> com.ibm.ws.injectionengine (PACKAGE)
 * <DD> com.ibm.wsspi.injectionengine (PACKAGE)
 * </DL> <p>
 *
 * Package registrations ensure that all classes under the package are covered.
 * Class registrations are required where the package the class resides in is
 * not owned by the injection service. In such cases if the package was
 * registered, all classes including those not owned by the injection
 * service would be covered - which is not what is needed. <p>
 *
 * The parent class, com.ibm.ws.ffdc.DiagnosticModule, directs this class to
 * write diagnostic information when exceptions occur. Rather than having a
 * fixed set of methods the parent can call, the parent class reflects over
 * this class when it registers and stores references to methods with names
 * that start 'ffdcDump' and 'ffdcDumpDefault'. It is these methods that are
 * called according to the following rules:- <p>
 *
 * 1. Methods starting with 'ffdcDumpDefault' will always be called when an
 * exception occurs in a package or class for which this diagnostic module
 * is registered. These methods capture the default information for the
 * Injection service. All components must provide at least one default
 * FFDC dump method. <p>
 *
 * 2. Methods starting with 'ffdcDump' (other then Default) are only called
 * under conditions. During the initial phases of the FFDC exception
 * processing cycle and before this class has been called, a component
 * called the Log Analysis Engine (LAE) is invoked to examine the
 * exception call stack. Based on a series of rules stored in a 'knowledge
 * base' (a number of xml files) the problem may be matched with a set of
 * directives. A directive is an English string such as 'StateTable'.
 * 'ffdcDump' methods have post-fixes that are actually the directive names
 * such as 'ffdcDumpStateTable'. If a set of directives is identified by
 * the LAE, the corresponding methods will be called in this class.
 * Provision of directives and corresponding 'ffdcDump<directive>' methods
 * is optional. <p>
 **/
public class InjectionDiagnosticModule
                extends DiagnosticModule
{
    private static final TraceComponent tc = Tr.register
                    (InjectionDiagnosticModule.class,
                     InjectionConfigConstants.traceString,
                     InjectionConfigConstants.messageFile);

    /** The singleton instance of this class. **/
    private static InjectionDiagnosticModule svInstance = null;

    /** True if registration with FFDC Service attempted. **/
    private boolean ivRegisteredWithFFDC = false;

    /** Injection service. **/
    private AbstractInjectionEngine ivInjectionEngine = null;

    /**
     * The list of package and class names for which this FFDC DM will be registered.
     **/
    private String[] ivPackageList = new String[] {
                                                   "com.ibm.ws.injectionengine",
                                                   "com.ibm.wsspi.injectionengine",
    };

    /**
     * Default constructor for the InjectionDiagnosticModule class. We don't
     * need to do any work here. This method is simply made private to ensure
     * that the class is a singleton.
     **/
    private InjectionDiagnosticModule()
    {
        // intentionally left empty.
    }

    /**
     * Static method to return single instance.
     *
     * @return The single instance of this class.
     **/
    public static synchronized InjectionDiagnosticModule instance()
    {
        if (svInstance == null)
        {
            svInstance = new InjectionDiagnosticModule();
        }

        return svInstance;
    }

    /**
     * Initializes the Injection Diagnostic Module so that it is ready
     * to begin handling FFDC requests for the specified Injection
     * Service.
     *
     * @param service Injection Service for which this DiagnosticModule
     *            will provide FFDC diagnostics.
     **/
    public synchronized void initialize(AbstractInjectionEngine service)
    {
        ivInjectionEngine = service;
    }

    /**
     * Register the InjectionDiagnosticModule with the FFDC service. <p>
     *
     * If a package can't be registered because its already registered then
     * press on with the registration of further packages to ensure maximum
     * FFDC coverage. If a package can't be registered for some other reason,
     * abort the registration process.
     *
     * @return true if registration was completed successfully for all packages,
     *         otherwise false.
     **/
    public synchronized boolean registerWithFFDCService()
    {
        boolean result = true;
        boolean abort = false;
        int retCode = 0;
        int packageIndex = 0;

        // Don't register more than once.
        if (ivRegisteredWithFFDC)
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                Tr.event(tc, "InjectionDiagnosticModule already registered");
            result = false;
            abort = true;
        }

        // Don't register with FFDC Service if not initialized properly.
        if (ivInjectionEngine == null)
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                Tr.event(tc, "InjectionDiagnosticModule not initialized: " +
                             "registration with FFDC Service not performed");
            result = false;
            abort = true;
        }

        // Register this class with the FFDC service under the various
        // package/class names required.
        while ((abort == false) && (packageIndex < ivPackageList.length))
        {
            retCode = FFDC.registerDiagnosticModule(this,
                                                    ivPackageList[packageIndex]);

            switch (retCode)
            {
                case 0:
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                        Tr.event(tc, "InjectionDiagnosticModule successfully " +
                                     "registered for package " + ivPackageList[packageIndex]);
                    break;

                case 1:
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                        Tr.event(tc, "Unable to register InjectionDiagnosticModule " +
                                     "as another diagnostic module has already been " +
                                     "registered with the package " +
                                     ivPackageList[packageIndex]);
                    result = false;
                    break;

                case 2:
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                        Tr.event(tc, "Unable to register InjectionDiagnosticModule " +
                                     "as it does not support the minimum diagnostic module " +
                                     "interface.");
                    result = false;
                    abort = true;
                    break;

                case 3:
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                        Tr.event(tc, "Unable to register InjectionDiagnosticModule " +
                                     "due to an unknown failure.");
                    result = false;
                    abort = true;
                    break;

                default:
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                        Tr.event(tc, "InjectionDiagnosticModule registration with " +
                                     ivPackageList[packageIndex] + " resulted in an " +
                                     "unexpected return code: " + retCode);
                    result = false;
                    abort = true;
                    break;
            }

            packageIndex++;
        }

        return result;
    }

    //============================================================================
    // Default FFDC data capture methods for the Injection Service
    //============================================================================

    /**
     * Required FFDC default dump method. All Diagnostic Modules must have at
     * least one default method. <p>
     *
     * By default, the FFDC service will always dump the following:
     *
     * <DL>
     * <DD> Exception Name
     * <DD> Source ID
     * <DD> Probe ID
     * <DD> Stack Dump
     * <DD> Dump of callerThis
     * </DL> <p>
     *
     * This method will dump any other miscellaneous information for the
     * EJB Container Service that would be useful for all exceptions. <p>
     *
     * Larger data structures should be dumped in more specific default
     * dump methods. For example, the ComponentMetaData should be dumped in
     * a default method like ffdcDumpDefaultComponentMetaData. <p>
     *
     * Information that is only useful in certain scenarios should be dumped
     * in directive driven methods (ffdcDump<directive>). <p>
     *
     * @param th The exception which triggered the FFDC capture process.
     * @param is The IncidentStream. Data to be captured is written to
     *            this stream.
     * @param callerThis The 'this' pointer for the object which invoked the filter.
     *            The value will be null if the method which invoked the
     *            filter was static, or if the method which invoked the
     *            filter does not correspond to the DM being invoked.
     * @param o The value of the array may be null. If not null, it
     *            contains an array of objects which the caller to the
     *            filter provided. Since the information in the array may
     *            vary depending upon the location in the code, the first
     *            index of the array may contain hints as to the content
     *            of the rest of the array.
     * @param sourceId The sourceId passed to the filter.
     **/
    public void ffdcDumpDefault(Throwable th, IncidentStream is,
                                Object callerThis, Object[] o,
                                String sourceId)
    {
        // Currently, DiagnosticEngine will already dump:
        //     Exception Name
        //     Source ID
        //     Probe ID
        //     Stack Dump
        //     Dump of callerThis

        is.writeLine("", "*** Start InjectionDiagnosticModule Dump ***");

        // Dump the object array.
        if (o != null)
        {
            is.writeLine("", "> InjectionDiagnosticModule: dump Object array : " + o.length);

            List<Object> nonFormattable = new ArrayList<Object>();
            for (Object obj : o)
            {
                if (obj == this.ivInjectionEngine)
                {
                    // skipping, will be handled below
                }
                else if (obj instanceof Formattable)
                {
                    // Nicely format any known objects
                    ((Formattable) obj).formatTo(is);
                }
                else
                {
                    // add to list to be output below
                    nonFormattable.add(obj);
                }
            }

            if (nonFormattable.size() > 0)
            {
                is.writeLine("", "");
                is.writeLine("", "   Non-Formattable :");
                for (Object obj : nonFormattable)
                {
                    is.writeLine("", "      > " + Util.identity(obj));
                    is.writeLine("", "           " + obj);
                }
            }
            is.writeLine("", "< InjectionDiagnosticModule: dump Object array complete");
        }

        ivInjectionEngine.formatTo(is);

        is.writeLine("", "*** InjectionDiagnosticModule Dump Complete ***");
    }

    //===========================================================================
    // Directive Driven FFDC data capture methods for the Injection Service
    //===========================================================================

    // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    // Directive based data capture is not currently used by the EJB Container
    // service FFDC support. It is provided here for future expansion.
    // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

    /*
     * FFDC data capture method to handle directive <Directive>.
     * This method will be called during FFDC exception processing in the event
     * that the LAE analysis resulted in directive <Directive>. <p>
     *
     *
     * @param th The exception which triggered the FFDC capture process.
     *
     * @param is The IncidentStream. Data to be captured is written to
     * this stream.
     *
     * @param callerThis The 'this' pointer for the object which invoked the filter.
     * The value will be null if the method which invoked the
     * filter was static, or if the method which invoked the
     * filter does not correspond to the DM being invoked.
     *
     * @param o The value of the array may be null. If not null, it
     * contains an array of objects which the caller to the
     * filter provided. Since the information in the array may
     * vary depending upon the location in the code, the first
     * index of the array may contain hints as to the content
     * of the rest of the array.
     *
     * @param sourceId The sourceId passed to the filter.
     */
    /*
     * public void ffdcDump<Directive>(Throwable th, IncidentStream is,
     * Object callerThis, Object[] o,
     * String sourceId)
     * {
     * }
     */

    /** For test/debug purposes only.... **/
    public static void main(String args[])
    {
        InjectionDiagnosticModule dm = new InjectionDiagnosticModule();
        dm.validate();
    }
}
