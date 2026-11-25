package lk.sliit.ieee.eventfinance;
import java.util.List;
import lk.sliit.ieee.eventfinance.dto.*;
import lk.sliit.ieee.eventfinance.entity.*;
import lk.sliit.ieee.eventfinance.repository.ChapterUserRepository;
import lk.sliit.ieee.eventfinance.service.ChapterUserService;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class ChapterUserServiceTests {
    @Test void retainsTheLastActiveChairAndRejectsOversizedPasswords() {
        var repository = mock(ChapterUserRepository.class);
        var chair = mock(ChapterUser.class);
        when(chair.getId()).thenReturn(1L); when(chair.isAccountAdmin()).thenReturn(true); when(chair.isEnabled()).thenReturn(true);
        when(repository.lockAllForAccessUpdate()).thenReturn(List.of(chair));
        var service = new ChapterUserService(repository, mock(lk.sliit.ieee.eventfinance.repository.RoleRepository.class), mock(lk.sliit.ieee.eventfinance.repository.RefreshTokenRepository.class), mock(PasswordEncoder.class));
        assertEquals(409, assertThrows(ResponseStatusException.class, () -> service.update(1L, new UpdateUserAccessRequest(ChapterRole.VOLUNTEER, true, false))).getStatusCode().value());
        assertEquals(409, assertThrows(ResponseStatusException.class, () -> service.update(1L, new UpdateUserAccessRequest(ChapterRole.COMMITTEE_MEMBER, false, true))).getStatusCode().value());
        verify(chair, never()).updateAccess(any(), anyBoolean(), anyBoolean());
        assertThrows(ResponseStatusException.class, () -> ChapterUserService.validatePassword("界".repeat(30)));
        assertThrows(ResponseStatusException.class, () -> ChapterUserService.validatePassword(" ".repeat(20)));
    }
}
