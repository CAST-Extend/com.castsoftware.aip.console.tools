@echo off
REM -- Upgrade Application --

SET SERVER_URL=%~1
SET API_KEY=%~2
SET APP_GUID=%~3
set TOOLS_EXTENSION=com.castsoftware.aip.console.tools
set EXTEND_URL=https://extend.castsoftware.com
set PATH=C:\CAST-Caches\Win64;%PATH%

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
echo -- Upgrade Application command --
echo java -jar aip-console-tools-cli.jar UpgradeApplicationJob --server-url="%SERVER_URL%" --apikey="%API_KEY%" --timeout=5000 ^
--app-guid="%APP_GUID%" --verbose=false
echo --------------------------------

CD /d "%TOOLS_CLI_PATH%"

java -jar aip-console-tools-cli.jar UpgradeApplicationJob --server-url="%SERVER_URL%" --apikey="%API_KEY%" --timeout=5000 ^
	--app-guid="%APP_GUID%" --verbose=false
	
echo exit code=%errorlevel%	

