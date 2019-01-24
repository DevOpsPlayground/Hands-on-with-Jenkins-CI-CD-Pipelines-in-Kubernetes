// Global Vars

["test", "staging", "live"].each { environment ->
    node() {

        def ci_job_name = env."CI_JOB_JOBNAME"
        def ci_job_number = env."CI_JOB_NUMBER"
        def graph


        stage("[${environment.toUpperCase()}] Reading manifest") {
            artifact_path = "${env.'JOB_NAME'}_${env.'BUILD-NUMBER'}"
            echo "Saving graph from ${ci_job_name}:${ci_job_number} into ${artifact_path}"

            deleteDir()

            //Artifact
            copyArtifacts(projectName: ci_job_name, selector: specific(ci_job_number), target: artifact_path)
            def files = findFiles(glob: "${artifact_path}/*.yaml")
            echo "Artifact found: ${files[0]}"
            sh "cat ${files[0]}"

            //Read yaml
            graph = readYaml file: "${files[0]}"
        }


        //*************************************** KUBERNETES DEPLOYMENT ***************************************//

        if (graph.'kubernetes-config') {
            stage("[${environment.toUpperCase()}] Kubernetes Deployment") {
                def current_file_path
                def current_file

                dir(environment){

                    dir("kubernetes-config") {
                        deleteDir()

                        sh returnStatus: true, script: "kubectl --kubeconfig /etc/kubernetes/config create namespace ${environment}"
                        // In case you need to use credentials
//                        git branch: "master", credentialsId: "git_token", url: graph.'kubernetes-config'.repo_url
                        git branch: "master", url: graph.'kubernetes-config'.repo_url
                        sh script: "git checkout ${graph.'kubernetes-config'.revision}"


                        ["daemonset", "deployment"].each { resource_type ->
                            current_file_path = "kubernetes/${graph.service.name}/${environment}/${resource_type}.yaml"

                            if (fileExists(current_file_path)) {

                                // Read and replace the bookmarks
                                current_file = readFile current_file_path
                                echo current_file
                                current_file = current_file.replace("SERVICE_NAME", "${graph.service.name}")
                                current_file = current_file.replace("IMAGE_VERSION", "${graph.service.revision}")
                                current_file = current_file.replace("IMAGE", "${graph.service.name}")

                                writeFile file: "resources_${environment}.yaml", text: current_file

                                sh script: "kubectl --kubeconfig /etc/kubernetes/config apply -n ${environment} -f resources_${environment}.yaml"
                            }
                        }
                    }
                }
            }
        }


        //*************************************** TESTS ***************************************//
        if (graph.'kubernetes-config') {
            stage("[${environment.toUpperCase()}] Run Tests") {

                dir(environment){

                    dir("tests") {
                        deleteDir()

//                        git branch: "master", credentialsId: "git_token", url: graph.'kubernetes-config'.repo_url
                        git branch: "master", url: graph.'kubernetes-config'.repo_url

                        sh script: "git checkout ${graph.'kubernetes-config'.revision}"

                        ["daemonset", "deployment"].each { resource_type ->

                            if (fileExists("kubernetes/${graph.service.name}/${resource_type}.yaml")) {

                                if (resource_type == "deployment"){
                                    waitUntil() {
                                        sleep(5)
                                        rollout_status = sh returnStdout: true, script: "kubectl --kubeconfig /etc/kubernetes/config rollout status --watch=false -n ${environment} ${resource_type}/${graph.service.name}"
                                        rollout_status.contains("successfully rolled out")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

