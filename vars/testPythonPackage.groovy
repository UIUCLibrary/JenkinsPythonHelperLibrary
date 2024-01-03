import org.ds.python.ToxRunner

def call(Map args){
    echo 'testPythonPackage deprecated: use testPythonPkg command instead'
    def defaultArgs = [
            testNodeLabels: "Python3",
            testEnvs: ["py"],


    ]
    def testRunners = [:]

    args = defaultArgs << args

    def pythonPkgs = findFiles glob: "${args.pkgRegex}"

    def envArg = "-e ${args.testEnvs.join(',')}"

    pythonPkgs.each{ it
        testRunners["Testing ${it}"] ={
            echo "Working on ${it}"
            def runner = new ToxRunner(this)
            runner.run_tox_test_in_node(args.pythonToolName, it, envArg, args.testNodeLabels)

        }
        parallel testRunners
    }
}
