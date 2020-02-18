var messages = {
//General
"DEPLOY_TOOL_TITLE": "Implementovat",
"SEARCH" : "Hledat",
"SEARCH_HOSTS" : "Hledat hostitele",
"EXPLORE_TOOL": "NÁSTROJ PRO PROZKOUMÁNÍ",
"EXPLORE_TOOL_INSERT": "Vyzkoušet nástroj pro prozkoumání",
"EXPLORE_TOOL_ARIA": "Vyhledat hostitele v nástroji pro prozkoumání",

//Rule Selector Panel
"RULESELECT_EDIT" : "UPRAVIT",
"RULESELECT_CHANGE_SELECTION" : "UPRAVIT VÝBĚR",
"RULESELECT_SERVER_DEFAULT" : "VÝCHOZÍ TYPY SERVERŮ",
"RULESELECT_SERVER_CUSTOM" : "VLASTNÍ TYPY",
"RULESELECT_SERVER_CUSTOM_ARIA" : "Vlastní typy serverů",
"RULESELECT_NEXT" : "DALŠÍ",
"RULESELECT_SERVER_TYPE": "Typ serveru",
"RULESELECT_SELECT_ONE": "Provést výběr",
"RULESELECT_DEPLOY_TYPE" : "Pravidlo implementace",
"RULESELECT_SERVER_SUBHEADING": "Server",
"RULESELECT_CUSTOM_PACKAGE": "Vlastní pravidlo",
"RULESELECT_RULE_DEFAULT" : "VÝCHOZÍ PRAVIDLA",
"RULESELECT_RULE_CUSTOM" : "VLASTNÍ PRAVIDLA",
"RULESELECT_FOOTER" : "Před návratem k formuláři implementace zvolte typ serveru a typ pravidla.",
"RULESELECT_CONFIRM" : "POTVRDIT",
"RULESELECT_CUSTOM_INFO": "Můžete nadefinovat vlastní pravidla s upravenými vstupy a chováním implementace.",
"RULESELECT_CUSTOM_INFO_LINK": "Další informace",
"RULESELECT_BACK": "Zpět",

//Rule Selector Aria
"RULESELECT_PANEL_ARIA" : "Panel výběru pravidel {0}", //RULESELECT_OPEN or RULESELECT_CLOSED
"RULESELECT_OPEN" : "Otevřít",
"RULESELECT_CLOSED" : "Uzavřeno",
"RULESELECT_SCROLL_UP": "Posunout nahoru",
"RULESELECT_SCROLL_DOWN": "Posunout dolů",
"RULESELECT_EDIT_SERVER_ARIA" : "Upravit typ serveru, aktuální výběr {0}", // Insert for SERVER TYPES
"RULESELECT_EDIT_RULE_ARIA" : "Upravit pravidlo, aktuální výběr {0}", //Insert for PACKAGE TYPES
"RULESELECT_NEXT_ARIA" : "Další panel",

//SERVER TYPES
"LIBERTY_SERVER" : "Server Liberty",
"NODEJS_SERVER" : "Server Node.js",

//PACKAGE TYPES
"APPLICATION_PACKAGE" : "Balík aplikace", //IMPORTANT: Ensure that the translation of this string is the same as the one for "DEPLOY_RULE_APPLICATION_PACKAGE_TYPE" in com.ibm.ws.collective.controller\resources\com\ibm\ws\collective\command\internal\resources\Messages.nlsprops
"SERVER_PACKAGE" : "Balík serveru", //IMPORTANT: Ensure that the translation of this string is the same as the one for "DEPLOY_RULE_SERVER_PACKAGE_TYPE" in com.ibm.ws.collective.controller\resources\com\ibm\ws\collective\command\internal\resources\Messages.nlsprops
"DOCKER_CONTAINER" : "Kontejner Docker", //IMPORTANT: Ensure that the translation of this string is the same as the one for "DEPLOY_RULE_DOCKER_PACKAGE_TYPE" in com.ibm.ws.collective.controller\resources\com\ibm\ws\collective\command\internal\resources\Messages.nlsprops

//Deployment parameters
"DEPLOYMENT_PARAMETERS": "Parametry implementace",
// "PARAMETERS_TOGGLE": "Toggle between upload and use a file located on the collective controller",
"GROUP_DEPLOYMENT_PARAMETERS": "Parametry implementace ({0})",
"PARAMETERS_DESCRIPTION": "Podrobnosti jsou založeny na vybraném typu serveru a šablony.",
"PARAMETERS_TOGGLE_CONTROLLER": "Použít soubor umístěný na kolektivním řadiči",
"PARAMETERS_TOGGLE_UPLOAD": "Odeslat soubor",
"SEARCH_IMAGES": "Prohledat obrazy",
"SEARCH_CLUSTERS": "Prohledat klastry",
"CLEAR_FIELD_BUTTON_ARIA": "Vymazat vstupní hodnotu",

// file browser and upload
"SERVER_BROWSE_DESCRIPTION": "Odeslat soubor balíku serveru",
"BROWSE_TITLE": "Odeslat {0}",
"STRONGLOOP_BROWSE": "Můžete přetáhnout soubor na toto místo nebo {0} soubory a zadat název souboru.", //BROWSE_INSERT
"BROWSE_INSERT" : "procházet",
"BROWSE_ARIA": "procházet soubory",
"FILE_UPLOAD_PREVIOUS" : "Použít soubor umístěný na kolektivním řadiči",
"IS_UPLOADING": "{0} se odesílá...",
"CANCEL" : "STORNO",
"UPLOAD_SUCCESSFUL" : "Balík {0} byl úspěšně odeslán!", // Package Name
"UPLOAD_FAILED" : "Odeslání se nezdařilo",
"RESET" : "resetovat",
"FILE_UPLOAD_WRITEDIRS_EMPTY_ERROR" : "Seznam adresářů k zápisu je prázdný.",
"FILE_UPLOAD_WRITEDIRS_PATH_ERROR" : "Určená cesta se musí nacházet v seznamu adresářů k zápisu.",
"PARAMETERS_FILE_ARIA" : "Parametry implementace nebo {0}", // Upload Package name

// docker
"DOCKER_MISSING_REPOSITORY_ERROR": "Musíte nakonfigurovat úložiště Docker",
"DOCKER_EMPTY_IMAGE_ERROR": "V úložišti Docker nebyly nalezeny žádné obrazy",
"DOCKER_GENERIC_ERROR": "Nebyly načteny žádné obrazy Docker. Ujistěte se, že máte nakonfigurováno úložiště Docker.",
"REFRESH": "Aktualizovat",
"REFRESH_ARIA": "Aktualizovat obrazy Docker",
"PARAMETERS_DOCKER_ARIA": "Parametry implementace nebo prohledat obrazy Docker",
"DOCKER_IMAGES_ARIA" : "Seznam obrazů Docker",
"LOCAL_IMAGE": "lokální název obrazu",
"DOCKER_INVALID_CONTAINER__NAME_ERROR" : "Název kontejneru musí být ve formátu [a-zA-Z0-9][a-zA-Z0-9_.-]*",

//Host selection
"ONE_SELECTED_HOST" : "Vybrán {0} hostitel", //quantity
"N_SELECTED_HOSTS": "Počet vybraných hostitelů: {0}", //quantity
"SELECT_HOSTS_MESSAGE": "Proveďte výběr ze seznamu dostupných hostitelů. Hostitele můžete vyhledat podle názvu nebo podle značek.",
"ONE_HOST" : "{0} výsledek", //quantity
"N_HOSTS": "Počet výsledků: {0}", //quantity
"SELECT_HOSTS_FOOTER": "Potřebujete složitější hledání? {0}", //EXPLORE_TOOL_INSERT
"NAME": "NÁZEV",
"NAME_FILTER": "Filtrovat hostitele podle názvu", // Used for aria-label
"TAG": "ZNAČKA",
"TAG_FILTER": "Filtrovat hostitele podle značky",
"ALL_HOSTS_LIST_ARIA" : "Seznam všech hostitelů",
"SELECTED_HOSTS_LIST_ARIA": "Seznam vybraných hostitelů",

//Security Details
"SECURITY_DETAILS": "Podrobnosti o zabezpečení",
"SECURITY_DETAILS_FOR_GROUP": "Podrobnosti o zabezpečení pro skupinu {0}",  // {0} is the translated group name
"SECURITY_DETAILS_MESSAGE": "Pro zabezpečení serveru jsou zapotřebí další pověření.",
"SECURITY_CREATE_PASSWORD" : "Vytvořit heslo",
"KEYSTORE_PASSWORD_MESSAGE": "Zadejte heslo pro ochranu nově vygenerovaných souborů úložiště klíčů obsahujících ověřovací pověření serveru.",
"PASSWORD_MESSAGE": "Zadejte heslo pro ochranu nově vygenerovaných souborů obsahujících ověřovací pověření serveru.",
"KEYSTORE_PASSWORD": "Heslo úložiště klíčů",
"CONFIRM_KEYSTORE_PASSWORD": "Potvrďte heslo úložiště klíčů",
"PASSWORDS_DONT_MATCH": "Hesla se neshodují",
"GROUP_GENERIC_PASSWORD": "{0} ({1})", // {0} is the display label for the initial password field
                                       // {1} is the group name if the password belongs to a group
"CONFIRM_GENERIC_PASSWORD": "Potvrdit {0}", // {0} is the display label for the initial password field
"CONFIRM_GROUP_GENERIC_PASSWORD": "Potvrdit {0} ({1})", // {0} is the display label for the initial password field
                                                       // {1} is the group name if the password belongs to a group

//Deploy
"REVIEW_AND_DEPLOY": "Zkontrolovat a implementovat",
"REVIEW_AND_DEPLOY_MESSAGE" : "Před implementací {0} všechna pole.", //REVIEW_AND_DEPLOY_INCOMPLETE_FIELDS
"REVIEW_AND_DEPLOY_INCOMPLETE_FIELDS": "je třeba vyplnit",
"READY_FOR_DEPLOYMENT": "připraveno pro implementaci.",
"READY_FOR_DEPLOYMENT_CAPS": "Připraveno pro implementaci.",
"READY_TO_DEPLOY": "Formulář je úplný. {0}", //READY_FOR_DEPLOYMENT_CAPS
"READY_TO_DEPLOY_SERVER": "Formulář je úplný. Balík serveru je {0}", //READY_FOR_DEPLOYMENT
"READY_TO_DEPLOY_DOCKER": "Formulář je úplný. Kontejner Docker je {0}", //READY_FOR_DEPLOYMENT
"DEPLOY": "IMPLEMENTOVAT",

"DEPLOY_UPLOADING" : "Počkejte na dokončení odeslání balíku serveru...",
"DEPLOY_FILE_UPLOADING" : "Dokončuje se odesílání souboru...",
"UPLOADING": "Odesílání...",
"DEPLOY_UPLOADING_MESSAGE" : "Toto okno ponechte otevřené až do zahájení procesu implementace.",
"DEPLOY_AFTER_UPLOADED_MESSAGE" : "Po dokončení odesílání balíku {0} zde můžete sledovat průběh implementace.", // Package name
"DEPLOY_FILE_UPLOAD_PERCENT" : "{0} - dokončeno {1}%", // Package name, number
"DEPLOY_WATCH_FOR_UPDATES": "Zde můžete sledovat aktualizace nebo můžete toto okno zavřít a nechat běžet proces na pozadí!",
"DEPLOY_CHECK_STATUS": "Stav implementace můžete kdykoli zkontrolovat klepnutím na ikonu Úlohy na pozadí v pravém horním rohu této obrazovky.",
"DEPLOY_IN_PROGRESS": "Probíhá implementace!",
"DEPLOY_VIEW_BG_TASKS": "Zobrazit úlohy na pozadí",
"DEPLOYMENT_PROGRESS": "Průběh implementace",
"DEPLOYING_IMAGE": "{0} na {1} hostitelů", // Package name (e.g.,  someServerName.zip, aLibertyDockerImage), number of hosts
"VIEW_DEPLOYED_SERVERS": "Zobrazit úspěšně implementované servery",
"DEPLOY_PERCENTAGE": "DOKONČENO {0}%", //quantity
"DEPLOYMENT_COMPLETE_TITLE" : "Implementace je dokončena!",
"DEPLOYMENT_COMPLETE_WITH_ERRORS_TITLE" : "Implementace je dokončena, ale vyskytly se chyby.",
"DEPLOYMENT_COMPLETE_MESSAGE" : "Můžete podrobněji prozkoumat chyby, zkontrolovat nově implementované servery nebo spustit další implementaci.",
"DEPLOYING": "Probíhá implementace...",
"DEPLOYMENT_FAILED": "Implementace se nezdařila.",
"RETURN_DEPLOY": "Vrátit se na formulář implementace a znovu odeslat",
"REUTRN_DEPLOY_HEADER": "Zkusit znovu",

//Footer
"FOOTER": "Další implementace?",
"FOOTER_BUTTON_MESSAGE" : "Spustit další implementaci",

//Error stuff
"ERROR_TITLE": "Souhrn chyb",
"ERROR_VIEW_DETAILS" : "Zobrazit podrobnosti o chybách",
"ONE_ERROR_ONE_HOST": "Došlo k jedné chybě na jednom hostiteli",
"ONE_ERROR_MULTIPLE_HOST": "Došlo k jedné chybě na více hostitelích",
"MULTIPLE_ERROR_ONE_HOST": "Došlo k více chybám na jednom hostiteli",
"MULTIPLE_ERROR_MULTIPLE_HOST": "Došlo k více chybám na více hostitelích",
"INITIALIZATION_ERROR_MESSAGE": "Nelze přistupovat k informacím o pravidlech implementace nebo o hostiteli",
"TRANSLATIONS_ERROR_MESSAGE" : "Nelze přistupovat k externalizovaným řetězcům",
"MISSING_HOST": "Vyberte ze seznamu alespoň jednoho hostitele",
"INVALID_CHARACTERS" : "Pole nemůže obsahovat speciální znaky jako např. '()$%&'",
"INVALID_DOCKER_IMAGE" : "Obraz nebyl nalezen",
"ERROR_HOSTS" : "{0} a {1} dalších hostitelů" // if there are more than three hosts reporting error, the message for errors from 6 hosts would be like:
                                      // host1.com, host2.com, host3.com and 3 others
};
