# ecsd-kubernetes-jenkins-playground
Resources for [Hands-on with Jenkins CI/CD Pipelines in Kubernetes](https://www.meetup.com/DevOpsPlayground/events/257184190/)

In this repository you can find all the resources required to build the components and run a simple example of CI/CD pipeline with Jenkins on Kubernetes.

### Jenkins-build folder:
It contains all the resources required to build and deploy a Jenkins stand-alone pod in Kubernetes. This custom build pre-installs a few requried tools to be used later in the pipeline, mouns the docker socket from the Kubernetes host, and uses a persisten volume to make the Jenkins configuration persistent.

Also, the rbac.yaml file is responsible to set the required permission to the Jenkins service account that'll be associated to this pod.

We'll build this container using the command:

`docker build --build-arg K8S_TOKEN=xxx -t jenkins:docker .`

Where K8S_TOKEN is the token of the Jenkins service account previously provisioned in Kubernetes

### Jenkins-jobs folder:
It contains the groovy Jenkins job definitions for all the Job used to create the CI/CD pipeline.

This jobs are either freestyle jobs and pipeline jobs. Also a piece of groovy pipeline containing the logic of the pipeline itself will be included from one of the jobs.
You can read more about [Jenkins DSL jobs from the plugin page](https://jenkinsci.github.io/job-dsl-plugin/).
 

### Service folder:
It contains the service resources used to build the artifact to deploy. In this case it's a simple python webserver built with a dockerfile. 
Jenkins will build this service with the command:

`docker build -t ${JOB_NAME}:${BUILD_NUMBER} .`

Where JOB_NAME is automatically replaced with the name of the Jenkins job itself and BUILD_NUMBER is the current build.

### Kubernetes folder:
This last folder contains the kubernetes configuration used to deploy the service. It can generally contains either a deployment definition, a service or a daemonset according to the kind of service we're going to deploy.

It also contains some placeholder (as SERVICE_NAME, IMAGE_VERSION) to be replaced during the pipeline execution with actual build data:
```
apiVersion: apps/v1
kind: Deployment
metadata:
  name: SERVICE_NAME
  labels:
    app: SERVICE_NAME
spec:
  replicas: 1
  strategy:
    type: RollingUpdate
  selector:
    matchLabels:
      app: SERVICE_NAME
  template:
    metadata:
      labels:
        app: SERVICE_NAME
    spec:
      containers:
        - name: SERVICE_NAME
          image: SERVICE_NAME:IMAGE_VERSION
          imagePullPolicy: IfNotPresent
          ports:
            - containerPort: 8080
```
This configuration ideally lives on its own repository and is versioned separately.
For simplicity it's included with in the same repository as the other resources for the scope of this playground.

### Requirements

The bash script used to setup the linux server requirements.

---
# Commands

#### CD repo folder

```
cd devopsplayground-27-k8s-jenkins-pipeline/
```

#### Initialize Kubernetes

```
sudo kubeadm init --pod-network-cidr=10.244.0.0/16
```

#### Configure Kubernetes command line for centos user

```
mkdir -p $HOME/.kube
sudo cp -i /etc/kubernetes/admin.conf $HOME/.kube/config
sudo chown $(id -u):$(id -g) $HOME/.kube/config
```

#### Check what's running on the clustert
```
kubectl cluster-info
kubectl get pods -n kube-system
```

#### Install Flannel

```
kubectl apply -f https://raw.githubusercontent.com/coreos/flannel/master/Documentation/kube-flannel.yml
```

#### Find master node name and make it schedulable
```
export K8S_MASTER=$(kubectl get nodes -o name | cut -d/ -f2)
echo $K8S_MASTER

kubectl describe node $K8S_MASTER

kubectl taint node $K8S_MASTER node-role.kubernetes.io/master:-
```
---

#### Jenkins RBAC Permissions
```
kubectl create -f jenkins-build/rbac.yaml
```

#### Find Jenkins secret token
```
JENKINS_TOKEN=$(kubectl get secrets $(kubectl get sa jenkins -o json|jq -r '.secrets[].name') -o json|jq -r '.data.token'|base64 -d)
echo $JENKINS_TOKEN
```

or use the manual commands
```
kubectl get secrets
kubectl describe secret <jenkins-token-xxxxxxxx>
```

#### Jenkins Build and deploy

```
# Build jenkins docker image
docker build --build-arg K8S_TOKEN=$JENKINS_TOKEN -t jenkins:docker jenkins-build/.

# Deploy Jenkins
kubectl create -f  jenkins-build/deployment.yaml

# Create service
kubectl create -f  jenkins-build/service.yaml
```

#### Find Jenkins admin password

```
# Save Jenkins pod name in env var
export JENKINS_POD=$(kubectl get po -l name=jenkins -o name | cut -d/ -f2)
echo $JENKINS_POD

# Get the admin password from the logs 
kubectl logs -f $JENKINS_POD

# Or from inside the container
kubectl exec $JENKINS_POD -- cat /var/jenkins_home/secrets/initialAdminPassword
```


---

### Jenkins configuration

##### Connect to Jenkins

> http://**<your_hostname_here>**.ldn.devopsplayground.com:**30001**

![unlock](readme_images/jenkins-setup-wizard/1.png?raw=true "unlock")
---

![plugins](readme_images/jenkins-setup-wizard/2.png?raw=true "plugins") 
---

![admin](readme_images/jenkins-setup-wizard/3a.png?raw=true "admin") 

![saveandcontinue](readme_images/jenkins-setup-wizard/3b.png?raw=true "saveandcontinue")
--- 

##### Install additional plugins

```
# Get into the jenkins pod
kubectl exec -ti $JENKINS_POD -- bash

java -jar /var/jenkins_home/war/WEB-INF/jenkins-cli.jar \
    -auth admin:admin \
    -s http://127.0.0.1:8080/ \
    install-plugin copyartifact job-dsl pipeline-utility-steps
```

or from the web interface: *http://**<your_hostname_here>**:30001/**pluginManager/available***

##### Disable security

```
sed -i 's/<useSecurity>true/<useSecurity>false/' /var/jenkins_home/config.xml
```

or from the web interface: *http://**<your_hostname_here>**:30001/**configureSecurity***

##### Restart Jenkins
```
java -jar /var/jenkins_home/war/WEB-INF/jenkins-cli.jar \
    -auth admin:admin \
    -s http://127.0.0.1:8080/ \
    safe-restart
```

##### Jenkins DSL Jobs Automatic Provisioning

![New Item](readme_images/dsl-jobs/1.png?raw=true "New Item")

---

![dsl-jobs](readme_images/dsl-jobs/2.png?raw=true "dsl-jobs")

---
Repository URL: `https://github.com/ecsdigital/devopsplayground-27-k8s-jenkins-pipeline.git`

![scm](readme_images/dsl-jobs/3.png?raw=true "scm")

---

![Build](readme_images/dsl-jobs/4.png?raw=true "Build")

---
DSL Scripts: `jenkins-jobs/dsl-jobs/**/*.groovy`

![Build Dsl Jobs](readme_images/dsl-jobs/5.png?raw=true "Build Dsl Jobs")
---

![Build Now](readme_images/dsl-jobs/6.png?raw=true "Build Now")

### Run the Pipeline

### Interact with the Kuberntes deployments


```
# Find pods in test namespace
kubectl get pods -n test

# Curl the webserver
kubectl -n test exec simple-webserver-xxx-xxx -- curl -s http://127.0.0.1:8080
```








