:: ====================================================================================================================
:: Build tool for aip console tools nuget extension
:: ====================================================================================================================
@if not defined LOGDEBUG set LOGDEBUG=off
@echo %LOGDEBUG%
SetLocal EnableDelayedExpansion

set RETCODE=1
for /f "delims=/" %%a in ('cd') do set WORKSPACE=%%a
for %%a in (%0) do set CMDDIR=%%~dpa
set CMDPATH=%0

cd /d %WORKSPACE%

:: Checking arguments
set BUILDDIR=
set ACTOOLSDIR=
set PACKDIR=
set BUILDNO=
set NOPUB=false

:LOOP_ARG
    set option=%1
    if not defined option goto CHECK_ARGS
    shift
    set value=%1
    if defined value set value=%value:"=%
    call set %option%=%%value%%
    shift
goto LOOP_ARG

:CHECK_ARGS
if not defined BUILDDIR (
	echo.
	echo No "builddir" defined !
	goto endclean
)
if not defined ACTOOLSDIR (
	echo.
	echo No "ACTOOLSDIR" defined !
	goto endclean
)
if not defined PACKDIR (
	echo.
	echo No "PACKDIR" defined !
	goto endclean
)
if not defined BUILDNO (
	echo.
	echo No "buildno" defined !
	goto endclean
)

for %%a in (%BUILDDIR% %ACTOOLSDIR%) do (
    if not exist %%a (
        echo.
        echo ERROR: Folder %%a does not exist
        goto endclean
    )
)

for %%a in (%PACKDIR%) do (
    if exist %%a rmdir /s /q %%a
    mkdir %%a
    if errorlevel 1 goto endclean
)

set ROBOPT=/ndl /njh /njs /np
robocopy %ROBOPT% %ACTOOLSDIR%\aip-console-tools-cli\target %PACKDIR% aip-console-tools-cli*.jar
if errorlevel 8 goto endclean
robocopy %ROBOPT% %ACTOOLSDIR%\aip-console-jenkins\target %PACKDIR% aip-console-jenkins*.hpi
if errorlevel 8 goto endclean
robocopy %ROBOPT% %ACTOOLSDIR%\nuspec\package_files %PACKDIR% plugin.nuspec
if errorlevel 8 goto endclean
 
:: ========================================================================================
:: Nuget packaging
:: ========================================================================================
set CMD=%BUILDDIR%\nuget_package_basics.bat outdir=%PACKDIR% pkgdir=%PACKDIR% buildno=%BUILDNO% nopub=%NOPUB%
echo Executing command:
echo %CMD%
call %CMD%
if errorlevel 1 goto endclean

for /f "tokens=*" %%a in ('dir /b %PACKDIR%\com.castsoftware.*.nupkg') do set PACKPATH=%PACKDIR%\%%a
if not defined PACKPATH (
	echo .
	echo ERROR: No package was created : file not found %PACKDIR%\com.castsoftware.*.nupkg ...
	goto endclean
)
if not exist %PACKPATH% (
	echo .
	echo ERROR: File not found %PACKPATH% ...
	goto endclean
)

set GROOVYEXE=groovy
%GROOVYEXE% --version 2>nul
if errorlevel 1 set GROOVYEXE="%GROOVY_HOME%\bin\groovy"
%GROOVYEXE% --version 2>nul
if errorlevel 1 (
	echo ERROR: no groovy executable available, need one!
	goto endclean
)

:: ========================================================================================
:: Nuget checking
:: ========================================================================================
set CMD=%GROOVYEXE% %BUILDDIR%\nuget_package_verification.groovy --packpath=%PACKPATH%
echo Executing command:
echo %CMD%
call %CMD%
if errorlevel 1 goto endclean

echo.
echo Extension creation in SUCCESS
set RETCODE=0

:endclean
cd /d %WORKSPACE%
exit /b %RETCODE%

