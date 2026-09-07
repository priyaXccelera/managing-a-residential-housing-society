@echo off
if not defined SERVER_PORT set SERVER_PORT=29976
for /f "usebackq tokens=1,* delims==" %%A in (.env_842c7de8-cd98-4525-9429-c0943e421fc9) do set %%A=%%B
call gradlew.bat bootJar -q
java -jar build\libs\app-0.1.0.jar --server.port=%SERVER_PORT%
