import org.jenkinsci.plugins.pipeline.modeldefinition.Utils
import org.ds.CustomMatrix

@NonCPS
// Function to generate all permutations, with support for exclusion lists
def generatePermutations(inputMaps, exclusions = []) {
    // Generate all possible combinations from the input maps
    def combinations = getCombinations(inputMaps)

    // Filter combinations based on exclusions
    def filteredCombinations = combinations.findAll { combination ->
        // Check each exclusion list to see if any of the combinations should be excluded
        !exclusions.any { exclusionList ->
            exclusionList.every { exclusion ->
                def mapInCombination = combination.find { it.name == exclusion.name }

                // If exclusion value is a list, check if the value is in the exclusion list
                if (exclusion.values instanceof List) {
                    return exclusion.values.contains(mapInCombination?.value)
                }
                // If exclusion value is a single string, check if the value matches the exclusion
                else {
                    return mapInCombination?.value == exclusion.values
                }
            }
        }
    }

    return filteredCombinations
}

// Helper function to generate all combinations of values
@NonCPS
def getCombinations(List<Map> maps) {
    // Start with a list containing an empty map
    def result = [[]]

    maps.each { map ->
        def tempResult = []
        map.values.each { value ->
            result.each { combination ->
                tempResult.add(combination + [name: map.name, value: value])
            }
        }
        result = tempResult
    }
    return result
}

def validateTopLevel(Map config){
    def requiredFields = ['axes', 'stages']
    def optionalFields = ['excludes', 'when']

    def missing = []
    requiredFields.each{
        if(!config.containsKey(it)){
            missing << it
        }
    }

    def invalidFields = []
    config.each{k,v ->
        if(!(requiredFields + optionalFields).contains(k)){
            invalidFields << k
        }
    }

    def errors = []
    if(missing.size() > 0){
        errors << "required parameter(s) missing: ${missing}"
    }
    if(invalidFields.size() > 0){
        errors << "Invalid parameter(s): ${invalidFields}"
    }

    if(errors.size() > 0){
        error "${errors}"
    }

}

def validateAxis(axis){
    def requiredKeys = ['name', 'values']

    def missing = []
    requiredKeys.each{k ->
        if(!axis.containsKey(k)){
            missing << k
        }
    }

    def invalidFields = []
    axis.keySet().each{k ->
        if(!requiredKeys.contains(k)){
            invalidFields << k
        }
    }

    def errors = []
    if(missing.size() > 0){
        errors << "required field(s) missing: ${missing}"
    }
    if(invalidFields.size() > 0){
        errors << "Invalid fields(s): ${invalidFields}"
    }
    if(errors.size() > 0){
        error "${errors}"
    }
}

def validateAxes(axes){

    try{
        if (!axes instanceof List) {
            error "axes is not a list"
        }
    } catch(Exception e){
        error "axes parameter is invalid. This should be in the format [[name: 'some_property, values: ['1','2']']"
        throw e
    }
    try{
        axes.each{
            if (!it instanceof Map) {
                error "axes does not contain a list of Maps"
            }
            validateAxis(it)

        }
    } catch(Exception e){
        error "axes parameter is invalid. This should be in the format [[name: 'some_property, values: ['1','2']']"
        throw e
    }

}

def validate(Map config){
    validateTopLevel(config)
    validateAxes(config.axes)
}


def call(Map config){
    def matrix = new CustomMatrix(this)

    try{
        matrix.validate(config)
//        validate(config)
        def stages = generatePermutations(config.axes, config.excludes).collect{
                row ->row.collectEntries{element -> [element.name, element.value]}
            }.collectEntries{entryParams ->
            def stageName = "Matrix: ${entryParams}"
            return [
                stageName,
                {
                    stage(stageName){
                        if(config.when ? config.when(entryParams) : true){
                            config.stages.each{
                                it(entryParams)
                            }
                        } else {
                            Utils.markStageSkippedForConditional(stageName)
                        }
                    }
                }
            ]
        }
        parallel(stages)
    } catch(Exception e){
        echo "customMatrix failed. Expected format customMatrix(axes: ,stages: ) Values used ${config}"
        throw e
    }
}