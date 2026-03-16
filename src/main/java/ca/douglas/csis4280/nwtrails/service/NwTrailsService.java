package ca.douglas.csis4280.nwtrails.service;

import ca.douglas.csis4280.nwtrails.api.dto.BadgeProgressResponse;
import ca.douglas.csis4280.nwtrails.api.dto.CategoryProgressResponse;
import ca.douglas.csis4280.nwtrails.api.dto.CheckInResultResponse;
import ca.douglas.csis4280.nwtrails.api.dto.CreateCheckInRequest;
import ca.douglas.csis4280.nwtrails.api.dto.CreateLandmarkRequest;
import ca.douglas.csis4280.nwtrails.api.dto.CreateRouteRequest;
import ca.douglas.csis4280.nwtrails.api.dto.RouteProgressResponse;
import ca.douglas.csis4280.nwtrails.api.dto.UpdateLandmarkRequest;
import ca.douglas.csis4280.nwtrails.api.dto.UpdateRouteRequest;
import ca.douglas.csis4280.nwtrails.api.dto.UserProgressResponse;
import ca.douglas.csis4280.nwtrails.common.ApiException;
import ca.douglas.csis4280.nwtrails.domain.CheckInPeriod;
import ca.douglas.csis4280.nwtrails.domain.CheckInRecord;
import ca.douglas.csis4280.nwtrails.domain.Landmark;
import ca.douglas.csis4280.nwtrails.domain.LandmarkCategory;
import ca.douglas.csis4280.nwtrails.domain.RoutePlan;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

@Service
public class NwTrailsService {

    private static final int CHECK_IN_MAX_DISTANCE_METERS = 50;

    private final Map<String, Landmark> landmarks = new LinkedHashMap<>();
    private final Map<String, RoutePlan> routes = new LinkedHashMap<>();

    private final Map<String, List<CheckInRecord>> checkInsByUser = new ConcurrentHashMap<>();
    private final Map<String, String> activeRouteIdByUser = new ConcurrentHashMap<>();

    private final AtomicLong checkInIdSequence = new AtomicLong(1);
    private final AtomicLong landmarkIdSequence = new AtomicLong(100);
    private final AtomicLong routeIdSequence = new AtomicLong(100);

    public NwTrailsService() {
        seedLandmarks();
        seedRoutes();
    }

    public synchronized List<Landmark> listLandmarks(
        @Nullable LandmarkCategory category,
        @Nullable String query
    ) {
        String normalizedQuery = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);

