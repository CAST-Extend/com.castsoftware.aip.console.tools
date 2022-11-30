@echo off
REM -- Publish existing application data to Imaging --
SET SERVER_URL=%~1
SET API_KEY=%~2
SET APP_NAME=%~3
set TOOLS_EXTENSION=com.castsoftware.aip.console.tools
set EXTEND_URL=https://extend.castsoftware.com
set PATH=C:\CAST-Caches\Win64;%PATH%


echo -- Publish existing application data to Imaging --
echo java -jar aip-console-tools-cli.jar Onboard-Application --server-url="%SERVER_URL%" --apikey="%API_KEY%" --timeout=5000 ^
     	--app-name="%APP_NAME%" --file="%SOURCES_ZIP%" --verbose=%VERBOSE% ^
     	%MORE_OPTIONS%
echo --------------------------------
SET TOOLS_CLI_PATH=%TOOLSDIR%\%TOOLS_EXTENSION%
CD /d "%TOOLS_CLI_PATH%"

java -jar aip-console-tools-cli.jar Onboard-Application --server-url="%SERVER_URL%" --apikey="%API_KEY%" --timeout=5000 ^
	--app-name="%APP_NAME%" --file="%SOURCES_ZIP%" --verbose=false ^
	%MORE_OPTIONS%

echo exit code=%errorlevel%

