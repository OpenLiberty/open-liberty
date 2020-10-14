/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.zos.tx.internal.rrs;

/**
 * Provides the ability to invoke native RRS services.
 */
public interface RRSServices {

    /**
     * RRS return code values.
     */
    public static final int ATR_OK = 0;
    public static final int ATR_NO_MORE_INCOMPLETE_INTERESTS = 0x004;
    public static final int ATR_PARTIAL_PERSISTENT_DATA = 0x005;
    public static final int ATR_RM_LOGNAME_NOT_SET = 0x006;
    public static final int ATR_REQUESTED_WID_UNAVAILABLE = 0x007;
    public static final int ATR_FORGET = 0x008;
    public static final int ATR_RM_ALREADY_HAS_INTEREST = 0x008;
    public static final int ATR_PARTIAL_RM_LOGNAME = 0x009;
    public static final int ATR_PARTIAL_UWID_DATA = 0x00A;
    public static final int ATR_OK_NO_CONTEXT = 0x010;
    public static final int ATR_FORGET_NOT_REQUIRED = 0x011;
    public static final int ATR_COMMITTED_OUTCOME_PENDING = 0x065;
    public static final int ATR_COMMITTED_OUTCOME_MIXED = 0x066;
    public static final int ATR_PROGRAM_STATE_CHECK = 0x0C8;
    public static final int ATR_ASCMODE_INF = 0x101;
    public static final int ATR_INTERRUPT_STATUS_INV = 0x103;
    public static final int ATR_MODE_INV = 0x104;
    public static final int ATR_LOCKS_HELD = 0x105;
    public static final int ATR_UNSUPPORTED_RELEASE = 0x107;
    public static final int ATR_ENVIRONMENT_INV = 0x109;
    public static final int ATR_BACKED_OUT = 0x12C;
    public static final int ATR_BACKED_OUT_OUTCOME_PENDING = 0x12D;
    public static final int ATR_BACKED_OUT_OUTCOME_MIXED = 0x12E;
    public static final int ATR_RM_TOKEN_INV = 0x301;
    public static final int ATR_CONTEXT_TOKEN_INV = 0x361;
    public static final int ATR_STOKEN_INV = 0x362;
    public static final int ATR_TRAN_MODE_INV = 0x363;
    public static final int ATR_ENV_SETTING_ID_INV = 0x364;
    public static final int ATR_ENV_SETTING_INV = 0x365;
    public static final int ATR_SCOPE_INV = 0x366;
    public static final int ATR_DU_TERMINATING = 0x36A;
    public static final int ATR_ACTION_INV = 0x36B;
    public static final int ATR_PROTLEVEL_INV = 0x36C;
    public static final int ATR_URI_TOKEN_INV = 0x370;
    public static final int ATR_INTEREST_TYPE_INV = 0x371;
    public static final int ATR_FAILURE_ACTION_INV = 0x372;
    public static final int ATR_PREPARE_CODE_INV = 0x373;
    public static final int ATR_COMMIT_CODE_INV = 0x374;
    public static final int ATR_TWO_PHASE_PROTOCOL_INV = 0x375;
    public static final int ATR_PERSISTENT_DATA_LEN_INV = 0x376;
    public static final int ATR_UWID_LEN_INV = 0x377;
    public static final int ATR_EXIT_NUMBER_INV = 0x378;
    public static final int ATR_COMP_CODE_INV = 0x379;
    public static final int ATR_RM_LOGNAME_INV = 0x37A;
    public static final int ATR_RM_LOGNAME_LEN_INV = 0x37B;
    public static final int ATR_RM_LOGNAME_BUF_LEN_INV = 0x37C;
    public static final int ATR_PERSIS_DATA_BUF_LEN_INV = 0X37D;
    public static final int ATR_RETRIEVE_OPTION_INV = 0x37E;
    public static final int ATR_SET_OPTION_INV = 0x37F;
    public static final int ATR_UWID_TYPE_INV = 0x380;
    public static final int ATR_LATER_INV = 0x381;
    public static final int ATR_UWID_BUF_LEN_INV = 0x382;
    public static final int ATR_SIDE_INFO_ID_INV = 0x383;
    public static final int ATR_RESPONSE_CODE_INV = 0X384;
    public static final int ATR_RESPONSE_CODE_INCORRECT = 0x385;
    public static final int ATR_FAILURE_ACTION_INCORRECT = 0x386;
    public static final int ATR_PREPARE_CODE_INCORRECT = 0x387;
    public static final int ATR_GENERATE_OPTION_INV = 0x388;
    public static final int ATR_PERSISTENT_DATA_NOT_ALLOWED = 0x389;
    public static final int ATR_ROLE_INV = 0x390;
    public static final int ATR_MULTIPLE_INTEREST_OPTION_INV = 0x391;
    public static final int ATR_ELEMENT_COUNT_INV = 0x392;
    public static final int ATR_LUWID_DATA_INV = 0x393;
    public static final int ATR_BACKOUT_CODE_INV = 0x394;
    public static final int ATR_LOG_OPT_INV = 0x395;
    public static final int ATR_FLIGHT_OPTION_INV = 0x396;
    public static final int ATR_XID_DATA_INV = 0x397;
    public static final int ATR_STATES_OPTION_INV = 0x398;
    public static final int ATR_UR_FAMILY_OPTION_INV = 0x399;
    public static final int ATR_PARENT_UR_TOKEN_INV = 0x39A;
    public static final int ATR_CHILD_CONTEXT_TOKEN_INV = 0x39B;
    public static final int ATR_XID_LENGTH_INV = 0x39C;
    public static final int ATR_XID_INV = 0x39D;
    public static final int ATR_PARENT_DU_TERMINATING = 0x39E;
    public static final int ATR_CHILD_DU_TERMINATING = 0x39F;
    public static final int ATR_SAME_CURRENT_CONTEXT_INV = 0x3A0;
    public static final int ATR_SAME_PARENT_CONTEXT_INV = 0x3A1;
    public static final int ATR_SAME_CHILE_CONTEXT_INV = 0x3A2;
    public static final int ATR_UR_TOKEN_INV = 0x3A3;
    public static final int ATR_PARENT_AUTH_FAILURE = 0x3A4;
    public static final int ATR_CHILD_AUTH_FAILURE = 0x3A5;
    public static final int ATR_PET_INV = 0x3A6;
    public static final int ATR_PET_OUTDATED = 0x3A7;
    public static final int ATR_PET_AUTH_FAILURE = 0x3A8;
    public static final int ATR_PET_SPACE_FAILURE = 0x3A9;
    public static final int ATR_PET_NOT_ASSOCIATED = 0x3AA;
    public static final int ATR_AUTH_FAILURE = 0x3AB;
    public static final int ATR_INTEREST_OPTIONS_INV = 0x3AC;
    public static final int ATR_CREATE_OPTIONS_INV = 0x3AD;
    public static final int ATR_COMMIT_OPTIONS_INV = 0x3AE;
    public static final int ATR_SIDE_INFORMATION_OPTIONS_INVALID = 0x3AF;
    public static final int ATR_XID_EXISTS = 0x3B0;
    public static final int ATR_SUBORDINATE_FAILED_EXIT_NOT_DEFINED = 0x3B1;
    public static final int ATR_SUBORDINATE_FAILED_EXIT_INV = 0x3B2;
    public static final int ATR_COMMIT_TIER_ONE_SRB_INV = 0x3B3;
    public static final int ATR_COMMIT_TIER_ONE_MISMATCH = 0x3B7;
    public static final int ATR_RM_STATE_ERROR = 0x701;
    public static final int ATR_RM_EXITS_UNSET = 0x702;
    public static final int ATR_NOT_PROTECTED_INTEREST = 0x730;
    public static final int ATR_UR_STATE_ERROR = 0x731;
    public static final int ATR_NO_DIST_SYNC_EXIT = 0x732;
    public static final int ATR_SSPC_ROLE_ERROR_DSRM = 0x733;
    public static final int ATR_SSPC_ROLE_ERROR_LAST_AGENT = 0x734;
    public static final int ATR_UWID_ALREADY_SET = 0x735;
    public static final int ATR_SROI_ALREADY_DONE = 0x736;
    public static final int ATR_RM_ATTR_INCORRECT = 0x738;
    public static final int ATR_PROTECTED_INTEREST = 0x739;
    public static final int ATR_RESTART_INCOMPLETE = 0x73A;
    public static final int ATR_AFTER_NEW_UR = 0x73C;
    public static final int ATR_INV_FOR_RESTART_INTEREST = 0x73D;
    public static final int ATR_NO_COMPLETION_EXIT_SET = 0x73E;
    public static final int ATR_LUWID_NOT_AVAILABLE = 0x73F;
    public static final int ATR_POST_NOT_PENDING = 0x740;
    public static final int ATR_NOT_RETRIEVED_INTEREST = 0x741;
    public static final int ATR_RESPONSE_NOT_PENDING = 0x742;
    public static final int ATR_PARENT_UR_STATE_ERROR = 0x743;
    public static final int ATR_CHILD_UR_STATE_ERROR = 0x744;
    public static final int ATR_AFTER_IN_PREPARE = 0x745;
    public static final int ATR_ROLE_INCORRECT = 0x746;
    public static final int ATR_TERMINATING_SYNCPOINT = 0x747;
    public static final int ATR_RM_IS_THE_SDSRM = 0x748;
    public static final int ATR_GEN_NOT_ALLOWED_NO_LUNAME = 0x748;
    public static final int ATR_MAX_UR_LOG_DATA_EXCEEDED = 0x749;
    public static final int ATR_NOT_SERVER_DSRM = 0x74A;
    public static final int ATR_SSPC_ROLE_ERROR_SERVER_DSRM = 0x74B;
    public static final int ATR_SDSRM_DISALLOWS_COMMIT = 0x74C;
    public static final int ATR_GEN_NOT_ALLOWED_EID = 0x74D;
    public static final int ATR_SET_NEXT_EID_INV = 0x74E;
    public static final int ATR_ROLE_CHANGE_AFTER_SYNC = 0x74F;
    public static final int ATR_RESPOND_CONTINUE_REQUIRED = 0x750;
    public static final int ATR_GEN_REQUIRED_XID = 0x751;
    public static final int ATR_SET_NEXT_XID_INV = 0x752;
    public static final int ATR_GEN_NOT_ALLOWED_NO_URI_TOKEN = 0x753;
    public static final int ATR_RETRIEVE_NEXT_EID_INV = 0x754;
    public static final int ATR_RETRIEVE_NEXT_XID_INV = 0x755;
    public static final int ATR_CASCADED_UR_DISALLOWS_COMMIT = 0x756;
    public static final int ATR_ID_CONFLICT = 0x757;
    public static final int ATR_APPL_COMPLETE_INV = 0x758;
    public static final int ATR_ROLE_ERROR_CASCADED_UR = 0x759;
    public static final int ATR_CASCADED_UR = 0x760;
    public static final int ATR_APPL_COMPLETE_INV_STATE = 0x761;
    public static final int ATR_PRESUMED_NOTHING_INVALID = 0x762;
    public static final int ATR_PARENT_LOCAL_TRAN_MODE_INV = 0x763;
    public static final int ATR_NO_CASCADE_TO_PARENT = ATR_PARENT_LOCAL_TRAN_MODE_INV;
    public static final int ATR_LOCAL_TRAN_MODE_INV = 0x764;
    public static final int ATR_NOT_ALLOWED_FOR_UR = ATR_LOCAL_TRAN_MODE_INV;
    public static final int ATR_GEN_LUWID_NOT_ALLOWED_LOCAL = 0x765;
    public static final int ATR_NO_LUWID_GEN_FOR_UR = ATR_GEN_LUWID_NOT_ALLOWED_LOCAL;
    public static final int ATR_SIDE_INFO_ID_LOCAL_INV = 0x766;
    public static final int ATR_NO_SIDE_INFO_FOR_UR = ATR_SIDE_INFO_ID_LOCAL_INV;
    public static final int ATR_GEN_XID_NOT_ALLOWED_LOCAL = 0x767;
    public static final int ATR_NO_XID_GEN_FOR_UR = ATR_GEN_XID_NOT_ALLOWED_LOCAL;
    public static final int ATR_XID_NO_GLOBAL_MATCH = 0x769;
    public static final int ATR_SETTING_PROTECTED = 0x801;
    public static final int ATR_STOKEN_NOT_ZERO = 0x802;
    public static final int ATR_CTOKEN_NOT_ZERO = 0x803;
    public static final int ATR_HYBRID_GLOBAL_MODE_ERROR = 0x804;
    public static final int ATR_CUR_UR_TOKEN_NOT_CURRENT = 0x805;
    public static final int ATR_NOT_AVAILABLE = 0xF00;
    public static final int ATR_HARDENED_DATA_LOSS = 0xF01;
    public static final int ATR_RESTART_WRONG_SYSTEM = 0xF02;
    public static final int ATR_UR_RESOLVED_BY_INSTALLATION = 0xF03;
    public static final int ATR_UNEXPECTED_UR_ERROR = 0xF04;
    public static final int ATR_UNEXPECTED_CTX_ERROR = 0xF05;
    public static final int ATR_WAS_NOT_AVAILABLE = 0xF06;
    public static final int ATR_RM_GROUP_RRS_DOWNLEVEL = 0xF07;
    public static final int ATR_UNEXPECTED_ERROR = 0xFFF;
    public static final int ATR_EXIT_PREPARE_NOT_SPECIFIFED = 0x8000;
    public static final int ATR_EXIT_COMMIT_NOT_SPECIFIED = 0x8001;
    public static final int ATR_EXIT_BACKOUT_NOT_SPECIFIFED = 0x8002;
    public static final int ATR_EXIT_EXIT_FAILED_NOT_SPECIFIED = 0x8003;
    public static final int ATR_RM_ACTIVE_ON_ANOTHER_SYSTEM = 0x8004;
    public static final int ATR_RM_NEW_KEY_INV = 0x8005;
    public static final int ATR_SEIF_PARM_NOT_ADDR = 0x8006;
    public static final int ATR_EM_WRONG_STATE = 0x8007;
    public static final int ATR_RM_WRONG_STATE = 0x8008;

