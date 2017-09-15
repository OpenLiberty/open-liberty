/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.ras;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ibm.ws.logging.internal.PackageProcessor;

/**
 * An exception which can produce trimmed stack traces. If passed as a parameter to the logger, it will output the
 * trimmed stack traces to the console, and the full stack traces to messages.log and trace.log.
 * To get full stack traces, call {@link #getWrappedException()} and then call {@link #printStackTrace()} on the returned exception.
 */
public class TruncatableThrowable extends Throwable {

    public static final String CAUSED_BY = "Caused by: ";
    private static final long serialVersionUID = -3401687914236012149L;
    public static final String INTERNAL_CLASSES_STRING = "[internal classes]";
    // An eye catcher which represents internal classes (we'd love to subclass, but can't)
    private static final StackTraceElement TRIMMED_STACK_TRACE_ELEMENT = new StackTraceElement(INTERNAL_CLASSES_STRING, "", null, 0);
    private static final String DUPLICATE_FRAMES_EYECATCHER = "DuplicateFramesEliminatedInternally";
    // Strip off optional object notations from the ClassNotFoundException message 
    private static final Pattern NO_CLASS_DEF_FOUND_ERROR_CLASS_PATTERN = Pattern.compile("L?([^;]*);?");
    // The NoClassDefFoundError may prefix its name to its message - use an optional anonymous group
    private static final Pattern CLASS_NOT_FOUND_EXCEPTION_CLASS_PATTERN = Pattern.compile("(?:" + ClassNotFoundException.class.getName() + ": )?(.*)");

    private final Throwable wrapped;
    private StackTraceElement[] stackTrace;
    private StackTraceElement[] noduplicatesStackTrace;
    // Useful for eliminating duplicate frames
    private final StackTraceElement[] parentFrames;
    private TruncatableThrowable truncatedCause;
    private boolean intermediateCausesStripped = false;
    private final PackageProcessor packageHelper = PackageProcessor.getPackageProcessor();

    public TruncatableThrowable(Throwable wrapped) {
        this(wrapped, null);
    }

    private TruncatableThrowable(Throwable wrapped, StackTraceElement[] parentFrames) {
        this.wrapped = wrapped;
        this.parentFrames = parentFrames;
    }

    @Override
    public void printStackTrace(PrintStream stream) {
        PrintWriter printWriter = new PrintWriter(stream);
        printStackTrace(printWriter);
        printWriter.flush();
    }

    /**
     * This method will print a trimmed stack trace to stderr.
     */
    @Override
    public void printStackTrace(PrintWriter p) {
        if (wrapped == null) {
            p.println("none");
        } else {

            StackTraceElement[] stackElements = getStackTraceEliminatingDuplicateFrames();

            // format and print
            p.println(wrapped);
            for (int i = 0; i < stackElements.length; i++) {
                StackTraceElement stackTraceElement = stackElements[i];
                final String toString = printStackTraceElement(stackTraceElement);
                p.println("\t" + toString);
            }

            TruncatableThrowable cause = getCause();
            // We know the cause will be truncatable, so not much extra work to do here
            // There's a super-class method we could call, but it's private :(

            if (cause != null) {
                // Non-internationalised string in what we're trying to imitate
                if (cause.isIntermediateCausesStripped()) {
                    p.print("Caused by (repeated) ... : ");

                } else {
                    p.print(CAUSED_BY);
                }

                cause.printStackTrace(p);
            }

        }

    }

    /**
     * @param frames
     * @param stackElements
     * @return
     */
    private int countNonDuplicatedFrames(StackTraceElement[] frames, StackTraceElement[] stackElements) {
        int numberToInclude = stackElements.length;
        if (frames != null) {
            int j = frames.length - 1;

            // Start at the bottom, since the top is more likely to be different
            for (numberToInclude = stackElements.length - 1; numberToInclude >= 0; numberToInclude--) {
                if (j >= 0) {
                    if (!(stackElements[numberToInclude].equals(frames[j]))) {
                        // Add one thing back to the stack trace
                        numberToInclude++;
                        break;
                    } else {
                        j--;
                    }
                }
            }
        }
        return numberToInclude;
    }

