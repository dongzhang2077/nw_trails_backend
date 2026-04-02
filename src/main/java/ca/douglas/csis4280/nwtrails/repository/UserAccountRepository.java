package ca.douglas.csis4280.nwtrails.repository;

import ca.douglas.csis4280.nwtrails.domain.UserAccount;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface UserAccountRepository extends MongoRepository<UserAccount, String> {

    Optional<UserAccount> findByUsername(String username);
}
