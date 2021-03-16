package software.amazon.iotfleethub.application;

import org.apache.commons.lang3.StringUtils;
import software.amazon.awssdk.awscore.AwsResponse;
import software.amazon.awssdk.awscore.exception.AwsServiceException;

import software.amazon.awssdk.services.iotfleethub.IoTFleetHubClient;
import software.amazon.awssdk.services.iotfleethub.model.CreateApplicationRequest;
import software.amazon.awssdk.services.iotfleethub.model.CreateApplicationResponse;
import software.amazon.awssdk.services.iotfleethub.model.TagResourceRequest;

import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.HashMap;
import java.util.Map;

public class CreateHandler extends BaseHandler<CallbackContext> {

    private final IoTFleetHubClient iotFleetHubClient;

    public CreateHandler() {
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
            logger.log(String.format("ClientToken is required, but a client request token was not provided."));
            return ProgressEvent.failed(model, callbackContext, HandlerErrorCode.InvalidRequest, "ClientToken was not provided.");
        }

        if (Translator.isReadOnlyFieldSet(model, logger, "ApplicationArn", model.getApplicationArn())
                || Translator.isReadOnlyFieldSet(model, logger, "ApplicationId", model.getApplicationId())
                || Translator.isReadOnlyFieldSet(model, logger, "ApplicationUrl", model.getApplicationUrl())
                || Translator.isReadOnlyFieldSet(model, logger, "ApplicationState", model.getApplicationState())
                || Translator.isReadOnlyFieldSet(model, logger, "SsoClientId", model.getSsoClientId())
                || Translator.isReadOnlyFieldSet(model, logger, "ErrorMessage", model.getErrorMessage())) {

            return ProgressEvent.failed(model, callbackContext, HandlerErrorCode.InvalidRequest,
                    "Can only set ApplicationName, RoleArn, ApplicationDescription (optional), and Tags (optional) in CreateApplication call.");
        }

        CreateApplicationRequest createRequest = Translator.translateToCreateRequest(request, model, logger);

        CreateApplicationResponse createResponse;
        try {
            createResponse = proxy.injectCredentialsAndInvokeV2(createRequest, iotFleetHubClient::createApplication);
        } catch (RuntimeException e) {
            HandlerErrorCode err = Translator.translateExceptionToErrorCode(e, logger);
            return ProgressEvent.failed(model, callbackContext, err, e.getMessage());
        }

        model.setApplicationArn(createResponse.applicationArn());
        model.setApplicationId(createResponse.applicationId());

        logger.log(String.format("Created Application with Arn %s and Id %s",
                createResponse.applicationArn(), createResponse.applicationId()));

        return ProgressEvent.defaultSuccessHandler(model);
    }
}
