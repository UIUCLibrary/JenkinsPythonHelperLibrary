package org.ds.python

class ToxRunner implements Serializable {
    def steps
    ToxRunner(steps){this.steps = steps}

    def run_tox_test_in_node(pythonToolName, pythonPkgFile, test_args, nodeLabels){
        script{
            def stashCode = UUID.randomUUID().toString()
            stash includes: "${pythonPkgFile}", name: "${stashCode}"

            node("${nodeLabels}"){
                def python_home = "${tool pythonToolName}"
                def python_exec = '"' + python_home + "\\python.exe" + '"'

                def python_version = bat(
                        label: "Checking Python version for ${python_exec}",
                        returnStdout: true,
                        script: "@${python_exec} --version").trim()

                if(python_version == ""){
                    error("No Python version detected")
                }
                try{
                    checkout scm
                    withEnv(['VENVPATH=venv']) {
                        bat(label: "Create virtualenv based on ${python_version} on ${NODE_NAME} node",
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

}