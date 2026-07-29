@echo off
echo =========================================
echo  ParkPal - Starlight Carnival Build Tool
echo =========================================

REM 1. Check for MySQL Connector JAR
IF NOT EXIST "lib\mysql-connector-j.jar" (
    echo.
    echo [ERROR] MySQL Connector JAR not found!
    echo.
    echo  Please download it from:
    echo  https://dev.mysql.com/downloads/connector/j/
    echo  Choose "Platform Independent" ZIP, extract and copy
    echo  mysql-connector-j-*.jar into the "lib\" folder,
    echo  renaming it to:  lib\mysql-connector-j.jar
    echo.
    pause
    exit /b 1
)

REM 2. Compile
echo.
echo [1/2] Compiling sources...
IF NOT EXIST "out" mkdir out

javac -d out src\starlightcarnival\*.java
IF ERRORLEVEL 1 (
    echo [ERROR] Compilation failed.
    pause
    exit /b 1
)
echo       OK

REM 3. Run
echo [2/2] Launching ParkPal...
echo.
echo  Default DB: localhost:3306 / starlight_carnival
echo  Default login: starlight_guest / guest123
echo.
echo  Override DB connection with:
echo    -Ddb.host=...  -Ddb.port=...  -Ddb.user=...  -Ddb.password=...
echo =========================================

java -cp "out;lib\mysql-connector-j.jar" starlightcarnival.Main %*

pause
