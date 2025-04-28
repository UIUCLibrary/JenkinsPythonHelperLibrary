import org.ds.CustomMatrix

def call(Map config){
    def matrix = new CustomMatrix(this)

    try{
        matrix.validate(config)
        parallel(matrix.generateStages(config))
    } catch(Exception e){
        echo "customMatrix failed. Expected format customMatrix(axes: ,stages: ) Values used ${config}"
        throw e
    }
}