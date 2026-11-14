@echo off
setlocal
set "GRADLE_VERSION=8.14.2"
set "GRADLE_HOME=%USERPROFILE%\.gradle\fake-spawner\gradle-%GRADLE_VERSION%"
set "GRADLE_ZIP=%USERPROFILE%\.gradle\fake-spawner\gradle-%GRADLE_VERSION%-bin.zip"
if exist "%GRADLE_HOME%\bin\gradle.bat" goto runGradle
if not exist "%USERPROFILE%\.gradle\fake-spawner" mkdir "%USERPROFILE%\.gradle\fake-spawner"
echo Downloading Gradle %GRADLE_VERSION% ...
powershell -NoProfile -ExecutionPolicy Bypass -Command "$ProgressPreference='SilentlyContinue'; Invoke-WebRequest 'https://services.gradle.org/distributions/gradle-%GRADLE_VERSION%-bin.zip' -OutFile '%GRADLE_ZIP%'; Expand-Archive -Force '%GRADLE_ZIP%' '%USERPROFILE%\.gradle\fake-spawner'"
if errorlevel 1 exit /b %errorlevel%
:runGradle
call "%GRADLE_HOME%\bin\gradle.bat" %*
exit /b %errorlevel%
