var messages = {
//General
"DEPLOY_TOOL_TITLE": "Telepítés",
"SEARCH" : "Keresés",
"SEARCH_HOSTS" : "Hosztok keresése",
"EXPLORE_TOOL": "BÖNGÉSZŐ ESZKÖZ",
"EXPLORE_TOOL_INSERT": "Próbálja ki a Böngésző eszközt",
"EXPLORE_TOOL_ARIA": "Hosztok keresése a Böngésző eszközben új lapon",

//Rule Selector Panel
"RULESELECT_EDIT" : "SZERKESZTÉS",
"RULESELECT_CHANGE_SELECTION" : "KIJELÖLÉS SZERKESZTÉSE",
"RULESELECT_SERVER_DEFAULT" : "ALAPÉRTELMEZETT KISZOLGÁLÓTÍPUSOK",
"RULESELECT_SERVER_CUSTOM" : "EGYÉNI TÍPUSOK",
"RULESELECT_SERVER_CUSTOM_ARIA" : "Egyéni kiszolgálótípusok",
"RULESELECT_NEXT" : "TOVÁBB",
"RULESELECT_SERVER_TYPE": "Kiszolgálótípus",
"RULESELECT_SELECT_ONE": "Válasszon ki egyet",
"RULESELECT_DEPLOY_TYPE" : "Szabály rendszerbe állítása",
"RULESELECT_SERVER_SUBHEADING": "Kiszolgáló",
"RULESELECT_CUSTOM_PACKAGE": "Egyéni csomag",
"RULESELECT_RULE_DEFAULT" : "ALAPÉRTELMEZETT SZABÁLYOK",
"RULESELECT_RULE_CUSTOM" : "EGYÉNI SZABÁLYOK",
"RULESELECT_FOOTER" : "Válasszon kiszolgálótípust és szabálytípust a Telepítés űrlapra történő visszatérés előtt.",
"RULESELECT_CONFIRM" : "MEGERŐSÍTÉS",
"RULESELECT_CUSTOM_INFO": "Meghatározhat saját szabályokat egyéni bemenetekkel és telepítési viselkedéssel.",
"RULESELECT_CUSTOM_INFO_LINK": "További információk",
"RULESELECT_BACK": "Vissza",

//Rule Selector Aria
"RULESELECT_PANEL_ARIA" : "Szabályválasztási panel {0}", //RULESELECT_OPEN or RULESELECT_CLOSED
"RULESELECT_OPEN" : "Megnyitás",
"RULESELECT_CLOSED" : "Bezárt",
"RULESELECT_SCROLL_UP": "Görgetés felfelé",
"RULESELECT_SCROLL_DOWN": "Görgetés lefelé",
"RULESELECT_EDIT_SERVER_ARIA" : "Kiszolgálótípus szerkesztése; az aktuális beállítás: {0}", // Insert for SERVER TYPES
"RULESELECT_EDIT_RULE_ARIA" : "Szabály szerkesztése; az aktuális beállítás: {0}", //Insert for PACKAGE TYPES
"RULESELECT_NEXT_ARIA" : "Következő panel",

//SERVER TYPES
"LIBERTY_SERVER" : "Liberty kiszolgáló",
"NODEJS_SERVER" : "Node.js kiszolgáló",

//PACKAGE TYPES
"APPLICATION_PACKAGE" : "Alkalmazáscsomag", //IMPORTANT: Ensure that the translation of this string is the same as the one for "DEPLOY_RULE_APPLICATION_PACKAGE_TYPE" in com.ibm.ws.collective.controller\resources\com\ibm\ws\collective\command\internal\resources\Messages.nlsprops
"SERVER_PACKAGE" : "Kiszolgálócsomag", //IMPORTANT: Ensure that the translation of this string is the same as the one for "DEPLOY_RULE_SERVER_PACKAGE_TYPE" in com.ibm.ws.collective.controller\resources\com\ibm\ws\collective\command\internal\resources\Messages.nlsprops
"DOCKER_CONTAINER" : "Docker tároló", //IMPORTANT: Ensure that the translation of this string is the same as the one for "DEPLOY_RULE_DOCKER_PACKAGE_TYPE" in com.ibm.ws.collective.controller\resources\com\ibm\ws\collective\command\internal\resources\Messages.nlsprops

//Deployment parameters
"DEPLOYMENT_PARAMETERS": "Telepítési paraméterek",
// "PARAMETERS_TOGGLE": "Toggle between upload and use a file located on the collective controller",
"GROUP_DEPLOYMENT_PARAMETERS": "Telepítési paraméterek ({0})",
"PARAMETERS_DESCRIPTION": "A részletek a kiválasztott kiszolgálón és sablontípuson alapulnak.",
"PARAMETERS_TOGGLE_CONTROLLER": "A kollektív vezérlőn található fájl használata",
"PARAMETERS_TOGGLE_UPLOAD": "Fájl feltöltése",
"SEARCH_IMAGES": "Képfájlok keresése",
"SEARCH_CLUSTERS": "Fürtök keresése",
"CLEAR_FIELD_BUTTON_ARIA": "Bevitt érték törlése",

// file browser and upload
"SERVER_BROWSE_DESCRIPTION": "Kiszolgáló csomagfájl feltöltése",
"BROWSE_TITLE": "{0} feltöltése",
"STRONGLOOP_BROWSE": "Húzza a fájlt ide vagy válassza a {0} lehetőséget a fájlnév megadására", //BROWSE_INSERT
"BROWSE_INSERT" : "tallózás",
"BROWSE_ARIA": "fájlok tallózása",
"FILE_UPLOAD_PREVIOUS" : "A kollektív vezérlőn található fájl használata",
"IS_UPLOADING": "{0} feltöltése folyamatban...",
"CANCEL" : "MÉGSE",
"UPLOAD_SUCCESSFUL" : "{0} sikeresen feltöltve!", // Package Name
"UPLOAD_FAILED" : "A feltöltés nem sikerült",
"RESET" : "alaphelyzetbe állítás",
"FILE_UPLOAD_WRITEDIRS_EMPTY_ERROR" : "Az írási könyvtárlista üres.",
"FILE_UPLOAD_WRITEDIRS_PATH_ERROR" : "A megadott útvonalnak szerepelnie kell az írási könyvtárlistában.",
"PARAMETERS_FILE_ARIA" : "Telepítési paraméterek vagy {0}", // Upload Package name

// docker
"DOCKER_MISSING_REPOSITORY_ERROR": "Be kell állítania egy Docker lerakatot",
"DOCKER_EMPTY_IMAGE_ERROR": "A beállított Docker lerakatban nem találhatók képfájlok",
"DOCKER_GENERIC_ERROR": "Nincs betöltve Docker képfájl. Győződjön meg róla, hogy van beállított Docker lerakat.",
"REFRESH": "Frissítés",
"REFRESH_ARIA": "Docker képfájlok frissítése",
"PARAMETERS_DOCKER_ARIA": "Telepítési paraméterek vagy Docker képek keresése",
"DOCKER_IMAGES_ARIA" : "Docker képek listája",
"LOCAL_IMAGE": "helyi kép neve",
"DOCKER_INVALID_CONTAINER__NAME_ERROR" : "A tárolónévnek a következő formátumnak kell megfelelnie: [a-zA-Z0-9][a-zA-Z0-9_.-]*",

//Host selection
"ONE_SELECTED_HOST" : "{0} kijelölt hoszt", //quantity
"N_SELECTED_HOSTS": "{0} kijelölt hoszt", //quantity
"SELECT_HOSTS_MESSAGE": "Válasszon az elérhető hosztok listájából. Kereshet hosztokra név vagy címke/címék alapján.",
"ONE_HOST" : "{0} találat", //quantity
"N_HOSTS": "{0} találat", //quantity
"SELECT_HOSTS_FOOTER": "Összetettebb keresésre van szüksége? {0}", //EXPLORE_TOOL_INSERT
"NAME": "NÉV",
"NAME_FILTER": "Hosztok szűrése név szerint", // Used for aria-label
"TAG": "CÍMKE",
"TAG_FILTER": "Hosztok szűrése címke szerint",
"ALL_HOSTS_LIST_ARIA" : "Minden hoszt listája",
"SELECTED_HOSTS_LIST_ARIA": "Kiválasztott hosztok listája",

//Security Details
"SECURITY_DETAILS": "Biztonsági részletek",
"SECURITY_DETAILS_FOR_GROUP": "{0} biztonsági részletei",  // {0} is the translated group name
"SECURITY_DETAILS_MESSAGE": "A kiszolgáló védelméhez szükséges további hitelesítési adatok.",
"SECURITY_CREATE_PASSWORD" : "Jelszó létrehozása",
"KEYSTORE_PASSWORD_MESSAGE": "A kiszolgáló hitelesítési adatokat tartalmazó, újonnan előállított kulcstárolófájlok védelmére szolgáló jelszó megadása.",
"PASSWORD_MESSAGE": "A kiszolgáló hitelesítési adatokat tartalmazó, újonnan előállított fájlok védelmére szolgáló jelszó megadása.",
"KEYSTORE_PASSWORD": "Kulcstároló jelszó",
"CONFIRM_KEYSTORE_PASSWORD": "Kulcstároló jelszó megerősítése",
"PASSWORDS_DONT_MATCH": "A jelszavak nem egyeznek",
"GROUP_GENERIC_PASSWORD": "{0} ({1})", // {0} is the display label for the initial password field
                                       // {1} is the group name if the password belongs to a group
"CONFIRM_GENERIC_PASSWORD": "{0} megerősítése", // {0} is the display label for the initial password field
"CONFIRM_GROUP_GENERIC_PASSWORD": "{0}  megerősítése ({1})", // {0} is the display label for the initial password field
                                                       // {1} is the group name if the password belongs to a group

//Deploy
"REVIEW_AND_DEPLOY": "Áttekintés és telepítés",
"REVIEW_AND_DEPLOY_MESSAGE" : "Minden {0} mezőt a telepítés előtt.", //REVIEW_AND_DEPLOY_INCOMPLETE_FIELDS
"REVIEW_AND_DEPLOY_INCOMPLETE_FIELDS": "ki kell tölteni",
"READY_FOR_DEPLOYMENT": "telepítésre kész.",
"READY_FOR_DEPLOYMENT_CAPS": "Telepítésre kész.",
"READY_TO_DEPLOY": "Az űrlap kész. {0}", //READY_FOR_DEPLOYMENT_CAPS
"READY_TO_DEPLOY_SERVER": "Az űrlap kész. A kiszolgálócsomag: {0}", //READY_FOR_DEPLOYMENT
"READY_TO_DEPLOY_DOCKER": "Az űrlap kész. A Docker tároló: {0}", //READY_FOR_DEPLOYMENT
"DEPLOY": "TELEPÍTÉS",

"DEPLOY_UPLOADING" : "Engedélyezze a kiszolgálócsomag számára a feltöltés befejezését...",
"DEPLOY_FILE_UPLOADING" : "Fájlfeltöltés befejezése...",
"UPLOADING": "Feltöltés...",
"DEPLOY_UPLOADING_MESSAGE" : "A telepítési folyamat megkezdéséig tartsa nyitva ezt az ablakot.",
"DEPLOY_AFTER_UPLOADED_MESSAGE" : "A(z) {0} feltöltésének befejeződése után itt figyelheti a telepítés előrehaladását.", // Package name
"DEPLOY_FILE_UPLOAD_PERCENT" : "{0} - {1}% kész", // Package name, number
"DEPLOY_WATCH_FOR_UPDATES": "Nézze meg a frissítéseket itt, vagy zárja be ezt az ablakot és hagyja futni a háttérben!",
"DEPLOY_CHECK_STATUS": "A telepítés állapotát bármikor ellenőrizheti a képernyő jobb felső sarkában található Háttérfeladatok ikonra kattintva.",
"DEPLOY_IN_PROGRESS": "A telepítés folyamatban van!",
"DEPLOY_VIEW_BG_TASKS": "Háttérben futó feladatok megtekintése",
"DEPLOYMENT_PROGRESS": "Telepítési folyamat",
"DEPLOYING_IMAGE": "{0} - {1} hoszt", // Package name (e.g.,  someServerName.zip, aLibertyDockerImage), number of hosts
"VIEW_DEPLOYED_SERVERS": "Sikeresen telepített kiszolgálók megjelenítése",
"DEPLOY_PERCENTAGE": "{0}% KÉSZ", //quantity
"DEPLOYMENT_COMPLETE_TITLE" : "A telepítés befejeződött!",
"DEPLOYMENT_COMPLETE_WITH_ERRORS_TITLE" : "A telepítés befejeződött, de hibák adódtak.",
"DEPLOYMENT_COMPLETE_MESSAGE" : "Megvizsgálhatja részletesebben a hibákat, ellenőrizheti az újonnan telepített kiszolgálókat vagy indíthat másik telepítést.",
"DEPLOYING": "Telepítés...",
"DEPLOYMENT_FAILED": "A telepítés nem sikerült.",
"RETURN_DEPLOY": "Menjen vissza a telepítési űrlapra és küldje el újra",
"REUTRN_DEPLOY_HEADER": "Próbálkozzon újra",

//Footer
"FOOTER": "További telepítendők?",
"FOOTER_BUTTON_MESSAGE" : "Másik telepítés indítása",

//Error stuff
"ERROR_TITLE": "Hibák összegzése",
"ERROR_VIEW_DETAILS" : "Hibarészletek megjelenítése",
"ONE_ERROR_ONE_HOST": "Egy hiba történt egy hoszton",
"ONE_ERROR_MULTIPLE_HOST": "Egy hiba történt több hoszton",
"MULTIPLE_ERROR_ONE_HOST": "Több hiba történt egy hoszton",
"MULTIPLE_ERROR_MULTIPLE_HOST": "Több hiba történt több hoszton",
"INITIALIZATION_ERROR_MESSAGE": "Nem érhető el a hoszt vagy a telepítési szabályok adatai a kiszolgálón",
"TRANSLATIONS_ERROR_MESSAGE" : "A külsőleg elérhetővé tett karaktersorozatok nem érhetők el",
"MISSING_HOST": "Válasszon ki legalább egy hosztot a listából",
"INVALID_CHARACTERS" : "A mező nem tartalmazhat speciális karaktereket, mint például '()$%&'",
"INVALID_DOCKER_IMAGE" : "A képfájl nem található",
"ERROR_HOSTS" : "{0} és {1} másik" // if there are more than three hosts reporting error, the message for errors from 6 hosts would be like:
                                      // host1.com, host2.com, host3.com and 3 others
};
