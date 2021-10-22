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


:: Checking arguments
set WKSP=
set BUILDDIR=
set ACTOOLSDIR=
set PACKDIR=
set BUILDNB=
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
if not defined WKSP (
	echo.
	echo No "wksp" defined !
	goto endclean
)
if not defined BUILDDIR (
	echo.
	echo No "builddir" defined !
	goto endclean
)
if not defined ACTOOLSDIR (
	echo.
	echo No "actoolsdir" defined !
	goto endclean
)
if not defined PACKDIR (
	echo.
	echo No "packdir" defined !
	goto endclean
)
if not defined BUILDNB (
	echo.
	echo No "buildnb" defined !
	goto endclean
)

set FILESRV=\\productfs01
set ENGBUILD=%FILESRV%\EngBuild
if not defined ENGTOOLS set ENGTOOLS=%FILESRV%\EngTools
set EXTERNAL_TOOLS=%ENGTOOLS%\external_Tools
set CACHEWIN64=c:\CAST-Caches\Win64
set EXTWIN64=%EXTERNAL_TOOLS%\win64
if not exist %CACHEWIN64% set PATH=%EXTWIN64%;%PATH%
if exist %CACHEWIN64%     set PATH=%CACHEWIN64%;%PATH%
set TMPFIC=%TEMP%\build_console_nightly.txt

pushd %WKSP%
if errorlevel 1 goto endclean

for %%a in (%WKSP% %BUILDDIR% %ACTOOLSDIR%) do (
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

pushd %PACKDIR%
set PACKDIR=%CD%
popd

set M3DIR=%WKSP%\Maven3
set REQUIRED_MAVEN="Apache Maven 3.5.0"
if not exist %M3DIR% (
    robocopy /mir /nfl /ndl /np %ENGTOOLS%\external_tools\Maven\M3 %M3DIR%
    if errorlevel 8 goto endclean
)
set PATH=%M3DIR%\bin;%PATH%
call mvn.bat --version  2>&1 | tee.exe %TMPFIC%
@echo %LOGDEBUG%
grep.exe %REQUIRED_MAVEN% %TMPFIC%
if errorlevel 1 (
    @echo.
    @echo ERROR: bad maven version, it should be %REQUIRED_MAVEN%
    goto endclean
)

@echo.
@echo ===============================================
@echo Retrieving version from main pom file
@echo ===============================================
set VERSION=
for /f "delims=<>- tokens=1-4" %%a in ('grep -A1 "<artifactId>aip-console-tools</artifactId>" %ACTOOLSDIR%\pom.xml ^| grep "<version>"') do set VERSION=%%c
if not defined VERSION (
    @echo.
    @echo ERROR: Cannot retrieve version from pom file: %ACTOOLSDIR%\pom.xml
    goto endclean
)
@echo Version is:%VERSION%
set ID=com.castsoftware.aip.console.tools

set MVNOPT=-f %ACTOOLSDIR%\pom.xml -Dmaven.repo.local=%WKSP%\.repository -U -B
if exist %ENGTOOLS%\certificates\settings_maven.xml set MVNOPT=%MVNOPT% -s %ENGTOOLS%\certificates\settings_maven.xml

@echo.
@echo ================================================
@echo Build jar
@echo ================================================
set CMD=mvn.bat clean install -pl "aip-console-tools-core" -DskipTests -Dbuild.number=%BUILDNB% %MVNOPT%
@echo Executing :
@echo %CMD%
call %CMD%
@echo %LOGDEBUG%
if errorlevel 1 goto endclean

set CMD=mvn.bat clean package -DskipTests -Dbuild.number=%BUILDNB% %MVNOPT%
@echo Executing :
@echo %CMD%
call %CMD%
@echo %LOGDEBUG%
if errorlevel 1 goto endclean

@echo.
@echo ================================================
@echo Run tests
@echo ================================================
set CMD=mvn.bat test %MVNOPT%
@echo Executing :
@echo %CMD%
call %CMD%
@echo %LOGDEBUG%
if errorlevel 1 goto endclean

set ZIPNAME=%ID%.%VERSION%.zip
pushd %ACTOOLSDIR%\aip-console-tools-cli\target
7z.exe a -y -r %PACKDIR%/%ZIPNAME% aip-console-tools-cli*.jar
if errorlevel 1 goto endclean
popd
pushd %ACTOOLSDIR%\aip-console-jenkins\target
7z.exe a -y -r %PACKDIR%/%ZIPNAME% aip-console-jenkins*.hpi
if errorlevel 1 goto endclean
popd

call :create_readme >%PACKDIR%\Readme.txt
pushd %PACKDIR%
7z.exe a -y -r %PACKDIR%/%ZIPNAME% Readme.txt
if errorlevel 1 goto endclean
popd

xcopy /f /y %ACTOOLSDIR%\nuget\plugin.nuspec %PACKDIR%
if errorlevel 1 goto endclean

sed -i 's/_THE_VERSION_/%VERSION%/' %PACKDIR%/plugin.nuspec
if errorlevel 1 goto endclean
sed -i 's/_THE_ID_/%ID%/' %PACKDIR%/plugin.nuspec
if errorlevel 1 goto endclean
 
:: ========================================================================================
:: Nuget packaging
:: ========================================================================================
set CMD=%BUILDDIR%\nuget_package_basics.bat outdir=%PACKDIR% pkgdir=%PACKDIR% BUILDNO=%BUILDNB% nopub=%NOPUB% is_component=true
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
goto:eof

:create_readme
    @echo ===============================================================================
    @echo ============ AIP Console Jenkins Plugin and AIP Console Tools CLI =============
    @echo ===============================================================================
    @echo =============== CAST Software Copyright (c) 2020 CAST Software ================
    @echo ===============================================================================
    @echo.
    @echo Version:%VERSION%
    @echo.
    @echo These components aims to help automating application on-boarding and/or analysis in AIP Console.
    @echo.
    @echo If you need more details about the AIP Console Jenkins Plugin, you can check the following page :
    @echo https://github.com/CAST-Extend/com.castsoftware.aip.console.tools/blob/develop/aip-console-jenkins/README.md
    @echo.
    @echo If you need details about the AIP Console Tools CLI, you can find them here :
    @echo https://github.com/CAST-Extend/com.castsoftware.aip.console.tools/blob/develop/aip-console-tools-cli/README.md
    @echo.
    @echo You can find release notes at:
    @echo https://github.com/CAST-Extend/com.castsoftware.aip.console.tools/blob/master/RELEASE-NOTES.md

    exit /b 0
goto:eof

