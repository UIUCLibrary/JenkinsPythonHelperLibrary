def call(Map args){
    def defaultArgs = [
            labels: "Python3"
    ]

    args = defaultArgs << args
    node(args.labels){
        checkout scm
        script{
            def python_command = "${tool ${args.toolName}}\\python.exe"
            def command = "\"${python_command}\" setup.py --version"
            echo "Executing ${command}"
            def pkg_version = bat(returnStdout: true, script: "@${command}").trim()
            deleteDir()
            return pkg_version
        }
    }
}