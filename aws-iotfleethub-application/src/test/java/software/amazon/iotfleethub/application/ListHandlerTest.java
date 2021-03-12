package software.amazon.iotfleethub.application;

import software.amazon.awssdk.services.iotfleethub.model.ListApplicationsRequest;
import software.amazon.awssdk.services.iotfleethub.model.ListApplicationsResponse;
import software.amazon.awssdk.services.iotfleethub.model.ApplicationSummary;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import static software.amazon.iotfleethub.application.TestConstants.APPLICATION_CREATION_DATE;
import static software.amazon.iotfleethub.application.TestConstants.APPLICATION_NAME;
import static software.amazon.iotfleethub.application.TestConstants.APPLICATION_NAME_2;
import static software.amazon.iotfleethub.application.TestConstants.APPLICATION_DESCRIPTION;
import static software.amazon.iotfleethub.application.TestConstants.APPLICATION_ID;
import static software.amazon.iotfleethub.application.TestConstants.APPLICATION_ID_2;
import static software.amazon.iotfleethub.application.TestConstants.APPLICATION_LAST_UPDATE_DATE;
import static software.amazon.iotfleethub.application.TestConstants.APPLICATION_STATE;
import static software.amazon.iotfleethub.application.TestConstants.APPLICATION_URL;
import static software.amazon.iotfleethub.application.TestConstants.APPLICATION_URL_2;

@ExtendWith(MockitoExtension.class)
public class ListHandlerTest {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private Logger logger;

    private AutoCloseable closeable;

    private ListHandler handler;

    @BeforeEach
    public void setup() {
        closeable = MockitoAnnotations.openMocks(this);
        handler = new ListHandler();
    }

    @AfterEach
    public void tear_down() throws Exception {
        closeable.close();
    }

    @Test
    public void handleRequest_Simple_Success() {
        ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .nextToken("nextToken1")
                .build();

        List<ApplicationSummary> applicationSummaries = new ArrayList<ApplicationSummary>(2);

        ApplicationSummary SimpleApp = ApplicationSummary.builder()
                .applicationId(APPLICATION_ID)
                .applicationName(APPLICATION_NAME)
                .applicationDescription(APPLICATION_DESCRIPTION)
                .applicationUrl(APPLICATION_URL)
                .applicationCreationDate(APPLICATION_CREATION_DATE)
                .applicationLastUpdateDate(APPLICATION_LAST_UPDATE_DATE)
                .applicationState(APPLICATION_STATE)
                .build();
        ApplicationSummary SimpleApp2 = ApplicationSummary.builder()
                .applicationId(APPLICATION_ID_2)
                .applicationName(APPLICATION_NAME_2)
                .applicationDescription(APPLICATION_DESCRIPTION)
                .applicationUrl(APPLICATION_URL_2)
                .applicationCreationDate(APPLICATION_CREATION_DATE)
                .applicationLastUpdateDate(APPLICATION_LAST_UPDATE_DATE)
                .applicationState(APPLICATION_STATE)
                .build();
        applicationSummaries.add(SimpleApp);
        applicationSummaries.add(SimpleApp2);

        ListApplicationsResponse listResponse = ListApplicationsResponse.builder()
                .applicationSummaries(applicationSummaries)
                .nextToken("nextToken2")
                .build();
        when(proxy.injectCredentialsAndInvokeV2(any(), any()))
                .thenReturn(listResponse);

        ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, null, logger);

        assertThat(response.getNextToken()).isEqualTo("nextToken2");
        List<ResourceModel> expectedModelList = Arrays.asList(
                ResourceModel.builder()
                        .applicationId(APPLICATION_ID)
                        .applicationName(APPLICATION_NAME)
                        .applicationDescription(APPLICATION_DESCRIPTION)
                        .applicationUrl(APPLICATION_URL)
                        .applicationCreationDate((int)APPLICATION_CREATION_DATE)
                        .applicationLastUpdateDate((int)APPLICATION_LAST_UPDATE_DATE)
                        .applicationState(APPLICATION_STATE)
                        .build(),
                ResourceModel.builder()
                        .applicationId(APPLICATION_ID_2)
                        .applicationName(APPLICATION_NAME_2)
                        .applicationDescription(APPLICATION_DESCRIPTION)
                        .applicationUrl(APPLICATION_URL_2)
                        .applicationCreationDate((int)APPLICATION_CREATION_DATE)
                        .applicationLastUpdateDate((int)APPLICATION_LAST_UPDATE_DATE)
                        .applicationState(APPLICATION_STATE)
                        .build());

        // ListHandler uses getResourceModels attribute, all other handlers use getResouceModel
        ProgressEvent<ResourceModel, CallbackContext> expectedResponse = ProgressEvent.<ResourceModel, CallbackContext>builder()
                .resourceModels(expectedModelList)
                .nextToken("nextToken2")
                .status(OperationStatus.SUCCESS)
                .callbackDelaySeconds(0)
                .build();
        assertThat(response).isEqualTo(expectedResponse);
    }

    @Test
    public void handleRequest_NextTokenNull_Success() {
        ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .nextToken(null)
                .build();

        List<ApplicationSummary> applicationSummaries = new ArrayList<ApplicationSummary>(1);
        ApplicationSummary SimpleApp = ApplicationSummary.builder()
                .applicationId(APPLICATION_ID)
                .applicationName(APPLICATION_NAME)
                .applicationDescription(APPLICATION_DESCRIPTION)
                .applicationUrl(APPLICATION_URL)
                .applicationCreationDate(APPLICATION_CREATION_DATE)
                .applicationLastUpdateDate(APPLICATION_LAST_UPDATE_DATE)
                .applicationState(APPLICATION_STATE)
                .build();
        applicationSummaries.add(SimpleApp);

        ListApplicationsResponse listResponse = ListApplicationsResponse.builder()
                .applicationSummaries(applicationSummaries)
                .nextToken(null)
                .build();
        when(proxy.injectCredentialsAndInvokeV2(any(), any()))
                .thenReturn(listResponse);

        ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, null, logger);

        assertThat(response.getNextToken()).isEqualTo(null);
        List<ResourceModel> expectedModelList = Arrays.asList(
                ResourceModel.builder()
                        .applicationId(APPLICATION_ID)
                        .applicationName(APPLICATION_NAME)
                        .applicationDescription(APPLICATION_DESCRIPTION)
                        .applicationUrl(APPLICATION_URL)
                        .applicationCreationDate((int)APPLICATION_CREATION_DATE)
                        .applicationLastUpdateDate((int)APPLICATION_LAST_UPDATE_DATE)
                        .applicationState(APPLICATION_STATE)
                        .build());
        ProgressEvent<ResourceModel, CallbackContext> expectedResponse = ProgressEvent.<ResourceModel, CallbackContext>builder()
                .resourceModels(expectedModelList)
                .nextToken(null)
                .status(OperationStatus.SUCCESS)
                .callbackDelaySeconds(0)
                .build();
        assertThat(response).isEqualTo(expectedResponse);
    }
}
