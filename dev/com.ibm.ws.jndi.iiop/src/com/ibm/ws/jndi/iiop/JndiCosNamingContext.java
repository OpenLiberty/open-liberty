/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jndi.iiop;

import static com.ibm.ws.jndi.WSName.EMPTY_NAME;
import static com.ibm.ws.jndi.WSNameUtil.normalize;
import static com.ibm.ws.jndi.iiop.CosNameUtil.cosify;
import static com.ibm.ws.jndi.iiop.CosNameUtil.detailed;
import static com.ibm.ws.jndi.iiop.MessageUtil.format;

import java.net.URI;
import java.util.Hashtable;

import javax.naming.Binding;
import javax.naming.CannotProceedException;
import javax.naming.ConfigurationException;
import javax.naming.Context;
import javax.naming.InvalidNameException;
import javax.naming.NameAlreadyBoundException;
import javax.naming.NameClassPair;
import javax.naming.NameNotFoundException;
import javax.naming.NameParser;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.OperationNotSupportedException;
import javax.naming.directory.SchemaViolationException;
import javax.rmi.PortableRemoteObject;

import org.omg.CORBA.LocalObject;
import org.omg.CORBA.ORB;
import org.omg.CORBA.portable.ObjectImpl;
import org.omg.CosNaming.NameComponent;
import org.omg.CosNaming.NamingContext;
import org.omg.CosNaming.NamingContextHelper;
import org.omg.CosNaming.NamingContextPackage.AlreadyBound;
import org.omg.CosNaming.NamingContextPackage.CannotProceed;
import org.omg.CosNaming.NamingContextPackage.InvalidName;
import org.omg.CosNaming.NamingContextPackage.NotFound;

import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.jndi.WSContextBase;
import com.ibm.ws.jndi.WSName;

public final class JndiCosNamingContext extends WSContextBase implements Context {
    final URI rootUrl;
    final NamingContext cosCtx;
    final WSName subname;
    final Hashtable<?,?> env;

    static String getProviderUri(Hashtable<?, ?> env) throws ConfigurationException {
        try {
            return (String) env.get(Context.PROVIDER_URL);
        } catch (ClassCastException e) {
            throw (ConfigurationException) new ConfigurationException().initCause(e);
        }
    }
    
    private static URI convertToUri(String uri) throws ConfigurationException {
        try {
            return URI.create(uri);
        } catch (Exception e) {
            throw (ConfigurationException) new ConfigurationException().initCause(e);
        }
    }

    private static NamingContext getCosContext(ORB orb, String url) {
        return NamingContextHelper.narrow(orb.string_to_object(url));
    }

    private static NamingContext duplicateRef(NamingContext cosCtx) {
        return cosCtx instanceof LocalObject ? cosCtx : 
            (NamingContext) PortableRemoteObject.narrow(cosCtx._duplicate(), NamingContext.class);
    }

    private JndiCosNamingContext(URI rootUrl, NamingContext cosCtx, WSName subname, Hashtable<?, ?> env) {
        this.rootUrl = rootUrl;
        this.cosCtx = cosCtx;
        this.subname = subname;
        this.env = new Hashtable<>(env);
    }
    
    private JndiCosNamingContext(String rootUrl, NamingContext cosCtx, WSName subname, Hashtable<?, ?> env) throws ConfigurationException {
        this(convertToUri(rootUrl), cosCtx, subname, env);
    }

    private JndiCosNamingContext(String rootUrl, ORB orb, Hashtable<?, ?> env) throws ConfigurationException {
        this(rootUrl, getCosContext(orb, rootUrl), EMPTY_NAME, env);
    }
    
    /**
     * @param env must contain a provider URL
     * @throws ConfigurationException if the provider URL is not provided or missing
     */
    public JndiCosNamingContext(ORB orb, Hashtable<?,?> env) throws ConfigurationException {
        this(getProviderUri(env), orb, env);
    }

    /** 
     * @param cosCtx This cosNamingContext will be owned by the created object
     * @throws ConfigurationException if the 
     */
    public JndiCosNamingContext(String rootUrl, NamingContext cosCtx, Hashtable<?, ?> env) throws ConfigurationException {
        this(rootUrl, cosCtx, EMPTY_NAME, env);
    }

    /** Copy constructor */
    private JndiCosNamingContext(JndiCosNamingContext that) {
        this(that.rootUrl, duplicateRef(that.cosCtx), that.subname, that.env);
    }

    /** Construct a subcontext */
    private JndiCosNamingContext(JndiCosNamingContext progenitor, NamingContext cosCtx, WSName subname) throws InvalidNameException {
        this(progenitor.rootUrl, cosCtx, progenitor.subname.plus(subname), progenitor.env);
    }

