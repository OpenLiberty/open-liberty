/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.ffdc;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.ListIterator;

/**
 * Extend this class to create a diagnostic module to help FFDC capture and dump
 * data for your component. Example usage:
 * 
 * <pre>
 * DiagnosticModuleForComponent dm = new DiagnosticModuleForComponent();
 * if (FFDC.registerDiagnosticModule(dm, &quot;com.ibm.ws.mycomponent.packagename&quot;) != 0)
 * {
 * // Diagnostic module failed to meet the criteria described above
 * // Return codes :
 * // 0 - DM was registered
 * // 1 - A DM has already been registered with the package name
 * // 2 - The DM did not meet the criteria to contain a single dump method
 * // 3 - An unknown failure occured during the registration
 * }
 * </pre>
 * 
 * Any methods beginning with "ffdcDump" will be recognized by the ffdc runtime,
 * and used for data collection. These methods should expect the following
 * parameters:
 * <ol>
 * <li>Throwable th. The encountered exception.
 * <li>IncidentStream is. Write captured data to this stream.
 * <li>Object[] others. The value of the array may be null. If not null, it
 * contains an array of objects which the caller to the FFDC filter passed.
 * <li>Object callerThis. The callerThis value may be null, if the method which
 * invoked the filter was a static method. Or if the current point of execution
 * for the filter, does not correspond to the DM being invoked.
 * <li>String sourceId. The sourceId passed to the filter.
 * </ol>
 * *
 * <p>
 * Method names that start with "ffdcDumpDefault" will always be called when
 * this component is involved in an exception or error situation. These methods
 * will be responsible for performing the default data capture for their
 * component.
 * 
 * <p>
 * Method names that simply start with "ffdcDump" [not ffdcDumpDefault] are only
 * called under certain conditions and are not considered part of the default
 * data capture.
 * 
 * <p>
 * Methods that don't start with "ffdcDump", or that don't conform to the
 * expected signature will be ignored. Setting the
 * <code>com.ibm.ws.ffdc.debugDiagnosticModule</code> system property to true
 * will cause exceptions to be thrown for malformed diagnostic module
 * signatures.
 */
public class DiagnosticModule {
    /**
     * Name of property used to enable noise-making when badly formed methods
     * are encountered when initializing a new module.
     */
    public static final String DEBUG_DM_PROPERTY = "com.ibm.ws.ffdc.debugDiagnosticModule";

    /** The prefix of ffdcdump methods (compared using toLowerCase) */
    public static final String FFDC_DUMP_PREFIX = "ffdcdump";

    /** The prefix of ffdcdumpdefault methods (compared using toLowerCase) */
    public static final String FFDC_DUMP_DEFAULT_PREFIX = FFDC_DUMP_PREFIX + "default";

    /** The parameter types of the ffdcdump methods */
    static final Class<?> FFDC_DUMP_PARAMS[] = { Throwable.class, IncidentStream.class, Object.class, Object[].class, String.class };

    /** Lazy-initialized value of debug property */
    static Boolean debugDiagnosticModules = null;

    /** True if this diagnostic module has already been initialized. */
    private boolean initialized;

    /**
     * A list of methods in this diagnostic module that start with
     * ffdcdumpdefault
     */
    private final List<Method> _dumpDefaultMethods = new ArrayList<Method>();

    /** A list of methods in this diagnostic module that start with ffdcdump */
    private final List<Method> _dumpMethods = new ArrayList<Method>();

    /** A list of methods in this diagnostic module that start with ffdcdump */
    private final List<String> _directives = new ArrayList<String>();

    /** Indicates if we can allow any other DMs to run once this one has run */
    private final ThreadLocal<Boolean> _continueProcessing = new ThreadLocal<Boolean>();

