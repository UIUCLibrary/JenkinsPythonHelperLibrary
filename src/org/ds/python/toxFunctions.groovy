package org.ds.python

def getToxEnvs(){
    def envs
    if(isUnix()){
        envs = sh(
                label: "Getting Tox Environments",
                returnStdout: true,
                script: "tox -l"
            ).trim().split('\n')
    } else{
        envs = bat(
                label: "Getting Tox Environments",
                returnStdout: true,
                script: "@tox -l"
            ).trim().split('\r\n')
    }
    envs.collect{
        it.trim()
    }
    return envs
}


def getToxTestsParallel(args = [:]){
    def envNamePrefix = args['envNamePrefix']
    def label = args['label']
    def dockerfile = args['dockerfile']
    def dockerArgs = args['dockerArgs']
    def preRunClosure = args['beforeRunning']
    def retries = args.containsKey('retry') ? args.retry : 1
    def dockerRunArgs = args.get('dockerRunArgs', '')

    script{
        def envs = []
        def originalNodeLabel
        def dockerImageName = "${currentBuild.fullProjectName}_tox_${UUID.randomUUID().toString()}".replaceAll("-", "").replaceAll('/', "_").replaceAll(' ', "").toLowerCase()
        retry(retries){
            node(label){
                originalNodeLabel = env.NODE_NAME
                checkout scm
                def dockerImage = docker.build(dockerImageName, "-f ${dockerfile} ${dockerArgs} .")
                try{
                    dockerImage.inside{
                        envs = getToxEnvs()
                    }
                } finally {
                    def untaggingScript = "docker image rm --no-prune ${dockerImage.imageName()}"
                    if(isUnix()){
                        sh(
                            label: "Untagging Docker Image used to run tox",
                            script: untaggingScript
                        )
                    } else {
                        bat(
                            label: "Untagging Docker Image used to run tox",
                            script: untaggingScript
                        )
                    }
                }
            }
        }
        echo "Found tox environments for ${envs.join(', ')}"
        def hosts = [:]
        def jobs = envs.collectEntries({ tox_env ->
            def jenkinsStageName = "${envNamePrefix} ${tox_env}"
            [jenkinsStageName,{
                retry(retries){
                    node(label){
                        if (!hosts.containsKey(env.NODE_NAME)){
                            hosts[env.NODE_NAME] = 0
                        }
                        ws{
                            checkout scm
                            def dockerImageForTesting = docker.build(dockerImageName, "-f ${dockerfile} ${dockerArgs} . ")
                            try{
                                dockerImageForTesting.inside(dockerRunArgs){
                                    hosts[env.NODE_NAME] += 1
                                    if(preRunClosure != null){
                                        preRunClosure()
                                    }
                                    if(isUnix()){
                                        sh(
                                            label: "Running Tox with ${tox_env} environment",
                                            script: "tox -v --workdir=/tmp/tox -e ${tox_env}"
                                        )
                                    } else {
                                        bat(
                                            label: "Running Tox with ${tox_env} environment",
                                            script: "tox -v --workdir=%TEMP%\\tox -e ${tox_env}"
                                        )
                                    }
                                    cleanWs(
                                        deleteDirs: true,
                                        patterns: [
                                            [pattern: ".tox/", type: 'INCLUDE'],
                                        ]
                                    )
                                }
                            } finally {
                                hosts[env.NODE_NAME] -= 1
                                if (hosts[env.NODE_NAME] == 0){
                                    if(isUnix()){
                                        def runningContainers = sh(
                                                                   script: "docker ps --no-trunc --filter ancestor=${dockerImageForTesting.imageName()} --format {{.Names}}",
                                                                   returnStdout: true,
                                                                   )
                                        if (!runningContainers?.trim()) {
                                            sh(
                                                label: "Untagging Docker Image used to run tox",
                                                script: "docker image rm --no-prune ${dockerImageForTesting.imageName()}",
                                                returnStatus: true
                                            )
                                        }
                                        sh(script: "docker ps --no-trunc --filter ancestor=${dockerImageForTesting.imageName()} --format {{.Names}}", returnStatus: true)
                                    } else {
                                        def powershellScript ="${env.WORKSPACE_TMP}/cleanupUnusedDockerImage.ps1"
                                        if (! fileExists(powershellScript) ){
                                            def scriptContent = libraryResource 'org/ds/cleanupUnusedDockerImage.ps1'
                                            writeFile file: powershellScript, text: scriptContent
                                        }
                                        powershell(script: "${powershellScript} ${dockerImageForTesting.imageName()}", returnStatus: true)
                                    }
                                }
                            }
                        }
                    }
                }
            }]
        })
        return jobs
    }
}
return [
        getToxTestsParallel: this.&getToxTestsParallel
]
