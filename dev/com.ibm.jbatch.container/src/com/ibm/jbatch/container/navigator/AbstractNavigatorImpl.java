/*
 * Copyright 2012 International Business Machines Corp.
 *
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership. Licensed under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ibm.jbatch.container.navigator;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.jbatch.container.annotation.TCKExperimentProperty;
import com.ibm.jbatch.container.jsl.impl.GlobPatternMatcherImpl;
import com.ibm.jbatch.container.jsl.impl.TransitionImpl;
import com.ibm.jbatch.container.status.ExecutionStatus;
import com.ibm.jbatch.container.status.ExtendedBatchStatus;
import com.ibm.jbatch.jsl.model.Decision;
import com.ibm.jbatch.jsl.model.End;
import com.ibm.jbatch.jsl.model.Fail;
import com.ibm.jbatch.jsl.model.Flow;
import com.ibm.jbatch.jsl.model.Next;
import com.ibm.jbatch.jsl.model.Split;
import com.ibm.jbatch.jsl.model.Step;
import com.ibm.jbatch.jsl.model.Stop;
import com.ibm.jbatch.jsl.model.helper.ExecutionElement;
import com.ibm.jbatch.jsl.model.helper.Transition;
import com.ibm.jbatch.jsl.model.helper.TransitionElement;

public abstract class AbstractNavigatorImpl<T> implements ModelNavigator<T> {

    private final static Logger logger = Logger.getLogger(AbstractNavigatorImpl.class.getName());

    @TCKExperimentProperty
    private final static boolean disallowDecisionLoopback = AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
        @Override
        public Boolean run() {
            return Boolean.getBoolean("disallow.decision.loopback");
        }
    });

    private final Map<String, ExecutionElement> alreadyExecutedElements = new HashMap<String, ExecutionElement>();

    public ExecutionElement getFirstExecutionElement(List<ExecutionElement> peerExecutionElements, String restartOn) throws IllegalTransitionException {
        final String method = "getFirstExecutionElement";

        logger.fine(method + " , restartOn = " + restartOn);

        ExecutionElement startElement = null;

        if (restartOn != null) {
            startElement = getExecutionElementFromId(peerExecutionElements, restartOn);
            if (startElement == null) {
                throw new IllegalStateException("Didn't find an execution element maching restart-on designated element: " + restartOn);
            }
        } else {
            if (peerExecutionElements.size() > 0) {
                startElement = peerExecutionElements.get(0);
            } else {
                logger.fine(method + " , Container appears to contain no execution elements.  Returning.");
                return null;
            }
        }

        if (logger.isLoggable(Level.FINE)) {
            logger.fine(method + " , Found start element: " + startElement);
        }

        if (!disallowDecisionLoopback) {
            // We allow repeating a decision
            if (!(startElement instanceof Decision)) {
                alreadyExecutedElements.put(startElement.getId(), startElement);
            }
        } else {
            alreadyExecutedElements.put(startElement.getId(), startElement);

        }

        validateElementType(startElement);

        return startElement;
    }

    /**
     * Precedence is: look at elements, then look at attribute, then return quietly
     *
     * @param currentElem
     * @param peerExecutionElements
     * @param currentExitStatus
     * @return
     * @throws IllegalTransitionException
     */
    public Transition getNextTransition(ExecutionElement currentElem, List<ExecutionElement> peerExecutionElements,
                                        ExecutionStatus currentStatus) throws IllegalTransitionException {
        final String method = "getNextTransition";

        if (logger.isLoggable(Level.FINE)) {
            logger.fine(method + " ,currentStatus=" + currentStatus);
        }

        Transition returnTransition = new TransitionImpl();

        ExecutionElement nextExecutionElement = null;

        List<TransitionElement> transitionElements = currentElem.getTransitionElements();

        // Check the transition elements first.
        if (!transitionElements.isEmpty()) {

            if (currentElem instanceof Flow) {
                throw new IllegalStateException("Flow-level transition elements such as <next>, <stop>, <end>, <fail>, are not allowed, " +
                                                "since the behavior is undefined by the specification.  Failing job."); // TODO - translate
            }

            for (TransitionElement t : transitionElements) {
                logger.fine(method + " Trying to match next transition element: " + t);

                boolean isMatched = matchExitStatusAgainstOnAttribute(currentStatus.getExitStatus(), t);
                if (isMatched) {
                    if (t instanceof Next) {
                        Next next = (Next) t;
                        nextExecutionElement = getExecutionElementFromId(peerExecutionElements, next.getTo());
                        returnTransition.setNextExecutionElement(nextExecutionElement);
                        break;
                    } else {
                        returnTransition.setTransitionElement(t);
                    }
                    return returnTransition;
                }
            }
        }

        // We've returned already if we matched a Stop, End or Fail
        if (nextExecutionElement == null) {
            if (currentStatus.getExtendedBatchStatus().equals(ExtendedBatchStatus.EXCEPTION_THROWN)) {
                logger.fine("Didn't match transition element, after exception thrown.  Need to fail job");
                returnTransition.setNoTransitionElementMatchAfterException();
                return returnTransition;
            } else {
                logger.fine("Didn't match transition element, check @next attribute now.");
                nextExecutionElement = getNextExecutionElemFromAttribute(peerExecutionElements, currentElem);
                returnTransition.setNextExecutionElement(nextExecutionElement);
            }
        }

        if (nextExecutionElement != null) {
            if (alreadyExecutedElements.containsKey(nextExecutionElement.getId())) {
                String errorMsg = "Execution loop detected !!!  Trying to re-execute execution element: " + nextExecutionElement.getId();
                throw new IllegalTransitionException(errorMsg);
            }

            if (!disallowDecisionLoopback) {
                // We allow repeating a decision
                if (!(nextExecutionElement instanceof Decision)) {
                    alreadyExecutedElements.put(nextExecutionElement.getId(), nextExecutionElement);
                }
            } else {
                alreadyExecutedElements.put(nextExecutionElement.getId(), nextExecutionElement);
            }
            logger.fine(method + " Transitioning to next element id = " + nextExecutionElement.getId());
        } else {
            logger.fine(method + " There is no next execution element. Mark transition to show we're finished.");
            returnTransition.setFinishedTransitioning();
        }
        return returnTransition;
    }

    private ExecutionElement getExecutionElementFromId(List<ExecutionElement> executionElements, String id) throws IllegalTransitionException {
        if (id != null) {
            logger.finer("attribute value is " + id);
            for (ExecutionElement elem : executionElements) {
                if (elem.getId().equals(id)) {
                    validateElementType(elem);
                    return elem;
                }
            }
            throw new IllegalTransitionException("No execution element found with id = " + id);
        } else {
            logger.finer("attribute value is <null>, so simply exiting...");
            return null;
        }
    }

    private static boolean matchSpecifiedExitStatus(String currentStepExitStatus, String exitStatusPattern) {

        logger.finer("matchSpecifiedExitStatus, matching current exitStatus  " + currentStepExitStatus + " against pattern: " + exitStatusPattern);

        GlobPatternMatcherImpl matcher = new GlobPatternMatcherImpl();
        boolean match = matcher.matchWithoutBackslashEscape(currentStepExitStatus, exitStatusPattern);

        if (match) {
            logger.finer("matchSpecifiedExitStatus, match=YES");
            return true;
        } else {
            logger.finer("matchSpecifiedExitStatus, match=NO");
            return false;
        }
    }

    private boolean matchExitStatusAgainstOnAttribute(String exitStatus, TransitionElement elem) {
        logger.fine("Trying to match exitStatus = " + exitStatus + " , against transition element: " + elem);
        String exitStatusToMatch = null;

        if (elem instanceof End) {
            exitStatusToMatch = ((End) elem).getOn();
        } else if (elem instanceof Fail) {
            exitStatusToMatch = ((Fail) elem).getOn();
            return matchSpecifiedExitStatus(exitStatus, exitStatusToMatch);
        } else if (elem instanceof Stop) {
            exitStatusToMatch = ((Stop) elem).getOn();
        } else if (elem instanceof Next) {
            exitStatusToMatch = ((Next) elem).getOn();
        } else {
            throw new IllegalStateException("Shouldn't be possible to get here. Unknown transition element,  " + elem.toString());
        }

        boolean match = matchSpecifiedExitStatus(exitStatus, exitStatusToMatch);
        String logMsg = match ? "Matched" : "Didn't match";
        logger.fine(logMsg);
        return match;
    }

    private ExecutionElement getNextExecutionElemFromAttribute(List<ExecutionElement> peerExecutionElements, ExecutionElement currentElem) throws IllegalTransitionException {
        ExecutionElement nextExecutionElement = null;
        String nextAttrId = null;
        if (currentElem instanceof Step) {
            nextAttrId = ((Step) currentElem).getNextFromAttribute();
            nextExecutionElement = getExecutionElementFromId(peerExecutionElements, nextAttrId);
        } else if (currentElem instanceof Split) {
            nextAttrId = ((Split) currentElem).getNextFromAttribute();
            nextExecutionElement = getExecutionElementFromId(peerExecutionElements, nextAttrId);
        } else if (currentElem instanceof Flow) {
            nextAttrId = ((Flow) currentElem).getNextFromAttribute();
            nextExecutionElement = getExecutionElementFromId(peerExecutionElements, nextAttrId);
        } else if (currentElem instanceof Decision) {
            // Nothing special to do in this case.
        }

        validateElementType(nextExecutionElement);

        logger.fine("From currentElem = " + currentElem + " , return @next attribute execution element: " + nextExecutionElement);
        return nextExecutionElement;
    }

    @Override
    public ExecutionElement getFirstExecutionElement() throws IllegalTransitionException {
        return getFirstExecutionElement(null);
    }

    private void validateElementType(ExecutionElement elem) {
        if (elem != null) {
            if (!((elem instanceof Decision) || (elem instanceof Flow) || (elem instanceof Split) || (elem instanceof Step))) {
                throw new IllegalArgumentException("Unknown execution element found, elem = " + elem + ", found with type: " + elem.getClass().getCanonicalName() +
                                                   " , which is not an instance of Decision, Flow, Split, or Step.");
            }
        }
    }

}
