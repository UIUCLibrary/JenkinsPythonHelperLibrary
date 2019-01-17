def call(Map args){
    def defaultArgs = [
            labels: "Python3"
    ]

    args = defaultArgs << args
    node(args.labels){
        checkout scm
        script{
            def  command = "\"${args.pythonPath}\\python.exe\" setup.py --name\""
            echo "Running ${command}"
            def pkg_name = bat(returnStdout: true, script: "@${command}").trim()
            deleteDir()
            return pkg_name
        }
    }
}