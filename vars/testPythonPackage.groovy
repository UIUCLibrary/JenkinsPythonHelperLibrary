import org.ds.python.ToxRunner

def call(Map args){
    def defaultArgs = [
            testNodeLabels: "Python3",
            testEnvs: ["py"],


    ]
    args = defaultArgs << args

    def pythonPkgs = findFiles glob: "${args.pkgRegex}"

    def envArg = "-e ${args.testEnvs.join(',')}"

    pythonPkgs.each{
        echo "Working on ${it}"
        def runner = new ToxRunner(this)
        runner.run_tox_test_in_node(args.pythonToolName, it, envArg, args.testNodeLabels)
    }
}
