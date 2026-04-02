package ca.douglas.csis4280.nwtrails.domain;

import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "users")
public record UserAccount(
    @Id String id,
    String username,
    String passwordHash,
    String displayName,
    List<String> roles,
    boolean enabled
) {}
