@echo off
REM -- Publish existing application data to Imaging --
SET SERVER_URL=%~1
SET API_KEY=%~2
SET APP_NAME=%~3
set TOOLS_EXTENSION=com.castsoftware.aip.console.tools.%TOOLS_VERSION%
set EXTEND_URL=https://extend.castsoftware.com
set PATH=C:\CAST-Caches\Win64;%PATH%

for %%a in ( TOOLSDIR ) do (
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
echo Unziping the downloaded build artifact
echo 7z.exe x "%TOOLSDIR%\%TOOLS_EXTENSION%.zip" -y -o"%TOOLSDIR%\%TOOLS_EXTENSION%"
echo ----------------------------
7z.exe x "%TOOLSDIR%\%TOOLS_EXTENSION%.zip" -y -o"%TOOLSDIR%\%TOOLS_EXTENSION%"

echo -- Publish existing application data to Imaging --
echo java -jar aip-console-tools-cli.jar Publish-Imaging --server-url="%SERVER_URL%" --apikey="%API_KEY%" --timeout=5000 ^
     	--app-name="%APP_NAME%" --verbose=%VERBOSE%
echo --------------------------------

SET TOOLS_CLI_PATH=%TOOLSDIR%\%TOOLS_EXTENSION%
CD /d "%TOOLS_CLI_PATH%"

java -jar aip-console-tools-cli.jar Publish-Imaging --server-url="%SERVER_URL%" --apikey="%API_KEY%" --timeout=5000 ^
	--app-name="%APP_NAME%"  --verbose=%VERBOSE%

echo exit code=%errorlevel%

