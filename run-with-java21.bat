@echo off
set JAVA_HOME=C:\Program Files\Java\jdk-21
set PATH=%JAVA_HOME%\bin;%PATH%

echo === Using JAVA from: %JAVA_HOME%
java -version

echo === Budowanie fat-JAR-a...
mvn clean package

if exist target\ocado-app.jar (
    echo === Uruchamianie aplikacji...
    java -jar target\ocado-app.jar C:\Users\jan_b\IdeaProjects\Ocado\src\main\resources\orders.json C:\Users\jan_b\IdeaProjects\Ocado\src\main\resources\paymentmethods.json
) else (
    echo !!! Nie znaleziono JAR-a. Czy pom.xml jest poprawnie skonfigurowany do budowania fat-JAR-a?
)

pause
