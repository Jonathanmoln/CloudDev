package ax.ha.clouddevelopment;

import software.amazon.awscdk.*;
import software.amazon.awscdk.services.iam.*;
import software.amazon.awscdk.services.route53.*;
import software.amazon.awscdk.services.route53.targets.BucketWebsiteTarget;
import software.amazon.awscdk.services.s3.BlockPublicAccess;
import software.amazon.awscdk.services.s3.Bucket;
import software.amazon.awscdk.services.s3.BucketPolicy;
import software.amazon.awscdk.services.s3.deployment.BucketDeployment;
import software.amazon.awscdk.services.s3.deployment.Source;
import software.constructs.Construct;
import software.amazon.awscdk.services.iam.PolicyStatement;
import java.util.List;
import java.util.Map;

import static java.lang.String.join;


public class WebsiteBucketStack extends Stack {

    public WebsiteBucketStack(final Construct scope,
                              final String id,
                              final StackProps props,
                              final String groupName) {
        super(scope, id, props);

        // ================== Bucket Creation ==================

        final Bucket siteBucket = Bucket.Builder.create(this, "websiteBucket")
                .bucketName(groupName + ".cloud-ha.com")  // Bucket name with group identifier
                .publicReadAccess(true)             // Allow public read access for website content
                .blockPublicAccess(BlockPublicAccess.BLOCK_ACLS) // Block public ACLs for security
                .removalPolicy(RemovalPolicy.DESTROY)           // Delete the bucket when the stack is destroyed
                .autoDeleteObjects(true)            // Automatically delete objects on bucket deletion
                .websiteIndexDocument("index.html")// Default file served for the website
                .build();

        // ================== CfnOutput: Bucket URL ==================

        CfnOutput.Builder.create(this, "websiteBucketOutput")
                .description(String.format("URL of your bucket.", groupName)) // Descriptive output
                //.value(groupName + ".cloud-ha.com")                  // Get the website URL
                .value(siteBucket.getBucketWebsiteUrl())
                .build();

        // ================== IP-Based Access Restriction ==================

        // 1. Build the Policy Statement
        PolicyStatement ipPolicy = PolicyStatement.Builder.create()
                .sid("AllowAccessFromMyIP")
                .effect(Effect.ALLOW)
                .actions(List.of("s3:GetObject"))
                .resources(List.of(siteBucket.getBucketArn() + "/*"))
                .principals(List.of(new AnyPrincipal()))
                .conditions(Map.of(
                        "IpAddress", Map.of(
                                "aws:SourceIp", List.of("193.167.183.15/32")
                        )
                ))
                .build();

        siteBucket.addToResourcePolicy(ipPolicy);


        BucketDeployment deployment = BucketDeployment.Builder.create(this, "DeployWebsite")
                .sources(List.of(Source.asset("C:\\Users\\techlab\\Desktop\\s3-assignment\\src\\main\\resources\\website\\")))
                .destinationBucket(siteBucket)
                .build();



        // Hämta Hosted Zone (skolans domän)
        IHostedZone hostedZone =  HostedZone.fromHostedZoneAttributes(this, "HostedZone", HostedZoneAttributes.builder()
                .hostedZoneId("Z0413857YT73A0A8FRFF")
                .zoneName("cloud-ha.com")
                .build());

// Skapa A Record i Route 53
        ARecord record = ARecord.Builder.create(this, "AliasRecord")
                .zone(hostedZone)
                .target(RecordTarget.fromAlias(new BucketWebsiteTarget(siteBucket)))
                .recordName(groupName + ".cloud-ha.com")  // Använd samma namn som bucketen
                .build();
    }
}