    @Override
    public TruncatableThrowable getCause() {

        if (truncatedCause == null) {
            Throwable wrappedCause = wrapped.getCause();

            if (wrappedCause != null) {

                // Strip out causes which are very very similar 
                if (wrapped instanceof NoClassDefFoundError) {
                    if (wrappedCause instanceof ClassNotFoundException) {
                        // Final check - are the class names the same?
                        String message = wrapped.getMessage();
                        String causeMessage = wrappedCause.getMessage();

                        if (message != null && causeMessage != null) {
                            // Last check - are we complaining about the same class? (Once we account for different presentation)

                            // Do the best we can to figure out the classes - if we get it wrong, consequences are minor
                            Matcher matcher = NO_CLASS_DEF_FOUND_ERROR_CLASS_PATTERN.matcher(message);
                            if (matcher.matches()) {
                                String wrappedTargetClass = matcher.group(1);
                                Matcher causeMatcher = CLASS_NOT_FOUND_EXCEPTION_CLASS_PATTERN.matcher(causeMessage);
                                if (causeMatcher.matches()) {
                                    String causeTargetClass = causeMatcher.group(1);
                                    if (wrappedTargetClass.replace('/', '.').equals(causeTargetClass)) {
                                        // We can't cache null, when null is the marker for our cache, but assume we won't pass
                                        // here very often
                                        return null;
                                    }
                                }
                            }
                        }

                    }
                }

                truncatedCause = new TruncatableThrowable(wrappedCause, getStackTrace());

                // Bypass causes which are too similar to ourselves
                boolean isBottomOfNest = wrappedCause.getCause() == null;
                if (!isBottomOfNest) {
                    boolean wrappedExceptionIsSimilarToUs = wrappedCause.getClass().equals(getWrappedException().getClass());
                    if (wrappedExceptionIsSimilarToUs) {
                        intermediateCausesStripped = true;
                        // This will handle any necessary recursive stripping of duplicates
                        truncatedCause = truncatedCause.getCause();
                    }
                }
            }
        }
        return truncatedCause;

    }

    private boolean isIntermediateCausesStripped() {
        // This will set 'intermediateCausesStripped'
        getCause();
        return intermediateCausesStripped;
    }

    @Override
    public StackTraceElement[] getStackTrace() {
        if (stackTrace == null) {
            stackTrace = getStackTrace(wrapped.getStackTrace());
        }
        return stackTrace.clone();
    }

    /**
     * Useful for exceptions which are the causes of other exceptions. Gets the stack frames, but not only
     * does it eliminate internal classes, it eliminates frames which are redundant with the parent
     * exception.
     * In the case where the exception is not a cause, it returns a normal exception.
     * If duplicate frames are stripped, it will add an
     * 
     * @DUPLICATE_FRAMES_STACK_TRACE_ELEMENT eyecatcher
     * 
     * @return
     */

    public StackTraceElement[] getStackTraceEliminatingDuplicateFrames() {
        // If this isn't a cause, there can't be any duplicate frames, so proceed no further
        if (parentFrames == null) {
            return getStackTrace();
        }
        if (noduplicatesStackTrace == null) {
            List<StackTraceElement> list = new ArrayList<StackTraceElement>();
            // Only put the comment saying there are other classes in the stack if there actually are,
            // and if this isn't a 'cause' exception
            StackTraceElement[] stackElements = getStackTrace();

            // Now do a second trimming, if necessary, to eliminate any duplication in 'caused by' traces
            int numberToInclude = countNonDuplicatedFrames(parentFrames, stackElements);

            for (int i = 0; i < numberToInclude; i++) {
                list.add(stackElements[i]);
            }
            // Only put the comment saying there are other classes in the stack if there actually are
            boolean duplicateFramesRemoved = numberToInclude < stackElements.length;
            if (duplicateFramesRemoved) {
                // Use shonky eyecatchers since we can't subclass StackTraceElement
                list.add(new StackTraceElement("... " + (stackElements.length - numberToInclude) + " more", DUPLICATE_FRAMES_EYECATCHER, null, 0));
            }

            noduplicatesStackTrace = list.toArray(new StackTraceElement[0]);

        }
        return noduplicatesStackTrace.clone();
    }

