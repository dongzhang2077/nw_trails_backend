package ca.douglas.csis4280.nwtrails.api;

import ca.douglas.csis4280.nwtrails.api.dto.CheckInResultResponse;
import ca.douglas.csis4280.nwtrails.api.dto.CreateCheckInRequest;
import ca.douglas.csis4280.nwtrails.domain.CheckInPeriod;
import ca.douglas.csis4280.nwtrails.domain.CheckInRecord;
import ca.douglas.csis4280.nwtrails.service.NwTrailsService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/checkins")
@Tag(name = "CheckIns")
public class CheckInController {

    private final NwTrailsService nwTrailsService;

    public CheckInController(NwTrailsService nwTrailsService) {
        this.nwTrailsService = nwTrailsService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CheckInResultResponse createCheckIn(
            Authentication authentication,
            @Valid @RequestBody CreateCheckInRequest request) {
        return nwTrailsService.createCheckIn(authentication.getName(), request);
    }

    @GetMapping
    public Map<String, List<CheckInRecord>> listMyCheckIns(
            Authentication authentication,
            @RequestParam(defaultValue = "ALL") CheckInPeriod period) {
        return Map.of("items", nwTrailsService.listCheckIns(authentication.getName(), period));
    }
}
