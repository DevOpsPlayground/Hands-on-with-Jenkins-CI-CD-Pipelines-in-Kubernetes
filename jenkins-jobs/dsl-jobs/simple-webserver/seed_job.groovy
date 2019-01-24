def project_name = "simple-webserver"
def git_url = "https://github.com/ecsdigital/devopsplayground-27-k8s-jenkins-pipeline.git"

freeStyleJob(project_name) {

    logRotator(-1, 30)

    properties{
        copyArtifactPermissionProperty {
            projectNames("ci_job_${project_name}")
        }
        githubProjectUrl(git_url)

    }

    triggers {
        githubPush()
    }

    scm {
        git {
            remote {
                url(git_url)
                name('${JOB_NAME}')
            }
            branch('master')
        }
    }

    steps {

        shell('''
cd service
echo "Release ${BUILD_NUMBER}" >> index.html
docker build -t ${JOB_NAME}:${BUILD_NUMBER} .
        ''')

        shell('''
echo "service:
  name: ${JOB_NAME}
  repo_url : $GIT_URL
  docker_image: ${JOB_NAME}
  revision: ${BUILD_NUMBER}" > metadata.txt
        ''')
    }

    publishers {
        archiveArtifacts{
            pattern('metadata.txt')
            onlyIfSuccessful()
            fingerprint()
        }
    }
}


