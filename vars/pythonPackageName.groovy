def call(Map args){
    def defaultArgs = []

    args = defaultArgs << args
    node("Python3"){
        checkout scm
        script{
            def pkg_name = bat(returnStdout: true, script: "@\"${args.pythonPath}\\python.exe\" setup.py --name").trim()
            deleteDir()
            return pkg_name
        }
    }
}