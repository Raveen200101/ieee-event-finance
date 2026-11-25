package lk.sliit.ieee.eventfinance.exception;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
public class RefreshTokenRejectedException extends ResponseStatusException {
    public RefreshTokenRejectedException() { super(HttpStatus.UNAUTHORIZED, "Refresh token is invalid or expired. Sign in again."); }
}
