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

package com.ibm.ws.sib.processor;

import java.util.List;
import java.util.Set;

import com.ibm.websphere.sib.exception.SIException;
import com.ibm.websphere.sib.exception.SIIncorrectCallException;
import com.ibm.websphere.sib.exception.SINotPossibleInCurrentConfigurationException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.sib.admin.DestinationAliasDefinition;
import com.ibm.ws.sib.admin.DestinationDefinition;
import com.ibm.ws.sib.admin.LocalizationDefinition;
import com.ibm.ws.sib.admin.MQLinkDefinition;
import com.ibm.ws.sib.admin.SIBExceptionBase;
import com.ibm.ws.sib.admin.VirtualLinkDefinition;
import com.ibm.ws.sib.processor.exceptions.SIMPDestinationAlreadyExistsException;
import com.ibm.ws.sib.processor.exceptions.SIMPNullParameterException;
import com.ibm.ws.sib.processor.runtime.SIMPMessageProcessorControllable;
import com.ibm.wsspi.sib.core.SITransaction;
import com.ibm.wsspi.sib.core.exception.SIConnectionLostException;
import com.ibm.wsspi.sib.core.exception.SIDestinationLockedException;
import com.ibm.wsspi.sib.core.exception.SIDiscriminatorSyntaxException;
import com.ibm.wsspi.sib.core.exception.SIDurableSubscriptionNotFoundException;
import com.ibm.wsspi.sib.core.exception.SINotAuthorizedException;
import com.ibm.wsspi.sib.core.exception.SISelectorSyntaxException;

/**
 * @author prmf
 */
public interface Administrator
{
  /**
   * <p>Creates a durable subscription.</p>
   *
   * @param id   The subscription id
   * @param def  The subscription definition
   * @param tran The transaction context
   * @return void
   * @throws SIDestinationAlreadyExistsException
   * @throws SINotPossibleInCurrentConfigurationException
   * @throws SIIncorrectCallException
   * @throws SIResourceException
   * @throws SIMPNullParameterException
   * @throws SINotAuthorizedException
   * @throws SIDestinationLockedException
   * @throws SIDiscriminatorSyntaxException
   * @throws SISelectorSyntaxException
   * @throws SICoreException
   */
  public void createSubscription(String id,SubscriptionDefinition def,SITransaction tran)
  throws
    SIMPNullParameterException,
    SINotAuthorizedException,
    SINotPossibleInCurrentConfigurationException,
    SIMPDestinationAlreadyExistsException,
    SIIncorrectCallException,
    SIDestinationLockedException,
    SIDiscriminatorSyntaxException,
    SISelectorSyntaxException,
    SIResourceException,
    SIException;

  /**
   * <p>Deletes a subscription.</p>
   *
   * @param  id    The subscription id
   * @param  force The force deletion switch
   * @return void
   * @throws SINotPossibleInCurrentConfigurationException
   * @throws SIResourceException
   * @throws SIIncorrectCallException
   * @throws SINotAuthorizedException
   * @throws SIDestinationLockedException
   * @throws SICoreException  For any unknown errors
   */
  public void deleteSubscription(String id,boolean force)
    throws
    SINotPossibleInCurrentConfigurationException,
    SIDurableSubscriptionNotFoundException,
    SIResourceException,
    SIIncorrectCallException,
    SINotAuthorizedException,
    SIDestinationLockedException,
    SIException;

  /**
   * <p>Queries a subscription.</p>
   *
   * @param  id   The subscription id
   * @return def  The subscription definition
   * @throws SIDestinationNotFoundException
   * @throws SIResourceException
   * @throws SIDurableSubscriptionNotFoundException
   */
  public SubscriptionDefinition querySubscription(String id)
    throws
    SINotPossibleInCurrentConfigurationException,
    SIResourceException,
    SIIncorrectCallException,
    SIDurableSubscriptionNotFoundException;

  /**
   * <p>Gets list of all durable subscriptions.</p>
   *
   * @return list  The list of subscriptions
   * @throws SIResourceException
   *
   */
  public List getSubscriptionList()
    throws
    SIResourceException;

  /**
   * <p>Creates a subscription definition.</p>
   *
   * @return The subscription definition
   * @throws SIResourceException
   */
  public SubscriptionDefinition createSubscriptionDefinition()
    throws
    SIResourceException;