    /**
     * Metadata related service return codes.
     */
    public static final int ATR_PARTIAL_RM_METADATA = 0x00B;
    public static final int ATR_RM_METADATA_LEN_INV = 0x38A;
    public static final int ATR_RM_METADATA_BUFFER_LEN_INV = 0x38B;
    public static final int ATR_RM_METADATA_LOG_UNAVAILABLE = 0x38C;
    public static final int ATR_RM_8K_METADATA_NOT_ALLOWED = 0x38D;
    public static final int ATR_RM_METADATA_MISSING_DATA = 0x38E;

    /**
     * Registration Services' codes.
     */
    public static final int CRG_OK = 0x000;
    public static final int CRG_RM_TOKEN_INV = 0x301;
    public static final int CRG_EM_STATE_ERROR = 0x720;
    public static final int CRG_UNREG_EOM = 0x002;

    /**
     * Exit return codes.
     */
    public static final int ATRX_OK = 0;
    public static final int ATRX_OK_OUTCOME_PENDING = 0x004;
    public static final int ATRX_BACKOUT = 0x008;
    public static final int ATRX_BACKOUT_OUTCOME_PENDING = 0x00C;
    public static final int ATRX_FORGET = 0x010;
    public static final int ATRX_ABSTAIN = 0x014;
    public static final int ATRX_REDRIVE = 0x01C;
    public static final int ATRX_STATE_INCORRECT = 0x020;
    public static final int ATRX_HC = 0x024;
    public static final int ATRX_HR = 0x028;
    public static final int ATRX_HM = 0x02C;
    public static final int ATRX_LATER = 0x030;
    public static final int ATRX_LATER_CONTINUE = 0x034;
    public static final int ATRX_HM_BACKOUT = 0x038;
    public static final int ATRX_HM_COMMIT = 0x03C;
    public static final int ATRX_DEFER = 0x040;
    public static final int ATRX_UNSET_RM = 0x404;

