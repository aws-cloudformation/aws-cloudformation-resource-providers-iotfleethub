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

        if (StringUtils.isEmpty(request.getClientRequestToken())) {
            logger.log(String.format("ClientToken is Required, but a client request token was not provided."));
            return ProgressEvent.failed(model, callbackContext, HandlerErrorCode.InvalidRequest, "ClientToken was not provided.");
        }

        // If application id regex is null or invalid, return NotFound to avoid stuck Delete-Failed state in CFN after invalid request
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

        DeleteApplicationRequest deleteRequest = Translator.translateToDeleteRequest(request, model);

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
}
