/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.fvt.cdi.jpalib.model;

import java.util.Arrays;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.PostLoad;
import javax.persistence.PostPersist;
import javax.persistence.PostRemove;
import javax.persistence.PostUpdate;
import javax.persistence.PrePersist;
import javax.persistence.PreRemove;
import javax.persistence.PreUpdate;

/**
 * This is a JPA Entity Listener implementation that does simple logging when its
 * associated entity (Widget) is used. JPA entity listeners have been around for
 * a while, but CDI injection is new in the JPA 2.1 spec.
 *
 * According to the spec, the injection should occur before the @PostConstruct
 * method and all other callback methods are invoked. Still, in case of a failure,
 * it will be nice to catch it rather than see a NullPointerException, so we'll
 * use a default value for the injected resource (LoggingService).
 *
 * The spec also allows for the callback methods to be any level of visibility, so
 * the visibility of these methods are arbitrarily public, package, default or private
 * to verify that. Also, the spec allows the parameter of the callback method to be
 * specific (Widget) or generic (Object), so we have different parameter types as well.
 *
 */
@Named
public class WidgetEntityListener {
    public final static String CLASS_NAME = WidgetEntityListener.class.getName();

    private LoggingService logger;

    public WidgetEntityListener() {
        // Default logger to avoid NPEs if injection fails.
        logger = new LoggingService() {

            private final List<String> _messages = Arrays.asList(new String[] { "injection failed" });

            @Override
            public void log(String s) {
                System.out.println("Default logger - injection failed: " + s);
            }

            @Override
            public List<String> getAndClearMessages() {
                return _messages;
            }

        };
        System.out.println("WidgetEntityListener <init>");
    }

    @Inject
    public void setLoggingService(LoggingService ls) {
        logger = ls;
        logger.log(msg("injection"));
    }

    @PostConstruct
    public void postConstruct() {
        logger.log(msg("postConstruct"));
    }

    @PreDestroy
    public void preDestroy() {
        logger.log(msg("preDestroy"));
    }

    @PrePersist
    public void prePersist(Object o) {
        logger.log(msg("prePersist " + o));
    }

    @PostPersist
    protected void postPersist(Object o) {
        logger.log(msg("postPersist " + o));
    }

    @PreRemove
    void preRemove(Object o) {
        logger.log(msg("preRemove " + o));
    }

    @PostRemove
    private void postRemove(Object o) {
        logger.log(msg("postRemove " + o));
    }

    @PreUpdate
    public void preUpdate(Widget o) {
        logger.log(msg("preUpdate " + o));
    }

    @PostUpdate
    protected void postUpdate(Widget o) {
        logger.log(msg("postUpdate " + o));
    }

    @PostLoad
    protected void postLoad(Widget o) {
        logger.log(msg("postLoad " + o));
    }

    private static String msg(String s) {
        return "WidgetEntityListener-" + s;
    }
}
