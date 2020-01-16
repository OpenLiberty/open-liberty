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
package jain.protocol.ip.sip;

import java.util.Iterator;

/**
 * This interface defines the methods required to represent a proprietary
 * SIP protocol stack, the implementation of which will be vendor specific.
 * Each vendor's protocol stack will have an object
 * that implements this interface to control the creation/deletion of proprietary
 * SipProviders.<P>
 * It must be noted that under the <B>JAIN Naming Convention</B>
 * the lower-level package structure and classname of
 * a proprietary implementation of the <I>jain.protocol.ip.sip.SipStack</I>
 * interface <B>must</B> be <I>jain.protocol.ip.sip.SipStackImpl</I>.<P>
 *
 *
 * Under the JAIN naming convention, the <B>upper-level package structure</B> (pathname)
 * can be used to differentiate between proprietary implementations from different IP Vendors.
 * The pathname used by each IP Vendor <B>must be</B> the domain name assigned
 * to the Vendor in reverse order, e.g. Sun Microsystem's would be 'com.sun' <P>
 
 * It follows that a proprietary implementation of a SipStack will be located
 * at: <BR><I>pathname<B>.</B>jain.protocol.ip.sip.SipStackImpl</I><BR><B>Where:</B><BR>
 * <I>pathname</I> = reverse domain name, e.g.  com.sun'<BR>
 * The resulting Peer JAIN IP Object would be located at: <I><B>com.sun</B>.jain.protocol.ip.sip.SipStackImpl</I><BR>
 *
 *
 * An application must create a SipStackImpl by invoking the
 * <a href="SipFactory.html#createSipStack()">SipFactory.createSipStack()</a>
 * method. The <b>PathName</b> of the vendor specific implementation of which you want to
 * instantiate can be set before calling this method or the default or current pathname may be used.
 * <BR>
 * <BR></BR>
 * For applications that require some means to identify multiple stacks
 * setStackName() can be used. An application can choose to supply any identifier to this method.
 * </BR><BR>
 * </BR>
 *
 * @see SipFactory
 * @see SipProvider
 *
 * @version 1.0
 *
 */
public interface SipStack
{
    
    /**
     * Creates a new Peer (vendor specific) <CODE>SipProvider</CODE>
     * that is attached to this SipStack on a specified ListeningPoint and returns a reference to it.
     * <i>Note to developers:</i> The implementation of this method should add
     * the newly created <CODE>SipProvider</CODE> to the
     * <a href="SipStack.html#getProviderList()">providerList</a> once the <CODE>SipProvider</CODE>
     * has been successfully created.
     * @return Peer JAIN Sip Provider attached to this SipStack on specified ListeningPoint.
     * @param <var>listeningPoint</var> the ListeningPoint the Provider is to be attached to
     * @throws <var>ListeningPointUnavailableException</var> thrown if the ListeningPoint specified is not
     * owned by this SipStack, or if another Provider is already using the ListeningPoint
     * @throws IllegalArgumentException if listeningPoint is null or not from same JAIN SIP implementation
     */
    public SipProvider createSipProvider(ListeningPoint listeningPoint)
                        throws IllegalArgumentException,ListeningPointUnavailableException;
    
    /**
     * Deletes the specified Peer JAIN SIP Provider attached to this SipStack.
     * <i>Note to developers:</i> The implementation of this method should remove
     * the specified Peer JAIN SIP Provider  from the <a href="#providerList">providerList</a>. <P>
     * @param <var>providerToBeDeleted</var> the Peer JAIN SIP Provider  to be deleted from this SipStack.
     * @exception <var>UnableToDeleteProviderException</var> thrown if the specified
     * Peer JAIN SIP Provider cannot be deleted. This may be because the
     * Peer JAIN SIP Provider has already been deleted, or because the Peer JAIN SIP Provider is currently in use.
     * @throws IllegalArgumentException if providerToBeDeleted is null or not from same JAIN SIP implementation
     */
    public void deleteSipProvider(SipProvider sipProvider)
                 throws UnableToDeleteProviderException,IllegalArgumentException;
    
    /**
     * Returns an Iterator of existing Peer JAIN SIP Providers that have been created by this
     * SipStackImpl. All of the Peer JAIN SIP Providers of this SipStack
     * will be proprietary objects belonging to the same stack vendor.
     * (Returns null if no SipProviders exist)
     * @return an Iterator containing all existing Peer JAIN SIP Providers created by this SipStack.
     */
    public Iterator getSipProviders();
    
    /**
     * Gets the name of this SipStack instance.
     * @return a string describing the stack instance
     */
    public String getStackName();
    
    /**
     * Sets the name of this SipStack instance. This name should be unique to this instance
     * of a vendor's implementation and optionally include a means to identify what listening points
     * the stack owns.
     * @param <var>stackName/var> the stack name.
     * @throws IllegalArgumentException if stackName is null or zero-length
     */
    public void setStackName(String stackName)
                 throws IllegalArgumentException;
    
    /**
     * Returns an Iterator of ListeningPoints available to this stack
     * (Returns null if no ListeningPoints exist)
     * @return an Iterator of ListeningPoints available to this stack
     */
    public Iterator getListeningPoints();
}
