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
package com.ibm.oauth.core.internal.oauth20.mediator;

import java.util.List;

import com.ibm.oauth.core.api.attributes.AttributeList;
import com.ibm.oauth.core.api.config.OAuthComponentConfiguration;
import com.ibm.oauth.core.api.error.OAuthException;
import com.ibm.oauth.core.api.error.oauth20.OAuth20MediatorException;
import com.ibm.oauth.core.api.oauth20.mediator.OAuth20Mediator;
import com.ibm.oauth.core.api.statistics.OAuthStatisticNames;
import com.ibm.oauth.core.internal.statistics.OAuthStatHelper;
import com.ibm.oauth.core.internal.statistics.OAuthStatisticsImpl;

/**
 * Wraps customer provided mediators with statistics measurements
 */
public class OAuth20MediatorWrapper implements OAuth20Mediator {

    List<OAuth20Mediator> mediators;
    OAuthStatisticsImpl _stats;

    public OAuth20MediatorWrapper(List<OAuth20Mediator> mediators,
            OAuthStatisticsImpl stats) {
        this.mediators = mediators;
        this._stats = stats;
    }

    public void init(OAuthComponentConfiguration config) {
        // don't stat this one
        for (OAuth20Mediator mediator : mediators) {
            mediator.init(config);
        }

    }

    public void mediateAuthorize(AttributeList attributeList)
            throws OAuth20MediatorException {
        OAuthStatHelper statHelper = new OAuthStatHelper(
                OAuthStatisticNames.OAUTH20_MEDIATOR_MEDIATEAUTHORIZE);
        for (OAuth20Mediator mediator : mediators) {
            mediator.mediateAuthorize(attributeList);
        }
        _stats.addMeasurement(statHelper);
    }

    public void mediateToken(AttributeList attributeList)
            throws OAuth20MediatorException {
        OAuthStatHelper statHelper = new OAuthStatHelper(
                OAuthStatisticNames.OAUTH20_MEDIATOR_MEDIATETOKEN);
        for (OAuth20Mediator mediator : mediators) {
            mediator.mediateToken(attributeList);
        }
        _stats.addMeasurement(statHelper);
    }

    public void mediateResource(AttributeList attributeList)
            throws OAuth20MediatorException {
        OAuthStatHelper statHelper = new OAuthStatHelper(
                OAuthStatisticNames.OAUTH20_MEDIATOR_MEDIATERESOURCE);
        for (OAuth20Mediator mediator : mediators) {
            mediator.mediateResource(attributeList);
        }
        _stats.addMeasurement(statHelper);
    }

    public void mediateAuthorizeException(AttributeList attributeList,
            OAuthException exception) throws OAuth20MediatorException {
        OAuthStatHelper statHelper = new OAuthStatHelper(
                OAuthStatisticNames.OAUTH20_MEDIATOR_MEDIATEAUTHORIZE_EXCEPTION);
        for (OAuth20Mediator mediator : mediators) {
            mediator.mediateAuthorizeException(attributeList, exception);
        }
        _stats.addMeasurement(statHelper);
    }

    public void mediateTokenException(AttributeList attributeList,
            OAuthException exception) throws OAuth20MediatorException {
        OAuthStatHelper statHelper = new OAuthStatHelper(
                OAuthStatisticNames.OAUTH20_MEDIATOR_MEDIATETOKEN_EXCEPTION);
        for (OAuth20Mediator mediator : mediators) {
            mediator.mediateTokenException(attributeList, exception);
        }
        _stats.addMeasurement(statHelper);
    }

    public void mediateResourceException(AttributeList attributeList,
            OAuthException exception) throws OAuth20MediatorException {
        OAuthStatHelper statHelper = new OAuthStatHelper(
                OAuthStatisticNames.OAUTH20_MEDIATOR_MEDIATERESOURCE_EXCEPTION);
        for (OAuth20Mediator mediator : mediators) {
            mediator.mediateResourceException(attributeList, exception);
        }
        _stats.addMeasurement(statHelper);
    }

}
