package ca.douglas.csis4280.nwtrails.api;

import ca.douglas.csis4280.nwtrails.api.dto.CreateRouteRequest;
import ca.douglas.csis4280.nwtrails.api.dto.RouteProgressResponse;
import ca.douglas.csis4280.nwtrails.api.dto.UpdateRouteRequest;
import ca.douglas.csis4280.nwtrails.domain.RoutePlan;
import ca.douglas.csis4280.nwtrails.service.NwTrailsService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
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
public class RouteController {

    private final NwTrailsService nwTrailsService;

    public RouteController(NwTrailsService nwTrailsService) {
        this.nwTrailsService = nwTrailsService;
    }

    @GetMapping("/routes")
    public Map<String, List<RoutePlan>> listRoutes(
        @RequestParam(required = false) String difficulty
    ) {
        return Map.of("items", nwTrailsService.listRoutes(difficulty));
    }

    @GetMapping("/routes/{routeId}")
    public RoutePlan getRouteById(@PathVariable String routeId) {
        return nwTrailsService.getRouteById(routeId);
    }

    @PostMapping("/routes/{routeId}/start")
    public RouteProgressResponse startRoute(
        Authentication authentication,
        @PathVariable String routeId
    ) {
        return nwTrailsService.startRoute(authentication.getName(), routeId);
    }

    @PostMapping("/admin/routes")
    @ResponseStatus(HttpStatus.CREATED)
    public RoutePlan createRoute(@Valid @RequestBody CreateRouteRequest request) {
        return nwTrailsService.createRoute(request);
    }

    @PutMapping("/admin/routes/{routeId}")
    public RoutePlan updateRoute(
        @PathVariable String routeId,
        @Valid @RequestBody UpdateRouteRequest request
    ) {
        return nwTrailsService.updateRoute(routeId, request);
    }

    @DeleteMapping("/admin/routes/{routeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRoute(@PathVariable String routeId) {
        nwTrailsService.deleteRoute(routeId);
    }
}
