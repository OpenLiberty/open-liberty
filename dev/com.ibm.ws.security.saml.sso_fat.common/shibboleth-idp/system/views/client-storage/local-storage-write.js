"use strict";
function writeLocalStorage(key, value) {
    var success;
    try {
    	if (value == null || value.length == 0) {
    		localStorage.removeItem(key);
    	} else {
    		localStorage.setItem(key, value);
    	}
        success = "true";
    } catch (e) {
        success = "false";
        document.form1["shib_idp_ls_exception." + key].value = e;
    }
    document.form1["shib_idp_ls_success." + key].value = success;
}