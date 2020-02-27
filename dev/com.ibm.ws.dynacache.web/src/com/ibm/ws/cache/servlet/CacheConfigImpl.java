/*******************************************************************************
 * Copyright (c) 1997, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cache.servlet;

import com.ibm.websphere.servlet.cache.CacheConfig;
import com.ibm.websphere.servlet.cache.ConfigElement;
import com.ibm.ws.cache.ServerCache;

public class CacheConfigImpl implements CacheConfig {
    int     timeout=0;
    int     inactivity=0; // CPF-Inactivity
    int     priority=0;
    int sharingPolicy=0;
    Class   metadatagenerator=DefaultMetaDataGeneratorImpl.class;
    Class   idgenerator=DefaultIdGeneratorImpl.class;
    protected String  metaDataGeneratorClassName = "com.ibm.ws.cache.DefaultMetaDataGeneratorImpl";
    protected String  idGeneratorClassName = "com.ibm.ws.cache.DefaultIdGeneratorImpl";
    String  uris[]=null;
    String  dataIds[]=null;
    boolean consumeSubfragments=false;
    boolean doNotConsume=false;
    
    //ref to the WebModule that originated this cache policy.
    //TODO: rework
//    protected com.ibm.websphere.models.config.applicationserver.webcontainer.WebModuleRef sourceWMRef = null;

    ConfigElement requestParameters[]=null;
    ConfigElement requestAttributes[]=null;
    ConfigElement sessionParameters[]=null;
    ConfigElement cookies[]=null;

    /*
    String  requestParameters[]=null;
    boolean  requestParameterRequired[]=null;
    String  requestAttributeIds[]=null;
    String  requestAttributeMethods[]=null;
    boolean  requestAttributeRequired[]=null;
    String  sessionParameterIds[]=null;    
    String  sessionParameterMethods[]=null;
    boolean  sessionParameterRequired[]=null; */    

    String  externalCache=null;
    
    String  name=null;
    String  servletimpl=null;
    boolean invalidateonly = false;
    


    public CacheConfigImpl() {
	// default to global policy, will be overridden if sharing is 
	// set for this servlet cache entry
	sharingPolicy = ServerCache.getSharingPolicy();

    }

    public CacheConfigImpl copy() {
       CacheConfigImpl cci= new CacheConfigImpl();
        
    cci.timeout=           this.timeout           ;
    cci.inactivity=        this.inactivity        ; // CPF-Inactivity
    cci.priority=          this.priority          ;
    cci.metadatagenerator= this.metadatagenerator ;
    cci.idgenerator=       this.idgenerator       ;
    cci.metaDataGeneratorClassName= this.metaDataGeneratorClassName ;
    cci.idGeneratorClassName      = this.idGeneratorClassName       ;
    
    if (this.uris    != null) {cci.uris = new String[this.uris.length]; System.arraycopy(this.uris   ,0,cci.uris   ,0,this.uris.length   );}
    if (this.dataIds != null) {cci.uris = new String[this.uris.length]; System.arraycopy(this.dataIds,0,cci.dataIds,0,this.dataIds.length);}
    
    
//TODO    cci.sourceWMRef =       this.sourceWMRef;

    if (this.requestParameters != null) {cci.requestParameters = new ConfigElement[this.requestParameters.length]; System.arraycopy(this.requestParameters,0,cci.requestParameters,0,this.requestParameters.length);}
    if (this.requestAttributes != null) {cci.requestAttributes = new ConfigElement[this.requestAttributes.length]; System.arraycopy(this.requestAttributes,0,cci.requestAttributes,0,this.requestAttributes.length);}
    if (this.sessionParameters != null) {cci.sessionParameters = new ConfigElement[this.sessionParameters.length]; System.arraycopy(this.sessionParameters,0,cci.sessionParameters,0,this.sessionParameters.length);}
    if (this.cookies != null) {          cci.cookies           = new ConfigElement[this.cookies.length];           System.arraycopy(this.cookies          ,0,cci.cookies          ,0,this.cookies.length)          ;}

    cci.externalCache=      this.externalCache;
    
    cci.name=               this.name           ;
    cci.servletimpl=        this.servletimpl    ;
    cci.invalidateonly =    this.invalidateonly ;
    cci.consumeSubfragments = this.consumeSubfragments ;
    cci.doNotConsume = this.doNotConsume;
    return cci;

    }


    public Class getIdGenerator() {return idgenerator;}
    public String[] getURIs() {return uris;}
    

    public ConfigElement[] getRequestParameters() {return requestParameters;}

    public ConfigElement[] getRequestAttributes() {return requestAttributes;}

    public ConfigElement[] getSessionParameters()  {return sessionParameters;}

    public ConfigElement[] getCookies()  {return cookies;}

    public boolean getInvalidateonly() {return invalidateonly;}

    public Class getMetaDataGenerator() {return metadatagenerator;}
    
    public int getSharingPolicy() {
	return this.sharingPolicy;
    }

    public int getPriority() {return priority;}
    public String getExternalCache() {return externalCache;}
    public int getTimeout() {return timeout;}
    public int getInactivity() {return inactivity;} // CPF-Inactivity
    public String getName() {return name;}
    public boolean getConsumeSubfragments() {return consumeSubfragments;}
    public boolean getDoNotConsume() {return doNotConsume;}

    //public String[] getDataIds() {return dataIds;}
    /*
    public String[] getParameters() {
       String[] ret =new String[requestParameters.length];         
       for (int i = 0;i<requestParameters.length;i++) {
          ret[i] = requestParameters[i].id;
       }      
       return ret;
    }

    public String[] getParameterDataIds() {
       String[] ret =new String[requestParameters.length];         
       for (int i = 0;i<requestParameters.length;i++) {
          ret[i] = requestParameters[i].dataId;
       }      
       return ret;
    }

    public boolean[] getParameterRequired() {
       boolean[] ret =new boolean[requestParameters.length];         
       for (int i = 0;i<requestParameters.length;i++) {
          ret[i] = requestParameters[i].required;
       }      
       return ret;
    }

    public String[] getAttributeMethods() {
       String[] ret =new String[requestAttributes.length];         
       for (int i = 0;i<requestAttributes.length;i++) {
          ret[i] = requestAttributes[i].method;
       }      
       return ret;
    } 

    public String[] getAttributeIds() {
       String[] ret =new String[requestAttributes.length];         
       for (int i = 0;i<requestAttributes.length;i++) {
          ret[i] = requestAttributes[i].id;
       }      
       return ret;
    }    

    public String[] getAttributeDataIds() {
       String[] ret =new String[requestAttributes.length];         
       for (int i = 0;i<requestAttributes.length;i++) {
          ret[i] = requestAttributes[i].dataId;
       }      
       return ret;
    }

    public boolean[] getAttributeRequired() {
       boolean[] ret =new boolean[requestAttributes.length];         
       for (int i = 0;i<requestAttributes.length;i++) {
          ret[i] = requestAttributes[i].required;
       }      
       return ret;
    }

    public String[] getSessionParameterMethods() {
       String[] ret =new String[requestAttributes.length];         
       for (int i = 0;i<sessionParameters.length;i++) {
          ret[i] = sessionParameters[i].method;
       }      
       return ret;
    } 

    public String[] getSessionParameterIds() {
       String[] ret =new String[sessionParameters.length];         
       for (int i = 0;i<sessionParameters.length;i++) {
          ret[i] = sessionParameters[i].id;
       }      
       return ret;
    }    

    public String[] getSessionParameterDataIds() {
       String[] ret =new String[sessionParameters.length];         
       for (int i = 0;i<sessionParameters.length;i++) {
          ret[i] = sessionParameters[i].dataId;
       }      
       return ret;
    }

    public boolean[] getSessionParameterRequired() {
       boolean[] ret =new boolean[sessionParameters.length];         
       for (int i = 0;i<sessionParameters.length;i++) {
          ret[i] = sessionParameters[i].required;
       }      
       return ret;
    } */

    
    
   public String toString() {
      StringBuffer sb = new StringBuffer();
      sb.append("timeout="+timeout);
      sb.append("\n");
      sb.append("priority="+priority);
      sb.append("\n");
      sb.append("consumeSubfragments="+consumeSubfragments);
      sb.append("\n");
      sb.append("doNotConsume="+doNotConsume);
      sb.append("\n");
      if (metadatagenerator != null) {
         sb.append("metadatagenerator="+metadatagenerator);
         sb.append("\n");
      }
      if (idgenerator != null) {
         sb.append("idgenerator="+idgenerator);
         sb.append("\n");
      }
      if (servletimpl!=null) {
         sb.append("servletimpl="+servletimpl);
         sb.append("\n");
      }
      for (int i=0;i<uris.length;i++) {
         sb.append("uris["+i+"]="+uris[i]);
         sb.append("\n");
      }
      if (requestParameters != null)
      for (int i=0;i<requestParameters.length;i++) {
         sb.append("requestParameters["+i+"]="+requestParameters[i]);
         sb.append("\n");
      }
      
      
      if (requestAttributes != null)
      for (int i=0;i<requestAttributes.length;i++) {
         sb.append("requestAttributes["+i+"]="+requestAttributes[i]);
         sb.append("\n");
      }   
      if (sessionParameters != null)
      for (int i=0;i<sessionParameters.length;i++) {
         sb.append("sessionParameters["+i+"]="+sessionParameters[i]);
         
         sb.append("\n");
      }
      if (cookies != null)
      for (int i=0;i<cookies.length;i++) {
         sb.append("cookies["+i+"]="+cookies[i]);
         
         sb.append("\n");
      }

      if (externalCache!=null) {
         sb.append("externalCache="+externalCache);
         sb.append("\n");
      }
      if (name!=null) {
         sb.append("name="+name);
         sb.append("\n");
      }
      if (invalidateonly) {
         sb.append("invalidateonly\n");
      }
         
      return sb.toString();
   }
}
