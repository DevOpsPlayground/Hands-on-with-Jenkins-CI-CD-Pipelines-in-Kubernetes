# Build with
# docker build --build-arg K8S_TOKEN=$JENKINS_TOKEN -t jenkins:docker .

FROM jenkins/jenkins:lts

USER root

RUN apt update && apt install -y apt-transport-https ca-certificates curl gnupg2 software-properties-common

# Docker
RUN curl -fsSL https://download.docker.com/linux/debian/gpg | apt-key add -
RUN apt-key fingerprint 0EBFCD88
RUN add-apt-repository "deb [arch=amd64] https://download.docker.com/linux/debian $(lsb_release -cs) stable"
RUN apt update && apt install -y docker-ce

# Kubernetes & Openshift CLI
ARG OC_RELEASE=openshift-origin-client-tools-v3.11.0-0cbc58b-linux-64bit
ARG OC_VERSION=v3.11.0

RUN wget https://github.com/openshift/origin/releases/download/$OC_VERSION/$OC_RELEASE.tar.gz
RUN tar xzvf $OC_RELEASE.tar.gz

RUN mv $OC_RELEASE/oc /usr/local/bin/
RUN mv $OC_RELEASE/kubectl /usr/local/bin/

# Kubernetes connection
ARG K8S_SERVER=kubernetes:443
ARG K8S_NAMESPACE=default
ARG K8S_SA=jenkins
ARG K8S_TOKEN=xxxx

RUN mkdir -p /etc/kubernetes/

RUN echo "apiVersion: v1 \n\
clusters: \n\
- cluster: \n\
    insecure-skip-tls-verify: true \n\
    server: https://$K8S_SERVER \n\
  name: $K8S_SERVER \n\
contexts: \n\
- context: \n\
    cluster: $K8S_SERVER \n\
    namespace: $K8S_NAMESPACE \n\
    user: jenkins/$K8S_SERVER \n\
  name: $K8S_NAMESPACE/$K8S_SERVER/$K8S_SA \n\
current-context: $K8S_NAMESPACE/$K8S_SERVER/$K8S_SA \n\
kind: Config \n\
preferences: {} \n\
users: \n\
- name: $K8S_SA/$K8S_SERVER \n\
  user: \n\
    token: $K8S_TOKEN" > /etc/kubernetes/config

RUN chown jenkins: /etc/kubernetes/config

