FROM java:8

ADD target/rebujito.jar /srv/myproj-app.jar
ADD src/main/resources/ /srv/resources/

EXPOSE 3000

CMD ["java", "-cp", "/srv/resources:/srv/myproj-app.jar", "ring.rebujito"]
