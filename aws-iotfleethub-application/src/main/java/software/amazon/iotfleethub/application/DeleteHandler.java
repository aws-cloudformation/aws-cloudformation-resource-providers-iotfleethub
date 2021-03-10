package software.amazon.iotfleethub.application;

import org.apache.commons.lang3.StringUtils;
import software.amazon.awssdk.services.iotfleethub.IoTFleetHubClient;
import software.amazon.awssdk.services.iotfleethub.model.DeleteApplicationRequest;
import software.amazon.awssdk.services.iotfleethub.model.DescribeApplicationRequest;
import software.amazon.awssdk.services.iotfleethub.model.DescribeApplicationResponse;
import software.amazon.awssdk.services.iotfleethub.model.ResourceNotFoundException;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.regex.Pattern;

public class DeleteHandler extends BaseHandler<CallbackContext> {

    private static final Pattern APP_ID_PATTERN = Pattern.compile("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$");

    private final IoTFleetHubClient iotFleetHubClient;

    public DeleteHandler() {
        iotFleetHubClient = IoTFleetHubClient.builder().build();
    }

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final Logger logger) {

        ResourceModel model = request.getDesiredResourceState();

        // ClientToken is required for idempotent Delete Operation
        if (StringUtils.isEmpty(request.getClientRequestToken())) {
            logger.log(String.format("ClientToken is Required, but a client request token was not provided."));
            return ProgressEvent.failed(model, callbackContext, HandlerErrorCode.InvalidRequest, "ClientToken was not provided.");
        }

        // From https://docs.aws.amazon.com/cloudformation-cli/latest/userguide/resource-type-test-contract.html
        // "A delete handler MUST return FAILED with a NotFound error code if the
        // resource did not exist prior to the delete request."
        // DeleteApplication API is idempotent, so we have to call Describe first.

        // Before we call Describe, we also need to deal with an InvalidRequest edge case.
        // If CFN is trying to delete a resource with an invalid name, returning InvalidRequest would
        // get CFN stuck in delete-failed state. If we return NotFound, it'll just succeed.
        // We wouldn't have to do this if aws-cloudformation-rpdk-java-plugin had functioning regex
        // pattern evaluation (known issue with an internal ticket).
        String applicationId = model.getApplicationId();
        if (applicationId == null) {
            logger.log("Returning NotFound from DeleteHandler due to no Id provided in the model.");
            return ProgressEvent.failed(model, callbackContext, HandlerErrorCode.NotFound, "Application Id was not provided.");
        } else {
            boolean matches = APP_ID_PATTERN.matcher(applicationId).matches();
            if (!matches) {
                logger.log("Returning NotFound from DeleteHandler due to invalid Id " + applicationId);
                return ProgressEvent.failed(model, callbackContext, HandlerErrorCode.NotFound, "Invalid Application Id");
            }
        }

        DescribeApplicationRequest describeRequest = DescribeApplicationRequest.builder()
                .applicationId(applicationId)
                .build();
        try {
            proxy.injectCredentialsAndInvokeV2(describeRequest, iotFleetHubClient::describeApplication);
        } catch (RuntimeException e) {
            // If the resource doesn't exist, DescribeCustomMetric will throw ResourceNotFoundException,
            // and we'll return FAILED with HandlerErrorCode.NotFound.
            // CFN (the caller) will swallow the "failure" and the customer will see success.
            HandlerErrorCode err = Translator.translateExceptionToErrorCode(e, logger);
            return ProgressEvent.failed(model, callbackContext, err, e.getMessage());
        }
        logger.log(String.format("Called Describe for application %s, accountId %s.",
                applicationId, request.getAwsAccountId()));

        DeleteApplicationRequest deleteRequest = translateToDeleteRequest(request, model);

        try {
            proxy.injectCredentialsAndInvokeV2(deleteRequest, iotFleetHubClient::deleteApplication);
        } catch (ResourceNotFoundException e) {
            logger.log(String.format("Application with Id %s was not found", model.getApplicationId()));
            return ProgressEvent.failed(model, callbackContext, HandlerErrorCode.NotFound, e.getMessage());
        } catch (RuntimeException e) {
            HandlerErrorCode err = Translator.translateExceptionToErrorCode(e, logger);
            return ProgressEvent.failed(model, callbackContext, err, e.getMessage());
        }

        logger.log(String.format("Deleted Application with Id %s from account %s ",
                model.getApplicationId(), request.getAwsAccountId()));

        return ProgressEvent.defaultSuccessHandler(null);
    }

    private DeleteApplicationRequest translateToDeleteRequest(
            ResourceHandlerRequest<ResourceModel> request,
            ResourceModel model) {

        return DeleteApplicationRequest.builder()
                .applicationId(model.getApplicationId())
                .clientToken(request.getClientRequestToken())
                .build();
    }
}
