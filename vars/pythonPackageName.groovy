def call(Map args){
    def defaultArgs = [
            labels: "Python3"
    ]
    args = defaultArgs << args
    echo "Locating Python version ${args.toolName}"
    node(args.labels){
        checkout scm
        script{
            try{
                def tool_path = tool "${args.toolName}"
                def python_command = "${tool_path}\\python.exe"
                echo "Using ${python_command}"
                def command = "\"${python_command}\" setup.py --name"
                echo "Executing ${command}"
                def pkg_name = bat(returnStdout: true, script: "@${command}").trim()
                return pkg_name
            }
            finally {
                deleteDir()

            }
        }
    }
}