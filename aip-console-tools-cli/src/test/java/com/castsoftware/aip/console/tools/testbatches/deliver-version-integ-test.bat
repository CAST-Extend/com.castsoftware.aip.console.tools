@echo off
REM -- Create Application --
SET SERVER_URL=%~1
SET API_KEY=%~2
SET APP_NAME=%~3
SET IN_PLACE_MODE=%~4
SET DOMAIN_NAME=%~5
SET CSS=%~6
set TOOLS_EXTENSION=com.castsoftware.aip.console.tools
set EXTEND_URL=https://extend.castsoftware.com
set PATH=C:\CAST-Caches\Win64;%PATH%

REM EXAMPLE
REM
SET MORE_OPTIONS=
if not "%VERSION_NAME%" == "" SET MORE_OPTIONS=--version-name="%VERSION_NAME%"
if not "%VERSION_DATE%" == "" SET MORE_OPTIONS=%MORE_OPTIONS% --version-date="%VERSION_DATE%"

if not "%IN_PLACE_MODE%" == "" SET MORE_OPTIONS=%MORE_OPTIONS% --no-version-history=%IN_PLACE_MODE%
if not "%CSS%" == "" SET MORE_OPTIONS=%MORE_OPTIONS% --css-server=%CSS%

for %%a in ( SOURCES_ZIP TOOLSDIR EXTEND_API_KEY) do (
	if not defined %%a (
			@echo.
			@echo ERROR : Environment variable %%a should exist as environment var...
			@echo.
			exit /b 1
	)
)

SET TOOLS_CLI_PATH=%TOOLSDIR%\%TOOLS_EXTENSION%
echo -- Delivers a new version to AIP Console --
echo java -jar aip-console-tools-cli.jar deliver --server-url="%SERVER_URL%" --apikey="%API_KEY%" --timeout=5000 ^
--app-name="%APP_NAME%" --verbose=false ^
%MORE_OPTIONS% 
echo --------------------------------

CD /d "%TOOLS_CLI_PATH%"

java -jar aip-console-tools-cli.jar deliver --server-url="%SERVER_URL%" --apikey="%API_KEY%" --timeout=5000 ^
	--app-name="%APP_NAME%" --file="%SOURCES_ZIP%" --verbose=false ^
	%MORE_OPTIONS% 
	
echo exit code=%errorlevel%	

