package com.ibm.ws.ejbcontainer.security.internal;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class EJBSecurityConfigImplTest {

    @Test
    public void testIsStrictCredentialExpirationCheck_false() {
        Map<String, Object> cfg = new HashMap<String, Object>();
        cfg.put(EJBSecurityConfigImpl.CFG_KEY_USE_UNAUTH_FOR_EXPIRED_CREDS, Boolean.FALSE);

        EJBSecurityConfig ejbCfg = new EJBSecurityConfigImpl(cfg);
        assertFalse(ejbCfg.getUseUnauthenticatedForExpiredCredentials());
    }

    @Test
    public void testIsStrictCredentialExpirationCheck_true() {
        Map<String, Object> cfg = new HashMap<String, Object>();
        cfg.put(EJBSecurityConfigImpl.CFG_KEY_USE_UNAUTH_FOR_EXPIRED_CREDS, Boolean.TRUE);

        EJBSecurityConfig ejbCfg = new EJBSecurityConfigImpl(cfg);
        assertTrue(ejbCfg.getUseUnauthenticatedForExpiredCredentials());
    }

    @Test
    public void testIsFullyQualifyUserName_false() {
        Map<String, Object> cfg = new HashMap<String, Object>();
        cfg.put(EJBSecurityConfigImpl.CFG_KEY_REALM_QUALIFY_USER_NAME, Boolean.FALSE);

        EJBSecurityConfig ejbCfg = new EJBSecurityConfigImpl(cfg);
        assertFalse(ejbCfg.getUseRealmQualifiedUserNames());
    }

    @Test
    public void testIsFullyQualifyUserName_true() {
        Map<String, Object> cfg = new HashMap<String, Object>();
        cfg.put(EJBSecurityConfigImpl.CFG_KEY_REALM_QUALIFY_USER_NAME, Boolean.TRUE);

        EJBSecurityConfig ejbCfg = new EJBSecurityConfigImpl(cfg);
        assertTrue(ejbCfg.getUseRealmQualifiedUserNames());
    }

}
