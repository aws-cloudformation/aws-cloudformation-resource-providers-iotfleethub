package software.amazon.iotfleethub.application;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import java.util.Map;
import java.util.Set;

public class TestConstants {
    protected static final String APPLICATION_ID = "87e64213-f61a-4e99-2b22-c633b4512917";
    protected static final String APPLICATION_ID_2 = "76d53102-e50f-3d88-1a11-b522a3401806";
    protected static final String APPLICATION_ARN = "arn:aws:iotfleethub:us-east-1:123456789012:application/" + APPLICATION_ID;
    protected static final String APPLICATION_NAME = "FleetHub_Test_Application";
    protected static final String APPLICATION_NAME_2 = "FleetHub_Test_Application_2";
    protected static final String APPLICATION_DESCRIPTION = "FleetHub Application created with CFN";
    protected static final String APPLICATION_DESCRIPTION_2 = "2nd FleetHub Application created with CFN";
    protected static final String APPLICATION_URL = "https://" + APPLICATION_ID + ".fleethub-endpoint.com";
    protected static final String APPLICATION_URL_2 = "https://" + APPLICATION_ID_2 + ".fleethub-endpoint.com";
    protected static final String APPLICATION_STATE = "ACTIVE";
    protected static final long APPLICATION_CREATION_DATE = 1605054959;
    protected static final long APPLICATION_LAST_UPDATE_DATE = 1605054959;
    protected static final String CLIENT_TOKEN = "Fl33tHuB";
    protected static final String ERROR_MESSAGE = "There was an error.";
    protected static final String ROLE_ARN = "arn:aws:iam::123456789012:role/service-role/AWSIotFleetHub_3";
    protected static final String SSO_CLIENT_ID = "LHGUNDyWARfdHlWpWgMuLmJ5hp58xwbqCc";

    protected static final Set<Tag> MODEL_TAGS = ImmutableSet.of(
            Tag.builder()
                    .key("resourceTagKey")
                    .value("resourceTagValue")
                    .build());
    protected static final Map<String, String> MODEL_TAG_MAP = ImmutableMap.of("resourceTagKey", "resourceTagValue");

    protected static final Set<Tag> MODEL_TAGS_2 = ImmutableSet.of(
            Tag.builder()
                    .key("resourceTagKey2")
                    .value("resourceTagValue2")
                    .build());
    protected static final Map<String, String> MODEL_TAG_MAP_2 = ImmutableMap.of("resourceTagKey2", "resourceTagValue2");

    protected static final String INVALID_APPLICATION_NAME =
            "AWS CloudFormation gives you an easy way to model a collection of " +
                    "related AWS and third-party resources, provision them quickly and consistently, and manage them throughout their lifecycles, " +
                    "by treating infrastructure as code. A CloudFormation template describes your desired resources and their dependencies so " +
                    "you can launch and configure them together as a stack";
    protected static final String INVALID_APPLICATION_ID = "1234-invalid-5678";
}