    /**
     * The init method is provided to subclasses to initialize this particular
     * DiagnosticModule. Called when the diagnostic module is registered.
     * 
     * <p>
     * Done on a best-effort basis-- methods not matching the signature are not
     * added. If the system property "com.ibm.ws.ffdc.debugDiagnosticModule" is
     * set to true, exceptions and other logging will occur for malformed
     * diagnostic module method signatures.
     * 
     * @throws DiagnosticModuleRegistrationFailureException
     */
    final void init() throws DiagnosticModuleRegistrationFailureException {
        if (initialized) {
            return;
        }

        initialized = true;

        Method[] methods = getClass().getMethods();
        for (Method method : methods) {
            String name = method.getName().toLowerCase();

            if (name.startsWith(FFDC_DUMP_PREFIX)) {
                Class<?>[] params = method.getParameterTypes();

                if (params.length == FFDC_DUMP_PARAMS.length) {
                    // Possible candidate method, so check the types
                    boolean allOK = true;
                    for (int i = 0; (i < params.length) && (allOK); i++) {
                        allOK = (params[i] == FFDC_DUMP_PARAMS[i]);
                    }

                    if (allOK) {
                        _directives.add(method.getName());

                        if (name.startsWith(FFDC_DUMP_DEFAULT_PREFIX))
                            _dumpDefaultMethods.add(method);
                        else
                            _dumpMethods.add(method);
                    } else if (makeNoise()) {
                        throw new DiagnosticModuleRegistrationFailureException("Error: " + method + " starts with " + FFDC_DUMP_DEFAULT_PREFIX
                                                                               + " but does not conform to the signature.  Method skipped!");
                    }
                } else if (makeNoise()) {
                    throw new DiagnosticModuleRegistrationFailureException("Error: " + method + " starts with " + FFDC_DUMP_DEFAULT_PREFIX
                                                                           + " but does not conform to the signature.  Method skipped!");
                }
            }
        }
    }

    /**
     * Return the list of directives returned by this diagnostic module.
     * 
     * @return String[]
     */
    public String[] getDirectives() {
        return _directives.toArray(new String[0]);
    }

    /**
     * This method is invoked to instruct the diagnostic module to capture all
     * relevant information that it has about a particular incident
     * 
     * @param input_directives
     *            The directives to be processed for this incident
     * @param ex
     *            The exception that caused this incident
     * @param ffdcis
     *            The incident stream to be used to record the relevant
     *            information
     * @param callerThis
     *            The object reporting the incident
     * @param catcherObjects
     *            Additional objects that might be involved
     * @param sourceId
     *            The source id of the class reporting the incident
     * @param callStack
     *            The list of classes on the stack
     * @return true if more diagnostic modules should be invoked after this one
     */
    public final boolean dumpComponentData(String[] input_directives, Throwable ex, IncidentStream ffdcis, Object callerThis, Object[] catcherObjects, String sourceId,
                                           String[] callStack) {
        startProcessing();

        try {
            ffdcis.writeLine("==> Performing default dump from " + getClass().getName() + " ", new Date());

            for (Method m : _dumpDefaultMethods) {
                invokeDiagnosticMethod(m, ex, ffdcis, callerThis, catcherObjects, sourceId);

                if (!continueProcessing())
                    break;
            }

            if (input_directives != null && input_directives.length > 0) {
                ffdcis.writeLine("==> Performing custom/other dump from " + getClass().getName() + " ", new Date());

                getDataForDirectives(input_directives, ex, ffdcis, callerThis, catcherObjects, sourceId);
            }

            ffdcis.writeLine("==> Dump complete for " + getClass().getName() + " ", new Date());
        } catch (Throwable th) {
            ffdcis.writeLine("==> Dump did not complete for " + getClass().getName() + " ", new Date());
        }

        return finishProcessing();
    }

    /**
     * Invoke all the ffdcdump methods for a set of directives
     * 
     * @param directives
     *            The list of directives to be invoked
     * @param ex
     *            The exception causing the incident
     * @param ffdcis
     *            The incident stream on which to report
     * @param callerThis
     *            The object reporting the incident
     * @param catcherObjects
     *            Any additional interesting objects
     * @param sourceId
     *            The sourceid of the class reporting the problem
     */
    public final void getDataForDirectives(String[] directives, Throwable ex, IncidentStream ffdcis, Object callerThis, Object[] catcherObjects, String sourceId) {
        if (directives == null || directives.length <= 0 || !continueProcessing())
            return;

        for (String s : directives) {
            String sName = s.toLowerCase();
            for (Method m : _dumpMethods) {
                String mName = m.getName().toLowerCase();
                if (mName.equals(sName)) {
                    invokeDiagnosticMethod(m, ex, ffdcis, callerThis, catcherObjects, sourceId);
                    break;
                }
            }

            if (!continueProcessing())
                break;
        }
    }

