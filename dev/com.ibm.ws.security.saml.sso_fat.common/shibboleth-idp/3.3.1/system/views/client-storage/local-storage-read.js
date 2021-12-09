"use strict";
function readLocalStorage(key) {
    var success;
    try {
        var value = localStorage.getItem(key);
        if (value != null) {
            document.form1["shib_idp_ls_value." + key].value = value;
        }
        success = "true";
    } catch (e) {
        success = "false";
        document.form1["shib_idp_ls_exception." + key].value = e;
    }
    document.form1["shib_idp_ls_success." + key].value = success;
}

function isLocalStorageSupported() {
    try {
        localStorage.setItem("shib_idp_ls_test", "shib_idp_ls_test");
        localStorage.removeItem("shib_idp_ls_test");
        return true;
    } catch (e) {
        return false;
    }
}
