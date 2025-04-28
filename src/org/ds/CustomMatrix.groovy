package org.ds

import org.jenkinsci.plugins.pipeline.modeldefinition.Utils

class CustomMatrix implements Serializable{
    def validator
    def stageGenerator

    CustomMatrix(script) {
        this.validator = new ArgumentValidator(script)
        this.stageGenerator = new StagesGenerator(script)
    }

    class ArgumentValidator {
        def script

        ArgumentValidator(script){
            this.script = script

        }

        def validate(Map config) {
            validateTopLevel(config)
            validateAxes(config.axes)
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
                this.script.error "${errors}"
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
                this.script.error "${errors}"
            }
        }
        def validateAxes(axes){

            try{
                if (!axes instanceof List) {
                    this.script.error "axes is not a list"
                }
            } catch(Exception e){
                this.script.error "axes parameter is invalid. This should be in the format [[name: 'some_property, values: ['1','2']']"
                throw e
            }
            try{
                axes.each{
                    if (!it instanceof Map) {
                        this.script.error "axes does not contain a list of Maps"
                    }
                    validateAxis(it)

                }
            } catch(Exception e){
                this.script.error "axes parameter is invalid. This should be in the format [[name: 'some_property, values: ['1','2']']"
                throw e
            }

        }
    }
    class StagesGenerator {
        def script

        StagesGenerator(script){
            this.script = script
        }
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

        @NonCPS
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
        def generateStages(Map config){
            def stages = generatePermutations(config.axes, config.excludes).collect{
                row ->row.collectEntries{element -> [element.name, element.value]}
            }.collectEntries{entryParams ->
                def stageName = "Matrix: ${entryParams}"
                return [
                        stageName,
                        {
                            this.script.stage(stageName){
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
            return stages

        }
    }
    def validate(Map config){
        validator.validate(config)
    }
    def generatePermutations(inputMaps, exclusions = []) {
        return this.stageGenerator.generatePermutations(inputMaps, exclusions)
    }

    def generateStages(Map config){
        return this.stageGenerator.generateStages(config)
    }

}
