package ca.douglas.csis4280.nwtrails.api;

import ca.douglas.csis4280.nwtrails.api.dto.UserProgressResponse;
import ca.douglas.csis4280.nwtrails.service.NwTrailsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/progress")
@Tag(name = "Progress")
public class ProgressController {

    private final NwTrailsService nwTrailsService;

    public ProgressController(NwTrailsService nwTrailsService) {
        this.nwTrailsService = nwTrailsService;
    }

    @GetMapping("/me")
    @Operation(summary = "Get badge and route progress for current user")
    public UserProgressResponse getMyProgress(Authentication authentication) {
        return nwTrailsService.getProgress(authentication.getName());
    }
}
