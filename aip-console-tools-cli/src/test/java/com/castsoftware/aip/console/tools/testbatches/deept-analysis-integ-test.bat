@echo off
REM -- Onboard application --
SET SERVER_URL=%~1
SET API_KEY=%~2
SET APP_NAME=%~3
set TOOLS_EXTENSION=com.castsoftware.aip.console.tools.%TOOLS_VERSION%
set EXTEND_URL=https://extend.castsoftware.com
set PATH=C:\CAST-Caches\Win64;%PATH%

REM
SET MORE_OPTIONS=
if not "%SNAPSHOT_NAME%" == "" SET MORE_OPTIONS=%MORE_OPTIONS% --snapshot-name="%SNAPSHOT_NAME%"
if not "%MODULE_GENERATION_TYPE%" == "" SET MORE_OPTIONS=%MORE_OPTIONS% --module-option="%MODULE_GENERATION_TYPE%"
if not "%SLEEP_DURATION%" == "" SET MORE_OPTIONS=%MORE_OPTIONS% --sleep-duration="%SLEEP_DURATION%"

for %%a in ( SOURCES_ZIP TOOLSDIR ) do (
	if not defined %%a (
			@echo.
			@echo ERROR : Environment variable %%a should exist as environment var...
			@echo.
			exit /b 1
	)
)
if not exist %TOOLSDIR%\%TOOLS_EXTENSION%.zip (
 @echo ===== Processing NUPKG artifact version %TOOLS_VERSION% ==========
7z.exe e "%DEV_ARTIFACT%\upload\com.castsoftware.aip.console.tools*.nupkg" -y -o"%TOOLSDIR%" %TOOLS_EXTENSION%.zip
)
echo ----------------------------
echo Unzip the downloaded build artifact
echo 7z.exe x "%TOOLSDIR%\%TOOLS_EXTENSION%.zip" -y -o"%TOOLSDIR%\%TOOLS_EXTENSION%"
echo ----------------------------
7z.exe x "%TOOLSDIR%\%TOOLS_EXTENSION%.zip" -y -o"%TOOLSDIR%\%TOOLS_EXTENSION%"

echo -- Onboard application to CAST Imaging Console --
echo -- DEEP-ANALYSIS
echo OPTIONS= %MORE_OPTIONS%
echo java -jar aip-console-tools-cli.jar Onboard-Application --server-url="%SERVER_URL%" --apikey="%API_KEY%" --timeout=5000 ^
     	--app-name="%APP_NAME%" --verbose=%VERBOSE% ^
     	%MORE_OPTIONS%
echo --------------------------------
SET TOOLS_CLI_PATH=%TOOLSDIR%\%TOOLS_EXTENSION%
CD /d "%TOOLS_CLI_PATH%"

java -jar aip-console-tools-cli.jar Deep-Analyze --server-url="%SERVER_URL%" --apikey="%API_KEY%" --timeout=5000 ^
	--app-name="%APP_NAME%" --verbose=false ^
	%MORE_OPTIONS%

echo exit code=%errorlevel%

