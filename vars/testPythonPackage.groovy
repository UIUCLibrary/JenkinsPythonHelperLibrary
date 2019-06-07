def call(Map args){
    def defaultArgs = [
            testNodeLabels: "Python3",
            testEnvs: ["py"],


    ]
    args = defaultArgs << args

    def pythonPkgs = findFiles glob: "${args.pkgRegex}"
    def environments = []

    args.testEnvs.each{
        environments.add("-e ${it}")
    }

    def testEnvironments = environments.join(" ")

    pythonPkgs.each{
        run_tox_test_in_node(args.pythonToolName, it, testEnvironments, args.testNodeLabels)
    }
}

def run_tox_test_in_node(pythonToolName, pythonPkgFile, test_args, nodeLabels){
    script{
        def stashCode = UUID.randomUUID().toString()
        stash includes: "${pythonPkgFile}", name: "${stashCode}"

        node("${nodeLabels}"){
            def python_exec = "tool ${pythonToolName}" + "/python.exe"
            def python_version = bat(
                    label: "Checking Python version for ${python_exec}",
                    returnStdout: true,
                    script: '@python --version').trim()
            try{
                checkout scm
                withEnv(['VENVPATH=venv']) {
                    bat(label: "Create virtualenv based on ${python_version} on ${NODE_NAME}",
                            script: "${python_exec} -m venv %VENVPATH%"
                    )
                    bat(label: "Update pip version in virtualenv",
                            script: "%VENVPATH%\\Scripts\\python.exe -m pip install pip --upgrade"
                    )


                    bat(label: "Install Tox in virtualenv",
                            script: "%VENVPATH%\\Scripts\\pip install tox"
                    )

                    unstash "${stashCode}"
                    bat(label: "Testing ${pythonPkgFile}",
                            script: "%VENVPATH%\\Scripts\\tox.exe -c ${WORKSPACE}/tox.ini --parallel=auto -o --workdir=${WORKSPACE}/tox --installpkg=${pythonPkgFile} ${test_args} -vv"
                    )
                }
            }
            finally{
                deleteDir()
            }
        }
    }
}