    /**
     * Context services return codes.
     */
    public static final int CTX_OK = 0x000;
    public static final int CTX_INTERRUPT_STATUS_INV = 0x103;
    public static final int CTX_MODE_INV = 0x104;
    public static final int CTX_LOCKS_HELD = 0x105;
    public static final int CTX_UNSUPPORTED_RELEASE = 0x107;
    public static final int CTX_COMPLETION_TYPE_INV = 0x360;
    public static final int CTX_CONTEXT_TOKEN_INV = 0x361;
    public static final int CTX_OTHER_WU_NATIVE = 0x363;
    public static final int CTX_PRIVATE_OTHER_WU = 0x366;
    public static final int CTX_CURRENT_WU_NATIVE = 0x368;
    public static final int CTX_SWITCH_EXIT_PREVENTED_END = 0x369;
    public static final int CTX_AUTH_FAILURE = 0x756;
    public static final int CTX_UNEXPECTED_ERROR = 0xFFF;

    /**
     * Context services exits.
     */
    public static final int CTX_EXIT_FAILED_EXIT = 1;
    public static final int CTX_SWITCH_EXIT = 2;
    public static final int CTX_PRIVATE_CONTEXT_OWNER = 3;
    public static final int CTX_END_CONTEXT_EXIT = 4;
    public static final int CTX_EOM_CONTEXT_EXIT = 5;

    /**
     * Context services exit return codes.
     */
    public static final int CTX_DIS_PVT_CONTEXT = 1;

    /**
     * RRS log options.
     */
    public static final int ATR_DEFER = 0;
    public static final int ATR_DEFER_IMPLICIT = 0;
    public static final int ATR_DEFER_EXPLICIT = 1;
    public static final int ATR_IMMEDIATE = 2;

    /**
     * RRS Identifier types.
     */
    public static final int ATR_LUWID = 0x000;
    public static final int ATR_EID = 0x001;
    public static final int ATR_XID = 0x002;

    /**
     * Work ID generation options.
     */
    public static final int ATR_DO_NOT_GENERATE = 0x000;
    public static final int ATR_GENERATE = 0x001;