    private StackTraceElement[] getStackTrace(StackTraceElement[] stackElements) {
        List<StackTraceElement> trimmedStack = new ArrayList<StackTraceElement>();

        if (stackElements != null) {
            // created trimmed stack
            // We may need to trim out bits of the middle and then keep going ... 
            for (StackTraceElement element : stackElements) {
                boolean isInternal = isInternal(element);
                if (isInternal) {
                    if (trimmedStack.size() == 0 || trimmedStack.get(trimmedStack.size() - 1) != TRIMMED_STACK_TRACE_ELEMENT) {
                        // include the topmost internal stack if we haven't exceeded the stack
                        trimmedStack.add(element);
                        trimmedStack.add(TRIMMED_STACK_TRACE_ELEMENT);
                    }
                } else {
                    trimmedStack.add(element);

                }
            }

            // Now make a second pass, in the reverse order. We want to keep spec classes used by 
            // user code, but not spec classes used by IBM code, or at the very bottom of the stack
            if (trimmedStack.contains(TRIMMED_STACK_TRACE_ELEMENT)) {
                // Start with the assumption that spec things at the bottom of the stack, under an [internal classes], should be removed
                boolean isCalledByIBMCode = true;
                List<Integer> maybeRemoves = new ArrayList<Integer>();
                // Work backwards from the end of the list or we risk missing elements as we shorten the list
                for (int i = trimmedStack.size() - 1; i >= 0; i--) {
                    StackTraceElement element = trimmedStack.get(i);
                    if (element != TRIMMED_STACK_TRACE_ELEMENT && isCalledByIBMCode && isSpecOrThirdParty(element)) {
                        // This is a candidate for removal - we should remove it if we hit another 
                        // [at internal classes] on the other side, with only spec stuff between
                        maybeRemoves.add(i);

                    } else if (isCalledByIBMCode && element == TRIMMED_STACK_TRACE_ELEMENT) {
                        // We hit the other half of the sandwich, so go through and remove all the spec classes in the sandwich

                        for (int index : maybeRemoves) {
                            trimmedStack.remove(index);
                        }
                        maybeRemoves.clear();
                        // Make sure we don't get two [internal classes] butted up against each other
                        // as a result of trimming out sandwiched bits
                        // (This won't apply to the very bottom [internal classes])
                        if (i < trimmedStack.size() - 1) {
                            trimmedStack.remove(i);
                            if (i < trimmedStack.size() - 1) {
                                // We also need to trim the IBM code in front of the [internal classes]
                                trimmedStack.remove(i);
                            }
                        }
                    } else if (element == TRIMMED_STACK_TRACE_ELEMENT) {
                        // We're potentially in an IBM sandwich 
                        isCalledByIBMCode = true;
                    } else {
                        // Reset the first time we hit user code 
                        if (!isInternal(element) && !isSpecOrThirdParty(element)) {
                            isCalledByIBMCode = false;
                            maybeRemoves.clear();
                        }
                    }

                }
            }
        }

        return trimmedStack.toArray(new StackTraceElement[0]);

    }

    private boolean isInternal(StackTraceElement element) {
        if (element == TRIMMED_STACK_TRACE_ELEMENT) {
            return true;
        }
        if (packageHelper != null) {
            String packageName = PackageProcessor.extractPackageFromStackTraceElement(element);
            return packageHelper.isIBMPackage(packageName);
        } else {
            return false;
        }
    }

    private boolean isSpecOrThirdParty(StackTraceElement element) {
        if (element == TRIMMED_STACK_TRACE_ELEMENT) {
            return false;
        }
        if (packageHelper != null) {
            String packageName = PackageProcessor.extractPackageFromStackTraceElement(element);
            return packageHelper.isSpecOrThirdPartyOrBootDelegationPackage(packageName);
        } else {
            return false;
        }
    }

    /**
     * @return
     */
    public Throwable getWrappedException() {
        return wrapped;
    }

    @Override
    public String getMessage() {
        return wrapped.getMessage();
    }

    /**
     * Unfortunately, toString() isn't good enough since we may have elements in the stack representing
     * internal classes, and they don't toString() well.
     */
    public static String printStackTraceElement(StackTraceElement stackTraceElement) {
        final String toString;
        if (stackTraceElement == null) {
            toString = "at null";
        }
        // Look for our eyecatcher representing a truncation
        else if (TRIMMED_STACK_TRACE_ELEMENT.equals(stackTraceElement)) {
            // Use only the class name, which is actually [internal classes]
            toString = "at " + stackTraceElement.getClassName();
            // Look for our eyecatcher representing a the stripping of redundant cause frames
        } else if (DUPLICATE_FRAMES_EYECATCHER.equals(stackTraceElement.getMethodName())) {
            // Use only the class name, which will include the ... - we don't want a "at " in this case
            toString = stackTraceElement.getClassName();
        } else {
            // The 'at' is a non-internationalised string in what we're trying to imitate   
            toString = "at " + stackTraceElement.toString();
        }
        return toString;
    }

    @Override
    public String toString() {
        // Delegate transparently to the wrapped exception
        return wrapped.toString();
    }

}
