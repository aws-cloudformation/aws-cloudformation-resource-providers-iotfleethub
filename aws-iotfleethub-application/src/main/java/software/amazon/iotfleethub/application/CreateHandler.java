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

        // ClientToken is required for idempotent Create Operation
        if (StringUtils.isEmpty(request.getClientRequestToken())) {
            logger.log(String.format("ClientToken is Required, but a client request token was not provided."));
            return ProgressEvent.failed(model, callbackContext, HandlerErrorCode.InvalidRequest, "ClientToken was not provided.");
        }

        // The following fields are Read-Only for this operation, so we don't want them being set in the model
        // These checks do not affect the call to CreateApp API itself, but we don't want incorrect info in the returned model
        if (SetReadOnlyFields(model, logger, "ApplicationArn", model.getApplicationArn())
                || SetReadOnlyFields(model, logger, "ApplicationId", model.getApplicationId())
                || SetReadOnlyFields(model, logger, "ApplicationUrl", model.getApplicationUrl())
                || SetReadOnlyFields(model, logger, "ApplicationState", model.getApplicationState())
                || SetReadOnlyFields(model, logger, "SsoClientId", model.getSsoClientId())
                || SetReadOnlyFields(model, logger, "ErrorMessage", model.getErrorMessage())) {

            return ProgressEvent.failed(model, callbackContext, HandlerErrorCode.InvalidRequest,
                    "Can only set ApplicationName, RoleArn, ApplicationDescription (optional), and Tags (optional) in CreateApplication call.");
        }

        CreateApplicationRequest createRequest = translateToCreateRequest(request, model, logger);

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

    private boolean SetReadOnlyFields(
            ResourceModel model,
            Logger logger,
            String fieldType,
            String fieldValue) {

        if (!StringUtils.isEmpty(fieldValue)) {
            logger.log(String.format("%s is Read-Only, but the caller passed %s.", fieldType, fieldValue));
            return true;
        }
        return false;
    }

    private CreateApplicationRequest translateToCreateRequest(
            ResourceHandlerRequest<ResourceModel> request,
            ResourceModel model,
            Logger logger) {

        Map<String, String> tags = new HashMap<>();
        if (model.getTags() != null) {
            // Including this because some tag errors occurring when testing while just using DesiredResourceTags below
            for (Tag t : model.getTags()) {
                tags.put(t.getKey(), t.getValue());
            }
        }
        if (request.getDesiredResourceTags() != null) {
            // DesiredResourceTags includes both model and stack-level tags.
            // Reference: https://tinyurl.com/yyxtd7w6
            tags.putAll(request.getDesiredResourceTags());
        }

        return CreateApplicationRequest.builder()
                .applicationName(model.getApplicationName())
                .applicationDescription(model.getApplicationDescription())
                .clientToken(request.getClientRequestToken())
                .roleArn(model.getRoleArn())
                .tags(tags)
                .build();
    }
}
