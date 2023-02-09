@echo off
REM -- Onboard application --
SET SERVER_URL=%~1
SET API_KEY=%~2
SET APP_NAME=%~3
SET DOMAIN_NAME=%~4
set TOOLS_EXTENSION=com.castsoftware.aip.console.tools
set EXTEND_URL=https://extend.castsoftware.com
set PATH=C:\CAST-Caches\Win64;%PATH%

REM
SET MORE_OPTIONS=--file="%SOURCES_ZIP%"
if not "%EXCLUSION_PATTERNS%" == "" SET MORE_OPTIONS=%MORE_OPTIONS% --exclude-patterns="%EXCLUSION_PATTERNS%"
if not "%EXCLUSION_RULES%" == "" SET MORE_OPTIONS=%MORE_OPTIONS% --exclusion-rules="%EXCLUSION_RULES%"

for %%a in ( SOURCES_ZIP TOOLSDIR ) do (
	if not defined %%a (
			@echo.
			@echo ERROR : Environment variable %%a should exist as environment var...
			@echo.
			exit /b 1
	)
)
echo ----------------------------
echo Unzip the downloaded build artifact
echo 7z.exe x "%TOOLSDIR%\%TOOLS_EXTENSION%.zip" -y -o"%TOOLSDIR%\%TOOLS_EXTENSION%"
echo ----------------------------
7z.exe x "%TOOLSDIR%\%TOOLS_EXTENSION%.zip" -y -o"%TOOLSDIR%\%TOOLS_EXTENSION%"

echo -- Onboard application to CAST Imaging Console --
echo OPTIONS= %MORE_OPTIONS%
echo java -jar aip-console-tools-cli.jar Onboard-Application --server-url="%SERVER_URL%" --apikey="%API_KEY%" --timeout=5000 ^
     	--app-name="%APP_NAME%" --verbose=%VERBOSE% ^
     	%MORE_OPTIONS%
echo --------------------------------
SET TOOLS_CLI_PATH=%TOOLSDIR%\%TOOLS_EXTENSION%
CD /d "%TOOLS_CLI_PATH%"

java -jar aip-console-tools-cli.jar Fast-Scan --server-url="%SERVER_URL%" --apikey="%API_KEY%" --timeout=5000 ^
	--app-name="%APP_NAME%" --verbose=false ^
	%MORE_OPTIONS%

echo exit code=%errorlevel%

