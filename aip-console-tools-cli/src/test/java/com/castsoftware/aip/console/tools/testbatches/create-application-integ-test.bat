@echo off
REM -- Create Application --
SET SERVER_URL=%~1
SET API_KEY=%~2
SET APP_NAME=%~3
SET IN_PLACE_MODE=%~4
SET DOMAIN_NAME=%~5
SET CSS=%~6
SET NODE_NAME=%~7
set TOOLS_EXTENSION=com.castsoftware.aip.console.tools
set EXTEND_URL=https://extend.castsoftware.com
set PATH=C:\CAST-Caches\Win64;%PATH%

REM EXAMPLE
REM create-application-integ-test.bat "download folder" "http://machine.corp.castsoftware.com:8081" "LPZ5i8lJ.5dKr2Y4e39cVIJ70rJETgG0sY29C2ElH" "WEBITOOLS-102-Test-App" false "TOOLS-CLI-TEST_DOM"
REM
SET MORE_OPTIONS=
if not "%NODE_NAME%" == "" SET MORE_OPTIONS=--node-name="%NODE_NAME%"
if not "%DOMAIN_NAME%" == "" SET MORE_OPTIONS=--domain-name="%DOMAIN_NAME%"
if not "%IN_PLACE_MODE%" == "" SET MORE_OPTIONS=%MORE_OPTIONS% --no-version-history=%IN_PLACE_MODE%
if not "%CSS%" == "" SET MORE_OPTIONS=%MORE_OPTIONS% --css-server=%CSS%

if not defined TOOLSDIR (
	set TOOLSDIR=%WORKSPACE%\bin
)

if not exist %TOOLSDIR%\%TOOLS_EXTENSION%.zip (
    @echo.
    @echo ERROR : %TOOLSDIR%\%TOOLS_EXTENSION%.zip file should be downloaded from DEV Build job ...
    @echo.
    exit /b -1
)

echo ----------------------------
echo Unzip the downloaded extension
echo 7z.exe x "%TOOLSDIR%\%TOOLS_EXTENSION%.zip" -y -o"%TOOLSDIR%\%TOOLS_EXTENSION%"
echo ----------------------------
7z.exe x "%TOOLSDIR%\%TOOLS_EXTENSION%.zip" -y -o"%TOOLSDIR%\%TOOLS_EXTENSION%"

SET TOOLS_CLI_PATH=%TOOLSDIR%\%TOOLS_EXTENSION%
echo -- Create Application command --
echo java -jar aip-console-tools-cli.jar CreateApplication --server-url="%SERVER_URL%" --apikey="%API_KEY%" --timeout=5000 ^
--app-name="%APP_NAME%" --verbose=false ^
%MORE_OPTIONS% 
echo --------------------------------

CD /d "%TOOLS_CLI_PATH%"

java -jar aip-console-tools-cli.jar CreateApplication --server-url="%SERVER_URL%" --apikey="%API_KEY%" --timeout=5000 ^
	--app-name="%APP_NAME%" --verbose=false ^
	%MORE_OPTIONS% 
	
echo exit code=%errorlevel%	

