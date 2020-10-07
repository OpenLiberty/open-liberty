/*******************************************************************************
 * Copyright (c) 1997, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cache.servlet;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.StringTokenizer;

import javax.servlet.Servlet;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpSession;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.cache.ServerCache;

import com.ibm.ws.cache.command.CommandCacheProcessor;
import com.ibm.ws.cache.config.CacheId;
import com.ibm.ws.cache.config.Component;
import com.ibm.ws.cache.config.ConfigEntry;
import com.ibm.ws.cache.config.Invalidation;
import com.ibm.ws.cache.config.Property;
import com.ibm.ws.cache.intf.DCache;
import com.ibm.ws.cache.util.SerializationUtility;
import com.ibm.ws.cache.web.config.ConfigManager;
import com.ibm.wsspi.cache.Constants;

public class FragmentCacheProcessor extends CommandCacheProcessor {
	protected static TraceComponent tc = Tr.register(FragmentCacheProcessor.class, "WebSphere Dynamic Cache", "com.ibm.ws.cache.resources.dynacache");

	public static final String PROPERTY_EDGEABLE = "edgeable";
	public static final String PROPERTY_CONSUME_SUBFRAGMENTS = "consume-subfragments";
	public static final String PROPERTY_DO_NOT_CONSUME = "do-not-consume";
	public static final String PROPERTY_EXTERNALCACHE = "externalcache";
	public static final String PROPERTY_ALTERNATE_URL = "alternate_url"; //NK2
	public static final String PROPERTY_SAVE_ATTRIBUTES = "save-attributes";
    public static final String PROPERTY_STORE_COOKIES = "store-cookies";
    public static final String PROPERTY_IGNORE_GET_POST = "ignore-get-post";
    public static final String PROPERTY_IGNORE_CHAR_ENCODING = "ignore-char-encoding";
	protected static final int SLOT_EDGEABLE = BASE_SLOTS + 0;
	protected static final int SLOT_CONSUME_SUBFRAGMENTS = BASE_SLOTS + 1;
	protected static final int SLOT_EXTERNALCACHE = BASE_SLOTS + 2;
	protected static final int SLOT_ALTERNATE_URL = BASE_SLOTS + 3; //NK2
	// next 3 slots are for supporting pathinfo in edgeable rules
	protected static final int SLOT_CONTAINS_EDGEABLE_PATHINFO = BASE_SLOTS + 4;
	protected static final int SLOT_ESI_VALUE_CACHE = BASE_SLOTS + 5;
	protected static final int SLOT_ESI_PATHINFO_VALUES = BASE_SLOTS + 6;
	//next 3 slots are for supporting servletpath in edgeable rules
	protected static final int SLOT_CONTAINS_EDGEABLE_SERVLETPATH = BASE_SLOTS + 7; //seneca
	protected static final int SLOT_ESI_SERVLETPATH_VALUES = BASE_SLOTS + 8;      //seneca

	protected static final int SLOT_SAVE_ATTRIBUTES = BASE_SLOTS + 9;
        protected static final int SLOT_STORE_COOKIES = BASE_SLOTS + 10;
	protected static final int SLOT_DO_NOT_CONSUME =  BASE_SLOTS + 11;  
	protected static final int SLOT_ATTRIBUTE_EXCLUDE_LIST =  BASE_SLOTS + 12;    //ST
	protected static final int SLOT_IGNORE_GET_POST = BASE_SLOTS + 13;
	protected static final int SLOT_COOKIE_EXCLUDE_LIST =  BASE_SLOTS + 14;    //ST
	protected static final int SLOT_CONSUME_EXCLUDE_LIST =  BASE_SLOTS + 15;
	protected static final int SLOT_IGNORE_CHAR_ENCODING =  BASE_SLOTS + 16;
	protected static final int PROCESSOR_SLOTS = BASE_SLOTS + 17; // total number of slots

	protected CacheProxyRequest request = null;
	protected CacheProxyResponse response = null;

	//a cache of ESI+ encoded Cache Policy Strings
	protected static HashMap ESICache = new HashMap();
	//a cache of ESI encoded Cache Policy Query Strings
	protected static HashMap ESIQueryCache = new HashMap();

	//vars we hold onto for the fragment info
	String externalCacheGroupId;
	boolean edgeable = false;
	String esiQueryString = null;
        String esiId = null;
	boolean consumeSubfragments = false;
	boolean doNotConsume = false;
	String baseName = null;
	String altUrl = null; //NK2
	String pathInfo = null; // pathInfo which is required is edgeable rule contains pathInfo
	String servletpath = null; //seneca
	boolean ignoreGetPost = false;
	boolean ignoreCharEnc = false;
	String[] consumeExcludeList = null;
	HashMap rrdRules = new HashMap(2);
	
	public FragmentCacheProcessor() {
	}

	//pool enabling method
	//called with ce=null when placed in pool
	public void reset(com.ibm.ws.cache.config.ConfigEntry ce) {
		super.reset(ce);
		request = null;
		response = null;
		//stored for frag/entry info
		externalCacheGroupId = null;
		consumeSubfragments = false;
		doNotConsume = false;
		edgeable = false;
		altUrl = null; //NK2
		pathInfo = null;
		servletpath = null; //seneca
		if (ce != null) {
			sharingPolicy = ce.sharingPolicy;
		}
		esiQueryString = null;
                esiId = null;
		baseName = null;
		ignoreGetPost = false;
		ignoreCharEnc = false;
		consumeExcludeList = null;
		rrdRules.clear();
	}

	//used when the configuration is loaded to validate and
	//preprocess any data
	public boolean preProcess(ConfigEntry configEntry) {
		configEntry.processorData = new Object[PROCESSOR_SLOTS];
		boolean valid = super.preProcess(configEntry);
		//edgeable
	       Property p =  (Property)configEntry.properties.get(PROPERTY_EDGEABLE); //ST
		String val = p != null ? (String)p.value : null;           //ST

		if (val != null)
			val = val.trim();
		Boolean b = new Boolean(val);
		configEntry.processorData[SLOT_EDGEABLE] = b;
		if (b.booleanValue()) {
			p =  (Property)configEntry.properties.get(PROPERTY_ALTERNATE_URL); //ST
			val = p != null ? (String)p.value : null;           //ST
			if (val != null) {
				val = val.trim();
				configEntry.processorData[SLOT_ALTERNATE_URL] = val;
			} //NK2 end
		}
		//consume-subfragments
		p =  (Property)configEntry.properties.get(PROPERTY_CONSUME_SUBFRAGMENTS);  //ST
		val = p != null ? (String)p.value : null;           
		if (val != null){
			val = val.trim();
			String excludeList[] = null;	   
			if (p != null && p.excludeList != null)  {
			    excludeList = new String[p.excludeList.length];
			    for (int i = 0; i < p.excludeList.length;i++) {			   
			       excludeList[i] = (String) p.excludeList[i];	
			     }	 					   
			}   
			configEntry.processorData[SLOT_CONSUME_EXCLUDE_LIST] = excludeList;				   
		}	
		configEntry.processorData[SLOT_CONSUME_SUBFRAGMENTS] = new Boolean(val);
		//do-not-consume											
		p =   (Property)configEntry.properties.get(PROPERTY_DO_NOT_CONSUME); //ST
		val = p != null ? (String)p.value : null;           //ST
		if (val != null)
			val = val.trim();
		configEntry.processorData[SLOT_DO_NOT_CONSUME] = new Boolean(val);
		//externalcache
		p =    (Property)configEntry.properties.get(PROPERTY_EXTERNALCACHE); //ST
		val = p != null ? (String)p.value : null;           //ST
		if (val != null) {
			val = val.trim();
			configEntry.processorData[SLOT_EXTERNALCACHE] = val;
		}
		//save-attributes
	        p =   (Property) configEntry.properties.get(PROPERTY_SAVE_ATTRIBUTES); //ST
		val = p != null ? (String)p.value : null;           //ST
		if (val != null) {
			val = val.trim();
			b = new Boolean(val);
			String excludeList[] = null;	   //ST-begin
			if (p != null && p.excludeList != null)  {
			    excludeList = new String[p.excludeList.length];
			    for (int i = 0; i < p.excludeList.length;i++) {
			       excludeList[i] = (String) p.excludeList[i];				 
			     }						   
			}     
			configEntry.processorData[SLOT_ATTRIBUTE_EXCLUDE_LIST] = excludeList;
					   //ST-end

		} else {
			b = Boolean.TRUE;
		}
		configEntry.processorData[SLOT_SAVE_ATTRIBUTES] = b;
                //store-cookies
		p =   (Property)configEntry.properties.get(PROPERTY_STORE_COOKIES);
		val = p != null ? (String)p.value : null;           //ST
		configEntry.processorData[SLOT_STORE_COOKIES] = val;
		if (val != null) {
			String excludeList[] = null;
			if (p != null && p.excludeList != null)  {
			    excludeList = new String[p.excludeList.length];
			    for (int i = 0; i < p.excludeList.length;i++) {
			       excludeList[i] = (String) p.excludeList[i];				 
			     }						   
			}     
			configEntry.processorData[SLOT_COOKIE_EXCLUDE_LIST] = excludeList;
		}
		
		//ignore-get-post
		p =  (Property)configEntry.properties.get(PROPERTY_IGNORE_GET_POST);  //ST
		val = p != null ? (String)p.value : null;           //ST
		if (val != null)
			val = val.trim();
		configEntry.processorData[SLOT_IGNORE_GET_POST] = new Boolean(val);
		
		//ignore-char-encoding
		p =  (Property)configEntry.properties.get(PROPERTY_IGNORE_CHAR_ENCODING);  //ST
		val = p != null ? (String)p.value : null;           //ST
		if (val != null)
			val = val.trim();
		configEntry.processorData[SLOT_IGNORE_CHAR_ENCODING] = new Boolean(val);
		
		for (int i = 0; i < configEntry.cacheIds.length; i++)
			valid &= preProcess(configEntry.cacheIds[i]);

		//check for edgeable pathinfo and build up eligible values for
		//esi rule cache.  we will only cache esi rules for pathinfo values
		//that are present.  not-values will require the rule to be dynamically
		//calculated
		boolean edgeablePathInfo = false;
		boolean edgeableServletPath = false; //seneca
		Boolean gb = (Boolean) configEntry.processorData[SLOT_EDGEABLE];
		boolean globalEdgeable = gb != null && gb.booleanValue();
		for (int i = 0; i < configEntry.cacheIds.length; i++) {
			CacheId ci = configEntry.cacheIds[i];
			b = (Boolean) ci.processorData[SLOT_EDGEABLE];
			if (globalEdgeable || (b != null && b.booleanValue())) {
				for (int j = 0; j < ci.components.length; j++)	{				
					if (ci.components[j].iType == Component.PATH_INFO) {
						edgeablePathInfo = true;
						HashSet hs = (HashSet) configEntry.processorData[SLOT_ESI_PATHINFO_VALUES];
						if (hs == null)
							hs = new HashSet();
						hs.addAll(ci.components[j].values.keySet());
						configEntry.processorData[SLOT_ESI_PATHINFO_VALUES] = hs;
					}
					else if (ci.components[j].iType == Component.SERVLET_PATH) {  //seneca begin
						edgeableServletPath = true;
						HashSet hs = (HashSet) configEntry.processorData[SLOT_ESI_SERVLETPATH_VALUES];
						if (hs == null)
							hs = new HashSet();
						hs.addAll(ci.components[j].values.keySet());
						configEntry.processorData[SLOT_ESI_SERVLETPATH_VALUES] = hs;
					}					 
				}	       		//seneca end

			}
		}
		configEntry.processorData[SLOT_CONTAINS_EDGEABLE_PATHINFO] = new Boolean(edgeablePathInfo);
		configEntry.processorData[SLOT_CONTAINS_EDGEABLE_SERVLETPATH] = new Boolean(edgeableServletPath); //seneca
		if (edgeablePathInfo || edgeableServletPath) //seneca
			configEntry.processorData[SLOT_ESI_VALUE_CACHE] = new HashMap();

		return valid;
	}

	//used when the configuration is loaded to validate and
	//preprocess any data
	public boolean preProcess(CacheId cacheId) {
		cacheId.processorData = new Object[PROCESSOR_SLOTS];
		boolean valid = super.preProcess(cacheId);
		if (cacheId.properties != null) {
			//edgeable
			Property p =  (Property)cacheId.properties.get(PROPERTY_EDGEABLE); //ST
		      	String val = p != null ? (String)p.value : null;           //ST
			if (val != null) {
				val = val.trim();
				Boolean b = new Boolean(val);
				cacheId.processorData[SLOT_EDGEABLE] = b;
				if (b.booleanValue()) {
				       p =   (Property)cacheId.properties.get(PROPERTY_ALTERNATE_URL);//ST
					val = p != null ? (String)p.value : null;           //ST
					if (val != null) {
						val = val.trim();
						cacheId.processorData[SLOT_ALTERNATE_URL] = val;
					}
				} //NK2 end

			}
			//consume-subfragments
			if (cacheId.processorData[SLOT_CONSUME_SUBFRAGMENTS] == null || ((Boolean) cacheId.processorData[SLOT_CONSUME_SUBFRAGMENTS]).booleanValue() == false) {
				p =  (Property) cacheId.properties.get(PROPERTY_CONSUME_SUBFRAGMENTS); //ST
				val = p != null ? (String)p.value : null;           //ST
				if (val != null) {
					val = val.trim();
					String excludeList[] = null;	   
					if (p != null && p.excludeList != null)  {
					    excludeList = new String[p.excludeList.length];
					    for (int i = 0; i < p.excludeList.length;i++) {			   
					       excludeList[i] = (String) p.excludeList[i];	
					     }	 					   
					}   
					cacheId.processorData[SLOT_CONSUME_EXCLUDE_LIST] = excludeList;
					cacheId.processorData[SLOT_CONSUME_SUBFRAGMENTS] = new Boolean(val);
				}       			
			}
			//do-not-consume												  
			if (cacheId.processorData[SLOT_DO_NOT_CONSUME] == null || ((Boolean) cacheId.processorData[SLOT_DO_NOT_CONSUME]).booleanValue() == false) {
				p = (Property)cacheId.properties.get(PROPERTY_DO_NOT_CONSUME); //ST
				val = p != null ? (String)p.value : null;           //ST
				if (val != null) {
					val = val.trim();
					cacheId.processorData[SLOT_DO_NOT_CONSUME] = new Boolean(val);
				}
			}
			//externalcache
			p =  (Property)cacheId.properties.get(PROPERTY_EXTERNALCACHE);
			val = p != null ? (String)p.value : null;           //ST
			if (val != null) {
				val = val.trim();
				cacheId.processorData[SLOT_EXTERNALCACHE] = val;
			}
			
			//ignore-get-post
			if (cacheId.processorData[SLOT_IGNORE_GET_POST] == null || ((Boolean) cacheId.processorData[SLOT_IGNORE_GET_POST]).booleanValue() == false) {
				p =  (Property) cacheId.properties.get(PROPERTY_IGNORE_GET_POST); //ST
				val = p != null ? (String)p.value : null;           //ST
				if (val != null) {
					val = val.trim();
					cacheId.processorData[SLOT_IGNORE_GET_POST] = new Boolean(val);
				}
			}
			
			//ignore-char-encoding
			if (cacheId.processorData[SLOT_IGNORE_CHAR_ENCODING] == null || ((Boolean) cacheId.processorData[SLOT_IGNORE_CHAR_ENCODING]).booleanValue() == false) {
				p =  (Property) cacheId.properties.get(PROPERTY_IGNORE_CHAR_ENCODING); 
				val = p != null ? (String)p.value : null;           
				if (val != null) {
					val = val.trim();
					cacheId.processorData[SLOT_IGNORE_CHAR_ENCODING] = new Boolean(val);
				}
			}
			
			
		}
		return valid;
	}

	// Methods call after successfull cache id match to make use
	// of any custom properties that might have been set

	public void processCacheIdProperties(CacheId cacheid) {
		super.processCacheIdProperties(cacheid);
		if (cacheid.processorData[SLOT_EDGEABLE] != null) {
			edgeable = ((Boolean) cacheid.processorData[SLOT_EDGEABLE]).booleanValue();
		}
		if (cacheid.processorData[SLOT_CONSUME_SUBFRAGMENTS] != null) {
			consumeSubfragments = ((Boolean) cacheid.processorData[SLOT_CONSUME_SUBFRAGMENTS]).booleanValue();
		}
		if (cacheid.processorData[SLOT_EXTERNALCACHE] != null) {
			externalCacheGroupId = (String) cacheid.processorData[SLOT_EXTERNALCACHE];
		}
		if (cacheid.processorData[SLOT_ALTERNATE_URL] != null) { //NK2 begin
			altUrl = (String) cacheid.processorData[SLOT_ALTERNATE_URL];
		} //NK2 end
		if (cacheid.processorData[SLOT_DO_NOT_CONSUME] != null) {
			doNotConsume = ((Boolean) cacheid.processorData[SLOT_DO_NOT_CONSUME]).booleanValue();
		}
		if (cacheid.processorData[SLOT_IGNORE_GET_POST] != null) {
			ignoreGetPost = ((Boolean) cacheid.processorData[SLOT_IGNORE_GET_POST]).booleanValue();
		}
		
		if (cacheid.processorData[SLOT_IGNORE_CHAR_ENCODING] != null) {
			ignoreCharEnc = ((Boolean) cacheid.processorData[SLOT_IGNORE_CHAR_ENCODING]).booleanValue();
		}
		
		if (cacheid.processorData[SLOT_CONSUME_EXCLUDE_LIST] != null) {
			consumeExcludeList = (String[])cacheid.processorData[SLOT_CONSUME_EXCLUDE_LIST];
		}
	}

	public void processConfigEntryProperties() {
		super.processConfigEntryProperties();
		if (configEntry.processorData[SLOT_EDGEABLE] != null) {
			edgeable = ((Boolean) configEntry.processorData[SLOT_EDGEABLE]).booleanValue();
		}
		if (configEntry.processorData[SLOT_CONSUME_SUBFRAGMENTS] != null) {
			consumeSubfragments = ((Boolean) configEntry.processorData[SLOT_CONSUME_SUBFRAGMENTS]).booleanValue();
		}
		if (configEntry.processorData[SLOT_EXTERNALCACHE] != null) {
			externalCacheGroupId = (String) configEntry.processorData[SLOT_EXTERNALCACHE];
		}
		if (configEntry.processorData[SLOT_ALTERNATE_URL] != null) { //NK2 begin
			altUrl = (String) configEntry.processorData[SLOT_ALTERNATE_URL];
		} //NK2 end
		if (configEntry.processorData[SLOT_DO_NOT_CONSUME] != null) {			
			doNotConsume = ((Boolean) configEntry.processorData[SLOT_DO_NOT_CONSUME]).booleanValue();
		}
		if (configEntry.processorData[SLOT_IGNORE_GET_POST] != null) {
			ignoreGetPost = ((Boolean) configEntry.processorData[SLOT_IGNORE_GET_POST]).booleanValue();
		}
		
		if (configEntry.processorData[SLOT_IGNORE_CHAR_ENCODING] != null) {
			ignoreCharEnc = ((Boolean) configEntry.processorData[SLOT_IGNORE_CHAR_ENCODING]).booleanValue();
		}
		
		if (configEntry.processorData[SLOT_CONSUME_EXCLUDE_LIST] != null) {
			consumeExcludeList = (String[])configEntry.processorData[SLOT_CONSUME_EXCLUDE_LIST];
		}
	}

	// returns the basename for the cache id
	public String getBaseName() {

		if(baseName == null)
		    baseName = configEntry.name;
		
		if (tc.isDebugEnabled()){
			Tr.debug(tc, "getBaseName: "+configEntry.name);
		}
		
		if (configEntry.name.contains(ConfigManager.DEFAULT_EXTENSION_PROCESSOR_IMPL)){
			String changedBaseName = String.copyValueOf(configEntry.name.toCharArray());
			baseName = changedBaseName.replace(ConfigManager.DEFAULT_EXTENSION_PROCESSOR_IMPL, ConfigManager.SIMPLE_FILE_SERVLET); 			
		}
		if (tc.isDebugEnabled()){
			Tr.debug(tc, "getBaseName returns: "+baseName);	
		}
				
		return baseName;
	}

	/**
	  * This is called after the execute() method, and populates the entryInfo
	  * with the calculated values.  take care of special properties here, which
	  * must have a corresponding place on the fragmentinfo object
	  *
	  * @param fi The FragmentInfo object to be populated
	  */
	public void populateFragmentInfo(FragmentInfo fi) {
		fi.setId(getId());
		int portletMethod = request.getPortletMethod();

		fi.addTemplate(getBaseName());
		fi.setInstanceName(configEntry.instanceName); // MSI
		fi.setSkipCacheAttribute(configEntry.skipCacheAttribute);
		if (!cacheable)
			return;

		boolean storeAttrib = ((Boolean) configEntry.processorData[SLOT_SAVE_ATTRIBUTES]).booleanValue(); // ST
		fi.setStoreAttributes(storeAttrib);
		String excludeList[] = (String[]) configEntry.processorData[SLOT_ATTRIBUTE_EXCLUDE_LIST]; // ST
		fi.setAttributeExcludeList(excludeList); // ST

		String storeCookies = (String) configEntry.processorData[SLOT_STORE_COOKIES];
		if (storeCookies != null) {
			fi.setStoreCookies((new Boolean(storeCookies.trim())).booleanValue());
			fi.setCookieExcludeList((String[]) configEntry.processorData[SLOT_COOKIE_EXCLUDE_LIST]);
		} else {
			DCache cache = ServerCache.getCache(configEntry.instanceName);
			if (cache != null)
				fi.setStoreCookies(cache.getCacheConfig()
						.isCacheInstanceStoreCookies());
			else
				fi.setStoreCookies(true);
		}

		fi.setSharingPolicy(sharingPolicy);
		fi.setPersistToDisk(persistToDisk); // @memOnly
		fi.setTimeLimit(timeout);
		fi.setInactivity(inactivity); // CPF-Inactivity
		fi.setIgnoreGetPost(ignoreGetPost);
		fi.setIgnoreCharEnc(ignoreCharEnc);

		fi.setDoNotCache(doNotCache);
		if (priority > 0)
			fi.setPriority(priority);

		if (groupIds != null) {
			for (int i = 0; i < groupIds.size(); i++)
				fi.addDataId((String) groupIds.get(i));
		}
		if (edgeable) {
			String cid = getESICacheId(configEntry);
			if (cid != null) {
				fi.setEdgeable(true);
				fi.setESICacheId(cid);
				fi.setESIQueryString(esiQueryString);
			}
			if (altUrl != null) { // NK2 begin
				fi.setAlternateUrl(altUrl);
			} // NK2 end
		}

		fi.setConsumeSubfragments(consumeSubfragments);
		fi.setConsumeExcludeList(consumeExcludeList);
		fi.setDoNotConsume(doNotConsume);
		if (externalCacheGroupId != null)
			fi.setExternalCacheGroupId(externalCacheGroupId);

		if (this.cacheIdForMetaDataGenerator != null) {
			CacheId cacheid = (CacheId) this.cacheIdForMetaDataGenerator.clone();
			if (cacheIdForMetaDataGenerator.idGeneratorImpl != null )
				cacheid.idGeneratorImpl = cacheIdForMetaDataGenerator.idGeneratorImpl;
			processMetaDataGenerator(cacheid);
			this.cacheIdForMetaDataGenerator = cacheid;
		}
	}

	public boolean execute(CacheProxyRequest request, CacheProxyResponse response, Servlet servlet) {
		if (tc.isEntryEnabled())
			Tr.entry(tc, "execute: " + configEntry.name);
		this.request = request;
		this.response = response;

		super.execute();
		if (cacheable) {
			//insert mandatory extra info here, like character encoding
			String encoding = request.getCharacterEncoding();
			if (encoding != null && !ignoreCharEnc) {
				id.append(':').append(encoding);
			} else {
				if (ignoreCharEnc){
					Tr.debug(tc,"ignore-char-encoding SET. Char Encoding will be ignored whilc creating cacheids");
				}
			}
			
			String requestType = request.getMethod(); 
			
			if (ignoreGetPost){
				if (!(requestType.equals("GET") || requestType.equals("POST"))){
					if (tc.isDebugEnabled())
						Tr.debug(tc, "Request type is not GET or POST, value of property ignore-get-post will be set to false for this request.");
					ignoreGetPost = false;
				}
				else{
					if (tc.isDebugEnabled())
						Tr.debug(tc, "ignore-get-post property is set to true, will not differentiate between GET and POST requests types unless defined explicitly in the cache policy.");
				}
			}
			
			if(((id.toString()).indexOf("requestType="+requestType) == -1) && (requestType.equals("GET") || requestType.equals("POST")) &&  !ignoreGetPost){ //NK
			    id.append(':').append("requestType="+requestType);			    			    			 			
			}
		}
		
		//LI3251-13
		String sc = ESISupport.getHeaderDirect(request,"Surrogate-Capability");
		if (sc != null && sc.indexOf("ESI/0.8") != -1){
		    if (tc.isDebugEnabled())
			Tr.debug(tc, "esiVersion: ESI/0.8, must set edgeable to true");
		    edgeable = true;
		    configEntry.processorData[SLOT_EDGEABLE] = new Boolean(true);
		    //The rrdRules are added to the hashmap in getComponentValue() if locale/requestType are part of the cache policy.
		    request.setAttribute(Constants.IBM_DYNACACHE_RRD_ESI, rrdRules.clone());
		}
      
		if (edgeable) {
			Boolean b = (Boolean) configEntry.processorData[SLOT_CONTAINS_EDGEABLE_PATHINFO];
			if (b.booleanValue()) {
				String dispatch_type = (String) request
						.getAttribute("com.ibm.servlet.engine.webapp.dispatch_type");
				if (dispatch_type != null && dispatch_type.equals("include"))
					pathInfo = (String) request
							.getAttribute("javax.servlet.include.path_info");
				else
					pathInfo = request.getPathInfo();
			}
			Boolean s = (Boolean) configEntry.processorData[SLOT_CONTAINS_EDGEABLE_SERVLETPATH]; // seneca
																									// begin
			if (s.booleanValue()) {
				String dispatch_type = (String) request
						.getAttribute("com.ibm.servlet.engine.webapp.dispatch_type");
				if (dispatch_type != null && dispatch_type.equals("include"))
					servletpath = (String) request
							.getAttribute("javax.servlet.include.servlet_path");
				else
					servletpath = request.getServletPath();
			} // seneca end
		}
		if (tc.isEntryEnabled())
			Tr.exit(tc, "execute");
		return cacheable;
	}

	protected boolean processCacheId(CacheId cacheid) {
		boolean success = super.processCacheId(cacheid);
		if (success)
			esiQueryString = getESIQueryString(cacheid);
		return success;
	}

	//this method deals with default values for any components, since the cache spec reader must remain generic
	protected Object getComponentValue(Component c) {
		if (c == null) {
			if (tc.isDebugEnabled())
				Tr.debug(tc, "FragmentCacheProcessor.getComponentValue(): null component passed in, returning null.");
			return null;
		}
		Object result = null;
		switch (c.iType) {
			case Component.PARAMETER : //parameter
				if (!c.id.equals("*")){
					return request.getParameter(c.id);
				}
				if (!response.getFragmentComposer().getInclude())
					result = request.getQueryString();
				else
					result = (String) request.getAttribute("javax.servlet.include.query_string");
				break;
			case Component.ATTRIBUTE : //attribute
				result = request.getAttribute(c.id);
				if (c.index > -1) {
					if (result instanceof Collection) {
						result = ((Collection) result).toArray();
					}
					if (result instanceof Object[]) {
						if (((Object[]) result).length > c.index)
							result = ((Object[]) result)[c.index];
					}
				}
				break;
			case Component.SESSION : //session
				HttpSession s = request.getSession(false);
				if (s != null) {
					result = s.getAttribute(c.id);
				}
				break;
			case Component.COOKIE : //cookie
				Cookie[] cookies = request.getCookies();
				if (cookies != null) {
					for (int i = 0; result == null && i < cookies.length; i++) {
						if (cookies[i].getName().equals(c.id)) {
							result = cookies[i].getValue();
						}
					}
				}
				break;
			case Component.HEADER : //header
				return request.getHeader(c.id);
			case Component.LOCALE : //locale
				result = request.getLocale();
				rrdRules.put(Constants.IBM_DYNACACHE_RRD_LOCALE,result);
				break;
			case Component.PATH_INFO : //Path Info
				if (!response.getFragmentComposer().getInclude())
					result = request.getPathInfo();
				else
					result = (String) request.getAttribute("javax.servlet.include.path_info");
				break;
			case Component.SERVLET_PATH : //Servlet Path
				if (!response.getFragmentComposer().getInclude())
					result = request.getServletPath();
				else
					result = (String) request.getAttribute("javax.servlet.include.servlet_path");
				break;
			case Component.PARAMETER_LIST :
				String[] pv = request.getParameterValues(c.id);
				if (pv != null) {
					java.util.Arrays.sort(pv);
					StringBuffer sb = new StringBuffer();
					for (int i = 0; i < pv.length; i++) {
						sb.append(pv[i]);
						if (i != pv.length - 1)
							sb.append(',');
					}
					result = sb.toString();
				}
				break;
			case Component.REQUEST_TYPE : 
				result = request.getMethod(); 
				rrdRules.put(Constants.IBM_DYNACACHE_RRD_REQUEST_TYPE,result);
				break;
			case Component.TILES_ATTRIBUTE:	 //ST-begin
			       try{
			          //Class cmpCtxClass = Class.forName("org.apache.struts.tiles.ComponentContext");		 
			          Object tilesContext = request.getAttribute("org.apache.struts.taglib.tiles.CompContext");	
			          Method method = tilesContext.getClass().getMethod("getAttribute", new Class[] {java.lang.String.class});
			          result = method.invoke(tilesContext, new Object[] {c.id});			      
				}
				catch(Exception ex){				    
			    	    System.out.println("Warning: struts.jar is not in the application or server classpath. Hence component type 'tiles_attribute' is not supported.");
				    return null;
				}
			        //ComponentContext tilesContext = (ComponentContext)request.getAttribute("org.apache.struts.taglib.tiles.CompContext");			 
			        //if(tilesContext != null)
			        //result = tilesContext.getAttribute(c.id);
				break;				 
			case Component.SESSION_ID:
				HttpSession session = request.getSession(false);
				if (session != null) {
					result = session.getId();
				}
				break;
			default :
				Tr.error(tc, "DYNA0040E", new Object[] { c.type, request.getContextPath() + request.getServletPath()});
				return null;
		}

		//let processComponent take care of the case where the object can't be found...could be not required, so no error
		if (result == null) {
			return null;
		}

		//check for existing methods and fields to evaluate on this root object.
		//If none are present, add the default method/field to the cache id.
		if (c.method != null)
			result = processMethod(c.method, result);
		else if (c.field != null)
			result = processField(c.field, result);
		return result;
	}

	protected String processIdGenerator(CacheId cacheid) {
		if (cacheid.idGeneratorImpl == null) {
			try {
				Class c = Class.forName(cacheid.idGenerator, true, SerializationUtility.getContextClassLoader());
				cacheid.idGeneratorImpl = c.newInstance();
			} catch (Exception ex) {
				com.ibm.ws.ffdc.FFDCFilter.processException(ex, "com.ibm.ws.cache.config.FragmentCacheProcessor.processIdGenerator", "291", this);
				//ex.printStackTrace();
				Tr.error(tc, "dynacache.idgeneratorerror", new Object[] { cacheid.idGenerator });
				return null;
			}
		}
		return ((com.ibm.websphere.servlet.cache.IdGenerator) (cacheid.idGeneratorImpl)).getId(request);
	}

	protected void processMetaDataGenerator(CacheId cacheid) {
		if (cacheid.metaDataGeneratorImpl == null) {
			try {
				Class c = Class.forName(cacheid.metaDataGenerator, true, SerializationUtility.getContextClassLoader());
				cacheid.metaDataGeneratorImpl = c.newInstance();
			} catch (Exception ex) {
				com.ibm.ws.ffdc.FFDCFilter.processException(ex, "com.ibm.ws.cache.config.FragmentCacheProcessor.processMetaDataGenerator", "306", this);
				//ex.printStackTrace();
				Tr.error(tc, "dynacache.metadatageneratorerror", new Object[] { cacheid.metaDataGenerator });
				return;
			}
		}
		((com.ibm.websphere.servlet.cache.MetaDataGenerator) (cacheid.metaDataGeneratorImpl)).setMetaData(request, response);
	}

 	protected String[] processInvalidationGenerator(Invalidation invalidation) {  
		if (tc.isDebugEnabled()) {
			Tr.debug(tc, "Servlets do not support custom invalidation generating classes.");
		}
		return null;
	}

	// Builds the ESI cache id string for the Surrogate-control header
	// after building it, it is stored in our private data slot
	public String getESICacheId(ConfigEntry ce) {
		if (esiId != null) {
			return esiId;
		}
		HashMap hm = (HashMap) ce.processorData[SLOT_ESI_VALUE_CACHE];
		if (hm != null) {
			esiId = (String) hm.get(pathInfo);
			if (esiId != null) {
				return esiId;
			}
			esiId = (String)hm.get(servletpath);   //seneca begin
			if(esiId != null){			    
			    return esiId;
			}			 	       //seneca end

		}

		StringBuffer sb = new StringBuffer(128);
		//build ESI compatible cache id
		if (ce.cacheIds != null && ce.cacheIds.length > 0) {
			for (int i = 0; i < ce.cacheIds.length; i++) {
			    CacheId cid = ce.cacheIds[i];
			    if( (cid.processorData[SLOT_EDGEABLE] != null &&((Boolean) cid.processorData[SLOT_EDGEABLE]).booleanValue()) ||
			          (ce.processorData[SLOT_EDGEABLE] != null && ((Boolean) ce.processorData[SLOT_EDGEABLE]).booleanValue()) )
				getESIRule(ce.cacheIds[i], sb, ce.name);
			}
		}
		if (sb.length() == 0) {
			//rule is empty
			return null;
		}
		sb.insert(0, "cacheid=\"");
		sb.append("\"");
		esiId = sb.toString();
		// if not edgeable pathinfo, cache the esi id
		if (((Boolean) ce.processorData[SLOT_CONTAINS_EDGEABLE_PATHINFO]).booleanValue()){
		    if (((HashSet) ce.processorData[SLOT_ESI_PATHINFO_VALUES]).contains(pathInfo)) {
			// if the pathInfo is in the eligible values list, then cache the esi rule, otherwise
			// we will need to recompute dynamically... this is to prevent the cache from growing
			// without bound
			hm.put(pathInfo, esiId);
		    }
		}

		if (((Boolean) ce.processorData[SLOT_CONTAINS_EDGEABLE_SERVLETPATH]).booleanValue()){  //seneca begin
		    if (((HashSet) ce.processorData[SLOT_ESI_SERVLETPATH_VALUES]).contains(servletpath)) {
			// if the pathInfo is in the eligible values list, then cache the esi rule, otherwise
			// we will need to recompute dynamically... this is to prevent the cache from growing
			// without bound
			hm.put(servletpath, esiId);						     //seneca end
		    }
		}

		return esiId;
	}

	 public String getESIQueryString(CacheId ci) {
		if (esiQueryString != null) {
			return esiQueryString;
		}
		//int index = request.getAbsoluteUri().indexOf("?");	
		String absoluteUri = request.getAbsoluteUri();	
		int index = -1;
		if (absoluteUri != null)
		    index = absoluteUri.indexOf("?");	   

		String parmStr = null;		
		StringTokenizer parmToks = null;   
		StringBuffer sb = null; 
		String parm = null;  
		String parmName = null; 
		boolean addParm; 
		//build ESI compatible query string
		if (ci.components != null)
			for (int i = 0; i < ci.components.length; i++) {
				Component c = ci.components[i];
				addParm = false;				
				if (c.type.equals("parameter")) {
				  if(index > -1){
				    parmStr = request.getAbsoluteUri().substring(index+1); 				    
				    parmToks = new StringTokenizer(parmStr, "&");
				  }
				  
				  if(parmToks != null)
				    while(parmToks.hasMoreTokens()) {
					parm = parmToks.nextToken(); 					
					parmName = parm.substring(0, parm.indexOf("="));	 
					if(parmName.trim().equals((c.id).trim())) {				 
					    addParm = true;					 
					    break;						 
					} 
				    }
				    if (sb == null) {
						sb = new StringBuffer(32 * ci.components.length);
						sb.append("?");
				    } else {
						sb.append("&");
				      }	
				    
				    if(addParm){
					sb.append(parm);
				    }
				    else 
					sb.append(c.id).append("=").append("$(QUERY_STRING{" + c.id + "})");					    				   
				}
				else if (c.type.equals("cookie")) { //AI1 begin
				  
				  if (sb == null) {
				    sb = new StringBuffer(32 * ci.components.length);
					sb.append("?");
				  } else {
					sb.append("&");
				  }	
				  sb.append(c.id).append("=").append("$(HTTP_COOKIE{" + c.id + "})");					    				   
			      } //AI1 end
			}
		if (sb != null)
			esiQueryString = sb.toString();
		else
			esiQueryString = "";	
		return esiQueryString;
	}

	public void getESIRule(CacheId ci, StringBuffer sb, String identifier) {
		//go through the (edge appropriate) components and derive the modifiers and ids needed
		StringBuffer parms = null;
		StringBuffer cookies = null;
		StringBuffer headers = null;
		for (int i = 0; ci.components != null && i < ci.components.length; i++) {
			Component c = ci.components[i];
			switch (c.iType) {
				case Component.PARAMETER :
					if (parms == null)
						parms = new StringBuffer();
					else
						parms.append(" ");
					c.getESIComponent(parms);
					break;
				case Component.HEADER :
					if (headers == null)
						headers = new StringBuffer();
					else
						headers.append(" ");
					c.getESIComponent(headers);
					break;
				case Component.COOKIE :
					if (cookies == null)
						cookies = new StringBuffer();
					else
						cookies.append(" ");
					c.getESIComponent(cookies);
					break;
				case Component.PATH_INFO :
					// if we have values and current pathinfo is not present then this
					// rule is not valid for the current pathinfo
					if (c.values != null && c.values.size() != 0 && !c.values.containsKey(pathInfo))
						return;
					//if we have not-values and current pathinfo is present then this
					// rule is not valid for the current pathinfo
					if (c.notValues != null && c.notValues.size() != 0 &&  c.notValues.containsKey(pathInfo)) {
						return;
					}
					break;
				case Component.SERVLET_PATH :
					   // if we have values and current pathinfo is not present then this
					   // rule is not valid for the current pathinfo				        
					if (c.values != null && c.values.size() != 0 && !c.values.containsKey(servletpath)){									       
						return;
					 }
					//if we have not-values and current pathinfo is present then this
					// rule is not valid for the current pathinfo
					if (c.notValues != null && c.notValues.size() !=0 && c.values.containsKey(servletpath)) {
						return;
					}
					break;	                                       //seneca end
				case Component.LOCALE :
					//LI3251-13
					String s1 = ESISupport.getHeaderDirect(request,"Surrogate-Capability");
					if (s1!= null && s1.indexOf("ESI/0.8") == -1){
						Tr.error(tc, "DYNA0041E", new Object[] { c.type, identifier });
						return;
					}	
					break;
				case Component.REQUEST_TYPE :
					//LI3251-13
					String s2 = ESISupport.getHeaderDirect(request,"Surrogate-Capability");
					if (s2!= null && s2.indexOf("ESI/0.8") == -1){
						Tr.error(tc, "DYNA0041E", new Object[] { c.type, identifier });
						return;
					}	
					break;
				default :
					Tr.error(tc, "DYNA0041E", new Object[] { c.type, identifier });
					return;
			}
		}
		sb.append("(");
		if (parms != null) {
			sb.append(parms.toString());
		}
		if (cookies == null && headers == null) {
			sb.append(")").toString();
			return;
		}
		sb.append(",");
		if (cookies != null)
			sb.append(cookies.toString());

		if (headers != null) {
			sb.append(",").append(headers.toString());
		}
		sb.append(")");
	}

}
