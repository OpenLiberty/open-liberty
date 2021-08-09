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

package com.ibm.websphere.sib;

/**
 * Constants denoting the reason a message was written to an Exception Destination
 *
 * @ibm-was-base
 * @ibm-api
 */
public class SIRCConstants {

  /**
   * SIRC0001_DELIVERY_ERROR.
   * <p>
   * The message could not be delivered because of a general delivery error.
   */
  public final static int SIRC0001_DELIVERY_ERROR                    = 1;

  /**
   * SIRC0002_INCORRECT_DESTINATION_USAGE_ERROR.
   * <p>
   * An application has connected to the messaging engine and requested a
   * destination type which does not match the underlying destination type.
   */
  public final static int SIRC0002_INCORRECT_DESTINATION_USAGE_ERROR = 2;

  /**
   * SIRC0003_INCORRECT_DESTINATION_USAGE_ERROR.
   * <p>
   * An application has connected to the messaging engine and requested a
   * destination type which does not match the underlying destination type.
   */
  public final static int SIRC0003_INCORRECT_DESTINATION_USAGE_ERROR = 3;

  /**
   * SIRC0004_INCORRECT_DESTINATION_USAGE_ERROR.
   * <p>
   * An application has connected to the messaging engine and requested a
   * destination type which does not match the underlying destination type.
   */
  public final static int SIRC0004_INCORRECT_DESTINATION_USAGE_ERROR = 4;

  /**
   * SIRC0005_INCORRECT_DESTINATION_USAGE_ERROR.
   * <p>
   * An application has connected to the messaging engine and requested a
   * destination type which does not match the underlying destination type.
   */
  public final static int SIRC0005_INCORRECT_DESTINATION_USAGE_ERROR = 5;

  /**
   * SIRC0006_INCORRECT_DESTINATION_USAGE_ERROR.
   * <p>
   * An application has connected to the messaging engine and requested a
   * destination type which does not match the underlying destination type.
   */
  public final static int SIRC0006_INCORRECT_DESTINATION_USAGE_ERROR = 6;

  /**
   * SIRC0007_INCORRECT_DESTINATION_USAGE_ERROR.
   * <p>
   * An application has connected to the messaging engine and requested a
   * destination type which does not match the underlying destination type.
   */
  public final static int SIRC0007_INCORRECT_DESTINATION_USAGE_ERROR = 7;

  /**
   * SIRC0008_INCORRECT_DESTINATION_USAGE_ERROR.
   * <p>
   * An application has connected to the messaging engine and requested a
   * destination type which does not match the underlying destination type.
   */
  public final static int SIRC0008_INCORRECT_DESTINATION_USAGE_ERROR = 8;

  /**
   * SIRC0009_INCORRECT_DESTINATION_USAGE_ERROR.
   * <p>
   * An application has connected to the messaging engine and requested a
   * destination type which does not match the underlying destination type.
   */
  public final static int SIRC0009_INCORRECT_DESTINATION_USAGE_ERROR = 9;

  /**
   * SIRC0010_INCORRECT_DESTINATION_USAGE_ERROR.
   * <p>
   * An application has connected to the messaging engine and requested a
   * destination type which does not match the underlying destination type.
   */
  public final static int SIRC0010_INCORRECT_DESTINATION_USAGE_ERROR = 10;

  /**
   * SIRC0011_INCORRECT_DESTINATION_USAGE_ERROR.
   * <p>
   * An application has connected to the messaging engine and requested a
   * destination type which does not match the underlying destination type.
   */
  public final static int SIRC0011_INCORRECT_DESTINATION_USAGE_ERROR = 11;

  /**
   * SIRC0012_INCORRECT_DESTINATION_USAGE_ERROR.
   * <p>
   * An application has connected to the messaging engine and requested a
   * destination type which does not match the underlying destination type.
   */
  public final static int SIRC0012_INCORRECT_DESTINATION_USAGE_ERROR = 12;

  /**
   * SIRC0013_INCORRECT_DESTINATION_USAGE_ERROR.
   * <p>
   * An application has connected to the messaging engine and requested a
   * destination type which does not match the underlying destination type.
   */
  public final static int SIRC0013_INCORRECT_DESTINATION_USAGE_ERROR = 13;

