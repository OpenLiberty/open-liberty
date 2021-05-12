<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<!-- 
This program may be used, executed, copied, modified and distributed
without royalty for the purpose of developing, using, marketing, or distributing.
-->
<HTML>
<HEAD>
<META http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<META name="GENERATOR" content="IBM Software Development Platform">
<TITLE>Interop Scenarios Client</TITLE>
<LINK rel="stylesheet" type="text/css" href="client.css">
</HEAD>
<BODY>

<SCRIPT language="JavaScript" type="text/javascript" src="client.js">
</SCRIPT>

<P><IMG border="0" src="ws_brandmark.gif" width="150" height="21"
	alt=""><IMG border="0" src="ws_mosaic.jpg" width="443"
	height="21" alt=""></P>
<H2>Interop Scenarios Client</H2>
<FORM ID="irtfile" NAME="irtfile" ACTION="UploadIRT" method="POST" enctype="multipart/form-data" onsubmit="return validateField('filedef', 'Filename');">
    <TABLE border="0" cellpadding="0" cellspacing="1" width="100%">
        <TR>
            <TD align="right"><B>IRT Filename:</B></TD>
            <TD WIDTH=8>&nbsp;</TD>
            <TD><B>
                <INPUT type="file" id="filedef" name="filedef" size="64" value="">
                </B>
            </TD>
            <td>
				<INPUT type="submit" value="Load IRT File">
				<INPUT type="button" value="Reset" ONCLICK="window.location='ClientWeb';">
			</td>
        </TR>
        <TR>
        	<TD align="right"><B>Current File:</B></TD>
            <TD WIDTH=8>&nbsp;</TD>
        	<TD>
        		<%=request.getAttribute("irtcount")%> endpoints loaded from <%=request.getAttribute("irtfile")%>
        	</TD> 
        </TR>
    </table>
</FORM>
<FORM id="theform" name="theform"
	action="javascript:checkChecked(theform);doSubmit(theform);">
