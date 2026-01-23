@echo off
setlocal

cd /d "%~dp0"

echo Building UAT documentation...
docker build -t uat-docs .

echo Extracting PDF...
docker create --name uat-temp uat-docs
docker cp uat-temp:/output/uat-documentation.pdf .
docker rm uat-temp

echo Done! Output: %~dp0uat-documentation.pdf
