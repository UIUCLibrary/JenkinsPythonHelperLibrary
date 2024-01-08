import org.ds.python.toxFunctions

def call(Map args) {
    def missingRequiredArgs = []
    if( !args.containsKey('envNamePrefix')){
        missingRequiredArgs.add('envNamePrefix')
    }
    if( !args.containsKey('label')){
        missingRequiredArgs.add('label')
    }
    if( !args.containsKey('dockerfile')){
        missingRequiredArgs.add('dockerfile')
    }
    if(missingRequiredArgs.size() > 0){
        error "getToxTestsParallel missing required parameters: [${missingRequiredArgs.join(', ')}]"
    }
    def tox = new toxFunctions()
    return tox.getToxTestsParallel(args)
}