@echo off
REM -- Take a snapshot of an existing version on AIP Console Application --
SET SERVER_URL=%~1
SET API_KEY=%~2
SET APP_NAME=%~3
SET VERSION_NAME=%~4
SET SNAPSHOT_NAME=%~5

set TOOLS_EXTENSION=com.castsoftware.aip.console.tools
set EXTEND_URL=https://extend.castsoftware.com
set PATH=C:\CAST-Caches\Win64;%PATH%

REM EXAMPLE
REM call "snapshot-version-integ-test.bat" "server-url" "api-key" "app-name" "version-name" "snapshot-name"
REM
SET MORE_OPTIONS=
if not "%SNAPSHOT_DATE%" == "" SET MORE_OPTIONS=%MORE_OPTIONS% --snapshot-date="%SNAPSHOT_DATE%"
if "%UPLOAD_APPLICATION%" == "true" SET MORE_OPTIONS=%MORE_OPTIONS% --upload-application
if "%PROCESS_IMAGING%" == "true" SET MORE_OPTIONS=%MORE_OPTIONS% --process-imaging


for %%a in ( VERSION_NAME APP_NAME SNAPSHOT_NAME TOOLSDIR) do (
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

echo --------------------------------
SET TOOLS_CLI_PATH=%TOOLSDIR%\%TOOLS_EXTENSION%
CD /d "%TOOLS_CLI_PATH%"

java -jar aip-console-tools-cli.jar snapshot --server-url="%SERVER_URL%" --apikey="%API_KEY%" --timeout=5000 ^
	--app-name="%APP_NAME%" --version-name="%VERSION_NAME%" --snapshot-name="%SNAPSHOT_NAME%" --verbose=false ^
	%MORE_OPTIONS% 
	
echo exit code=%errorlevel%	

