@Grab(group='software.amazon.awssdk', module='s3', version='2.20.35')
@Grab(group='software.amazon.awssdk', module='auth', version='2.20.35')
@Grab(group='com.fasterxml.jackson.core', module='jackson-databind', version='2.15.2')

import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.regions.Region
import java.nio.file.Paths
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature

// ---------------------------
// Configuration
// ---------------------------
def appName = 'local-test-app'
def bucketName = 'ff-mogileeswar-20251009-airbyte'
def s3Path = "jenkins-test/${System.getenv('BUILD_NUMBER') ?: 'manual'}_record.json"
def jsonFile = 'record.json'

// ---------------------------
// Generate JSON payload
// ---------------------------
def payload = [
    app       : appName,
    job       : System.getenv('JOB_NAME') ?: 'manual-job',
    build     : System.getenv('BUILD_NUMBER') ?: 'manual-build',
    buildUrl  : System.getenv('BUILD_URL') ?: '',
    timestamp : new Date().format("yyyy-MM-dd'T'HH:mm:ssXXX")
]

// Convert to pretty JSON
def mapper = new ObjectMapper()
mapper.enable(SerializationFeature.INDENT_OUTPUT)
def jsonContent = mapper.writeValueAsString(payload)

// Save locally
new File(jsonFile).write(jsonContent)
println "JSON payload saved locally to ${jsonFile}"

// ---------------------------
// Upload to S3
// ---------------------------
def s3 = S3Client.builder()
        .region(Region.US_EAST_1)  // Change if needed
        .build()

def putRequest = PutObjectRequest.builder()
        .bucket(bucketName)
        .key(s3Path)
        .build()

s3.putObject(putRequest, Paths.get(jsonFile))
println "Uploaded JSON to s3://${bucketName}/${s3Path}"
