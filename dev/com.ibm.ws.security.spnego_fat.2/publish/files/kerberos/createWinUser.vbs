On Error Resume Next
Dim ComputerName, userGroup, objGroup, objUser, ArgObj, user, password, group, inputFile, outputFile, isUserType, isFileType, host
Dim isEnableTrustedForDelegation, isEnableTrustedForAuthDelegation, isEnableAllowedToDelegate, isEnablePasswordNeverExpires, isDisableKerberosAuthentication

Set ArgObj = WScript.Arguments

If Wscript.Arguments.Count = 0 Then
	call usage()
End If

isUserType = false
isFileType = false
isEnableTrustedForDelegation = false
isEnableTrustedForAuthDelegation = false
isEnableAllowedToDelegate = false
isEnablePasswordNeverExpires = false
isDisableKerberosAuthentication = false
'set initial value for group in case -user & -password are specified without group name.
group = "none"
index = 0
for each arg In ArgObj
	'wsh.echo "index: "&index
	'wsh.echo "arg: "&arg
	if arg = "-user" then
		isUserType = true
		if isFileType = true then
			wsh.echo "-user and -file cannot be used together"
			call usage()
		end if
		user = getArgValue(index)
		wsh.echo "user: ***"
	end if
	if arg = "-password" then
		password = getArgValue(index)
		wsh.echo "password: ***"
	end if
	if arg = "-group" then
		group = getArgValue(index)
		wsh.echo "group: " + group
	end if
	if arg = "-file" then
		isFileType = true
		if isUserType = true then
			wsh.echo "-user and -file cannot be used together"
			call usage()
		end if
		inputFile = getArgValue(index)
		wsh.echo "file: " + inputFile
	end if
	if arg = "-host" then
		host = getArgValue(index)
		wsh.echo "host: ***"
	end if
	if arg = "-enableTrustedForDelegation" then
		isEnableTrustedForDelegation = getArgValue(index)
		wsh.echo "isEnableTrustedForDelegation: " + isEnableTrustedForDelegation
	end if
	if arg = "-enableTrustedForAuthDelegation" then
		isEnableTrustedForAuthDelegation = getArgValue(index)
		wsh.echo "isEnableTrustedForAuthDelegation: " + isEnableTrustedForAuthDelegation
	end if
	if arg = "-enableAllowedToDelegate" then
		isEnableAllowedToDelegate = getArgValue(index)
		wsh.echo "isEnableAllowedToDelegate: " + isEnableAllowedToDelegate
	end if
	if arg = "-enablePasswordNeverExpires" then
		isEnablePasswordNeverExpires = getArgValue(index)
		wsh.echo "isEnablePasswordNeverExpires: " + isEnablePasswordNeverExpires
	end if
	if arg = "-disableKerberosAuthentication" then
		isDisableKerberosAuthentication = getArgValue(index)
		wsh.echo "isDisableKerberosAuthentication: " + isDisableKerberosAuthentication
	end if
	
	index = index + 1
next


Set objService = GetObject("winmgmts://.")
Set oCmdLib = CreateObject( "Microsoft.CmdLib" )
ComputerName = oCmdLib.gethostname (objService)
if ComputerName = "N/A" then
	ComputerName = ""
End If
If ComputerName = "" then
	ComputerName = host

Else
	wsh.echo "hostname discovered dynamically."
End If
wsh.echo "hostname: " + ComputerName

On Error Goto 0

Set userGroup = GetObject("WinNT://"&Computername&"/Users,group")
Set Computer= GetObject("WinNT://"&Computername&",computer")


If (isUserType) Then
	call createUser (user, password, group)
ElseIf (isFileType) Then
'	outputFile = ".\createWinUsers.log"
	Set fso=CreateObject("Scripting.FileSystemObject")
	Set UserFile = fso.OpenTextFile(inputFile,1)
	if Err.Number <> 0 then
		WScript.StdErr.writeline Err.Description
		WScript.Quit(Err.Number)
	end if
