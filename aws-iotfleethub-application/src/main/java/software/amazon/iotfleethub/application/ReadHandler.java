package software.amazon.iotfleethub.application;

import com.google.common.annotations.VisibleForTesting;
import software.amazon.awssdk.services.iotfleethub.IoTFleetHubClient;
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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class ReadHandler extends BaseHandler<CallbackContext> {

    private final IoTFleetHubClient iotFleetHubClient;

    public ReadHandler() {
        iotFleetHubClient = IoTFleetHubClient.builder().build();
    }

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final Logger logger) {

        ResourceModel model = request.getDesiredResourceState();

        // ReadHandler must return NotFound error if the ApplicationId is not provided
        if (model.getApplicationId() == null) {
            logger.log(String.format("ApplicationId was not provided."));
            return ProgressEvent.failed(model, callbackContext, HandlerErrorCode.NotFound, "ApplicationId was not provided.");
        }

        DescribeApplicationRequest describeRequest = Translator.translateToDescribeRequest(model);

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

        String applicationId = describeResponse.applicationId();
        String applicationName = describeResponse.applicationName();
        logger.log(String.format("Described Application %s, named %s.", applicationId, applicationName));

        Map<String, String> tagMap = describeResponse.tags();
        Set<Tag> tagSet = new HashSet<Tag>();

        if (tagMap != null) {
            for (Map.Entry<String,String> tagEntry : tagMap.entrySet()) {
                Tag tag = Tag.builder()
                        .key(tagEntry.getKey())
                        .value(tagEntry.getValue())
                        .build();
                tagSet.add(tag);
            }
        }

        return ProgressEvent.defaultSuccessHandler(
                ResourceModel.builder()
                        .applicationId(applicationId)
                        .applicationArn(describeResponse.applicationArn())
                        .applicationName(applicationName)
                        .applicationDescription(describeResponse.applicationDescription())
                        .applicationUrl(describeResponse.applicationUrl())
                        .applicationState(describeResponse.applicationStateAsString())
                        .applicationCreationDate(describeResponse.applicationCreationDate().intValue())
                        .applicationLastUpdateDate(describeResponse.applicationLastUpdateDate().intValue())
                        .roleArn(describeResponse.roleArn())
                        .ssoClientId(describeResponse.ssoClientId())
                        .errorMessage(describeResponse.errorMessage())
                        .tags(tagSet)
                        .build());
    }
}
