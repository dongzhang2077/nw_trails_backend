package ca.douglas.csis4280.nwtrails.api;

import ca.douglas.csis4280.nwtrails.api.dto.CreateLandmarkRequest;
import ca.douglas.csis4280.nwtrails.api.dto.UpdateLandmarkRequest;
import ca.douglas.csis4280.nwtrails.domain.Landmark;
import ca.douglas.csis4280.nwtrails.domain.LandmarkCategory;
import ca.douglas.csis4280.nwtrails.service.NwTrailsService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class LandmarkController {

    private final NwTrailsService nwTrailsService;

    public LandmarkController(NwTrailsService nwTrailsService) {
        this.nwTrailsService = nwTrailsService;
    }

    @GetMapping("/landmarks")
    public Map<String, List<Landmark>> listLandmarks(
        @RequestParam(required = false) LandmarkCategory category,
        @RequestParam(required = false, name = "q") String query
    ) {
        return Map.of("items", nwTrailsService.listLandmarks(category, query));
    }

    @GetMapping("/landmarks/{landmarkId}")
    public Landmark getLandmarkById(@PathVariable String landmarkId) {
        return nwTrailsService.getLandmarkById(landmarkId);
    }

    @PostMapping("/admin/landmarks")
    @ResponseStatus(HttpStatus.CREATED)
    public Landmark createLandmark(@Valid @RequestBody CreateLandmarkRequest request) {
        return nwTrailsService.createLandmark(request);
    }

    @PutMapping("/admin/landmarks/{landmarkId}")
    public Landmark updateLandmark(
        @PathVariable String landmarkId,
        @Valid @RequestBody UpdateLandmarkRequest request
    ) {
        return nwTrailsService.updateLandmark(landmarkId, request);
    }

    @DeleteMapping("/admin/landmarks/{landmarkId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteLandmark(@PathVariable String landmarkId) {
        nwTrailsService.deleteLandmark(landmarkId);
    }
}