'	Set LogFile = fso.OpenTextFile(""&outputFile&"",2,True)
	GroupsCreated=0
	UsersCreated=0
	lineNo=0
	
	do while UserFile.AtEndOfStream <> True
	 Result = ""
	
	 Data = UserFile.Readline
	 lineNo = lineNo + 1
	 wsh.echo "Line# "&Chr(13)&lineNo& ": " + Data
	 UserAttributes = Split(Data, " ")
	
	 UserName = UserAttributes(0)
	 Password = UserAttributes(1)
	 GroupName = UserAttributes(2)
	
	 call createUser (UserName, Password, GroupName)
	
	loop
	
	UserFile.close
'	LogFile.close
End If

Result = "Done adding users."
wsh.echo Result

WScript.Quit(0)

Function createUser(ByRef UserName, ByRef Password, ByRef GroupName)
 CreateUser = True
 CreateGroup = True
 isGroupMember = false

 if GroupName <> "none" then
  isGroupMember = true
  for each GroupObject In Computer
   if UCASE(GroupObject.name) = UCASE(GroupName) then
'    LogFile.WriteLine("Group "&GroupName&" already exists.")
    CreateGroup = false
   end if
  next
 else
  CreateGroup = false
 end if

 if isGroupMember = true then
  if CreateGroup then
   'wsh.echo "Creating group " + GroupName
   Set objGroup = Computer.Create("group", GroupName)
   objGroup.SetInfo
   Result = "Created group "&GroupName
  else
   Set objGroup = GetObject("WinNT://"&Computername&"/"&GroupName&",group")
  end if
 end if
 
 for each UserObject In Computer
  if UCASE(UserObject.name) = UCASE(UserName) then
   Wscript.Echo UserName&": already exists."
'   LogFile.WriteLine(UserName&": already exists.")
   CreateUser = false
  end if
 next

 if CreateUser then
  wsh.echo "Creating user " + UserName
  Set objUser = Computer.Create("user", UserName)
  objUser.SetPassword Password
  objUser.SetInfo
  UserGroup.Add objUser.ADsPath
rem  if GroupName <> "none" then
rem   objGroup.Add objUser.ADsPath
rem   LogFile.WriteLine(UserName&": added to group "&GroupName&".")
rem  end if
  Result = Result + UserName&" was created."
 end if

set objUserLDAP = getObjUserLDAP(UserName)
'Wscript.Echo objUserLDAP.distinguishedName

if isEnableTrustedForDelegation = "true" then
  wsh.echo "Will enable trusted for delegation"
  call enableTrustedForDelegation(objUserLDAP)
end if
if isEnableTrustedForAuthDelegation = "true" then
  wsh.echo "Will enable trusted for auth delegation"
  call enableTrustedForAuthDelegation(objUserLDAP)
end if
if isEnableAllowedToDelegate = "true" then
  wsh.echo "Will enable allowed to delegate"
  call enableAllowedToDelegate(objUserLDAP)
end if
if isEnablePasswordNeverExpires = "true" then
  wsh.echo "Will enable password never expires"
  call enablePasswordNeverExpires(objUserLDAP)
end if
if isDisableKerberosAuthentication = "true" then
  wsh.echo "Will disable Kerberos authentication"
  call disableKerberosAuthentication(objUserLDAP)
end if


' Set objSysInfo = CreateObject("ADSystemInfo")
' Set strDomainOrWorkgroup = objSysInfo.DomainDNSName
' Set objUser = GetObject("WinNT://" & strDomainOrWorkgroup & "/" & _
'    strComputer & "/" & strUser & ",User")


 if isGroupMember then
  set objUser = GetObject("WinNT://"&ComputerName&"/"&UserName&",user")
  'wsh.echo "objUser.ADsPath " + objUser.ADsPath
  if objGroup.IsMember(objUserLDAP.ADsPath) then
    wsh.echo UserName&": is already a member of group "&GroupName&"."
  else
    objGroup.Add objUser.ADsPath
    wsh.echo UserName + ": added to group " + GroupName + "."
  end if
 else
   wsh.echo UserName&": not added to a group."
 end if
End Function

Function getObjUserLDAP(ByRef UserName)
  Set objRootDSE = GetObject("LDAP://rootDSE")
  strUser = "LDAP://cn="& UserName &",cn=users," & objRootDSE.Get("defaultNamingContext")
  'Wscript.Echo strUser
  set getObjUserLDAP = getobject(strUser)
  Wscript.Echo "DN: " + getObjUserLDAP.distinguishedName