    /**
     * Work ID retrieval options.
     */
    public static final int ATR_CURRENT = 0x000;
    public static final int ATR_NEXT = 0x001;

    /**
     * Environment protection options.
     */
    public static final int ATR_UNPROTECTED_SETTING = 0x001;
    public static final int ATR_PROTECTED_SETTING = 0x002;

    /**
     * Interest protection options.
     */
    public static final int ATR_UNPROTECTED = 0x000;
    public static final int ATR_PROTECTED = 0x001;
    public static final int ATR_PROT_LOGGED = 0x002;

    /**
     * Express interest multi-interest options.
     */
    public static final int ATR_UNCONDITIONAL = 0x000;
    public static final int ATR_CONDITIONAL = 0x001;

    /**
     * Express interest failure action options.
     */
    public static final int ATR_FAIL_STANDARD = 0x000;
    public static final int ATR_FAIL_FUTURE = 0x001;
    public static final int ATR_FAIL_FORGET = 0x002;

    /**
     * Express interest two phase protocol.
     */
    public static final int ATR_PRESUMED_NOTHING = 0x000;
    public static final int ATR_PRESUMED_ABORT = 0x001;

    /**
     * ATRADCT1 options.
     */
    public static final int ATR_STANDARD_COMMIT_MASK = 0x0;
    public static final int ATR_REMOVE_SDSRM_INTEREST_MASK = 0x10000000;

    /**
     * Pre-vote replies.
     */
    public static final int ATR_BACKOUT_OK = 0x000;
    public static final int ATR_DRIVE_BACKOUT_EXIT = 0xFFF;
    public static final int ATR_COMMIT_OK = 0x000;
    public static final int ATR_DRIVE_COMMIT_EXIT = 0xFFF;
    public static final int ATR_PREPARE_OK = 0x000;
    public static final int ATR_PREPARE_ABSTAIN = 0x014;
    public static final int ATR_DRIVE_PREPARE_EXIT = 0xFFF;

    /**
     * Role types.
     */
    public static final int ATR_PARTICIPANT = 0x000;
    public static final int ATR_LAST_AGENT = 0x001;
    public static final int ATR_DSRM = 0x002;
    public static final int ATR_SDSRM = 0x003;

    /**
     * Side flags.
     */
    public static final int ATR_HEURISTIC_MIX = 0x000;
    public static final int ATR_BACKOUT_REQUIRED = 0x001;
    public static final int ATR_BREAK_TREE = 0x010;
    public static final int ATR_DRIVE_BACKOUT = 0x011;
    public static final int ATR_RESYNC_IN_PROGRESS = 0x012;
    public static final int ATR_NEW_LUWID_PSH_UNACCEPTABLE = 0x013;
    public static final int ATR_DRIVE_COMPLETION = 0x014;
    public static final int ATR_SDSRM_INITIATED = 0x015;
    public static final int ATR_RESOLVED_BY_INSTALLATION = 0x016;
    public static final int ATR_TERM_SYNCPOINT = 0x017;
    public static final int ATR_COMMITTED = 0x018;
    public static final int ATR_IMMEDIATE_BACKOUT = 0x020;
    public static final int ATR_APPL_COMPLETE = 0x021;
    public static final int ATR_RESET_APPL_COMPLETE = 0x022;
    public static final int ATR_SI_LOCAL_MODE = 0x023;
    public static final int ATR_SI_GLOBAL_MODE = 0x024;

    /**
     * Side value results.
     */
    public static final int ATR_SIDE_VALUE_NOT_SET = 0x000;
    public static final int ATR_SIDE_VALUE_SET = 0x001;

    /**
     * Exit numbers.
     */
    public static final int ATR_STATE_CHECK_EXIT = 0x001;
    public static final int ATR_PREPARE_EXIT = 0x002;
    public static final int ATR_DISTRIBUTED_SYNCPOINT_EXIT = 0x003;
    public static final int ATR_COMMIT_EXIT = 0x004;
    public static final int ATR_BACKOUT_EXIT = 0x005;
    public static final int ATR_END_UR_EXIT = 0x006;
    public static final int ATR_EXIT_FAILED_EXIT = 0x007;
    public static final int ATR_COMPLETION_EXIT = 0x008;
    public static final int ATR_ONLY_AGENT_EXIT = 0x009;
    public static final int ATR_SUBORDINATE_FAILED_EXIT = 0x00A;
    public static final int ATR_PRE_PREPARE_EXIT = 0x00B;

    /**
     * UR States.
     */
    public static final int ATR_IN_RESET = 0x000;
    public static final int ATR_IN_FLIGHT = 0x001;
    public static final int ATR_IN_STATE_CHECK = 0x002;
    public static final int ATR_IN_PREPARE = 0x003;
    public static final int ATR_IN_DOUBT = 0x004;
    public static final int ATR_IN_COMMIT = 0x005;
    public static final int ATR_IN_BACKOUT = 0x006;
    public static final int ATR_IN_END = 0x007;
    public static final int ATR_IN_ONLY_AGENT = 0x008;
    public static final int ATR_IN_COMPLETION = 0x009;
    public static final int ATR_FORGOTTEN = 0x00A;
    public static final int ATR_IN_FORGET = 0x00B;
    public static final int ATR_PREFLIGHT = 0x00C;

    /**
     * Exit manager name.
     */
    public static final String ATR_EXITMGR_NAME = "ATR.EXITMGR.IBM ";

    /**
     * Context services exit manager name.
     */
    public static final String CTX_EXITMGR_NAME = "CTX.EXITMGR.IBM ";

    /**
     * Registration services resource manager name
     */
    public static final String CRG_RESMGR_NAME = "ATR.RESOURCEMANAGER.IBM         ";

    /**
     * Exit types.
     */
    public static final int CRG_EXIT_TYPE_NONE = 0x000;
    public static final int CRG_EXIT_TYPE_SRB = 0x001;
    public static final int CRG_EXIT_TYPE_PC = 0x002;
    public static final int CRG_EXIT_TYPE_BALR = 0x003;

    public static final int ATR_EXIT_TYPE_SRB = 0x001;
    public static final int ATR_EXIT_TYPE_PC = 0x002;

