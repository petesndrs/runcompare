
set JSON_JAVA=C:\Users\Pete\Downloads\JSON-java-20250107\src\main\java
dir %JSON_JAVA%
jar -tf  %JSON_JAVA%\json-java.jar

set CLASSPATH=.;%JSON_JAVA%\json-java.jar;
echo %CLASSPATH%


"c:\Program Files\Java\jdk-24\bin\javac" -Xlint -classpath %CLASSPATH% ReadRunData.java
"c:\Program Files\Java\jdk-24\bin\javac" -Xlint RunPageParser.java

"c:\Program Files\Java\jdk-24\bin\java" ReadRunData