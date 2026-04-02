package ca.douglas.csis4280.nwtrails.service;

import ca.douglas.csis4280.nwtrails.api.dto.BadgeProgressResponse;
import ca.douglas.csis4280.nwtrails.api.dto.CategoryProgressResponse;
import ca.douglas.csis4280.nwtrails.api.dto.CheckInResultResponse;
import ca.douglas.csis4280.nwtrails.api.dto.CreateCheckInRequest;
import ca.douglas.csis4280.nwtrails.api.dto.CreateRouteRequest;
import ca.douglas.csis4280.nwtrails.api.dto.RouteProgressResponse;
import ca.douglas.csis4280.nwtrails.api.dto.UpdateRouteRequest;
import ca.douglas.csis4280.nwtrails.api.dto.UserProgressResponse;
import ca.douglas.csis4280.nwtrails.common.ApiException;
import ca.douglas.csis4280.nwtrails.domain.CheckInPeriod;
import ca.douglas.csis4280.nwtrails.domain.CheckInRecord;
import ca.douglas.csis4280.nwtrails.domain.Landmark;
import ca.douglas.csis4280.nwtrails.domain.LandmarkCategory;
import ca.douglas.csis4280.nwtrails.domain.RoutePlan;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import ca.douglas.csis4280.nwtrails.repository.LandmarkRepository;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class NwTrailsService {

    private final LandmarkRepository landmarkRepository;

    private static final int CHECK_IN_MAX_DISTANCE_METERS = 50;
    private static final int CHECK_IN_MAX_PHOTOS = 9;
    private static final long CHECK_IN_PHOTO_MAX_BYTES = 5L * 1024 * 1024;
    private static final Set<String> ALLOWED_PHOTO_EXTENSIONS = Set.of(
        "jpg",
        "jpeg",
        "png",
        "webp"
    );
    private static final String PHOTO_URL_PREFIX = "/checkins/photos/";

    private final Map<String, RoutePlan> routes = new LinkedHashMap<>();

    private final Map<String, List<CheckInRecord>> checkInsByUser = new ConcurrentHashMap<>();
    private final Map<String, String> activeRouteIdByUser = new ConcurrentHashMap<>();
    private final Map<String, StoredCheckInPhoto> checkInPhotosById = new ConcurrentHashMap<>();
    private final Path checkInPhotoStorageRoot = Path.of("storage", "checkin-photos");

    private final AtomicLong checkInIdSequence = new AtomicLong(1);
    private final AtomicLong routeIdSequence = new AtomicLong(100);

    public NwTrailsService(LandmarkRepository landmarkRepository) {
        this.landmarkRepository = landmarkRepository;
        initializeCheckInPhotoStorage();
        seedRoutes();
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

    public synchronized String uploadCheckInPhoto(String username, MultipartFile file) {
        if (file.isEmpty()) {
            throw new ApiException(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_ERROR",
                "Check-in photo file is required."
            );
        }

        if (file.getSize() > CHECK_IN_PHOTO_MAX_BYTES) {
            throw new ApiException(
                HttpStatus.PAYLOAD_TOO_LARGE,
                "PHOTO_TOO_LARGE",
                "Check-in photo must be 5 MB or smaller."
            );
        }

        String extension = resolveImageExtension(file.getOriginalFilename());
        if (extension == null || !ALLOWED_PHOTO_EXTENSIONS.contains(extension)) {
            throw new ApiException(
                HttpStatus.BAD_REQUEST,
                "INVALID_PHOTO_TYPE",
                "Supported photo types: jpg, jpeg, png, webp."
            );
        }

        String photoId = UUID.randomUUID().toString().replace("-", "");
        Path userDirectory = checkInPhotoStorageRoot.resolve(username).normalize();
        Path targetPath = userDirectory.resolve(photoId + "." + extension).normalize();

        if (!targetPath.startsWith(userDirectory)) {
            throw new ApiException(
                HttpStatus.BAD_REQUEST,
                "INVALID_PHOTO_PATH",
                "Invalid check-in photo target path."
            );
        }

        try {
            Files.createDirectories(userDirectory);
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            throw new ApiException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "PHOTO_STORE_FAILED",
                "Failed to store check-in photo."
            );
        }

        checkInPhotosById.put(photoId, new StoredCheckInPhoto(username, targetPath));
        return PHOTO_URL_PREFIX + photoId;
    }

    public synchronized Resource loadCheckInPhoto(String username, String photoId) {
        StoredCheckInPhoto storedPhoto = findOwnedPhoto(username, photoId);
        if (!Files.exists(storedPhoto.path())) {
            throw notFound("Check-in photo not found: " + photoId);
        }
        return new FileSystemResource(storedPhoto.path());
    }

    public synchronized CheckInResultResponse createCheckIn(
        String username,
        CreateCheckInRequest request
    ) {
        Landmark targetLandmark = landmarkRepository.findById(request.landmarkId())
        .orElseThrow(() -> notFound("Landmark not found: " + request.landmarkId()));

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

        List<String> normalizedPhotoUrls = normalizePhotoUrls(request.photoUrls());
        for (String normalizedPhotoUrl : normalizedPhotoUrls) {
            ensurePhotoBelongsToUser(username, normalizedPhotoUrl);
        }

        CheckInRecord record = new CheckInRecord(
            "c" + checkInIdSequence.getAndIncrement(),
            username,
            request.landmarkId(),
            Instant.now(),
            normalizeNote(request.note()),
            normalizedPhotoUrls
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

        for (Landmark landmark : landmarkRepository.findAll()) {
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
        int totalLandmarks = Math.toIntExact(landmarkRepository.count());

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
        return landmarkRepository.findById(landmarkId)
            .map(Landmark::name)
            .orElse(landmarkId);
    }

    private void validateRouteLandmarkIds(List<String> landmarkIds) {
        Set<String> existingLandmarkIds = new LinkedHashSet<>();
        landmarkRepository.findAllById(landmarkIds)
            .forEach(landmark -> existingLandmarkIds.add(landmark.id()));

        List<String> invalidIds = landmarkIds
            .stream()
            .filter(id -> !existingLandmarkIds.contains(id))
            .distinct()
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

    private void initializeCheckInPhotoStorage() {
        try {
            Files.createDirectories(checkInPhotoStorageRoot);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to initialize check-in photo storage.", exception);
        }
    }

    private String resolveImageExtension(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return null;
        }
        int lastDotIndex = originalFilename.lastIndexOf('.');
        if (lastDotIndex < 0 || lastDotIndex == originalFilename.length() - 1) {
            return null;
        }
        return originalFilename.substring(lastDotIndex + 1).toLowerCase();
    }

    private String normalizePhotoUrl(String photoUrl) {
        if (photoUrl == null) {
            return null;
        }
        String value = photoUrl.trim();
        if (value.isEmpty()) {
            return null;
        }
        if (value.startsWith("/api/v1" + PHOTO_URL_PREFIX)) {
            return value.substring("/api/v1".length());
        }
        if (value.startsWith(PHOTO_URL_PREFIX)) {
            return value;
        }
        throw new ApiException(
            HttpStatus.BAD_REQUEST,
            "INVALID_PHOTO_URL",
            "Check-in photo URL is invalid."
        );
    }

    private List<String> normalizePhotoUrls(List<String> photoUrls) {
        if (photoUrls == null || photoUrls.isEmpty()) {
            return List.of();
        }

        List<String> normalized = new ArrayList<>();
        for (String photoUrl : photoUrls) {
            String value = normalizePhotoUrl(photoUrl);
            if (value != null) {
                normalized.add(value);
            }
        }

        List<String> deduplicated = new ArrayList<>(new LinkedHashSet<>(normalized));
        if (deduplicated.size() > CHECK_IN_MAX_PHOTOS) {
            throw new ApiException(
                HttpStatus.BAD_REQUEST,
                "TOO_MANY_PHOTOS",
                "You can attach up to " + CHECK_IN_MAX_PHOTOS + " photos per check-in."
            );
        }

        return List.copyOf(deduplicated);
    }

    private String extractPhotoId(String normalizedPhotoUrl) {
        if (!normalizedPhotoUrl.startsWith(PHOTO_URL_PREFIX)) {
            throw new ApiException(
                HttpStatus.BAD_REQUEST,
                "INVALID_PHOTO_URL",
                "Check-in photo URL is invalid."
            );
        }
        String photoId = normalizedPhotoUrl.substring(PHOTO_URL_PREFIX.length()).trim();
        if (photoId.isEmpty()) {
            throw new ApiException(
                HttpStatus.BAD_REQUEST,
                "INVALID_PHOTO_URL",
                "Check-in photo URL is invalid."
            );
        }
        return photoId;
    }

    private void ensurePhotoBelongsToUser(String username, String normalizedPhotoUrl) {
        String photoId = extractPhotoId(normalizedPhotoUrl);
        findOwnedPhoto(username, photoId);
    }

    private StoredCheckInPhoto findOwnedPhoto(String username, String photoId) {
        StoredCheckInPhoto photo = checkInPhotosById.get(photoId);
        if (photo == null || !photo.ownerUsername().equals(username)) {
            throw notFound("Check-in photo not found: " + photoId);
        }
        return photo;
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

    private record StoredCheckInPhoto(String ownerUsername, Path path) {}
}
