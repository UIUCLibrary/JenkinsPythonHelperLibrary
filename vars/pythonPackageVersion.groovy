def call(Map args){
    def defaultArgs = []

    args = defaultArgs << args
    node("Python3"){
        checkout scm
        script{
            def  command = "\"${args.pythonPath}\\python.exe\" setup.py --version"
            echo "Executing ${command}"
            def pkg_version = bat(returnStdout: true, script: "@${command}").trim()
            deleteDir()
            return pkg_version
        }
    }
}