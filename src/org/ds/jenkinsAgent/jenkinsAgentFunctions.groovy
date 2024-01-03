package org.ds.jenkinsAgent

def getNodeLabel(agent){
    def label
    if (agent.containsKey("dockerfile")){
        return agent.dockerfile.label
    }
    return label
}

def getDockerRuntimeArgs(agent){
    def args
    if (agent.containsKey("dockerfile")){
        if (agent.dockerfile.containsKey("args")){
            return agent.dockerfile.args
        }
    }
    return ""
}

def getAgent(args){
    if (args.agent.containsKey("label")){
        return { inner ->
            node(args.agent.label){
                ws{
                    inner()
                }
            }
        }

    }

    if (args.agent.containsKey("dockerfile")){
        def nodeLabel = getNodeLabel(args.agent)
        def dockerArgs = getDockerRuntimeArgs(args.agent)
        return { inner ->
            node(nodeLabel){
                ws{
                    checkout scm
                    def dockerImage
                    def dockerImageName = args.agent.dockerfile.dockerImageName ? args.agent.dockerfile.dockerImageName : UUID.randomUUID().toString().replaceAll("-", "_").replaceAll('/', "_").replaceAll(' ', "").toLowerCase()
//                    def dockerImageName = "${currentBuild.fullProjectName}_${getToxEnv(args)}_${UUID.randomUUID().toString()}".replaceAll("-", "_").replaceAll('/', "_").replaceAll(' ', "").toLowerCase()
                    lock("docker build-${env.NODE_NAME}"){
                        dockerImage = docker.build(dockerImageName, "-f ${args.agent.dockerfile.filename} ${args.agent.dockerfile.additionalBuildArgs} .")
                    }
                    try{
                        dockerImage.inside(dockerArgs){
                            inner()
                        }
                    } finally{
                        if(isUnix()){
                            sh(
                                    label: "Untagging Docker Image used",
                                    script: "docker image rm --no-prune ${dockerImage.imageName()}",
                                    returnStatus: true
                            )
                        } else {
                            powershell(
                                    label: "Untagging Docker Image used",
                                    script: "docker image rm --no-prune ${dockerImage.imageName()}",
                                    returnStatus: true
                            )
                        }
                    }
                }
            }
        }
    }
    error('Invalid agent type, expect [dockerfile,label]')
}