  /**
   * SIRC0014_INCORRECT_DESTINATION_USAGE_ERROR.
   * <p>
   * An application has connected to the messaging engine and requested a
   * destination type which does not match the underlying destination type.
   */
  public final static int SIRC0014_INCORRECT_DESTINATION_USAGE_ERROR = 14;

  /**
   * SIRC0015_DESTINATION_NOT_FOUND_ERROR.
   * <p>
   * The message could not be delivered due to an incorrect destination usage.
   */
  public final static int SIRC0015_DESTINATION_NOT_FOUND_ERROR = 15;

  /**
   * SIRC0016_TRANSACTION_SEND_USAGE_ERROR.
   * <p>
   * An attempt was made to use a transaction that has already been completed.
   */
  public final static int SIRC0016_TRANSACTION_SEND_USAGE_ERROR = 16;

  /**
   * SIRC0017_DESTINATION_LOCKED_ERROR.
   * <p>
   * An attempt has been made to send a message to a destination and a check has been made to ensure that this destination is send allowed.  This check failed as the destination is send disallowed.
   */
  public final static int SIRC0017_DESTINATION_LOCKED_ERROR = 17;

  /**
   * SIRC0018_USER_NOT_AUTH_SEND_ERROR.
   * <p>
   * A user is not authorized to send to the destination.
   */
  public final static int SIRC0018_USER_NOT_AUTH_SEND_ERROR = 18;

  /**
   * SIRC0019_USER_NOT_AUTH_SEND_ERROR.
   * <p>
   * A user is not authorized to send to temporary destinations with the named prefix.
   */
  public final static int SIRC0019_USER_NOT_AUTH_SEND_ERROR = 19;

  /**
   * SIRC0020_USER_NOT_AUTH_SEND_ERROR.
   * <p>
   * A user is not authorized to send to the destination using the specified topic.
   */
  public final static int SIRC0020_USER_NOT_AUTH_SEND_ERROR = 20;

  /**
   * SIRC0021_OBJECT_CLOSED_ERROR.
   * <p>
   * The Producer has been closed and cannot be used on the specified destination.
   */
  public final static int SIRC0021_OBJECT_CLOSED_ERROR = 21;

  /**
   * SIRC0022_OBJECT_CLOSED_ERROR.
   * <p>
   * The connection has been closed and cannot be used for any operation.
   */
  public final static int SIRC0022_OBJECT_CLOSED_ERROR = 22;

  /**
   * SIRC0023_EXCEPTION_DESTINATION_ERROR.
   * <p>
   * The next destination in the forward routing path could not be found so the
   * message was put to the exception destination but there was an error putting
   * the message to the exception destination.
   */
  public final static int SIRC0023_EXCEPTION_DESTINATION_ERROR = 23;

  /**
   * SIRC0024_DESTINATION_SEND_DISALLOWED.
   * <p>
   * An attempt has been made to send a message to a destination and a check has
   * been made to ensure that a queue point is available for this destination.
   * This check failed as the only queue point for this destination is send
   * disallowed.
   */
  public final static int SIRC0024_DESTINATION_SEND_DISALLOWED = 24;

  /**
   * SIRC0025_DESTINATION_HIGH_MESSAGES_ERROR.
   * <p>
   * All queues to the named destination have already reached their configured
   * high limit therefore the message cannot be accepted.
   */
  public final static int SIRC0025_DESTINATION_HIGH_MESSAGES_ERROR = 25;

  /**
   * SIRC0026_NO_LOCALISATIONS_FOUND_ERROR.
   * <p>
   * An attempt was made to send a message to a destination, but no message
   * points of that destination could be found.
   */
  public final static int SIRC0026_NO_LOCALISATIONS_FOUND_ERROR = 26;

  /**
   * SIRC0027_DESTINATION_CORRUPT_ERROR.
   * <p>
   * The destination with the given name is corrupt and cannot be used.
   */
  public final static int SIRC0027_DESTINATION_CORRUPT_ERROR = 27;

