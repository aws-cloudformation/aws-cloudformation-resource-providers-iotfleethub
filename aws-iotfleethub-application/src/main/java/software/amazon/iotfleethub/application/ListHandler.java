package software.amazon.iotfleethub.application;

import software.amazon.awssdk.services.iotfleethub.IoTFleetHubClient;
import software.amazon.awssdk.services.iotfleethub.model.ListApplicationsRequest;
import software.amazon.awssdk.services.iotfleethub.model.ListApplicationsResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.List;
import java.util.stream.Collectors;

public class ListHandler extends BaseHandler<CallbackContext> {

    private final IoTFleetHubClient iotFleetHubClient;

    public ListHandler() {
        iotFleetHubClient = IoTFleetHubClient.builder().build();
    }

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final Logger logger) {

        ListApplicationsRequest listRequest = Translator.translateToListRequest(request);

        ListApplicationsResponse listResponse;
        try {
            listResponse = proxy.injectCredentialsAndInvokeV2(listRequest, iotFleetHubClient::listApplications);
        } catch (RuntimeException e) {
            HandlerErrorCode err = Translator.translateExceptionToErrorCode(e, logger);
            return ProgressEvent.failed(request.getDesiredResourceState(), callbackContext, err, e.getMessage());
        }

        String nextToken = listResponse.nextToken();

        // Date attributes need to be converted to int for CFN Model. Dates are in epoch seconds and within int range.
        List<ResourceModel> models = listResponse.applicationSummaries().stream()
                .map(applicationSummary -> ResourceModel.builder()
                        .applicationId(applicationSummary.applicationId())
                        .applicationName(applicationSummary.applicationName())
                        .applicationDescription(applicationSummary.applicationDescription())
                        .applicationUrl(applicationSummary.applicationUrl())
                        .applicationCreationDate(applicationSummary.applicationCreationDate().intValue())
                        .applicationLastUpdateDate(applicationSummary.applicationLastUpdateDate().intValue())
                        .applicationState(applicationSummary.applicationStateAsString())
                        .build())
                .collect(Collectors.toList());

        logger.log(String.format("Listing Applications for Account %s", request.getAwsAccountId()));

        return ProgressEvent.<ResourceModel, CallbackContext>builder()
                .resourceModels(models)
                .nextToken(nextToken)
                .status(OperationStatus.SUCCESS)
                .build();
    }
}
