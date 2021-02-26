Just a quick note on the test class naming.
How we logged in affects how logout behaves.  Because of this, we have a large number of combinations to test. 

Issues considered when structuring the tests:
- IDP Initiated and Solicited SP Initiated logins use the same configurations, but Unsolicited SP Initiated login needs a 
tweak to the config (that's what differentiates Solicited and Unsolicited) 

- For logouts, httpServletRequest and ibm_security_logout behavior (staying local or going remote (to the IDP)) is controlled by the spLogout 
config attribute.

- The tests can run with SP Cookies or LTPA and this is controlled by config settings

- ibm_security_logout builds the logout url based on the app context root.  We typically run with multiple servlets off the same app.  This won't work
with ibm_security_logout - we need multiple apps (basically one for each SP that we use to test)

With all of that in mind, we have the following naming:

Basic logout tests:  (arranged to run tests for one login/logout combination)
<Login Type>_<Logout Type>(_spLogout<True|False>>_{if needed})(_LTPA{if using LTPA})_Tests.java
	ie:  IDPInitiatedLogin_ibmSecurityLogout_spLogoutFalse_LTPA_Tests.java 
				IDP initiated Login, ibm_security_logout (local only), using LTPA
		 SolicitedSPInitiatedLogin_servletRequestLogout_spLogoutFalse_Tests.java
		 		Solicited SP initiated login, httpServletRequest Logout (local only), SP Cookies
		 UnsolicitedSPInitiatedLogin_IDPInitiated_LogoutUrl_Tests.java
		 		 UnSolicited SP initiated login, IDP Initiated Logouty, SP Cookies
		 
2 Server Tests: (arranged to run 2 server tests for as many combinations that use a config)
<Login Type>_2ServerLogout_<usingApps|usingServlets>_Tests.java
	ie:  IDPInitiatedLogin_2ServerLogout_usingApps_Tests.java
			IDP initiated Login, ibm_security_logout (since that requires unique apps)
		 SolicitedSPInitiatedLogin_2ServerLogout_usingServlets_Tests.java
		 	Solicited SP Initiated Login, IDP Initiated and httpServletRequest logouts (since the can use servlets)
		 UnsolicitedSPInitiatedLogin_2ServerLogout_usingApps_Tests.java
		 	UnSolicited SP initiated Login, ibm_security_logout (since that requires unique apps)
		
IDP Time out tests: (arranged to run test for as many combinations that use a config - with Shibboleth that has a short lifetime)
		These tests have to tweak the Shibboleth config to have a unique timeout, so, we can't 
		combine with other tests out of a fear of unexpected/unplanned timeouts
<Login Type>_IDPSessionTimeout_<usingApps|usingServlets>_Tests.java
	ie:  IDPInitiatedLogin_IDPSessionTimeout_usingApps_Tests.java
			IDP initiated Login, ibm_security_logout (since that requires unique apps)
		 SolicitedSPInitiatedLogin_IDPSessionTimeout_usingServlets_Tests.java
		 	Solicited SP Initiated Login, IDP Initiated and httpServletRequest logouts (since the can use servlets)
		 UnsolicitedSPInitiatedLogin_IDPSessionTimeout_usingApps_Tests.java
		 	UnSolicited SP initiated Login, ibm_security_logout (since that requires unique apps)
