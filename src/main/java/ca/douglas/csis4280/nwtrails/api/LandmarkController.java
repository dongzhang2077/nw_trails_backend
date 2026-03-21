package ca.douglas.csis4280.nwtrails.api;

import ca.douglas.csis4280.nwtrails.api.dto.CreateLandmarkRequest;
import ca.douglas.csis4280.nwtrails.api.dto.UpdateLandmarkRequest;
import ca.douglas.csis4280.nwtrails.domain.Landmark;
import ca.douglas.csis4280.nwtrails.domain.LandmarkCategory;
import ca.douglas.csis4280.nwtrails.repository.LandmarkRepository;
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
@Tag(name = "Landmarks")
public class LandmarkController {

    private final NwTrailsService nwTrailsService;
    private final LandmarkRepository landmarkRepository;

    public LandmarkController(NwTrailsService nwTrailsService, LandmarkRepository landmarkRepository) {
        this.nwTrailsService = nwTrailsService;
        this.landmarkRepository = landmarkRepository;
    }

    @GetMapping("/landmarks")
    public Map<String, List<Landmark>> listLandmarks(
            @RequestParam(required = false) LandmarkCategory category,
            @RequestParam(required = false, name = "q") String query) {
        List<Landmark> results;
        if (category != null && query != null)
            results = landmarkRepository.findByCategoryAndNameContainingIgnoreCase(category, query);
        else if (category != null)
            results = landmarkRepository.findByCategory(category);
        else if (query != null)
            results = landmarkRepository.findByNameContainingIgnoreCase(query);
        else
            results = landmarkRepository.findAll();
        return Map.of("items", results);
    }

    @GetMapping("/landmarks/{landmarkId}")
    public Landmark getLandmarkById(@PathVariable String landmarkId) {
        return landmarkRepository.findById(landmarkId).orElse(null);
    }

    @PostMapping("/admin/landmarks")
    @ResponseStatus(HttpStatus.CREATED)
    public Landmark createLandmark(@Valid @RequestBody CreateLandmarkRequest request) {
        return landmarkRepository.save(new Landmark(
                null,
                request.name(),
                request.category(),
                request.address(),
                request.description(),
                request.latitude(),
                request.longitude(),
                request.imageUrl(),
                request.rating()));
    }

    @PutMapping("/admin/landmarks/{landmarkId}")
    public Landmark updateLandmark(
            @PathVariable String landmarkId,
            @Valid @RequestBody UpdateLandmarkRequest request) {
        Landmark existing = landmarkRepository.findById(landmarkId)
                .orElseThrow(() -> new RuntimeException("Landmark not found: " + landmarkId));
        return landmarkRepository.save(new Landmark(
                existing.id(),
                request.name(),
                request.category(),
                request.address(),
                request.description(),
                request.latitude(),
                request.longitude(),
                request.imageUrl(),
                request.rating()));
    }

    @DeleteMapping("/admin/landmarks/{landmarkId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteLandmark(@PathVariable String landmarkId) {
        landmarkRepository.deleteById(landmarkId);
    }
}
