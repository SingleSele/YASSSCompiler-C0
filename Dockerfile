FROM gradle:jdk14 
WORKDIR /app 
COPY src /app/src
RUN mkdir /app/build
WORKDIR /app/src
RUN javac *.java -d /app/build
WORKDIR /app
RUN jar -cvf minic0.jar -C /app/build .

