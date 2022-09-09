@echo off
REM -- Analyses an existing version on AIP Console Application --
SET SERVER_URL=%~1
SET API_KEY=%~2
SET APP_NAME=%~3
SET VERSION_NAME=%~4

set TOOLS_EXTENSION=com.castsoftware.aip.console.tools
set EXTEND_URL=https://extend.castsoftware.com
set PATH=C:\CAST-Caches\Win64;%PATH%

REM EXAMPLE
REM call "analyzer-version-integ-test.bat" "server-url" "api-key" "app-name" "version-name"
REM
SET MORE_OPTIONS=
if "%SHOW_SQL%" == "true" SET MORE_OPTIONS=%MORE_OPTIONS% --show-sql
if "%AMT_PROFILING%" == "true" SET MORE_OPTIONS=%MORE_OPTIONS% --amt-profiling"

if not "%MODULE_OPTION%" == "" SET MORE_OPTIONS=%MORE_OPTIONS% --module-option="%MODULE_OPTION%"
if "%UPLOAD_APPLICATION%" == "true" SET MORE_OPTIONS=%MORE_OPTIONS% --upload-application
if "%PROCESS_IMAGING%" == "true" SET MORE_OPTIONS=%MORE_OPTIONS% --process-imaging
if "%WITH_SNAPSHOT%" == "true" SET MORE_OPTIONS=%MORE_OPTIONS% --snapshot


for %%a in ( VERSION_NAME APP_NAME TOOLSDIR) do (
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

echo -- Analyses an existing version on AIP Console --
echo OPTIONS= %MORE_OPTIONS%
echo java -jar aip-console-tools-cli.jar analyze --server-url="%SERVER_URL%" --apikey="%API_KEY%" --timeout=5000 ^
     	--app-name="%APP_NAME%" --version-name="%VERSION_NAME%" --verbose=%VERBOSE% ^
     	%MORE_OPTIONS%
echo --------------------------------
SET TOOLS_CLI_PATH=%TOOLSDIR%\%TOOLS_EXTENSION%
CD /d "%TOOLS_CLI_PATH%"

java -jar aip-console-tools-cli.jar analyze --server-url="%SERVER_URL%" --apikey="%API_KEY%" --timeout=5000 ^
	--app-name="%APP_NAME%" --version-name="%VERSION_NAME%" --verbose=false ^
	%MORE_OPTIONS% 
	
echo exit code=%errorlevel%	