<TABLE border="0" cellpadding="0" cellspacing="1" width="100%">
	<TBODY>
		<TR>
			<TD>
			<TABLE border="0" cellpadding="0" cellspacing="1" bgcolor="#DDFFDD"
				width="100%">
				<TBODY>
					<TR>
						<TD><FONT size="+1">Scenarios</FONT> [ <A
							href="javascript:selectAll(theform, true);">Select All</A>, <A
							href="javascript:selectAll(theform, false);">Select None</A> ] &nbsp;&nbsp;&nbsp;<input
							id="submit0" name="submit0" type="submit" value="Start Tests"></TD>
					</TR>
					<TR>
						<TD><BR>
						</TD>
					</TR>
					<TR>
						<TD>
						<TABLE border="2" width="100%">
							
							<TR>
								<!-- Start Row -->
								<TD><INPUT type="hidden" id="servlet01"
									value="Client?scenario=<%=request.getAttribute("scenario")%>&soap11&test=ping">
								<TABLE width="100%">
									<TR>
										<TD width="10%"><SPAN id="plus01" class="plussign"
											onclick="javascript:showDiv('details01', 'plus01');">
										+ </SPAN></TD>
										<TD width="1%"><INPUT type="checkbox" name="check01"
											id="check01"></TD>
										<TD colspan="2"><%=request.getAttribute("scenario")%> SOAP 1.1 One-Way - Ping</TD>
										<TD width="10%"><INPUT id="message01" type="text" size="10" value=""></TD>
										<TD width="10%"><SPAN id="stat01"> Not Run </SPAN></TD>
									</TR>
									<TR>
										<TD colspan="5">
										<DIV id='details01' style="display: none">
										<TABLE border="0">
											<TR>
												<TD colspan="2" width="20%" align="right"><B>Loop
												Count:</B></TD>
												<TD><B> <INPUT type="text" id="count01"
													name="count01" size="4"
													value='<%=request.getAttribute("msgcount")%>'> </B></TD>
											</TR>
											<TR>
												<TD colspan="2" width="20%" align="right"><B>Service
												URI:</B></TD>
												<TD><B> <INPUT type="text" id="uri01" name="uri01"
													size="100"
													value='<%=request.getAttribute("interopsoap11ping")%>'>
												</B></TD>
											</TR>
											<TR>
												<TD colspan="2" width="20%" align="right"><B>WSDL
												URI:</B></TD>
												<TD><B> <INPUT type="text" readonly="readonly"
													name="wsdl01" size="100"
													value='<%=request.getAttribute("interopsoap11pingwsdl")%>'>
												</B></TD>
											</TR>
											<TR>
												<TD colspan="3"><TT> <SPAN id="out01"> </SPAN> </TT></TD>
											</TR>

										</TABLE>
										</DIV>
										</TD>
									</TR>

								</TABLE>
								</TD>
							</TR>
							<!-- End Row -->
							<TR>
								<!-- Start Row -->
								<TD><INPUT type="hidden" id="servlet02"
									value="Client?scenario=<%=request.getAttribute("scenario")%>&soap11&test=echo">
								<TABLE width="100%">
									<TR>
										<TD width="10%"><SPAN id="plus02" class="plussign"
											onclick="javascript:showDiv('details02', 'plus02');">
										+ </SPAN></TD>
										<TD width="1%"><INPUT type="checkbox" name="check02"
											id="check02"></TD>
										<TD colspan="2"><%=request.getAttribute("scenario")%> SOAP 1.1 RequestReply Anonymous - Echo</TD>
										<TD width="10%"><INPUT id="message02" type="text" size="10" value=""></TD>
										<TD width="10%"><SPAN id="stat02"> Not Run </SPAN></TD>
									</TR>
									<TR>
										<TD colspan="5">
										<DIV id='details02' style="display: none">
										<TABLE border="0">
											<TR>
												<TD colspan="2" width="20%" align="right"><B>Loop
												Count:</B></TD>
												<TD><B> <INPUT type="text" id="count02"
													name="count02" size="4"
													value='<%=request.getAttribute("msgcount")%>'> </B></TD>
											</TR>
											<TR>
												<TD colspan="2" width="20%" align="right"><B>Service
												URI:</B></TD>
												<TD><B> <INPUT type="text" id="uri02" name="uri02"
													size="100"
													value='<%=request.getAttribute("interopsoap11echo")%>'>
												</B></TD>
											</TR>
											<TR>
												<TD colspan="2" width="20%" align="right"><B>WSDL
												URI:</B></TD>
												<TD><B> <INPUT type="text" readonly="readonly"
													name="wsdl02" size="100"
													value='<%=request.getAttribute("interopsoap11echowsdl")%>'>
												</B></TD>
											</TR>
											<TR>
												<TD colspan="3"><TT> <SPAN id="out02"> </SPAN> </TT></TD>
											</TR>

										</TABLE>
										</DIV>
										</TD>
									</TR>

								</TABLE>
								</TD>
							</TR>
							<!-- End Row -->


							<TR>
								<!-- Start Row -->
								<TD><INPUT type="hidden" id="servlet03"
									value="Client?scenario=<%=request.getAttribute("scenario")%>&soap11&test=async">
								<TABLE width="100%">
									<TR>
										<TD width="10%"><SPAN id="plus03" class="plussign"
											onclick="javascript:showDiv('details03', 'plus03');">
										+ </SPAN></TD>
										<TD width="1%"><INPUT type="checkbox" name="check03"
											id="check03"></TD>
										<TD colspan="2"><%=request.getAttribute("scenario")%> SOAP 1.1 RequestReply Addressable - Async Echo</TD>
										<TD width="10%"><INPUT id="message03" type="text" size="10" value=""></TD>
										<TD width="10%"><SPAN id="stat03"> Not Run </SPAN></TD>
									</TR>
									<TR>
										<TD colspan="5">
										<DIV id='details03' style="display: none">
										<TABLE border="0">
											<TR>
												<TD colspan="2" width="20%" align="right"><B>Loop
												Count:</B></TD>
												<TD><B> <INPUT type="text" id="count03"
													name="count03" size="4"
													value='<%=request.getAttribute("msgcount")%>'> </B></TD>
											</TR>
											<TR>
												<TD colspan="2" width="20%" align="right"><B>Service
												URI:</B></TD>
												<TD><B> <INPUT type="text" id="uri03" name="uri03"
													size="100"
													value='<%=request.getAttribute("interopsoap11async")%>'>
												</B></TD>
											</TR>
											<TR>
												<TD colspan="2" width="20%" align="right"><B>WSDL
												URI:</B></TD>
												<TD><B> <INPUT type="text" readonly="readonly"
													name="wsdl03" size="100"
													value='<%=request.getAttribute("interopsoap11asyncwsdl")%>'>
												</B></TD>
											</TR>
											<TR>
												<TD colspan="3"><TT> <SPAN id="out03"> </SPAN> </TT></TD>
											</TR>

										</TABLE>
										</DIV>
										</TD>
									</TR>

								</TABLE>
								</TD>
							</TR>
							<!-- End Row -->



							<TR>
								<!-- Start Row -->
								<TD><INPUT type="hidden" id="servlet04"
									value="Client?scenario=<%=request.getAttribute("scenario")%>&soap12&test=ping">
								<TABLE width="100%">
									<TR>
										<TD width="10%"><SPAN id="plus04" class="plussign"
											onclick="javascript:showDiv('details04', 'plus04');">
										+ </SPAN></TD>
										<TD width="1%"><INPUT type="checkbox" name="check04"
											id="check04"></TD>
										<TD colspan="2"><%=request.getAttribute("scenario")%> SOAP 1.2 One-Way - Ping</TD>
										<TD width="10%"><INPUT id="message04" type="text" size="10" value=""></TD>
										<TD width="10%"><SPAN id="stat04"> Not Run </SPAN></TD>
									</TR>
									<TR>
										<TD colspan="5">
										<DIV id='details04' style="display: none">
										<TABLE border="0">
											<TR>
												<TD colspan="2" width="20%" align="right"><B>Loop
												Count:</B></TD>
												<TD><B> <INPUT type="text" id="count04"
													name="count04" size="4"
													value='<%=request.getAttribute("msgcount")%>'> </B></TD>
											</TR>
											<TR>
												<TD colspan="2" width="20%" align="right"><B>Service
												URI:</B></TD>
												<TD><B> <INPUT type="text" id="uri04" name="uri04"
													size="100"
													value='<%=request.getAttribute("interopsoap12ping")%>'>
												</B></TD>
											</TR>
											<TR>
												<TD colspan="2" width="20%" align="right"><B>WSDL
												URI:</B></TD>
												<TD><B> <INPUT type="text" readonly="readonly"
													name="wsdl04" size="100"
													value='<%=request.getAttribute("interopsoap12pingwsdl")%>'>
												</B></TD>
											</TR>
											<TR>
												<TD colspan="3"><TT> <SPAN id="out04"> </SPAN> </TT></TD>
											</TR>

										</TABLE>
										</DIV>
										</TD>
									</TR>

								</TABLE>
								</TD>
							</TR>
							<!-- End Row -->

							<TR>
								<!-- Start Row -->
								<TD><INPUT type="hidden" id="servlet05"
									value="Client?scenario=<%=request.getAttribute("scenario")%>&soap12&test=echo">
								<TABLE width="100%">
									<TR>
										<TD width="10%"><SPAN id="plus05" class="plussign"
											onclick="javascript:showDiv('details05', 'plus05');">
										+ </SPAN></TD>
										<TD width="1%"><INPUT type="checkbox" name="check05"
											id="check05"></TD>
										<TD colspan="2"><%=request.getAttribute("scenario")%> SOAP 1.2 RequestReply Anonymous - Echo</TD>
										<TD width="10%"><INPUT id="message05" type="text" size="10" value=""></TD>
										<TD width="10%"><SPAN id="stat05"> Not Run </SPAN></TD>
									</TR>
									<TR>
										<TD colspan="5">
										<DIV id='details05' style="display: none">
										<TABLE border="0">
											<TR>
												<TD colspan="2" width="20%" align="right"><B>Loop
												Count:</B></TD>
												<TD><B> <INPUT type="text" id="count05"
													name="count05" size="4"
													value='<%=request.getAttribute("msgcount")%>'> </B></TD>
											</TR>
											<TR>
												<TD colspan="2" width="20%" align="right"><B>Service
												URI:</B></TD>
												<TD><B> <INPUT type="text" id="uri05" name="uri05"
													size="100"
													value='<%=request.getAttribute("interopsoap12echo")%>'>
												</B></TD>
											</TR>
											<TR>
												<TD colspan="2" width="20%" align="right"><B>WSDL
												URI:</B></TD>
												<TD><B> <INPUT type="text" readonly="readonly"
													name="wsdl05" size="100"
													value='<%=request.getAttribute("interopsoap12echowsdl")%>'>
												</B></TD>
											</TR>
											<TR>
												<TD colspan="3"><TT> <SPAN id="out05"> </SPAN> </TT></TD>
											</TR>

										</TABLE>
										</DIV>
										</TD>
									</TR>

								</TABLE>
								</TD>
							</TR>
							<!-- End Row -->


							<TR>
								<!-- Start Row -->
								<TD><INPUT type="hidden" id="servlet06"
									value="Client?scenario=<%=request.getAttribute("scenario")%>&soap12&test=async">
								<TABLE width="100%">
									<TR>
										<TD width="10%"><SPAN id="plus06" class="plussign"
											onclick="javascript:showDiv('details06', 'plus06');">
										+ </SPAN></TD>
										<TD width="1%"><INPUT type="checkbox" name="check06"
											id="check06"></TD>
										<TD colspan="2"><%=request.getAttribute("scenario")%> SOAP 1.2 RequestReply Addressable - Async Echo</TD>
										<TD width="10%"><INPUT id="message06" type="text" size="10" value=""></TD>
										<TD width="10%"><SPAN id="stat06"> Not Run </SPAN></TD>
									</TR>
									<TR>
										<TD colspan="5">
										<DIV id='details06' style="display: none">
										<TABLE border="0">
											<TR>
												<TD colspan="2" width="20%" align="right"><B>Loop
												Count:</B></TD>
												<TD><B> <INPUT type="text" id="count06"
													name="count06" size="4"
													value='<%=request.getAttribute("msgcount")%>'> </B></TD>
											</TR>
											<TR>
												<TD colspan="2" width="20%" align="right"><B>Service
												URI:</B></TD>
												<TD><B> <INPUT type="text" id="uri06" name="uri06"
													size="100"
													value='<%=request.getAttribute("interopsoap12async")%>'>
												</B></TD>
											</TR>
											<TR>
												<TD colspan="2" width="20%" align="right"><B>WSDL
												URI:</B></TD>
												<TD><B> <INPUT type="text" readonly="readonly"
													name="wsdl06" size="100"
													value='<%=request.getAttribute("interopsoap12asyncwsdl")%>'>
												</B></TD>
											</TR>
											<TR>
												<TD colspan="3"><TT> <SPAN id="out06"> </SPAN> </TT></TD>
											</TR>

										</TABLE>
										</DIV>
										</TD>
									</TR>

								</TABLE>
								</TD>
							</TR>
							<!-- End Row -->
							

						</TABLE>
						</TD>
					</TR> <!-- End Row -->
					
					<TR>
						<TD><BR>
						</TD>
					</TR>	
					
					<TR>
						<TD> [ <A
							href="javascript:selectAll(theform, true);">Select All</A>, <A
							href="javascript:selectAll(theform, false);">Select None</A> ] &nbsp;&nbsp;&nbsp;<input
							id="submit1" name="submit1" type="submit" value="Start Tests"></TD>
					</TR>
				</TBODY>
			</TABLE>
			</TD>
		</TR>
	</TBODY>
</TABLE>
</FORM>
</BODY>
</HTML>
