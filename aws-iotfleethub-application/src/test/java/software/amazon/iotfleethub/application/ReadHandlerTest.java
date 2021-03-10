package software.amazon.iotfleethub.application;

import software.amazon.awssdk.services.iotfleethub.model.DescribeApplicationRequest;
import software.amazon.awssdk.services.iotfleethub.model.DescribeApplicationResponse;
import software.amazon.awssdk.services.iotfleethub.model.ResourceNotFoundException;
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
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import static junit.framework.Assert.assertEquals;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import static software.amazon.iotfleethub.application.TestConstants.APPLICATION_ARN;
import static software.amazon.iotfleethub.application.TestConstants.APPLICATION_CREATION_DATE;
import static software.amazon.iotfleethub.application.TestConstants.APPLICATION_NAME;
import static software.amazon.iotfleethub.application.TestConstants.APPLICATION_DESCRIPTION;
import static software.amazon.iotfleethub.application.TestConstants.APPLICATION_ID;
import static software.amazon.iotfleethub.application.TestConstants.APPLICATION_LAST_UPDATE_DATE;
import static software.amazon.iotfleethub.application.TestConstants.APPLICATION_STATE;
import static software.amazon.iotfleethub.application.TestConstants.APPLICATION_URL;
import static software.amazon.iotfleethub.application.TestConstants.ERROR_MESSAGE;
import static software.amazon.iotfleethub.application.TestConstants.MODEL_TAG_MAP;
import static software.amazon.iotfleethub.application.TestConstants.MODEL_TAGS;
import static software.amazon.iotfleethub.application.TestConstants.ROLE_ARN;
import static software.amazon.iotfleethub.application.TestConstants.SSO_CLIENT_ID;

@ExtendWith(MockitoExtension.class)
public class ReadHandlerTest {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private Logger logger;

    private AutoCloseable closeable;

    private ReadHandler handler;

    @BeforeEach
    public void setup() {
        closeable = MockitoAnnotations.openMocks(this);
        handler = new ReadHandler();
    }

    @AfterEach
    public void tear_down() throws Exception {
        verifyNoMoreInteractions(proxy);
        closeable.close();
    }

    @Test
    public void handleRequest_Simple_Success() {
        ResourceModel model = ResourceModel.builder()
                .applicationId(APPLICATION_ID)
                .build();

        ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        DescribeApplicationResponse describeResponse = DescribeApplicationResponse.builder()
                .applicationId(APPLICATION_ID)
                .applicationArn(APPLICATION_ARN)
                .applicationName(APPLICATION_NAME)
                .applicationDescription(APPLICATION_DESCRIPTION)
                .applicationUrl(APPLICATION_URL)
                .applicationState(APPLICATION_STATE)
                .applicationCreationDate(APPLICATION_CREATION_DATE)
                .applicationLastUpdateDate(APPLICATION_LAST_UPDATE_DATE)
                .roleArn(ROLE_ARN)
                .ssoClientId(SSO_CLIENT_ID)
                .errorMessage(ERROR_MESSAGE)
                .tags(MODEL_TAG_MAP)
                .build();

        when(proxy.injectCredentialsAndInvokeV2(any(), any()))
                .thenReturn(describeResponse);

        ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        // Main purpose of this is to verify the response tags are in Set<Tag> format, not Map<String, String>
        // Cannot only check tags directly, as it has private access in ResourceModel
        ResourceModel expectedModel = ResourceModel.builder()
                .applicationId(APPLICATION_ID)
                .applicationArn(APPLICATION_ARN)
                .applicationName(APPLICATION_NAME)
                .applicationDescription(APPLICATION_DESCRIPTION)
                .applicationUrl(APPLICATION_URL)
                .applicationState(APPLICATION_STATE)
                .applicationCreationDate((int)(long)APPLICATION_CREATION_DATE)
                .applicationLastUpdateDate((int)(long)APPLICATION_LAST_UPDATE_DATE)
                .roleArn(ROLE_ARN)
                .ssoClientId(SSO_CLIENT_ID)
                .errorMessage(ERROR_MESSAGE)
                .tags(MODEL_TAGS)
                .build();
        assertThat(response.getResourceModel()).isEqualTo(expectedModel);
    }

    @Test
    public void handleRequest_NoAppId_Failure() {
        ResourceModel model = ResourceModel.builder().build();

        ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getMessage()).isNotNull();
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.NotFound);
    }

    @Test
    public void handleRequest_AppNotFound_Failure() {
        ResourceModel model = ResourceModel.builder()
                .applicationId(APPLICATION_ID)
                .build();

        ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        when(proxy.injectCredentialsAndInvokeV2(any(), any()))
                .thenThrow(ResourceNotFoundException.builder().build());

        ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, null, logger);

        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.NotFound);
    }
}