    /**
     * Returned URI responses.
     */
    public static final int ATR_RESPOND_CONTINUE = 0x000;
    public static final int ATR_RESPOND_COMPLETE = 0x001;

    /**
     * ATRRUSF options.
     */
    public static final int ATR_INTEREST_COUNT_MASK = 0x00000001;

    /**
     * ATRRUSF environment info flags
     */
    public static final int ATR_NO_INTERESTS_MASK = 0x00000001;
    public static final int ATR_RM_COORD_OK_MASK = 0x00000002;
    public static final int ATR_RRS_MUST_COORD_MASK = 0x00000004;
    public static final int ATR_ZERO_INTEREST_COUNT_MASK = 0x00000010;
    public static final int ATR_ONE_INTEREST_COUNT_MASK = 0x00000020;
    public static final int ATR_MULTIPLE_INTEREST_COUNT_MASK = 0x00000040;
    public static final int ATR_UR_STATE_IN_RESET_MASK = 0x00000100;
    public static final int ATR_GLOBAL_MODE_MASK = 0x00010000;
    public static final int ATR_LOCAL_MODE_MASK = 0x00020000;
    public static final int ATR_HYBRID_GLOBAL_MASK = 0x00040000;

    /**
     * ATRRURD States options.
     */
    public static final int ATR_STANDARD_STATES = 0x00000000;
    public static final int ATR_EXTENDED_STATES = 0x00000001;

    /**
     * ATREINT5 masks.
     */
    public static final int ATR_UNPROT_INT_MASK = 0x00000000;
    public static final int ATR_PROTECTED_INT_MASK = 0x01000000;
    public static final int ATR_PRESUME_NOTHING_MASK = 0x00000000;
    public static final int ATR_PRESUME_ABORT_MASK = 0x00010000;
    public static final int ATR_CREATE_STANDARD_UR_MASK = 0x00000000;
    public static final int ATR_CREATE_CASCADED_UR_MASK = 0x00001000;
    public static final int ATR_USE_FORMATID_MASK = 0x00000008;
    public static final int ATR_USE_BQUAL_MASK = 0x00000010;

    /**
     * CTXENDC flags.
     */
    public static final int CTX_NORMAL_TERMINATION = 0x00000000;
    public static final int CTX_ABNORMAL_TERMINATION = 0x00000001;
    public static final int CTX_FORCED_END_OF_CONTEXT = 0x00000003;

    /**
     * ATR4SENV options.
     */
    public static final int ATR_ADDRESS_SPACE_SCOPE = 0x001;
    public static final int ATR_CONTEXT_SCOPE = 0x002;

    public static final int ATR_TRAN_MODE_SETTING = 0x001;
    public static final int ATR_NORM_CTX_END_SETTING = 0x002;

    public static final int ATR_ENVIRONMENT_NOT_SET = 0x000;
    public static final int ATR_GLOBAL_MODE = 0x001;
    public static final int ATR_LOCAL_MODE = 0x002;
    public static final int ATR_HYBRID_GLOBAL_MODE = 0x003;

    /**
     * ATR4END Resolution direction.
     */
    public static final int ATR_COMMIT_ACTION = 0x001;
    public static final int ATR_ROLLBACK_ACTION = 0x002;

    /**
     * Misc. Options.
     */
    public static final int ATR_MAX_RM_LOGNAME_LENGTH = 0x40;
    public static final int ATR_MAX_PDATA_LENGTH = 0x1000;
    public static final int ATR_8K_RM_METADATA_LENGTH = 0x2000;
    public static final int ATR_MAX_RM_METADATA_LENGTH = 0x2000;

    /**
     * Begins a transaction with RRS.
     *
     * @param tranMode The transaction mode: Local, Global.
     *
     * @return The object containing all information returned by the service call or
     *         null if the native method detected an error during the invocation process.
     */
    public BeginTransactionReturnType beginTransaction(int transactionMode);

    /**
     * Ends the unit of recovery pointed to by the specified UR token.
     *
     * @param action         The direction of the outcome.
     * @param currentUrToken The unit of recovery token that identifies the UR to
     *                           be ended.
     *
     * @return 0 if the RRS service completed successfully. A number greater
     *         than zero if the RRS service returned a bad return code.
     *         -1 if there was a native failure during the invocation process.
     */
    public int endUR(int action, byte[] currentUrToken);

    /**
     * Backs out the unit of recovery currently on the thread.
     *
     * @return 0 if the RRS service completed successfully. A number greater
     *         than zero if the RRS service returned a bad return code.
     */
    public int backoutUR();

    /**
     * Retrieve the current settings of RRS-related environment
     * attributes for the unit of recovery associated with the specified context.
     *
     * @param ctxToken    The context token associated to the UR whose
     *                        information is being requested.
     * @param infoOptions The information to be retrieved.
     *
     * @return The object containing all information returned by the service call or
     *         null if the native method detected an error during the invocation process.
     */
    public RetrieveSideInformationFastReturnType retrieveSideInformationFast(byte[] ctxToken,
                                                                             int infoOptions);

    /**
     * Retrieves data for a unit of recovery.
     *
     * @param uriToken     The token that identifies the resource
     *                         manager interest in the unit of recovery whose data is to
     *                         be retrieved.
     * @param statesOption The states RRS may return for the specified unit
     *                         of recovery.
     *
     * @return The object containing all information returned by the service call or
     *         null if the native method detected an error during the invocation process.
     */
    public RetrieveURDataReturnType retrieveURData(byte[] uriToken,
                                                   int statesOption);

    /**
     * Registers the resource manager with the system (registration
     * services).
     *
     * @param unregOption  The option that indicates how the system is to
     *                         determine that the resource manager is ending unexpectedly.
     * @param globalData   The global data for the resource manager.
     *                         The system passes this data to all exit routines for the resource
     *                         manager.
     * @param rmNamePrefix The prefix name to be used to build the resource manager name.
     * @param rmNameSTCK   The timestamp (STCK) to be used to build the resource manager name.
     *
     * @return The object containing all information returned by the service call or
     *         null if the native method detected an error during the invocation process.
     */
    public RegisterResMgrReturnType registerResourceManager(int unregOption,
                                                            byte[] globalData,
                                                            byte[] rmNamePrefix,
                                                            byte[] rmNameSTCK);

