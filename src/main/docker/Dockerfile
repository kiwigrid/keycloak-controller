FROM gcr.io/distroless/java:11

WORKDIR /kiwigrid

COPY ${project.build.finalName}.jar /kiwigrid/${project.build.finalName}.jar
COPY lib /kiwigrid/lib

CMD [ "/kiwigrid/${project.build.finalName}.jar" ]