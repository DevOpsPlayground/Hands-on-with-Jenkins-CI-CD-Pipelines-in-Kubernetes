FROM alpine

RUN apk add python curl

ADD index.html /code/

EXPOSE 8080
WORKDIR /code
CMD ["/usr/bin/python","-m", "SimpleHTTPServer","8080"]