    /**
     * Unregisters the resource manager with the system (registration
     * services).
     *
     * @param rmNameRegistryToken The token used to look up the name
     *                                in the native registry. The name identifies
     *                                the resource manager to be unregistered.
     * @param rmRegistryToken     The token used to look up the token that
     *                                identifies the resource manager to be unregistered.
     *
     * @return 0 if the RRS service completed successfully. A number greater
     *         than zero if the RRS service returned a bad return code.
     *         -1 if there was a native failure during the invocation process.
     */
    public int unregisterResourceManager(byte[] rmNameRegistryToken, byte[] rmRegistryToken);

    /**
     * Registers with the context services and RRS services exit managers.
     * For context services, no exits are registered. For RRS services, only
     * the required exits are registered (exit failed, commit, backout, and
     * prepare). The exit provided is in the server authorized function
     * module.
     *
     * @param rmNameRegistryToken The token used to look up the name
     *                                in the native registry. The name identifies
     *                                the resource manager whose exit information is to be set.
     * @param rmRegistryToken     The token used to look up the token that identifies the resource manager.
     * @param recovery            Indicates whether or not this calls is being made for a
     *                                recovering resource manager.
     *
     * @return The object containing all information returned by the native call or
     *         null if the native method detected an error during the invocation process.
     */
    public SetExitInformationReturnType setExitInformation(byte[] rmNameRegistryToken, byte[] rmRegistryToken, boolean recovery);

    /**
     * Begins the resource manager's restart.
     *
     * @param rmRegistryToken The token used to look up the token that identifies the resource manager.
     *
     * @return 0 if the RRS service completed successfully. A number greater
     *         than zero if the RRS service returned a bad return code.
     *         -1 if there was a native failure during the invocation process.
     */
    public int beginRestart(byte[] rmRegistryToken);

    /**
     * Ends resource manager restart.
     *
     * @param rmRegistryToken The token used to look up the token that identifies the resource manager.
     *
     * @return 0 if the RRS service completed successfully. A number greater
     *         than zero if the RRS service returned a bad return code.
     *         -1 if there was a native failure during the invocation process.
     */
    public int endRestart(byte[] rmRegistryToken);

    /**
     * Retrieves the log name associated to the specified resource manager.
     * Usually called before the call to begin restart.
     *
     * @param rmRegistryToken The token used to look up the token that identifies the resource manager.
     *
     * @return The object containing all information returned by the service call or
     *         null if the native method detected an error during the invocation process.
     */
    public RetrieveLogNameReturnType retrieveLogName(byte[] rmRegistryToken);

    /**
     * Sets the log name associated to the specified resource manager.
     *
     * @param rmRegistryToken The token used to look up the token that identifies the resource manager.
     * @param logname         The resource manager log name.
     *
     * @return 0 if the RRS service completed successfully. A number greater
     *         than zero if the RRS service returned a bad return code.
     *         -1 if there was a native failure during the invocation process.
     */
    public int setLogName(byte[] rmRegistryToken, byte[] logname);

    /**
     * Retrieves the work identifier (UWID/XID) for a unit of recovery.
     *
     * @param uriRegistryToken The token used to look up the URI token in the
     *                             native registry. The URI token represents the
     *                             interest that identifies the unit of recovery
     *                             whose work id is to be retrieved.
     *
     * @return The object containing all information returned by the service call or
     *         null if the native method detected an error during the invocation process.
     * @throws RegistryException Thrown if the URI registry token was not recognized.
     */
    public RetrieveWorkIdentifierReturnType retrieveWorkIdentifier(byte[] uriRegistryToken) throws RegistryException;

    /**
     * Sets the work identifier (UWID/XID) for a unit of recovery.
     *
     * @param ur_or_uriToken The token for the unit of recovery or unit of recovery
     *                           interest that identifies the unit of recovery whose work id is to
     *                           be set.
     * @param xid            The work ID.
     *
     * @return 0 if the RRS service completed successfully. A number greater
     *         than zero if the RRS service returned a bad return code.
     *         -1 if there was a native failure during the invocation process.
     */
    public int setWorkIdentifier(byte[] ur_or_uriToken, byte[] xid);

    /**
     * Expresses an interest, either protected or unprotected,
     * in a unit of recovery
     *
     * @param rmRegistryToken The token used to look up the token that identifies the resource manager.
     * @param ctxToken        The token that identifies the context associated with the
     *                            unit of recovery.
     * @param protocol        The two-phase protocol to use.
     * @param nonPData        The non persistent interest data for the
     *                            resource manager interest.
     * @param pdata           The persistent interest data (max 4K bytes).
     * @param xid             The XidImpl for the transaction.
     * @param parentUrToken   The token that identifies the unit of recovery
     *                            associated to the parent when in cascaded mode.
     *
     * @return The object containing all information returned by the service call or
     *         null if the native method detected an error during the invocation process.
     * @throws RegistryException Thrown if the URI could not be added to the native registry.
     *                               The URI will be deleted.
     */
    public ExpressInterestReturnType expressURInterest(byte[] rmRegistryToken,
                                                       byte[] ctxToken,
                                                       int protocol,
                                                       byte[] nonPData,
                                                       byte[] pdata,
                                                       byte[] xid,
                                                       byte[] parentUrToken) throws RegistryException;

    /**
     * Retrieves side information for an interest in a unit of recovery.
     *
     * @param uriRegistryToken The token used to look up the URI token in the
     *                             native registry. The URI token represents the
     *                             interest that identifies the unit of recovery
     *                             whose work id is to be retrieved.
     * @param info_ids         The ids for the data to be retrieved.
     *
     * @return The object containing all information returned by the service call or
     *         null if the native method detected an error during the invocation process.
     * @throws RegistryException Thrown if the URI registry token was not recognized.
     */
    public RetrieveSideInformationReturnType retrieveSideInformation(byte[] uriRegistryToken,
                                                                     int[] info_ids) throws RegistryException;

