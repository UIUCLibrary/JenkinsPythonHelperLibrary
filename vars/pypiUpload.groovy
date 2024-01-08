import org.ds.python.pypiFunctions

def call(Map args) {
    def pypi = new pypiFunctions()
    return pypi.pypiUpload(args)
}