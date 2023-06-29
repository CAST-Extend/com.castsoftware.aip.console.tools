@echo off
REM -- deliver version to existing Application --
SET SERVER_URL=%~1
SET API_KEY=%~2
SET APP_NAME=%~3
SET IN_PLACE_MODE=%~4
SET DOMAIN_NAME=%~5
SET CSS=%~6
REM SET NODE_NAME=%~7
set TOOLS_EXTENSION=com.castsoftware.aip.console.tools.%TOOLS_VERSION%
set EXTEND_URL=https://extend.castsoftware.com
set PATH=C:\CAST-Caches\Win64;%PATH%

REM EXAMPLE
REM
SET MORE_OPTIONS=
if not "%DOMAIN_NAME%" == "" SET MORE_OPTIONS=--domain-name="%DOMAIN_NAME%"
if not "%NODE_NAME%" == "" SET MORE_OPTIONS=%MORE_OPTIONS% --node-name="%NODE_NAME%"
if not "%VERSION_NAME%" == "" SET MORE_OPTIONS=%MORE_OPTIONS% --version-name="%VERSION_NAME%"
if "%SHOW_SQL%" == "true" SET MORE_OPTIONS=%MORE_OPTIONS% --show-sql
if "%AMT_PROFILING%" == "true" SET MORE_OPTIONS=%MORE_OPTIONS% --amt-profiling"
if not "%VERSION_DATE%" == "" SET MORE_OPTIONS=%MORE_OPTIONS% --version-date="%VERSION_DATE%"
if not "%BACKUP_NAME%" == "" SET MORE_OPTIONS=%MORE_OPTIONS% --backup-name="%BACKUP_NAME%"
if not "%CSS%" == "" SET MORE_OPTIONS=%MORE_OPTIONS% --css-server=%CSS%
if not "%SNAPSHOT_NAME%" == "" SET MORE_OPTIONS=%MORE_OPTIONS% --snapshot-name="%SNAPSHOT_NAME%"

REM Add all boolean
SET MORE_OPTIONS=%MORE_OPTIONS% --backup=%BACKUP% --enable-security-assessment=%SECURITY_ASSESSMENT% --enable-security-dataflow=%SECURITY_DATAFLOW%
SET MORE_OPTIONS=%MORE_OPTIONS% --blueprint=%BLUEPRINT% --auto-create=%AUTO_CREATE%
if "%PROCESS_IMAGING%" == "true" SET MORE_OPTIONS=%MORE_OPTIONS% --process-imaging
if "%UPLOAD_APPLICATION%" == "true" SET MORE_OPTIONS=%MORE_OPTIONS% --upload-application
if not "%MODULE_OPTION%" == "" SET MORE_OPTIONS=%MORE_OPTIONS% --module-option="%MODULE_OPTION%"

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

echo -- Delivers a new version to AIP Console --
echo OPTIONS= %MORE_OPTIONS%
echo java -jar aip-console-tools-cli.jar add --server-url="%SERVER_URL%" --apikey="%API_KEY%" --timeout=5000 ^
     	--app-name="%APP_NAME%" --file="%SOURCES_ZIP%" --verbose=%VERBOSE% ^
     	%MORE_OPTIONS%
echo --------------------------------
SET TOOLS_CLI_PATH=%TOOLSDIR%\%TOOLS_EXTENSION%
CD /d "%TOOLS_CLI_PATH%"

java -jar aip-console-tools-cli.jar add --server-url="%SERVER_URL%" --apikey="%API_KEY%" --timeout=5000 ^
	--app-name="%APP_NAME%" --file="%SOURCES_ZIP%" --verbose=false ^
	%MORE_OPTIONS% 
	
echo exit code=%errorlevel%	

