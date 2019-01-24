#!/bin/bash

# Basic software
yum install -y epel-release
yum install -y curl wget make gcc-c++ git htop vim jq
yum groupinstall -y 'Development Tools'

# SSH User
echo '${ssh_pass}' | sudo passwd centos --stdin
sed -i 's/PasswordAuthentication no/PasswordAuthentication yes/g' /etc/ssh/sshd_config
systemctl restart sshd

# Node and wetty
curl --silent --location https://rpm.nodesource.com/setup_8.x | bash -
yum install -y nodejs

npm install -g yarn
npm install -g aye-spy

yarn global add wetty


cat << EOF > /etc/systemd/system/wetty.service
[Unit]
Description=Wetty terminal on Web
After=network.target

[Service]
Type=simple
User=centos
WorkingDirectory=/home/centos
ExecStart=/usr/local/bin/wetty -p 3000 --sshuser centos
Restart=always

[Install]
WantedBy=multi-user.target
EOF

# SELINUX

setenforce 0
sed -i 's/SELINUX=enforcing/SELINUX=permissive/g' /etc/selinux/config

# Docker
yum-config-manager --add-repo https://download.docker.com/linux/centos/docker-ce.repo
yum install -y --setopt=obsoletes=0 docker-ce-17.03.3.ce-1.el7 docker-ce-selinux.noarch-17.03.3.ce-1.el7

systemctl enable docker
systemctl start docker

usermod -G docker centos

# Kubernetes
cat <<EOF > /etc/yum.repos.d/kubernetes.repo
[kubernetes]
name=Kubernetes
baseurl=https://packages.cloud.google.com/yum/repos/kubernetes-el7-x86_64
enabled=1
gpgcheck=1
repo_gpgcheck=1
gpgkey=https://packages.cloud.google.com/yum/doc/yum-key.gpg https://packages.cloud.google.com/yum/doc/rpm-package-key.gpg
EOF

yum -y install kubelet-1.11.3-0 kubeadm-1.11.3-0 kubectl-1.11.3-0

cat <<EOF >  /etc/sysctl.d/k8s.conf
net.bridge.bridge-nf-call-ip6tables = 1
net.bridge.bridge-nf-call-iptables = 1
EOF

sysctl --system

su - centos -c "git clone https://github.com/ecsdigital/devopsplayground-27-k8s-jenkins-pipeline.git"

# Pre pull images ready to be installed
kubeadm config images pull
docker pull jenkins/jenkins:lts

# Wetty
systemctl enable wetty.service
systemctl start wetty.service














