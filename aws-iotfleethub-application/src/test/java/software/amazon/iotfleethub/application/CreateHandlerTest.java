package software.amazon.iotfleethub.application;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import software.amazon.awssdk.core.SdkClient;
import software.amazon.awssdk.services.iotfleethub.model.CreateApplicationRequest;
import software.amazon.awssdk.services.iotfleethub.model.CreateApplicationResponse;
import software.amazon.awssdk.services.iotfleethub.model.InvalidRequestException;
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

import static junit.framework.Assert.assertEquals;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import static software.amazon.iotfleethub.application.TestConstants.APPLICATION_ARN;
import static software.amazon.iotfleethub.application.TestConstants.APPLICATION_NAME;
import static software.amazon.iotfleethub.application.TestConstants.APPLICATION_DESCRIPTION;
import static software.amazon.iotfleethub.application.TestConstants.APPLICATION_ID;
import static software.amazon.iotfleethub.application.TestConstants.CLIENT_TOKEN;
import static software.amazon.iotfleethub.application.TestConstants.MODEL_TAG_MAP;
import static software.amazon.iotfleethub.application.TestConstants.INVALID_APPLICATION_NAME;
import static software.amazon.iotfleethub.application.TestConstants.MODEL_TAGS;
import static software.amazon.iotfleethub.application.TestConstants.ROLE_ARN;

@ExtendWith(MockitoExtension.class)
public class CreateHandlerTest {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private Logger logger;

    private AutoCloseable closeable;

    private CreateHandler handler;

    @BeforeEach
    public void setup() {
        closeable = MockitoAnnotations.openMocks(this);
        handler = new CreateHandler();
    }

    @AfterEach
    public void tear_down() throws Exception {
        verifyNoMoreInteractions(proxy);
        closeable.close();
    }

    @Test
    public void handleRequest_Simple_Success() {
        ResourceModel model = ResourceModel.builder()
                .applicationName(APPLICATION_NAME)
                .roleArn(ROLE_ARN)
                .build();

        ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .clientRequestToken(CLIENT_TOKEN)
                .build();

        CreateApplicationResponse createResponse = CreateApplicationResponse.builder()
                .applicationId(APPLICATION_ID)
                .applicationArn(APPLICATION_ARN)
                .build();
        when(proxy.injectCredentialsAndInvokeV2(any(), any()))
                .thenReturn(createResponse);

        ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        ResourceModel expectedModel = ResourceModel.builder()
                .applicationName(APPLICATION_NAME)
                .applicationId(APPLICATION_ID)
                .applicationArn(APPLICATION_ARN)
                .roleArn(ROLE_ARN)
                .build();
        assertThat(response.getResourceModel()).isEqualTo(expectedModel);
    }

    @Test
    public void handleRequest_OptionalFields_Success() {
        ResourceModel model = ResourceModel.builder()
                .applicationName(APPLICATION_NAME)
                .applicationDescription(APPLICATION_DESCRIPTION)
                .roleArn(ROLE_ARN)
                .tags(MODEL_TAGS)
                .build();

        ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .clientRequestToken(CLIENT_TOKEN)
                .desiredResourceTags(MODEL_TAG_MAP)
                .build();

        CreateApplicationResponse createResponse = CreateApplicationResponse.builder()
                .applicationId(APPLICATION_ID)
                .applicationArn(APPLICATION_ARN)
                .build();
        when(proxy.injectCredentialsAndInvokeV2(any(), any()))
                .thenReturn(createResponse);

        ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        ResourceModel expectedModel = ResourceModel.builder()
                .applicationName(APPLICATION_NAME)
                .applicationDescription(APPLICATION_DESCRIPTION)
                .applicationId(APPLICATION_ID)
                .applicationArn(APPLICATION_ARN)
                .roleArn(ROLE_ARN)
                .tags(MODEL_TAGS)
                .build();
        assertThat(response.getResourceModel()).isEqualTo(expectedModel);

        // Examining the actual request used in CreateApplication call
        ArgumentCaptor<CreateApplicationRequest> requestCaptor = ArgumentCaptor.forClass(CreateApplicationRequest.class);
        verify(proxy).injectCredentialsAndInvokeV2(requestCaptor.capture(), any());
        CreateApplicationRequest actualRequest = requestCaptor.getAllValues().get(0);

        Map<String, String> testTags = new HashMap<>();
        testTags.putAll(request.getDesiredResourceTags());
        assertEquals(testTags, actualRequest.tags());
    }

    @Test
    public void handleRequest_SettingReadOnlyFields_Failure() {
        ResourceModel model = ResourceModel.builder()
                .applicationName(APPLICATION_NAME)
                .applicationArn(APPLICATION_ARN)
                .applicationId(APPLICATION_ID)
                .roleArn(ROLE_ARN)
                .build();

        ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .clientRequestToken(CLIENT_TOKEN)
                .build();

        ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNotNull();
        assertEquals(response.getErrorCode(), HandlerErrorCode.InvalidRequest);
    }

    @Test
    public void handleRequest_InvalidFieldValues_Failure() {
        ResourceModel model = ResourceModel.builder()
                .applicationName(INVALID_APPLICATION_NAME)
                .roleArn(ROLE_ARN)
                .build();

        ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .clientRequestToken(CLIENT_TOKEN)
                .build();

        when(proxy.injectCredentialsAndInvokeV2(any(), any()))
                .thenThrow(InvalidRequestException.builder().build());

        ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.InvalidRequest);
    }
}
