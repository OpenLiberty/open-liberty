/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ffdc;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.ws.logging.internal.impl.IntrospectionLevel;

import test.TestConstants;
import test.common.SharedOutputManager;

public class IntrospectionLevelMemberTest {
    static SharedOutputManager outputMgr;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        // make stdout/stderr "quiet"-- no output will show up for test
        // unless one of the copy methods or documentThrowable is called
        outputMgr = SharedOutputManager.getInstance();
        outputMgr.logTo(TestConstants.BUILD_TMP);
        outputMgr.captureStreams();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        // Make stdout and stderr "normal"
        outputMgr.restoreStreams();

    }

    @After
    public void tearDown() throws Exception {
        outputMgr.resetStreams();
    }

    private static class TestObject {
        short s1;
        int i2;
        long l3;
        float f4;
        double d5;
        char c6;
        byte b7;
        boolean b8;
        String s9;
        Short s10;
        Integer i11;
        Long l12;
        Float f13;
        Double d14;
        Character c15;
        Byte b16;
        Boolean b17;
        Object o18;
        Object o19;
        Object o20;
        Object o21;
        Object o22;
        Object o23;
        Object o24;
        Object o25;
        Object o26;
        Object o27;
        Object o28;
        Object o29;
        Object o30;
        Object o31;
        Object o32;
        Object o33;
        Object o34;
        Object o35;
        Object o36;
        Object o37;
        Object o38;
        Object o39;
        Object o40;
        Object o41;
        Object o42;
        Object o43;
        Object o44;
        Object o45;
        Object o46;
        Object o47;
        Object o48;
        Object o49;
        Object o50;
        Object o51;
        Object o52;
        Object o53;
        Object o54;
        Object o55;
        Object o56;
        Object o57;
        Object o58;
        Object o59;
        Object o60;
        Object o61;
        Object o62;
        Object o63;
        Object o64;
        Object o65;
        Object o66;
        Object o67;
        Object o68;
        Object o69;
        Object o70;
        Object o71;
        Object o72;
        Object o73;
        Object o74;
        Object o75;
        Object o76;
        Object o77;
        Object o78;
        Object o79;
        Object o80;
        Object o81;
        Object o82;
        Object o83;
        Object o84;
        Object o85;
        Object o86;
        Object o87;
        Object o88;
        Object o89;
        Object o90;
        Object o91;
        Object o92;
        Object o93;
        Object o94;
        Object o95;
        Object o96;
        Object o97;
        Object o98;
        Object o99;
        Object o100;
        Object o101;
        Object o102;
        Object o103;
        Object o104;
        Object o105;
        Object o106;
        Object o107;
        Object o108;
        Object o109;
        Object o110;
        Object o111;
        Object o112;
        Object o113;
        Object o114;
        Object o115;
        Object o116;
        Object o117;
        Object o118;
        Object o119;
        Object o120;
        Object o121;
        Object o122;
        Object o123;
        Object o124;
        Object o125;
        Object o126;
        Object o127;
        Object o128;
        Object o129;
        Object o130;
        Object o131;
        Object o132;
        Object o133;
        Object o134;
        Object o135;
        Object o136;
        Object o137;
        Object o138;
        Object o139;
        Object o140;
        Object o141;
        Object o142;
        Object o143;
        Object o144;
        Object o145;
        Object o146;
        Object o147;
        Object o148;
        Object o149;
        Object o150;
        Object o151;
        Object o152;
        Object o153;
        Object o154;
        Object o155;
        Object o156;
        Object o157;
        Object o158;
        Object o159;
        Object o160;
        Object o161;
        Object o162;
        Object o163;
        Object o164;
        Object o165;
        Object o166;
        Object o167;
        Object o168;
        Object o169;
        Object o170;
        Object o171;
        Object o172;
        Object o173;
        Object o174;
        Object o175;
        Object o176;
        Object o177;
        Object o178;
        Object o179;
        Object o180;
        Object o181;
        Object o182;
        Object o183;
        Object o184;
        Object o185;
        Object o186;
        Object o187;
        Object o188;
        Object o189;
        Object o190;
        Object o191;
        Object o192;
        Object o193;
        Object o194;
        Object o195;
        Object o196;
        Object o197;
        Object o198;
        Object o199;
        Object o200;
        Object o201;
        Object o202;
        Object o203;
        Object o204;
        Object o205;
        Object o206;
        Object o207;
        Object o208;
        Object o209;
        Object o210;
        Object o211;
        Object o212;
        Object o213;
        Object o214;
        Object o215;
        Object o216;
        Object o217;
        Object o218;
        Object o219;
        Object o220;
        Object o221;
        Object o222;
        Object o223;
        Object o224;
        Object o225;
        Object o226;
        Object o227;
        Object o228;
        Object o229;
        Object o230;
        Object o231;
        Object o232;
        Object o233;
        Object o234;
        Object o235;
        Object o236;
        Object o237;
        Object o238;
        Object o239;
        Object o240;
        Object o241;
        Object o242;
        Object o243;
        Object o244;
        Object o245;
        Object o246;
        Object o247;
        Object o248;
        Object o249;
        Object o250;
        Object o251;
        Object o252;
        Object o253;
        Object o254;
        Object o255;
        Object o256;
        Object o257;
        Object o258;
        Object o259;
        Object o260;
        Object o261;
        Object o262;
        Object o263;
        Object o264;
        Object o265;
        Object o266;
        Object o267;
        Object o268;
        Object o269;
        Object o270;
        Object o271;
        Object o272;
        Object o273;
        Object o274;
        Object o275;
        Object o276;
        Object o277;
        Object o278;
        Object o279;
        Object o280;
        Object o281;
        Object o282;
        Object o283;
        Object o284;
        Object o285;
        Object o286;
        Object o287;
        Object o288;
        Object o289;
        Object o290;
        Object o291;
        Object o292;
        Object o293;
        Object o294;
        Object o295;
        Object o296;
        Object o297;
        Object o298;
        Object o299;
        Object o300;
        Object o301;
        Object o302;
        Object o303;
        Object o304;
        Object o305;
        Object o306;
        Object o307;
        Object o308;
        Object o309;
        Object o310;
        Object o311;
        Object o312;
        Object o313;
        Object o314;
        Object o315;
        Object o316;
        Object o317;
        Object o318;
        Object o319;
        Object o320;
        Object o321;
        Object o322;
        Object o323;
        Object o324;
        Object o325;
        Object o326;
        Object o327;
        Object o328;
        Object o329;
        Object o330;
        Object o331;
        Object o332;
        Object o333;
        Object o334;
        Object o335;
        Object o336;
        Object o337;
        Object o338;
        Object o339;
        Object o340;
        Object o341;
        Object o342;
        Object o343;
        Object o344;
        Object o345;
        Object o346;
        Object o347;
        Object o348;
        Object o349;
        Object o350;
        Object o351;
        Object o352;
        Object o353;
        Object o354;
        Object o355;
        Object o356;
        Object o357;
        Object o358;
        Object o359;
        Object o360;
        Object o361;
        Object o362;
        Object o363;
        Object o364;
        Object o365;
        Object o366;
        Object o367;
        Object o368;
        Object o369;
        Object o370;
        Object o371;
        Object o372;
        Object o373;
        Object o374;
        Object o375;
        Object o376;
        Object o377;
        Object o378;
        Object o379;
        Object o380;
        Object o381;
        Object o382;
        Object o383;
        Object o384;
        Object o385;
        Object o386;
        Object o387;
        Object o388;
        Object o389;
        Object o390;
        Object o391;
        Object o392;
        Object o393;
        Object o394;
        Object o395;
        Object o396;
        Object o397;
        Object o398;
        Object o399;
        Object o400;
        Object o401;
        Object o402;
        Object o403;
        Object o404;
        Object o405;
        Object o406;
        Object o407;
        Object o408;
        Object o409;
        Object o410;
        Object o411;
        Object o412;
        Object o413;
        Object o414;
        Object o415;
        Object o416;
        Object o417;
        Object o418;
        Object o419;
        Object o420;
        Object o421;
        Object o422;
        Object o423;
        Object o424;
        Object o425;
        Object o426;
        Object o427;
        Object o428;
        Object o429;
        Object o430;
        Object o431;
        Object o432;
        Object o433;
        Object o434;
        Object o435;
        Object o436;
        Object o437;
        Object o438;
        Object o439;
        Object o440;
        Object o441;
        Object o442;
        Object o443;
        Object o444;
        Object o445;
        Object o446;
        Object o447;
        Object o448;
        Object o449;
        Object o450;
        Object o451;
        Object o452;
        Object o453;
        Object o454;
        Object o455;
        Object o456;
        Object o457;
        Object o458;
        Object o459;
        Object o460;
        Object o461;
        Object o462;
        Object o463;
        Object o464;
        Object o465;
        Object o466;
        Object o467;
        Object o468;
        Object o469;
        Object o470;
        Object o471;
        Object o472;
        Object o473;
        Object o474;
        Object o475;
        Object o476;
        Object o477;
        Object o478;
        Object o479;
        Object o480;
        Object o481;
        Object o482;
        Object o483;
        Object o484;
        Object o485;
        Object o486;
        Object o487;
        Object o488;
        Object o489;
        Object o490;
        Object o491;
        Object o492;
        Object o493;
        Object o494;
        Object o495;
        Object o496;
        Object o497;
        Object o498;
        Object o499;
        Object o500;
        Object o501;
        Object o502;
        Object o503;
        Object o504;
        Object o505;
        Object o506;
        Object o507;
        Object o508;
        Object o509;
        Object o510;
        Object o511;
        Object o512;
        Object o513;
        Object o514;
        Object o515;
        Object o516;
        Object o517;
        Object o518;
        Object o519;
        Object o520;
        Object o521;
        Object o522;
        Object o523;
        Object o524;
        Object o525;
        Object o526;
        Object o527;
        Object o528;
        Object o529;
        Object o530;
        Object o531;
        Object o532;
        Object o533;
        Object o534;
        Object o535;
        Object o536;
        Object o537;
        Object o538;
        Object o539;
        Object o540;
        Object o541;
        Object o542;
        Object o543;
        Object o544;
        Object o545;
        Object o546;
        Object o547;
        Object o548;
        Object o549;
        Object o550;
        Object o551;
        Object o552;
        Object o553;
        Object o554;
        Object o555;
        Object o556;
        Object o557;
        Object o558;
        Object o559;
        Object o560;
        Object o561;
        Object o562;
        Object o563;
        Object o564;
        Object o565;
        Object o566;
        Object o567;
        Object o568;
        Object o569;
        Object o570;
        Object o571;
        Object o572;
        Object o573;
        Object o574;
        Object o575;
        Object o576;
        Object o577;
        Object o578;
        Object o579;
        Object o580;
        Object o581;
        Object o582;
        Object o583;
        Object o584;
        Object o585;
        Object o586;
        Object o587;
        Object o588;
        Object o589;
        Object o590;
        Object o591;
        Object o592;
        Object o593;
        Object o594;
        Object o595;
        Object o596;
        Object o597;
        Object o598;
        Object o599;
        Object o600;
        Object o601;
        Object o602;
        Object o603;
        Object o604;
        Object o605;
        Object o606;
        Object o607;
        Object o608;
        Object o609;
        Object o610;
        Object o611;
        Object o612;
        Object o613;
        Object o614;
        Object o615;
        Object o616;
        Object o617;
        Object o618;
        Object o619;
        Object o620;
        Object o621;
        Object o622;
        Object o623;
        Object o624;
        Object o625;
        Object o626;
        Object o627;
        Object o628;
        Object o629;
        Object o630;
        Object o631;
        Object o632;
        Object o633;
        Object o634;
        Object o635;
        Object o636;
        Object o637;
        Object o638;
        Object o639;
        Object o640;
        Object o641;
        Object o642;
        Object o643;
        Object o644;
        Object o645;
        Object o646;
        Object o647;
        Object o648;
        Object o649;
        Object o650;
        Object o651;
        Object o652;
        Object o653;
        Object o654;
        Object o655;
        Object o656;
        Object o657;
        Object o658;
        Object o659;
        Object o660;
        Object o661;
        Object o662;
        Object o663;
        Object o664;
        Object o665;
        Object o666;
        Object o667;
        Object o668;
        Object o669;
        Object o670;
        Object o671;
        Object o672;
        Object o673;
        Object o674;
        Object o675;
        Object o676;
        Object o677;
        Object o678;
        Object o679;
        Object o680;
        Object o681;
        Object o682;
        Object o683;
        Object o684;
        Object o685;
        Object o686;
        Object o687;
        Object o688;
        Object o689;
        Object o690;
        Object o691;
        Object o692;
        Object o693;
        Object o694;
        Object o695;
        Object o696;
        Object o697;
        Object o698;
        Object o699;
        Object o700;
        Object o701;
        Object o702;
        Object o703;
        Object o704;
        Object o705;
        Object o706;
        Object o707;
        Object o708;
        Object o709;
        Object o710;
        Object o711;
        Object o712;
        Object o713;
        Object o714;
        Object o715;
        Object o716;
        Object o717;
        Object o718;
        Object o719;
        Object o720;
        Object o721;
        Object o722;
        Object o723;
        Object o724;
        Object o725;
        Object o726;
        Object o727;
        Object o728;
        Object o729;
        Object o730;
        Object o731;
        Object o732;
        Object o733;
        Object o734;
        Object o735;
        Object o736;
        Object o737;
        Object o738;
        Object o739;
        Object o740;
        Object o741;
        Object o742;
        Object o743;
        Object o744;
        Object o745;
        Object o746;
        Object o747;
        Object o748;
        Object o749;
        Object o750;
        Object o751;
        Object o752;
        Object o753;
        Object o754;
        Object o755;
        Object o756;
        Object o757;
        Object o758;
        Object o759;
        Object o760;
        Object o761;
        Object o762;
        Object o763;
        Object o764;
        Object o765;
        Object o766;
        Object o767;
        Object o768;
        Object o769;
        Object o770;
        Object o771;
        Object o772;
        Object o773;
        Object o774;
        Object o775;
        Object o776;
        Object o777;
        Object o778;
        Object o779;
        Object o780;
        Object o781;
        Object o782;
        Object o783;
        Object o784;
        Object o785;
        Object o786;
        Object o787;
        Object o788;
        Object o789;
        Object o790;
        Object o791;
        Object o792;
        Object o793;
        Object o794;
        Object o795;
        Object o796;
        Object o797;
        Object o798;
        Object o799;
        Object o800;
        Object o801;
        Object o802;
        Object o803;
        Object o804;
        Object o805;
        Object o806;
        Object o807;
        Object o808;
        Object o809;
        Object o810;
        Object o811;
        Object o812;
        Object o813;
        Object o814;
        Object o815;
        Object o816;
        Object o817;
        Object o818;
        Object o819;
        Object o820;
        Object o821;
        Object o822;
        Object o823;
        Object o824;
        Object o825;
        Object o826;
        Object o827;
        Object o828;
        Object o829;
        Object o830;
        Object o831;
        Object o832;
        Object o833;
        Object o834;
        Object o835;
        Object o836;
        Object o837;
        Object o838;
        Object o839;
        Object o840;
        Object o841;
        Object o842;
        Object o843;
        Object o844;
        Object o845;
        Object o846;
        Object o847;
        Object o848;
        Object o849;
        Object o850;
        Object o851;
        Object o852;
        Object o853;
        Object o854;
        Object o855;
        Object o856;
        Object o857;
        Object o858;
        Object o859;
        Object o860;
        Object o861;
        Object o862;
        Object o863;
        Object o864;
        Object o865;
        Object o866;
        Object o867;
        Object o868;
        Object o869;
        Object o870;
        Object o871;
        Object o872;
        Object o873;
        Object o874;
        Object o875;
        Object o876;
        Object o877;
        Object o878;
        Object o879;
        Object o880;
        Object o881;
        Object o882;
        Object o883;
        Object o884;
        Object o885;
        Object o886;
        Object o887;
        Object o888;
        Object o889;
        Object o890;
        Object o891;
        Object o892;
        Object o893;
        Object o894;
        Object o895;
        Object o896;
        Object o897;
        Object o898;
        Object o899;
        Object o900;
        Object o901;
        Object o902;
        Object o903;
        Object o904;
        Object o905;
        Object o906;
        Object o907;
        Object o908;
        Object o909;
        Object o910;
        Object o911;
        Object o912;
        Object o913;
        Object o914;
        Object o915;
        Object o916;
        Object o917;
        Object o918;
        Object o919;
        Object o920;
        Object o921;
        Object o922;
        Object o923;
        Object o924;
        Object o925;
        Object o926;
        Object o927;
        Object o928;
        Object o929;
        Object o930;
        Object o931;
        Object o932;
        Object o933;
        Object o934;
        Object o935;
        Object o936;
        Object o937;
        Object o938;
        Object o939;
        Object o940;
        Object o941;
        Object o942;
        Object o943;
        Object o944;
        Object o945;
        Object o946;
        Object o947;
        Object o948;
        Object o949;
        Object o950;
        Object o951;
        Object o952;
        Object o953;
        Object o954;
        Object o955;
        Object o956;
        Object o957;
        Object o958;
        Object o959;
        Object o960;
        Object o961;
        Object o962;
        Object o963;
        Object o964;
        Object o965;
        Object o966;
        Object o967;
        Object o968;
        Object o969;
        Object o970;
        Object o971;
        Object o972;
        Object o973;
        Object o974;
        Object o975;
        Object o976;
        Object o977;
        Object o978;
        Object o979;
        Object o980;
        Object o981;
        Object o982;
        Object o983;
        Object o984;
        Object o985;
        Object o986;
        Object o987;
        Object o988;
        Object o989;
        Object o990;
        Object o991;
        Object o992;
        Object o993;
        Object o994;
        Object o995;
        Object o996;
        Object o997;
        Object o998;
        Object o999;
        Object o1000;
        Object o1001;
        Object o1002;
        Object o1003;
        Object o1004;
        Object o1005;
        Object o1006;
        Object o1007;
        Object o1008;
        Object o1009;
        Object o1010;
        Object o1011;
        Object o1012;
        Object o1013;
        Object o1014;
        Object o1015;
        Object o1016;
        Object o1017;
        Object o1018;
        Object o1019;
        Object o1020;
        Object o1021;
        Object o1022;
        Object o1023;
        Object o1024;
        Object o1025;
    }

    private static class TestLog implements IncidentStream {
        private static final int DEFAULT_DEPTH = 1;
        private static final int DEFAULT_MAX_SIZE = 1024 * 1024;
        StringBuffer sb = new StringBuffer();

        @Override
        public void write(String text, boolean value) {
            sb.append(text).append("=").append(value);
        }

        @Override
        public void write(String text, byte value) {
            sb.append(text).append("=").append(value);
        }

        @Override
        public void write(String text, char value) {
            sb.append(text).append("=").append(value);
        }

        @Override
        public void write(String text, short value) {
            sb.append(text).append("=").append(value);
        }

        @Override
        public void write(String text, int value) {
            sb.append(text).append("=").append(value);
        }

        @Override
        public void write(String text, long value) {
            sb.append(text).append("=").append(value);
        }

        @Override
        public void write(String text, float value) {
            sb.append(text).append("=").append(value);
        }

        @Override
        public void write(String text, double value) {
            sb.append(text).append("=").append(value);
        }

        @Override
        public void write(String text, String value) {
            sb.append(text).append("=").append(value);
        }

        @Override
        public void write(String text, Object value) {
            sb.append(text).append("=").append(value);
        }

        private void introspect(Object value, int max_depth, int max_size) {
            if (value == null) {
                sb.append("null");
            } else {
                IntrospectionLevel rootLevel = new IntrospectionLevel(value);

                IntrospectionLevel currentLevel = rootLevel;
                IntrospectionLevel nextLevel = rootLevel.getNextLevel();
                int totalBytes = currentLevel.getNumberOfBytesinJustThisLevel();
                int actualDepth = 0;
                while (actualDepth < max_depth && nextLevel.hasMembers() && totalBytes <= max_size) {
                    totalBytes -= currentLevel.getNumberOfBytesinJustThisLevel();
                    totalBytes += currentLevel.getNumberOfBytesInAllLevelsIncludingThisOne();
                    currentLevel = nextLevel;
                    nextLevel = nextLevel.getNextLevel();
                    totalBytes += currentLevel.getNumberOfBytesinJustThisLevel();
                    actualDepth++;
                }
                boolean exceededMaxBytes = false;
                if (totalBytes > max_size && actualDepth > 0) {
                    actualDepth--;
                    exceededMaxBytes = true;
                }
                rootLevel.print(this, actualDepth);
                if (exceededMaxBytes == true) {
                    sb.append("Only " +
                              actualDepth +
                              " levels of object introspection were performed because performing the next level would have exceeded the specified maximum bytes of " + max_size);
                    sb.append("\n");
                }
            }
        }

        @Override
        public void introspectAndWrite(String text, Object value) {
            sb.append(text).append("\n");
            introspect(value, DEFAULT_DEPTH, DEFAULT_MAX_SIZE);
        }

        @Override
        public void introspectAndWrite(String text, Object value, int depth) {
            sb.append(text).append("\n");
            introspect(value, depth, DEFAULT_MAX_SIZE);
        }

        @Override
        public void introspectAndWrite(String text, Object value, int depth, int maxBytes) {
            sb.append(text).append("\n");
            introspect(value, depth, maxBytes);
        }

        @Override
        public void writeLine(String text, boolean value) {
            write(text, value);
            sb.append("\n");
        }

        @Override
        public void writeLine(String text, byte value) {
            write(text, value);
            sb.append("\n");
        }

        @Override
        public void writeLine(String text, char value) {
            write(text, value);
            sb.append("\n");
        }

        @Override
        public void writeLine(String text, short value) {
            write(text, value);
            sb.append("\n");
        }

        @Override
        public void writeLine(String text, int value) {
            write(text, value);
            sb.append("\n");
        }

        @Override
        public void writeLine(String text, long value) {
            write(text, value);
            sb.append("\n");
        }

        @Override
        public void writeLine(String text, float value) {
            write(text, value);
            sb.append("\n");
        }

        @Override
        public void writeLine(String text, double value) {
            write(text, value);
            sb.append("\n");
        }

        @Override
        public void writeLine(String text, String value) {
            write(text, value);
            sb.append("\n");
        }

        @Override
        public void writeLine(String text, Object value) {
            write(text, value);
            sb.append("\n");
        }

        @Override
        public void introspectAndWriteLine(String text, Object value) {
            introspectAndWrite(text, value);
            sb.append("\n");
        }

        @Override
        public void introspectAndWriteLine(String text, Object value, int depth) {
            introspectAndWrite(text, value, depth);
            sb.append("\n");
        }

        @Override
        public void introspectAndWriteLine(String text, Object value, int depth, int maxBytes) {
            introspectAndWrite(text, value, depth, maxBytes);
            sb.append("\n");
        }

        @Override
        public String toString() {
            return sb.toString();
        }
    }

    @Test
    public void testArray1023() {
        final String m = "testArray1023";
        IntrospectionLevel il = new IntrospectionLevel(new Object[1023]);
        TestLog log = new TestLog();
        il.print(log, 1);
        String logContent = log.toString();
        try {
            assertTrue("Log did not contain [1022]", logContent.contains("[1022]"));
            assertFalse("Log contained [1023]", logContent.contains("[1023]"));
            assertFalse("Log contained [...]", logContent.contains("[...]"));
        } catch (AssertionError ae) {
            System.out.println(logContent);
            outputMgr.dumpStreams();
            throw ae;
        }
    }

    @Test
    public void testArray1024() {
        final String m = "testArray1024";
        IntrospectionLevel il = new IntrospectionLevel(new Object[1024]);
        TestLog log = new TestLog();
        il.print(log, 1);
        String logContent = log.toString();
        try {
            assertTrue("Log did not contain [1023]", logContent.contains("[1023]"));
            assertFalse("Log contained [...]", logContent.contains("[...]"));
        } catch (AssertionError ae) {
            System.out.println(logContent);
            outputMgr.dumpStreams();
            throw ae;
        }
    }

    @Test
    public void testArray1025() {
        IntrospectionLevel il = new IntrospectionLevel(new Object[1025]);
        TestLog log = new TestLog();
        il.print(log, 1);
        String logContent = log.toString();
        try {
            assertTrue("Log did not contain [1023]", logContent.contains("[1023]"));
            assertTrue("Log did not contain [...]", logContent.contains("[...]"));
        } catch (AssertionError ae) {
            System.out.println(logContent);
            outputMgr.dumpStreams();
            throw ae;
        }
    }

    @Test
    public void testPrimitiveArray1023() {
        IntrospectionLevel il = new IntrospectionLevel(new int[1023]);
        TestLog log = new TestLog();
        il.print(log, 1);
        String logContent = log.toString();
        try {
            assertTrue("Log did not contain [0..1022]={0,0,0", logContent.contains("[0..1022]={0,0,0"));
            assertTrue("Log did not contain 1023", logContent.contains("array length = 1023"));
            assertFalse("Log contained ...", logContent.contains("..."));
        } catch (AssertionError ae) {
            System.out.println(logContent);
            outputMgr.dumpStreams();
            throw ae;
        }
    }

    @Test
    public void testPrimitive1024() {
        IntrospectionLevel il = new IntrospectionLevel(new short[1024]);
        TestLog log = new TestLog();
        il.print(log, 1);
        String logContent = log.toString();
        try {
            assertTrue("Log did not contain [0..1023]={0,0,0", logContent.contains("[0..1023]={0,0,0"));
            assertTrue("Log did not contain 1024", logContent.contains("array length = 1024"));
            assertFalse("Log contained ...", logContent.contains("..."));
        } catch (AssertionError ae) {
            System.out.println(logContent);
            outputMgr.dumpStreams();
            throw ae;
        }
    }

    @Test
    public void testPrimitive1025() {
        IntrospectionLevel il = new IntrospectionLevel(new byte[1025]);
        TestLog log = new TestLog();
        il.print(log, 1);
        String logContent = log.toString();
        try {
            assertTrue("Log did not contain [0..1024]={0,0,0", logContent.contains("[0..1024]={0,0,0"));
            assertTrue("Log did not contain ...", logContent.contains("..."));
        } catch (AssertionError ae) {
            System.out.println(logContent);
            outputMgr.dumpStreams();
            throw ae;
        }
    }

    @Test
    public void testField1025() {
        final String m = "testField1025";
        IntrospectionLevel il = new IntrospectionLevel(new TestObject());
        TestLog log = new TestLog();
        il.print(log, 1);
        String logContent = log.toString();
        try {
            assertTrue("Log did not contain o1024", logContent.contains("o1024"));
            assertTrue("Log contained ...", logContent.contains("..."));
        } catch (AssertionError ae) {
            System.out.println(logContent);
            outputMgr.dumpStreams();
            throw ae;
        }
    }
}