  /**
   * SIRC0028_ALIAS_TARGET_DESTINATION_NOT_FOUND_EXCEPTION.
   * <p>
   * The target destination in the alias path is unknown on the local messaging engine.
   */
  public final static int SIRC0028_ALIAS_TARGET_DESTINATION_NOT_FOUND_EXCEPTION = 28;

  /**
   * SIRC0029_ALIAS_TARGETS_SERVICE_DESTINATION_ERROR.
   * <p>
   * The alias destination is not valid because it targets a service destination
   */
  public final static int SIRC0029_ALIAS_TARGETS_SERVICE_DESTINATION_ERROR = 29;

  /**
   * SIRC0030_LINK_NOT_FOUND_ERROR.
   * <p>
   * The named link is unknown on the local messaging engine, so the attempt to
   * create the named bus failed
   */
  public final static int SIRC0030_LINK_NOT_FOUND_ERROR = 30;

  /**
   * SIRC0031_DESTINATION_ALREADY_EXISTS_ERROR.
   * <p>
   * The named destination is already known on the local messaging engine, so an
   * attempt to create one failed.
   */
  public final static int SIRC0031_DESTINATION_ALREADY_EXISTS_ERROR = 31;

  /**
   * SIRC0032_DESTINATION_DELETED_ERROR.
   * <p>
   * The destination is no longer usable because it has been deleted.
   */
  public final static int SIRC0032_DESTINATION_DELETED_ERROR = 32;

  /**
   * SIRC0033_MESSAGE_RELIABILITY_ERROR.
   * <p>
   * An attempt was made to put a message to a destination, but the message has
   * a higher reliability than the destination can support.
   */
  public final static int SIRC0033_MESSAGE_RELIABILITY_ERROR = 33;

  /**
   * SIRC0034_FORWARD_ROUTING_PATH_ERROR.
   * <p>
   * An attempt to use a service destination when the forward routing path is empty has been made.
   */
  public final static int SIRC0034_FORWARD_ROUTING_PATH_ERROR = 34;

  /**
   * SIRC0035_BACKOUT_THRESHOLD_ERROR.
   * <p>
   * A message was rolled back more times than the redelivery threshold
   */
  public final static int SIRC0035_BACKOUT_THRESHOLD_ERROR = 35;

  /**
   * SIRC0036_MESSAGE_ADMINISTRATIVELY_REROUTED_TO_EXCEPTION_DESTINATION.
   * <p>
   * The message was administratively rerouted to the exception destination
   */
  public final static int SIRC0036_MESSAGE_ADMINISTRATIVELY_REROUTED_TO_EXCEPTION_DESTINATION = 36;

  /**
   * SIRC0037_INVALID_ROUTING_PATH_ERROR.
   * <p>
   * The message could not be delivered because a forward/reverse routing path was invalid
   */
  public final static int SIRC0037_INVALID_ROUTING_PATH_ERROR = 37;

  /**
   * SIRC0038_FOREIGN_BUS_NOT_FOUND_ERROR.
   * <p>
   * The message could not be delivered because a foreign bus could not be found
   */
  public final static int SIRC0038_FOREIGN_BUS_NOT_FOUND_ERROR = 38;

  /**
   * SIRC0039_FOREIGN_BUS_NOT_FOUND_ERROR.
   * <p>
   * The message could not be delivered because a foreign bus could not be found
   */
  public final static int SIRC0039_FOREIGN_BUS_NOT_FOUND_ERROR = 39;

  /**
   * SIRC0040_FOREIGN_BUS_LINK_NOT_FOUND_ERROR.
   * <p>
   * The message could not be delivered because the foreign bus link could not be found
   */
  public final static int SIRC0040_FOREIGN_BUS_LINK_NOT_FOUND_ERROR = 40;

  /**
   * SIRC0041_FOREIGN_BUS_LINK_NOT_DEFINED_ERROR
   * <p>
   * The message could not be delivered because the foreign bus link was not defined.
   */
  public final static int SIRC0041_FOREIGN_BUS_LINK_NOT_DEFINED_ERROR = 41;

