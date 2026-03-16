package ca.douglas.csis4280.nwtrails.api;

import ca.douglas.csis4280.nwtrails.api.dto.UserProgressResponse;
import ca.douglas.csis4280.nwtrails.service.NwTrailsService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/progress")
public class ProgressController {

    private final NwTrailsService nwTrailsService;

    public ProgressController(NwTrailsService nwTrailsService) {
        this.nwTrailsService = nwTrailsService;
    }

    @GetMapping("/me")
    public UserProgressResponse getMyProgress(Authentication authentication) {
        return nwTrailsService.getProgress(authentication.getName());
    }
}
