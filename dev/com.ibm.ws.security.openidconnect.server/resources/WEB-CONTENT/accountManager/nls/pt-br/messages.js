/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
var messages = {
    // Common Strings
    "ADD_NEW": "Incluir novo",
    "CANCEL": "Cancelar",
    "CLEAR": "Limpar entrada de procura",
    "CLICK_TO_SORT": "Clique para classificar a coluna",
    "CLOSE": "Fechar",
    "COPY_TO_CLIPBOARD": "Copiar para a área de transferência",
    "COPIED_TO_CLIPBOARD": "Copiado para área de transferência",
    "DELETE": "Excluir",
    "DONE": "Pronto",
    "EDIT": "Editar",
    "FALSE": "Falso",
    "GENERATE": "Gerar",
    "LOADING": "Carregando",
    "LOGOUT": "Logout",
    "NEXT_PAGE": "Próxima página",
    "NO_RESULTS_FOUND": "Nenhum resultado localizado",
    "PAGES": "{0} de {1} página(s)",   // {0} - current page number; {1} - total pages
    "PAGE_SELECT": "Selecione o número da página para visualização",
    "PREVIOUS_PAGE": "Página anterior",
    "PROCESSING": "Processando",
    "REGENERATE": "Regenerar",
    "REGISTER": "Registrar",
    "TABLE_FIELD_SORT_ASC": "A tabela é classificada por {0} em ordem crescente. ",   // {0} - column name (ie. 'Name', 'Client ID')
    "TABLE_FIELD_SORT_DESC": "A tabela é classificada por {0} em ordem decrescente. ", // {0} - column name (ie. 'Name', 'Client ID')
    "TRUE": "Verdadeiro",
    "TRY_AGAIN": "Tentar novamente ...",
    "UPDATE": "Atualizar",

    // Common Column Names
    "EXPIRES_COL": "Expira em",
    "ISSUED_COL": "Emitido em",
    "NAME_COL": "Nome",
    "TYPE_COL": "Tipo",

    // Account Manager
    // 'app-token' and 'app-password' are keywords - don't translate
    // name - the user defined name given the app-password or app-token
    "ACCT_MGR_TITLE": "Gerenciar tokens pessoais",
    "ACCT_MGR_DESC": "Crie, exclua e gere novamente os app-passwords e app-tokens.",
    "ADD_NEW_AUTHENTICATION": "Incluir novo app-password ou app-token.",
    "NAME_IDENTIFIER": "Nome: {0}",
    "ADD_NEW_TITLE": "Registrar nova autenticação",
    "NOT_GENERATED_PLACEHOLDER": "Não gerado",
    "AUTHENTICAION_GENERATED": "Autenticação gerada",
    "GENERATED_APP_PASSWORD": "app-password gerado ",
    "GENERATED_APP_TOKEN": "app-token gerado ",
    "COPY_APP_PASSWORD": "Copiar app-password para a área de transferência",
    "COPY_APP_TOKEN": "Copiar app-token para a área de transferência",
    "REGENERATE_APP_PASSWORD": "Gerar novamente app-password",
    "REGENERATE_PW_WARNING": "Esta ação sobrescreverá o app-password atual.",
    "REGENERATE_PW_PLACEHOLDER": "Senha gerada anteriormente em {0}",        // 0 - date
    "REGENERATE_APP_TOKEN": "Gerar novamente app-token",
    "REGENERATE_TOKEN_WARNING": "Esta ação sobrescreverá o app-token atual.",
    "REGENERATE_TOKEN_PLACEHOLDER": "Token gerado anteriormente em {0}",        // 0 - date
    "DELETE_PW": "Excluir este app-password",
    "DELETE_TOKEN": "Excluir este app-token",
    "DELETE_WARNING_PW": "Esta ação removerá o app-password designado atualmente.",
    "DELETE_WARNING_TOKEN": "Esta ação removerá o app-token designado atualmente.",
    "REGENERATE_ARIA": "Gerar novamente {0} para {1}",     // 0 - 'app-password' or 'app-token'; 1 - name
    "DELETE_ARIA": "Excluir o {0} denominado {1}",       // 0 - 'app-password' or 'app-token'; 1 - name
    "GENERIC_GENERATE_FAIL": "Erro ao gerar {0}", // 0 - 'App-Password' or 'App-Token'
    "GENERIC_GENERATE_FAIL_MSG": "Ocorreu um erro ao gerar um novo {0} com o nome {1}.",  // 0 - 'app-password' or 'app-token'; 1 - name
    "ERR_NAME": "O nome já está associado a um {0} ou é muito longo.", // 0 - 'app-password' or 'app-token'
    "GENERIC_DELETE_FAIL": "Erro ao excluir {0}",     // 0 - 'App-Password' or 'App-Token'
    "GENERIC_DELETE_FAIL_MSG": "Ocorreu um erro ao excluir o {0} denominado {1}.",  // 0 - 'app-password' or 'app-token'; 1 - name
    "GENERIC_REGENERATE_FAIL": "Erro ao gerar novamente {0}",  // 0 - 'App-Password' or 'App-Token'
    "GENERIC_REGENERATE_FAIL_MSG": "Ocorreu um erro ao gerar novamente o {0} denominado {1}.",  // 0 - 'app-password' or 'app-token'; 1 - name
    "GENERIC_REGENERATE_FAIL_CREATE_MSG": "Ocorreu um erro ao gerar novamente o {0} denominado {1}. O {0} foi excluído, mas não pôde ser recriado.", // 0 - 'app-password' or 'app-token'; 1 - name
    "GENERIC_FETCH_FAIL": "Erro ao recuperar autenticações",
    "GENERIC_FETCH_FAIL_MSG": "Não é possível obter a lista atual de app-passwords ou app-tokens.",
    "GENERIC_NOT_CONFIGURED": "Cliente não configurado",
    "GENERIC_NOT_CONFIGURED_MSG": "Os atributos do cliente appPasswordAllowed e appTokenAllowed não estão configurados.  Nenhum dado pode ser recuperado.",
    "APP_PASSWORD_NOT_CONFIGURED": "O atributo do cliente appPasswordAllowed não está configurado.",  // 'appPasswordAllowed' is a config option. Do not translate.
    "APP_TOKEN_NOT_CONFIGURED": "O atributo do cliente appTokenAllowed não está configurado."         // 'appTokenAllowed' is a config option.  Do not translate.
};
