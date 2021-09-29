package software.amazon.iotfleethub.application;

import com.google.common.collect.ImmutableMap;

import software.amazon.awssdk.core.SdkClient;
import software.amazon.awssdk.services.iotfleethub.model.IoTFleetHubRequest;
import software.amazon.awssdk.services.iotfleethub.model.DescribeApplicationRequest;
import software.amazon.awssdk.services.iotfleethub.model.DescribeApplicationResponse;
import software.amazon.awssdk.services.iotfleethub.model.ResourceNotFoundException;
import software.amazon.awssdk.services.iotfleethub.model.TagResourceRequest;
import software.amazon.awssdk.services.iotfleethub.model.UntagResourceRequest;
import software.amazon.awssdk.services.iotfleethub.model.UpdateApplicationRequest;
import software.amazon.awssdk.services.iotfleethub.model.UpdateApplicationResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import static software.amazon.iotfleethub.application.TestConstants.APPLICATION_ARN;
import static software.amazon.iotfleethub.application.TestConstants.APPLICATION_CREATION_DATE;
import static software.amazon.iotfleethub.application.TestConstants.APPLICATION_DESCRIPTION;
import static software.amazon.iotfleethub.application.TestConstants.APPLICATION_DESCRIPTION_2;
import static software.amazon.iotfleethub.application.TestConstants.APPLICATION_ID;
import static software.amazon.iotfleethub.application.TestConstants.APPLICATION_ID_2;
import static software.amazon.iotfleethub.application.TestConstants.APPLICATION_LAST_UPDATE_DATE;
import static software.amazon.iotfleethub.application.TestConstants.APPLICATION_NAME;
import static software.amazon.iotfleethub.application.TestConstants.APPLICATION_NAME_2;
import static software.amazon.iotfleethub.application.TestConstants.APPLICATION_URL;
import static software.amazon.iotfleethub.application.TestConstants.CLIENT_TOKEN;
import static software.amazon.iotfleethub.application.TestConstants.ERROR_MESSAGE;
import static software.amazon.iotfleethub.application.TestConstants.MODEL_TAG_MAP;
import static software.amazon.iotfleethub.application.TestConstants.MODEL_TAG_MAP_2;
import static software.amazon.iotfleethub.application.TestConstants.MODEL_TAGS;
import static software.amazon.iotfleethub.application.TestConstants.MODEL_TAGS_2;
import static software.amazon.iotfleethub.application.TestConstants.ROLE_ARN;
import static software.amazon.iotfleethub.application.TestConstants.SSO_CLIENT_ID;

@ExtendWith(MockitoExtension.class)
public class UpdateHandlerTest {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private Logger logger;

    private AutoCloseable closeable;

    private UpdateHandler handler;

    @BeforeEach
    public void setup() {
        closeable = MockitoAnnotations.openMocks(this);
        handler = new UpdateHandler();
    }

    @AfterEach
    public void tear_down() throws Exception {
        verifyNoMoreInteractions(proxy);
        closeable.close();
    }

    @Test
    public void handleRequest_Simple_Success() {
        ResourceModel previousModel = ResourceModel.builder()
                .applicationId(APPLICATION_ID)
                .applicationName(APPLICATION_NAME)
                .applicationDescription(APPLICATION_DESCRIPTION)
                .tags(MODEL_TAGS_2)
                .build();

        ResourceModel desiredModel = ResourceModel.builder()
                .applicationId(APPLICATION_ID)
                .applicationName(APPLICATION_NAME_2)
                .applicationDescription(APPLICATION_DESCRIPTION_2)
                .tags(MODEL_TAGS)
                .build();

        ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .previousResourceState(previousModel)
                .desiredResourceState(desiredModel)
                .desiredResourceTags(MODEL_TAG_MAP)
                .clientRequestToken(CLIENT_TOKEN)
                .build();

        // Mocking DescribeApp in UpdateHandler
        DescribeApplicationRequest expectedDescribeRequest = DescribeApplicationRequest.builder()
                .applicationId(APPLICATION_ID)
                .build();
        DescribeApplicationResponse describeResponse = DescribeApplicationResponse.builder()
                .applicationId(APPLICATION_ID)
                .applicationArn(APPLICATION_ARN)
                .tags(MODEL_TAG_MAP_2)
                .build();
        when(proxy.injectCredentialsAndInvokeV2(eq(expectedDescribeRequest), any()))
                .thenReturn(describeResponse);

        // Mocking UpdateApp in UpdateHandler
        UpdateApplicationRequest expectedUpdateRequest = UpdateApplicationRequest.builder()
                .applicationId(APPLICATION_ID)
                .applicationName(APPLICATION_NAME_2)
                .applicationDescription(APPLICATION_DESCRIPTION_2)
                .clientToken(CLIENT_TOKEN)
                .build();
        UpdateApplicationResponse updateResponse = UpdateApplicationResponse.builder().build();
        doReturn(updateResponse)
                .when(proxy)
                .injectCredentialsAndInvokeV2(eq(expectedUpdateRequest), any());

        // Handle Update Request
        ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, null, logger);

        ProgressEvent<ResourceModel, CallbackContext> expectedResponse = ProgressEvent.<ResourceModel, CallbackContext>builder()
                .resourceModel(desiredModel)
                .status(OperationStatus.SUCCESS)
                .callbackDelaySeconds(0)
                .build();
        assertThat(response).isEqualTo(expectedResponse);

