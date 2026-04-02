package ca.douglas.csis4280.nwtrails.api;

import ca.douglas.csis4280.nwtrails.api.dto.CheckInResultResponse;
import ca.douglas.csis4280.nwtrails.api.dto.CreateCheckInRequest;
import ca.douglas.csis4280.nwtrails.domain.CheckInPeriod;
import ca.douglas.csis4280.nwtrails.domain.CheckInRecord;
import ca.douglas.csis4280.nwtrails.service.NwTrailsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

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
    @Operation(summary = "Create check-in for a landmark", description = """
            Business rules:
            - Same user cannot check in to same landmark more than once per day.
            - User must be within 50 meters of the landmark.
            """)
    public CheckInResultResponse createCheckIn(
            Authentication authentication,
            @Valid @RequestBody CreateCheckInRequest request) {
        return nwTrailsService.createCheckIn(authentication.getName(), request);
    }

    @PostMapping(path = "/photos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, String> uploadCheckInPhoto(
            Authentication authentication,
            @RequestPart("file") MultipartFile file) {
        String photoUrl = nwTrailsService.uploadCheckInPhoto(authentication.getName(), file);
        return Map.of("photoUrl", photoUrl);
    }

    @GetMapping("/photos/{photoId}")
    public ResponseEntity<Resource> getCheckInPhoto(
            Authentication authentication,
            @PathVariable String photoId) {
        Resource resource = nwTrailsService.loadCheckInPhoto(authentication.getName(), photoId);
        MediaType mediaType = MediaTypeFactory
            .getMediaType(resource)
            .orElse(MediaType.APPLICATION_OCTET_STREAM);
        return ResponseEntity.ok().contentType(mediaType).body(resource);
    }

    @GetMapping
    public Map<String, List<CheckInRecord>> listMyCheckIns(
            Authentication authentication,
            @RequestParam(defaultValue = "ALL") CheckInPeriod period) {
        return Map.of("items", nwTrailsService.listCheckIns(authentication.getName(), period));
    }
}
