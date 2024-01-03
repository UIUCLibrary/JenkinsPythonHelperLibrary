import org.ds.jenkinsAgent.jenkinsAgentFunctions

def getToxEnv(args){
    try{
        def pythonVersion = args.pythonVersion.replace(".", "")
        return "py${pythonVersion}"
    } catch(e){
        return "py"
    }
}


def call(Map args) {
    def missingParams = []
    if(args.containsKey("agent")){
        if(args.agent.containsKey('dockerfile')){
            args.agent.dockerfile.dockerImageName = "${currentBuild.fullProjectName}_${getToxEnv(args)}_${UUID.randomUUID().toString()}".replaceAll("-", "_").replaceAll('/', "_").replaceAll(' ', "").toLowerCase()
        }
    } else {
        missingParams.add("agent")
    }

    def retries = 1
    if (args.containsKey('retries')){
        retries = args['retries']
        args.remove('retries')
    }

    def glob = 'dist/*.tar.gz,dist/*.zip,dist/*.whl'
    if (args.containsKey('glob')){
        glob = args['glob']
        args.remove('glob')
    }

    def testCommand =  {
        def distroFiles = findFiles(glob: glob)
        if (distroFiles.size() == 0){
            error("No files located to test")
        }
        distroFiles.each{
            def toxCommand = "tox --installpkg ${it.path} -e py"
            if(isUnix()){
                sh(label: "Testing tox version", script: "tox --version")
                toxCommand = toxCommand + " --workdir /tmp/tox"
                sh(label: "Running Tox on Python package", script: toxCommand)
            } else{
                bat(label: "Testing tox version", script: "tox --version")
                toxCommand = toxCommand + " --workdir %TEMP%\\tox"
                bat(label: "Running Tox on Python package", script: toxCommand)
            }
        }
    }
    if (args.containsKey('testCommand')){
        testCommand = args['testCommand']
        args.remove('testCommand')
    }
    def setup = {
        checkout scm
    }
    if (args['testSetup']){
        setup = args['testSetup']
        args.remove('testSetup')
    }
    def cleanup = {}
    def successful = {}
    def failure = {}
    if(args.containsKey('post')){
        if (args['post'].getClass() != LinkedHashMap){
            error "testPythonPkg argument 'post' incorrect type. Expected: LinkedHashMap. Actual: ${args['post'].getClass()}"
        }
        if (args['post'].containsKey('cleanup')){
            cleanup = args['post']['cleanup']
        }

        if (args['post'].containsKey('success')){
            successful = args['post']['success']
        }

        if (args['post'].containsKey('failure')){
            failure = args['post']['failure']
        }
        args.remove('post')
    }

    if (missingParams.size() > 0){
        error "testPythonPkg missing required parameters: [${missingParams.join(', ')}]"
    }
    def jenkinsAgent = new jenkinsAgentFunctions()
    def agentRunner = jenkinsAgent.getAgent(args.clone())
    args.remove('agent')

    if(args.size() > 0){
        error "testPythonPkg has invalid params ${args.keySet()} "
    }
    retry(retries){
        agentRunner {
            setup()
            try{
                testCommand()
                successful()
            } catch(e){
                failure()
                throw e
            } finally{
                cleanup()
            }
        }
    }
}