        return landmarks
            .values()
            .stream()
            .filter(landmark -> category == null || landmark.category() == category)
            .filter(landmark -> {
                if (normalizedQuery.isBlank()) {
                    return true;
                }
                String content = (landmark.name() + " " + landmark.address()).toLowerCase(Locale.ROOT);
                return content.contains(normalizedQuery);
            })
            .toList();
    }

    public synchronized Landmark getLandmarkById(String landmarkId) {
        Landmark landmark = landmarks.get(landmarkId);
        if (landmark == null) {
            throw notFound("Landmark not found: " + landmarkId);
        }
        return landmark;
    }

    public synchronized Landmark createLandmark(CreateLandmarkRequest request) {
        String id = "l" + landmarkIdSequence.getAndIncrement();
        Landmark landmark = new Landmark(
            id,
            request.name(),
            request.category(),
            request.address(),
            request.description(),
            request.latitude(),
            request.longitude(),
            request.imageUrl(),
            request.rating()
        );
        landmarks.put(id, landmark);
        return landmark;
    }

    public synchronized Landmark updateLandmark(String landmarkId, UpdateLandmarkRequest request) {
        if (!landmarks.containsKey(landmarkId)) {
            throw notFound("Landmark not found: " + landmarkId);
        }

        Landmark updated = new Landmark(
            landmarkId,
            request.name(),
            request.category(),
            request.address(),
            request.description(),
            request.latitude(),
            request.longitude(),
            request.imageUrl(),
            request.rating()
        );
        landmarks.put(landmarkId, updated);
        return updated;
    }

    public synchronized void deleteLandmark(String landmarkId) {
        Landmark removed = landmarks.remove(landmarkId);
        if (removed == null) {
            throw notFound("Landmark not found: " + landmarkId);
        }
    }

    public synchronized List<RoutePlan> listRoutes(@Nullable String difficulty) {
        if (difficulty == null || difficulty.isBlank()) {
            return List.copyOf(routes.values());
        }
        return routes
            .values()
            .stream()
            .filter(route -> route.difficulty().equalsIgnoreCase(difficulty.trim()))
            .toList();
    }

    public synchronized RoutePlan getRouteById(String routeId) {
        RoutePlan route = routes.get(routeId);
        if (route == null) {
            throw notFound("Route not found: " + routeId);
        }
        return route;
    }

    public synchronized RoutePlan createRoute(CreateRouteRequest request) {
        validateRouteLandmarkIds(request.landmarkIds());
        String id = "r" + routeIdSequence.getAndIncrement();
        RoutePlan route = new RoutePlan(
            id,
            request.name(),
            request.distanceKm(),
            request.durationMinutes(),
            request.difficulty(),
            List.copyOf(request.landmarkIds())
        );
        routes.put(id, route);
        return route;
    }

    public synchronized RoutePlan updateRoute(String routeId, UpdateRouteRequest request) {
        if (!routes.containsKey(routeId)) {
            throw notFound("Route not found: " + routeId);
        }
        validateRouteLandmarkIds(request.landmarkIds());

        RoutePlan updated = new RoutePlan(
            routeId,
            request.name(),
            request.distanceKm(),
            request.durationMinutes(),
            request.difficulty(),
            List.copyOf(request.landmarkIds())
        );
        routes.put(routeId, updated);
        return updated;
    }

    public synchronized void deleteRoute(String routeId) {
        RoutePlan removed = routes.remove(routeId);
        if (removed == null) {
            throw notFound("Route not found: " + routeId);
        }
    }

    public synchronized RouteProgressResponse startRoute(String username, String routeId) {
        RoutePlan route = getRouteById(routeId);
        activeRouteIdByUser.put(username, routeId);
        return computeRouteProgress(username, route);
    }

    public synchronized CheckInResultResponse createCheckIn(
        String username,
        CreateCheckInRequest request
    ) {
        Landmark targetLandmark = getLandmarkById(request.landmarkId());
        List<CheckInRecord> userRecords = recordsForUser(username);

        LocalDate today = LocalDate.now();
        boolean duplicateToday = userRecords
            .stream()
            .anyMatch(record ->
                record.landmarkId().equals(request.landmarkId()) &&
                toLocalDate(record.checkedInAt()).equals(today)
            );

        if (duplicateToday) {
            throw new ApiException(
                HttpStatus.CONFLICT,
                "DUPLICATE_CHECKIN",
                "You already checked in at this landmark today."
            );
        }

        int distanceMeters = calculateDistanceMeters(
            request.latitude(),
            request.longitude(),
            targetLandmark.latitude(),
            targetLandmark.longitude()
        );
        if (distanceMeters > CHECK_IN_MAX_DISTANCE_METERS) {
            throw new ApiException(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "OUT_OF_RANGE",
                "Move closer to this landmark before checking in.",
                Map.of(
                    "distanceMeters",
                    distanceMeters,
                    "requiredMeters",
                    CHECK_IN_MAX_DISTANCE_METERS
                )
            );
        }

        CheckInRecord record = new CheckInRecord(
            "c" + checkInIdSequence.getAndIncrement(),
            username,
            request.landmarkId(),
            Instant.now(),
            normalizeNote(request.note())
        );
        userRecords.add(record);
        userRecords.sort(Comparator.comparing(CheckInRecord::checkedInAt).reversed());

        BadgeProgressResponse badgeProgress = computeBadgeProgress(username);
        RouteProgressAfterCheckIn routeComputation = applyRouteProgressAfterCheckIn(
            username,
            targetLandmark.id()
        );
        String message = "Check-in saved. " + badgeProgress.nextBadgeHint() + routeComputation.messageSuffix();

        return new CheckInResultResponse(
            "SUCCESS",
            message,
            record,
            badgeProgress,
            routeComputation.routeProgress()
        );
    }

    public synchronized List<CheckInRecord> listCheckIns(String username, CheckInPeriod period) {
        Instant now = Instant.now();
        LocalDate today = LocalDate.now();

        return recordsForUser(username)
            .stream()
            .sorted(Comparator.comparing(CheckInRecord::checkedInAt).reversed())
            .filter(record -> {
                if (period == CheckInPeriod.ALL) {
                    return true;
                }
                if (period == CheckInPeriod.TODAY) {
                    return toLocalDate(record.checkedInAt()).equals(today);
                }
                return !record.checkedInAt().isBefore(now.minusSeconds(7L * 24 * 60 * 60));
            })
            .toList();
    }

    public synchronized UserProgressResponse getProgress(String username) {
        BadgeProgressResponse badgeProgress = computeBadgeProgress(username);

        Set<String> visitedIds = visitedLandmarkIds(username);
        Map<LandmarkCategory, Integer> totalByCategory = new EnumMap<>(LandmarkCategory.class);
        Map<LandmarkCategory, Integer> visitedByCategory = new EnumMap<>(LandmarkCategory.class);

        for (LandmarkCategory category : LandmarkCategory.values()) {
            totalByCategory.put(category, 0);
            visitedByCategory.put(category, 0);
        }

        for (Landmark landmark : landmarks.values()) {
            totalByCategory.computeIfPresent(landmark.category(), (k, v) -> v + 1);
            if (visitedIds.contains(landmark.id())) {
                visitedByCategory.computeIfPresent(landmark.category(), (k, v) -> v + 1);
            }
        }

        List<CategoryProgressResponse> categoryProgress =
            List
                .of(LandmarkCategory.values())
                .stream()
                .map(category ->
                    new CategoryProgressResponse(
                        category,
                        visitedByCategory.getOrDefault(category, 0),
                        totalByCategory.getOrDefault(category, 0)
                    )
                )
                .toList();

        RouteProgressResponse activeRoute = null;
        String activeRouteId = activeRouteIdByUser.get(username);
        if (activeRouteId != null) {
            RoutePlan route = routes.get(activeRouteId);
            if (route != null) {
                activeRoute = computeRouteProgress(username, route);
            }
        }

        return new UserProgressResponse(badgeProgress, categoryProgress, activeRoute);
    }

    private List<CheckInRecord> recordsForUser(String username) {
        return checkInsByUser.computeIfAbsent(username, key -> new ArrayList<>());
    }

    private Set<String> visitedLandmarkIds(String username) {
        return recordsForUser(username)
            .stream()
            .map(CheckInRecord::landmarkId)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private BadgeProgressResponse computeBadgeProgress(String username) {
        int visitedCount = visitedLandmarkIds(username).size();
        int totalLandmarks = landmarks.size();

        boolean bronze = visitedCount >= 5;
        boolean silver = visitedCount >= 10;
        boolean gold = visitedCount >= 15;

        String nextBadgeHint;
        if (!bronze) {
            nextBadgeHint = "Next badge: Bronze (5)";
        } else if (!silver) {
            nextBadgeHint = "Next badge: Silver (10)";
        } else if (!gold) {
            nextBadgeHint = "Next badge: Gold (15)";
        } else {
            nextBadgeHint = "All tier badges earned.";
        }

        return new BadgeProgressResponse(
            visitedCount,
            totalLandmarks,
            bronze,
            silver,
            gold,
            nextBadgeHint
        );
    }

    private RouteProgressAfterCheckIn applyRouteProgressAfterCheckIn(
        String username,
        String checkedLandmarkId
    ) {
        String activeRouteId = activeRouteIdByUser.get(username);
        if (activeRouteId == null) {
            return new RouteProgressAfterCheckIn("", null);
        }

        RoutePlan route = routes.get(activeRouteId);
        if (route == null) {
            activeRouteIdByUser.remove(username);
            return new RouteProgressAfterCheckIn("", null);
        }

        if (!route.landmarkIds().contains(checkedLandmarkId)) {
            return new RouteProgressAfterCheckIn("", null);
        }

        RouteProgressResponse progress = computeRouteProgress(username, route);

        if (progress.completed()) {
            activeRouteIdByUser.remove(username);
            String suffix = "\n\nRoute completed: " + route.name() + ".";
            return new RouteProgressAfterCheckIn(suffix, progress);
        }

        String suffix =
            "\n\nRoute progress: " +
            progress.completedStops() +
            "/" +
            progress.totalStops() +
            " stops. Next stop: " +
            progress.nextStopName() +
            ".";
        return new RouteProgressAfterCheckIn(suffix, progress);
    }

    private RouteProgressResponse computeRouteProgress(String username, RoutePlan route) {
        Set<String> visitedIds = visitedLandmarkIds(username);
        int completedStops = 0;
        String nextStopId = null;

        for (String landmarkId : route.landmarkIds()) {
            if (visitedIds.contains(landmarkId)) {
                completedStops += 1;
                continue;
            }
            if (nextStopId == null) {
                nextStopId = landmarkId;
            }
        }

        boolean completed = nextStopId == null;
        String nextStopName = nextStopId == null ? null : landmarkNameById(nextStopId);

        return new RouteProgressResponse(
            route.id(),
            route.name(),
            completedStops,
            route.landmarkIds().size(),
            nextStopId,
            nextStopName,
            completed
        );
    }

    private String landmarkNameById(String landmarkId) {
        Landmark landmark = landmarks.get(landmarkId);
        return landmark == null ? landmarkId : landmark.name();
    }

    private void validateRouteLandmarkIds(List<String> landmarkIds) {
        List<String> invalidIds = landmarkIds
            .stream()
            .filter(id -> !landmarks.containsKey(id))
            .toList();
        if (!invalidIds.isEmpty()) {
            throw new ApiException(
                HttpStatus.BAD_REQUEST,
                "INVALID_ROUTE_LANDMARKS",
                "Route includes unknown landmark ids.",
                Map.of("invalidLandmarkIds", invalidIds)
            );
        }
    }

    private String normalizeNote(String note) {
        if (note == null) {
            return null;
        }
        String value = note.trim();
        return value.isEmpty() ? null : value;
    }

    private LocalDate toLocalDate(Instant instant) {
        return instant.atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private ApiException notFound(String message) {
        return new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", message);
    }

    private int calculateDistanceMeters(
        double userLat,
        double userLng,
        double landmarkLat,
        double landmarkLng
    ) {
        double earthRadiusMeters = 6_371_000;
        double dLat = Math.toRadians(landmarkLat - userLat);
        double dLng = Math.toRadians(landmarkLng - userLng);

        double a =
            Math.sin(dLat / 2) * Math.sin(dLat / 2) +
            Math.cos(Math.toRadians(userLat)) *
            Math.cos(Math.toRadians(landmarkLat)) *
            Math.sin(dLng / 2) *
            Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return (int) Math.round(earthRadiusMeters * c);
    }

    private void seedLandmarks() {
        putLandmark(
            new Landmark(
                "l1",
                "Irving House",
                LandmarkCategory.historic,
                "302 Royal Ave",
                "A preserved 19th century home museum.",
                49.2064,
                -122.9094,
                "https://picsum.photos/seed/nw-l1/1200/700",
                4.7
            )
        );
        putLandmark(
            new Landmark(
                "l2",
                "New Westminster Museum",
                LandmarkCategory.historic,
                "777 Columbia St",
                "Local history exhibitions and archives.",
                49.2060,
                -122.9079,
                "https://picsum.photos/seed/nw-l2/1200/700",
                4.5
            )
        );
        putLandmark(
            new Landmark(
                "l3",
                "City Hall",
                LandmarkCategory.historic,
                "511 Royal Ave",
                "Historic municipal building in downtown core.",
                49.2070,
                -122.9119,
                "https://picsum.photos/seed/nw-l3/1200/700",
                4.3
            )
        );
        putLandmark(
            new Landmark(
                "l4",
                "Westminster Pier Park",
                LandmarkCategory.historic,
                "1 Sixth St",
                "Riverfront park with boardwalk views.",
                49.2046,
                -122.9119,
                "https://picsum.photos/seed/nw-l4/1200/700",
                4.8
            )
        );
        putLandmark(
            new Landmark(
                "l5",
                "Queens Park",
                LandmarkCategory.nature,
                "First St",
                "Large urban park with open lawns and trails.",
                49.2120,
                -122.9056,
                "https://picsum.photos/seed/nw-l5/1200/700",
                4.9
            )
        );
        putLandmark(
            new Landmark(
                "l6",
                "Fraser River Trail",
                LandmarkCategory.nature,
                "Fraser River Waterfront",
                "Scenic walk route along the river.",
                49.2043,
                -122.9110,
                "https://picsum.photos/seed/nw-l6/1200/700",
                4.6
            )
        );
        putLandmark(
            new Landmark(
                "l7",
                "Hume Park",
                LandmarkCategory.nature,
                "660 East Columbia St",
                "Forest-style city park and recreation space.",
                49.2067,
                -122.8963,
                "https://picsum.photos/seed/nw-l7/1200/700",
                4.4
            )
        );
        putLandmark(
            new Landmark(
                "l8",
                "Tipperary Park",
                LandmarkCategory.nature,
                "315 Queens Ave",
                "Small downtown park near civic landmarks.",
                49.2076,
                -122.9099,
                "https://picsum.photos/seed/nw-l8/1200/700",
                4.2
            )
        );
        putLandmark(
            new Landmark(
                "l9",
                "River Market",
                LandmarkCategory.food,
                "810 Quayside Dr",
                "Popular food market by the waterfront.",
                49.2028,
                -122.9121,
                "https://picsum.photos/seed/nw-l9/1200/700",
                4.7
            )
        );
        putLandmark(
            new Landmark(
                "l10",
                "Columbia Street Cafes",
                LandmarkCategory.food,
                "Columbia St",
                "Coffee shops and local student hangouts.",
                49.2060,
                -122.9090,
                "https://picsum.photos/seed/nw-l10/1200/700",
                4.1
            )
        );
        putLandmark(
            new Landmark(
                "l11",
                "Steel and Oak Brewing",
                LandmarkCategory.food,
                "1319 Third Ave",
                "Local craft brewery with tasting room.",
                49.2093,
                -122.9020,
                "https://picsum.photos/seed/nw-l11/1200/700",
                4.6
            )
        );
        putLandmark(
            new Landmark(
                "l12",
                "Anvil Centre",
                LandmarkCategory.culture,
                "777 Columbia St",
                "Arts and culture venue with public events.",
                49.2058,
                -122.9079,
                "https://picsum.photos/seed/nw-l12/1200/700",
                4.5
            )
        );
        putLandmark(
            new Landmark(
                "l13",
                "Massey Theatre",
                LandmarkCategory.culture,
                "735 Eighth Ave",
                "Performance venue for concerts and shows.",
                49.2124,
                -122.9054,
                "https://picsum.photos/seed/nw-l13/1200/700",
                4.4
            )
        );
        putLandmark(
            new Landmark(
                "l14",
                "Douglas College New West",
                LandmarkCategory.culture,
                "700 Royal Ave",
                "Campus hub for Douglas College students.",
                49.2071,
                -122.9115,
                "https://picsum.photos/seed/nw-l14/1200/700",
                4.0
            )
        );
        putLandmark(
            new Landmark(
                "l15",
                "Samson V Maritime Museum",
                LandmarkCategory.culture,
                "Gallery at Quayside",
                "Historic paddlewheeler ship museum exhibit.",
                49.2030,
                -122.9100,
                "https://picsum.photos/seed/nw-l15/1200/700",
                4.3
            )
        );
    }

    private void putLandmark(Landmark landmark) {
        landmarks.put(landmark.id(), landmark);
    }

    private void seedRoutes() {
        routes.put(
            "r1",
            new RoutePlan(
                "r1",
                "Historic Downtown Walk",
                2.5,
                45,
                "Easy",
                List.of("l1", "l2", "l3", "l4", "l12")
            )
        );
        routes.put(
            "r2",
            new RoutePlan(
                "r2",
                "Waterfront Trail",
                3.8,
                70,
                "Medium",
                List.of("l4", "l6", "l9", "l11", "l15", "l12")
            )
        );
        routes.put(
            "r3",
            new RoutePlan(
                "r3",
                "Food and Market Tour",
                1.8,
                30,
                "Easy",
                List.of("l9", "l10", "l11", "l14")
            )
        );
        routes.put(
            "r4",
            new RoutePlan(
                "r4",
                "Queens Park and Beyond",
                3.2,
                55,
                "Medium",
                List.of("l5", "l7", "l8", "l13")
            )
        );
        routes.put(
            "r5",
            new RoutePlan(
                "r5",
                "Complete New West",
                6.5,
                120,
                "Hard",
                List.of(
                    "l1",
                    "l2",
                    "l3",
                    "l4",
                    "l5",
                    "l6",
                    "l7",
                    "l8",
                    "l9",
                    "l10",
                    "l11",
                    "l12",
                    "l13",
                    "l14",
                    "l15"
                )
            )
        );
    }

    private record RouteProgressAfterCheckIn(String messageSuffix, RouteProgressResponse routeProgress) {}
}
