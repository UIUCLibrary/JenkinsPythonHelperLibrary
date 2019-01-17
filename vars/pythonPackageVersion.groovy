def call(Map args){
    def defaultArgs = []

    args = defaultArgs << args
    node("Python3"){
        checkout scm
        script{
            def pkg_version = bat(returnStdout: true, script: "@\"${args.pythonPath}\\python.exe\" setup.py --version").trim()
            deleteDir()
            return pkg_version
        }
    }
}