    @Override
    public String toString() {
        return rootUrl + "#" + subname;
    }

    @Override
    @FFDCIgnore({NotFound.class,CannotProceed.class, InvalidName.class})
    protected Object lookup(WSName name) throws NamingException {
        if (name.isEmpty()) {
            return new JndiCosNamingContext(this);
        }
        NameComponent[] cosName = cosify(name);
        try {
            org.omg.CORBA.Object result = cosCtx.resolve(cosName);
            if (result._is_a(NamingContextHelper.id())) {
                NamingContext cosCtx = NamingContextHelper.unchecked_narrow(result);
                return new JndiCosNamingContext(this, cosCtx, name);
            }
            return result;
        } catch (NotFound e) {
            throw CosNameUtil.detailed(new NameNotFoundException(), e, e.rest_of_name);
        } catch (CannotProceed e) {
            throw detailed(new CannotProceedException(), e, e.rest_of_name);
        } catch (InvalidName e) {
            throw detailed(new InvalidNameException(), e);
        }
    }

    @Override
    protected void bind(WSName name, Object obj) throws NamingException {
        bind(normalize(name),obj, BindOperation.BIND);
    }

    private enum BindOperation{BIND, REBIND};

    @FFDCIgnore({ClassCastException.class, NotFound.class, CannotProceed.class, InvalidName.class, AlreadyBound.class})
    private void bind(WSName name, Object obj, BindOperation bindOp) throws NamingException {
        name.ensureNotEmpty();
        NameComponent[] cosName = cosify(name);
        try {
            ObjectImpl ref = (ObjectImpl) obj;
            switch (bindOp) {
                case BIND:
                    cosCtx.bind(cosName, ref);
                    break;
                case REBIND:
                    cosCtx.rebind(cosName, ref);
            }
        } catch (ClassCastException e) {
            String msg = format("invalid.object", "Object of type {0} is not a valid remote reference.", obj.getClass().getName());
            throw detailed(new SchemaViolationException(msg), e, cosName);
        } catch (NotFound e) {
            throw CosNameUtil.detailed(new NameNotFoundException(), e, e.rest_of_name);
        } catch (CannotProceed e) {
            throw detailed(new CannotProceedException(), e, e.rest_of_name);
        } catch (InvalidName e) {
            throw detailed(new InvalidNameException(), e);
        } catch (AlreadyBound e) {
            throw detailed(new NameAlreadyBoundException(), e);
        }
    }

    @Override
    protected  void rebind(WSName name, Object obj) throws NamingException {
        bind(normalize(name), obj, BindOperation.REBIND);
    }

    @Override
    protected void unbind(WSName name) throws NamingException {
        throw new OperationNotSupportedException();
    }

    @Override
    public void rename(WSName oldName, WSName newName) throws NamingException {
        throw new OperationNotSupportedException();
    }

    @Override
    protected NamingEnumeration<NameClassPair> list(WSName name) throws NamingException {
        throw new OperationNotSupportedException();
    }

    @Override
    protected NamingEnumeration<Binding> listBindings(WSName name) throws NamingException {
        throw new OperationNotSupportedException();
    }

    @Override
    protected void destroySubcontext(WSName name) throws NamingException {
        throw new OperationNotSupportedException();
    }

    @Override
    protected Context createSubcontext(WSName name) throws NamingException {
        throw new OperationNotSupportedException();
    }

    @Override
    protected Object lookupLink(WSName name) throws NamingException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected NameParser getNameParser(WSName name) throws NamingException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object addToEnvironment(String propName, Object propVal) throws NamingException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object removeFromEnvironment(String propName) throws NamingException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Hashtable<?, ?> getEnvironment() throws NamingException {
        return new Hashtable<>(env);
    }

    @Override
    public void close() throws NamingException {
        if (cosCtx instanceof LocalObject) return;
        this.cosCtx._release();
    } 

    @Override
    public String getNameInNamespace() throws NamingException {
        throw new OperationNotSupportedException();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((rootUrl == null) ? 0 : rootUrl.hashCode());
        result = prime * result + ((subname == null) ? 0 : subname.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        JndiCosNamingContext other = (JndiCosNamingContext) obj;
        if (rootUrl == null) {
            if (other.rootUrl != null)
                return false;
        } else if (!rootUrl.equals(other.rootUrl))
            return false;
        if (subname == null) {
            if (other.subname != null)
                return false;
        } else if (!subname.equals(other.subname))
            return false;
        return true;
    }
}
