package org.ds

class CustomMatrix implements Serializable{
    def script
    def error

    CustomMatrix(script) {
        this.script = script
        this.error = script.error
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
                steps.error "axes is not a list"
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
}
