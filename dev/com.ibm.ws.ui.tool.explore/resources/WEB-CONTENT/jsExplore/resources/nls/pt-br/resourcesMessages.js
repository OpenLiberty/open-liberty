/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
define({
    ERROR : "Erro",
    ERROR_STATUS : "{0} Erro: {1}", //responseStatusCode, response.text
    ERROR_URL_REQUEST_NO_STATUS : "Ocorreu um erro ao solicitar {0}.", // url
    ERROR_URL_REQUEST : "{0} Ocorreu um erro ao solicitar {1}.", //responseStatusCode, url
    ERROR_SERVER_UNAVAILABLE : "O servidor não respondeu no tempo alocado.",
    ERROR_APP_NOT_AVAILABLE : "O aplicativo {0} não está mais disponível para o servidor {1} no host {2} no diretório {3}.", //appName, serverName, hostName, userdirName
    ERROR_APPLICATION_OPERATION : "Ocorreu um erro ao tentar {0} o aplicativo {1} no servidor {2} no host {3} no diretório {4}.", //operation, appName, serverName, hostName, userdirName
    CLUSTER_STATUS : "O cluster {0} está {1}.", //clusterName, clusterStatus
    CLUSTER_UNAVAILABLE : "O cluster {0} não está mais disponível.", //clusterName
    STOP_FAILED_DURING_RESTART : "A parada não foi concluída com sucesso durante a reinicialização.  O erro foi: {0}", //errMsg
    ERROR_CLUSTER_OPERATION : "Ocorreu um erro ao tentar {0} o cluster {1}.", //operation, clusterName
    SERVER_NONEXISTANT : "O servidor {0} não existe.", // serverName
    ERROR_SERVER_OPERATION : "Ocorreu um erro ao tentar {0} o servidor {1} no host {2} no diretório {3}.", //operation, serverName, hostName, userdirName
    ERROR_SERVER_OPERATION_SET_MAINTENANCE_MODE : "O modo de manutenção para o servidor {0} no host {1} no diretório {2} não foi configurado, devido a um erro.", //serverName, hostName, userdirName
    ERROR_SERVER_OPERATION_UNSET_MAINTENANCE_MODE : "A tentativa de desconfigurar o modo de manutenção para o servidor {0} no host {1} no diretório {2} não foi concluída, devido a um erro.", //serverName, hostName, userdirName
    ERROR_HOST_OPERATION_SET_MAINTENANCE_MODE : "O modo de manutenção para o host {0} não foi configurado, devido a um erro.", // hostName
    ERROR_HOST_OPERATION_UNSET_MAINTENANCE_MODE : "A tentativa de desconfigurar o modo de manutenção para o host {0} não foi concluída, devido a um erro.", // hostName
    // Dialog messages on operation errors
    SERVER_START_ERROR: "Erro ao iniciar o servidor.",
    SERVER_START_CLEAN_ERROR: "Erro ao iniciar o servidor --limpar.",
    SERVER_STOP_ERROR: "Erro ao parar o servidor.",
    SERVER_RESTART_ERROR: "Erro ao reiniciar o servidor.",
    // Used by standalone stop operation
    STANDALONE_STOP_NO_MBEAN : 'O servidor não parou. A API necessária para parar o servidor estava indisponível.',
    STANDALONE_STOP_CANT_DETERMINE_MBEAN : 'O servidor não parou. A API necessária para parar o servidor não pôde ser determinada.',
    STANDALONE_STOP_FAILED : 'A operação de parada do servidor não foi concluída com sucesso. Verifique os logs do servidor para obter detalhes.',
    STANDALONE_STOP_SUCCESS : 'Servidor interrompido com sucesso.',
});