  /**
   * <p>Creates a destination that has at least one of the following on the
   * local me; queue point,
   * MQ queue point proxy.
   * <p>This call should be used at server startup to (re)create localised
   * destinations, or as part of dynamic configuration changes to create
   * new localised destinations or to create a new localisation of
   * an existing destination.</p>
   * <p>Note:  Message processor does not clone the definitions,
   * so once they are passed into this method, they must not be changed.</p>
   * @param destinationDefinition
   * @param destinationLocalizationDefinition
   * @param mqLocalizationProxyDefinition
   * @throws SIMPDestinationAlreadyExistsException
   * @throws SIStoreException
   * @throws SIResourceException
   * @throws SICoreException
   */
  public void createDestinationLocalization(
    DestinationDefinition destinationDefinition,
    LocalizationDefinition destinationLocalizationDefinition)
  throws SIMPDestinationAlreadyExistsException,
         SIResourceException,
         SIException,
         SIBExceptionBase;

  /**
   * Method to create a destination localization.
   * MUST ONLY BE CALLED FROM A UNIT TEST
   *
   * @param destinationDefinition
   * @param destinationLocalizationDefinition
   * @param mqLocalizationProxyDefinition
   * @param destinationLocalizingMEs
   * @param isTemporary
   * @throws SIResourceException
   * @throws SIMPDestinationAlreadyExistsException
   */
  public void createDestinationLocalization(
      DestinationDefinition destinationDefinition,
      LocalizationDefinition destinationLocalizationDefinition,
      Set destinationLocalizingMEs,
      boolean isTemporary) throws SIResourceException, SIMPDestinationAlreadyExistsException;
  /**
   * <p>Alter a destination that has at least one of the following on the
   * local me; queue point,
   * MQ queue point proxy
   * <p>This call should be used as part of dynamic configuration changes to create
   * new localised destinations or to create a new localisation of
   * an existing destination.</p>
   * <p>Note:  Message processor does not clone the definitions,
   * so once they are passed into this method, they must not be changed.</p>
   * @param destinationDefinition
   * @param destinationLocalizationDefinition
   * @param mqLocalizationProxyDefinition
   * @throws SIMPDestinationAlreadyExistsException
   * @throws SIStoreException
   * @throws SIResourceException
   * @throws SICoreException
   */
  public void alterDestinationLocalization(
      DestinationDefinition destinationDefinition,
      LocalizationDefinition destinationLocalizationDefinition
      )
    throws SINotPossibleInCurrentConfigurationException,
    SIIncorrectCallException,
    SIResourceException,
    SIConnectionLostException,
    SIException,
    SIBExceptionBase;

  /**
   * <p>Delete an entire destination localised on the local ME by
   * passing in null for the destinationDefinition, or just delete
   * the local queue point of
   * a destination by passing in the current destination definition.</p>
   * <p>This call should be used for dynamic configuration changes to delete
   * existing localised destinations, i.e. destinations that have a queue
   * point on this ME.</p>
   * <p>Note:  Message processor does not clone the destination and localization
   * definitions, so once they are passed into this method, they must not be
   * changed.</p>
   * @param destinationUuid
   * @param destinationDefinition
   *
   * @throws SINotPossibleInCurrentConfigurationException
   * @throws SIStoreException
   * @throws SIResourceException
   * @throws SICommsException
   * @throws SICoreException
   */
  public void deleteDestinationLocalization(String destinationUuid
                                            ,DestinationDefinition destinationDefinition
                                            )
  throws SINotPossibleInCurrentConfigurationException,
         SIResourceException,
         SIConnectionLostException ,
         SIException,
         SIBExceptionBase;

  /**
   * <p>Delete a SIB Link localised on the local ME. This call should be used
   * for dynamic configuration changes to delete existing localised links.</p>
   * <p>Note:  Message processor does not clone the localization
   * definitions, so once they are passed into this method, they must not be
   * changed.</p>
   * @param linkUuid
   *
   * @throws SINotPossibleInCurrentConfigurationException
   * @throws SIResourceException
   * @throws SIConnectionLostException
   * @throws SIException
   * @throws SIBExceptionBase
   */
  public void deleteGatewayLink(String linkUuid)
  throws SINotPossibleInCurrentConfigurationException,
         SIResourceException,
         SIConnectionLostException ,
         SIException,
         SIBExceptionBase;

