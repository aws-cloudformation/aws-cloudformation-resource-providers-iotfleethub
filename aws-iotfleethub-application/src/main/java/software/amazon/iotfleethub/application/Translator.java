package software.amazon.iotfleethub.application;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.StringUtils;
import software.amazon.awssdk.services.iotfleethub.model.ConflictException;
import software.amazon.awssdk.services.iotfleethub.model.CreateApplicationRequest;
import software.amazon.awssdk.services.iotfleethub.model.DeleteApplicationRequest;
import software.amazon.awssdk.services.iotfleethub.model.DescribeApplicationRequest;
import software.amazon.awssdk.services.iotfleethub.model.InternalFailureException;
import software.amazon.awssdk.services.iotfleethub.model.InvalidRequestException;
import software.amazon.awssdk.services.iotfleethub.model.LimitExceededException;
import software.amazon.awssdk.services.iotfleethub.model.ListApplicationsRequest;
import software.amazon.awssdk.services.iotfleethub.model.ResourceNotFoundException;
import software.amazon.awssdk.services.iotfleethub.model.ThrottlingException;
import software.amazon.awssdk.services.iotfleethub.model.UpdateApplicationRequest;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import com.google.common.collect.Lists;
import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.awscore.AwsResponse;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This class is a centralized placeholder for
 *  - api request construction
 *  - object translation to/from aws sdk
 *  - resource model construction for read/list handlers
 */

public class Translator {

  static HandlerErrorCode translateExceptionToErrorCode(Exception e, Logger logger) {
    logger.log(String.format("Translating exception \"%s\", stack trace: %s",
            e.getMessage(), ExceptionUtils.getStackTrace(e)));

    // https://docs.aws.amazon.com/iot/latest/apireference/API_Operations_AWS_IoT_Fleet_Hub.html
    if (e instanceof ConflictException) {
      return HandlerErrorCode.ResourceConflict;
    } else if (e instanceof InternalFailureException) {
      return HandlerErrorCode.InternalFailure;
    } else if (e instanceof InvalidRequestException) {
      return HandlerErrorCode.InvalidRequest;
    } else if (e instanceof LimitExceededException) {
      return HandlerErrorCode.ServiceLimitExceeded;
    } else if (e instanceof ResourceNotFoundException) {
      return HandlerErrorCode.NotFound;
    } else if (e instanceof ThrottlingException) {
      return HandlerErrorCode.Throttling;
    } else {
      logger.log(String.format("Unexpected exception \"%s\", stack trace: %s",
              e.getMessage(), ExceptionUtils.getStackTrace(e)));
      // Any other exception at this point is unexpected.
      return HandlerErrorCode.InternalFailure;
    }
  }

  static CreateApplicationRequest translateToCreateRequest(
          ResourceHandlerRequest<ResourceModel> request,
          ResourceModel model,
          Logger logger) {

    Map<String, String> tags = new HashMap<>();
    if (model.getTags() != null) {
      for (Tag t : model.getTags()) {
        tags.put(t.getKey(), t.getValue());
      }
    }
    if (request.getDesiredResourceTags() != null) {
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

  static DeleteApplicationRequest translateToDeleteRequest(
          ResourceHandlerRequest<ResourceModel> request,
          ResourceModel model) {

    return DeleteApplicationRequest.builder()
            .applicationId(model.getApplicationId())
            .clientToken(request.getClientRequestToken())
            .build();
  }

  static ListApplicationsRequest translateToListRequest(ResourceHandlerRequest<ResourceModel> request) {
    return ListApplicationsRequest.builder()
            .nextToken(request.getNextToken())
            .build();
  }

  static DescribeApplicationRequest translateToDescribeRequest(ResourceModel model) {
    return DescribeApplicationRequest.builder()
            .applicationId(model.getApplicationId())
            .build();
  }

  static UpdateApplicationRequest translateToUpdateRequest(
          ResourceHandlerRequest<ResourceModel> request,
          ResourceModel model) {

    return UpdateApplicationRequest.builder()
            .applicationId(model.getApplicationId())
            .applicationName(model.getApplicationName())
            .applicationDescription(model.getApplicationDescription())
            .clientToken(request.getClientRequestToken())
            .build();
  }

  static boolean isReadOnlyFieldSet(
          Logger logger,
          String fieldType,
          String fieldValue) {

    if (!StringUtils.isEmpty(fieldValue)) {
      logger.log(String.format("%s is Read-Only, but the caller passed %s.", fieldType, fieldValue));
      return true;
    }
    return false;
  }

  static boolean isReadOnlyFieldChanged(
          Logger logger,
          String fieldType,
          String previousFieldValue,
          String currentFieldValue) {

    if (!StringUtils.equals(previousFieldValue, currentFieldValue)) {
      logger.log(String.format("%s is Read-Only, but the caller attempted to modify %s to %s", fieldType, previousFieldValue, currentFieldValue));
      return true;
    }
    return false;
  }
}
