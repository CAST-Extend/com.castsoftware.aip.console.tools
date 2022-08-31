@echo off
REM -- Create Application --
SET TOOLS_CLI_PATH=%~1
SET SERVER_URL=%~2
SET API_KEY=%~3
SET APP_NAME=%~4
SET IN_PLACE_MODE=%~5
SET DOMAIN_NAME=%~6
SET CSS=%~7
set TOOLS_EXTENSION=com.castsoftware.aip.console.tools
set EXTEND_URL=https://extend.castsoftware.com
set PATH=C:\CAST-Caches\Win64;%PATH%

REM EXAMPLE
REM create-application-integ-test.bat "download folder" "http://machine.corp.castsoftware.com:8081" "LPZ5i8lJ.5dKr2Y4e39cVIJ70rJETgG0sY29C2ElH" "WEBITOOLS-102-Test-App" false "TOOLS-CLI-TEST_DOM"
REM
SET MORE_OPTIONS=
if not "%DOMAIN_NAME%" == "" SET MORE_OPTIONS=--domain-name="%DOMAIN_NAME%"
if not "%IN_PLACE_MODE%" == "" SET MORE_OPTIONS=%MORE_OPTIONS% --no-version-history=%IN_PLACE_MODE%
if not "%CSS%" == "" SET MORE_OPTIONS=%MORE_OPTIONS% --css-server=%CSS%

for %%a in (TOOLS_VERSION EXTEND_API_KEY) do (
	if not defined %%a (
			@echo.
			@echo ERROR : Environment variable %%a should exist as environment var...
			@echo.
			exit /b 1
	)
)

if not defined TOOLSDIR (
	set TOOLSDIR=%WORKSPACE%\bin
)
echo -- Creating %TOOLSDIR% folder --
if exist %TOOLSDIR% rmdir /S/Q %TOOLSDIR%
mkdir %TOOLSDIR%

echo ================================
echo -------- Downloading extension from CAST Extend... -----------
echo %TOOLS_EXTENSION% with version %TOOLS_VERSION%
echo.

curl -X GET "%EXTEND_URL%/api/package/download/%TOOLS_EXTENSION%/%TOOLS_VERSION%" -H "x-nuget-apikey: %EXTEND_API_KEY%" --output %TOOLSDIR%\%TOOLS_EXTENSION%.zip
if errorlevel 1 goto endclean

echo ----------------------------
echo Unzip the downloaded extension
echo 7z.exe x -y -o. "%TOOLSDIR%\%TOOLS_EXTENSION%.zip"
echo ----------------------------
7z.exe x "%TOOLSDIR%\%TOOLS_EXTENSION%.zip" -y -o. "%TOOLSDIR%\%TOOLS_EXTENSION%"

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

