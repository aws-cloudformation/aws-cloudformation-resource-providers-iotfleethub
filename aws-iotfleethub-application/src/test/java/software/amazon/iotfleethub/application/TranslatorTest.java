package software.amazon.iotfleethub.application;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Test;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import software.amazon.awssdk.services.iotfleethub.model.ConflictException;
import software.amazon.awssdk.services.iotfleethub.model.InternalFailureException;
import software.amazon.awssdk.services.iotfleethub.model.LimitExceededException;
import software.amazon.awssdk.services.iotfleethub.model.ThrottlingException;

import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class TranslatorTest {

    @Mock
    private Logger logger;

    private AutoCloseable closeable;

    @BeforeEach
    public void setup() {
        closeable = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    public void tear_down() throws Exception {
        closeable.close();
    }

    @Test
    public void translateExceptionToErrorCode_ConflictErrorCode() {
        HandlerErrorCode result = Translator.translateExceptionToErrorCode(ConflictException.builder().build(), logger);
        assertThat(result).isEqualByComparingTo(HandlerErrorCode.ResourceConflict);
    }

    @Test
    public void translateExceptionToErrorCode_InternalFailureErrorCode() {
        HandlerErrorCode result = Translator.translateExceptionToErrorCode(InternalFailureException.builder().build(), logger);
        assertThat(result).isEqualByComparingTo(HandlerErrorCode.InternalFailure);
    }

    @Test
    public void translateExceptionToErrorCode_LimitExceededErrorCode() {
        HandlerErrorCode result = Translator.translateExceptionToErrorCode(LimitExceededException.builder().build(), logger);
        assertThat(result).isEqualByComparingTo(HandlerErrorCode.ServiceLimitExceeded);
    }

    @Test
    public void translateExceptionToErrorCode_ThrottlingErrorCode() {
        HandlerErrorCode result = Translator.translateExceptionToErrorCode(ThrottlingException.builder().build(), logger);
        assertThat(result).isEqualByComparingTo(HandlerErrorCode.Throttling);
    }
}
