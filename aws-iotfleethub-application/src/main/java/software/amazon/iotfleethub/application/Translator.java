package software.amazon.iotfleethub.application;

import org.apache.commons.lang3.exception.ExceptionUtils;
import software.amazon.awssdk.services.iotfleethub.model.ConflictException;
import software.amazon.awssdk.services.iotfleethub.model.InternalFailureException;
import software.amazon.awssdk.services.iotfleethub.model.InvalidRequestException;
import software.amazon.awssdk.services.iotfleethub.model.LimitExceededException;
import software.amazon.awssdk.services.iotfleethub.model.ResourceNotFoundException;
import software.amazon.awssdk.services.iotfleethub.model.ThrottlingException;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;

import com.google.common.collect.Lists;
import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.awscore.AwsResponse;

import java.util.Collection;
import java.util.List;
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

    // https://code.amazon.com/packages/AwsJavaSdk-IoTFleetHub/blobs/aws-sdk-java-v2/release/--/src/software/amazon/awssdk/services/iotfleethub/IoTFleetHubClient.java
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
}
