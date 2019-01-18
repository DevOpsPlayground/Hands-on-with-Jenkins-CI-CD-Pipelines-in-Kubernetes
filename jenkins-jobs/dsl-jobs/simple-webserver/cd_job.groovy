def ci_job_name = "ci-job-simple-webserver"
def cd_job_name = "cd-job-simple-webserver"

pipelineJob(cd_job_name) {
    logRotator(-1, 30)

    definition {

        parameters {
            runParam('CI_JOB', ci_job_name, "", 'SUCCESSFUL')
        }

        triggers {
            upstream(ci_job_name, 'SUCCESS')
        }

        cps {
            sandbox()
            script(readFileFromWorkspace('jenkins-jobs/pipelines/multi-config-pipeline-kubernetes.groovy'))
        }
    }

    publishers {
        archiveArtifacts{
            pattern('${SERVICE_JOBNAME}_${BUILD_NUMBER}.yaml')
            onlyIfSuccessful()
            fingerprint()
        }
    }
}