    /**
     * Invoke dump method
     * 
     * @param m
     *            The method to be invoked
     * @param ex
     *            The exception causing the incident
     * @param ffdcis
     *            The incident stream on which to report
     * @param callerThis
     *            The object reporting the incident
     * @param catcherObjects
     *            Any additional interesting objects
     * @param sourceId
     *            The sourceid of the class reporting the problem
     */
    private final void invokeDiagnosticMethod(Method m, Throwable ex, IncidentStream ffdcis, Object callerThis, Object[] catcherObjects, String sourceId) {
        try {
            m.invoke(this, new Object[] { ex, ffdcis, callerThis, catcherObjects, sourceId });

            ffdcis.writeLine("+ Data for directive [" + m.getName().substring(FFDC_DUMP_PREFIX.length()) + "] obtained.", "");
        } catch (Throwable t) {
            ffdcis.writeLine("Error while processing directive [" + m.getName().substring(FFDC_DUMP_PREFIX.length()) + "] !!!", t);
        }
    }

    /**
     * Validate whether the diagnostic module is correctly coded. Method can be
     * used as a simple validation of a components diagnostic module. The
     * information printed can be used during the development of the DM.
     * 
     * @return true; if the system property
     *         "com.ibm.ws.ffdc.debugDiagnosticModule" is set, then some
     *         validation will be performed on the diagnostic module.
     */
    public final boolean validate() {
        if (makeNoise()) {
            System.out.println("This method is NOT intended to be called from the runtime");
            System.out.println("but is provided as part of unit test for diagnostic modules");

            ListIterator<Method> im;

            try {
                init();

                System.out.println("default directives on the diagnostic module : ");
                im = _dumpDefaultMethods.listIterator();
                while (im.hasNext()) {
                    System.out.println("\t" + im.next());
                }

                System.out.println("ffdc methods on the diagnostic module : ");
                im = _dumpMethods.listIterator();
                while (im.hasNext()) {
                    System.out.println("\t" + im.next());
                }
            } catch (DiagnosticModuleRegistrationFailureException dmfailed) {
                System.out.println("Diagnostic Module failed to register: " + dmfailed);
                dmfailed.printStackTrace();
                return false;
            } catch (Throwable th) {
                System.out.println("Some unknown failure occured: " + th);
                th.printStackTrace();
                return false;
            }
        }
        return true;
    }

    /**
     * Check the the ThreadLocal to see if we should continue processing this
     * FFDC exception
     * 
     * @return
     */
    private boolean continueProcessing() {
        Boolean currentValue = _continueProcessing.get();
        if (currentValue != null)
            return currentValue.booleanValue();

        return true;
    }

    private final void startProcessing() {
        _continueProcessing.set(Boolean.TRUE);
    }

    private final boolean finishProcessing() {
        Boolean currentValue = _continueProcessing.get();
        _continueProcessing.remove();
        return currentValue.booleanValue();
    }

    /**
     * Inform this base class (and the diagnostic engine) that no more diagnosis
     * information is required for this incident
     */
    public final void stopProcessingException() {
        _continueProcessing.set(Boolean.FALSE);
    }

    /**
     * Lazy check for system property (wait until a DiagnosticModule is
     * constructed) to see whether or not we should be making noise for badly
     * formed modules.
     * 
     * @return true if we should make noise
     */
    private final static boolean makeNoise() {
        if (debugDiagnosticModules == null) {
            debugDiagnosticModules = Boolean.valueOf(Boolean.getBoolean(DEBUG_DM_PROPERTY));
        }
        return debugDiagnosticModules.booleanValue();
    }
}