  /**
   * <p>Alter a 'localized destination' for use by a specific instance of a SIBGatewayLink on the local messaging engine.
   *
   * @param vld The VirtualLinkDefinition for the link
   * @param uuid The UUID of the SIBGatewayLink
   * @throws <exceptions to be defined by MP>
   */
  public void alterGatewayLink(
    VirtualLinkDefinition vld,
    String uuid
  )
  throws SINotPossibleInCurrentConfigurationException,
         SIResourceException,
         SIConnectionLostException ,
         SIException,
         SIBExceptionBase;

  /**
   * <p>Delete an MQLink localised on the local ME. This call should be used
   * for dynamic configuration changes to delete existing localised links.</p>
   * <p>Note:  Message processor does not clone the localization
   * definitions, so once they are passed into this method, they must not be
   * changed.</p>
   * @param mqLinkUuid
   *
   * @throws SINotPossibleInCurrentConfigurationException
   * @throws SIResourceException
   * @throws SIConnectionLostException
   * @throws SIException
   * @throws SIBExceptionBase
   */
  public void deleteMQLink(String mqLinkUuid)
  throws SINotPossibleInCurrentConfigurationException,
         SIResourceException,
         SIConnectionLostException,
         SIException,
         SIBExceptionBase;

  /**
   * <p>Alter a 'localized destination' for use by a specific instance of a SIBMQLink on the local messaging engine.
   *
   * @param vld The VirtualLinkDefinition for the link
   * @param uuid The UUID of the SIBMQLink
   * @param ld The localization definition used by the MQLink,
   * @throws <exceptions to be defined by MP>
   */
  public void alterMQLink(
    VirtualLinkDefinition vld,
    MQLinkDefinition mqld,
    LocalizationDefinition ld
  )
  throws SINotPossibleInCurrentConfigurationException,
         SIResourceException,
         SIConnectionLostException,
         SIException,
         SIBExceptionBase;

  /**
   * Returns the LocalizationDefinition associated with the destination.
   *
   * @param      destinationUuid   obvious
   * @return     A clone of the LocalizationDefinition
   * @throws     SINotPossibleInCurrentConfigurationException
   */
  public LocalizationDefinition getDestinationLocalizationDefinition(String destinationUuid)
    throws SINotPossibleInCurrentConfigurationException;

  /**
   * <p>Redrives the clean-up thread that processes
   * the background deletes of local destinations and state
   * associated with deleted remote destinations
   * and messaging engines.</p>
   * @param destinationUuid
   */
  public void initiateCleanUp(String destinationUuid);

  /**
   * <p>Create a localization for use by a specific instance of a SIBMQLink on the local messaging engine.
   *
   * @param vld The VirtualLinkDefinition for the link
   * @param mqld The MQLinkDefinition of the SIBMQLink
   * @param ld The localization definition used by the MQLink,
   * @throws <exceptions to be defined by MP>
   */
  public void createMQLink(
    VirtualLinkDefinition vld,
    MQLinkDefinition mqld,
    LocalizationDefinition ld
  )
  throws SIMPDestinationAlreadyExistsException,
         SIResourceException,
         SIException;

  /**
   * <p>Create a localization for use by a specific instance of a SIBGatewayLink on the local messaging engine.
   *
   * @param vld The VirtualLinkDefinition for the link
   * @param uuid The UUID of the SIBGatewayLink
   * @throws <exceptions to be defined by MP>
   */
  public void createGatewayLink(
    VirtualLinkDefinition vld,
    String uuid
  ) throws SIMPDestinationAlreadyExistsException,
           SIResourceException,
           SIException;

  

  /**
   * Queries to see if a destination exists.
   *
   * @param destinationName The named destination to find
   *
   * @return true if the destination exists.
   */
  public boolean destinationExists(String destinationName);

  /**
   * Get the SIMPMessageProcessorControllable object for this MP.
   *
   * @return a SIMPMessageProcessorControllable
   */
  public SIMPMessageProcessorControllable getMPRuntimeControl();
  /*
   * Alters the alias destination type in liberty profile at runtime
   */
  public void alterDestinationAlias(DestinationAliasDefinition destinationAliasDefinition)
	    throws SINotPossibleInCurrentConfigurationException,
	    SIIncorrectCallException,
	    SIResourceException,
	    SIConnectionLostException,
	    SIException,
	    SIBExceptionBase;
}

