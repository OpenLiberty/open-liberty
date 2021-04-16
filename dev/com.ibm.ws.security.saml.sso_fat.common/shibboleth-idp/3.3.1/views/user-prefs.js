"use strict";

function createCookie(name, value, seconds) {
    var date = new Date();
    date.setTime(date.getTime() + (seconds * 1000));
    var expires = "; expires=" + date.toGMTString();
    
    var path = '$environment.getProperty("idp.cookie.path", $request.getContextPath())';
    if (path.length > 0)
        path = "; path=" + path;
    document.cookie = name + "=" + value + expires + path;
}

function eraseCookie(name) {
    createCookie(name, "", -31536000);
}

function readCookie(name) {
    var nameEQ = name + "=";
    var ca = document.cookie.split(';');
    for (var i = 0; i < ca.length; i++) {
        var c = ca[i];
        while (c.charAt(0) == ' ')
            c = c.substring(1, c.length);
        if (c.indexOf(nameEQ) == 0)
            return c.substring(nameEQ.length, c.length);
    }
    return null;
}

function load(id) {
    var checkbox = document.getElementById(id);
    if (checkbox != null) {
        var spnego = readCookie(checkbox.name);
        checkbox.checked = (spnego == "1");
    }
}

function check(checkbox) {
    if (checkbox.checked) {
        createCookie(checkbox.name, checkbox.value, $environment.getProperty("idp.cookie.maxAge","31536000"));
    } else {
        eraseCookie(checkbox.name);
    }
}
