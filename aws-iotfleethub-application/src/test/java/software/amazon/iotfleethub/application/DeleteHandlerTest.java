package software.amazon.iotfleethub.application;

import java.time.Duration;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.SdkClient;
import software.amazon.awssdk.services.iotfleethub.model.DeleteApplicationRequest;
import software.amazon.awssdk.services.iotfleethub.model.IoTFleetHubRequest;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import static software.amazon.iotfleethub.application.TestConstants.APPLICATION_ID;
import static software.amazon.iotfleethub.application.TestConstants.CLIENT_TOKEN;
import static software.amazon.iotfleethub.application.TestConstants.INVALID_APPLICATION_ID;

@ExtendWith(MockitoExtension.class)
public class DeleteHandlerTest {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private Logger logger;

    private AutoCloseable closeable;

    private DeleteHandler handler;

    @BeforeEach
    public void setup() {
        closeable = MockitoAnnotations.openMocks(this);
        handler = new DeleteHandler();
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
                .clientRequestToken(CLIENT_TOKEN)
                .build();

        ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isNull();
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        ArgumentCaptor<IoTFleetHubRequest> requestCaptor = ArgumentCaptor.forClass(DeleteApplicationRequest.class);
        verify(proxy, times(2)).injectCredentialsAndInvokeV2(requestCaptor.capture(), any());

        DeleteApplicationRequest deleteRequest = (DeleteApplicationRequest) requestCaptor.getAllValues().get(1);
        assertThat(deleteRequest.applicationId()).isEqualTo(APPLICATION_ID);
    }

    @Test
    public void handleRequest_InvalidAppId_Failure() {
        ResourceModel model = ResourceModel.builder()
                .applicationId(INVALID_APPLICATION_ID)
                .build();

        ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .clientRequestToken(CLIENT_TOKEN)
                .build();

        ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getResourceModel()).isNotNull();
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNotNull();
        // See comments in DeleteHandler for explanation on why NotFound error is needed here
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.NotFound);
    }

    @Test
    public void handleRequest_AppNotFound_Failure() {
        ResourceModel model = ResourceModel.builder()
                .applicationId(APPLICATION_ID)
                .build();

        ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .clientRequestToken(CLIENT_TOKEN)
                .build();

        when(proxy.injectCredentialsAndInvokeV2(any(), any()))
                .thenThrow(ResourceNotFoundException.builder().build());

        ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, null, logger);

        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.NotFound);
    }
}