  /**
   * SIRC0042_MQ_LINK_NOT_FOUND_ERROR
   * <p>
   * The message could not be delivered because the MQ Link could not be found
   */
  public final static int SIRC0042_MQ_LINK_NOT_FOUND_ERROR = 42;

  /**
   * SIRC0043_MAX_FRP_DEPTH_EXCEEDED.
   * <p>
   * The message could not be delivered because the forward routing path max depth was exceeded
   */
  public final static int SIRC0043_MAX_FRP_DEPTH_EXCEEDED = 43;

  /**
   * SIRC0044_CANT_SEND_TO_MQ_DIRECTLY_FROM_REMOTE_BUS
   * <p>
   * The message could not be delivered because the message came from a remote bus and is going directly to MQ
   * (which we don't support as it would require the message store to support two-phase commit)
   */
  public final static int SIRC0044_CANT_SEND_TO_MQ_DIRECTLY_FROM_REMOTE_BUS = 44;

  /**
   * SIRC0101_MESSAGE_REROUTED_TO_EXCEPTION_DESTINATION_BY_MEDIATION_WARNING.
   * <p>
   * The mediation elected to route the message to the exception destination.
   */
  public final static int SIRC0101_MESSAGE_REROUTED_TO_EXCEPTION_DESTINATION_BY_MEDIATION_WARNING = 101;

  /**
   * SIRC0102_MESSAGE_NO_LONGER_CONFORMS_TO_MESSAGE_FORMAT_AFTER_MEDIATION_ERROR.
   * <p>
   * The message after the mediation had been run no longer conformed to its
   * format.
   */
  public final static int SIRC0102_MESSAGE_NO_LONGER_CONFORMS_TO_MESSAGE_FORMAT_AFTER_MEDIATION_ERROR = 102;

  /**
   * SIRC0103_MESSAGE_AFTER_MEDIATING_WAS_MALFORMED.
   * <p>
   * The message after the mediation had been run was no longer well formed.
   */
  public final static int SIRC0103_MESSAGE_AFTER_MEDIATING_WAS_MALFORMED = 103;

  /**
   * SIRC0104_MESSAGE_TOO_BIG_ERROR.
   * <p>
   * Message too big to be handled by remote MQ
   */
  public final static int SIRC0104_MESSAGE_TOO_BIG_ERROR = 104;

  /**
   * SIRC0105_MESSAGE_FORMAT_ERROR.
   * <p>
   * Message format error
   */
  public final static int SIRC0105_MESSAGE_FORMAT_ERROR = 105;
  
  /** 
   * SIRC0106_ERROR_RECEIVED_FROM_WEBSPHERE_MQ. 
   * <p> 
   * WebSphere MQ raised an error. 
   */
  public final static int SIRC0106_ERROR_RECEIVED_FROM_WEBSPHERE_MQ = 106; 

  /**
   * SIRC0200_OBJECT_FAILED_TO_SERIALIZE
   * <p>
   * The Object which was set into a JMS ObjectMessage can not be serialized.
   */
  public final static int SIRC0200_OBJECT_FAILED_TO_SERIALIZE = 200;

  /**
   * SIRC0900_INTERNAL_MESSAGING_ERROR.
   * <p>
   * The message could not be delivered due to an incorrect destination usage.
   */
  public final static int SIRC0900_INTERNAL_MESSAGING_ERROR = 900;

  /**
   * SIRC0901_INTERNAL_MESSAGING_ERROR.
   * <p>
   * The message could not be delivered due to an incorrect destination usage.
   */
  public final static int SIRC0901_INTERNAL_MESSAGING_ERROR = 901;

  /**
   * SIRC0905_INTERNAL_MESSAGING_ERROR.
   * <p>
   * The message could not be delivered due to an incorrect destination usage.
   */
  public final static int SIRC0905_INTERNAL_CONFIGURATION_ERROR = 905;
  
  /**
   * SIRC0906_SUSPECT_DELIVERY_DELAY_TIME.
   * <p>
   * The message has a delivery delay time which may be incorrect.
   */
  public final static int SIRC0906_SUSPECT_DELIVERY_DELAY_TIME = 906;

}