    /**
     * Retrieve information about the resource manager's interest
     * in an incomplete, protected unit of recovery.
     *
     * @param rmRegistryToken The token used to look up the token that identifies the resource manager whose
     *                            unit of recovery interest data is to be retrieved.
     *
     * @return The object containing all information returned by the service call or
     *         null if the native method detected an error during the invocation process.
     * @throws RegistryException Thrown if the returned URI could not be added to the native registry.
     *                               The runtime will respond ATR_CONTINUE to the URI.
     */
    public RetrieveURInterestReturnType retrieveURInterest(byte[] rmRegistryToken) throws RegistryException;

    /**
     * Establishes environmental settings for RRS.
     *
     * @param stoken           The space token of the address space for which the
     *                             resource manager is establishing address space scope environment
     *                             settings.
     * @param envIds           The one or more identifiers to set. Each identifier supplies
     *                             an environment attribute that is to be set.
     * @param envIdValues      The value for each identifier on the environment id parameter.
     * @param protectionValues The protection value for each identifier in the
     *                             environment id parameter.
     *
     * @return 0 if the RRS service completed successfully. A number greater
     *         than zero if the RRS service returned a bad return code.
     *         -1 if there was a native failure during the invocation process.
     */
    public int setEnvironment(byte[] stoken,
                              int[] envIds,
                              int[] envIdValues,
                              int[] protectionValues);

    /**
     * Tell RRS to prepare the unit of recovery associated with the specified
     * unit of recovery interest.
     *
     * @param uriRegistryToken The token used to look up the unit of recovery
     *                             interest (URI) token in the native registry. The URI token
     *                             identifies an instance of the resource manager interest in
     *                             a unit of recovery.
     * @param ctxRegistryToken The token used to look up the context token in
     *                             the native registry. The context token is used to express
     *                             an interest in the context should the UR go in-doubt.
     * @param rmRegistryToken  The token used to look up the resource manager token.
     * @param logOption        The option that indicates how RRS is to process
     *                             log entries for the unit of recovery.
     *
     * @return An object encapsulating the return codes from ATR4APRP and
     *         CTX4EINT, plus the context interest token returned from CTX4EINT
     *         if ATR4APRP and CTX4EINT return successfully.
     *
     * @throws RegistryException Thrown if the URI or context token registry token is not recognized.
     */
    public PrepareAgentURReturnType prepareAgentUR(byte[] uriRegistryToken, byte[] ctxRegistryToken, byte[] rmRegistryToken, int logOption) throws RegistryException;

    /**
     * Tell RRS to commit the unit of recovery associated with the specified
     * unit of recovery interest.
     *
     * @param uriRegistryToken The token used to look up the unit of recovery
     *                             interest (URI) token in the native registry. The URI token
     *                             identifies an instance of the resource manager interest in
     *                             a unit of recovery.
     * @param ciRegistryToken  The token used to look up the context interest
     *                             token in the native registry. This token exists if the UR
     *                             is in-doubt, and we created a context interest.
     * @param logOption        The option that indicates how RRS is to process
     *                             log entries for the unit of recovery.
     *
     * @return 0 if the RRS service completed successfully. A number greater
     *         than zero if the RRS service returned a bad return code.
     *         -1 if there was a native failure during the invocation process.
     * @throws RegistryException Thrown if the URI registry token is not recognized.
     */
    public int commitAgentUR(byte[] uriRegistryToken, byte[] ciRegistryToken, int logOption) throws RegistryException;

    /**
     * Tells RRS to initiate and complete a syncpoint operation for the unit of
     * recovery associated with the specified UR interest.
     *
     * @param uriRegistryToken The token used to look up the unit of recovery
     *                             interest (URI) token in the native registry. The URI token
     *                             identifies an instance of the resource manager interest in
     *                             a unit of recovery.
     * @param logOption        The option that indicates how RRS is to process
     *                             log entries for the unit of recovery.
     * @param commitOptions    The option that determines how RRS is to perform
     *                             the delegated commit request.
     *
     * @return 0 if the RRS service completed successfully. A number greater
     *         than zero if the RRS service returned a bad return code.
     *         -1 if there was a native failure during the invocation process.
     * @throws RegistryException Thrown if the URI registry token is not recognized.
     */
    public int delegateCommitAgentUR(byte[] uriRegistryToken,
                                     int logOption,
                                     int commitOptions) throws RegistryException;

    /**
     * Tell RRS to backout the unit of recovery associated with the specified
     * unit of recovery interest.
     *
     * @param uriRegistryToken The token used to look up the unit of recovery
     *                             interest (URI) token in the native registry. The URI token
     *                             identifies an instance of the resource manager interest in
     *                             a unit of recovery.
     * @param ciRegistryToken  The token used to look up the context interest
     *                             token in the native registry. This token exists if the UR
     *                             is in-doubt, and we created a context interest.
     * @param logOption        The option that indicates how RRS is to process
     *                             log entries for the unit of recovery.
     *
     * @return 0 if the RRS service completed successfully. A number greater
     *         than zero if the RRS service returned a bad return code.
     *         -1 if there was a native failure during the invocation process.
     * @throws RegistryException Thrown if the URI registry token is not recognized.
     */
    public int backoutAgentUR(byte[] uriRegistryToken, byte[] ciRegistryToken, int logOption) throws RegistryException;

    /**
     * Tells RRS to delete the SDSRM interest in the specified
     * unit of recovery, and depending on the log_option value,
     * delete any log entries that exist.
     *
     * @param uriRegistryToken The token used to look up the unit of recovery
     *                             interest (URI) token in the native registry. The URI token
     *                             identifies an instance of the resource manager interest in
     *                             a unit of recovery.
     * @param logOption        The option that indicates how RRS is to process
     *                             log entries for the unit of recovery.
     *
     * @return 0 if the RRS service completed successfully. A number greater
     *         than zero if the RRS service returned a bad return code.
     *         -1 if there was a native failure during the invocation process.
     * @throws RegistryException Thrown if the URI registry token is not recognized.
     */
    public int forgetAgentURInterest(byte[] uriRegistryToken, int logOption) throws RegistryException;

