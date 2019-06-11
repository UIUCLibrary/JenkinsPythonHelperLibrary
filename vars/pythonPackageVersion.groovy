def call(Map args){
    def defaultArgs = [
            labels: "Python3"
    ]
    args = defaultArgs << args
    return getPythonVersion(args)
}

def getPythonVersion(Map<String, String> args) {
    echo "Locating Python version ${args.toolName}"
    node(args.labels) {
        checkout scm
        script {
            try {

                def tool_path = tool "${args.toolName}"
                def python_command = "${tool_path}\\python.exe"
                echo "Using ${python_command}"
                def command = "\"${python_command}\" setup.py --version"
                echo "Executing ${command}"
                def pkg_version = bat(returnStdout: true, script: "@${command}").trim()
                return pkg_version
            }
            finally {

                deleteDir()
            }
        }
    }
}