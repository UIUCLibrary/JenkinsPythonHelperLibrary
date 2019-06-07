package org.ds.python

class ToxRunner implements Serializable {
    def steps
    ToxRunner(steps){
        this.steps = steps
        this.WORKSPACE = steps.WORKSPACE
        this.NODE_NAME = steps.NODE_NAME

    }

    def run_tox_test_in_node(pythonToolName, pythonPkgFile, test_args, nodeLabels){
        def stashCode = UUID.randomUUID().toString()
        steps.stash includes: "${pythonPkgFile}", name: "${stashCode}"

        steps.node("${nodeLabels}"){
            def python_home = "${steps.tool pythonToolName}"
            def python_exec = '"' + python_home + "\\python.exe" + '"'

            def python_version = steps.bat(
                    label: "Checking Python version for ${python_exec}",
                    returnStdout: true,
                    script: "@${python_exec} --version").trim()

            if(python_version == ""){
                steps.error("No Python version detected")
            }
            try{
                steps.checkout steps.scm
                steps.withEnv(['VENVPATH=venv']) {
                    steps.bat(label: "Create virtualenv based on ${python_version} on ${NODE_NAME} node",
                            script: "${python_exec} -m venv %VENVPATH%"
                    )
                    steps.bat(label: "Update pip version in virtualenv",
                            script: "%VENVPATH%\\Scripts\\python.exe -m pip install pip --upgrade"
                    )


                    steps.bat(label: "Install Tox in virtualenv",
                            script: "%VENVPATH%\\Scripts\\pip install tox"
                    )

                    steps.unstash "${stashCode}"
                    steps.bat(label: "Testing ${pythonPkgFile}",
                            script: "%VENVPATH%\\Scripts\\tox.exe -c ${WORKSPACE}/tox.ini --parallel=auto -o --workdir=${WORKSPACE}/tox --installpkg=${pythonPkgFile} ${test_args} -vv"
                    )
                }
            }
            finally{
                steps.deleteDir()
            }
        }
    }

}