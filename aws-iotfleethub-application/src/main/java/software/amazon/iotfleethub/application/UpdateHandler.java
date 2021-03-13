package software.amazon.iotfleethub.application;

import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.lang3.StringUtils;
import software.amazon.awssdk.services.iotfleethub.IoTFleetHubClient;
import software.amazon.awssdk.services.iotfleethub.model.DescribeApplicationRequest;
import software.amazon.awssdk.services.iotfleethub.model.DescribeApplicationResponse;
import software.amazon.awssdk.services.iotfleethub.model.ResourceNotFoundException;
import software.amazon.awssdk.services.iotfleethub.model.TagResourceRequest;
import software.amazon.awssdk.services.iotfleethub.model.UntagResourceRequest;
import software.amazon.awssdk.services.iotfleethub.model.UpdateApplicationRequest;
import software.amazon.awssdk.services.iotfleethub.model.UpdateApplicationResponse;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class UpdateHandler extends BaseHandler<CallbackContext> {

    private final IoTFleetHubClient iotFleetHubClient;

    public UpdateHandler() {
        iotFleetHubClient = IoTFleetHubClient.builder().build();
    }

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final Logger logger) {

        ResourceModel model = request.getDesiredResourceState();

        // UpdateHandler must return a NotFound error if the ApplicationId is not provided
        if (model.getApplicationId() == null) {
            logger.log(String.format("ApplicationId was not provided."));
            return ProgressEvent.failed(model, callbackContext, HandlerErrorCode.NotFound, "ApplicationId was not provided.");
        }

        if (StringUtils.isEmpty(request.getClientRequestToken())) {
            logger.log(String.format("ClientToken is Required, but a client request token was not provided."));
            return ProgressEvent.failed(model, callbackContext, HandlerErrorCode.InvalidRequest, "ClientToken was not provided.");
        }

        if (Translator.isReadOnlyFieldSet(model, logger, "ApplicationArn", model.getApplicationArn())
                || Translator.isReadOnlyFieldSet(model, logger, "ApplicationUrl", model.getApplicationUrl())
                || Translator.isReadOnlyFieldSet(model, logger, "ApplicationState", model.getApplicationState())
                || Translator.isReadOnlyFieldSet(model, logger, "SsoClientId", model.getSsoClientId())
                || Translator.isReadOnlyFieldSet(model, logger, "ErrorMessage", model.getErrorMessage())) {

            return ProgressEvent.failed(model, callbackContext, HandlerErrorCode.InvalidRequest,
                    "Can only update ApplicationName, ApplicationDescription, or Tags.");
        }

        UpdateApplicationRequest updateRequest = Translator.translateToUpdateRequest(request, model);

        try {
            proxy.injectCredentialsAndInvokeV2(updateRequest, iotFleetHubClient::updateApplication);
        } catch (ResourceNotFoundException e) {
            logger.log(String.format("Application with Id %s was not found", model.getApplicationId()));
        } catch (RuntimeException e) {
            HandlerErrorCode err = Translator.translateExceptionToErrorCode(e, logger);
            return ProgressEvent.failed(model, callbackContext, err, e.getMessage());
        }

        // Retrieving applicationArn to update tags
        DescribeApplicationRequest describeRequest = DescribeApplicationRequest.builder()
                .applicationId(model.getApplicationId())
                .build();

        DescribeApplicationResponse describeResponse;
        try {
            describeResponse = proxy.injectCredentialsAndInvokeV2(describeRequest, iotFleetHubClient::describeApplication);
        } catch (ResourceNotFoundException e) {
            logger.log(String.format("Application with Id %s was not found", model.getApplicationId()));
            return ProgressEvent.failed(model, callbackContext, HandlerErrorCode.NotFound, e.getMessage());
        } catch (RuntimeException e) {
            HandlerErrorCode err = Translator.translateExceptionToErrorCode(e, logger);
            return ProgressEvent.failed(model, callbackContext, err, e.getMessage());
        }

        String applicationArn = describeResponse.applicationArn();
        Map<String, String> currentTags = describeResponse.tags();

        try {
            updateTags(proxy, request, applicationArn, currentTags, logger);
        } catch (RuntimeException e) {
            HandlerErrorCode err = Translator.translateExceptionToErrorCode(e, logger);
            return ProgressEvent.failed(model, callbackContext, err, e.getMessage());
        }

        logger.log(String.format("Updated Application with Id %s.", model.getApplicationId()));
        return ProgressEvent.defaultSuccessHandler(request.getDesiredResourceState());
    }

    void updateTags(AmazonWebServicesClientProxy proxy,
                    ResourceHandlerRequest<ResourceModel> request,
                    String applicationArn,
                    Map<String, String> currentTags,
                    Logger logger) {

        Map<String, String> desiredTags = new HashMap<>();
        ResourceModel model = request.getDesiredResourceState();
        if (model.getTags() != null) {
            for (Tag t : model.getTags()) {
                desiredTags.put(t.getKey(), t.getValue());
            }
        }
        if (request.getDesiredResourceTags() != null) {
            desiredTags.putAll(request.getDesiredResourceTags());
        }

        // Add Tags
        Map<String, String> tagsToAdd = new HashMap<>();
        for (Map.Entry<String,String> tagEntry : desiredTags.entrySet()) {
            String currentTagValue = currentTags.get(tagEntry.getKey());
            if (currentTagValue == null || !currentTagValue.equals(tagEntry.getValue())) {
                tagsToAdd.put(tagEntry.getKey(), tagEntry.getValue());
            }
        }

        if (!tagsToAdd.isEmpty()) {
            TagResourceRequest tagRequest = TagResourceRequest.builder()
                    .resourceArn(applicationArn)
                    .tags(tagsToAdd)
                    .build();
            proxy.injectCredentialsAndInvokeV2(tagRequest, iotFleetHubClient::tagResource);
            logger.log(String.format("Called TagResource for %s.", applicationArn));
        }

        // Remove Tags
        Collection<String> tagKeysToRemove = new HashSet<>();
        for (Map.Entry<String,String> tagEntry : currentTags.entrySet()) {
            String currentKey = tagEntry.getKey();
            if (desiredTags.get(currentKey) == null) {
                tagKeysToRemove.add(currentKey);
            }
        }

        if (!tagKeysToRemove.isEmpty()) {
            UntagResourceRequest untagRequest = UntagResourceRequest.builder()
                    .resourceArn(applicationArn)
                    .tagKeys(tagKeysToRemove)
                    .build();
            proxy.injectCredentialsAndInvokeV2(untagRequest, iotFleetHubClient::untagResource);
            logger.log(String.format("Called UntagResource for %s.", applicationArn));
        }
    }
}
