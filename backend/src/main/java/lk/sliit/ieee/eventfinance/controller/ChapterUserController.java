package lk.sliit.ieee.eventfinance.controller;
import java.util.List;
import jakarta.validation.Valid;
import lk.sliit.ieee.eventfinance.dto.*;
import lk.sliit.ieee.eventfinance.service.ChapterUserService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/users")
public class ChapterUserController {
    private final ChapterUserService users;
    public ChapterUserController(ChapterUserService users) { this.users = users; }
    @GetMapping public List<ChapterAccountResponse> list() { return users.list(); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public UserResponse create(@Valid @RequestBody CreateUserRequest request) { return users.create(request); }
    @PutMapping("/{id}") public UserResponse update(@PathVariable Long id, @Valid @RequestBody UpdateUserAccessRequest request) { return users.update(id, request); }
}
