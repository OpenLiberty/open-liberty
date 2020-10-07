/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package jain.protocol.ip.sip.header;

/**
 * <p>
 * This interface represents the Encryption general-header.
 * The EncryptionHeader specifies that the content has
 * been encrypted. It is intended for end-to-end encryption of
 * Requests and Responses. Requests are encrypted
 * based on the public key belonging to the entity named in the
 * ToHeader. Responses are encrypted based on the public key
 * conveyed in the ResponseKeyHeader. Note that the public keys
 * themselves may not be used for the encryption. This depends on the
 * particular algorithms used.
 * </p><p>
 * For any encrypted Message, at least the Message body and possibly
 * other Message Headers are encrypted. An application receiving a
 * Request or Response containing an EncryptionHeader decrypts
 * the body using the private key, which returns the decrypted Message with
 * decrypted body plus any decrypted Headers. Message Headers in the decrypted
 * part completely replace those with the same field name in the
 * unencrypted part. Note that the Request method and RequestURI cannot
 * be encrypted.
 * </p><p>
 * Encryption only provides privacy; the recipient has no
 * guarantee that the Request or Response came from the party
 * listed in the FromHeader, only that the sender
 * used the recipient's public key. However, proxies will not
 * be able to modify the Request or Response.
 * </p><p>
 * Since proxies can base their forwarding decision on any combination
 * of Headers, there is no guarantee that an encrypted Request
 * "hiding" Headers will reach the same destination as an
 * otherwise identical un-encrypted Request.
 * </p>
 *
 * @version 1.0
 *
 */
public interface EncryptionHeader extends SecurityHeader
{
    
    /**
     * Name of EncryptionHeader
     */
    public final static String name = "Encryption";
}
