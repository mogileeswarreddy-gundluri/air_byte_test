@Grab(group='software.amazon.awssdk', module='s3', version='2.20.35')
@Grab(group='software.amazon.awssdk', module='auth', version='2.20.35')
@Grab(group='com.fasterxml.jackson.core', module='jackson-databind', version='2.15.2')

import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.regions.Region
import java.nio.file.Paths
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature

// This function can be called with custom bucket and file, or with no parameters
def call(Map config = [:]) {
    // ---------------------------
    // Configuration with defaults
    // ---------------------------
    def appName = config.appName ?: 'local-test-app'
    def bucketName = config.bucketName ?: 'ff-mogileeswar-20251009-airbyte'
    def s3Path = config.s3Path ?: "jenkins-test/${env.BUILD_NUMBER ?: 'manual'}_record.json"
    def jsonFile = config.jsonFile ?: 'record.json'
    def region = config.region ?: 'US_EAST_1'
    
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
    
    // Convert to pretty JSON
    def mapper = new ObjectMapper()
    mapper.enable(SerializationFeature.INDENT_OUTPUT)
    def jsonContent = mapper.writeValueAsString(payload)
    
    // Save locally
    new File(jsonFile).write(jsonContent)
    echo "JSON payload saved locally to ${jsonFile}"
    
    // ---------------------------
    // Upload to S3
    // ---------------------------
    def s3 = S3Client.builder()
            .region(Region.valueOf(region))
            .build()
    
    def putRequest = PutObjectRequest.builder()
            .bucket(bucketName)
            .key(s3Path)
            .build()
    
    s3.putObject(putRequest, Paths.get(jsonFile))
    echo "Uploaded JSON to s3://${bucketName}/${s3Path}"
    
    return "s3://${bucketName}/${s3Path}"
}