End Function

Function enableTrustedForDelegation(ByRef objUserLDAP)
  const ADS_UF_ACCOUNT_TRUSTED = &H80000
    
  '<<<<< Enable Account is trusted for delegation >>>>>
  intUAC = objUserLDAP.Get("userAccountControl")
  if  (intUAC AND ADS_UF_ACCOUNT_TRUSTED)=0 Then
    objUserLDAP.put "userAccountControl",  intUAC XOR ADS_UF_ACCOUNT_TRUSTED
    objUserLDAP.setinfo
  end if
  
 Wscript.Echo UserName&": Account is trusted for delegation."
End Function

Function enableTrustedForAuthDelegation(ByRef objUserLDAP)
  const ADS_TRUSTED_TO_AUTH_FOR_DELEGATION = &H1000000
  
    '<<<<< Enable Account is trusted for auth delegation >>>>>
  intUAC = objUserLDAP.Get("userAccountControl")
  if  (intUAC AND ADS_TRUSTED_TO_AUTH_FOR_DELEGATION)=0 Then
    objUserLDAP.put "userAccountControl",  intUAC XOR ADS_TRUSTED_TO_AUTH_FOR_DELEGATION
    objUserLDAP.setinfo
  end if
  
 Wscript.Echo UserName&": Account is trusted for auth delegation."
End Function

Function disableKerberosAuthentication(ByRef objUserLDAP)
	const ADS_UF_KERBEROS_PREAUTH = &H400000
	intUAC = objUserLDAP.Get("userAccountControl")
	'<<<<< In order for the function to work, we need to enable and disable >>>>>
	'<<<<< Enable Do not require Kerberos preauthentication >>>>>
	if  (intUAC AND ADS_UF_KERBEROS_PREAUTH)=0 Then
		objUserLDAP.put "userAccountControl",  intUAC XOR ADS_UF_KERBEROS_PREAUTH
		objUserLDAP.setinfo
	end if
	'<<<<< Disable Do not require Kerberos preauthentication >>>>>
	if intUAC and ADS_UF_KERBEROS_PREAUTH Then
		objUserLDAP.put "userAccountControl",  intUAC XOR ADS_UF_KERBEROS_PREAUTH
		objUserLDAP.setinfo
	end if
End Function

Function enableAllowedToDelegate(ByRef objUserLDAP)
   '<<<<< msDS-AllowedToDelegateTo needs to get set >>>>>
    strdelegateSPN = "HTTP/s4u_backend_service"
    objUserLDAP.put "msDS-AllowedToDelegateTo", strdelegateSPN 
 Wscript.Echo UserName&": allowed to delegate."
End Function

Function enablePasswordNeverExpires(ByRef objUser)
  const ADS_UF_DONT_EXPIRE_PASSWD = &H10000
  intUAC = objUser.Get("userAccountControl")
  
  '<<<<< Enable Password never expires >>>>>
  if  (intUAC AND ADS_UF_DONT_EXPIRE_PASSWD)=0 Then
  	objUser.put "userAccountControl",  intUAC XOR ADS_UF_DONT_EXPIRE_PASSWD
  	objUser.setinfo
  end if
  Wscript.Echo UserName&": Password Never Expires."
End Function


Function getArgValue(ByRef index)
	'wsh.echo ">>getArgValue"
	if Wscript.Arguments.Count > (index + 1) then
		getArgValue = ArgObj(index + 1)
		'Verify a value was given instead of another -option
		dashIndex = InStr(getArgValue, "-")		
		if dashIndex = 1 then
			call usage()
		end if
	else
		call usage()
	end if
	'wsh.echo "<<getArgValue"
End Function

Function usage()
	Wscript.Echo "Usage: createWinUsers.bat"
	Wscript.Echo " -file <filename> | -user <username> -password <password> [-group <group>] [-enableTrustedForDelegation | -enableTrustedForAuthDelegation | -enableAllowedToDelegate | -enablePasswordNeverExpires | -disableKerberosAuthentication]"
	Wscript.Quit(1)
End Function
