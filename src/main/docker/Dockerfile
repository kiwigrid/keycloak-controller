FROM openjdk:11-jre-slim

RUN useradd -m -d /kiwigrid -s /bin/bash micronaut
USER micronaut
WORKDIR /kiwigrid

COPY ${project.build.finalName}.jar /kiwigrid/${project.build.finalName}.jar
COPY lib /kiwigrid/lib

ENTRYPOINT [ "java", "-noverify", "-jar", "${project.build.finalName}.jar" ]