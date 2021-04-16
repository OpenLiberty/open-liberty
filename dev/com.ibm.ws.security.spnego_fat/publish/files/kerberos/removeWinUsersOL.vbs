Dim ComputerName, userGroup, objGroup, objUser, ArgObj, user, password, group, inputFile, isUserType, isFileType, isDeleteGroup, host

Set ArgObj = WScript.Arguments

If Wscript.Arguments.Count = 0 Then
	call usage()
End If

isUserType = false
isFileType = false
'set initial value for group in case -user & -password are specified without group name.
group = "none"
index = 0
for each arg In ArgObj
	'wsh.echo "index: "&index
	if arg = "-user" then
		isUserType = true
		if isFileType = true then
			wsh.echo "-user and -file cannot be used together"
			call usage()
		end if
		user = getArgValue(index)
		wsh.echo "user: ***"
	elseif arg = "-password" then
		password = getArgValue(index)
		wsh.echo "password: ***"
	elseif arg = "-group" then
		group = getArgValue(index)
		wsh.echo "group: " + group
	elseif arg = "-file" then
		isFileType = true
		if isUserType = true then
			wsh.echo "-user and -file cannot be used together"
			call usage()
		end if
		inputFile = getArgValue(index)
		wsh.echo "file: " + inputFile
	elseif arg = "-deleteGroups" then
		isDeleteGroup = true
		wsh.echo "deleteGroup: " &isDeleteGroup
	elseif arg = "-host" then
		host = getArgValue(index)
		wsh.echo "host: ***"
	end if
	index = index + 1
next

if isDeleteGroup = false then
	wsh.echo "No groups will be deleted since -deleteGroups was not specified"
end if

rem Set objService = GetObject("winmgmts://.")
rem Set oCmdLib = CreateObject( "Microsoft.CmdLib" )
rem ComputerName = oCmdLib.gethostname (objService)
ComputerName = host
wsh.echo "Computername: " + ComputerName

rem Set userGroup = GetObject("WinNT://"&Computername&"/Users,group")
Set Computer= GetObject("WinNT://"&Computername&",computer")
wsh.echo "Retrieved Computer.Name: " + Computer.Name

If (isUserType) Then
	call removeUser (user, group)
ElseIf (isFileType) Then
	Set fso=CreateObject("Scripting.FileSystemObject")
	Set UserFile = fso.OpenTextFile(inputFile,1)
	lineNo=0

	do while UserFile.AtEndOfStream <> True
	 Result = ""

	 Data = UserFile.Readline
	 lineNo = lineNo + 1
	 wsh.echo "Line# "&Chr(13)&lineNo& ": " + Data
	 UserAttributes = Split(Data, " ")

	 user = UserAttributes(0)
	 if isDeleteGroup = true then
	 	group = UserAttributes(2)
	 else
	 	group = "none"
	 end if

	 call removeUser(user, group)

	loop

	UserFile.close
End If

wsh.echo "Done."

Function removeUser(ByRef UserName, ByRef GroupName)
 wsh.echo ">>removeUser"
 userDeleted = false
 groupDeleted = false
 for each UserObject In Computer
  if UCASE(UserObject.name) = UCASE(UserName) then
    wsh.echo "Attempting to delete user " + UserName + "..."
    Computer.Delete "user", UserName
    wsh.echo "User " + UserName + " deleted."
    userDeleted = true
  end if
 next
 if GroupName <> "none" then
  for each GroupObject In Computer
   if UCASE(GroupObject.name) = UCASE(GroupName) then
     Computer.Delete "group", GroupName
     wsh.echo "Group " + GroupName + " deleted."
     groupDeleted = true
   end if
  next
  
  if groupDeleted = false then
  	wsh.echo "group " + GroupName + " not found"
  end if
 end if
 if userDeleted = false then
  wsh.echo "user " + UserName + " not found"
 end if
 wsh.echo "<<removeUser"
End Function

Function getArgValue(ByRef index)
	if Wscript.Arguments.Count > (index + 1) then
		getArgValue = ArgObj(index + 1)
		'Verify a value was given instead of another -option
		dashIndex = InStr(value, "-")
		if dashIndex = 1 then
			call usage()
		end if
	else
		call usage()
	end if
End Function

Function usage()
	Wscript.Echo "Usage: removeWinUsers.bat"
	Wscript.Echo " -file <filename> -deleteGroups| -user <username> -password <password> [-group <group>]"
	Wscript.Quit(1)
End Function