        ArgumentCaptor<IoTFleetHubRequest> requestCaptor = ArgumentCaptor.forClass(IoTFleetHubRequest.class);
        // IoTFleetHubClient made 4 calls in this case:
        // describeApplication, updateApplication, tagResource, untagResource
        verify(proxy, times(4)).injectCredentialsAndInvokeV2(requestCaptor.capture(), any());
        List<IoTFleetHubRequest> submittedRequests = requestCaptor.getAllValues();

        TagResourceRequest submittedTagRequest = (TagResourceRequest) submittedRequests.get(2);
        assertEquals(submittedTagRequest.tags(), MODEL_TAG_MAP);
        assertThat(submittedTagRequest.resourceArn()).isEqualTo(APPLICATION_ARN);

        UntagResourceRequest submittedUntagRequest = (UntagResourceRequest) submittedRequests.get(3);
        assertEquals(submittedUntagRequest.tagKeys().size(), 1);
        assertTrue(submittedUntagRequest.tagKeys().contains("resourceTagKey2"));
        assertThat(submittedUntagRequest.resourceArn()).isEqualTo(APPLICATION_ARN);
    }

    @Test
    public void updateTags_SameKeyDifferentValue_Success() {
        Map<String, String> desiredTags = ImmutableMap.of("resourceTagKey", "differentTagValue");

        ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .previousResourceState(ResourceModel.builder().build())
                .desiredResourceState(ResourceModel.builder().build())
                .desiredResourceTags(desiredTags)
                .build();

        handler.updateTags(proxy, request, APPLICATION_ARN, MODEL_TAG_MAP, logger);

        ArgumentCaptor<IoTFleetHubRequest> requestCaptor = ArgumentCaptor.forClass(IoTFleetHubRequest.class);
        // There should only be 1 call (we should only be updating one Tag, and no Tag removals)
        verify(proxy, times(1)).injectCredentialsAndInvokeV2(requestCaptor.capture(), any());
        List<IoTFleetHubRequest> submittedRequests = requestCaptor.getAllValues();

        TagResourceRequest submittedTagRequest = (TagResourceRequest) submittedRequests.get(0);
        assertEquals(submittedTagRequest.tags(), desiredTags);
    }

    @Test
    public void updateTags_OnlyRemoveTag_Success() {
        ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .previousResourceState(ResourceModel.builder().build())
                .desiredResourceState(ResourceModel.builder().build())
                .build();

        handler.updateTags(proxy, request, APPLICATION_ARN, MODEL_TAG_MAP, logger);

        ArgumentCaptor<IoTFleetHubRequest> requestCaptor = ArgumentCaptor.forClass(IoTFleetHubRequest.class);
        // There should only be 1 call (we should only be removing one Tag)
        verify(proxy, times(1)).injectCredentialsAndInvokeV2(requestCaptor.capture(), any());
        List<IoTFleetHubRequest> submittedRequests = requestCaptor.getAllValues();

        UntagResourceRequest submittedUntagRequest = (UntagResourceRequest) submittedRequests.get(0);
        assertEquals(submittedUntagRequest.tagKeys().size(), 1);
        assertTrue(submittedUntagRequest.tagKeys().contains("resourceTagKey"));
    }

    @Test
    public void handleRequest_SettingReadOnlyFields_Failure() {
        ResourceModel previousModel = ResourceModel.builder()
                .applicationId(APPLICATION_ID)
                .applicationName(APPLICATION_NAME)
                .applicationDescription(APPLICATION_DESCRIPTION)
                .build();

        ResourceModel desiredModel = ResourceModel.builder()
                .applicationId(APPLICATION_ID_2)
                .applicationArn(APPLICATION_ARN)
                .applicationName(APPLICATION_NAME)
                .applicationDescription(APPLICATION_DESCRIPTION)
                .build();

        ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .previousResourceState(previousModel)
                .desiredResourceState(desiredModel)
                .clientRequestToken(CLIENT_TOKEN)
                .build();

        ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.InvalidRequest);
        assertThat(response.getMessage()).isEqualTo("Can only update ApplicationName, ApplicationDescription, or Tags.");
    }

    @Test
    public void handleRequest_NoAppId_Failure() {
        ResourceModel desiredModel = ResourceModel.builder()
                .applicationName(APPLICATION_NAME_2)
                .applicationDescription(APPLICATION_DESCRIPTION_2)
                .build();

        ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(desiredModel)
                .clientRequestToken(CLIENT_TOKEN)
                .build();

        ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.NotFound);
        assertThat(response.getMessage()).isEqualTo("ApplicationId was not provided.");
    }

    @Test
    public void handleRequest_AppNotFound_Failure() {
        ResourceModel desiredModel = ResourceModel.builder()
                .applicationId(APPLICATION_ID)
                .applicationName(APPLICATION_NAME_2)
                .applicationDescription(APPLICATION_DESCRIPTION_2)
                .build();

        ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(desiredModel)
                .clientRequestToken(CLIENT_TOKEN)
                .build();

        when(proxy.injectCredentialsAndInvokeV2(any(), any()))
                .thenThrow(ResourceNotFoundException.builder().build());

        ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.NotFound);
        assertThat(response.getMessage()).isNull();
    }

    @Test
    public void handleRequest_NoClientRequestToken_Failure() {
        ResourceModel model = ResourceModel.builder()
                .applicationId(APPLICATION_ID)
                .applicationName(APPLICATION_NAME_2)
                .applicationDescription(APPLICATION_DESCRIPTION_2)
                .build();

        ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .clientRequestToken(null)
                .build();

        ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.InvalidRequest);
        assertThat(response.getMessage()).isEqualTo("ClientToken was not provided.");
    }
}
