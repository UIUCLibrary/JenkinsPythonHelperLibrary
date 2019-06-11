python_package_version = ''

def call(Map args){
    def defaultArgs = [
            labels: "Python3"
    ]
    args = defaultArgs << args

    if(python_package_version?.trip()){
        echo "Reusing ${python_package_version} as version"
        return python_package_version
    }
    python_package_version = getPythonVersion(args)
    return python_package_version
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