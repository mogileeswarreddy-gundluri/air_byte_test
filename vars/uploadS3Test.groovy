import groovy.json.JsonBuilder

// This function can be called with custom bucket and file, or with no parameters
def call(Map config = [:]) {
    // ---------------------------
    // Configuration with defaults
    // ---------------------------
    def appName = config.appName ?: 'local-test-app'
    def bucketName = config.bucketName ?: 'ff-mogileeswar-20251009-airbyte'
    def s3Path = config.s3Path ?: "jenkins-test/${env.BUILD_NUMBER ?: 'manual'}_record.json"
    def region = config.region ?: 'us-east-1'
    
    echo "Starting S3 upload process..."
    echo "Bucket: ${bucketName}"
    echo "S3 Path: ${s3Path}"
    
    // ---------------------------
    // Generate JSON payload
    // ---------------------------
    def payload = [
        app       : appName,
        job       : env.JOB_NAME ?: 'manual-job',
        build     : env.BUILD_NUMBER ?: 'manual-build',
        buildUrl  : env.BUILD_URL ?: '',
        timestamp : new Date().format("yyyy-MM-dd'T'HH:mm:ssXXX")
    ]
    
    // Convert to pretty JSON using Groovy's built-in JsonBuilder
    def jsonContent = new JsonBuilder(payload).toPrettyString()
    
    echo "JSON payload created (${jsonContent.length()} bytes)"
    
    // ---------------------------
    // Upload to S3 using Jenkins S3 Plugin
    // ---------------------------
    // Create a temporary file in workspace
    def tempFile = "temp_${env.BUILD_NUMBER}_record.json"
    writeFile file: tempFile, text: jsonContent
    
    // Upload to S3 using Jenkins Pipeline S3 Upload step
    // Uses IAM role from EC2 instance/EKS pod
    withAWS(region: region) {
        s3Upload(
            bucket: bucketName,
            path: s3Path,
            file: tempFile,
            contentType: 'application/json'
        )
    }
    
    // Clean up temp file
    sh "rm -f ${tempFile}"
    
    echo "Uploaded JSON to s3://${bucketName}/${s3Path}"
    
    return "s3://${bucketName}/${s3Path}"
}
