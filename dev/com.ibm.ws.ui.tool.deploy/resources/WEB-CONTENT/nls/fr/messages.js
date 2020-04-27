var messages = {
//General
"DEPLOY_TOOL_TITLE": "Déployer",
"SEARCH" : "Recherche",
"SEARCH_HOSTS" : "Rechercher des hôtes",
"EXPLORE_TOOL": "OUTIL D'EXPLORATION",
"EXPLORE_TOOL_INSERT": "Essayez l'outil d'exploration.",
"EXPLORE_TOOL_ARIA": "Rechercher les hôtes dans l'outil d'exploration dans un nouvel onglet",

//Rule Selector Panel
"RULESELECT_EDIT" : "EDITER",
"RULESELECT_CHANGE_SELECTION" : "EDITER LA SELECTION",
"RULESELECT_SERVER_DEFAULT" : "TYPES DE SERVEUR PAR DEFAUT",
"RULESELECT_SERVER_CUSTOM" : "TYPES PERSONNALISES",
"RULESELECT_SERVER_CUSTOM_ARIA" : "Types de serveur personnalisés",
"RULESELECT_NEXT" : "SUIVANT",
"RULESELECT_SERVER_TYPE": "Type de serveur",
"RULESELECT_SELECT_ONE": "Sélectionnez une réponse",
"RULESELECT_DEPLOY_TYPE" : "Règle de déploiement",
"RULESELECT_SERVER_SUBHEADING": "Serveur",
"RULESELECT_CUSTOM_PACKAGE": "Package personnalisé",
"RULESELECT_RULE_DEFAULT" : "REGLES PAR DEFAUT",
"RULESELECT_RULE_CUSTOM" : "REGLES PERSONNALISEES",
"RULESELECT_FOOTER" : "Choisissez un type de serveur et un type de règle avant de revenir au formulaire de déploiement.",
"RULESELECT_CONFIRM" : "CONFIRMER",
"RULESELECT_CUSTOM_INFO": "Vous pouvez définir vos propres règles avec des entrées et un comportement de déploiement personnalisés.",
"RULESELECT_CUSTOM_INFO_LINK": "En savoir plus",
"RULESELECT_BACK": "Arrière",

//Rule Selector Aria
"RULESELECT_PANEL_ARIA" : "Panneau de sélection de règle {0}", //RULESELECT_OPEN or RULESELECT_CLOSED
"RULESELECT_OPEN" : "Ouvert",
"RULESELECT_CLOSED" : "Fermé",
"RULESELECT_SCROLL_UP": "Défilement vers le haut",
"RULESELECT_SCROLL_DOWN": "Défilement vers le bas",
"RULESELECT_EDIT_SERVER_ARIA" : "Editez le type de serveur ; sélection en cours : {0}", // Insert for SERVER TYPES
"RULESELECT_EDIT_RULE_ARIA" : "Editez la règle ; sélection en cours : {0}", //Insert for PACKAGE TYPES
"RULESELECT_NEXT_ARIA" : "Panneau suivant",

//SERVER TYPES
"LIBERTY_SERVER" : "Liberty Server",
"NODEJS_SERVER" : "Serveur Node.js",

//PACKAGE TYPES
"APPLICATION_PACKAGE" : "Package d'application", //IMPORTANT: Ensure that the translation of this string is the same as the one for "DEPLOY_RULE_APPLICATION_PACKAGE_TYPE" in com.ibm.ws.collective.controller\resources\com\ibm\ws\collective\command\internal\resources\Messages.nlsprops
"SERVER_PACKAGE" : "Package serveur", //IMPORTANT: Ensure that the translation of this string is the same as the one for "DEPLOY_RULE_SERVER_PACKAGE_TYPE" in com.ibm.ws.collective.controller\resources\com\ibm\ws\collective\command\internal\resources\Messages.nlsprops
"DOCKER_CONTAINER" : "Conteneur Docker", //IMPORTANT: Ensure that the translation of this string is the same as the one for "DEPLOY_RULE_DOCKER_PACKAGE_TYPE" in com.ibm.ws.collective.controller\resources\com\ibm\ws\collective\command\internal\resources\Messages.nlsprops

//Deployment parameters
"DEPLOYMENT_PARAMETERS": "Paramètres de déploiement",
// "PARAMETERS_TOGGLE": "Toggle between upload and use a file located on the collective controller",
"GROUP_DEPLOYMENT_PARAMETERS": "Paramètres de déploiement ({0})",
"PARAMETERS_DESCRIPTION": "Les détails dépendent du type de serveur et du type de modèle sélectionnés.",
"PARAMETERS_TOGGLE_CONTROLLER": "Utiliser un fichier situé sur le contrôleur de collectivité",
"PARAMETERS_TOGGLE_UPLOAD": "Envoyer par téléchargement un fichier",
"SEARCH_IMAGES": "Rechercher des images",
"SEARCH_CLUSTERS": "Rechercher des clusters",
"CLEAR_FIELD_BUTTON_ARIA": "Effacer la valeur d'entrée",

// file browser and upload
"SERVER_BROWSE_DESCRIPTION": "Envoyer par téléchargement le fichier de package serveur",
"BROWSE_TITLE": "Envoyer par téléchargement {0}",
"STRONGLOOP_BROWSE": "Faites glisser ici un fichier ou {0} les fichiers pour fournir le nom de fichier", //BROWSE_INSERT
"BROWSE_INSERT" : "parcourez",
"BROWSE_ARIA": "parcourir les fichiers",
"FILE_UPLOAD_PREVIOUS" : "Utiliser un fichier situé sur le contrôleur de collectivité",
"IS_UPLOADING": "L'envoi par téléchargement de {0} est en cours...",
"CANCEL" : "ANNULER",
"UPLOAD_SUCCESSFUL" : "L'envoi par téléchargement de {0} a abouti.", // Package Name
"UPLOAD_FAILED" : "Echec de l'envoi par téléchargement",
"RESET" : "réinitialiser",
"FILE_UPLOAD_WRITEDIRS_EMPTY_ERROR" : "La liste de répertoires accessibles en écriture est vide.",
"FILE_UPLOAD_WRITEDIRS_PATH_ERROR" : "Le chemin spécifié doit figurer dans la liste des répertoires accessibles en écriture.",
"PARAMETERS_FILE_ARIA" : "Paramètres de déploiement ou {0}", // Upload Package name

// docker
"DOCKER_MISSING_REPOSITORY_ERROR": "Vous devez configurer un référentiel Docker",
"DOCKER_EMPTY_IMAGE_ERROR": "Aucune image n'a été trouvée dans le référentiel Docker configuré",
"DOCKER_GENERIC_ERROR": "Aucune image Docker chargée. Assurez-vous d'avoir configuré un référentiel Docker.",
"REFRESH": "Actualiser",
"REFRESH_ARIA": "Actualiser les images Docker",
"PARAMETERS_DOCKER_ARIA": "Paramètres de déploiement ou recherche d'images Docker",
"DOCKER_IMAGES_ARIA" : "Liste d'images Docker",
"LOCAL_IMAGE": "nom de l'image locale",
"DOCKER_INVALID_CONTAINER__NAME_ERROR" : "Le nom du conteneur doit correspondre au format [a-zA-Z0-9][a-zA-Z0-9_.-]*",

//Host selection
"ONE_SELECTED_HOST" : "{0} hôte sélectionné", //quantity
"N_SELECTED_HOSTS": "{0} hôtes sélectionnés", //quantity
"SELECT_HOSTS_MESSAGE": "Effectuez une sélection dans la liste des hôtes disponibles. Vous pouvez rechercher des hôtes d'après leur nom ou leurs étiquettes.",
"ONE_HOST" : "{0} résultat", //quantity
"N_HOSTS": "{0} résultats", //quantity
"SELECT_HOSTS_FOOTER": "Vous avez besoin d'effectuer une recherche plus complexe ? {0}", //EXPLORE_TOOL_INSERT
"NAME": "NOM",
"NAME_FILTER": "Filtrer les hôtes par nom", // Used for aria-label
"TAG": "ETIQUETTE",
"TAG_FILTER": "Filtrer les hôtes par étiquette",
"ALL_HOSTS_LIST_ARIA" : "Liste de tous les hôtes",
"SELECTED_HOSTS_LIST_ARIA": "Liste des hôtes sélectionnés",

//Security Details
"SECURITY_DETAILS": "Informations sur la sécurité",
"SECURITY_DETAILS_FOR_GROUP": "Informations sur la sécurité pour {0}",  // {0} is the translated group name
"SECURITY_DETAILS_MESSAGE": "Des données d'identification supplémentaires sont requises pour la sécurité du serveur.",
"SECURITY_CREATE_PASSWORD" : "Créer un mot de passe",
"KEYSTORE_PASSWORD_MESSAGE": "Indiquez un mot de passe pour protéger les fichiers de clés nouvellement générés et contenant les données d'identification d'authentification serveur.",
"PASSWORD_MESSAGE": "Spécifiez un mot de passe pour protéger les fichiers générés qui comportent des données d'identification pour l'authentification auprès du serveur.",
"KEYSTORE_PASSWORD": "Mot de passe du fichier de clés",
"CONFIRM_KEYSTORE_PASSWORD": "Confirmation du mot de passe de fichier de clés",
"PASSWORDS_DONT_MATCH": "Les mots de passe ne concordent pas.",
"GROUP_GENERIC_PASSWORD": "{0} ({1})", // {0} is the display label for the initial password field
                                       // {1} is the group name if the password belongs to a group
"CONFIRM_GENERIC_PASSWORD": "Confirmer {0}", // {0} is the display label for the initial password field
"CONFIRM_GROUP_GENERIC_PASSWORD": "Confirmer {0} ({1})", // {0} is the display label for the initial password field
                                                       // {1} is the group name if the password belongs to a group

//Deploy
"REVIEW_AND_DEPLOY": "Revue et déploiement",
"REVIEW_AND_DEPLOY_MESSAGE" : "Toutes les zones {0} avant le déploiement.", //REVIEW_AND_DEPLOY_INCOMPLETE_FIELDS
"REVIEW_AND_DEPLOY_INCOMPLETE_FIELDS": "doivent être remplies",
"READY_FOR_DEPLOYMENT": "prêt pour le déploiement.",
"READY_FOR_DEPLOYMENT_CAPS": "Prêt pour le déploiement.",
"READY_TO_DEPLOY": "Le formulaire est complet. {0}", //READY_FOR_DEPLOYMENT_CAPS
"READY_TO_DEPLOY_SERVER": "Le formulaire est complet. Le package serveur est {0}", //READY_FOR_DEPLOYMENT
"READY_TO_DEPLOY_DOCKER": "Le formulaire est complet. Le conteneur Docker est {0}", //READY_FOR_DEPLOYMENT
"DEPLOY": "DEPLOYER",

"DEPLOY_UPLOADING" : "Patientez jusqu'à ce que l'envoi par téléchargement du package serveur soit terminé...",
"DEPLOY_FILE_UPLOADING" : "Finalisation de l'envoi par téléchargement du fichier...",
"UPLOADING": "Envoi par téléchargement en cours...",
"DEPLOY_UPLOADING_MESSAGE" : "Gardez cette fenêtre ouverte jusqu'au lancement du processus de déploiement.",
"DEPLOY_AFTER_UPLOADED_MESSAGE" : "Une fois l'envoi par téléchargement de {0} terminé, vous pourrez suivre ici la progression de votre déploiement.", // Package name
"DEPLOY_FILE_UPLOAD_PERCENT" : "{0} - {1} % terminé(s)", // Package name, number
"DEPLOY_WATCH_FOR_UPDATES": "Vous pouvez rechercher des mises à jour ici, ou fermer cette fenêtre et laisser le processus s'exécuter en arrière-plan.",
"DEPLOY_CHECK_STATUS": "Vous pouvez consulter le statut de votre déploiement à tout moment en cliquant sur l'icône Tâches dans le coin supérieur droit de cet écran.",
"DEPLOY_IN_PROGRESS": "Votre déploiement est en cours.",
"DEPLOY_VIEW_BG_TASKS": "Afficher les tâches en arrière-plan",
"DEPLOYMENT_PROGRESS": "Progression du déploiement",
"DEPLOYING_IMAGE": "{0} sur {1} hôte(s)", // Package name (e.g.,  someServerName.zip, aLibertyDockerImage), number of hosts
"VIEW_DEPLOYED_SERVERS": "Afficher les serveurs dont le déploiement a abouti",
"DEPLOY_PERCENTAGE": "{0} % TERMINE(S)", //quantity
"DEPLOYMENT_COMPLETE_TITLE" : "Votre déploiement est terminé.",
"DEPLOYMENT_COMPLETE_WITH_ERRORS_TITLE" : "Votre déploiement est terminé, mais des erreurs sont survenues.",
"DEPLOYMENT_COMPLETE_MESSAGE" : "Vous pouvez examiner les erreurs en détail, vérifiez les nouveaux serveurs déployés, ou démarrer un autre déploiement.",
"DEPLOYING": "Déploiement en cours...",
"DEPLOYMENT_FAILED": "Votre déploiement a échoué.",
"RETURN_DEPLOY": "Revenez au formulaire de déploiement et soumettez-le à nouveau",
"REUTRN_DEPLOY_HEADER": "Réessayer",

//Footer
"FOOTER": "Vous avez d'autres éléments à déployer ?",
"FOOTER_BUTTON_MESSAGE" : "Démarrer un autre déploiement",

//Error stuff
"ERROR_TITLE": "Récapitulatif des erreurs",
"ERROR_VIEW_DETAILS" : "Afficher les détails des erreurs",
"ONE_ERROR_ONE_HOST": "Une erreur est survenue sur un hôte",
"ONE_ERROR_MULTIPLE_HOST": "Une erreur est survenue sur plusieurs hôtes",
"MULTIPLE_ERROR_ONE_HOST": "Plusieurs erreurs sont survenues sur un hôte",
"MULTIPLE_ERROR_MULTIPLE_HOST": "Plusieurs erreurs sont survenues sur plusieurs hôtes",
"INITIALIZATION_ERROR_MESSAGE": "Impossible d'accéder à l'hôte ou aux informations relatives aux règles de déploiement sur le serveur",
"TRANSLATIONS_ERROR_MESSAGE" : "Impossible d'accéder aux chaînes externalisées",
"MISSING_HOST": "Sélectionnez au moins un hôte dans la liste",
"INVALID_CHARACTERS" : "La zone ne peut pas contenir de caractères spéciaux tels que '()$%&'",
"INVALID_DOCKER_IMAGE" : "Image introuvable",
"ERROR_HOSTS" : "{0} et {1} autre(s)" // if there are more than three hosts reporting error, the message for errors from 6 hosts would be like:
                                      // host1.com, host2.com, host3.com and 3 others
};
