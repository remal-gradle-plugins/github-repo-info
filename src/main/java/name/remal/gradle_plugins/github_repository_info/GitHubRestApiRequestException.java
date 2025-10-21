package name.remal.gradle_plugins.github_repository_info;

public abstract class GitHubRestApiRequestException extends RuntimeException {

    private GitHubRestApiRequestException(String message) {
        super(message);
    }

    private GitHubRestApiRequestException(String message, Throwable cause) {
        super(message, cause);
    }


    public static class Retryable extends GitHubRestApiRequestException {
        Retryable(String message) {
            super(message);
        }

        Retryable(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class NotRetryable extends GitHubRestApiRequestException {
        NotRetryable(String message) {
            super(message);
        }

        NotRetryable(String message, Throwable cause) {
            super(message, cause);
        }
    }

}
