package name.remal.gradle_plugins.github_repository_info;

import java.time.Duration;
import lombok.Getter;
import org.jspecify.annotations.Nullable;

public abstract class HttpRequestException extends RuntimeException {

    private HttpRequestException(String message) {
        super(message);
    }


    @Getter
    public static class Retryable extends HttpRequestException {

        @Nullable
        private final Duration retryAfter;

        Retryable(String message, @Nullable Duration retryAfter) {
            super(message);
            this.retryAfter = retryAfter;
        }

        Retryable(String message, @Nullable Duration retryAfter, @Nullable Throwable cause) {
            this(message, retryAfter);
            initCause(cause);
        }

    }

    public static class NotRetryable extends HttpRequestException {
        NotRetryable(String message) {
            super(message);
        }

        NotRetryable(String message, @Nullable Throwable cause) {
            this(message);
            initCause(cause);
        }
    }

}