    /**
     * Allows the resource manager to initiate asynchronous processing, and
     * return to RRS with a return code that indicates a deferred response.
     *
     * @param uriRegistryToken The token used to look up the unit of recovery
     *                             interest (URI) token in the native registry. The URI token
     *                             identifies an instance of the resource manager interest in
     *                             a unit of recovery.
     * @param exitNumber       The exit number to asynchronously process.
     * @param completionCode   The response code from the asynchronous exit
     *                             routine that has 'completed' processing.
     *
     * @return 0 if the RRS service completed successfully. A number greater
     *         than zero if the RRS service returned a bad return code.
     *         -1 if there was a native failure during the invocation process.
     * @throws RegistryException Thrown if the URI registry token is not recognized.
     */
    public int postDeferredURExit(byte[] uriRegistryToken,
                                  int exitNumber,
                                  int completionCode) throws RegistryException;

    /**
     * Tells RRS how to process an interest in an incomplete unit of recovery.
     *
     * @param uriRegistryToken The token used to look up the unit of recovery
     *                             interest (URI) token in the native registry. The URI token
     *                             identifies an instance of the resource manager interest in
     *                             a unit of recovery.
     * @param responseCode     The code that indicates how RRS is to respond to
     *                             the unit of recovery interest.
     * @param nonPData         The non-persistent interest data.
     *
     * @return 0 if the RRS service completed successfully. A number greater
     *         than zero if the RRS service returned a bad return code.
     *         -1 if there was a native failure during the invocation process.
     * @throws RegistryException Thrown if the URI registry token is not recognized.
     */
    public int respondToRetrievedInterest(byte[] uriRegistryToken,
                                          int responseCode,
                                          byte[] nonPData) throws RegistryException;

    /**
     * Sets persistent interest data for a protected interest in a
     * unit of recovery.
     *
     * @param uriRegistryToken The token used to look up the unit of recovery
     *                             interest (URI) token in the native registry. The URI token
     *                             identifies an instance of the resource manager interest in
     *                             a unit of recovery.
     * @param pdata            The persistent interest data
     *
     * @return 0 if the RRS service completed successfully. A number greater
     *         than zero if the RRS service returned a bad return code.
     *         -1 if there was a native failure during the invocation process.
     * @throws RegistryException Thrown if the URI registry token is not recognized.
     */
    public int setPersistentInterestData(byte[] uriRegistryToken,
                                         byte[] pdata) throws RegistryException;

    /**
     * Defines the role the resource manager will play in processing a UR.
     * It also allows for exit pre-voting.
     *
     * @param uriRegistryToken The token used to look up the unit of recovery
     *                             interest (URI) token in the native registry. The URI token
     *                             identifies an instance of the resource manager interest in
     *                             a unit of recovery.
     * @param prepareExitCode  The code that specifies whether RRS
     *                             is to invoke the prepare exit routine.
     * @param commitExitCode   The code that specifies whether RRS
     *                             is to invoke the commit exit routine.
     * @param backoutExitCode  The code that specifies whether RRS
     *                             is to invoke the backout exit routine.
     * @param role             The role the resource manager is to take for the
     *                             specified UR interest.
     *
     * @return 0 if the RRS service completed successfully. A number greater
     *         than zero if the RRS service returned a bad return code.
     *         -1 if there was a native failure during the invocation process.
     * @throws RegistryException Thrown if the URI registry token is not recognized.
     */
    public int setSyncpointControls(byte[] uriRegistryToken,
                                    int prepareExitCode,
                                    int commitExitCode,
                                    int backoutExitCode,
                                    int role) throws RegistryException;

    /**
     * Sets side information for an interest in a unit of recovery.
     *
     * @param uriRegistryToken The token used to look up the unit of recovery
     *                             interest (URI) token in the native registry. The URI token
     *                             identifies an instance of the resource manager interest in
     *                             a unit of recovery.
     * @param infoIds          The identifiers that set the side information.
     *
     * @return 0 if the RRS service completed successfully. A number greater
     *         than zero if the RRS service returned a bad return code.
     *         -1 if there was a native failure during the invocation process.
     * @throws RegistryException Thrown if the URI registry token is not recognized.
     */
    public int setSideInformation(byte[] uriRegistryToken,
                                  int[] infoIds) throws RegistryException;

    /**
     * Creates a privately managed context.
     *
     * @param rmRegistryToken The token used to look up the token that identifies
     *                            the resource manager on whose behalf the context is to be created.
     *
     * @return The object containing all information returned by the service call or
     *         null if the native method detected an error during the invocation process.
     */
    public BeginContextReturnType beginContext(byte[] rmRegistryToken);

    /**
     * Switches the context on the current thread with the one specified.
     *
     * @param ctxToken The context to be put on the thread.
     *
     * @return The object containing all information returned by the service call or
     *         null if the native method detected an error during the invocation process.
     */
    public SwitchContextReturnType contextSwitch(byte[] ctxToken);

    /**
     * Ends the specified context.
     *
     * @param ctxToken       The context token to end.
     * @param completionType The completion type
     *
     * @return 0 if the RRS service completed successfully. A number greater
     *         than zero if the RRS service returned a bad return code.
     *         -1 if there was a native failure during the invocation process.
     */
    public int endContext(byte[] ctxToken, int completionType);

    /**
     * Retrieves the context token for the context currently set onto the
     * calling thread.
     *
     * @return An object encapsulating the return code from the retrieve
     *         current context token service, and the context token (if the
     *         return code was 0).
     */
    public RetrieveCurrentContextTokenReturnType retrieveCurrentContextToken();

    /**
     * Sets the resource manager information in the METADATA logstream if
     * one is defined and the user under which the server is running is
     * authorized.
     *
     * @param rmRegistryToken The token used to look up the resource manager token
     *                            representing the resource manager on whose behalf the input data is hardened.
     * @param metadata        The information to be logged.
     *
     */
    public int setRMMetadata(byte[] rmRegistryToken, byte[] metadata);

    /**
     * Retrieve the most recently logged information for the resource manager
     * pointed to by the input rmRegistryToken.
     *
     * @param rmRegistryToken The token used to look up the resource manager token
     *                            representing the resource manager on whose data is to be retrieved.
     *
     */
    public RetrieveRMMetadataReturnType retrieveRMMetadata(byte[] rmRegistryToken);
}
