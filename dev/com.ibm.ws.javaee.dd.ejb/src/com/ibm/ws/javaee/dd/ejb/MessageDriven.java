/*******************************************************************************
 * Copyright (c) 2012, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.dd.ejb;

/**
 * Represents &lt;message-driven>.
 */
public interface MessageDriven
                extends TimerServiceBean,
                TransactionalBean,
                MethodInterceptor
{
    /**
     * The {@link ActivationConfigProperty#getName} to represent
     * &lt;message-selector> in {@link #getActivationConfig}.
     *
     * @see org.eclipse.jst.j2ee.ejb.internal.util.MDBActivationConfigModelUtil#messageSelectorKey
     */
    String ACTIVATION_CONFIG_PROPERTY_MESSAGE_SELECTOR = "messageSelector";

    /**
     * The {@link ActivationConfigProperty#getName} to represent
     * &lt;acknowledge-mode> in {@link #getActivationConfig}.
     *
     * @see org.eclipse.jst.j2ee.ejb.internal.util.MDBActivationConfigModelUtil#ackModeKey
     */
    String ACTIVATION_CONFIG_PROPERTY_ACKNOWLEDGE_MODE = "acknowledgeMode";

    /**
     * The {@link ActivationConfigProperty#getName} to represent
     * &lt;destination-type> in {@link #getActivationConfig}.
     *
     * @see org.eclipse.jst.j2ee.ejb.internal.util.MDBActivationConfigModelUtil#destinationTypeKey
     */
    String ACTIVATION_CONFIG_PROPERTY_DESTINATION_TYPE = "destinationType";

    /**
     * The {@link ActivationConfigProperty#getValue} to represent "Queue" for
     * &lt;destination-type> in {@link #getActivationConfig}.
     *
     * @see org.eclipse.jst.j2ee.ejb.internal.util.MDBActivationConfigModelUtil#destinationTypeValues
     */
    String ACTIVATION_CONFIG_PROPERTY_DESTINATION_TYPE_QUEUE = "javax.jms.Queue";

    /**
     * The {@link ActivationConfigProperty#getValue} to represent "Topic" for
     * &lt;destination-type> in {@link #getActivationConfig}.
     *
     * @see org.eclipse.jst.j2ee.ejb.internal.util.MDBActivationConfigModelUtil#destinationTypeValues
     */
    String ACTIVATION_CONFIG_PROPERTY_DESTINATION_TYPE_TOPIC = "javax.jms.Topic";

    /**
     * The {@link ActivationConfigProperty#getName} to represent
     * &lt;subscription-durability> in {@link #getActivationConfig}.
     *
     * @see org.eclipse.jst.j2ee.ejb.internal.util.MDBActivationConfigModelUtil#durabilityKey
     */
    String ACTIVATION_CONFIG_PROPERTY_SUBSCRIPTION_DURABILITY = "subscriptionDurability";

    /**
     * @return &lt;messaging-type>, or null if unspecified
     */
    String getMessagingTypeName();

    /**
     * @return &lt;message-destination-type>, or null if unspecified
     */
    String getMessageDestinationName();

    /**
     * @return &lt;message-destination-link>, or null if unspecified
     */
    String getLink();

    /**
     * @return &lt;activation-config>, null if unspecified, an object containing
     *         the following properties if the deployment descriptor version is {@link EJBJar#VERSION_2_0} and the corresponding elements are specified,
     *         or null if all the corresponding elements are unspecified
     *         <ul>
     *         <li>{@link #ACTIVATION_CONFIG_PROPERTY_MESSAGE_SELECTOR} - &lt;message-selector>
     *         <li>{@link #ACTIVATION_CONFIG_PROPERTY_ACKNOWLEDGE_MODE} - &lt;acknowledge-mode>
     *         <li>{@link #ACTIVATION_CONFIG_PROPERTY_DESTINATION_TYPE} - &lt;message-driven-destination>&lt;destination-type>
     *         <li>{@link #ACTIVATION_CONFIG_PROPERTY_SUBSCRIPTION_DURABILITY} - &lt;message-driven-destination>&lt;subscription-durability>
     *         </ul>
     */
    ActivationConfig getActivationConfigValue();
}
