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
    def jenkinsAgent = new jenkinsAgentFunctions()
    if(args.containsKey("agent")){
        if(args.agent.containsKey('dockerfile')){
            args.agent.dockerfile.dockerImageName = "${currentBuild.fullProjectName}_${getToxEnv(args)}_${UUID.randomUUID().toString()}".replaceAll("-", "_").replaceAll('/', "_").replaceAll(' ', "").toLowerCase()
        }
    } else {
        missingParams.add("agent")
    }

    if (missingParams.size() > 0){
        error "buildPythonPkg missing required parameters: [${missingParams.join(', ')}]"
    }

    def agentRunner = jenkinsAgent.getAgent(args.clone())
    args.remove('agent')

    def setup = {
        checkout scm
    }
    if (args['buildSetup']){
        setup = args['buildSetup']
        args.remove('buildSetup')
    }

    def cleanup = {}
    def successful = {}
    def failure = {}
    if(args.containsKey('post')){
        if (args['post'].getClass() != LinkedHashMap){
            error "buildPythonPkg argument 'post' incorrect type. Expected: LinkedHashMap. Actual: ${args['post'].getClass()}"
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

    def buildCmd = {
        if(isUnix()){
            sh 'python -m build --outdir ./dist .'
        } else {
            bat 'py -m build --outdir ./dist .'
        }
    }
    if (args.containsKey('buildCmd')) {
        buildCmd = args['buildCmd']
        args.remove('buildCmd')
    }

    def retries = 1
    if (args.containsKey('retries')){
        retries = args['retries']
        args.remove('retries')
    }

    if(args.size() > 0){
        error "buildPythonPkg has invalid params ${args.keySet()} "
    }
    retry(retries) {
        agentRunner {
            setup()
            try {
                buildCmd()
                successful()
            } catch (e) {
                failure()
                throw e
            } finally {
                cleanup()
            }
        }
    }
}